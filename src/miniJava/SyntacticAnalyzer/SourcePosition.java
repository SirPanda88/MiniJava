package miniJava.SyntacticAnalyzer;

public class SourcePosition {
    private int lineNumber;

    public SourcePosition (int i) {
        lineNumber = i;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
