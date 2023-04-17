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

/**
 * Effects the actual compilation output. Gets its input from a JackTokenizer 
 * and emits its parsed structure into an output file/stream.
 */

public class JackAnalyzer {


    public static void main (String args[]) {

        if (args.length != 1) {
			System.out.println("Usage: java JackAnalyzer filename or directory");
			System.exit(1);
		}

        File file = new File(args[0]);
        FileFilter isJack = (f) -> { return f.getName().endsWith(".jack"); };
        List<File> inputFileList = new ArrayList<File>();
        List<JackTokenizer> tokenizerList = new ArrayList<JackTokenizer>();
        List<CompilationEngine> engineList = new ArrayList<CompilationEngine>();

        if (file.isDirectory())
            inputFileList = Arrays.asList(file.listFiles(isJack));
        else
            inputFileList.add(file);

        int numFiles = inputFileList.size();
        FileWriter[] writer = new FileWriter[numFiles];

        // Iterate through each file and generate token XML file for each
        for (int i = 0; i < numFiles; i++) {
            String inputFileName = inputFileList.get(i).getPath();
            String x = inputFileName;

            // For my own testing so as not to interfere with the XML files given as comparisons
            // String tokenOutputFileName = "XML/" + x.substring(0, x.indexOf(".")) + "T.xml";
            // String outputFileName = "XML/" + x.substring(0, x.indexOf(".")) + ".xml";
            
            // These will output and replace the files provided, so NOT to be used in development
            String tokenOutputFileName = x.substring(0, x.indexOf(".")) + "T.xml";
            String outputFileName = x.substring(0, x.indexOf(".")) + ".xml";

            try {
                tokenizerList.add(new JackTokenizer(inputFileList.get(i), inputFileName));
                JackTokenizer tokenizer = tokenizerList.get(i);

                engineList.add(new CompilationEngine(tokenizer, inputFileName, outputFileName));

                // Write to the token XML file
                writer[i] = new FileWriter(tokenOutputFileName);
                writer[i].write("<tokens>\n");
                while (tokenizer.hasMoreTokens()) {
                    tokenizer.advance();
                    writer[i].write(tokenizer.toXML() + "\n");
                }
                tokenizer.reset();    
                writer[i].write("</tokens>\n");
                writer[i].close();
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }

        // Iterate through each compilation engine
        for (int i = 0; i < numFiles; i++) {
            CompilationEngine engine = engineList.get(i);
            engine.run();
        }


    }
}