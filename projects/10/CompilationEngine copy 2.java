import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import java.util.regex.*;


class CompilationEngine {

    private final FileWriter writer;
    private final String inputFileName;
    private final JackTokenizer tokenizer;
    private List<String> identifierList = new ArrayList<String>();
    private Token tk;
    private int numTabs = 0;
    private String tabString = "";

    public CompilationEngine(JackTokenizer tk, String in, String out) throws IOException {
        inputFileName = in;
        writer = new FileWriter(out);
        tokenizer = tk;
    }

    public void run() {
        tokenizer.reset();
        tokenizer.advance();
        compileClass();
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
            writer.write(tab() + s + node + ">" + "\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    private void writeXMLleaves(String[] arr) {
        try {
            for (int i = 0; i < arr.length; i++) {
                writer.write(tab() + tokenizer.toXML() + "\n");
                tokenizer.advance();
            }
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
        // 'class' + className + '{' + classVarDec* + subroutineDec* +  '}'
        writeXMLnode("class", true);
        numTabs++;
        String[] arr = { "class", "Main", "{" };
        writeXMLleaves(arr);
        compileClassVarDecW();
        compileSubroutine();
        numTabs--;
        writeXMLnode("class", false);
    }

    public void compileClassVarDec() {
        // 'static' or 'field' + 
        // 'int' or 'char' or 'boolean' or varName + 
        // (',' + varName*)
        writeXMLnode("classVarDec", true);
        numTabs++;
        String[] arr = { "" };
        writeXMLleaves(arr);
        compileClassVarDec();
        compileSubroutine();
        numTabs--;
        writeXMLnode("classVarDec", false);
    }

    String[] hell = { "POLICE" };
    public void compileClassVarDecW() {
        try {
            writer.write(tab() + "<classVarDec>\n");
            numTabs++;
            writeXMLleaves(hell);
            numTabs--;
            writer.write(tab() + "</classVarDec>\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public void compileSubroutine() {
        try {
            writer.write(tab() + "<subroutineDec>\n");
            numTabs++;
            writeXMLleaves(hell);
            compileParameterList();
            writeXMLleaves(hell);
            compileSubroutineBody();
            numTabs--;
            writer.write(tab() + "</subroutineDec>\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public void compileSubroutineBody() {
        try {
            writer.write(tab() + "<subroutineBody>\n");
            numTabs++;
            writeXMLleaves(hell);
            compileVarDec();
            numTabs--;
            writer.write(tab() + "</subroutineBody>\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public void compileParameterList() {
        try {
            writer.write(tab() + "<parameterList>\n");
            numTabs++;
            // HARD
            numTabs--;
            writer.write(tab() + "</parameterList>\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public void compileVarDec() {
        try {
            writer.write(tab() + "<varDec>\n");
            numTabs++;
            writeXMLleaves(hell);
            numTabs--;
            writer.write(tab() + "</varDec>\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public String toString() {
        return "Compiler for " + inputFileName;
    }
    
}