/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST {

    public TypeKind typeKind;
    
    public TypeDenoter(TypeKind type, SourcePosition posn){
        super(posn);
        typeKind = type;
    }

    public boolean sameType(TypeDenoter other) {
        if (typeKind == TypeKind.CLASS) {
            if (other.typeKind == TypeKind.CLASS) {
                if (((ClassType)this).className == ((ClassType)other).className) {
                    return true;
                }
            }
            return false;
        }
        return typeKind == other.typeKind;
    }
    
}

        