package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class Identification implements Visitor<Object, Object> {

    public IdTable table;
    private ErrorReporter reporter;
    private ClassDecl currentClass;
    private boolean withinStaticMethod;
    private AST ast;
    private String declaredVariable;

    public Identification(AST ast, ErrorReporter reporter) {
        this.reporter = reporter;
        this.table = new IdTable(reporter);
        this.ast = ast;
        this.withinStaticMethod = false;
        this.declaredVariable = null;

        //attempt to add predefined classes
//        Token classname = new Token(Token.TokenKind.ID, "System", null);
//        FieldDeclList fdl = new FieldDeclList();
//        MethodDeclList mdl = new MethodDeclList();
//
//        TypeDenoter td = new ClassType(new Identifier(classname), null);
//        fdl.add(new FieldDecl(false, true, td, out, new SourcePosition(memberLineNum)));

//        classname = new Token(Token.TokenKind.ID, "_PrintStream", null);
//        classname = new Token(Token.TokenKind.ID, "String", null);

//        ClassType _PrintStream = new ClassType(new Identifier(new Token(Token.TokenKind.ID, "_PrintStream", null)), null);
//        FieldDecl out = new FieldDecl(false, true, _PrintStream, "out", null);
//        ClassDecl system = new ClassDecl("System", new FieldDeclList(), new MethodDeclList(), null);

        FieldDeclList fieldDeclListSystem = new FieldDeclList();
        fieldDeclListSystem.add(new FieldDecl(false, true, new ClassType(new Identifier(new Token
                (Token.TokenKind.ID, "_PrintStream", null)), null), "out", null));
        ClassDecl system = new ClassDecl("System", fieldDeclListSystem, new MethodDeclList(), null);

        // toggle isStaticFieldAccess within the declaration of System.out
        system.fieldDeclList.get(0).isStaticFieldRef = true;

        MethodDeclList methodDeclListPrintSystem = new MethodDeclList();
        ParameterDeclList parameterDeclList = new ParameterDeclList();
        parameterDeclList.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));

        MethodDecl printLnMethodDecl = new MethodDecl(new FieldDecl(false, false, new BaseType
                (TypeKind.VOID, null), "println", null), parameterDeclList, new StatementList(), null);
        methodDeclListPrintSystem.add(printLnMethodDecl);
        printLnMethodDecl.isPrintLn = true;

        ClassDecl printStream = new ClassDecl("_PrintStream", new FieldDeclList(), methodDeclListPrintSystem, null);

        ClassDecl string = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);

        Package astAsPackage = (Package) ast;
        astAsPackage.classDeclList.add(system);
        astAsPackage.classDeclList.add(printStream);
        astAsPackage.classDeclList.add(string);
    }

    // identificationError is used to trace error when identification fails
    static class IdentificationError extends Error {
        private static final long serialVersionUID = 1L;
    }

    private void idError(String e, SourcePosition sp) throws Identification.IdentificationError {
        reporter.reportError("*** line " + sp.getLineNumber() + ": Identification error - " + e);
        throw new IdentificationError();
    }

    // Decorate all identifier and reference nodes with corresponding declarations while catching any possible errors
    public void identify() {
        try {
            ast.visit(this, null);
        }
        catch (Identification.IdentificationError e) {

        }
//        catch (Exception e) {
//            reporter.reportError("Should not encounter exceptions in identification. Exception message: " + e.getMessage());
//        }
    }


    // Package

    @Override
    public Object visitPackage(Package prog, Object arg) {
        table.openScope();

        // add all the classes to the table, catch duplicate class declarations
        SourcePosition sp = null;
        try {
            for(ClassDecl cd: prog.classDeclList) {
                sp = cd.posn;
                table.enter(cd);
            }
        } catch (IllegalArgumentException e) {
            idError(e.getMessage(), sp);
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
        // set current class so "this" keyword works
        currentClass = cd;

        // add members so all fields and methods are visible
        table.openScope();
        SourcePosition sp = null;
        try {
            for(FieldDecl fd: cd.fieldDeclList) {
                sp = fd.posn;
                table.enter(fd);
            }
            for(MethodDecl md: cd.methodDeclList) {
                sp = md.posn;
                table.enter(md);
            }
        } catch (IllegalArgumentException e) {
            idError(e.getMessage(), sp);
        }

        // visit all members
        for(FieldDecl fd: cd.fieldDeclList) {
            fd.visit(this, null);
        }
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

        // check for unique "private static main (String[] args)" method


        // let method body know the access modifier of the method by setting withinStaticMethod flag
        withinStaticMethod = md.isStatic;

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
        try {
            table.enter(pd);
        } catch (IllegalArgumentException e) {
            idError(e.getMessage(), pd.posn);
        }
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        decl.type.visit(this, null);
        try {
            table.enter(decl);
        } catch (IllegalArgumentException e) {
            idError(e.getMessage(), decl.posn);
        }
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
        Declaration originalDecl = table.searchClasses(classTypeName.spelling);
        if (originalDecl == null) {
            idError("Undeclared class type", type.posn);
        }
//        if (table.scopeLevel(classTypeName.spelling) > 0) {
//            idError("New called on non class identifier in new object expr");
//        }
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
        // outdated old way, does not prevent following case

        // check if left hand declaration exists already to prevent double errors
        // visit right hand expression after checking if left exists
        // to prevent use of the declared variable in the initializing expression
        // then visit left

        // int x in higher scope (field decl)
        // int x = x + 2;
        // should fail bc compiler should reference right x to left x instead of higher x

        // new way is to check if the variable name on left shows up in the expression on right
        // check for reference to left varDecl on all references on the right
        // if forbidden variable shows up throw idError

        this.declaredVariable = stmt.varDecl.name;
        stmt.initExp.visit(this, null);
        this.declaredVariable = null;
        stmt.varDecl.visit(this, null);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.val.visit(this, null);

        if (stmt.ref.decl instanceof FieldDecl) {
            FieldDecl fieldDecl = (FieldDecl)stmt.ref.decl;
            if (fieldDecl.isStatic) {
                stmt.ref.decl.isStaticFieldRef = true;
            }
        }
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
        if (!(stmt.methodRef.decl instanceof MethodDecl)) {
            idError("Method call in statement must point to method declaration", stmt.methodRef.posn);
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
        table.openScope();
        if (stmt.thenStmt instanceof VarDeclStmt) {
            idError("Solitary variable declaration in THEN branch of if statement", stmt.thenStmt.posn);
        }
        stmt.thenStmt.visit(this, null);
        table.closeScope();
        if (stmt.elseStmt != null) {
            table.openScope();
            if (stmt.elseStmt instanceof VarDeclStmt) {
                idError("Solitary variable declaration in ELSE branch of if statement", stmt.elseStmt.posn);
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
        if (stmt.body instanceof VarDeclStmt) {
            idError("Solitary variable declaration in THEN branch of if statement", stmt.body.posn);
        }
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
        Declaration originalDecl = table.searchClasses(newClassName.spelling);
        if (originalDecl == null) {
            idError("Undeclared identifier after 'new' (not a class) in new object expr", expr.posn);
        }
//        if (table.scopeLevel(newClassName.spelling) > 0) {
//            idError("New called on non class identifier in new object expr");
//        }
        newClassName.decl = originalDecl;
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        TypeKind arrType = expr.eltType.typeKind;
        if (arrType == TypeKind.CLASS) {
            Identifier arrTypeName = ((ClassType)(expr.eltType)).className;
            ClassDecl originalDecl = (ClassDecl) table.searchClasses(arrTypeName.spelling);
            if (originalDecl == null) {
                idError("Undeclared class identifier after 'new' in new array expr", expr.posn);
            }
//            if (table.scopeLevel(arrTypeName.spelling) > 0) {
//                idError("New called on non class identifier in new array expr");
//            }
            arrTypeName.decl = originalDecl;
        }
        expr.sizeExpr.visit(this, null);
        return null;
    }


    // References

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        if (withinStaticMethod) {
            idError("Cannot reference this within a static method", ref.posn);
        }
        ref.decl = currentClass;
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        // if right hand expression of variable declaration contains name of variable throw idError
        if (ref.id.spelling.equals(this.declaredVariable)) {
            idError("Illegal use of a declared variable in its initializing expression", ref.posn);
        }

        // decorate id of ref
        Declaration originalDecl = table.search(ref.id.spelling);
        if (originalDecl == null) {
            idError("Undeclared identifier", ref.posn);
        }

        // if declaration is a member decl check static access
        if (originalDecl instanceof MemberDecl) {
            if (withinStaticMethod && !( ( (MemberDecl) originalDecl).isStatic )) {
                idError("Reference within static method cannot directly access a non-static member of current class", ref.posn);
            }
        }
        ref.id.decl = originalDecl;
        ref.decl = ref.id.decl;
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, null);
        if (ref.ref.decl instanceof MethodDecl) {
            idError("Method reference not allowed within qualRef chain", ref.posn);
        }
        ref.id.visit(this, ref.ref);
        ref.decl = ref.id.decl;
        return null;
    }


    // Terminals

    // only time we visit identifier using visitor is when it is called by visitQRef
    // we use the arg parameter of the visitor pattern to pass in the left hand reference
    // decorate identifier of idRef with corresponding declaration
    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        if (arg == null) {
            idError("Should never reach a call to visitIdentifier with null arg", id.posn);
        }
        if ( !(arg instanceof Reference) ) {
            idError("Dot operator cannot be called on non reference", id.posn);
        }

        // if right hand expression of variable declaration contains name of variable throw idError
        if (id.spelling.equals(this.declaredVariable)) {
            idError("Illegal use of a declared variable in its initializing expression", id.posn);
        }

        assert arg != null;
        assert arg instanceof Reference;
        if (((Reference) arg).decl.type.typeKind == TypeKind.ARRAY) {
            if (!id.spelling.equals("length")) {
                idError("Cannot access a field of an array which is not length", id.posn);
            }
            id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, id.posn), "length", id.posn);
            id.decl.isArrayLength = true;
            return null;
        }

//        System.out.println("Call to visitIdentifier: arg = " + ((Reference) arg).decl.name + ", id = " + id.spelling);

        if (arg instanceof ThisRef) {
            // QualRef calling visitIdentifier method looks like this.id
            // check if identifier is a member of current class by checking currentClass declaration
            for (FieldDecl fd : currentClass.fieldDeclList) {
                if (id.spelling.equals(fd.name)) {
                    if (withinStaticMethod && !fd.isStatic) {
                        idError("Reference within static method cannot directly access a non-static member of current class", id.posn);
                    }
                    id.decl = fd;
                    return null;
                }
            }
            for (MethodDecl md : currentClass.methodDeclList) {
                if (id.spelling.equals(md.name)) {
                    if (withinStaticMethod && !md.isStatic) {
                        idError("Reference within static method cannot directly access a non-static member of current class", id.posn);
                    }
                    id.decl = md;
                    return null;
                }
            }
            idError("Identifier not found in current class for this.ID QualRef", id.posn);

        } else if (arg instanceof IdRef) {
            // QualRef calling visitIdentifier looks like id.id, with nothing else on the left
            // first id has to be in one of the scopes of the id table because it is unqualified ref
            // first id has already been decorated with its decl
            // check that first id's type is a class by checking its decl
            // check if identifier is a member of the class referenced by the first id's decl's type

            // special case: first id's declaration points to the currentClass declaration
            // (it is the same type as the current class)
            // allow the second id to be private

            IdRef idRef = (IdRef) arg;
//            System.out.println("Arg is instance of idRef: arg = " + ((Reference) arg).decl.name + ", id = " + id.spelling);
            String className;

            // boolean to mark if first id's type is the same as current class
            boolean isCurrentClass = false;

            if (idRef.decl instanceof LocalDecl) {
                LocalDecl localDecl = (LocalDecl) idRef.decl;
                if (localDecl.type.typeKind != TypeKind.CLASS) {
                    idError("Cannot use dot operator on reference which does not point to a class declaration", idRef.posn);
                }

                // find the class which idRef is a type of
                className = ( (ClassType) localDecl.type ).className.spelling;
                ClassDecl classDecl = (ClassDecl) table.searchClasses(className);
                if (classDecl == null) {
                    idError("Undeclared class identifier for localDecl of IdRef in QualRef", idRef.posn);
                }

                // mark that first id's type is the same as current class
                if (classDecl.name.equals(currentClass.name)) {
                    isCurrentClass = true;
                }

                // search class members for id
                for (FieldDecl fd : classDecl.fieldDeclList) {
                    if (id.spelling.equals(fd.name)) {
                        if (fd.isPrivate && !isCurrentClass) {
                            idError("QualRef to a member of another class may not have private visibility", id.posn);
                        }
                        id.decl = fd;
                        return null;
                    }
                }
                for (MethodDecl md : classDecl.methodDeclList) {
                    if (id.spelling.equals(md.name)) {
                        if (md.isPrivate && !isCurrentClass) {
                            idError("QualRef to a member of another class may not have private visibility", id.posn);
                        }
                        id.decl = md;
                        return null;
                    }
                }
                idError("Identifier not found in referenced class for ID.ID QualRef, first ID points to localDecl", id.posn);

            } else if (idRef.decl instanceof MemberDecl) {
                MemberDecl memberDecl = (MemberDecl) idRef.decl;
                if (memberDecl.type.typeKind != TypeKind.CLASS) {
                    idError("Cannot use dot operator on reference which does not point to a class declaration", idRef.posn);
                }

                // find the class which idRef is a type of
                className = ( (ClassType) memberDecl.type ).className.spelling;
                ClassDecl classDecl = (ClassDecl) table.searchClasses(className);
                if (classDecl == null) {
                    idError("Undeclared class identifier for memberDecl of IdRef in QualRef", idRef.posn);
                }

                // mark that first id's type is the same as current class
                if (classDecl.name.equals(currentClass.name)) {
                    isCurrentClass = true;
                }

                // search class members for id
                for (FieldDecl fd : classDecl.fieldDeclList) {
                    if (id.spelling.equals(fd.name)) {
                        if (fd.isPrivate && !isCurrentClass) {
                            idError("QualRef to a member of another class may not have private visibility", id.posn);
                        }
                        id.decl = fd;
                        return null;
                    }
                }
                for (MethodDecl md : classDecl.methodDeclList) {
                    if (id.spelling.equals(md.name)) {
                        if (md.isPrivate && !isCurrentClass) {
                            idError("QualRef to a member of another class may not have private visibility", id.posn);
                        }
                        id.decl = md;
                        return null;
                    }
                }
                idError("Identifier not found in referenced class for ID.ID QualRef, first ID points to memberDecl", id.posn);

            } else if (idRef.decl instanceof ClassDecl) {
                ClassDecl classDecl = (ClassDecl) idRef.decl;

                // instead of finding the class which idRef is a type of, we have direct access to it
                // don't need to cast the type of the decl to a class decl and then search classes
                // also dont need to check if classDecl is a class

                // mark that first id's type is the same as current class
                if (classDecl.name.equals(currentClass.name)) {
                    isCurrentClass = true;
                }

                // search class members for id
                for (FieldDecl fd : classDecl.fieldDeclList) {
                    if (id.spelling.equals(fd.name)) {
                        if (fd.isPrivate && !isCurrentClass) {
                            idError("QualRef to a member of another class may not have private visibility", id.posn);
                        }
                        if (!fd.isStatic) {
                            idError("QualRef to a member of a class must be declared to have static access", id.posn);
                        }
                        id.decl = fd;
                        // static field ref
                        classDecl.isStaticFieldRef = true;
                        id.decl.isStaticFieldRef = true;
                        return null;
                    }
                }
                for (MethodDecl md : classDecl.methodDeclList) {
                    if (id.spelling.equals(md.name)) {
                        if (md.isPrivate && !isCurrentClass) {
                            idError("QualRef to a member of another class may not have private visibility", id.posn);
                        }
                        if (!md.isStatic) {
                            idError("QualRef to a member of a class must be declared to have static access", id.posn);
                        }
                        id.decl = md;
                        return null;
                    }
                }
                idError("Identifier not found in referenced class for ID.ID QualRef, first ID points to classDecl", id.posn);
            }

        } else if (arg instanceof QualRef) {
            // arg must be a MemberDecl, and id must have both public and static access

            // QualRef calling visitIdentifier looks like QualRef.id, or (id | this) (.id)* .id
            // arbitrary number of qualrefs to the left of the dot operator
            // previous qualref has already been decorated with its decl
            // as stated above, the decl of previous qualref (arg) must be a MemberDecl
            // check that previous qualref type is a class by checking its decl
            // check if identifier is a member of the class referenced by the first id's decl's type
            // check if identifier has both public and static access

            QualRef qualRef = (QualRef) arg;
//            System.out.println("Arg is instance of QualRef: arg = " + ((Reference) arg).decl.name + ", id = " + id.spelling);
            String className;

            // boolean to mark if first id's type is the same as current class
            boolean isCurrentClass = false;

            if (!(qualRef.decl instanceof MemberDecl)) {
                idError("Qualref of QualRef.id does not point to a MemberDecl", qualRef.posn);
            }
            MemberDecl memberDecl = (MemberDecl) qualRef.decl;

            // check that previous qualref type is a class by checking its decl
            if (memberDecl.type.typeKind != TypeKind.CLASS) {
                idError("Cannot use dot operator on reference which does not point to a class declaration", qualRef.posn);
            }

            // find the class which idRef is a type of
            className = ( (ClassType) memberDecl.type ).className.spelling;
            ClassDecl classDecl = (ClassDecl) table.searchClasses(className);
            if (classDecl == null) {
                idError("Undeclared class identifier for memberDecl of QualRef in QualRef", qualRef.posn);
                return null;
            }

            // mark that first id's type is the same as current class
            if (classDecl.name.equals(currentClass.name)) {
                isCurrentClass = true;
            }

            // search class members for id
            // check if identifier is a member of the class referenced by the first id's decl's type
            // check if identifier has both public and static access
            for (FieldDecl fd : classDecl.fieldDeclList) {
                if (id.spelling.equals(fd.name)) {
                    if (fd.isPrivate && !isCurrentClass) {
                        idError("QualRef to a member of another class may not have private visibility", id.posn);
                    }
//                    if (!fd.isStatic) {
//                        idError("QualRef to a member of a class must be declared to have static access");
//                    }
                    id.decl = fd;
                    return null;
                }
            }
            for (MethodDecl md : classDecl.methodDeclList) {
                if (id.spelling.equals(md.name)) {
                    if (md.isPrivate && !isCurrentClass) {
                        idError("QualRef to a member of another class may not have private visibility", id.posn);
                    }
//                    if (!md.isStatic) {
//                        idError("QualRef to a member of a class must be declared to have static access");
//                    }
                    id.decl = md;
                    return null;
                }
            }
            idError("Identifier not found in referenced class for QualRef.ID QualRef", id.posn);
        }

        idError("Should never reach this point", id.posn);
        return null;

//        Declaration originalDecl = table.search(id.spelling);
//        if (originalDecl == null) {
//            idError("Undeclared identifier");
//            return null;
//        }
//        id.decl = originalDecl;
//        return null;
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