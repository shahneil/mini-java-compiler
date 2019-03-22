/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassType extends TypeDenoter {
	public ClassType(Identifier cn, SourcePosition posn) {
		super(TypeKind.CLASS, posn);
		className = cn;
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitClassType(this, o);
	}

	@Override
	public boolean equals(TypeDenoter t) {
		if (!super.equals(t)) {
			return false;
		}

		if (!(t instanceof ClassType)) {
			return false;
		}

		return this.className.spelling == ((ClassType) t).className.spelling;
	}

	public Identifier className;
}
