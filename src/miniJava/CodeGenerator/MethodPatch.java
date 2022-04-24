package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.MethodDecl;

public class MethodPatch {
    int methodCodeAddress;
    MethodDecl methodDecl;

    public MethodPatch (int address, MethodDecl md) {
        this.methodCodeAddress = address;
        this.methodDecl = md;
    }
}
