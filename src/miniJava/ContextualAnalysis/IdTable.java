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
            throw new IllegalArgumentException("Name already declared in current scope");
        }

        currentScope.put(decl.name, decl);
    }

    public void closeScope() {
        stack.pop();
    }
}
