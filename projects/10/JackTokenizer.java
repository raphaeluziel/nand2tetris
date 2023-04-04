import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import java.util.regex.*;


class JackTokenizer {

    private Scanner reader;
    private String filename;
    private final List<String> keywords = KeyWordsAndSymbols.getkeywords();
    private final List<Character> symbols = KeyWordsAndSymbols.getsymbols();
    private final List<Token> tokenList = new ArrayList<Token>();
    private Token tk;
    private boolean multiLineComment = false;
    private final int numTokens;
    private int t = 0;
    private int line = 0;


	public JackTokenizer(File inputFile, String f) throws IOException {
        reader = new Scanner(inputFile);
        filename = f;

        String jackCode = "";

        // Look for anything that SEPARATES tokens
        String separatorRegex = "[" + Pattern.quote("{}()[].,;+-*/&|<>=~\" ") + "]";
        Pattern separatorPattern = Pattern.compile(separatorRegex);

        // Look for the next NON WHITESPACE character
        Pattern nextCharPattern = Pattern.compile("\\S");

        int cursor = 0; // Index of cursor within the string of code
        int num = 0;    // Will be used to calculate the number of tokens
        
        while (reader.hasNextLine()) {
            line++;
            jackCode = removeComments(reader.nextLine());     
            if (jackCode.isEmpty()) continue;

            int len = jackCode.length();
            cursor = 0;

            Matcher separatorMatcher = separatorPattern.matcher(jackCode);
            Matcher nextCharMatcher = nextCharPattern.matcher(jackCode);

            while (cursor < jackCode.length()) {
      
                int nextSeparator = separatorMatcher.find(cursor+1) ? separatorMatcher.start() : len;
  
                Character c = jackCode.charAt(cursor);
                
                if (Character.isWhitespace(c)) {
                    cursor = nextSeparator;
                }
                else if (symbols.contains(c)) {
                    tokenList.add(new Token(Character.toString(c), "SYMBOL", "symbol", line));
                    cursor++;
                }
                else if (c.equals('\"')) {
                    cursor++;
                    int endQuote = jackCode.indexOf("\"", cursor);
                    tokenList.add(new Token(jackCode.substring(cursor, endQuote), "STRING_CONST", "stringConstant", line));
                    cursor = endQuote + 1;
                }
                else if (Character.isDigit(c)) {
                    tokenList.add(new Token(jackCode.substring(cursor, nextSeparator), "INT_CONSTANT", "integerConstant", line));
                    cursor = nextSeparator;
                }
                else {
                    String sy = jackCode.substring(cursor, nextSeparator);
                    if (keywords.contains(sy))
                        tokenList.add(new Token(sy, "KEYWORD", "keyword", line));
                    else
                        tokenList.add(new Token(sy, "IDENTIFIER", "identifier", line)); 
                    cursor = nextSeparator;
                }

                num++;
                cursor = nextCharMatcher.find(cursor) ? nextCharMatcher.start() : len;
 
            }
        }
        reader.close();
        numTokens = num;

        for (Token k : tokenList)   System.out.println(k);

	}


    private String removeComments(String code) {
        // Remove regular comments
        int comment = code.indexOf("//");
        if (comment >= 0)   
            code = code.substring(0, comment);

        // Remove any comments in between code
        int inBtwnComment = code.indexOf("/*");
        while (inBtwnComment >= 0) {
            String start = code.substring(0, inBtwnComment);
            int endComment = code.indexOf("*/");
            if (endComment >= 0) {
                code = start + code.substring(endComment + 2);
                inBtwnComment = code.indexOf("/*");
                endComment = code.indexOf("*/");
            }
            else {
                multiLineComment = true;
                break;
            }
        }

        if (multiLineComment) {
            int endComment = code.indexOf("*/");
            if (endComment >= 0) {
                multiLineComment = false;
                code = code.substring(endComment+2);
            }
            else return "";
        }


        return code.trim();
    }


    public void advance() {
        if (hasMoreTokens())  tk = tokenList.get(t);
        t++;
    }

    public void reset() {
        t = 0;
        tk = tokenList.get(t);
    }

    public boolean hasMoreTokens() {
		return t < numTokens;
	}

    public String tokenType() {
        return tk.gettype();
    }

    public String keyword() {
        return tk.gettag().equals("keyword") ? tk.gettoken() : null;
    }

    public char symbol() {
        return tk.gettag().equals("symbol") ? tk.gettoken().charAt(0) : '\0';
    }

    public String identifier() {
        return tk.gettag().equals("identifier") ? tk.gettoken() : null;
    }

    public int intVal() {
        return  tk.gettag().equals("integerConstant") ? Integer.parseInt(tk.gettoken()) : null;
    }

    public String stringVal() {
        return tk.gettag().equals("stringConstant") ? tk.gettoken() : null;
    }

    public String toXML() {
        return tk.toXML();
    }

    public String getWord() {
        return tk.gettoken();
    }

    public String toString() {
        return "JackTokenizer for " + filename;
    }

}