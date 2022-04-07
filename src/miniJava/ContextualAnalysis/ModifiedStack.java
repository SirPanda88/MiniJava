package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;

import java.util.ArrayList;
import java.util.HashMap;

public class ModifiedStack {
    public ArrayList<HashMap<String, Declaration>> arrList;
    public int topOfStack;

    public ModifiedStack () {
        arrList = new ArrayList<HashMap<String, Declaration>>();
        topOfStack = 0;
    }

    public boolean isEmpty() {
        return arrList.size() == 0;
    }

    public HashMap<String, Declaration> peek() {
        if (topOfStack == 0) {
            return null;
        }
        return arrList.get(topOfStack - 1);
    }

    public HashMap<String, Declaration> pop() {
        HashMap<String, Declaration> result = arrList.get(topOfStack-1);
        arrList.remove(topOfStack-1);
        topOfStack--;
        return result;
    }

    public void push(HashMap<String, Declaration> map) {
        arrList.add(topOfStack, map);
        topOfStack++;
    }

    // clears all entries on the stack from index inclusive to the top of the stack
    public void clearUntilTop(int index) {
        if (topOfStack > index) {
            arrList.subList(index, topOfStack).clear();
        }
    }

    public boolean contains(String s) {
        for (HashMap<String, Declaration> map : arrList) {
            if (map.containsKey(s)) {
                return true;
            }
        }
        return false;
    }

    // returns the declaration in a hashmap containing desired string searching from top to bottom
    // returns null if desired string is not found
    public Declaration search (String s) {
        for (int i = topOfStack - 1; i >=0; i--) {
            if (arrList.get(i).containsKey(s)) {
                return arrList.get(i).get(s);
            }
        }
        return null;
    }

    public int scopeLevel (String s) {
        for (int i = topOfStack - 1; i >=0; i--) {
            if (arrList.get(i).containsKey(s)) {
                return i;
            }
        }
        return -1;
    }

    public Declaration searchClasses (String s) {
            if (arrList.get(0).containsKey(s)) {
                return arrList.get(0).get(s);
            }
        return null;
    }
}
