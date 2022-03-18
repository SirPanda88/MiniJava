package miniJava;

public class ErrorReporter {
    private int numErrors = 0;

    ErrorReporter() {
    }

    public boolean hasErrors() {
        return this.numErrors > 0;
    }

    public void reportError(String message) {
        System.out.println(message);
        this.numErrors++;
    }
}
