/******************************************************************************
 * 
 * Author: Raphael Uziel
 * Date: May 9, 2023
 * 
******************************************************************************/

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;

/**
 * Actually writes to the .vm file all the commands given to it by
 * the compilation engine
 */

class VMWriter {

    private final FileWriter writer;
    private final String outputFile;

    private final HashMap<String, String> ops;
    private final HashMap<String, String> segments;
    
	public VMWriter(String out) throws IOException {
        writer = new FileWriter(out);
        outputFile = out;

        ops = new HashMap<String, String>();
        ops.put("+", "add");
        ops.put("-", "sub");
        ops.put("=", "eq");
        ops.put("&gt;", "gt");
        ops.put("&lt;", "lt");
        ops.put("&amp;", "and");
        ops.put("|", "or");
        ops.put("~", "not");
        ops.put("neg", "neg");

        segments = new HashMap<String, String>();
        segments.put("CONSTANT", "constant");
        segments.put("LOCAL", "local");
        segments.put("VAR", "local");
        segments.put("ARG", "argument");
        segments.put("ARGUMENT", "argument");
        segments.put("STATIC", "static");
        segments.put("TEMP", "temp");
        segments.put("FIELD", "this");
        segments.put("THIS", "this");
        segments.put("THAT", "that");
        segments.put("POINTER", "pointer");
	}

    private void tryWrite(String code) {
        try {
            writer.write(code + "\n");
        }
        catch (IOException e) {
            System.out.println(e);
        }   
    }

    private void writeVM(String command) {
        tryWrite(command.toLowerCase());
    }

    private void writeVM(String command, String label) {
        tryWrite(command.toLowerCase() + " " + label);
    }

    private void writeVM(String command, String seg, int index) {
        String s = segments.get(seg);
        tryWrite(command + " " + s + " " + index);
    }

    private void writeVM(String command, String name, int numArgs, boolean functionOrCall) {
        tryWrite(command + " " + name + " " + numArgs);
    }

    public void writePush(String segment, int index) {
        writeVM("push", segment, index);
    }

    public void writePop(String segment, int index) {
        writeVM("pop", segment, index);
    }

    public void writeArithmetic(String command) {
        writeVM(ops.get(command));
    }

    public void writeLabel(String label) {
        writeVM("label", label);
    }

    public void writeGoto(String label) {
        writeVM("goto", label);
    }

    public void writeIf(String label) {
        writeVM("if-goto", label);
    }

    public void writeCall(String name, int nArgs) {
        writeVM("call", name, nArgs, true);
    }

    public void writeFunction(String name, int nVars) {
        writeVM("function", name, nVars, true);
    }

    public void writeReturn() {
        writeVM("return");
    }

    public void close() {
        try {
            writer.close();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public String toString() {
        return "VMWriter for " + outputFile;
    }

    public static void main(String[] args) {
        try {
            VMWriter vmw = new VMWriter("Test.vm");
            vmw.writePush("CONSTANT", 8);
            vmw.writePop("STATIC", 9);
            vmw.writeArithmetic("ADD");
            vmw.writeLabel("IF_TRUE");
            vmw.writeGoto("IF_TRUE");
            vmw.writeIf("IF_TRUE");
            vmw.writeCall("Main.fibonacci", 7);
            vmw.writeFunction("Main.fibonacci", 9);
            vmw.writeReturn();
            vmw.close();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

}