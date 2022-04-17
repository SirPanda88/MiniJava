package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class TypeChecking implements Visitor<Object, Object> {

    private ErrorReporter reporter;
    private AST ast;

    public TypeChecking (AST ast, ErrorReporter reporter) {
        this.reporter = reporter;
        this.ast = ast;
    }

    // typeError is used to trace error when identification fails
    static class TypeError extends Error {
        private static final long serialVersionUID = 1L;
    }

    private void typeError(String e, SourcePosition sp) throws TypeChecking.TypeError {
        reporter.reportError("*** line " + sp.getLineNumber() + ": Type error - " + e);
//        throw new TypeError();
    }


    // Check type equality while catching any possible errors
    public void typeCheck() {
        try {
            ast.visit(this, null);
        }
        catch (TypeChecking.TypeError e) {

        }
        catch (Exception e) {
            reporter.reportError("Should not encounter exceptions in type checking. Exception message: " + e.getMessage());
        }
    }


    // Package

    @Override
    public Object visitPackage(Package prog, Object arg) {
        for (ClassDecl cd : prog.classDeclList) {
            cd.visit(this, null);
        }
        return null;
    }


    // Declarations

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {

        // visit all members, no types to check in FieldDecls so just visit all MethodDecls
        for(MethodDecl md: cd.methodDeclList) {
            md.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        return null;
    }

    // TODO: fix pass 337 (return this)
    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        for (Statement st : md.statementList) {
            st.visit(this, null);
        }

        if (md.type.typeKind == TypeKind.VOID) {
            for (int i = 0; i < md.statementList.size(); i++) {
                if (md.statementList.get(i) instanceof ReturnStmt) {
                    if (( (ReturnStmt) (md.statementList.get(i)) ).returnExpr != null) {
                        typeError("Return expression not allowed for method of type void", md.statementList.get(i).posn);
                    }
                }
            }
        } else {
            for (int i = 0; i < md.statementList.size(); i++) {
                if (md.statementList.get(i) instanceof ReturnStmt) {
                    if (((ReturnStmt) md.statementList.get(i)).returnExpr == null) {
                        typeError("Missing return expression in non void method", md.statementList.get(i).posn);
                        continue;
                    }
                    if ( ! (md.type.sameType( ( ( (ReturnStmt) (md.statementList.get(i)) ).returnExpr.typeAttribute ) ) ) ) {
                        typeError("Return expression not the same type as non void method", md.statementList.get(i).posn);
                    }
                }
            }
            if (md.statementList.size() == 0) {
                typeError("Missing return expression in non void method", md.posn);
                return null;
            }
            if ( !(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt) ) {
                typeError("Last statement in method not a return statement", md.posn);
            }
        }
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        return null;
    }


    // Types

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        return null;
    }


    // Statements

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        for (Statement statement : stmt.sl) {
            statement.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        stmt.initExp.visit(this, null);
        if ( !( stmt.varDecl.type.sameType(stmt.initExp.typeAttribute) ) ) {
            typeError("Expression does not match type of variable declaration", stmt.posn);
        }
        return null;
    }

    // todo: find places where method refs and class refs are illegal for both typechecking and identification
    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        if (stmt.ref instanceof QualRef) {
            QualRef qualRef = (QualRef) stmt.ref;
            if (qualRef.id.decl.isArrayLength) {
                typeError("Assignment to array length is illegal", stmt.posn);
            }
        }
        stmt.val.visit(this, null);
        if ( !( stmt.ref.decl.type.sameType(stmt.val.typeAttribute) ) ) {
            typeError("Assigned expression does not match type of reference", stmt.posn);
        }
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ix.visit(this, null);
        if ( !(stmt.ix.typeAttribute.typeKind == TypeKind.INT || stmt.ix.typeAttribute.typeKind == TypeKind.ERROR) ) {
            typeError("Size expression is not of type int in indexed array assignment statement", stmt.posn);
        }

        stmt.exp.visit(this, null);

        if (stmt.ref.decl.type.typeKind != TypeKind.ARRAY) {
            typeError("Reference is not of type array", stmt.posn);
            return null;
        }

        if ( !( ( (ArrayType) (stmt.ref.decl.type) ).eltType.sameType(stmt.exp.typeAttribute) ) ) {
            typeError("Assigned expression does not match element type of array", stmt.posn);
        }
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {

        for (Expression expr : stmt.argList) {
            expr.visit(this, null);
        }

        if ( !(stmt.methodRef.decl instanceof MethodDecl) ) {
            typeError("Method call in statement must point to method declaration", stmt.posn);
            return null;
        }

        MethodDecl methodDecl = (MethodDecl) stmt.methodRef.decl;
        if ( methodDecl.parameterDeclList.size() != stmt.argList.size() ) {
            typeError("Size of argument list does not match size of argument list of referenced method", stmt.posn);
            return null;
        }

        // compare referenced method parameter types to passed in parameters
        for (int i = 0; i < methodDecl.parameterDeclList.size(); i++) {
            TypeDenoter paramType = methodDecl.parameterDeclList.get(i).type;
            if (!paramType.sameType(stmt.argList.get(i).typeAttribute)) {
                typeError("CallStmt has different argument type than referenced method", stmt.posn);
                return null;
            }
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        if ( !(stmt.cond.typeAttribute.typeKind == TypeKind.BOOLEAN || stmt.cond.typeAttribute.typeKind == TypeKind.ERROR) ) {
            typeError("Condition of if stmt is not of type boolean", stmt.posn);
        }
        stmt.thenStmt.visit(this, null);
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        if ( !(stmt.cond.typeAttribute.typeKind == TypeKind.BOOLEAN || stmt.cond.typeAttribute.typeKind == TypeKind.ERROR) ) {
            typeError("Condition of while statement is not of type boolean", stmt.posn);
        }
        stmt.body.visit(this, null);
        return null;
    }


    // Expressions

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.expr.visit(this, null);
        if (expr.operator.kind == Token.TokenKind.NOT) {
            // logical negation can only be applied to booleans
            expr.typeAttribute = new BaseType(TypeKind.BOOLEAN, null);
            if ( !(expr.expr.typeAttribute.typeKind == TypeKind.BOOLEAN || expr.expr.typeAttribute.typeKind == TypeKind.ERROR) ) {
                typeError("'!' (NOT) applied to non boolean expression", expr.posn);
                expr.typeAttribute = new BaseType(TypeKind.ERROR, null);
            }
        } else if (expr.operator.kind == Token.TokenKind.MINUS) {
            // arithmetic negation can only be applied to integers
            expr.typeAttribute = new BaseType(TypeKind.INT, null);
            if ( !(expr.expr.typeAttribute.typeKind == TypeKind.INT || expr.expr.typeAttribute.typeKind == TypeKind.ERROR) ) {
                typeError("'-' (MINUS) applied to non integer expression", expr.posn);
                expr.typeAttribute = new BaseType(TypeKind.ERROR, null);
            }
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.operator.visit(this, null);
        expr.left.visit(this, null);
        expr.right.visit(this, null);
        if (!expr.left.typeAttribute.sameType(expr.right.typeAttribute)) {
            typeError("Unequal types on either side of binary expression", expr.posn);
            expr.typeAttribute = new BaseType(TypeKind.ERROR, null);
            return null;
        }

        switch (expr.operator.kind) {

            // strictly numerical operators
                // relational operators return a boolean and only operate on integers
            case GREATER:
            case LESS:
            case LESSEQUAL:
            case GREATEREQUAL:
                expr.typeAttribute = new BaseType(TypeKind.BOOLEAN, null);
                if ( !(expr.left.typeAttribute.typeKind == TypeKind.INT || expr.left.typeAttribute.typeKind == TypeKind.ERROR) ) {
                    typeError("Non integer used with relational integer operator", expr.posn);
                }
                break;
                // arithmetic operators return an int and only operate on ints
            case PLUS:
            case MINUS:
            case MULT:
            case DIV:
                expr.typeAttribute = new BaseType(TypeKind.INT, null);
                if ( !(expr.left.typeAttribute.typeKind == TypeKind.INT || expr.left.typeAttribute.typeKind == TypeKind.ERROR) ) {
                    typeError("Non integer used with arithmetic integer operator", expr.posn);
                }
                break;

                // strictly logical operators return a boolean and only operate on booleans
            case AND:
            case OR:
                expr.typeAttribute = new BaseType(TypeKind.BOOLEAN, null);
                if ( !(expr.left.typeAttribute.typeKind == TypeKind.BOOLEAN || expr.left.typeAttribute.typeKind == TypeKind.ERROR)) {
                    typeError("Non boolean used with logical integer operator", expr.posn);
                }
                break;

                // equality operators can operate on all types, type equality between sides checked above
            case EQUALS:
            case NOTEQUAL:
                expr.typeAttribute = new BaseType(TypeKind.BOOLEAN, null);
                break;

        }
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.typeAttribute = expr.ref.decl.type;
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        if ( !(expr.ref.decl.type.typeKind == TypeKind.ARRAY || expr.ref.decl.type.typeKind == TypeKind.ERROR)) {
            typeError("Reference is not of type array", expr.posn);
            expr.typeAttribute = new BaseType(TypeKind.ERROR, null);
        } else {
            expr.typeAttribute = ( (ArrayType)(expr.ref.decl.type) ).eltType;
        }
        expr.ixExpr.visit(this, null);
        if ( !(expr.ixExpr.typeAttribute.typeKind == TypeKind.INT || expr.ixExpr.typeAttribute.typeKind == TypeKind.ERROR) ){
            typeError("Size expression is not of type int in indexed array expression", expr.posn);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {

        for (Expression argument : expr.argList) {
            argument.visit(this, null);
        }

        if (!(expr.functionRef.decl instanceof MethodDecl)) {
            typeError("Method call in expression must point to method declaration", expr.functionRef.posn);
            expr.typeAttribute = new BaseType(TypeKind.ERROR, null);
            return null;
        }

        MethodDecl methodDecl = (MethodDecl) expr.functionRef.decl;

        if ( methodDecl.parameterDeclList.size() != expr.argList.size() ) {
            typeError("Size of argument list does not match size of argument list of referenced method", expr.posn);
            expr.typeAttribute = new BaseType(TypeKind.ERROR, null);
            return null;
        }

        // compare referenced method parameter types to passed in parameters
        for (int i = 0; i < methodDecl.parameterDeclList.size(); i++) {
            TypeDenoter paramType = methodDecl.parameterDeclList.get(i).type;
            if ( !(paramType.sameType(expr.argList.get(i).typeAttribute)) ) {
                typeError("CallExpr has different argument type than referenced method", expr.posn);
                expr.typeAttribute = new BaseType(TypeKind.ERROR, null);
                return null;
            }
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        switch (expr.lit.kind) {
            case NUM:
                expr.typeAttribute = new BaseType(TypeKind.INT, null);
                break;
            case TRUE:
            case FALSE:
                expr.typeAttribute = new BaseType(TypeKind.BOOLEAN, null);
                break;
            case NULL:
                expr.typeAttribute = new BaseType(TypeKind.NULL, null);
                break;
        }
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.typeAttribute = expr.classtype;
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.typeAttribute = new ArrayType(expr.eltType, null);
        expr.sizeExpr.visit(this, null);
        if ( !(expr.sizeExpr.typeAttribute.typeKind == TypeKind.INT || expr.sizeExpr.typeAttribute.typeKind == TypeKind.ERROR) ){
            typeError("Size expression is not of type int in new array expression", expr.posn);
        }
        return null;
    }


    // References

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        return null;
    }


    // Terminals

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
        return null;
    }
}
