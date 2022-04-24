/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntityDescription;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST {

	public String name;
	public TypeDenoter type;
	public boolean isArrayLength;
	public boolean isStaticFieldRef;
	public boolean isPrintLn;
	public RuntimeEntityDescription rED = null;

	// rED offsets are relative, what they are relative to depends on the declaration it is associated with

	// offset relative to LB for local variables and parameter variables
	// offset relative to SB for static fields
	// offset relative to OB for instance variables
	// offset relative to CB for methods
	
	public Declaration(String name, TypeDenoter type, SourcePosition posn) {
		super(posn);
		this.name = name;
		this.type = type;
		this.isArrayLength = false;
		this.isStaticFieldRef = false;
		this.isPrintLn = false;
	}
}
