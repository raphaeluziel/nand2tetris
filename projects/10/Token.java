import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;


class Token {
    private final String token;
    private final String type;
    private final String tag;

    public Token(String tok, String typ, String tg) {
        type = typ;
        tag = tg;

        if      (tok.equals("<"))   token = "&lt;";
        else if (tok.equals(">"))   token = "&gt;";
        else if (tok.equals("\""))  token = "&quot;";
        else if (tok.equals("&") )  token = "&amp;";
        else token = tok;
    }

    public String toXML() {
        return "<" + tag + "> " + token + " </" + tag + ">";
    }

    public String toString() {
        return "[Token: '" + token + "', type: " + type + ", tag:" + tag + "]";
    }

    public String gettoken()     { return token; }
    public String gettype()      { return type;  }
    public String gettag()       { return tag;   }
}



class KeyWordsAndSymbols {

    private static final String[] keywordArray = { "class", "constructor", 
                                                   "function", "method", "field",
                                                   "static", "var", "int", "char", 
                                                   "boolean", "void", "true", "false", 
                                                   "null", "this", "let", "do", "if", 
                                                   "else", "while", "return" };

    private static final Character[] symbolArray = { '{', '}', '(', ')', '[', ']',
                                                     '.', ',', ';', '+', '-', '*', '/',
                                                     '&', '|', '<', '>', '=', '~' };


    private static final List<String> keywords = Arrays.asList(keywordArray);
    private static final List<Character> symbols = Arrays.asList(symbolArray);

    public static List<String> getkeywords() { return keywords; }
    public static List<Character> getsymbols()  { return symbols;  }

}