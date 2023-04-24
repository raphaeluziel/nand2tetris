/******************************************************************************
 * 
 * Author: Raphael Uziel
 * Date: ???
 * 
******************************************************************************/

import java.util.*;

/**
 * ???
 */

class SymbolTable {

    private HashMap<String, Info> symbolTable;
    private int f, s, a, l;

    private class Info {
        private final String type;
        private final String kind;
        private final int n;

        public Info(String t, String k, int numb) {
            type = t;
            kind = k;
            n = numb;
        }

        public String toString() {
            return type + " " + kind + " " + n;  
        }
    }

    // Constructor
	public SymbolTable() {
        symbolTable = new HashMap<String, Info>();
        f = 0;
        s = 0;
        a = 0;
        l = 0;
    }

    public boolean has(String key) {
        return symbolTable.containsKey(key);
    }

    public void define(String nme, String typ, String knd) {
        int num = 0;
        switch (knd) {
            case "STATIC":
                num = s;
                s++;
                break;
            case "FIELD":
                num = f;
                f++;
                break;
            case "ARG":
                num = a;
                a++;
                break;
            case "VAR":
                num = l;
                l++;
        }
        Info inf = new Info(typ, knd, num);
        symbolTable.put(nme, inf);
    }

    public void reset() {
        symbolTable.clear();
        f = 0;
        s = 0;
        a = 0;
        l = 0;
    }

    public int varCount(String knd) {
        switch(knd) {
            case "STATIC":
                return s;
            case "FIELD":
                return f;
            case "ARG":
                return a;
            case "VAR":
                return l;
        }
        return 0;
    }

    public String kindOf(String nme) {
        Info i = symbolTable.get(nme);
        return (i != null) ? i.kind : "NONE";
    }

    public String typeOf(String nme) {
        return symbolTable.get(nme).type;
    }

    public int indexOf(String nme) {
        return symbolTable.get(nme).n;
    }

    public String toString() {
        String x = "";
        for (Map.Entry<String, Info> entry : symbolTable.entrySet()) {
            String key = entry.getKey();
            Info value = entry.getValue();
            x += "\n" + key + " " + value.type + " " + value.kind + " " + value.n;
        }
        return x;
    }

}