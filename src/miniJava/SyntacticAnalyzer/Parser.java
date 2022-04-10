package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

public class Parser {

    private Scanner scanner;
    private ErrorReporter reporter;
    private Token token;
    private boolean trace = false;

    public Parser(Scanner scanner, ErrorReporter reporter) {
        this.scanner = scanner;
        this.reporter = reporter;
    }

    // SyntaxError is used to unwind parse stack when parse fails
    class SyntaxError extends Error {
        private static final long serialVersionUID = 1L;
    }

    // parse input while catching any possible errors
    public Package parse() {
        token = scanner.scan();
        try {
            return parseProgram();
        }
        catch (SyntaxError e) {
            return null;
        }
    }

    // Program ::= (ClassDeclaration)* eot
    private Package parseProgram() throws SyntaxError {
        int packageLineNum = scanner.getLineNumber();
        ClassDeclList cdl = new ClassDeclList();
        while (token.kind != Token.TokenKind.EOT) {
            ClassDecl cd = parseClassDeclaration();
            cdl.add(cd);
        }
        accept(Token.TokenKind.EOT);
        return new Package(cdl, new SourcePosition(packageLineNum));
    }

    // ClassDeclaration ::= class id { ( FieldDeclaration | MethodDeclaration )* }
    private ClassDecl parseClassDeclaration() throws SyntaxError {
        int classLineNum = scanner.getLineNumber();

        accept(Token.TokenKind.CLASS);

        // create classname variable to hold name info
        Token classname = new Token(Token.TokenKind.ID, token.spelling, token.sourcePosition);

        accept(Token.TokenKind.ID);
        accept(Token.TokenKind.OPENCURLY);

        // create FieldDeclLists and MethodDeclLists
        FieldDeclList fdl = new FieldDeclList();
        MethodDeclList mdl = new MethodDeclList();

        while (token.kind != Token.TokenKind.EOT && token.kind != Token.TokenKind.CLOSECURLY) {
            // FieldDeclaration ::= Visibility Access Type id ;
            // MethodDeclaration ::= Visibility Access ( Type | void ) id ( ParameterList? ) {Statement*}
            int memberLineNum = scanner.getLineNumber();

            boolean isPrivate = parseVisibility();
            boolean isStatic = parseAccess();
            TypeDenoter typeDenoter;
            String memberName;
            ParameterDeclList pdl = new ParameterDeclList();
            StatementList stl = new StatementList();

            if (token.kind == Token.TokenKind.VOID) {
                typeDenoter = new BaseType(TypeKind.VOID, new SourcePosition(memberLineNum));
                accept(token.kind);
                memberName = token.spelling;
                accept(Token.TokenKind.ID);
                accept(Token.TokenKind.OPENPAREN);
                if (token.kind != Token.TokenKind.CLOSEPAREN) {
                    pdl = parseParameterList();
                }
                accept(Token.TokenKind.CLOSEPAREN);
                accept(Token.TokenKind.OPENCURLY);
                while (token.kind != Token.TokenKind.EOT && token.kind != Token.TokenKind.CLOSECURLY) {
                    Statement stmtInList = parseStatement();
                    stl.add(stmtInList);
                }
                accept(Token.TokenKind.CLOSECURLY);
                mdl.add(new MethodDecl(new FieldDecl(isPrivate, isStatic, typeDenoter, memberName, new SourcePosition(memberLineNum)), pdl, stl, new SourcePosition(memberLineNum)));
            } else {
                typeDenoter = parseType();
                memberName = token.spelling;
                accept(Token.TokenKind.ID);
                if (token.kind == Token.TokenKind.SEMICOLON) {
                    accept(token.kind);
                    fdl.add(new FieldDecl(isPrivate, isStatic, typeDenoter, memberName, new SourcePosition(memberLineNum)));
                } else {
                    accept(Token.TokenKind.OPENPAREN);
                    if (token.kind != Token.TokenKind.CLOSEPAREN) {
                        pdl = parseParameterList();
                    }
                    accept(Token.TokenKind.CLOSEPAREN);
                    accept(Token.TokenKind.OPENCURLY);
                    while (token.kind != Token.TokenKind.EOT && token.kind != Token.TokenKind.CLOSECURLY) {
                        Statement stmtInList = parseStatement();
                        stl.add(stmtInList);
                    }
                    accept(Token.TokenKind.CLOSECURLY);
                    mdl.add(new MethodDecl(new FieldDecl(isPrivate, isStatic, typeDenoter, memberName, new SourcePosition(memberLineNum)), pdl, stl, new SourcePosition(memberLineNum)));
                }
            }
        }
        accept(Token.TokenKind.CLOSECURLY);
        return new ClassDecl(classname.spelling, fdl, mdl, new SourcePosition(classLineNum));
    }

    // Visibility ::= ( public | private )?
    // returns true if private
    private boolean parseVisibility() throws SyntaxError {
        if (token.kind == Token.TokenKind.PRIVATE) {
            accept(token.kind);
            return true;
        }
        if (token.kind == Token.TokenKind.PUBLIC) {
            accept(token.kind);
        }
        return false;
    }

    // Access ::= static ?
    // returns true if static
    private boolean parseAccess() throws SyntaxError {
        if (token.kind == Token.TokenKind.STATIC) {
            accept(token.kind);
            return true;
        }
        return false;
    }

    // Type ::= int | boolean | id | ( int | id ) []
    private TypeDenoter parseType() throws SyntaxError {
        int typeLineNum = scanner.getLineNumber();
        switch (token.kind) {
            case INT:
            case ID:
                if (token.kind == Token.TokenKind.INT) {
                    TypeKind typeKind = TypeKind.INT;
                    accept(token.kind);
                    if (token.kind == Token.TokenKind.OPENBRACKET) {
                        accept(Token.TokenKind.OPENBRACKET);
                        accept(Token.TokenKind.CLOSEBRACKET);
                        return new ArrayType(new BaseType(typeKind, new SourcePosition(typeLineNum)), new SourcePosition(typeLineNum));
                    } else {
                        return new BaseType(typeKind, new SourcePosition(typeLineNum));
                    }
                } else if (token.kind == Token.TokenKind.ID) {
                    Token idToken = token;
                    accept(token.kind);
                    if (token.kind == Token.TokenKind.OPENBRACKET) {
                        accept(Token.TokenKind.OPENBRACKET);
                        accept(Token.TokenKind.CLOSEBRACKET);
                        return new ArrayType(new ClassType(new Identifier(idToken), new SourcePosition(typeLineNum)), new SourcePosition(typeLineNum));
                    } else {
                        return new ClassType(new Identifier(idToken), new SourcePosition(typeLineNum));
                    }
                }
            case BOOLEAN:
                accept(token.kind);
                return new BaseType(TypeKind.BOOLEAN, new SourcePosition(typeLineNum));
            case VOID:
                accept(token.kind);
                return new BaseType(TypeKind.VOID, new SourcePosition(typeLineNum));
            default:
                parseError("invalid type");
                return new BaseType(TypeKind.ERROR, new SourcePosition(typeLineNum));
        }
    }

    // ParameterList ::= Type id ( , Type id )*
    private ParameterDeclList parseParameterList() throws SyntaxError {
        int parameterLineNum = scanner.getLineNumber();
        ParameterDeclList pdl = new ParameterDeclList();
        TypeDenoter typeDenoter = parseType();
        String paramName = token.spelling;
        accept(Token.TokenKind.ID);
        pdl.add(new ParameterDecl(typeDenoter, paramName, new SourcePosition(parameterLineNum)));
        while (token.kind == Token.TokenKind.COMMA) {
            accept(Token.TokenKind.COMMA);
            parameterLineNum = scanner.getLineNumber();
            typeDenoter = parseType();
            paramName = token.spelling;
            accept(Token.TokenKind.ID);
            pdl.add(new ParameterDecl(typeDenoter, paramName, new SourcePosition(parameterLineNum)));
        }
        return pdl;
    }

    // ArgumentList ::= Expression ( , Expression )*
    private ExprList parseArgumentList() throws SyntaxError {
        ExprList exprList = new ExprList();
        Expression expression = parseExpression();
        exprList.add(expression);
        while (token.kind == Token.TokenKind.COMMA) {
            accept(Token.TokenKind.COMMA);
            expression = parseExpression();
            exprList.add(expression);
        }
        return exprList;
    }

    // Reference ::= id | this | Reference . id
    private Reference parseReference() throws SyntaxError {
        int refLineNum = scanner.getLineNumber();
        QualRef qualRef;
        BaseRef baseRef = null;
        switch (token.kind) {
            case ID:
                Identifier identifier = new Identifier(token);
                baseRef = new IdRef(identifier, new SourcePosition(refLineNum));
                accept(token.kind);
                break;
            case THIS:
                baseRef = new ThisRef(new SourcePosition(refLineNum));
                accept(token.kind);
                break;
            default:
                parseError("invalid reference");
        }
        if (token.kind == Token.TokenKind.DOT) {
            accept(Token.TokenKind.DOT);
            qualRef = new QualRef(baseRef, new Identifier(token), new SourcePosition(refLineNum));
            accept(Token.TokenKind.ID);
        } else {
            return baseRef;
        }
        while(token.kind == Token.TokenKind.DOT) {
            accept(Token.TokenKind.DOT);
            qualRef = new QualRef(qualRef, new Identifier(token), new SourcePosition(refLineNum));
            accept(Token.TokenKind.ID);
        }
        return qualRef;
    }

    /* Statement ::=
        { Statement* }
        | Type id = Expression ;
        | Reference = Expression ;
        | Reference [ Expression ] = Expression ;
        | Reference ( ArgumentList? ) ;
        | return Expression? ;
        | if ( Expression ) Statement (else Statement)?
        | while ( Expression ) Statement
    */
    private Statement parseStatement() throws SyntaxError {
        int stmtLineNum = scanner.getLineNumber();
        Expression expr = null;
        Expression expr1 = null;
        Statement stmt = null;
        Statement stmt1 = null;
        TypeDenoter typeDenoter = null;
        String idName = null;
        Reference reference = null;
        ExprList exprList = new ExprList();
        BaseRef baseRef = null;
        QualRef qualRef = null;
        switch (token.kind) {
            case RETURN:
                accept(token.kind);
                if (token.kind != Token.TokenKind.SEMICOLON) {
                    expr = parseExpression();
                    accept(Token.TokenKind.SEMICOLON);
                    return new ReturnStmt(expr, new SourcePosition(stmtLineNum));
                }
                accept(Token.TokenKind.SEMICOLON);
                return new ReturnStmt(null, new SourcePosition(stmtLineNum));
            case IF:
                accept(token.kind);
                accept(Token.TokenKind.OPENPAREN);
                expr = parseExpression();
                accept(Token.TokenKind.CLOSEPAREN);
                stmt = parseStatement();
                if (token.kind == Token.TokenKind.ELSE) {
                    accept(token.kind);
                    stmt1 = parseStatement();
                    return new IfStmt(expr, stmt, stmt1, new SourcePosition(stmtLineNum));
                }
                return new IfStmt(expr, stmt, new SourcePosition(stmtLineNum));
            case WHILE:
                accept(token.kind);
                accept(Token.TokenKind.OPENPAREN);
                expr = parseExpression();
                accept(Token.TokenKind.CLOSEPAREN);
                stmt = parseStatement();
                return new WhileStmt(expr, stmt, new SourcePosition(stmtLineNum));
            case OPENCURLY:
                accept(token.kind);
                StatementList stl = new StatementList();
                while (token.kind != Token.TokenKind.EOT && token.kind != Token.TokenKind.CLOSECURLY) {
                    stl.add(parseStatement());
                }
                accept(Token.TokenKind.CLOSECURLY);
                return new BlockStmt(stl, new SourcePosition(stmtLineNum));

                // we know the next nonterminal is Type
            case BOOLEAN:
            case INT:
                typeDenoter = parseType();
                idName = token.spelling;
                accept(Token.TokenKind.ID);
                accept(Token.TokenKind.ASSIGNMENT);
                expr = parseExpression();
                accept(Token.TokenKind.SEMICOLON);
                return new VarDeclStmt(new VarDecl(typeDenoter, idName, new SourcePosition(stmtLineNum)), expr, new SourcePosition(stmtLineNum));

                // we know the next nonterminal is Reference
            case THIS:
                reference = parseReference();
                switch (token.kind) {
                    case ASSIGNMENT:
                        accept(token.kind);
                        expr = parseExpression();
                        accept(Token.TokenKind.SEMICOLON);
                        return new AssignStmt(reference, expr, new SourcePosition(stmtLineNum));
                    case OPENBRACKET:
                        accept(token.kind);
                        expr = parseExpression();
                        accept(Token.TokenKind.CLOSEBRACKET);
                        accept(Token.TokenKind.ASSIGNMENT);
                        expr1 = parseExpression();
                        accept(Token.TokenKind.SEMICOLON);
                        return new IxAssignStmt(reference, expr, expr1, new SourcePosition(stmtLineNum));
                    case OPENPAREN:
                        accept(token.kind);
                        if (token.kind != Token.TokenKind.CLOSEPAREN) {
                            exprList = parseArgumentList();
                        }
                        accept(Token.TokenKind.CLOSEPAREN);
                        accept(Token.TokenKind.SEMICOLON);
                        return new CallStmt(reference, exprList, new SourcePosition(stmtLineNum));
                    default:
                        parseError("expected one of the following: " +
                                "'=', '[', '(' after reference (this) within statement," +
                                " but found '" + token.kind + "'");
                }
            case ID: // check rest of code
                typeDenoter = new ClassType(new Identifier(token), new SourcePosition(stmtLineNum));
                idName = token.spelling;
                accept(token.kind);
                switch(token.kind) {
                    case OPENBRACKET:
                        accept(token.kind);
                        if (token.kind == Token.TokenKind.CLOSEBRACKET) {
                            // we know it is Type id = Expression ; where type = id[], we are moving through the brackets
                            // so we can use the logic in the below case id
                            accept(token.kind);
                            typeDenoter = new ArrayType(typeDenoter, new SourcePosition(stmtLineNum));
                        } else {
                            // we know it is Reference [ Expression ] = Expression ;
                            expr = parseExpression();
                            accept(Token.TokenKind.CLOSEBRACKET);
                            accept(Token.TokenKind.ASSIGNMENT);
                            expr1 = parseExpression();
                            accept(Token.TokenKind.SEMICOLON);
                            return new IxAssignStmt(new IdRef(new Identifier(new Token(Token.TokenKind.ID, idName, new SourcePosition(stmtLineNum))), new SourcePosition(stmtLineNum)),
                                    expr, expr1, new SourcePosition(stmtLineNum));
                        }
                    case ID:
                        // we know it is Type ID = Expression ;
                        idName = token.spelling;
                        accept(Token.TokenKind.ID);
                        accept(Token.TokenKind.ASSIGNMENT);
                         expr = parseExpression();
                        accept(Token.TokenKind.SEMICOLON);
                        return new VarDeclStmt(new VarDecl(typeDenoter, idName, new SourcePosition(stmtLineNum)), expr, new SourcePosition(stmtLineNum));
                    case DOT:
                        // we know it is one of the three references
                        Identifier identifier = new Identifier(new Token(Token.TokenKind.ID, idName, new SourcePosition(stmtLineNum)));
                        baseRef = new IdRef(identifier, new SourcePosition(stmtLineNum));
                        accept(Token.TokenKind.DOT);
                        qualRef = new QualRef(baseRef, new Identifier(token), new SourcePosition(stmtLineNum));
                        accept(Token.TokenKind.ID);

                        while(token.kind == Token.TokenKind.DOT) {
                            accept(Token.TokenKind.DOT);
                            qualRef = new QualRef(qualRef, new Identifier(token), new SourcePosition(stmtLineNum));
                            accept(Token.TokenKind.ID);
                        }
                        reference = qualRef;
                    default:
                        if (qualRef == null) {
                            reference = new IdRef(new Identifier(new Token(Token.TokenKind.ID, idName, new SourcePosition(stmtLineNum))), new SourcePosition(stmtLineNum));
                        }
                        switch (token.kind) {
                            case ASSIGNMENT:
                                accept(token.kind);
                                    expr = parseExpression();
                                accept(Token.TokenKind.SEMICOLON);
                                return new AssignStmt(reference, expr, new SourcePosition(stmtLineNum));
                            case OPENBRACKET:
                                accept(token.kind);
                                expr = parseExpression();
                                accept(Token.TokenKind.CLOSEBRACKET);
                                accept(Token.TokenKind.ASSIGNMENT);
                                expr1 = parseExpression();
                                accept(Token.TokenKind.SEMICOLON);
                                return new IxAssignStmt(reference, expr, expr1, new SourcePosition(stmtLineNum));
                            case OPENPAREN:
                                accept(token.kind);
                                if (token.kind != Token.TokenKind.CLOSEPAREN) {
                                    exprList = parseArgumentList();
                                }
                                accept(Token.TokenKind.CLOSEPAREN);
                                accept(Token.TokenKind.SEMICOLON);
                                return new CallStmt(reference, exprList, new SourcePosition(stmtLineNum));
                            default:
                                parseError("expected one of the following: " +
                                        "'=', '[', '(' after reference within statement," +
                                        " but found '" + token.kind + "'");
                                return null;
                        }

                }
            default:
                parseError("invalid statement");
                return null;
        }
    }

    /* Expression ::=
        Reference
        | Reference [ Expression ]
        | Reference ( ArgumentList? )
        | unop Expression
        | Expression binop Expression
        | ( Expression )
        | num | true | false
        | new ( id () | int [ Expression ] | id [ Expression ] )
     */

    /*
    disjunction ||
    conjunction &&
    equality ==, !=
    relational <=, <, >, >=
    additive +, -
    multiplicative *, /
    unary -, !
     */

    private Expression parseExpression() throws SyntaxError {
        int exprLineNum = scanner.getLineNumber();

        Expression expr = parseConjunctionExpr();
        while(token.kind == Token.TokenKind.OR) {
            Token oper = token;
            accept(Token.TokenKind.OR);
            Expression expr1 = parseConjunctionExpr();
            expr = new BinaryExpr(new Operator(oper), expr, expr1, new SourcePosition(exprLineNum));
        }
        return expr;
    }
    private Expression parseConjunctionExpr() throws SyntaxError {
        int exprLineNum = scanner.getLineNumber();

        Expression expr = parseEqualityExpr();
        while(token.kind == Token.TokenKind.AND) {
            Token oper = token;
            accept(token.kind);
            Expression expr1 = parseEqualityExpr();
            expr = new BinaryExpr(new Operator(oper), expr, expr1, new SourcePosition(exprLineNum));
        }
        return expr;
    }
    private Expression parseEqualityExpr() throws SyntaxError {
        int exprLineNum = scanner.getLineNumber();

        Expression expr = parseRelationalExpr();
        while(token.kind == Token.TokenKind.EQUALS || token.kind == Token.TokenKind.NOTEQUAL) {
            Token oper = token;
            accept(token.kind);
            Expression expr1 = parseRelationalExpr();
            expr = new BinaryExpr(new Operator(oper), expr, expr1, new SourcePosition(exprLineNum));
        }
        return expr;
    }
    private Expression parseRelationalExpr() throws SyntaxError {
        int exprLineNum = scanner.getLineNumber();

        Expression expr = parseAdditiveExpr();
        while(token.kind == Token.TokenKind.LESSEQUAL || token.kind == Token.TokenKind.LESS ||
                token.kind == Token.TokenKind.GREATER || token.kind == Token.TokenKind.GREATEREQUAL) {
            Token oper = token;
            accept(token.kind);
            Expression expr1 = parseAdditiveExpr();
            expr = new BinaryExpr(new Operator(oper), expr, expr1, new SourcePosition(exprLineNum));
        }
        return expr;
    }
    private Expression parseAdditiveExpr() throws SyntaxError {
        int exprLineNum = scanner.getLineNumber();

        Expression expr = parseMultiplicativeExpr();
        while(token.kind == Token.TokenKind.PLUS || token.kind == Token.TokenKind.MINUS) {
            Token oper = token;
            accept(token.kind);
            Expression expr1 = parseMultiplicativeExpr();
            expr = new BinaryExpr(new Operator(oper), expr, expr1, new SourcePosition(exprLineNum));
        }
        return expr;
    }
    private Expression parseMultiplicativeExpr() throws SyntaxError {
        int exprLineNum = scanner.getLineNumber();

        Expression expr = parseUnaryExpr();
        while(token.kind == Token.TokenKind.MULT || token.kind == Token.TokenKind.DIV) {
            Token oper = token;
            accept(token.kind);
            Expression expr1 = parseUnaryExpr();
            expr = new BinaryExpr(new Operator(oper), expr, expr1, new SourcePosition(exprLineNum));
        }
        return expr;
    }
    private Expression parseUnaryExpr() throws SyntaxError {
        int exprLineNum = scanner.getLineNumber();

        Expression expr;
        if (token.kind == Token.TokenKind.MINUS || token.kind == Token.TokenKind.NOT) {
            Token oper = token;
            accept(token.kind);
            return new UnaryExpr(new Operator(oper), parseUnaryExpr(), new SourcePosition(exprLineNum));
        } else {
            return parseBaseExpression();
        }
    }

    /* Expression ::=
    Reference
    | Reference [ Expression ]
    | Reference ( ArgumentList? )
    | unop Expression
    | Expression binop Expression
    | ( Expression )
    | num | true | false
    | new ( id () | int [ Expression ] | id [ Expression ] )
 */

    private Expression parseBaseExpression() throws SyntaxError {
        int exprLineNum = scanner.getLineNumber();

        Token literalToken;
        Expression expr;
        switch (token.kind) {
            case NULL:
                literalToken = token;
                accept(token.kind);
                return new LiteralExpr(new NullLiteral(literalToken), new SourcePosition(exprLineNum));
            case NUM:
                literalToken = token;
                accept(token.kind);
                return new LiteralExpr(new IntLiteral(literalToken), new SourcePosition(exprLineNum));
            case TRUE:
            case FALSE:
                literalToken = token;
                accept(token.kind);
                return new LiteralExpr(new BooleanLiteral(literalToken), new SourcePosition(exprLineNum));
            case NEW:
                accept(token.kind);
                switch(token.kind) {
                    case ID:
                        Token idToken = token;
                        accept(token.kind);
                        if (token.kind == Token.TokenKind.OPENPAREN) {
                            accept(token.kind);
                            accept(Token.TokenKind.CLOSEPAREN);
                            return new NewObjectExpr(new ClassType(new Identifier(idToken), new SourcePosition(exprLineNum)), new SourcePosition(exprLineNum));
                        } else if (token.kind == Token.TokenKind.OPENBRACKET) {
                            accept(token.kind);
                            expr = parseExpression();
                            accept(Token.TokenKind.CLOSEBRACKET);
                            return new NewArrayExpr(new ClassType(new Identifier(idToken), new SourcePosition(exprLineNum)), expr, new SourcePosition(exprLineNum));
                        } else {
                            parseError("invalid token after new id");
                        }
                    case INT:
                        accept(token.kind);
                        accept(Token.TokenKind.OPENBRACKET);
                        expr = parseExpression();
                        accept(Token.TokenKind.CLOSEBRACKET);
                        return new NewArrayExpr(new BaseType(TypeKind.INT, new SourcePosition(exprLineNum)), expr, new SourcePosition(exprLineNum));
                    default:
                        parseError("invalid token after new");
                }
            case OPENPAREN:
                accept(token.kind);
                expr = parseExpression();
                accept(Token.TokenKind.CLOSEPAREN);
                return expr;
            case ID:
            case THIS:
                Reference ref = parseReference();
                if (token.kind == Token.TokenKind.OPENBRACKET) {
                    accept(token.kind);
                    expr = parseExpression();
                    accept(Token.TokenKind.CLOSEBRACKET);
                    return new IxExpr(ref, expr, new SourcePosition(exprLineNum));
                } else if (token.kind == Token.TokenKind.OPENPAREN) {
                    accept(token.kind);
                    ExprList exprList = new ExprList();
                    if (token.kind != Token.TokenKind.CLOSEPAREN) {
                        exprList = parseArgumentList();
                    }
                    accept(Token.TokenKind.CLOSEPAREN);
                    return new CallExpr(ref, exprList, new SourcePosition(exprLineNum));
                }
                return new RefExpr(ref, new SourcePosition(exprLineNum));
            default:
                parseError("invalid expression");
                return null;
        }
    }

    /**
     * verify that current token in input matches expected token and advance to next token
     * param expectedToken
     * throws SyntaxError if match fails
     */
    private void accept(Token.TokenKind expectedTokenKind) throws SyntaxError {
        if (token.kind == expectedTokenKind) {
            if (trace)
                pTrace();
            token = scanner.scan();
        }
        else
            parseError("expecting '" + expectedTokenKind +
                    "' but found '" + token.kind + "'");
    }

    // show parse stack whenever terminal is accepted
    private void pTrace() {
        StackTraceElement [] stl = Thread.currentThread().getStackTrace();
        for (int i = stl.length - 1; i > 0 ; i--) {
            if(stl[i].toString().contains("parse"))
                System.out.println(stl[i]);
        }
        System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
        System.out.println();
    }

    private void parseError(String e) throws SyntaxError {
        reporter.reportError("Parse error: " + e);
        throw new SyntaxError();
    }
}
