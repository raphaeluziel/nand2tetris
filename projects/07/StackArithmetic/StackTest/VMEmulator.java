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

	public CodeWriter(String fileName) throws IOException {
		writer = new FileWriter(fileName);
	}

	public static void writePushPop(String commandType, String segment, int i) throws IOException{
		String asmCode = "";
		//System.out.println("999 " + commandType + " " + segment + " " + i);
		if (commandType.equals("C_PUSH")) {
			asmCode = "@" + i + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n";
		}
		if (commandType.equals("C_POP")) {
			asmCode = "@SP\nAM=M-1\nD=M\n@" + (i+5) + "\nM=D\n";
		}
		writer.write(asmCode);
	}

	public static void writeArithmetic(Command vmCode, int counter) throws IOException{
		
		if (vmCode.getarg1().equals("not")) {
			writer.write("@SP\nA=M\nA=A-1\nM=!M");
		}
		if (vmCode.getarg1().equals("neg")) {
			writer.write("@SP\nA=M\nA=A-1\nM=-M");
		}

		if (vmCode.getarg1().equals("add")) {
			writer.write("@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D+M\n@SP\nM=M-1");
		}
		if (vmCode.getarg1().equals("sub")) {
			writer.write("@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=M-D\n@SP\nM=M-1");
		}
		
		if (vmCode.getarg1().equals("and")) {
			writer.write("@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D&M\n@SP\nM=M-1");
		}
		if (vmCode.getarg1().equals("or")) {
			writer.write("@SP\nA=M\nA=A-1\nD=M\nA=A-1\nM=D|M\n@SP\nM=M-1");
		}

		if (vmCode.getarg1().equals("eq")) {
			writer.write("@SP\nA=M\nA=A-1\nD=M\nA=A-1\nD=M-D\n@SP\nM=M-1\nM=M-1\n@SP\nA=M\nM=-1\n@EQ_" + counter + "\nD;JEQ\n@SP\nA=M\nM=0\n(EQ_" + counter + ")\n@SP\nM=M+1\n");
		}
		if (vmCode.getarg1().equals("lt")) {
			writer.write("@SP\nA=M\nA=A-1\nD=M\nA=A-1\nD=M-D\n@SP\nM=M-1\nM=M-1\n@SP\nA=M\nM=-1\n@LT_" + counter + "\nD;JLT\n@SP\nA=M\nM=0\n(LT_" + counter + ")\n@SP\nM=M+1\n");
		}
		if (vmCode.getarg1().equals("gt")) {
			writer.write("@SP\nA=M\nA=A-1\nD=M\nA=A-1\nD=M-D\n@SP\nM=M-1\nM=M-1\n@SP\nA=M\nM=-1\n@GT_" + counter + "\nD;JGT\n@SP\nA=M\nM=0\n(GT_" + counter + ")\n@SP\nM=M+1\n");
		}
	}

	public static void writeAsm(Command vmCode, int counter) throws IOException {
		writer.write("// " + vmCode.getcode() + "\n");
		if (vmCode.gettype().equals("C_PUSH") || vmCode.gettype().equals("C_POP")) {
			writePushPop(vmCode.gettype(), vmCode.getarg1(), vmCode.getarg2()); 
		}
		if (vmCode.gettype().equals("C_ARITHMETIC")) { 
			writeArithmetic(vmCode, counter); 
		}
		//System.out.println(vmCode.gettype() + " " + vmCode.getarg1() + " " + vmCode.getarg2());
	}

	public static void closeOutputFile() throws IOException {
		writer.close();
	}

}

   
public class VMEmulator {
	
	public static void main(String args[]) {
    
		if (args.length != 1) {
			System.out.println("Usage: java Assemble filename");
			System.exit(1);
		}
		String inputFile = args[0];
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
