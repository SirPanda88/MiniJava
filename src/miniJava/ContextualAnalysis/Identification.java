package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

public class Identification implements Visitor<Object, Object> {

    public IdTable table;
    private ErrorReporter reporter;

    public Identification(Package ast, ErrorReporter reporter) {
        this.reporter = reporter;
        table = new IdTable(reporter);
        ast.visit(this, null);
    }

    // Package
    public Object visitPackage(Package prog, Object obj) {
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
    public Object visitClassDecl(ClassDecl cd, Object obj) {
        currentClass = cd;

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

    public Object visitFieldDecl(FieldDecl fd, Object obj) {
        fd.type.visit(this, null);
        return null;
    }

    public Object visitMethodDecl(MethodDecl md, Object obj) {
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

}