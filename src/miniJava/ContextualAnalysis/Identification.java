package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

import java.util.Map;

public class Identification implements Visitor<Object, Object> {

    public IdTable table;
    private ErrorReporter reporter;
    private ClassDecl currentClass;
    private Map<String, Declaration> initializedClasses;

    public Identification(Package ast, ErrorReporter reporter) {
        this.reporter = reporter;
        table = new IdTable(reporter);
        ast.visit(this, null);
    }

    // identificationError is used to trace error when identification fails
    static class IdentificationError extends Error {
        private static final long serialVersionUID = 1L;
    }

    private void idError(String e) throws Identification.IdentificationError {
        reporter.reportError("Identification error: " + e);
        throw new IdentificationError();
    }

    // TODO: put visitPackage inside of try catch finally block
    // catch illegal argument exception thrown by idTable to find duplicate declarations
    // have each method throw IdentificationError



    // Package

    @Override
    public Object visitPackage(Package prog, Object arg) {
        table.openScope();

        // add all the classes to the table.
        for(ClassDecl cd: prog.classDeclList) {
            table.enter(cd);
        }

        //then visit classes
        for(ClassDecl cd: prog.classDeclList) {
            cd.visit(this, null);
        }

        table.closeScope();
        return null;
    }


    // Declarations

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        // set current class so this keyword works
        currentClass = cd;
        // add it to the map of initialized class names to decls to support static/ non static references
        initializedClasses.put(cd.name, cd);

        // add members so all fields and methods are visible
        table.openScope();
        for(FieldDecl fd: cd.fieldDeclList) {
            table.enter(fd);
        }
        for(MethodDecl md: cd.methodDeclList) {
            table.enter(md);
        }

        // visit all members
        for(FieldDecl fd: cd.fieldDeclList)
            fd.visit(this, null);
        for(MethodDecl md: cd.methodDeclList) {
            md.visit(this, null);
        }

        table.closeScope();
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        fd.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        md.type.visit(this, null);

        table.openScope();
        for (ParameterDecl pd : md.parameterDeclList) {
            pd.visit(this, null);
        }
        table.openScope();
        for (Statement st : md.statementList) {
            st.visit(this, null);
        }
        table.closeScope();
        table.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, null);
        table.enter(pd);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        table.enter(decl);
        decl.type.visit(this, null);
        return null;
    }


    // Types

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        Identifier classTypeName = type.className;
        Declaration originalDecl = table.search(classTypeName.spelling);
        if (originalDecl == null) {
            idError("Undeclared identifier after 'new' (not a class) in new object expr");
            return null;
        }
        if (table.scopeLevel(classTypeName.spelling) > 0) {
            idError("New called on non class identifier in new object expr");
        }
        classTypeName.decl = originalDecl;
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, null);
        return null;
    }


    // Statements

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        table.openScope();
        for (Statement individualStmt : stmt.sl) {
            individualStmt.visit(this, null);
        }
        table.closeScope();
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        stmt.initExp.visit(this, null);
        stmt.varDecl.visit(this, null);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.val.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.ix.visit(this, null);
        stmt.exp.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        stmt.methodRef.visit(this, null);
        for (Expression expr : stmt.argList) {
            expr.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        if (stmt.returnExpr != null)
            stmt.returnExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        table.openScope();
        if (stmt.thenStmt instanceof VarDeclStmt) {
            idError("solitary variable declaration in THEN branch of if statement");
        }
        stmt.thenStmt.visit(this, null);
        table.closeScope();
        if (stmt.elseStmt != null) {
            table.openScope();
            if (stmt.elseStmt instanceof VarDeclStmt) {
                idError("solitary variable declaration in ELSE branch of if statement");
            }
            stmt.elseStmt.visit(this, null);
            table.closeScope();
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        table.openScope();
        stmt.body.visit(this, null);
        table.closeScope();
        return null;
    }


    // Expressions

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.operator.visit(this, null);
        expr.expr.visit(this, null);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.operator.visit(this, null);
        expr.left.visit(this, null);
        expr.right.visit(this, null);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.visit(this, null);
        expr.ixExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        expr.functionRef.visit(this, null);
        for (Expression e: expr.argList) {
            e.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        Identifier newClassName = expr.classtype.className;
        Declaration originalDecl = table.search(newClassName.spelling);
        if (originalDecl == null) {
            idError("Undeclared identifier after 'new' (not a class) in new object expr");
            return null;
        }
        if (table.scopeLevel(newClassName.spelling) > 0) {
            idError("New called on non class identifier in new object expr");
        }
        newClassName.decl = originalDecl;
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        TypeKind arrType = expr.eltType.typeKind;
        if (arrType == TypeKind.CLASS) {
            Identifier arrTypeName = ((ClassType)(expr.eltType)).className;
            Declaration originalDecl = table.search(arrTypeName.spelling);
            if (originalDecl == null) {
                idError("Undeclared identifier after 'new' (not a class) in new array expr");
                return null;
            }
            if (table.scopeLevel(arrTypeName.spelling) > 0) {
                idError("New called on non class identifier in new array expr");
            }
            arrTypeName.decl = originalDecl;
        }
        expr.sizeExpr.visit(this, null);
        return null;
    }


    // References

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        ref.decl = currentClass;
        return null;
    }




    // fix idref and qref
    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        ref.id.visit(this, null);
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, null);
        ref.id.visit(this, ref.ref);
        return null;
    }


    // Terminals

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        Declaration originalDecl = table.search(id.spelling);
        if (originalDecl == null) {
            idError("Undeclared identifier");
            return null;
        }
        id.decl = originalDecl;
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