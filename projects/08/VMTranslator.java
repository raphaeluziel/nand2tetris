import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException; 
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
	
	public Parser(String fileName) throws IOException {
		File file = new File(fileName);
		reader = new Scanner(file);
	}

	public static Command advance() {
		
		String vmCode = "";
		boolean hasCode = true;

		// Advance to next command, skipping whitespace and comment lines
		do {
			vmCode = reader.nextLine().trim().replaceAll("\\s", " ").split("//")[0];
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
	private static final HashMap<String, String> seg = new HashMap<String, String>();

	public CodeWriter(String f) throws IOException {
		writer = new FileWriter(f);
		fileName = f;

		seg.put("local", "LCL");
		seg.put("argument", "ARG");
		seg.put("this", "THIS");
		seg.put("that", "THAT");
	}

	public static void writePushPop(String commandType, String segment, int i) throws IOException{

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
			if (segment.equals("constant")) {
				writer.write("@" + i + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
			}
			else if (segment.equals("temp")) {
				writer.write("@" + i + "\nD=A\n@5" + "\nA=A+D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
			}
			else if (segment.equals("pointer")) {
				writer.write("@" + seg.get(segment) + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
			}
			else if (segment.equals("static")) {
				writer.write("@" + variable + "\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
			}
			else {
				writer.write("@" + i + "\nD=A\n@" + seg.get(segment) + "\nA=M+D\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
			}	
		}

		else if (commandType.equals("C_POP")) {
			if (segment.equals("temp")){
				writer.write("@SP\nAM=M-1\nD=M\n@" + (i+5) + "\nM=D\n");
			}
			else if (segment.equals("pointer")) {
				writer.write("@SP\nAM=M-1\nD=M\n@" + seg.get(segment) + "\nM=D\n");
			}
			else if (segment.equals("static")) {
				writer.write("@SP\nAM=M-1\nD=M\n@" + variable + "\nM=D\n");
			}
			else {
				writer.write("@" + i + "\nD=A\n@" + seg.get(segment) + "\nA=M\nD=A+D\n@SP\nAM=M-1\nA=M\nD=A+D\nA=D-A\nD=D-A\nM=D\n");
			}
		}
	}

	public static void writeArithmetic(Command vmCode, int counter) throws IOException{

		String code = "";

		switch (vmCode.getarg1()) {
			case "not":
				code = "@SP\nA=M\nA=A-1\nM=!M";
				break;
			case "neg":
				code = "@SP\nA=M\nA=A-1\nM=-M";
				break;
			case "add":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D+M\n@SP\nM=M-1";
				break;
			case "sub":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=M-D\n@SP\nM=M-1";
				break;
			case "and":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D&M\n@SP\nM=M-1";
				break;
			case "or":
				code = "@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D|M\n@SP\nM=M-1";
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

	public static void writeAsm(Command vmCode, int counter) throws IOException {

		writer.write("// " + vmCode.getcode() + "\n");

		if (vmCode.gettype().equals("C_PUSH") || vmCode.gettype().equals("C_POP")) {
			writePushPop(vmCode.gettype(), vmCode.getarg1(), vmCode.getarg2()); 
		}
		else if (vmCode.gettype().equals("C_ARITHMETIC")) { 
			writeArithmetic(vmCode, counter); 
		}
	}

	public static void closeOutputFile() throws IOException {
		writer.close();
	}

}

   
public class VMTranslator {
	
	public static void main(String args[]) {
    
		if (args.length != 1) {
			System.out.println("Usage: java Assemble filename");
			System.exit(1);
		}
		String inputFile = args[0];
		String inFileName = inputFile.substring(args[0].lastIndexOf("/") + 1);
		String outputFile = inputFile.substring(0, inputFile.indexOf(".")) + ".asm";

		try {
			Parser parser = new Parser(inputFile);
			CodeWriter codeWriter = new CodeWriter(outputFile);

			int counter = 0;

			while (parser.hasMoreLines()) {
				Command vmCode = parser.advance();
				counter++;
				codeWriter.writeAsm(vmCode, counter);
			}

			parser.closeInputFile();
			codeWriter.closeOutputFile();

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
	}	
}
