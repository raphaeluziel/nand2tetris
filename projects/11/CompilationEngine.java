/******************************************************************************
 * 
 * Author: Raphael Uziel
 * Date: April 17, 2023
 * 
******************************************************************************/

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import java.util.regex.*;

/**
 * Effects the actual compilation output. Gets its input from a JackTokenizer 
 * and emits its parsed structure into an output file/stream.
 * 
 *  @author Raphael Uziel
 */


class CompilationEngine {

    private final VMWriter writer;
    private final String inputFileName;
    private final JackTokenizer tokenizer;

    private final SymbolTable classSymbolTable;
    private final SymbolTable subroutineSymbolTable;
    
    private List<String> typeList;
    private List<String> returnTypeList;
    private List<String> statementList;
    private List<String> opList;
    private List<String> unaryOpList;
    private List<String> noSymbolCheckNeeded;
    private List<String> noTypeCheckNeeded;
    private List<String> keywordConstantList;

    private String className;

    // This will be for the number of arguments of functions that are NOT defined
    // by me, thus, the subroutineSymbolTable will have no count of the number
    // of arguments for these functions, i.e, do Output.printInt(1 + (2 * 3));
    private int nArgs;

    private int whileLabelNum;
    private int ifLabelNum;


    public CompilationEngine(JackTokenizer jtk, String in, String vmout) throws IOException {
        
        inputFileName = in;
        tokenizer = jtk;

        classSymbolTable = new SymbolTable();
        subroutineSymbolTable = new SymbolTable();

        writer = new VMWriter(vmout);

        typeList = new ArrayList<String>(Arrays.asList("int", "char", "boolean"));
        returnTypeList = new ArrayList<String>(Arrays.asList("int", "char", "boolean", "void"));
        statementList = new ArrayList<String>(Arrays.asList("let", "if", "while", "do", "return"));
        keywordConstantList = new ArrayList<String>(Arrays.asList("true", "false", "null", "this"));
        opList = new ArrayList<String>(Arrays.asList("+", "-", "*", "/", "&amp;", "|", "&lt;", "&gt;", "="));
        unaryOpList = new ArrayList<String>(Arrays.asList("~", "-"));

        noSymbolCheckNeeded = Collections.<String>emptyList();
        noTypeCheckNeeded = Collections.<String>emptyList();

        whileLabelNum = 0;
        ifLabelNum = 0;
    }

    /**
     * This is the method that is called each time another file is given
     * This is what is called to start the compilation process
     */
    public void run() {
        tokenizer.reset();
        tokenizer.advance();     
        compileClass();
        writer.close();
    }

    /**
     * Compiles a whole class
     * 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() {
        
        tokenizer.advance();    // 'class'

        // Add the identifier (className) to the list of types such as int
        // char, boolean, since a class can also be a type
        className = tokenizer.getToken();
        typeList.add(tokenizer.getToken());
        returnTypeList.add(tokenizer.getToken());

        tokenizer.advance();   // className 
        tokenizer.advance();   // '{'

        compileClassVarDec();
        compileSubroutine();

        tokenizer.advance();    // '}'
        //System.out.println(classSymbolTable);
        writer.close();
    }

    /**
     * Compiles the class variable declarations
     * ('static' | 'field') type varName (',' varName)* ';'
     */
    public void compileClassVarDec() {

        List<String> typ = Arrays.asList("static", "field");

        if (!typ.contains(tokenizer.getToken()))    return;

        String kindd = tokenizer.getToken().toUpperCase();
        tokenizer.advance();    // ('static' | 'field') 

        String typee = tokenizer.getToken();
        tokenizer.advance();    // type

        String namee = tokenizer.getToken();
        classSymbolTable.define(namee, typee, kindd);
        tokenizer.advance();    // varName

        while (tokenizer.getToken().equals(",")) {
            tokenizer.advance();    // ','
            namee = tokenizer.getToken();
            classSymbolTable.define(namee, typee, kindd);
            tokenizer.advance();    // varName
        }

        tokenizer.advance();    // ';'
        compileClassVarDec();
    }

    /**
     * Compiles a soubroutine declaration
     * ('constructor' | 'function' | 'method') ('void' | type)
     * subroutineName '(' parameterList ')' subroutineBody
     */
    public void compileSubroutine() {

        List<String> typ = Arrays.asList("constructor", "function", "method");

        if (!typ.contains(tokenizer.getToken()))    return;

        String fType = tokenizer.getToken();

        subroutineSymbolTable.reset();

        if (fType.equals("method")) {
            //System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
            subroutineSymbolTable.define("this", className, "ARG");
        }

        tokenizer.advance();   // ('constructor' | 'function' | 'method') 
        tokenizer.advance();    // ('void' | type)

        String fName = className + "." + tokenizer.getToken();

        tokenizer.advance();   // subroutineName 
        tokenizer.advance();    // '('
        compileParameterList();
        tokenizer.advance();    // ')'

        //compileSubroutineBody();
        // '{' varDec* statements '}'
        tokenizer.advance();    // '{'
        compileVarDec();
        
        writer.writeFunction(fName, subroutineSymbolTable.varCount("VAR"));
        
        if (fType.equals("constructor")) {
            writer.writePush("CONSTANT", classSymbolTable.varCount("FIELD"));
            writer.writeCall("Memory.alloc", 1);
            writer.writePop("POINTER", 0);
        }
        else if (fType.equals("method")) {
            writer.writePush("ARG", 0);
            writer.writePop("POINTER", 0);
        }
        compileStatements();
        tokenizer.advance();    // '}'

        whileLabelNum = 0;
        ifLabelNum = 0;

        //if (fName.equals("setDestination")) 
        //System.out.println("--------------------------------START " + fName);
        //System.out.println(subroutineSymbolTable);
        //System.out.println("----------------------------------END " + fName + "\n\n\n\n");

        compileSubroutine();
    }

    /**
     * Compile a paremeter list
     * ((type varName) (',' type varName)*)?
     */
    public void compileParameterList() {

        // If there are parameters within the parenthesis
        if (!tokenizer.getToken().equals(")")) {
            String typee = tokenizer.getToken();
            tokenizer.advance();    // type

            // varName
            String namee = tokenizer.getToken();
            subroutineSymbolTable.define(namee, typee, "ARG");
            tokenizer.advance();

            // (',' + type + varName)*
            while (tokenizer.getToken().equals(",")) {
                // Symbol: ','
                tokenizer.advance();    // ','

                // type ('int', 'char', 'booean', or className)
                typee = tokenizer.getToken();
                tokenizer.advance();

                // varName
                namee = tokenizer.getToken();
                subroutineSymbolTable.define(namee, typee, "ARG");
                tokenizer.advance();
            }
        }
    }


    /**
     * Compile variable declarations
     * 'var' type varName (',' varName)* ';'
     */
    public void compileVarDec() {

        // If there are no variable declarations return
        if (!tokenizer.getToken().equals("var"))    return;

        tokenizer.advance();    // 'var'

        String typee = tokenizer.getToken();
        tokenizer.advance();    // type

        String namee = tokenizer.getToken();
        subroutineSymbolTable.define(namee, typee, "VAR");
        tokenizer.advance();    // varName

        while (tokenizer.getToken().equals(",")) {
            tokenizer.advance();    // ','
            namee = tokenizer.getToken();
            subroutineSymbolTable.define(namee, typee, "VAR");
            tokenizer.advance();    // varName
        }

        tokenizer.advance();    // ';'

        compileVarDec();
    }

    /**
     * Compile statements
     * statement*
     */
    public void compileStatements() {

        while(statementList.contains(tokenizer.getToken())) {
            switch(tokenizer.getToken()) {
                case "let":
                    compileLet();
                    break;
                case "if":
                    compileIf();
                    break;
                case "while":
                    compileWhile();
                    break;
                case "do":
                    compileDo();
                    break;
                case "return":
                    compileReturn();
                    break;
                default:
                    return;
            }
        }
    }


    /**
     * Compile a let statement
     * 'let' varName ('[' expression ']')? '=' expression ';'
     */
    public void compileLet() {
        
        tokenizer.advance();    // 'let'

        // Memorize the segment and index of the current token
        String s = tokenizer.getToken();
        String seg = classSymbolTable.has(s) ? classSymbolTable.kindOf(s) : subroutineSymbolTable.kindOf(s);
        int index = classSymbolTable.has(s) ?  classSymbolTable.indexOf(s) : subroutineSymbolTable.indexOf(s);
    
        tokenizer.advance();    // varName

        boolean isArray = tokenizer.getToken().equals("[");
        
        if (tokenizer.getToken().equals("[")) {
            tokenizer.advance();    // '['
            compileExpression();
            tokenizer.advance();    // ']'
            writer.writePush(seg, index);
            writer.writeArithmetic("+");  
        }

        tokenizer.advance();    // '='
        
        compileExpression();

        if (isArray) {
            writer.writePop("TEMP", 0);
            writer.writePop("POINTER", 1);
            writer.writePush("TEMP", 0);
            writer.writePop("THAT", 0);
        }
        else {
            writer.writePop(seg, index);
        }

        tokenizer.advance();    // ';'
    }

    /**
     * Compile an if statement
     * 'if' '(' expression ')' '{' statements '}'
     * ('else' '{' statements '}')?
     */
    public void compileIf() {
        int x = ifLabelNum;
        tokenizer.advance();    // 'if'
        tokenizer.advance();    // '('
        compileExpression();
        tokenizer.advance();    // ')'
        writer.writeIf("IF_TRUE" + x);
        writer.writeGoto("IF_FALSE" + x);
        tokenizer.advance();    // '{'
        writer.writeLabel("IF_TRUE" + x);
        ifLabelNum++;
        compileStatements();
        ifLabelNum--;
        tokenizer.advance();    // '}'

        if (tokenizer.getToken().equals("else")) {
            writer.writeGoto("IF_END" + x);
            writer.writeLabel("IF_FALSE" + x);
            tokenizer.advance();    // 'else'
            tokenizer.advance();    // '{' 
            ifLabelNum++;
            compileStatements();
            ifLabelNum--;
            writer.writeLabel("IF_END" + x);
            tokenizer.advance();    // '}'
        }
        else {
            writer.writeLabel("IF_FALSE" + x);
        }
        ifLabelNum++;
    }

    /**
     * Compile a while statement
     * 'while' '(' expression ')' '{' statements '}'
     */
    public void compileWhile() {
        int x = whileLabelNum;
        tokenizer.advance();    // 'while'
        tokenizer.advance();    // '('
        writer.writeLabel("WHILE_EXP" + x);
        compileExpression();
        writer.writeArithmetic("~");
        writer.writeIf("WHILE_END" + x);
        tokenizer.advance();    // ')'
        tokenizer.advance();    // '{'
        whileLabelNum++;
        compileStatements();
        whileLabelNum--;
        writer.writeGoto("WHILE_EXP" + x);
        writer.writeLabel("WHILE_END" + x);
        tokenizer.advance();    // '}'
        whileLabelNum++;
    }

    /**
     * Compile a do statement
     * 'do' subroutineCall ';'
     */
    public void compileDo() {
        tokenizer.advance();    // 'do'
        compileExpression();
        writer.writePop("TEMP", 0);
        tokenizer.advance();    // ';'
    }

    /**
     * Compiles an expression list
     * (expression (',' expression)*)?
     */
    public void compileExpressionList() {

        nArgs = 0;

        if (!tokenizer.getToken().equals(")")) {
            nArgs++;
            compileExpression();

            while (tokenizer.getToken().equals(",")) {
                nArgs++;
                tokenizer.advance();    // ','
                compileExpression();
            }
        }
    }

    /**
     * Compiles a return statement
     * 'return' expression? ';'
     */
    public void compileReturn() {
        tokenizer.advance();    // 'return'
        if (!tokenizer.getToken().equals(";"))  compileExpression();
        else    writer.writePush("CONSTANT", 0);
        writer.writeReturn();
        tokenizer.advance();    // ';'
    }

    /**
     * Compiles an expression
     * term (op term)*
     */
    public void compileExpression() {

        compileTerm();

        while (opList.contains(tokenizer.getToken())) {
            String op = tokenizer.getToken();
            tokenizer.advance();    // op     
            compileTerm();
            if (op.equals("*")) writer.writeCall("Math.multiply", 2);
            else if (op.equals("/")) writer.writeCall("Math.divide", 2);
            else writer.writeArithmetic(op);
        }
    }

    /**
     * Compiles a term
     * integerConstant | stringConstant | keywordConstant | 
     * varName | varName '[' expression ']' |
     * subroutineCall |
     * '(' expression ')' |
     * unaryOp term
     * NOTE: a subroutineCall can be any of the following:
     * subroutineName '(' expressionList ')' OR
     * className|varName '.' subroutineName '(' expressionList ')' 
     */
    public void compileTerm() {
        
        String tkn = tokenizer.getToken();
        String fName = "";
        boolean isMethod = false;

        // If the current token is an identifier, we must look ahead to the next
        // token to decide what we are dealing with
        if (tokenizer.tokenType().equals("IDENTIFIER")) {
            
            switch (tokenizer.getNextToken()) {
                // A dot means 
                // className|varName '.' subroutineName '(' expressionList ')' 
                case ".":

                    // This is a varName NOT a className
                    // It will either be in the subroutineSymbolTable OR
                    // the classSymbolTable, but either way, the TYPE
                    // will be the name of a CLASS and this will therefore
                    // be a METHOD being APPLIED to that varName
                    
                    if (classSymbolTable.has(tkn)) {
                        fName = classSymbolTable.typeOf(tkn);
                        writer.writePush(classSymbolTable.kindOf(tkn), classSymbolTable.indexOf(tkn));
                        isMethod = true;
                    }
                    else if (subroutineSymbolTable.has(tkn)) {
                        fName = subroutineSymbolTable.typeOf(tkn);
                        writer.writePush(subroutineSymbolTable.kindOf(tkn), subroutineSymbolTable.indexOf(tkn));
                        isMethod = true;
                    }
                    // Otherwise this is a className NOT a varName
                    // It must therefore be a FUNCTION that is being called
                    // Therefore we DO NOT push the instance
                    else {
                        fName = tkn;
                    }


                    tokenizer.advance();    // className | varName
                    tokenizer.advance();    // '.'

                    fName += "." + tokenizer.getToken();

                    tokenizer.advance();    // functionName     
                    tokenizer.advance();    // '('
                    compileExpressionList();
                    if (isMethod) nArgs++;
                    tokenizer.advance();    // ')'

                    // NOTE: The function call needs to determine how many args
                    // were pushed, which can be determined when compiling the 
                    // expressionList
                    writer.writeCall(fName, nArgs);

                    break;

                // A [ means this is an array
                case "[":
                    tokenizer.advance();    // varName
                    tokenizer.advance();    // '['
                    compileExpression();
                    writer.writePush(subroutineSymbolTable.kindOf(tkn), subroutineSymbolTable.indexOf(tkn));
                    writer.writeArithmetic("+"); 
                    writer.writePop("POINTER", 1);
                    writer.writePush("THAT", 0);
                    tokenizer.advance();    // ']'
                    break;

                // This is a subroutine METHOD within a class, so it does not have the
                // '.', example (do draw())
                case "(":
                    //System.out.println("THERE "); //-----------------------------------------
                    fName = className + "." + tkn;
                    tokenizer.advance();    // subroutineName
                    tokenizer.advance();    // '('
                    writer.writePush("POINTER", 0);
                    compileExpressionList();
                    nArgs++;
                    tokenizer.advance();    // ')'
                    //System.out.println(fName + " " + nArgs);
                    
                    writer.writeCall(fName, nArgs);
                    break;

                // This is just a variable
                default:
                    String seg = classSymbolTable.has(tkn) ? classSymbolTable.kindOf(tkn) : subroutineSymbolTable.kindOf(tkn);
                    int index = classSymbolTable.has(tkn) ?  classSymbolTable.indexOf(tkn) : subroutineSymbolTable.indexOf(tkn);
                    writer.writePush(seg, index);
                    tokenizer.advance();    // varNazme
            }
        }

        // This is NOT an identifier, but since it is a parenthesis must add an 
        // expression within the parenthesis
        else if (tkn.equals("(")) {
            tokenizer.advance();    // '('
            compileExpression();
            tokenizer.advance();    // ')'
        }

        // This is NOT an identifier, but it IS a unaryOp
        else if (unaryOpList.contains(tkn)) {
            boolean negate = tkn.equals("-");
            tokenizer.advance();   // unaryOp
            compileTerm();
            if (negate) writer.writeArithmetic("neg");
            else        writer.writeArithmetic("~");
        }

        // this is a 'true' or 'false' or 'null' or 'this'
        else if (keywordConstantList.contains(tkn)) {
            if (tkn.equals("true")) {
                writer.writePush("CONSTANT", 0);
                writer.writeArithmetic("~");
            }
            else if (tkn.equals("false") || tkn.equals("null")) {
                writer.writePush("CONSTANT", 0);
            }
            else if (tkn.equals("this")) {
                //System.out.println("HERE "); //------------------------------------------------
                writer.writePush("POINTER", 0);
            }
            tokenizer.advance();    // 'true' | 'false'
        }

        // This is an interConstant
        else if (Character.isDigit(tkn.charAt(0))) {
            writer.writePush("CONSTANT", Integer.parseInt(tokenizer.getToken()));
            tokenizer.advance();    // integerConstant
        }

        // This is a stringConstant
        else {
            writer.writePush("CONSTANT", tkn.length());
            writer.writeCall("String.new", 1);
            for (int i = 0; i < tkn.length(); i++) {
                writer.writePush("CONSTANT", (int) tkn.charAt(i));
                writer.writeCall("String.appendChar", 2);
            }
            tokenizer.advance();    // stringConstant
        }
    }


    public String toString() {
        return "Compiler for " + inputFileName;
    }
    
}