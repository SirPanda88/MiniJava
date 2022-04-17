/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class ClassDecl extends Declaration {

  public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  super(cn, new ClassType(new Identifier(new Token(Token.TokenKind.ID, cn, null)), null), posn);
	  if (cn.equals("String")) {
	      this.type = new BaseType(TypeKind.UNSUPPORTED, null);
      }
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
  }

  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }
      
  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
}
