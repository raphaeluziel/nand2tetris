import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import java.util.regex.*;


class CompilationEngine {

    private final FileWriter writer;
    private final String inputFileName;
    private final JackTokenizer tokenizer;
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

    private void writeXMLnode(String node, boolean type, String[] words) {
        // type = true if it's an open tag
        String s = type ? "< " : "</ ";

        try {
            writer.write(tab() + s + node + " >" + "\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    private void writeXMLleaves(int numTokens) {

        try {
            for (int i = 0; i < numTokens; i++) {
                String typ = tokenizer.tokenType();
                String tok = tokenizer.getWord();
                // Error check that the tokens match what is expected
                for (int i = 0; i < words.length; i++) {
                    if (typ.equals("KEYWORD") || typ.equals("SYMBOL")) {
                        System.out.println("HEY " + typ + " " + tok);
                        if (tok.equals(words[i]));
                    }
                    else {
                        System.out.println("Syntax error");
                        System.exit(1);
                    }
                }


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
    String[] hell = {"hell"};
    public void compileClass() {
        // class + className + { + classVarDec* + subroutineDec* +  }
        String[] arrOpen = { "class", "className", "{" };
        writeXMLnode("class", true);
        numTabs++;
        writeXMLleaves(3, arrOpen);
        compileClassVarDec();
        compileSubroutine();
        numTabs--;
        String[] arrClose = { "}" };
        writeXMLnode("class", false);
    }

    public void compileClassVarDec() {
        try {
            writer.write(tab() + "<classVarDec>\n");
            numTabs++;
            writeXMLleaves(4, hell);
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
            writeXMLleaves(4, hell);
            compileParameterList();
            writeXMLleaves(1, hell);
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
            writeXMLleaves(1, hell);
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
            writeXMLleaves(4, hell);
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