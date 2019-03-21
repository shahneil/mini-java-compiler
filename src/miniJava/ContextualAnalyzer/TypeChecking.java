package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxRef;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class TypeChecking implements Visitor<Object, Object> {
	private ErrorReporter reporter;
	private Identification identification;

	public TypeChecking(ErrorReporter reporter) {
		this.reporter = reporter;
		this.identification = new Identification(reporter);
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitPackage(Package prog, Object o) {
		// Run identification
		// TODO: If identification fails, don't run type checking (exit early).
		prog.visit(identification, null);

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitClassDecl(ClassDecl cd, Object o) {
		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, Object o) {
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, Object o) {
		return null;
	}

	public Object visitParameterDecl(ParameterDecl pd, Object o) {
		return null;
	}

	public Object visitVarDecl(VarDecl decl, Object o) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitBaseType(BaseType type, Object o) {
		return null;
	}

	public Object visitClassType(ClassType type, Object o) {
		return null;
	}

	public Object visitArrayType(ArrayType type, Object o) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitBlockStmt(BlockStmt stmt, Object o) {
		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, Object o) {
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, Object o) {
		return null;
	}

	public Object visitCallStmt(CallStmt stmt, Object o) {
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, Object o) {
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, Object o) {
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, Object o) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitUnaryExpr(UnaryExpr expr, Object o) {
		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, Object o) {
		return null;
	}

	public Object visitRefExpr(RefExpr expr, Object o) {
		return null;
	}

	public Object visitCallExpr(CallExpr expr, Object o) {
		return null;
	}

	public Object visitLiteralExpr(LiteralExpr expr, Object o) {
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, Object o) {
		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, Object o) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitThisRef(ThisRef ref, Object o) {
		return null;
	}

	public Object visitIdRef(IdRef ref, Object o) {
		return null;
	}

	public Object visitQRef(QualRef ref, Object o) {
		return null;
	}

	public Object visitIxRef(IxRef ref, Object o) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitIdentifier(Identifier id, Object o) {
		return null;
	}

	public Object visitOperator(Operator op, Object o) {
		return null;
	}

	public Object visitIntLiteral(IntLiteral num, Object o) {
		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, Object o) {
		return null;
	}

	public Object visitNullLiteral(NullLiteral nul, Object o) {
		return null;
	}
}
