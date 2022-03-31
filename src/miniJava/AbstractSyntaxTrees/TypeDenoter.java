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
        if (typeKind == TypeKind.NULL) {
            return other.typeKind == TypeKind.CLASS || other.typeKind == TypeKind.ARRAY || other.typeKind == TypeKind.NULL;
        }
        if (typeKind == TypeKind.CLASS) {
            if (other.typeKind == TypeKind.CLASS) {
                return ((ClassType) this).className == ((ClassType) other).className;
            }
            return other.typeKind == TypeKind.NULL;
        }
        if (this.typeKind == TypeKind.ARRAY) {
            if (other.typeKind == TypeKind.ARRAY) {
                if ( ( (ArrayType) (this) ).eltType.typeKind == TypeKind.CLASS) {
                    return ( (ClassType) ((ArrayType) (this)).eltType ).className == ( (ClassType) ((ArrayType) (this)).eltType ).className;
                }
                return ((ArrayType)(this)).eltType.typeKind == ((ArrayType)other).eltType.typeKind;
            }
            return other.typeKind == TypeKind.NULL;
        }
        return typeKind == other.typeKind;
    }

//    public boolean comparable(TypeDenoter other) {
//        if (this.typeKind == TypeKind.CLASS || this.typeKind == TypeKind.NULL) {
//            return other.typeKind == TypeKind.CLASS || other.typeKind == TypeKind.NULL;
//        }
//        if (this.typeKind == TypeKind.ARRAY) {
//            if (other.typeKind == TypeKind.ARRAY) {
//                return ((ArrayType)(this)).eltType.typeKind == ((ArrayType)other).eltType.typeKind;
//            }
//            return other.typeKind == TypeKind.NULL;
//        }
//        return typeKind == other.typeKind;
//    }
}

        