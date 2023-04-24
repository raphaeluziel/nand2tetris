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
import java.util.regex.*;

/**
 * Removes all comments and white space from the input stream and breaks it 
 * into Jack-language tokens, as specified by the Jack grammar.
 */

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

    /**
     * The constructor will create a List of all the tokens
     */
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
                    tokenList.add(new Token(jackCode.substring(cursor, nextSeparator), "INT_CONST", "integerConstant", line));
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
	}

    /**
     * Determines what type of comments are here, then removes comments
     */
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

    /**
     * Advances to the next token, making it the current token of the tokenizer
     */
    public void advance() {
        if (hasMoreTokens())  tk = tokenList.get(t);
        t++;
    }

    /**
     * Used by the CompilationEngine compileTerm, which requires us to look for 
     * the token ahead of the current token.  Could have also been done by just
     * saving the current token, but hey.
     */
    public String getNextToken() {
        return (t < tokenList.size()) ? tokenList.get(t).gettoken() : "";
    }
    
    /**
     * Resets the tokenizer, making the first token the current one
     */
    public void reset() {
        t = 0;
        tk = tokenList.get(t);
    }

    /**
     * Does the tokenizer have more tokens?
     */
    public boolean hasMoreTokens() {
		return t < numTokens;
	}

    /**
     * What type of token is this:
     * KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, SRING_CONST
     */
    public String tokenType() {
        return tk.gettype();
    }

    /**
     * Returns the actual keyword, the actual token
     */
    public String keyword() {
        return tk.gettag().equals("keyword") ? tk.gettoken() : null;
    }

    /**
     * Returns the symbol as a char
     * I did not end up using this in the JackAnalyzer
     */
    public char symbol() {
        return tk.gettag().equals("symbol") ? tk.gettoken().charAt(0) : '\0';
    }

    /**
     * Returns the identifier
     * I did not end up using this, though I probably should have
     * I instead did the test in the ComilationEngine
     */
    public String identifier() {
        return tk.gettag().equals("identifier") ? tk.gettoken() : null;
    }

    /**
     * Returns the integer value of the current token
     * I again, did not use this in the JackAnalyzer
     */
    public int intVal() {
        return  tk.gettag().equals("integerConstant") ? Integer.parseInt(tk.gettoken()) : null;
    }

    /**
     * Returns the string value the current token
     * I again, did not use this in the JackAnalyzer
     */
    public String stringVal() {
        return tk.gettag().equals("stringConstant") ? tk.gettoken() : null;
    }

    /**
     * Returns a String which is the XML 
     * Badly written, but the toXML method is also used in the Token class
     * Here, it is just being called
     */
    public String toXML() {
        return tk.toXML();
    }

    /**
     * Returns the actual token.
     * Obviously, this is the same as the keyword method above, without the check
     * Leaving it for now, since both were used in the code
     */
    public String getToken() {
        return tk.gettoken();
    }

    /**
     * Returns the line number where the token is located 
     * Used for outputting any syntax errors
     */
    public int getLine() {
        return tk.getline();
    }

    /**
     * Returns the file that contains this token
     * Used for outputting the location of any syntax errors
     */
    public String getFileName() {
        return filename;
    }

    public String toString() {
        return "JackTokenizer for " + filename;
    }

}