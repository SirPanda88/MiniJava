package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;

import java.util.ArrayList;
import java.util.HashMap;

public class ModifiedStack {
    private ArrayList<HashMap<String, Declaration>> stack;
    private int topOfStack;

    public ModifiedStack () {
        stack = new ArrayList<HashMap<String, Declaration>>();
        topOfStack = 0;
    }

    public boolean isEmpty() {
        return stack.size() == 0;
    }

    public HashMap<String, Declaration> peek() {
        return stack.get(topOfStack);
    }

    public HashMap<String, Declaration> pop() {
        HashMap<String, Declaration> result = stack.get(topOfStack-1);
        stack.remove(topOfStack-1);
        topOfStack--;
        return result;
    }

    public void push(HashMap<String, Declaration> map) {
        stack.add(topOfStack, map);
        topOfStack++;
    }

    // clears all entries on the stack from index inclusive to the top of the stack
    public void clearUntilTop(int index) {
        if (topOfStack > index) {
            stack.subList(index, topOfStack).clear();
        }
    }

    public boolean contains(String s) {
        for (HashMap<String, Declaration> map : stack) {
            if (map.containsKey(s)) {
                return true;
            }
        }
        return false;
    }

    // returns the index of a hashmap containing desired string searching from top to bottom
    public int search (String s) {
        for (int i = topOfStack - 1; i >=0; i++) {
            if (stack.get(i).containsKey(s)) {
                return i;
            }
        }
        return -1;
    }
}
