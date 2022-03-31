package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.ErrorReporter;

import java.util.HashMap;

public class IdTable {

    private ErrorReporter reporter;
    public ModifiedStack stack;
    public HashMap<String, Declaration> currentScope;

    public IdTable(ErrorReporter reporter) {
        this.reporter = reporter;
        stack = new ModifiedStack();
    }

    public void openScope() {
        currentScope = new HashMap<String, Declaration>();
        stack.push(currentScope);
    }

    public void enter(Declaration decl) {
        if (currentScope.containsKey(decl.name)) {
            throw new IllegalArgumentException("Duplicate declaration (name already declared in current scope)");
        }
        if (stack.topOfStack > 4) {
            for (int i = 2; i < stack.arrList.size(); i++) {
                if (stack.arrList.get(i).containsKey(decl.name)) {
                    throw new IllegalArgumentException("Declarations at level 4 or higher may not hide declarations at levels 3 or higher");
                }
            }
        }
        currentScope.put(decl.name, decl);
    }

    public void closeScope() {
        stack.pop();
    }

    public Declaration search(String s) {
        return stack.search(s);
    }

    public int scopeLevel(String s) {
        return stack.scopeLevel(s);
    }

    public Declaration searchClasses(String s) {
        return stack.searchClasses(s);
    }
}
