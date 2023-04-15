import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import java.util.regex.*;


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
    private int numTabs = 0;
    private String tabString = "";


    public CompilationEngine(JackTokenizer jtk, String in, String out) throws IOException {
        inputFileName = in;
        writer = new FileWriter(out);
        tokenizer = jtk;
    }

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

    private void syntaxError() {
        System.out.println("Syntax error in " + tokenizer.getFileName() + ", line " + tokenizer.getLine() + ": " + tokenizer.getToken());
        System.exit(1);
    }

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

    private String tab() {
        tabString = "";
        for (int i = 0; i < numTabs; i++) {
            tabString += "\t";
        }
        return tabString;
    }

    public void compileClass() {

        writeXMLnode("class", true);
        
        // class
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("class"), noTypeCheckNeeded);

        // className (add it to the types list as well)
        if (tokenizer.tokenType().equals("IDENTIFIER")) {
            typeList.add(tokenizer.getToken());
            returnTypeList.add(tokenizer.getToken());
        }
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);

        // Symbol: '{'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);

        // classVarDec*
        compileClassVarDec();

        // compileSubroutineDec*
        compileSubroutineDec();

        // Symbol: '}'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);

        writeXMLnode("class", false);     
    }

    public void compileClassVarDec() {

        List<String> typ = Arrays.asList("static", "field");

        // End recursion
        if (!typ.contains(tokenizer.getToken()))    return;

        writeXMLnode("classVarDec", true);

        // 'static' or 'field'
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
        
        // type ('int', 'char', 'booean', or className)
        writeXMLleaf(tokenizer.getToken(), noTypeCheckNeeded, noTypeCheckNeeded);

        // varName
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        // (',' + varName)*
        while (tokenizer.getToken().equals(",")) {
            // Symbol: ','
            writeXMLleaf(tokenizer.getToken(), Arrays.asList(","), noTypeCheckNeeded);
            // varName
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
        }

        // ';'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("classVarDec", false);

        // Recursion for every class variable declaration
        compileClassVarDec();
    }

    public void compileSubroutineDec() {

        List<String> typ = Arrays.asList("constructor", "function", "method");

        // End recursion
        if (!typ.contains(tokenizer.getToken()))    return;

        writeXMLnode("subroutineDec", true);

        // 'constructor', 'function', 'method'
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);
        
        // 'void' or type ('int', 'char', 'booean', or className)
        writeXMLleaf(tokenizer.getToken(), returnTypeList, noTypeCheckNeeded);

        // subroutineName
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        // Symbol: '('
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);

        // parameterList
        compileParameterList();

        // Symbol: ')'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);

        // subroutineBody
        compileSubroutineBody();

        writeXMLnode("subroutineDec", false);

        // Recursion for every subroutine declaration
        compileSubroutineDec();
    }

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

    public void compileSubroutineBody() {

        writeXMLnode("subroutineBody", true);

        // Symbol: '{'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);

        // varDec*
        compileVarDec();

        // statements
        compileStatements();

        // Symbol: '}'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);

        writeXMLnode("subroutineBody", false);  
    }

    public void compileVarDec() {

        // If there are no variable declarations return
        if (!tokenizer.getToken().equals("var"))    return;

        writeXMLnode("varDec", true);

        // 'var'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("var"), noTypeCheckNeeded);

        // type ('int', 'char', 'booean', or className)
        writeXMLleaf(tokenizer.getToken(), noTypeCheckNeeded, noTypeCheckNeeded);

        // varName
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        // (',' + varName)*
        while (tokenizer.getToken().equals(",")) {
            // Symbol: ','
            writeXMLleaf(tokenizer.getToken(), Arrays.asList(","), noTypeCheckNeeded);
            // varName
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
        }

        // Symbol: ';'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("varDec", false);

        compileVarDec();
    }

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

    public void compileLet() {

        writeXMLnode("letStatement", true);

        // 'let'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("let"), noTypeCheckNeeded);

        // varName
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        // '[' + expression + ']'

        // Symbol: '='
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("="), noTypeCheckNeeded);

        // expression
        compileExpression();

        // Symbol: ';'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("letStatement", false);
    }

    public void compileIf() {

        writeXMLnode("ifStatement", true);

        // 'if'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("if"), noTypeCheckNeeded);

        // Symbol: '('
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);

        // expression
        compileExpression();

        // Symbol: ')'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);

        // Symbol: '{'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);

        // statements
        compileStatements();

        // Symbol: '}'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);

        // ('else' '{' statements '}')?
        if (tokenizer.getToken().equals("else")) {
            // 'else'
            writeXMLleaf(tokenizer.getToken(), Arrays.asList("else"), noTypeCheckNeeded);

            // Symbol: '{'
            writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);

            // statements
            compileStatements();

            // Symbol: '}'
            writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);
        }

        writeXMLnode("ifStatement", false);
    }

    public void compileWhile() {

        writeXMLnode("whileStatement", true);

        // 'while'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("while"), noTypeCheckNeeded);

        // Symbol: '('
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);

        // expression
        compileExpression();

        // Symbol: ')'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);

        // Symbol: '{'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("{"), noTypeCheckNeeded);

        // statements
        compileStatements();

        // Symbol: '}'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("}"), noTypeCheckNeeded);

        writeXMLnode("whileStatement", false);
    }

    public void compileDo() {

        writeXMLnode("doStatement", true);

        // 'do'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("do"), noTypeCheckNeeded);

        // subroutineCall
        subroutineCall();

        // Symbol: ';'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("doStatement", false);
    }

    private void subroutineCall() {

        // subroutineName or className or varName
        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));

        // If a '.' appears now ...
        if (tokenizer.getToken().equals(".")) {
            // Symbol: '.'
            writeXMLleaf(".", noSymbolCheckNeeded, noTypeCheckNeeded);
            // subroutineName
            writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, Arrays.asList("IDENTIFIER"));
        }

        // Symbol: '('
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("("), noTypeCheckNeeded);

        // expressionList
        compileExpressionList();

        // Symbol: ')'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(")"), noTypeCheckNeeded);
    }

    public void compileExpressionList() {

        writeXMLnode("expressionList", true);

        if (!tokenizer.getToken().equals(")")) {
            // expression
            compileExpression();

            // (',' + expression)*
            while (tokenizer.getToken().equals(",")) {
                // Symbol: ','
                writeXMLleaf(tokenizer.getToken(), Arrays.asList(","), noTypeCheckNeeded);
                // expression
                compileExpression();
            }
        }

        writeXMLnode("expressionList", false);
    }

    public void compileReturn() {

        writeXMLnode("returnStatement", true);

        // 'return'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList("return"), noTypeCheckNeeded);

        // expression?
        if (!tokenizer.getToken().equals(";")) compileExpression();

        // Symbol: ';'
        writeXMLleaf(tokenizer.getToken(), Arrays.asList(";"), noTypeCheckNeeded);

        writeXMLnode("returnStatement", false);
    }

    public void compileExpression() {

        writeXMLnode("expression", true);

        // term
        compileTerm();

        // (op term)*
        if (opList.contains(tokenizer.getToken())) {
            writeXMLleaf(tokenizer.getToken(), opList, noTypeCheckNeeded);
            compileTerm();
        }

        writeXMLnode("expression", false);
    }

    public void compileTerm() {

        writeXMLnode("term", true);

        writeXMLleaf(tokenizer.getToken(), noSymbolCheckNeeded, noTypeCheckNeeded);

        writeXMLnode("term", false);
    }


    public String toString() {
        return "Compiler for " + inputFileName;
    }
    
}