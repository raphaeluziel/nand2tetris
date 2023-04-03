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

    private void writeXML(int numTokens) {
        try {
            for (int i = 0; i < numTokens; i++) {
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
        try {
            writer.write("<class>\n");
            numTabs++;
            writeXML(3);
            compileClassVarDec();
            compileSubroutine();
            numTabs--;
            writer.write("</class>\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public void compileClassVarDec() {
        try {
            writer.write(tab() + "<classVarDec>\n");
            numTabs++;
            writeXML(4);
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
            writeXML(4);
            compileParameterList();
            writeXML(1);
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
            writeXML(1);
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
            writeXML(4);
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