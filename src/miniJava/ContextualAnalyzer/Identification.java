package miniJava.ContextualAnalyzer;

import miniJava.CompilationError;
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
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxRef;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
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
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.Token;

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

	public void run(AST prog) {
		// Build environment at L0
		table.openScope();
		buildEnvironment();
		prog.visit(this, null);
		table.closeScope();
	}

	private void error(String message) {
		reporter.reportError("*** " + message);
		throw new CompilationError();
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// ENVIRONMENT
	//
	/////////////////////////////////////////////////////////////////////////////

	private void buildEnvironment() {
		/***
		 * class String { }
		 */
		FieldDeclList StringFDL = new FieldDeclList();
		MethodDeclList StringMDL = new MethodDeclList();
		ClassDecl StringDecl = new ClassDecl("String", StringFDL, StringMDL, null);
		StringDecl.type = new BaseType(TypeKind.UNSUPPORTED, null);
		table.enter("String", StringDecl);

		/**
		 * class _PrintStream { public void println(int n) {}; }
		 */
		FieldDeclList _PrintStreamFDL = new FieldDeclList();
		MethodDeclList _PrintStreamMDL = new MethodDeclList();

		// public void println(int n) {};
		FieldDecl _PrintStreamPrintlnFD = new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println",
				null);
		ParameterDeclList _PrintStreamPrintlnPDL = new ParameterDeclList();
		_PrintStreamPrintlnPDL.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
		StatementList _PrintStreamPrintlnSDL = new StatementList();
		MethodDecl _PrintStreamPrintlnMD = new MethodDecl(_PrintStreamPrintlnFD, _PrintStreamPrintlnPDL,
				_PrintStreamPrintlnSDL, null);
		_PrintStreamMDL.add(_PrintStreamPrintlnMD);

		ClassDecl _PrintStreamDecl = new ClassDecl("_PrintStream", _PrintStreamFDL, _PrintStreamMDL, null);
		table.enter("_PrintStream", _PrintStreamDecl);

		/**
		 * class System { public static _PrintStream out; }
		 */
		FieldDeclList SystemFDL = new FieldDeclList();
		MethodDeclList SystemMDL = new MethodDeclList();

		// public static _PrintStream out;
		ClassType _PrintStreamCT = new ClassType(new Identifier(new Token(Token.CLASS, "_PrintStream", null)), null);
		FieldDecl SystemOut = new FieldDecl(false, true, _PrintStreamCT, "out", null);
		SystemFDL.add(SystemOut);

		ClassDecl SystemDecl = new ClassDecl("System", SystemFDL, SystemMDL, null);
		table.enter("System", SystemDecl);
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitPackage(Package prog, Object o) {

		// Declare program classes at L1
		table.openScope();

		for (ClassDecl cd : prog.classDeclList) {

			// Enter class declaration in scoped identification table
			boolean unique = table.enter(cd.name, cd);
			if (!unique)
				error("Identifier " + cd.name + " already declared at " + cd.position);

			// Add member declarations in class-specific identification tables
			for (FieldDecl fd : cd.fieldDeclList) {
				unique = cd.table.enter(fd.name, fd);
				if (!unique)
					error("Identifier " + fd.name + " already declared at " + fd.position);
			}

			for (MethodDecl md : cd.methodDeclList) {
				unique = cd.table.enter(md.name, md);
				if (!unique)
					error("Identifier " + md.name + " already declared at " + md.position);
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
			error("Identifier " + fd.name + " already declared at " + fd.position);
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, Object o) {
		boolean unique = table.enter(md.name, md);
		if (!unique)
			error("Identifier " + md.name + " already declared at " + md.position);

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
			error("Identifier " + pd.name + " already declared at " + pd.position);
		return null;
	}

	public Object visitVarDecl(VarDecl decl, Object o) {
		boolean unique = table.enter(decl.name, decl);
		if (!unique)
			error("Identifier " + decl.name + " already declared at " + decl.position);

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

		Declaration d = table.retrieve(expr.ref.spelling);

		// Can't reference classes or method names
		// Ex: for (...) { System; }
		if (d instanceof ClassDecl) {
			error("Invalid reference to class at " + expr.ref.position);
		} else if (d instanceof MethodDecl) {
			error("Invalid reference to method at " + expr.ref.position);
		}

		// If we are inside a static method, we cannot access non-static members of the
		// current class.
		if (!(expr.ref instanceof QualRef) && d instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl) d;
			if (currentMethod.isStatic && !fd.isStatic) {
				error("Cannot access non-static member from a static context at " + expr.position);
			}
		}

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
			error("Cannot use 'this' in a static context at " + ref.position + ".");
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

		if (d == null) {
			error("Class " + ref.ref.spelling + " does not exist at " + ref.ref.position);
		}

		ClassDecl cd = (ClassDecl) d;
		MemberDecl md = (MemberDecl) cd.table.retrieve(ref.id.spelling);

		// Prevent qualified references of the form a().b
		if (d instanceof MethodDecl) {
			error("Invalid usage of method in qualified reference at " + ref.ref.position);
		}

		// Access and Visibility restrictions
		if (!md.isStatic && d instanceof ClassDecl) {
			error("Cannot access non-static member " + md.name + " from a static context at " + ref.id.position);
		}

		if (md.isPrivate && cd != currentClass) {
			error("Cannot access private member " + md.name + " at " + ref.id.position);
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
			error("Cannot reference variable " + id.spelling + " in its declaration at " + id.position);
		}

		// Find and attach corresponding declaration
		Declaration d = table.retrieve(id.spelling);
		if (d == null) {
			error("Cannot reference undeclared variable " + id.spelling + " at " + id.position);
		} else {
			id.decl = d;
		}

		// Access (since we are within a class, don't need to check Visibility)
		if (d instanceof MemberDecl) {
			MemberDecl md = (MemberDecl) d;
			if (currentMethod.isStatic && !md.isStatic) {
				error("Cannot access non-static method " + md.name + " from static method " + currentMethod.name
						+ " at " + id.position);
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
