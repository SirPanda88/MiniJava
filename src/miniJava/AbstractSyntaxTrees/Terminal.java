/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.Token.TokenKind;

abstract public class Terminal extends AST {

  public Terminal (Token t) {
	super(t.getPosn());
    spelling = t.spelling;
    kind = t.kind;
  }

  public TokenKind kind;
  public String spelling;
}
