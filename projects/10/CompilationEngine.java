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

    private final FileWriter writer;
    private final String inputFileName;
    private final JackTokenizer tokenizer;
    private List<String> typeList = new ArrayList<String>(Arrays.asList("int", "char", "boolean"));
    private List<String> returnTypeList = new ArrayList<String>(Arrays.asList("int", "char", "boolean", "void"));
    private List<String> statementList = new ArrayList<String>(Arrays.asList("let", "if", "while", "do", "return"));
    private List<String> opList = new ArrayList<String>(Arrays.asList("+", "-", "*", "/", "&amp;", "|", "&lt;", "&gt;", "="));
    private List<String> unaryOpList = new ArrayList<String>(Arrays.asList("~", "-"));
    private List<String> noSymbolCheckNeeded = Collections.<String>emptyList();
    private List<String> noTypeCheckNeeded = Collections.<String>emptyList();
    private List<String> keywordConstantList = new ArrayList<String>(Arrays.asList("true", "false", "null", "this"));
    private int numTabs = 0;
    private String tabString = "";


    public CompilationEngine(JackTokenizer jtk, String in, String out) throws IOException {
        inputFileName = in;
        writer = new FileWriter(out);
        tokenizer = jtk;
    }

    /**
     * This is the method that is called each time another file is given
     * This is what is called to start the compilation process
     */
    public void run() {
        tokenizer.reset();
        tokenizer.advance();
        while (tokenizer.hasMoreTokens()) {
            compileClass();
        }    
        try {
            writer.close();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Writes the nodes, that is the non terminal branches
     */
    private void writeXMLnode(String node, boolean type) {
        // type = true if it's an open tag
        String s = type ? "<" : "</";

        try {
            if (!type)     numTabs--;
            writer.write(tab() + s + node + ">" + "\n");
            if (type)      numTabs++;
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Outputs an error message and exits if a syntax error is caught
     */
    private void syntaxError() {
        System.out.println("Syntax error in " + tokenizer.getFileName() + ", line " + tokenizer.getLine() + ": " + tokenizer.getToken());
        System.exit(1);
    }

    /**
     * Writes the leaves, that is the end tokens, or the terminal language elements
     * The method takes three parameters, tk, the actual token to write,
     * check, a list of strings to compare the token to (for checking syntax errors),
     * and a list of strings to check token types for syntax errors.
     */
    private void writeXMLleaf(String tk, List<String> check, List<String> checkType) {

        // check and checkType are empty if these checks are not required
        boolean good = checkType.isEmpty() || checkType.contains(tokenizer.tokenType());
        good = good && (check.isEmpty() || check.contains(tk));

        // If not good then output a syntax error and exit program
        if (!good)  syntaxError();

        try {
            writer.write(tab() + tokenizer.toXML() + "\n");
            tokenizer.advance();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Formats the XML nicely by setting up tabs
     */
    private String tab() {
        tabString = "";
        for (int i = 0; i < numTabs; i++) {
            tabString += "\t";
        }
        return tabString;
    }

    /**
     * Compiles a whole class
     * 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() {

        writeXMLnode("class", true);
        
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("class"), noTypeCheckNeeded);

        // Add the identifier (className) to the list of types such as int
        // char, boolean, since a class can also be a type
        if (tokenizer.tokenType().equals("IDENTIFIER")) {
            typeList.add(tokenizer.getToken());
            returnTypeList.add(tokenizer.getToken());
        }

        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);
        compileClassVarDec();
        compileSubroutineDec();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);

        writeXMLnode("class", false);     
    }

    /**
     * Compiles the class variable declarations
     * ('static' | 'field') type varName (',' varName)* ';'
     */
    public void compileClassVarDec() {

        List<String> typ = Arrays.asList("static", "field");

        if (!typ.contains(tokenizer.getToken()))    return;

        writeXMLnode("classVarDec", true);

        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), noTypeCheckNeeded, noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        while (tokenizer.getToken().equals(",")) {
            writeXMLleaf(tokenizer.getToken(), Arrays.asList(","), noTypeCheckNeeded);
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
        }

        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);
        writeXMLnode("classVarDec", false);

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

        writeXMLnode("subroutineDec", true);

        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), returnTypeList, noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);
        compileParameterList();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);
        compileSubroutineBody();

        writeXMLnode("subroutineDec", false);

        compileSubroutineDec();
    }

    /**
     * Compile a paremeter list
     * ((type varName) (',' type varName)*)?
     */
    public void compileParameterList() {

        writeXMLnode("parameterList", true);
        // If there are parameters within the parenthesis
        if (!tokenizer.getToken().equals(")")) {
            // type ('int', 'char', 'booean', or className)
            writeXMLleaf(tokenizer.getToken(), noTypeCheckNeeded, noTypeCheckNeeded);
            // varName
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
            // (',' + type + varName)*
            while (tokenizer.getToken().equals(",")) {
                // Symbol: ','
                writeXMLleaf(tokenizer.getToken(), Arrays.asList(","), noTypeCheckNeeded);
                // type ('int', 'char', 'booean', or className)
                writeXMLleaf(tokenizer.getToken(), typeList, noTypeCheckNeeded);
                // varName
                writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
            }
        }

        writeXMLnode("parameterList", false);
    }

    /**
     * Compile body of subroutine
     * '{' varDec* statements '}'
     */
    public void compileSubroutineBody() {

        writeXMLnode("subroutineBody", true);

        writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);
        compileVarDec();
        compileStatements();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);

        writeXMLnode("subroutineBody", false);  
    }

    /**
     * Compile variable declarations
     * 'var' type varName (',' varName)* ';'
     */
    public void compileVarDec() {

        // If there are no variable declarations return
        if (!tokenizer.getToken().equals("var"))    return;

        writeXMLnode("varDec", true);
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("var"), noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), noTypeCheckNeeded, noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        while (tokenizer.getToken().equals(",")) {
            writeXMLleaf(tokenizer.getToken(), Arrays.asList(","), noTypeCheckNeeded);
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
        }

        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("varDec", false);

        compileVarDec();
    }

    /**
     * Compile statements
     * statement*
     */
    public void compileStatements() {

        writeXMLnode("statements", true);

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

        writeXMLnode("statements", false);
    }

    /**
     * Compile a let statement
     * 'let' varName ('[' expression ']')? '=' expression ';'
     */
    public void compileLet() {

        writeXMLnode("letStatement", true);

        writeXMLleaf(tokenizer.getToken(), Arrays.asList("let"), noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        if (tokenizer.getToken().equals("[")) {
            writeXMLleaf(tokenizer.getToken(), Arrays.asList("["), noTypeCheckNeeded);
            compileExpression();
            writeXMLleaf(tokenizer.getToken(), Arrays.asList("]"), noTypeCheckNeeded);
        }

        writeXMLleaf(tokenizer.getToken(), Arrays.asList("="), noTypeCheckNeeded);
        compileExpression();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("letStatement", false);
    }

    /**
     * Compile an if statement
     * 'if' '(' expression ')' '{' statements '}'
     * ('else' '{' statements '}')?
     */
    public void compileIf() {

        writeXMLnode("ifStatement", true);

        writeXMLleaf(tokenizer.getToken(), Arrays.asList("if"), noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);
        compileExpression();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);
        compileStatements();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);

        if (tokenizer.getToken().equals("else")) {
            writeXMLleaf(tokenizer.getToken(), Arrays.asList("else"), noTypeCheckNeeded);
            writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);
            compileStatements();
            writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);
        }

        writeXMLnode("ifStatement", false);
    }

    /**
     * Compile a while statement
     * 'while' '(' expression ')' '{' statements '}'
     */
    public void compileWhile() {

        writeXMLnode("whileStatement", true);

        writeXMLleaf(tokenizer.getToken(), Arrays.asList("while"), noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);
        compileExpression();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);
        compileStatements();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);

        writeXMLnode("whileStatement", false);
    }

    /**
     * Compile a do statement
     * 'do' subroutineCall ';'
     */
    public void compileDo() {

        writeXMLnode("doStatement", true);

        writeXMLleaf(tokenizer.getToken(), Arrays.asList("do"), noTypeCheckNeeded);
        subroutineCall();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("doStatement", false);
    }

    /**
     * Compiles a subroutine
     * Not sure if needed, but hey, it's here
     * subroutineName '(' expressionList ')' | 
     * (className | varName) '.' subroutineName '(' expressionList ')'
      */
    private void subroutineCall() {

        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        // If a '.' appears now ...
        if (tokenizer.getToken().equals(".")) {
            writeXMLleaf(".", noSymbolCheckNeeded, noTypeCheckNeeded);
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
        }

        writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);
        compileExpressionList();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);
    }

    /**
     * Compiles an expression list
     * (expression (',' expression)*)?
     */
    public void compileExpressionList() {

        writeXMLnode("expressionList", true);

        if (!tokenizer.getToken().equals(")")) {
            compileExpression();

            while (tokenizer.getToken().equals(",")) {
                writeXMLleaf(tokenizer.getToken(), Arrays.asList(","), noTypeCheckNeeded);
                compileExpression();
            }
        }

        writeXMLnode("expressionList", false);
    }

    /**
     * Compiles a return statement
     * 'return' expression? ';'
     */
    public void compileReturn() {

        writeXMLnode("returnStatement", true);

        writeXMLleaf(tokenizer.getToken(), Arrays.asList("return"), noTypeCheckNeeded);
        if (!tokenizer.getToken().equals(";")) compileExpression();
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("returnStatement", false);
    }

    /**
     * Compiles an expression
     * term (op term)*
     */
    public void compileExpression() {

        writeXMLnode("expression", true);

        compileTerm();

        while (opList.contains(tokenizer.getToken())) {
            writeXMLleaf(tokenizer.getToken(), opList, noTypeCheckNeeded);
            compileTerm();
        }

        writeXMLnode("expression", false);
    }

    /**
     * TCompiles a term
     * integerConstant | stringConstant | keywordConstant | 
     * varName | varName '[' expression ']' |
     * subroutineCall |
     * '(' expression ')' |
     * unaryOp term
     */
    public void compileTerm() {

        writeXMLnode("term", true);

        // If the current token is an identifier, we must look ahead to the next
        // token to decide what we are dealing with
        if (tokenizer.tokenType().equals("IDENTIFIER")) {
            switch (tokenizer.getNextToken()) {

                // A dot means an identifier with a subroutineCall will be coming
                case ".":
                    writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
                    writeXMLleaf(".", noSymbolCheckNeeded, noTypeCheckNeeded);
                    writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
                    writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);
                    compileExpressionList();
                    writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);
                    break;

                // A [ means this is an array
                case "[":
                    writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
                    writeXMLleaf("[", noSymbolCheckNeeded, noTypeCheckNeeded);
                    compileExpression();
                    writeXMLleaf("]", Arrays.asList("]"), noTypeCheckNeeded);
                    break;

                // This is just a variable
                default:
                    writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
            }
        }

        // This is NOT an identifier, but since it is a parenthesis must add an 
        // expression within the parenthesis
        else if (tokenizer.getToken().equals("(")) {
            writeXMLleaf("(", noSymbolCheckNeeded, noTypeCheckNeeded);
            compileExpression();
            writeXMLleaf(")", Arrays.asList(")"), noTypeCheckNeeded);
        }

        // This is NOT an identifier, but it IS a unaryOp
        else if (unaryOpList.contains(tokenizer.getToken())) {
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
            compileTerm();
        }

        // This is NONE of the options above.  
        // Not sure if this is even needed
        else {
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
        }

        writeXMLnode("term", false);
    }


    public String toString() {
        return "Compiler for " + inputFileName;
    }
    
}