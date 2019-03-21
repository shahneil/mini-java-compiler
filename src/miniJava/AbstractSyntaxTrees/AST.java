/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class AST {

	public AST(SourcePosition position) {
		this.position = position;
	}

	public String toString() {
		String fullClassName = this.getClass().getName();
		String cn = fullClassName.substring(1 + fullClassName.lastIndexOf('.'));
		if (ASTDisplay.showPosition)
			cn = cn + " " + position.toString();
		return cn;
	}

	public abstract <A, R> R visit(Visitor<A, R> v, A o);

	public SourcePosition position;
}
