import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;

public class JackAnalyzer {

    private static void writeXML (FileWriter w, String s, boolean close) {
        try {
            w.write(s);
            if (close)  w.close();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void main (String args[]) {

        if (args.length != 1) {
			System.out.println("Usage: java JackAnalyzer filename or directory");
			System.exit(1);
		}

        File file = new File(args[0]);
        FileFilter isJack = (f) -> { return f.getName().endsWith(".jack"); };
        List<File> inputFileList = new ArrayList<File>();
        List<JackTokenizer> jackTokenizerList = new ArrayList<JackTokenizer>();

        if (file.isDirectory())
            inputFileList = Arrays.asList(file.listFiles(isJack));
        else
            inputFileList.add(file);

        int numFiles = inputFileList.size();
        FileWriter[] writer = new FileWriter[numFiles];

        // Iterate through each file
        for (int i = 0; i < numFiles; i++) {
            String inputFileName = inputFileList.get(i).getPath();
            String x = inputFileList.get(i).getPath();
            String outputFileName = "XML/" + x.substring(0, x.indexOf(".")) + "T.xml";

            try {
                jackTokenizerList.add(new JackTokenizer(inputFileList.get(i), inputFileName));
                writer[i] = new FileWriter(outputFileName);
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }

        // Iterate through each jackTokenizer, then iterate through each token
        // and write the XML to the file
        for (int i = 0; i < numFiles; i++) {
            JackTokenizer tokenizer = jackTokenizerList.get(i);
            writeXML(writer[i], "<tokens>\n", false);
            while (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
                writeXML(writer[i], tokenizer.toXML() + "\n", false);
            }        
            writeXML(writer[i], "</tokens>\n", true);
        }

    }
}