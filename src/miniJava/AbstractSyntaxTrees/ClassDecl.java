/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.ContextualAnalyzer.IdentificationTable;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassDecl extends Declaration {

	public FieldDeclList fieldDeclList;
	public MethodDeclList methodDeclList;
	public IdentificationTable table;

	public ClassDecl(String cn, FieldDeclList fdl, MethodDeclList mdl, SourcePosition position) {
		super(cn, null, position);
		fieldDeclList = fdl;
		methodDeclList = mdl;
		table = new IdentificationTable();
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitClassDecl(this, o);
	}

}
