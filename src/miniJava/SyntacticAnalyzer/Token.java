package miniJava.SyntacticAnalyzer;

public class Token {
    public TokenKind kind;
    public String spelling;
    public SourcePosition sourcePosition;

    public Token (TokenKind kind, String spelling, SourcePosition posn) {
        this.kind = kind;
        this.spelling = spelling;
    }


    public enum TokenKind {
        EOT,
        CLASS,
        ID,
        VOID,
        PUBLIC,
        PRIVATE,
        STATIC,
        INT,
        BOOLEAN,
        THIS,
        RETURN,
        IF,
        ELSE,
        WHILE,
        TRUE,
        FALSE,
        NEW,
        NUM,
        COMMA,
        DOT,
        OPENCURLY,
        CLOSECURLY,
        OPENPAREN,
        CLOSEPAREN,
        OPENBRACKET,
        CLOSEBRACKET,
        SEMICOLON,
        ASSIGNMENT,
        GREATER,
        LESS,
        EQUALS,
        LESSEQUAL,
        GREATEREQUAL,
        NOTEQUAL,
        AND,
        OR,
        NOT,
        PLUS,
        MINUS,
        MULT,
        DIV,
        ERROR,
        NULL;
    }
}
