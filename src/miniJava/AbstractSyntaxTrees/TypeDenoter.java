/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST {

	public TypeDenoter(TypeKind type, SourcePosition posn) {
		super(posn);
		typeKind = type;
	}

	public boolean equals(TypeDenoter t) {
		TypeKind tk1 = this.typeKind;
		TypeKind tk2 = t.typeKind;

		// UNSUPPORTED
		if (tk1 == TypeKind.UNSUPPORTED || tk2 == TypeKind.UNSUPPORTED) {
			return false;
		}

		// ERROR
		if (tk1 == TypeKind.ERROR || tk2 == TypeKind.ERROR) {
			return true;
		}

		if (tk1 == tk2) {
			return true;
		}

		return false;
	}

	public TypeKind typeKind;

}
