/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayType extends TypeDenoter {

	public ArrayType(TypeDenoter eltType, SourcePosition posn) {
		super(TypeKind.ARRAY, posn);
		this.eltType = eltType;
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitArrayType(this, o);
	}

	@Override
	public boolean equals(TypeDenoter t) {
		if (!super.equals(t)) {
			return false;
		}

		if (!(t instanceof ArrayType)) {
			return false;
		}

		return this.eltType.equals(((ArrayType) t).eltType);
	}

	public TypeDenoter eltType;
}
