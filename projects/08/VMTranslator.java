import java.util.Scanner;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import java.util.HashMap;


class Command {
	private String vmCode;
	private String commandType;
	private String arg1;
	private Integer arg2;
	private String[] comm;
	private int counter;

	public Command(String command) {

		vmCode = command;

		comm = command.split(" ");

		switch(comm[0]) {
			case "push":
				commandType = "C_PUSH";
				arg1 = comm[1];
				arg2 = Integer.parseInt(comm[2]);
				break;
			case "pop":
				commandType = "C_POP";
				arg1 = comm[1];
				arg2 = Integer.parseInt(comm[2]);
				break;
			case "label":
				commandType = "C_LABEL";
				arg1 = comm[1];
				break;
			case "goto":
				commandType = "C_GOTO";
				arg1 = comm[1];
				break;
			case "if-goto":
				commandType = "C_IF";
				arg1 = comm[1];
				break;
			case "function":
				commandType = "C_FUNCTION";
				arg1 = comm[1];
				arg2 = Integer.parseInt(comm[2]);
				break;
			case "return":
				commandType = "C_RETURN";
				break;
			case "call":
				commandType = "C_CALL";
				arg1 = comm[1];
				break;
			default:
				commandType = "C_ARITHMETIC";
				arg1 = comm[0];
		}

		if (comm.length > 1) { arg1 = comm[1]; }
		if (comm.length > 2) { arg2 = Integer.parseInt(comm[2]); }
	}

	public String  getcode() { return vmCode; }
	public String  gettype() { return commandType; }
	public String  getarg1() { return arg1;}
	public Integer getarg2() { return arg2;}
	
}

class Parser {

	private static Scanner reader;
	
	public Parser(File file) throws IOException {
		reader = new Scanner(file);
	}

	public static Command advance() {
		
		String vmCode = "";
		boolean hasCode = true;

		// Advance to next command, skipping whitespace and comment lines
		do {
			String splits[] = reader.nextLine().trim().replaceAll("\\s", " ").split("//");
			vmCode = splits.length > 0 ? splits[0] : "";
		} while (vmCode.isEmpty() && hasMoreLines());

		return new Command(vmCode);
	}

	public static boolean hasMoreLines() {
		return reader.hasNextLine();
	}

	public static void closeInputFile() throws IOException {
		reader.close();
	}

}


class CodeWriter {

	private static FileWriter writer;
	private static String fileName;
	private static String functionName = "";	// Needed for writing labels
	private static final HashMap<String, String> seg = new HashMap<String, String>();

	public CodeWriter(String f) throws IOException {
		writer = new FileWriter(f);
		fileName = f;

		seg.put("local", "LCL");
		seg.put("argument", "ARG");
		seg.put("this", "THIS");
		seg.put("that", "THAT");
	}

	private static void setFunctionName(String newName)	{ 
		functionName = newName; 
	}

	public static void writePushPop(Command vmCode, String classname) throws IOException {

		String code = "";

		String commandType = vmCode.gettype();
		String segment = vmCode.getarg1();
		int i = vmCode.getarg2();

		seg.put("temp", Integer.toString(5 + i));
		String variable = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.indexOf(".")) + "." + i;

		if (segment.equals("pointer")) {
			if (i == 0){
				seg.put("pointer", "THIS"); 
				i=3;
			}
			if (i == 1){
				seg.put("pointer", "THAT"); 
				i=4;
			}
		}

		if (commandType.equals("C_PUSH")) {

			switch(segment) {
				case "constant":
					code = "@" + i + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
					break;
				case "temp":
					code = "@" + i + "\nD=A\n@5" + "\nA=A+D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
					break;
				case "pointer":
					code = "@" + seg.get(segment) + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
					break;
				case "static":
					code = "@" + classname + "." + i + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
					break;
				// Default is for 'this', 'that', 'local', or 'argument'	
				default:
					code = "@" + i + "\nD=A\n@" + seg.get(segment) + "\nA=M+D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
			}	
		}

		else if (commandType.equals("C_POP")) {
			switch(segment) {
				case "temp":
					code = "@SP\nAM=M-1\nD=M\n@" + (i+5) + "\nM=D\n";
					break;
				case "pointer":
					code = "@SP\nAM=M-1\nD=M\n@" + seg.get(segment) + "\nM=D\n";
					break;
				case "static":
					code = "@SP\nAM=M-1\nD=M\n@" + classname + "." + i + "\nM=D\n";
					break;
				// Default is for 'this', 'that', 'local', or 'argument'	
				default:
					code = "@" + i + "\nD=A\n@" + seg.get(segment) + "\nA=M\nD=A+D\n@SP\nAM=M-1\nA=M\nD=A+D\nA=D-A\nD=D-A\nM=D\n";
			}
		}
		writer.write(code);
	}

	public static void writeArithmetic(Command vmCode, int counter) throws IOException {

		String code = "";

		switch (vmCode.getarg1()) {
			case "not":
				code = "@SP\nA=M\nA=A-1\nM=!M\n";
				break;
			case "neg":
				code = "@SP\nA=M\nA=A-1\nM=-M\n";
				break;
			case "add":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D+M\n@SP\nM=M-1\n";
				break;
			case "sub":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=M-D\n@SP\nM=M-1\n";
				break;
			case "and":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D&M\n@SP\nM=M-1\n";
				break;
			case "or":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D|M\n@SP\nM=M-1\n";
				break;
			case "eq":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nD=M-D\n@SP\nM=M-1\nM=M-1\n@SP\nA=M\nM=-1\n@EQ_" + counter + "\nD;JEQ\n@SP\nA=M\nM=0\n(EQ_" + counter + ")\n@SP\nM=M+1\n";
				break;
			case "lt":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nD=M-D\n@SP\nM=M-1\nM=M-1\n@SP\nA=M\nM=-1\n@LT_" + counter + "\nD;JLT\n@SP\nA=M\nM=0\n(LT_" + counter + ")\n@SP\nM=M+1\n";
				break;
			case "gt":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nD=M-D\n@SP\nM=M-1\nM=M-1\n@SP\nA=M\nM=-1\n@GT_" + counter + "\nD;JGT\n@SP\nA=M\nM=0\n(GT_" + counter + ")\n@SP\nM=M+1\n";
		}
		writer.write(code);
	}

	public static void writeLabel(Command vmCode) throws IOException {
		if (!functionName.equals(""))
			writer.write("(" + functionName.toLowerCase() + "$" + vmCode.getarg1().toLowerCase() + ")\n");
		else
			writer.write("(" + vmCode.getarg1() + ")\n");
	}

	public static void writeGoto(Command vmCode) throws IOException {
		if (!functionName.equals(""))
			writer.write("@" + functionName.toLowerCase() + "$" + vmCode.getarg1().toLowerCase() + "\n0;JEQ\n");
		else
			writer.write("@" + vmCode.getarg1() + "\n0;JEQ\n");
	}

	public static void writeIf(Command vmCode) throws IOException{
		if (!functionName.equals(""))
			writer.write("@SP\nA=M-1\nD=M\n@SP\nM=M-1\n@" + functionName.toLowerCase() + "$" + vmCode.getarg1().toLowerCase() + "\nD;JNE\n");
		else
			writer.write("@SP\nA=M-1\nD=M\n@SP\nM=M-1\n@" + vmCode.getarg1() + "\nD;JNE\n");
	}

	public static void writeFunction(Command vmCode) throws IOException {
		String funcName = vmCode.getarg1().toLowerCase();
		writer.write("(" + funcName + ")\n");
		setFunctionName(funcName);
		int numLocals = vmCode.getarg2();
		for (int i = 0; i < numLocals; i++)
			writer.write("@0\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
	}

	public static void writeReturn(Command vmCode) throws IOException {
		String code = "";
		
		code += "@LCL\nD=M\n@R13\nM=D\n";					// FRAME = LCL
		code += "@5\nD=A\n@R13\nA=M-D\nD=M\n@14\nM=D\n";	// RET = *(FRAME-5)
		code += "@SP\nA=M-1\nD=M\n@ARG\nA=M\nM=D\n";		// *ARG = pop()
		code += "@ARG\nD=M\n@SP\nM=D+1\n";					// SP = ARG + 1
		code += "@R13\nA=M-1\nD=M\n@THAT\nM=D\n";			// THAT = *(FRAME - 1)
		code += "@2\nD=A\n@R13\nA=M-D\nD=M\n@THIS\nM=D\n";	// THIS = (FRAME - 2)
		code += "@3\nD=A\n@R13\nA=M-D\nD=M\n@ARG\nM=D\n";	// ARG = *(FRAME - 3)
		code += "@4\nD=A\n@R13\nA=M-D\nD=M\n@LCL\nM=D\n";	// LCL = *(FRAME - 4)
		code += "@14\nA=M\n0;JMP\n";						// goto RET

		writer.write(code);
	}


	public static void writeCall(Command vmCode, int counter) throws IOException {
		String code = "";
		int numArgs = vmCode.getarg2();

		code += "@RET_ADDRESS_" + counter + "\n";			// Create label for return address
		code += "D=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";			// Push return address
		code += "@LCL\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";	// Push LCL
		code += "@ARG\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";	// Push ARG
		code += "@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";	// Push THIS
		code += "@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";	// Push THAT
		// Reposition ARG to ARG = SP - numArgs - 5
		code += "@" + numArgs + "\nD=A\n@5\nD=D+A\n@SP\nA=M\nD=A-D\n@ARG\nM=D\n";
		// Reposition LCL to LCL = SP
		code += "@SP\nD=M\n@LCL\nM=D\n";				
		code += "@" + vmCode.getarg1().toLowerCase() + "\n0;JMP\n";	// goto function
		code += "(RET_ADDRESS_" + counter + ")\n";

		writer.write(code);
	}


	public static void writeBoot() throws IOException {
		writer.write("// Set SP to 256\n");		
		writer.write("@256\nD=A\n@SP\nM=D\n");		// Set SP = 256
		Command vm = new Command("call Sys.init 0");
		writeAsm(vm, 0, "");
	}


	public static void writeAsm(Command vmCode, int counter, String classname) throws IOException {

		writer.write("// " + vmCode.getcode() + "\n");
		
		switch (vmCode.gettype()) {
			case "C_PUSH":
			case "C_POP":
				writePushPop(vmCode, classname);
				break;
			case "C_ARITHMETIC":
				writeArithmetic(vmCode, counter); 
				break;
			case "C_LABEL":
				writeLabel(vmCode);
				break;
			case "C_GOTO":
				writeGoto(vmCode);
				break;
			case "C_IF":
				writeIf(vmCode);
				break;
			case "C_FUNCTION":
				writeFunction(vmCode);
				break;
			case "C_RETURN":
				writeReturn(vmCode);
				break;
			case "C_CALL":
				writeCall(vmCode, counter);
				break;
		}
	}

	public static void closeOutputFile() throws IOException {
		writer.close();
	}

}

   
public class VMTranslator {

	private static int startRunning(File inputFile, CodeWriter codeWriter, int counter) {

		String classname = inputFile.getName();
		classname = classname.substring(0, classname.indexOf(".")).toLowerCase();

		try {
			Parser parser = new Parser(inputFile);
			
			while (parser.hasMoreLines()) {
				Command vmCode = parser.advance();
				counter++;
				codeWriter.writeAsm(vmCode, counter, classname);
			}
			parser.closeInputFile();
		}
		catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				System.out.println("File not found.");
				System.exit(1);
			}
			else {
				e.printStackTrace();
			}
		}
		return counter;
	}
	
	public static void main(String args[]) {

		if (args.length != 1) {
			System.out.println("Usage: java VMTranslator filename or directory");
			System.exit(1);
		}

		File inputFile = new File(args[0]);
		String outputFile = "";
		int counter = 0;

		try {
			if (inputFile.isDirectory()) {
				outputFile = args[0] + "/" + inputFile.getName() + ".asm";
				CodeWriter codeWriter = new CodeWriter(outputFile);
				codeWriter.writeBoot();

				File[] listOfFiles = inputFile.listFiles();

				for (File f : listOfFiles) 
					if (f.isFile() && f.getName().endsWith("Sys.vm"))
						counter = startRunning(f, codeWriter, counter);

				for (File f : listOfFiles)
					if (f.isFile() && !f.getName().endsWith("Sys.vm") && f.getName().endsWith(".vm"))
						counter = startRunning(f, codeWriter, counter);
				
				codeWriter.closeOutputFile();
			}	
			else {
				outputFile = args[0].substring(0, args[0].indexOf(".")) + ".asm";
				CodeWriter codeWriter = new CodeWriter(outputFile);
				counter = startRunning(inputFile, codeWriter, counter);
				codeWriter.closeOutputFile();
			}
		}
		catch (IOException e) {
			System.out.println("Could not create output file");
		}

	}	
}
