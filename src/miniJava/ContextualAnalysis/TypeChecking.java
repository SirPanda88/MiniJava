package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
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

    private void typeError(String e, int line) throws TypeChecking.TypeError {
        reporter.reportError("*** line " + line + ": Type error - " + e);
    }


// TODO: put visitpackage inside of try catch

    // Check type equality while catching any possible errors
    public void typeCheck() {
        try {
            ast.visit(this, null);
        }
        catch (Exception e) {
            System.out.println("Should not encounter exceptions in type checking. Exception message: " + e.getMessage());
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

        // visit all member, no types to check in FieldDecls so just visit all MethodDecls
        for(MethodDecl md: cd.methodDeclList) {
            md.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        for (Statement st : md.statementList) {
            st.visit(this, null);
        }

        if (md.type.typeKind == TypeKind.VOID) {
            for (int i = 0; i < md.statementList.size(); i++) {
                if (md.statementList.get(i) instanceof ReturnStmt) {
                    if (( (ReturnStmt) (md.statementList.get(i)) ).returnExpr != null) {
                        typeError("Return expression not allowed for method of type void");
                    }
                }
            }
        } else {
            for (int i = 0; i < md.statementList.size(); i++) {
                if (md.statementList.get(i) instanceof ReturnStmt) {
                    if ( md.type.sameType( ( ( (ReturnStmt) (md.statementList.get(i)) ).returnExpr.typeAttribute ) ) ) {
                        typeError("Return expression not the same type as method");
                    }
                }
            }
            if ( !(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt) ) {
                typeError("Last statement in method not a return statement");
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
            typeError("Expression does not match type of variable declaration");
        }
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.val.visit(this, null);
        if ( !( stmt.ref.decl.type.sameType(stmt.val.typeAttribute) ) ) {
            typeError("Assigned expression does not match type of variable");
        }
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ix.visit(this, null);
        if (stmt.ix.typeAttribute.typeKind != TypeKind.INT) {
            typeError("Size expression is not of type int in indexed array assignment statement");
        }
        stmt.exp.visit(this, null);
        if ( !( ( (ArrayType) (stmt.ref.decl.type) ).eltType.sameType(stmt.exp.typeAttribute) ) ) {
            typeError("Assigned expression does not match element type of array");
        }
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        if (( (MethodDecl) (stmt.methodRef.decl) ).parameterDeclList.size() != stmt.argList.size()) {
            typeError("Size of argument list does not match size of argument list of referenced method");
        }

        // compare referenced method parameter types to passed in parameters
        for (int i = 0; i < ( (MethodDecl) (stmt.methodRef.decl) ).parameterDeclList.size(); i++) {
            TypeDenoter paramType = ( (MethodDecl) (stmt.methodRef.decl) ).parameterDeclList.get(i).type;
            if (!paramType.sameType(stmt.argList.get(i).typeAttribute)) {
                typeError("CallExpr has different argument type than referenced method");
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
        if (stmt.cond.typeAttribute.typeKind != TypeKind.BOOLEAN) {
            typeError("Condition of if stmt is not of type boolean");
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
        if (stmt.cond.typeAttribute.typeKind != TypeKind.BOOLEAN) {
            typeError("Condition of while statement is not of type boolean");
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
            if (expr.expr.typeAttribute.typeKind != TypeKind.BOOLEAN) {
                typeError("'!' (NOT) applied to non boolean expression");
            }
        } else if (expr.operator.kind == Token.TokenKind.MINUS) {
            // arithmetic negation can only be applied to integers
            expr.typeAttribute = new BaseType(TypeKind.INT, null);
            if (expr.expr.typeAttribute.typeKind != TypeKind.INT) {
                typeError("'-' (MINUS) applied to non integer expression");
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
            typeError("Unequal types on either side of binary expression");
        }
        switch (expr.operator.kind) {

            // strictly numerical operators
                // relational operators return a boolean and only operate on integers
            case GREATER:
            case LESS:
            case LESSEQUAL:
            case GREATEREQUAL:
                expr.typeAttribute = new BaseType(TypeKind.BOOLEAN, null);
                if (expr.left.typeAttribute.typeKind != TypeKind.INT) {
                    typeError("Non integer used with relational integer operator");
                }
                break;
                // arithmetic operators return an int and only operate on ints
            case PLUS:
            case MINUS:
            case MULT:
            case DIV:
                expr.typeAttribute = new BaseType(TypeKind.INT, null);
                if (expr.left.typeAttribute.typeKind != TypeKind.INT) {
                    typeError("Non integer used with arithmetic integer operator");
                }
                break;

                // strictly logical operators return a boolean and only operate on booleans
            case AND:
            case OR:
                expr.typeAttribute = new BaseType(TypeKind.BOOLEAN, null);
                if (expr.left.typeAttribute.typeKind != TypeKind.BOOLEAN) {
                    typeError("Non boolean used with logical integer operator");
                }
                break;

                // equality operators can operate on all types, type equality between sides checked above
            case EQUALS:
            case NOTEQUAL:
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
        expr.typeAttribute = ((ArrayType)(expr.ref.decl.type)).eltType;
        expr.ixExpr.visit(this, null);
        if (expr.ixExpr.typeAttribute.typeKind != TypeKind.INT) {
            typeError("Size expression is not of type int in indexed array expression");
        }
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        expr.typeAttribute = expr.functionRef.decl.type;
        if (((MethodDecl)(expr.functionRef.decl)).parameterDeclList.size() != expr.argList.size()) {
            typeError("Size of argument list does not match size of argument list of referenced method");
        }

        // compare referenced method parameter types to passed in parameters
        for (int i = 0; i < ((MethodDecl)(expr.functionRef.decl)).parameterDeclList.size(); i++) {
            TypeDenoter paramType = ((MethodDecl)(expr.functionRef.decl)).parameterDeclList.get(i).type;
            if (!paramType.sameType(expr.argList.get(i).typeAttribute)) {
                typeError("CallExpr has different argument type than referenced method");
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
        if (expr.sizeExpr.typeAttribute.typeKind != TypeKind.INT) {
            typeError("Size expression is not of type int in new array expression");
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
