import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException; 
import java.util.HashMap;

public class Assemble {

    public static int VARIABLE = 16;      // RAM location of first variable
    
    public static boolean isInteger(String str) {
        // https://stackoverflow.com/a/237204/10951070

        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static HashMap<String, String> createMap(String filename) {
        // Create a HashMap using the lookup table in the file with filename
        HashMap<String, String> mnemonic = new HashMap<String, String>();
        Scanner mnemonicCodes = new Scanner(System.in);
        try {
            File mnemonicCodeFile = new File(filename);
            Scanner reader = new Scanner(mnemonicCodeFile);
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                String[] mnemonicCode = data.split("\\s+");
                mnemonic.put(mnemonicCode[0], mnemonicCode[1]);
            }
            reader.close();   
        } 
        catch (FileNotFoundException e) {
            System.out.println("File not found.");
            e.printStackTrace();
        }
        return mnemonic;
    }

    public static void addLabelsToSymbolMap(String inputFilename, HashMap<String, Integer> label) {

        int lineNumber = 0;

        try {
            File inputFile = new File(inputFilename);
            Scanner reader = new Scanner(inputFile);

            // Populate the symbol HashMap with the labels
            while (reader.hasNextLine()) {

                String data = reader.nextLine();
                data = data.trim().replaceAll("\\s", "").split("//")[0];

                boolean isNotEmpty = !data.isEmpty() && !data.startsWith("//");
                boolean isLabel = data.startsWith("(") && data.endsWith(")");           
                
                if (isNotEmpty && isLabel) {
                    label.put(data.substring(1, data.length() - 1), lineNumber);
                }
                else if (isNotEmpty && !isLabel) {
                    lineNumber += 1;
                }
            }
        }
        catch (IOException e) {
            System.out.println("File not found.");
            //e.printStackTrace();
        }
    
    }


    public static void addVirtualRegistersToSymbolMap(HashMap<String, Integer> vRegs) {
        String reg = "R";
        Integer i = 0;
        for (i = 0; i < 16; i++) {
            vRegs.put(reg + i.toString(), i);
        }
    }


    public static void addSpecialRegistersToSymbolMap(HashMap<String, Integer> sRegs) {
        sRegs.put("SP", 0);
        sRegs.put("LCL", 1);
        sRegs.put("ARG", 2);
        sRegs.put("THIS", 3);
        sRegs.put("THAT", 4);
        sRegs.put("SCREEN", 16384);
        sRegs.put("KBD", 24576);
    }


    public static String aInstruction(String asm, HashMap<String, Integer> symbol) {

        // Return the binary Hack code for a line with an a-Instruction

        String aInst = asm.replace("@", "");
        int num = 0;

        if (!isInteger(aInst)) {
            if (symbol.get(aInst) == null) {
                symbol.put(aInst, VARIABLE);
                num = VARIABLE;
                VARIABLE += 1;
            }
            else {
                num = symbol.get(aInst);
            }
        }
        else {
            num = Integer.parseInt(aInst);
        }
        System.out.println("DREAM " + num);

        aInst = String.format("%32s", Integer.toBinaryString(num))
                            .replace(' ', '0').substring(17);
        aInst = "0" + aInst;
        
        return aInst;
    }


    public static String cInstruction(String asm, 
                                      HashMap<String, String> jump,
                                      HashMap<String, String> comp,
                                      HashMap<String, String> dest) {

        // dest=comp;jump
        // op 11 ac1c2c3c4c5c6 d1d2d3 j1j2j3

        String binary[] = new String[3];

        int equals = asm.indexOf("=");
        int semicolon = asm.contains(";") ? asm.indexOf(";") : asm.length();

        binary[0] = asm.substring(equals + 1, semicolon);               // comp
        binary[1] = asm.contains("=") ? asm.split("=")[0] : "null";     // dest
        binary[2] = asm.contains(";") ? asm.split(";")[1] : "null";     // jump

        return "111" + comp.get(binary[0]) + dest.get(binary[1]) + jump.get(binary[2]);
    }
    

    public static void main(String args[]) {

        if (args.length != 1) {
            System.out.println("Usage: java Assemble filename");
            System.exit(1);
        }

        String inputFilename = args[0];

        // Read the lookup codes from the files into the HashMaps
        HashMap<String, String> jump = createMap("jumpCodes.txt");
        HashMap<String, String> dest = createMap("destinationCodes.txt");
        HashMap<String, String> comp = createMap("computationCodes.txt");

        // Create a HashMap of all the symbols in the file
        HashMap<String, Integer> symbol = new HashMap<String, Integer>();
        addVirtualRegistersToSymbolMap(symbol);
        addSpecialRegistersToSymbolMap(symbol);

        try {
            File inputFile = new File(inputFilename);
            String outputFilename = inputFilename.substring(0, inputFilename.indexOf(".")) + ".hack";

            FileWriter writer = new FileWriter(outputFilename);

            Scanner reader = new Scanner(inputFile);

            addLabelsToSymbolMap(inputFilename, symbol);

            while (reader.hasNextLine()) {

                String data = reader.nextLine();
                String hackCode = "";

                data = data.trim().replaceAll("\\s", "").split("//")[0];

                boolean isNotEmpty = !data.isEmpty() && !data.startsWith("//");
                boolean isLabel = data.startsWith("(") && data.endsWith(")");

                if (isNotEmpty && !isLabel) {
                    
                    if (data.startsWith("@")) {
                        hackCode = aInstruction(data, symbol);
                    }
                    else {
                        hackCode = cInstruction(data, jump, comp, dest);
                    }
                    // String hackCode = data.startsWith("@") ? aInstruction(data) : cInstruction(data);
                    writer.write(hackCode + "\n");
                }  
   
            }
            writer.close();
            reader.close();      
        } 
        catch (IOException e) {
            System.out.println("File not found.");
            System.exit(1);
            //e.printStackTrace();
        }


        symbol.entrySet().forEach(entry -> {
            System.out.println(" - " + entry.getValue() + "\t" + entry.getKey());
        });
        

    }

}
