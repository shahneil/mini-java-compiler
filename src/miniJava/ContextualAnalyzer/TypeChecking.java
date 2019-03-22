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
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
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
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class TypeChecking implements Visitor<Object, TypeDenoter> {
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

	public TypeDenoter visitPackage(Package prog, Object o) {
		// Run identification
		// TODO: If identification fails, don't run type checking (exit early).
		prog.visit(identification, null);

		for (ClassDecl c : prog.classDeclList) {
			c.visit(this, null);
		}

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitClassDecl(ClassDecl cd, Object o) {
		for (FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, null);
		}

		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}

		return null;
	}

	public TypeDenoter visitFieldDecl(FieldDecl fd, Object o) {
		return null;
	}

	public TypeDenoter visitMethodDecl(MethodDecl md, Object o) {
		boolean isVoid = md.type.typeKind == TypeKind.VOID;
		boolean containsReturnStmt = false;

		// Check for void parameters
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, null);
		}

		for (Statement s : md.statementList) {
			s.visit(this, null);

			if (s instanceof ReturnStmt) {
				containsReturnStmt = true;
				TypeDenoter returnType = ((ReturnStmt) s).returnExpr.visit(this, null);

				// Void methods shouldn't contain return statements
				if (isVoid && returnType != null) {
					reporter.reportError(
							"*** Unexpected return statement in void method " + md.name + " at " + md.position);
				}

				// Check that return type matches
				if (!isVoid && returnType != null) {
					if (!md.type.equals(returnType)) {
						reporter.reportError("*** Expected " + md.type + " but found " + returnType + returnType);
					}
				}
			}
		}

		// Methods that aren't void should contain return statements
		if (!isVoid && !containsReturnStmt) {
			reporter.reportError("*** Method " + md.name + " does not contain a return statement.");
		}

		return null;
	}

	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object o) {
		pd.type.visit(this, null);
		if (pd.type.typeKind == TypeKind.VOID) {
			reporter.reportError("*** Parameter " + pd.name + " at " + pd.position + " cannot have void type.");
		}

		return null;
	}

	public TypeDenoter visitVarDecl(VarDecl decl, Object o) {
		decl.type.visit(this, null);
		if (decl.type.typeKind == TypeKind.VOID) {
			reporter.reportError("*** Variable " + decl.name + " at " + decl.position + " cannot have void type.");
		}

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitBaseType(BaseType type, Object o) {
		return type;
	}

	public TypeDenoter visitClassType(ClassType type, Object o) {
		type.className.visit(this, null);
		return type;
	}

	public TypeDenoter visitArrayType(ArrayType type, Object o) {
		type.eltType.visit(this, null);
		return type;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitBlockStmt(BlockStmt stmt, Object o) {
		for (Statement s : stmt.sl) {
			s.visit(this, null);
		}

		return null;
	}

	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object o) {
		stmt.varDecl.visit(this, null);

		TypeDenoter declType = stmt.varDecl.type;
		TypeDenoter exprType = stmt.initExp.visit(this, null);
		if (!declType.equals(exprType)) {
			reporter.reportError("*** Expected " + declType + " but found " + exprType + " at " + stmt.position);
		}

		return null;
	}

	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object o) {
		TypeDenoter refType = stmt.ref.visit(this, null);
		TypeDenoter valType = stmt.val.visit(this, null);

		// Check if types match
		if (!refType.equals(valType)) {
			reporter.reportError("*** Expected " + refType + " but found " + valType + " at " + stmt.position);
		}

		// Can't assign value to 'this'
		if (stmt.ref instanceof ThisRef) {
			reporter.reportError("*** Cannot use 'this' keyword in assignment statement.");
		}

		// Can't assign value to method
		if (stmt.ref.decl instanceof MethodDecl) {
			reporter.reportError("*** Cannot assign value to method at " + stmt.position);
		}

		return null;
	}

	public TypeDenoter visitCallStmt(CallStmt stmt, Object o) {
		stmt.methodRef.visit(this, null);

		MethodDecl md = (MethodDecl) stmt.methodRef.decl;
		ExprList args = stmt.argList;
		ParameterDeclList params = md.parameterDeclList;

		// Check if argument list and parameter list are of same size
		if (params.size() != args.size()) {
			reporter.reportError("*** Expected " + params.size() + " parameters but received " + args.size()
					+ " arguments at " + stmt.position);
		}

		// Check if arguments match parameters
		for (int i = 0; i < params.size(); i++) {

			ParameterDecl param = params.get(i);
			Expression arg = args.get(i);
			TypeDenoter argType = arg.visit(this, null);
			TypeDenoter paramType = param.type;

			if (!argType.equals(paramType)) {
				reporter.reportError("*** Expected parameter " + param.name + " of type " + paramType
						+ " but received argument of type " + argType);
			}
		}

		return null;
	}

	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object o) {
		stmt.returnExpr.visit(this, null);
		return null;
	}

	public TypeDenoter visitIfStmt(IfStmt stmt, Object o) {

		// Check that condition evaluates to boolean
		TypeDenoter condType = stmt.cond.visit(this, null);
		if (condType.typeKind != TypeKind.BOOLEAN) {
			reporter.reportError("*** Expected condition to evaluate to boolean but received " + condType + " at "
					+ condType.position);
		}

		// Variable declarations shouldn't be the only statements in branches of
		// conditional statements
		if (stmt.thenStmt instanceof VarDeclStmt) {
			reporter.reportError(
					"*** A variable declaration cannot be the only statement in a branch of a conditional statement at "
							+ stmt.thenStmt.position);
		}
		stmt.thenStmt.visit(this, null);

		// If there's an else statement, check the same as above
		if (stmt.elseStmt != null) {
			if (stmt.elseStmt instanceof VarDeclStmt) {
				reporter.reportError(
						"*** A variable declaration cannot be the only statement in a branch of a conditional statement at "
								+ stmt.elseStmt.position);
			}
			stmt.elseStmt.visit(this, null);
		}

		return null;
	}

	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object o) {

		// Check that condition evaluates to boolean
		TypeDenoter condType = stmt.cond.visit(this, null);
		if (condType.typeKind != TypeKind.BOOLEAN) {
			reporter.reportError("*** Expected condition to evaluate to boolean but received " + condType + " at "
					+ condType.position);
		}

		// Variable declarations shouldn't be the only statements in branches of
		// conditional statements
		if (stmt.body instanceof VarDeclStmt) {
			reporter.reportError(
					"*** A variable declaration cannot be the only statement in a branch of a conditional statement at "
							+ stmt.body.position);
		}
		stmt.body.visit(this, null);

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object o) {
		return null;
	}

	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object o) {
		return null;
	}

	public TypeDenoter visitRefExpr(RefExpr expr, Object o) {
		return null;
	}

	public TypeDenoter visitCallExpr(CallExpr expr, Object o) {
		return null;
	}

	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object o) {
		return null;
	}

	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object o) {
		return null;
	}

	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object o) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitThisRef(ThisRef ref, Object o) {
		return null;
	}

	public TypeDenoter visitIdRef(IdRef ref, Object o) {
		return null;
	}

	public TypeDenoter visitQRef(QualRef ref, Object o) {
		return null;
	}

	public TypeDenoter visitIxRef(IxRef ref, Object o) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitIdentifier(Identifier id, Object o) {
		return null;
	}

	public TypeDenoter visitOperator(Operator op, Object o) {
		return null;
	}

	public TypeDenoter visitIntLiteral(IntLiteral num, Object o) {
		return null;
	}

	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object o) {
		return null;
	}

	public TypeDenoter visitNullLiteral(NullLiteral nul, Object o) {
		return null;
	}
}
