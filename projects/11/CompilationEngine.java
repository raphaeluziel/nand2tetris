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

    private int numTabs;
    private String tabString;
    private String className;
    private String functionName;

    private int nArgs;

    private int labelNum;


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
        
        numTabs = 0;
        tabString = "";

        labelNum = 1;
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
        compileSubroutineDec();

        tokenizer.advance();    // '}'
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
    public void compileSubroutineDec() {

        List<String> typ = Arrays.asList("constructor", "function", "method");

        if (!typ.contains(tokenizer.getToken()))    return;

        subroutineSymbolTable.reset();
       
        tokenizer.advance();   // ('constructor' | 'function' | 'method') 
        tokenizer.advance();    // ('void' | type)
        functionName = className + "." + tokenizer.getToken();

        tokenizer.advance();   // subroutineName 
        tokenizer.advance();    // '('
        compileParameterList();
        tokenizer.advance();    // ')'
        compileSubroutineBody();

        compileSubroutineDec();
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
     * Compile body of subroutine
     * '{' varDec* statements '}'
     */
    public void compileSubroutineBody() {
        tokenizer.advance();    // '{'
        compileVarDec();
        writer.writeFunction(functionName, subroutineSymbolTable.varCount("VAR"));
        compileStatements();
        tokenizer.advance();    // '}'
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

        if (tokenizer.getToken().equals("[")) {
            tokenizer.advance();    // '['
            compileExpression();
            tokenizer.advance();    // ']'
        }

        tokenizer.advance();    // '='

        compileExpression();
        
        writer.writePop(seg, index);

        tokenizer.advance();    // ';'
    }

    /**
     * Compile an if statement
     * 'if' '(' expression ')' '{' statements '}'
     * ('else' '{' statements '}')?
     */
    public void compileIf() {

        tokenizer.advance();    // 'if'
        tokenizer.advance();    // '('
        compileExpression();
        tokenizer.advance();    // ')'
        writer.writeArithmetic("~");
        writer.writeIf("L" + labelNum);
        tokenizer.advance();    // '{'
        labelNum += 2;
        compileStatements();
        labelNum -= 2;
        tokenizer.advance();    // '}'
        writer.writeGoto("L" + (labelNum + 1));
        writer.writeLabel("L" + labelNum);

        if (tokenizer.getToken().equals("else")) {
            tokenizer.advance();    // 'else'
            tokenizer.advance();    // '{'
            compileStatements();
            tokenizer.advance();    // '}'
        }
        writer.writeLabel("L" + (labelNum + 1));
    }

    /**
     * Compile a while statement
     * 'while' '(' expression ')' '{' statements '}'
     */
    public void compileWhile() {

        tokenizer.advance();    // 'while'
        tokenizer.advance();    // '('
        writer.writeLabel("L" + labelNum);
        compileExpression();
        writer.writeArithmetic("~");
        writer.writeIf("L" + (labelNum + 1));
        tokenizer.advance();    // ')'
        tokenizer.advance();    // '{'
        labelNum += 2;
        compileStatements();
        labelNum -= 2;
        writer.writeGoto("L" + labelNum);
        writer.writeLabel("L" + (labelNum + 1));
        tokenizer.advance();    // '}'
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

        nArgs = 1;

        if (!tokenizer.getToken().equals(")")) {
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
     */
    public void compileTerm() {

        String tkn = tokenizer.getToken();

        // If the current token is an identifier, we must look ahead to the next
        // token to decide what we are dealing with
        if (tokenizer.tokenType().equals("IDENTIFIER")) {
            
            switch (tokenizer.getNextToken()) {

                // A dot means an identifier with a subroutineCall will be coming
                case ".":
                    String fullFunctionName = tokenizer.getToken();
                    tokenizer.advance();    // className
                    tokenizer.advance();    // '.'
                    functionName = fullFunctionName + "." + tokenizer.getToken(); 
                    tokenizer.advance();    // functionName
                    tokenizer.advance();    // '('
                    compileExpressionList();
                    writer.writeCall(functionName, nArgs);
                    tokenizer.advance();    // ')'
                    break;

                // A [ means this is an array
                case "[":
                    tokenizer.advance();    // varName
                    tokenizer.advance();    // '['
                    compileExpression();
                    tokenizer.advance();    // ']'
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
                writer.writePush("CONSTANT", 1);
                writer.writeArithmetic("neg");
            }
            else if (tkn.equals("false") || tkn.equals("null")) {
                writer.writePush("CONSTANT", 0);
            }
            else if (tkn.equals("this")) {
                writer.writePush("POINTER", 0);
            }
            tokenizer.advance();    // 'true' | 'false'
        }

        // This is an interConstant
        else {
            writer.writePush("CONSTANT", Integer.parseInt(tokenizer.getToken()));
            tokenizer.advance();    // integerConstant
        }

    }


    public String toString() {
        return "Compiler for " + inputFileName;
    }
    
}