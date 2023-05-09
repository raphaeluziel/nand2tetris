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
 * Effects the actual compilation output. Gets its input from a JackTokenizer 
 * and emits its parsed structure into an output file/stream.  This is the actual
 * compiler that starts the whole process
 */

public class JackCompiler {

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

        // Iterate through each file
        for (int i = 0; i < numFiles; i++) {
            String inputFileName = inputFileList.get(i).getPath();
            String x = inputFileName;

            //String vmOutputFileName = "VMS/" + x.substring(0, x.indexOf(".")) + ".vm";
            String vmOutputFileName = x.substring(0, x.indexOf(".")) + ".vm";

            try {
                tokenizerList.add(new JackTokenizer(inputFileList.get(i), inputFileName));
                JackTokenizer tokenizer = tokenizerList.get(i);

                engineList.add(new CompilationEngine(tokenizer, inputFileName, vmOutputFileName));
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