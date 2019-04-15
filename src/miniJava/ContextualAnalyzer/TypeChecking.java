package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.AST;
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
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class TypeChecking implements Visitor<Object, TypeDenoter> {
	private ErrorReporter reporter;
	private Identification identification;

	public TypeChecking(ErrorReporter reporter) {
		this.reporter = reporter;
		this.identification = new Identification(reporter);
	}

	public void check(AST prog) {
		identification.run(prog);
		prog.visit(this, null);
	}

	private void error(String message, SourcePosition position) {
		reporter.reportError("*** line " + position.line + ": " + message);
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitPackage(Package prog, Object o) {
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
		StatementList sl = md.statementList;
		boolean isVoid = md.type.typeKind == TypeKind.VOID;
		boolean returnsValue = false;

		// Check for void parameters
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, null);
		}

		// Check that last statement in non-void method is a return statement
		Statement lastStmt = sl.get(sl.size() - 1);
		if (!isVoid && !(lastStmt instanceof ReturnStmt)) {
			error("Last statement in non-void method " + md.name + " must be a return statement.", lastStmt.position);
		}

		// Add empty return statement to void methods whose last statement isn't a
		// return statement
		else if (isVoid && !(lastStmt instanceof ReturnStmt)) {
			sl.add(new ReturnStmt(null, lastStmt.position));
		}

		for (Statement s : sl) {
			s.visit(this, null);

			if (s instanceof ReturnStmt) {
				ReturnStmt rs = (ReturnStmt) s;

				// Void methods shouldn't return values
				if (isVoid && rs.returnExpr != null) {
					error("Unexpected return expression in void method " + md.name + ".", rs.returnExpr.position);
				}

				// Non-void methods should not have empty return statements
				else if (!isVoid && rs.returnExpr == null) {
					error("Method " + md.name + " missing return expression.", md.position);
				}

				else if (rs.returnExpr != null) {
					returnsValue = true;
					TypeDenoter returnType = ((ReturnStmt) s).returnExpr.visit(this, null);

					// Check that return type matches
					if (!isVoid && returnType != null) {
						if (!md.type.equals(returnType)) {
							error("Expected " + md.type + " but found " + returnType, s.position);
						}
					}
				}
			}
		}

		// Non-void methods should contain at least one non-empty return statement
		if (!isVoid && !returnsValue) {
			error("Method " + md.name + " missing return statement.", md.position);
		}

		return null;
	}

	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object o) {
		pd.type.visit(this, null);

		// Check for void parameters
		if (pd.type.typeKind == TypeKind.VOID) {
			error("Parameter " + pd.name + " cannot be void.", pd.position);
		}

		return null;
	}

	public TypeDenoter visitVarDecl(VarDecl decl, Object o) {
		decl.type.visit(this, null);
		if (decl.type.typeKind == TypeKind.VOID) {
			error("Variable " + decl.name + " cannot be void.", decl.position);
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

		// null can be assigned to any object
		if (exprType.typeKind == TypeKind.NULL) {
			return null;
		}

		if (!declType.equals(exprType)) {
			error("Expected " + declType.typeKind + " but found " + exprType.typeKind + ".", stmt.position);
		}

		return null;
	}

	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object o) {
		TypeDenoter refType = stmt.ref.visit(this, null);
		TypeDenoter valType = stmt.val.visit(this, null);

		// Can't assign value to 'this'
		if (stmt.ref instanceof ThisRef) {
			error("Cannot use 'this' keyword in assignment statement.", stmt.position);
		}

		// Can't assign value to method
		if (stmt.ref.decl instanceof MethodDecl) {
			error("Cannot assign value to method.", stmt.position);
		}

		// null can be assigned to any object
		if (valType.typeKind == TypeKind.NULL) {
			return null;
		}

		// Check if types match
		if (!refType.equals(valType)) {
			error("Expected " + refType.typeKind + " but found " + valType.typeKind + ".", stmt.position);
		}

		return null;
	}

	public TypeDenoter visitCallStmt(CallStmt stmt, Object o) {
		stmt.methodRef.visit(this, null);

		if (!(stmt.methodRef.decl instanceof MethodDecl)) {
			error("Can only call methods.", stmt.methodRef.position);
			return null;
		}

		MethodDecl md = (MethodDecl) stmt.methodRef.decl;
		ExprList args = stmt.argList;
		ParameterDeclList params = md.parameterDeclList;

		// Check if argument list and parameter list are of same size
		if (params.size() != args.size()) {
			error("Expected " + params.size() + " parameters but found " + args.size() + " arguments.", stmt.position);
			return null;
		}

		// Check if arguments match parameters
		for (int i = 0; i < params.size(); i++) {

			ParameterDecl param = params.get(i);
			Expression arg = args.get(i);
			TypeDenoter argType = arg.visit(this, null);
			TypeDenoter paramType = param.type;

			if (!argType.equals(paramType)) {
				error("Expected parameter " + param.name + " of type " + paramType.typeKind
						+ " but found argument of type " + argType.typeKind, arg.position);
			}
		}

		return null;
	}

	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object o) {
		// Return statements may not contain return expressions
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
		}
		return null;
	}

	public TypeDenoter visitIfStmt(IfStmt stmt, Object o) {

		// Check that condition evaluates to boolean
		TypeDenoter condType = stmt.cond.visit(this, null);
		if (condType.typeKind != TypeKind.BOOLEAN) {
			error("Expected condition to be boolean but found " + condType.typeKind + ".", condType.position);
		}

		// Variable declarations shouldn't be the only statements in branches of
		// conditional statements
		if (stmt.thenStmt instanceof VarDeclStmt) {
			error("A variable declaration cannot be the only statement in a branch of a conditional statement.",
					stmt.thenStmt.position);
		}
		stmt.thenStmt.visit(this, null);

		// If there's an else statement, check the same as above
		if (stmt.elseStmt != null) {
			if (stmt.elseStmt instanceof VarDeclStmt) {
				error("A variable declaration cannot be the only statement in a branch of a conditional statement.",
						stmt.elseStmt.position);
			}
			stmt.elseStmt.visit(this, null);
		}

		return null;
	}

	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object o) {

		// Check that condition evaluates to boolean
		TypeDenoter condType = stmt.cond.visit(this, null);
		if (condType.typeKind != TypeKind.BOOLEAN) {
			error("Expected condition to be boolean but found " + condType.typeKind + ".", condType.position);
		}

		// Variable declarations shouldn't be the only statements in branches of
		// conditional statements
		if (stmt.body instanceof VarDeclStmt) {
			error("A variable declaration cannot be the only statement in a branch of a conditional statement.",
					stmt.body.position);
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

		// Check that operator type matches expression type
		TypeDenoter exprType = expr.expr.visit(this, null);
		TypeDenoter opType = expr.operator.visit(this, null);

		if (!exprType.equals(opType)) {
			error("Invalid operator type " + opType + " for expression type " + exprType + ".", expr.position);
		}

		return opType;
	}

	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object o) {

		TypeDenoter leftType = expr.left.visit(this, null);
		TypeDenoter rightType = expr.right.visit(this, null);

		switch (expr.operator.spelling) {

		// Operands must be of same type (but can be of any type)
		case "==":
		case "!=":
			if (leftType.typeKind == TypeKind.NULL || rightType.typeKind == TypeKind.NULL) {
				break;
			}
			if (!leftType.equals(rightType)) {
				error("Operand types do not match.", expr.position);
			}
			break;

		// Operands must evaluate to booleans
		case "&&":
		case "||":
			if (leftType.typeKind != TypeKind.BOOLEAN) {
				error("Expected left operand to be boolean but found " + leftType.typeKind + ".", expr.left.position);
			}
			if (rightType.typeKind != TypeKind.BOOLEAN) {
				error("Expected right operand to be boolean but found " + rightType.typeKind + ".",
						expr.right.position);
			}
			break;

		// Operands must evaluate to ints
		default:
			if (leftType.typeKind != TypeKind.INT) {
				error("Expected left operand to be int but found " + leftType.typeKind + ".", expr.left.position);
			}
			if (rightType.typeKind != TypeKind.INT) {
				error("Expected right operand to be int but found " + rightType.typeKind + ".", expr.right.position);
			}
		}

		// Determine type of expression
		return expr.operator.visit(this, null);
	}

	public TypeDenoter visitRefExpr(RefExpr expr, Object o) {
		expr.type = expr.ref.visit(this, null);
		return expr.type;
	}

	public TypeDenoter visitCallExpr(CallExpr expr, Object o) {

		// Borrow logic from visitCallStmt
		TypeDenoter funcType = expr.functionRef.visit(this, null);

		// Can only call methods
		if (!(expr.functionRef.decl instanceof MethodDecl)) {
			error("Can only call methods.", expr.functionRef.position);
			return funcType;
		}

		MethodDecl md = (MethodDecl) expr.functionRef.decl;
		ExprList args = expr.argList;
		ParameterDeclList params = md.parameterDeclList;

		// Check if argument list and parameter list are of same size
		if (params.size() != args.size()) {
			error("Expected " + params.size() + " parameters but received " + args.size() + " arguments.",
					expr.position);
			return funcType;
		}

		// Check if arguments match parameters
		for (int i = 0; i < params.size(); i++) {

			ParameterDecl param = params.get(i);
			Expression arg = args.get(i);
			TypeDenoter argType = arg.visit(this, null);
			TypeDenoter paramType = param.type;

			if (!argType.equals(paramType)) {
				error("Expected parameter " + param.name + " of type " + paramType.typeKind
						+ " but received argument of type " + argType.typeKind, param.position);
			}
		}

		return funcType;
	}

	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object o) {
		expr.type = expr.lit.visit(this, null);
		return expr.type;
	}

	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object o) {
		return expr.classtype.visit(this, null);
	}

	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object o) {
		TypeDenoter eltType = expr.eltType.visit(this, null);

		// NewArrayExpr -> new ( int [Expression] | id [Expression] )
		if (eltType.typeKind != TypeKind.INT && eltType.typeKind != TypeKind.CLASS) {
			error("Invalid array type " + eltType.typeKind + ".", expr.position);
		}

		// Array size must be an int
		TypeDenoter sizeType = expr.sizeExpr.visit(this, null);
		if (sizeType.typeKind != TypeKind.INT) {
			error("Expected array size to be of type int but received " + sizeType.typeKind + ".",
					expr.sizeExpr.position);
		}

		return new ArrayType(eltType, expr.position);
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitThisRef(ThisRef ref, Object o) {
		return ref.decl.type;
	}

	public TypeDenoter visitIdRef(IdRef ref, Object o) {
		return ref.decl.type;
	}

	public TypeDenoter visitQRef(QualRef ref, Object o) {
		return ref.decl.type;
	}

	public TypeDenoter visitIxRef(IxRef ref, Object o) {
		TypeDenoter refType = ref.ref.visit(this, null);

		// Reference must point to an array type
		if (refType.typeKind != TypeKind.ARRAY) {
			error("Invalid indexed reference to non-array type " + refType.typeKind + ".", ref.ref.position);
			return new BaseType(TypeKind.ERROR, ref.position);
		}

		// Array index must be an integer
		TypeDenoter indexType = ref.indexExpr.visit(this, null);
		if (indexType.typeKind != TypeKind.INT) {
			error("Expected int but found " + indexType.typeKind + ".", ref.indexExpr.position);
		}

		// Check that reference points to an initialized array
		if (ref.decl == null) {
			// error("Invalid reference to uninitialized array", ref.position);
			return new BaseType(TypeKind.NULL, ref.position);
		}

		// Check that element type is either class or int
		switch (((ArrayType) refType).eltType.typeKind) {

		case CLASS:
			return ((ArrayType) ref.decl.type).eltType;

		case INT:
			return new BaseType(TypeKind.INT, ref.position);

		default:
			return new BaseType(TypeKind.UNSUPPORTED, ref.position);
		}
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	/////////////////////////////////////////////////////////////////////////////

	public TypeDenoter visitIdentifier(Identifier id, Object o) {
		return id.decl.type;
	}

	public TypeDenoter visitOperator(Operator op, Object o) {
		switch (op.spelling) {

		case "!=":
		case "==":
		case ">=":
		case "<=":
		case "<":
		case ">":
		case "||":
		case "&&":
		case "!":
			return new BaseType(TypeKind.BOOLEAN, op.position);

		case "+":
		case "-":
		case "*":
		case "/":
			return new BaseType(TypeKind.INT, op.position);

		default:
			return new BaseType(TypeKind.ERROR, op.position);
		}
	}

	public TypeDenoter visitIntLiteral(IntLiteral num, Object o) {
		return new BaseType(TypeKind.INT, num.position);
	}

	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object o) {
		return new BaseType(TypeKind.BOOLEAN, bool.position);
	}

	public TypeDenoter visitNullLiteral(NullLiteral nul, Object o) {
		return new BaseType(TypeKind.NULL, nul.position);
	}
}
