package miniJava.SyntacticAnalyzer;

import java.io.*;
import miniJava.ErrorReporter;

public class Scanner {

    public InputStream inputStream;
    private ErrorReporter reporter;

    private char currentChar;
    private StringBuilder currentSpelling;
    private boolean eot = false;
    private int lineNumber;

    public Scanner(InputStream inputStream, ErrorReporter reporter) {
        this.inputStream = inputStream;
        this.reporter = reporter;
        lineNumber = 1;
        // initialize scanner state
        readChar();
    }

    // scan next token ignoring whitespace and comments
    public Token scan() {
        // skip whitespace and comments
        while (!eot && (isWhiteSpace(currentChar) || currentChar == '/')) {
            if (isWhiteSpace(currentChar)) {
                if (currentChar == '\n' || currentChar == '\r') {
                    lineNumber++;
                }
                skipIt();
                continue;
            }

            // skip the first slash /
            skipIt();

            // skip /* ... */
            if (!eot && currentChar == '*') {
                skipIt();
                char temp = currentChar;
                if (currentChar == '\n' || currentChar == '\r') {
                    lineNumber++;
                }
                skipIt();
                while (!eot && (currentChar != '/' || temp != '*')) {
                    temp = currentChar;
                    if (currentChar == '\n' || currentChar == '\r') {
                        lineNumber++;
                    }
                    skipIt();
                }
                if (eot) {
                    scanError("Unterminated * comment in input");
                    return(new Token(Token.TokenKind.ERROR, "Unterminated * comment in input", new SourcePosition(lineNumber)));
                }
                skipIt();
                // skip //
            } else if (!eot && currentChar == '/') {
                while (!eot && (currentChar != '\n' && currentChar != '\r')) {
                    skipIt();
                }
                if (eot) {
                    scanError("Unterminated single line comment in input");
                    return(new Token(Token.TokenKind.ERROR, "Unterminated single line comment in input", new SourcePosition(lineNumber)));
                }
                // end of single line comment, increment line number
                lineNumber++;
                skipIt();
            } else {
                return(new Token(Token.TokenKind.DIV, "/", new SourcePosition(lineNumber)));
            }
        }

        // start of a token: collect spelling and identify token kind
        currentSpelling = new StringBuilder();
        Token.TokenKind kind = scanToken();
        String spelling = currentSpelling.toString();

        // return new token
        return new Token(kind, spelling, new SourcePosition(lineNumber));
    }

    public Token.TokenKind scanToken() {
        if (eot) {
            return (Token.TokenKind.EOT);
        }
        // scan Token
        switch (currentChar) {
            case '>':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return(Token.TokenKind.GREATEREQUAL);
                }
                return Token.TokenKind.GREATER;

            case '<':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return(Token.TokenKind.LESSEQUAL);
                }
                return Token.TokenKind.LESS;

            case '=':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return(Token.TokenKind.EQUALS);
                }
                return Token.TokenKind.ASSIGNMENT;

            case '!':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return(Token.TokenKind.NOTEQUAL);
                }
                return Token.TokenKind.NOT;

            case '&':
                takeIt();
                if (currentChar == '&') {
                    takeIt();
                    return(Token.TokenKind.AND);
                }
                scanError("Single ampersand in input");
                return Token.TokenKind.ERROR;

            case '|':
                takeIt();
                if (currentChar == '|') {
                    takeIt();
                    return(Token.TokenKind.OR);
                }
                scanError("Single or bar in input");
                return Token.TokenKind.ERROR;

            case '+':
                takeIt();
                return Token.TokenKind.PLUS;
            case '-':
                takeIt();
                return Token.TokenKind.MINUS;
            case '*':
                takeIt();
                return Token.TokenKind.MULT;
            case '{':
                takeIt();
                return Token.TokenKind.OPENCURLY;
            case '}':
                takeIt();
                return Token.TokenKind.CLOSECURLY;
            case '(':
                takeIt();
                return Token.TokenKind.OPENPAREN;
            case ')':
                takeIt();
                return Token.TokenKind.CLOSEPAREN;
            case '[':
                takeIt();
                return Token.TokenKind.OPENBRACKET;
            case ']':
                takeIt();
                return Token.TokenKind.CLOSEBRACKET;
            case ';':
                takeIt();
                return Token.TokenKind.SEMICOLON;
            case ',':
                takeIt();
                return Token.TokenKind.COMMA;
            case '.':
                takeIt();
                return Token.TokenKind.DOT;

            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                while (isDigit(currentChar)) {
                    takeIt();
                }
                return(Token.TokenKind.NUM);

            default:
                if (isLetter(currentChar)) {
                    takeIt();
                    while (!eot && (isLetter(currentChar) || isDigit(currentChar) || currentChar == '_')) {
                        takeIt();
                    }
                    switch (currentSpelling.toString()) {
                        case "class":
                            return Token.TokenKind.CLASS;
                        case "void":
                            return Token.TokenKind.VOID;
                        case "public":
                            return Token.TokenKind.PUBLIC;
                        case "private":
                            return Token.TokenKind.PRIVATE;
                        case "static":
                            return Token.TokenKind.STATIC;
                        case "int":
                            return Token.TokenKind.INT;
                        case "boolean":
                            return Token.TokenKind.BOOLEAN;
                        case "this":
                            return Token.TokenKind.THIS;
                        case "return":
                            return Token.TokenKind.RETURN;
                        case "if":
                            return Token.TokenKind.IF;
                        case "else":
                            return Token.TokenKind.ELSE;
                        case "while":
                            return Token.TokenKind.WHILE;
                        case "true":
                            return Token.TokenKind.TRUE;
                        case "false":
                            return Token.TokenKind.FALSE;
                        case "new":
                            return Token.TokenKind.NEW;
                        case "null":
                            return Token.TokenKind.NULL;
                        default:
                            return Token.TokenKind.ID;
                    }
                }
                scanError("Unrecognized character '" + currentChar + "' in input");
                return(Token.TokenKind.ERROR);
        }
    }

    private boolean isWhiteSpace(char c) {
        return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
    }
    private boolean isDigit(char c) {
        return (c >= '0') && (c <= '9');
    }
    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private void takeIt() {
        currentSpelling.append(currentChar);
        nextChar();
    }

    private void skipIt() {
        nextChar();
    }

    private void nextChar() {
        if (!eot) {
            readChar();
        }
    }

    private void readChar() {
        try {
            int c = inputStream.read();
            currentChar = (char) c;
            if (c == -1) {
                eot = true;
            }
        } catch (IOException e) {
            scanError("I/O Exception!");
            eot = true;
        }
    }

    private void scanError(String message) {
        reporter.reportError("Scan Error:  " + message);
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
