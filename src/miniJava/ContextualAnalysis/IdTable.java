package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.ErrorReporter;

import java.util.HashMap;
import java.util.Stack;

public class IdTable {

    private ErrorReporter reporter;
    public Stack<HashMap<String, Declaration>> scopedIdTable;

    public IdTable(ErrorReporter reporter) {
        this.reporter = reporter;
    }

    public void openScope() {
        return;
    }

    public void enter(Declaration decl) {
        return;
    }

    public void closeScope() {
        return;
    }
}
