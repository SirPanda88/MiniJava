package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.ErrorReporter;

import java.util.HashMap;

public class IdTable {

    private ErrorReporter reporter;
    public ModifiedStack scopedIdTable;
    public HashMap<String, Declaration> currentScope;

    public IdTable(ErrorReporter reporter) {
        this.reporter = reporter;
        scopedIdTable = new ModifiedStack();
    }

    public void openScope() {
        currentScope = new HashMap<String, Declaration>();
    }

    public void enter(Declaration decl) {
        if (currentScope.containsKey(decl.name)) {
            throw new IllegalArgumentException();
        }

        currentScope.put(decl.name, decl);
    }

    public void closeScope() {
        return;
    }
}
