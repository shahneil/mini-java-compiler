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
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxRef;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
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
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;

// TODO: Implement position in parser.
public class Identification implements Visitor<Object, Object> {

	private ErrorReporter reporter;
	private IdentificationTable table; // Scoped identification table for traversal
	private ClassDecl currentClass;
	private MethodDecl currentMethod;
	private VarDecl currentVar;

	public Identification(ErrorReporter reporter) {
		this.reporter = reporter;
		this.table = new IdentificationTable();
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitPackage(Package prog, Object o) {

		// TODO: Build standard environment at L0

		// Declare program classes at L1
		table.openScope();

		for (ClassDecl cd : prog.classDeclList) {

			// Enter class declaration in scoped identification table
			boolean unique = table.enter(cd.name, cd);
			if (!unique)
				reporter.reportError("Identifier " + cd.name + "already declared at " + cd.position);

			// Add member declarations in class-specific identification tables
			for (FieldDecl fd : cd.fieldDeclList) {
				unique = cd.table.enter(fd.name, fd);
				if (!unique)
					reporter.reportError("Identifier " + fd.name + "already declared at " + fd.position);
			}

			for (MethodDecl md : cd.methodDeclList) {
				unique = cd.table.enter(md.name, md);
				if (!unique)
					reporter.reportError("Identifier " + md.name + "already declared at " + md.position);
			}
		}

		for (ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}

		table.closeScope();
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitClassDecl(ClassDecl cd, Object o) {
		currentClass = cd;

		// Enter member declarations at L2
		table.openScope();
		for (FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, null);
		}

		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}
		table.closeScope();
		currentClass = null;
		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, Object o) {
		boolean unique = table.enter(fd.name, fd);
		if (!unique)
			reporter.reportError("Identifier " + fd.name + "already declared at " + fd.position);
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, Object o) {
		boolean unique = table.enter(md.name, md);
		if (!unique)
			reporter.reportError("Identifier " + md.name + "already declared at " + md.position);

		currentMethod = md;

		// Add parameter names at L3
		table.openScope();
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, null);
		}

		// Add local variable names at L4+
		table.openScope();
		for (Statement s : md.statementList) {
			s.visit(this, null);
		}
		table.closeScope();

		table.closeScope();
		currentMethod = null;
		return null;
	}

	public Object visitParameterDecl(ParameterDecl pd, Object o) {
		boolean unique = table.enter(pd.name, pd);
		if (!unique)
			reporter.reportError("Identifier " + pd.name + "already declared at " + pd.position);
		return null;
	}

	public Object visitVarDecl(VarDecl decl, Object o) {
		boolean unique = table.enter(decl.name, decl);
		if (!unique)
			reporter.reportError("Identifier " + decl.name + "already declared at " + decl.position);

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
		// Visit the identifier
		type.className.visit(this, null);
		return null;
	}

	public Object visitArrayType(ArrayType type, Object o) {
		// Visit the identifier
		type.eltType.visit(this, null);
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitBlockStmt(BlockStmt stmt, Object o) {
		table.openScope();
		for (Statement s : stmt.sl) {
			s.visit(this, null);
		}
		table.closeScope();
		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, Object o) {
		stmt.varDecl.visit(this, null);
		currentVar = stmt.varDecl;
		stmt.initExp.visit(this, null);
		currentVar = null;
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, Object o) {
		stmt.ref.visit(this, null);
		stmt.val.visit(this, null);
		return null;
	}

	public Object visitCallStmt(CallStmt stmt, Object o) {
		stmt.methodRef.visit(this, null);
		for (Expression e : stmt.argList) {
			e.visit(this, null);
		}
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, Object o) {
		stmt.returnExpr.visit(this, null);
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, Object o) {
		stmt.cond.visit(this, null);
		stmt.thenStmt.visit(this, null);
		if (stmt.elseStmt != null)
			stmt.elseStmt.visit(this, null);
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, Object o) {
		stmt.cond.visit(this, null);
		stmt.body.visit(this, null);
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitUnaryExpr(UnaryExpr expr, Object o) {
		expr.expr.visit(this, null);
		expr.operator.visit(this, null);
		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, Object o) {
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		expr.operator.visit(this, null);
		return null;
	}

	public Object visitRefExpr(RefExpr expr, Object o) {
		expr.ref.visit(this, null);
		return null;
	}

	public Object visitCallExpr(CallExpr expr, Object o) {
		expr.functionRef.visit(this, null);
		for (Expression e : expr.argList) {
			e.visit(this, null);
		}
		return null;
	}

	public Object visitLiteralExpr(LiteralExpr expr, Object o) {
		expr.lit.visit(this, null);
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, Object o) {
		expr.classtype.visit(this, null);
		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, Object o) {
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, null);
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitThisRef(ThisRef ref, Object o) {
		ref.decl = currentClass;
		ref.spelling = "this";
		if (currentMethod.isStatic) {
			reporter.reportError("*** Cannot use 'this' in a static context at " + ref.position + ".");
		}
		return null;
	}

	public Object visitIdRef(IdRef ref, Object o) {
		ref.id.visit(this, null);
		ref.decl = ref.id.decl;
		ref.spelling = ref.id.spelling;
		return null;
	}

	public Object visitQRef(QualRef ref, Object o) {
		ref.ref.visit(this, null);

		// We know d links to the class declaration, but we need the id spelling.
		// Ex: A.x vs. a.x vs. this.x
		Declaration d = table.retrieve(ref.ref.spelling);
		ClassType ct = (ClassType) ref.ref.decl.type;
		ClassDecl cd = (ClassDecl) table.retrieve(ct.className.spelling);

		if (cd == null) {
			reporter.reportError("*** Class " + ct.className.spelling + " does not exist at " + ct.position);
		}

		MemberDecl md = (MemberDecl) cd.table.retrieve(ref.id.spelling);

		// Prevent qualified references of the form a().b
		if (d instanceof MethodDecl) {
			reporter.reportError("*** Invalid usage of method in qualified reference at " + ref.ref.position);
		}

		// Access and Visibility restrictions
		if (!md.isStatic && d instanceof ClassDecl) {
			reporter.reportError(
					"*** Cannot access non-static member " + md.name + " from a static context at " + ref.id.position);
		}

		if (md.isPrivate && cd != currentClass) {
			reporter.reportError("*** Cannot access private member " + md.name + " at " + ref.id.position);
		}

		ref.decl = md;
		ref.id.decl = ref.decl;
		ref.spelling = ref.id.spelling;
		return null;
	}

	public Object visitIxRef(IxRef ref, Object o) {
		ref.ref.visit(this, null);
		ref.indexExpr.visit(this, null);
		ref.spelling = ref.ref.spelling;
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitIdentifier(Identifier id, Object o) {

		// Make sure variables aren't referenced in their declarations.
		if (currentVar != null && currentVar.name.equals(id.spelling)) {
			reporter.reportError(
					"*** Cannot reference variable " + id.spelling + " in its declaration at " + id.position);
		}

		// Find and attach corresponding declaration
		Declaration d = table.retrieve(id.spelling);
		if (d == null) {
			reporter.reportError("Cannot reference undeclared variable " + id.spelling + " at " + id.position);
		} else {
			id.decl = d;
		}

		// Access (since we are within a class, don't need to check Visibility)
		if (d instanceof MemberDecl) {
			MemberDecl md = (MemberDecl) d;
			if (currentMethod.isStatic && !md.isStatic) {
				reporter.reportError("Cannot access non-static method " + md.name + " from static method "
						+ currentMethod.name + " at " + id.position);
			}
		}

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
