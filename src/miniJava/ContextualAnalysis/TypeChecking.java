package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token;

public class TypeChecking implements Visitor<Object, Object> {

    private ErrorReporter reporter;

    public TypeChecking (Package ast, ErrorReporter reporter) {
        this.reporter = reporter;
        ast.visit(this, null);
    }

    // typeError is used to trace error when identification fails
    static class TypeError extends Error {
        private static final long serialVersionUID = 1L;
    }

    private void typeError(String e) throws TypeChecking.TypeError {
        reporter.reportError("Type error: " + e);
        throw new TypeChecking.TypeError();
    }







    // Package

    @Override
    public Object visitPackage(Package prog, Object arg) {
        return null;
    }


    // Declarations

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
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
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        return null;
    }


    // Expressions

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.expr.visit(this, null);
        if (expr.operator.kind == Token.TokenKind.NOT) {
            // logical negation can only be applied to booleans
            expr.typeDenoter = new BaseType(TypeKind.BOOLEAN, null);
            if (expr.expr.typeDenoter.typeKind != TypeKind.BOOLEAN) {
                typeError("'!' (NOT) applied to non boolean expression");
            }
        } else if (expr.operator.kind == Token.TokenKind.MINUS) {
            // arithmetic negation can only be applied to integers
            expr.typeDenoter = new BaseType(TypeKind.INT, null);
            if (expr.expr.typeDenoter.typeKind != TypeKind.INT) {
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
        if (!expr.left.typeDenoter.sameType(expr.right.typeDenoter)) {
            typeError("Unequal types on either side of binary expression");
        }
        switch (expr.operator.kind) {

            // strictly numerical operators
                // relational operators return a boolean and only operate on integers
            case GREATER:
            case LESS:
            case LESSEQUAL:
            case GREATEREQUAL:
                expr.typeDenoter = new BaseType(TypeKind.BOOLEAN, null);
                if (expr.left.typeDenoter.typeKind != TypeKind.INT) {
                    typeError("Non integer used with relational integer operator");
                }
                break;
                // arithmetic operators return an int and only operate on ints
            case PLUS:
            case MINUS:
            case MULT:
            case DIV:
                expr.typeDenoter = new BaseType(TypeKind.INT, null);
                if (expr.left.typeDenoter.typeKind != TypeKind.INT) {
                    typeError("Non integer used with arithmetic integer operator");
                }
                break;

                // strictly logical operators return a boolean and only operate on booleans
            case AND:
            case OR:
                expr.typeDenoter = new BaseType(TypeKind.BOOLEAN, null);
                if (expr.left.typeDenoter.typeKind != TypeKind.BOOLEAN) {
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
        expr.typeDenoter = expr.ref.decl.type;
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.typeDenoter = ((ArrayType)(expr.ref.decl.type)).eltType;
        expr.ixExpr.visit(this, null);
        if (expr.ixExpr.typeDenoter.typeKind != TypeKind.INT) {
            typeError("Size expression is not of type int in index array expression");
        }
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        expr.typeDenoter = expr.functionRef.decl.type;
        if (((MethodDecl)(expr.functionRef.decl)).parameterDeclList.size() != expr.argList.size()) {
            typeError("Size of argument list does not match size of argument list of referenced method");
        }
        for (Expression argumentExpr : expr.argList) {

        }
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        switch (expr.lit.kind) {
            case NUM:
                expr.typeDenoter = new BaseType(TypeKind.INT, null);
                break;
            case TRUE:
            case FALSE:
                expr.typeDenoter = new BaseType(TypeKind.BOOLEAN, null);
                break;
            case NULL:
                expr.typeDenoter = new BaseType(TypeKind.NULL, null);
                break;
        }
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.typeDenoter = expr.classtype;
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.typeDenoter = new ArrayType(expr.eltType, null);
        expr.sizeExpr.visit(this, null);
        if (expr.sizeExpr.typeDenoter.typeKind != TypeKind.INT) {
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
