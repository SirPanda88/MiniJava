package miniJava.CodeGenerator;

import mJAM.*;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

import java.util.ArrayList;
import java.util.Stack;

public class CodeGenerator implements Visitor<Object, Object> {

    private final ErrorReporter reporter;
    private AST ast;
    int localsOffset;
    int mainAddr;
    int staticFieldPushAddr;
    int localAllocationPopCount;
    int currMethodParamCount;

    ArrayList<MethodPatch> methodPatches;

    boolean debug = true;

    public CodeGenerator(AST ast, ErrorReporter reporter) {
        this.ast = ast;
        this.reporter = reporter;
        Machine.initCodeGen();
        methodPatches = new ArrayList<MethodPatch>();
    }

    static class CodeGenerationError extends Error {
        private static final long serialVersionUID = 1L;
    }

    private void compileError(String e, SourcePosition sp) throws CodeGenerator.CodeGenerationError {
        reporter.reportError("*** line " + sp.getLineNumber() + ": Code generation error - " + e);
        throw new CodeGenerator.CodeGenerationError();
    }

    public void generate(String fileName) {
        staticFieldPushAddr = Machine.nextInstrAddr();
        Machine.emit(Op.PUSH, 0);

        Machine.emit(Op.LOADL, 0);
        Machine.emit(Prim.newarr);

        mainAddr = Machine.nextInstrAddr();
        Machine.emit(Op.CALL, Machine.Reg.CB, 0);
        Machine.emit(Op.HALT, 0, 0, 0);

        ast.visit(this, null);

        String objectCodeFileName = fileName.replace(".java", ".mJAM");
        System.out.print("Writing objectFile " + objectCodeFileName + " ... ");
        ObjectFile objectFile = new ObjectFile(objectCodeFileName);
        if (objectFile.write()) {
            compileError("Writing objectFile unsuccessful", new SourcePosition(-1));
        } else {
            System.out.println("Writing objectFile successful");
        }

        String asmCodeFileName = objectCodeFileName.replace(".mJAM", ".asm");
        System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
        Disassembler d = new Disassembler(objectCodeFileName);
        if (d.disassemble()) {
            compileError("Writing assembly file unsuccessful", new SourcePosition(-1));
            return;
        } else {
            System.out.println("Writing assembly file successful");
        }

//        if (debug) {
//            System.out.println("Running code in debugger ... ");
//            Interpreter.debug(objectCodeFileName, asmCodeFileName);
//            System.out.println(" program complete");
//        }
    }

    @Override
    public Object visitPackage(Package prog, Object arg) {
        int staticFieldOffset = 0;
        for (ClassDecl classDecl : prog.classDeclList) {
            int instanceFieldOffset = 0;
            for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
                if (fieldDecl.isStatic) {
                    fieldDecl.rED = new RuntimeEntityDescription(staticFieldOffset++);
                } else {
                    fieldDecl.rED = new RuntimeEntityDescription(instanceFieldOffset++);
                }
            }

            classDecl.rED = new RuntimeEntityDescription(instanceFieldOffset);
        }

        Machine.patch(staticFieldPushAddr, staticFieldOffset);
        Machine.patch(mainAddr, Machine.nextInstrAddr());

        // check main method
        boolean foundMain = false;
        for (ClassDecl cd : prog.classDeclList) {
            for (MethodDecl md : cd.methodDeclList) {
                if (md.name.equals("main")) {
                    if (md.parameterDeclList.size() == 1) {
                        if (md.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY) {
                            ArrayType arrayType = (ArrayType) md.parameterDeclList.get(0).type;
                            ClassType classType = (ClassType) arrayType.eltType;

                            if ( classType.className.spelling.equals("String") ) {
                                if (foundMain) {
                                    compileError("Duplicate main declaration in package", md.posn);
                                }

                                foundMain = true;

                                if (md.isPrivate) {
                                    compileError("Private main method", md.posn);
                                }

                                if ( !(md.isStatic) ) {
                                    compileError("Non static main method", md.posn);
                                }
                            }
                        }
                    }
                }
            }
        }

        // generate return machine code for all functions including void
        for (ClassDecl cd : prog.classDeclList) {
            for (MethodDecl md : cd.methodDeclList) {
                StatementList statementList = md.statementList;
                if (statementList.size() == 0) {
                    md.statementList.add(new ReturnStmt(null, md.posn));
                }
                Statement lastStatement = statementList.get(statementList.size()-1);
                if (md.type.typeKind == TypeKind.VOID) {
                    md.statementList.add(new ReturnStmt(null, lastStatement.posn));
                }
            }
        }

        for (ClassDecl c : prog.classDeclList)
            c.visit(this, null);

        for (MethodPatch methodPatch : methodPatches) {
            Machine.patch(methodPatch.methodCodeAddress, methodPatch.methodDecl.rED.displacement);
        }

        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        for (FieldDecl f : cd.fieldDeclList)
            f.visit(this, null);
        for (MethodDecl m : cd.methodDeclList)
            m.visit(this, null);
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        fd.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        localsOffset = 3;
        currMethodParamCount = md.parameterDeclList.size();

        md.type.visit(this, null);

        int parameterOffsetStart = -md.parameterDeclList.size();
        for (ParameterDecl p : md.parameterDeclList) {
            p.visit(this, null);
            p.rED = new RuntimeEntityDescription(parameterOffsetStart++);
        }

        md.rED = new RuntimeEntityDescription(Machine.nextInstrAddr());

        for (Statement statement : md.statementList) {
            statement.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        //		System.out.println("Placing " + decl.name + " at offset " + localsDisplacement);
        decl.rED = new RuntimeEntityDescription(localsOffset++);
        decl.type.visit(this, null);
        return null;
    }



    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        type.className.visit(this, null);
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, null);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        this.localAllocationPopCount = 0;
        for (Statement statement : stmt.sl)
            statement.visit(this, null);
        if (this.localAllocationPopCount > 0) {
            this.localsOffset -= this.localAllocationPopCount;
            Machine.emit(Op.POP, this.localAllocationPopCount);
        }
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        this.localAllocationPopCount++;
        stmt.varDecl.visit(this, null);
        stmt.initExp.visit(this, null);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        if (stmt.ref.decl.isStaticFieldRef) {
            stmt.val.visit(this, null);
            Machine.emit(Op.STORE, Machine.Reg.SB, stmt.ref.decl.rED.displacement);

        } else if (stmt.ref instanceof IdRef) {
            IdRef idRef = (IdRef) stmt.ref;

            if (idRef.decl instanceof FieldDecl) {
                Machine.emit(Op.LOADA, Machine.Reg.OB, 0);
                Machine.emit(Op.LOADL, idRef.id.decl.rED.displacement);
                stmt.val.visit(this, null);
                Machine.emit(Prim.fieldupd);

            } else {
                stmt.val.visit(this, null);
                storeIdRef(idRef);
            }

        } else if (stmt.ref instanceof QualRef) {
            QualRef qualRef = (QualRef) stmt.ref;
            pushQRefInfo(qualRef);
            stmt.val.visit(this, null);
            Machine.emit(Prim.fieldupd);
        }
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.ix.visit(this, null);
        stmt.exp.visit(this, null);
        Machine.emit(Prim.arrayupd);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        for (Expression argument : stmt.argList) {
            argument.visit(this, null);
        }

        if (stmt.methodRef.decl.isPrintLn) {
            Machine.emit(Prim.putintnl);
            return null;
        }
        int callAddr = Machine.nextInstrAddr();

        MethodDecl methodDecl = (MethodDecl) stmt.methodRef.decl;

        if (methodDecl.isStatic) {
            Machine.emit(Op.CALL, Machine.Reg.CB, 0);
            methodPatches.add(new MethodPatch(callAddr, methodDecl));
        } else {
            stmt.methodRef.visit(this, null);
            if (stmt.methodRef instanceof QualRef) {
                QualRef qualRef = (QualRef) stmt.methodRef;
                Reference callingObject = qualRef.ref;
                callingObject.visit(this, null);
            } else {
                Machine.emit(Op.LOADA, Machine.Reg.OB, 0);
            }

            callAddr = Machine.nextInstrAddr();
            Machine.emit(Op.CALLI, Machine.Reg.CB, 0);
            methodPatches.add(new MethodPatch(callAddr, methodDecl));
        }

        if (stmt.methodRef.decl.type.typeKind != TypeKind.VOID) {
            Machine.emit(Op.POP, 1);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        int resultSize = 0;
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, null);
            resultSize = 1;
        }

        Machine.emit(Op.RETURN, resultSize, 0, currMethodParamCount);
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, null);

        int baseJumpAddr = Machine.nextInstrAddr();
        Machine.emit(Op.JUMPIF, 0, Machine.Reg.CB, 0);

        stmt.thenStmt.visit(this, null);

        int endJumpAddr = Machine.nextInstrAddr();
        Machine.emit(Op.JUMP, 0, Machine.Reg.CB, 0);
        Machine.patch(baseJumpAddr, Machine.nextInstrAddr());

        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, null);
        }

        Machine.patch(endJumpAddr, Machine.nextInstrAddr());

        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        int baseJumpAddr = Machine.nextInstrAddr();
        Machine.emit(Op.JUMP, 0, Machine.Reg.CB, 0);

        stmt.body.visit(this, null);

        int endJumpAddr = Machine.nextInstrAddr();

        stmt.cond.visit(this, null);
        Machine.emit(Op.JUMPIF, 1, Machine.Reg.CB, baseJumpAddr + 1);

        Machine.patch(baseJumpAddr, endJumpAddr);

        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        // TODO: check
        if (expr.operator.kind == Token.TokenKind.MINUS) {
            // convert unary -x into binary 0-x
            // ensures consistent behavior with Prim.sub
            // since visit operator cannot differentiate between unary and binary minus
            Machine.emit(Op.LOADL, 0);
        }
        expr.expr.visit(this, null);
        expr.operator.visit(this, null);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        // TODO: check
        expr.left.visit(this, null);
        expr.right.visit(this, null);
        expr.operator.visit(this, null);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        if (expr.ref.decl.isStaticFieldRef) {
            Machine.emit(Op.LOAD, Machine.Reg.SB, expr.ref.decl.rED.displacement);
        } else {
            expr.ref.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.visit(this, null);
        expr.ixExpr.visit(this, null);
        Machine.emit(Prim.arrayref);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        for (Expression argument : expr.argList) {
            argument.visit(this, null);
        }

        if (expr.functionRef.decl.isPrintLn) {
            return null;
        }

        expr.functionRef.visit(this, null);
        int callAddr = Machine.nextInstrAddr();
        MethodDecl methodDecl = (MethodDecl) expr.functionRef.decl;

        if (methodDecl.isStatic) {
            Machine.emit(Op.CALL, Machine.Reg.CB, 0);
            methodPatches.add(new MethodPatch(callAddr, methodDecl));
        } else {
            if (expr.functionRef instanceof QualRef) {
                QualRef qualRef = (QualRef) expr.functionRef;
                qualRef.ref.visit(this, null);
            } else {
                Machine.emit(Op.LOADA, Machine.Reg.OB, 0);
            }
            callAddr = Machine.nextInstrAddr();
            Machine.emit(Op.CALLI, Machine.Reg.CB, 0);
            methodPatches.add(new MethodPatch(callAddr, methodDecl));
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
        Machine.emit(Op.LOADL, -1);
        Machine.emit(Op.LOADL, expr.classtype.className.decl.rED.displacement);
        Machine.emit(Prim.newobj);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.sizeExpr.visit(this, null);
        Machine.emit(Prim.newarr);
        return null;
    }


    // References

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        Machine.emit(Op.LOADA, Machine.Reg.OB, 0);
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        pushIdRef(ref);
        return null;
    }

    public void pushIdRef(IdRef ref) {
        if (ref.decl instanceof FieldDecl) {
            FieldDecl fieldDecl = (FieldDecl) ref.decl;
            if (fieldDecl.isStatic) {
                Machine.emit(Op.LOAD, Machine.Reg.SB, ref.id.decl.rED.displacement);
            } else {
                Machine.emit(Op.LOAD, Machine.Reg.OB, ref.id.decl.rED.displacement);
            }
        } else if (ref.id.decl.rED != null) {
            if (ref.id.decl.isStaticFieldRef) {
                Machine.emit(Op.LOAD, Machine.Reg.SB, ref.id.decl.rED.displacement);
            } else if ( !(ref.id.decl instanceof MethodDecl) ) {
                Machine.emit(Op.LOAD, Machine.Reg.LB, ref.id.decl.rED.displacement);
            }
        }
    }

    public void storeIdRef(IdRef ref) {
        if (ref.decl instanceof FieldDecl) {
            FieldDecl fieldDecl = (FieldDecl) ref.decl;
            if (fieldDecl.isStatic) {
                Machine.emit(Op.STORE, Machine.Reg.SB, ref.id.decl.rED.displacement);
            }
        } else {
            if (ref.id.decl.isStaticFieldRef) {
                Machine.emit(Op.STORE, Machine.Reg.SB, ref.id.decl.rED.displacement);
            } else {
                Machine.emit(Op.STORE, Machine.Reg.LB, ref.id.decl.rED.displacement);
            }
        }
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        if (ref.id.decl.isArrayLength) {
            pushIdRef((IdRef) ref.ref);
            Machine.emit(Prim.arraylen);
        } else if (ref.id.decl.rED != null) {
            pushQRefInfo(ref);
            Machine.emit(Prim.fieldref);
        }
        return null;
    }

    public void pushQRefInfo(QualRef qualRef) {
        if (qualRef.id.decl.rED == null) {
            return;
        }
        // create a stack of all the displacements of the nested qrefs until the last one is reached
        Stack<Integer> fieldDisplacementStack = new Stack<Integer>();

        fieldDisplacementStack.push(qualRef.id.decl.rED.displacement);
        while (qualRef.ref instanceof QualRef) {
            qualRef = (QualRef) qualRef.ref;
            fieldDisplacementStack.push(qualRef.decl.rED.displacement);
        }

        qualRef.ref.visit(this, null);

        int stackSize = fieldDisplacementStack.size();
        for (int i = 0; i < stackSize; i++) {
            int fieldDisplacement = fieldDisplacementStack.pop();
            Machine.emit(Op.LOADL, fieldDisplacement);
            if (i < stackSize - 1) {
                Machine.emit(Prim.fieldref);
            }
        }
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        switch (op.kind) {
            case GREATER:
                Machine.emit(Prim.gt);
            case LESS:
                Machine.emit(Prim.lt);
                break;
            case EQUALS:
                Machine.emit(Prim.eq);
                break;
            case LESSEQUAL:
                Machine.emit(Prim.le);
                break;
            case GREATEREQUAL:
                Machine.emit(Prim.ge);
                break;
            case NOTEQUAL:
                Machine.emit(Prim.ne);
                break;
            case AND:
                Machine.emit(Prim.and);
                break;
            case OR:
                Machine.emit(Prim.or);
                break;
            case NOT:
                Machine.emit(Prim.neg);
                break;
            case PLUS:
                Machine.emit(Prim.add);
                break;
            case MINUS:
                Machine.emit(Prim.sub);
                break;
            case MULT:
                Machine.emit(Prim.mult);
                break;
            case DIV:
                Machine.emit(Prim.div);
                break;
            default:
                compileError("Unknown operator " + op.kind, op.posn);
                break;
        }
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        int intValue = Integer.parseInt(num.spelling);
        Machine.emit(Op.LOADL, intValue);
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        if (bool.spelling.equals("true")) {
            Machine.emit(Op.LOADL, Machine.trueRep);
        } else if (bool.spelling.equals("false")) {
            Machine.emit(Op.LOADL, Machine.falseRep);
        }
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
        Machine.emit(Op.LOADL, Machine.nullRep);
        return null;
    }
}
