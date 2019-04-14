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
import miniJava.AbstractSyntaxTrees.ClassDeclList;
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
import miniJava.SyntacticAnalyzer.SourcePosition;
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
		buildEnvironment();
		prog.visit(this, null);
	}

	private void error(String message, SourcePosition position) {
		reporter.reportError("*** line " + position.line + ": " + message);
		throw new Error();
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// ENVIRONMENT
	//
	/////////////////////////////////////////////////////////////////////////////

	private void buildEnvironment() {
		ClassDeclList envClasses = new ClassDeclList();
		SourcePosition nullPos = new SourcePosition();

		/***
		 * class String { }
		 */
		FieldDeclList strFields = new FieldDeclList();
		MethodDeclList strMethods = new MethodDeclList();
		BaseType strType = new BaseType(TypeKind.UNSUPPORTED, nullPos);

		ClassDecl strClass = new ClassDecl("String", strFields, strMethods, nullPos);
		strClass.type = strType;
		envClasses.add(strClass);

		/**
		 * class _PrintStream { public void println(int n) {}; }
		 */
		FieldDeclList psFields = new FieldDeclList();
		MethodDeclList psMethods = new MethodDeclList();

		// public void println(int n) {};
		FieldDecl printField = new FieldDecl(false, false, new BaseType(TypeKind.VOID, nullPos), "println", nullPos);
		ParameterDeclList printParams = new ParameterDeclList();
		StatementList printStmts = new StatementList();

		printParams.add(new ParameterDecl(new BaseType(TypeKind.INT, nullPos), "n", nullPos));
		MethodDecl printMethod = new MethodDecl(printField, printParams, printStmts, nullPos);
		psMethods.add(printMethod);

		ClassDecl psClass = new ClassDecl("_PrintStream", psFields, psMethods, nullPos);
		envClasses.add(psClass);

		/**
		 * class System { public static _PrintStream out; }
		 */
		FieldDeclList sysFields = new FieldDeclList();
		MethodDeclList sysMethods = new MethodDeclList();
		ClassType psType = new ClassType(new Identifier(new Token(Token.CLASS, "_PrintStream", nullPos)), nullPos);

		// public static _PrintStream out;
		FieldDecl outField = new FieldDecl(false, true, psType, "out", nullPos);
		sysFields.add(outField);

		ClassDecl sysClass = new ClassDecl("System", sysFields, sysMethods, nullPos);
		envClasses.add(sysClass);

		for (ClassDecl cd : envClasses) {

			// Enter class declaration in scoped identification table
			boolean unique = table.enter(cd.name, cd);
			if (!unique)
				error("Identifier " + cd.name + " already declared.", cd.position);

			// Add member declarations in class-specific identification tables
			for (FieldDecl fd : cd.fieldDeclList) {
				unique = cd.table.enter(fd.name, fd);
				if (!unique)
					error("Identifier " + fd.name + " already declared.", fd.position);
			}

			for (MethodDecl md : cd.methodDeclList) {
				unique = cd.table.enter(md.name, md);
				if (!unique)
					error("Identifier " + md.name + " already declared.", md.position);
			}
		}
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
				error("Identifier " + cd.name + " already declared.", cd.position);

			// Add member declarations in class-specific identification tables
			for (FieldDecl fd : cd.fieldDeclList) {
				unique = cd.table.enter(fd.name, fd);
				if (!unique)
					error("Identifier " + fd.name + " already declared.", fd.position);
			}

			for (MethodDecl md : cd.methodDeclList) {
				unique = cd.table.enter(md.name, md);
				if (!unique)
					error("Identifier " + md.name + " already declared.", md.position);
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
		table.enter("this", currentClass);

		// Enter member declarations at L2
		table.openScope();

		// Declare all member names before visiting
		for (FieldDecl fd : cd.fieldDeclList) {
			boolean unique = table.enter(fd.name, fd);
			if (!unique)
				error("Identifier " + fd.name + " already declared.", fd.position);
		}

		for (MethodDecl md : cd.methodDeclList) {
			boolean unique = table.enter(md.name, md);
			if (!unique)
				error("Identifier " + md.name + " already declared.", md.position);
		}

		// Visit members
		for (FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, null);
		}

		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}

		table.closeScope();
		currentClass = null;
		table.remove("this");
		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, Object o) {
		fd.type.visit(this, null);
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, Object o) {
		currentMethod = md;
		md.type.visit(this, null);

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
			error("Identifier " + pd.name + " already declared.", pd.position);
		pd.type.visit(this, null);

		return null;
	}

	public Object visitVarDecl(VarDecl decl, Object o) {
		boolean unique = table.enter(decl.name, decl);
		if (!unique)
			error("Identifier " + decl.name + " already declared.", decl.position);
		decl.type.visit(this, null);

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

		// Check if class has been declared
		Declaration d = table.retrieve(type.className.spelling);

		if (d == null) {
			error("Undeclared class " + type.className.spelling + ".", type.position);
		} else if (d != null && d instanceof MethodDecl) {
			error("Undeclared class " + type.className.spelling + ".", type.position);
		}

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
		// Return statements may not contain return expressions
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
		}
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
		// Reference to "this" is an exception.
		if (d instanceof ClassDecl && !(expr.ref instanceof ThisRef)) {
			error("Invalid reference to class.", expr.ref.position);
		} else if (d instanceof MethodDecl) {
			error("Invalid reference to method.", expr.ref.position);
		}

		// Can't access non-static fields from a static method.
		if (!(expr.ref instanceof QualRef) && d instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl) d;
			if (currentMethod.isStatic && !fd.isStatic) {
				error("Cannot access non-static member " + fd.name + " from a static context.", expr.position);
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
			error("Cannot use 'this' in a static context.", ref.position);
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

		Declaration d = ref.ref.decl;
		ClassDecl cd = null;

		// Classes
		if (d instanceof ClassDecl) {
			cd = (ClassDecl) table.retrieve(d.name);
		}

		// Methods
		else if (d instanceof MethodDecl) {
			error("Invalid method usage in qualified reference.", ref.ref.position);
		}

		// Arrays
		// Can't refer to member of IxRef (ex: d[].x)
		else if (d.type instanceof ArrayType) {
			if (!ref.id.spelling.equals("length")) {
				error("Reference " + ref.ref.spelling + " of type " + d.type.typeKind + " does not have public member "
						+ ref.id.spelling + ".", ref.ref.position);
			}

			ref.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, ref.id.position), null, null);
			ref.id.decl = ref.decl;
			ref.spelling = ref.id.spelling;
			return null;
		}

		else if (!(d.type instanceof ClassType)) {
			error("Reference " + ref.ref.spelling + " of type " + d.type.typeKind + " does not have public member "
					+ ref.id.spelling + ".", ref.ref.position);
		}

		// Fields, Variables, Parameters
		else {
			ClassType ct = (ClassType) d.type;
			String cn = ct.className.spelling;
			cd = (ClassDecl) table.retrieve(cn);
		}

		MemberDecl md = (MemberDecl) cd.table.retrieve(ref.id.spelling);

		if (md == null) {
			error("Class " + cd.name + " does not contain member " + ref.id.spelling + ".", ref.position);
		}

		// Can't access private members from outside their containing class.
		if (md.isPrivate && cd != currentClass) {
			error("Cannot access private member " + md.name + ".", ref.position);
		}

		// Within a static method in a class C, a reference cannot directly access a
		// non-static member of class C (but it can access the member through an
		// instance of the class).
		if (currentMethod != null && currentMethod.isStatic) {
			boolean instantiated = false;

			// Check if the reference's spelling is the same as the class' spelling:
			// Ex: A.x -> is A referring to an instance?
			if (d.type instanceof ClassType) {
				ClassType ct = (ClassType) d.type;
				String refName = ref.ref.spelling;
				String className = ct.className.spelling;
				instantiated = !refName.equals(className);
			}

			// Check if member was declared in current class
			boolean memberInCurrentClass = currentClass.table.retrieve(md.name) != null;
			if (memberInCurrentClass && !md.isStatic && !instantiated) {
				error("Cannot access non-static member " + md.name + " from static context.", md.position);
			}
		}

		ref.decl = md;
		ref.id.decl = md;
		ref.spelling = ref.id.spelling;
		return null;
	}

	public Object visitIxRef(IxRef ref, Object o) {
		ref.ref.visit(this, null);
		ref.indexExpr.visit(this, null);
		ref.decl = ref.ref.decl;
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
			error("Cannot use variable " + id.spelling + " in its own declaration.", id.position);
		}

		// Check for array length
		if (id.spelling.equals("length")) {
			id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, id.position), null, null);
			return null;
		}

		// Find and attach corresponding declaration
		Declaration d = table.retrieve(id.spelling);

		if (d == null) {
			error("Cannot reference undeclared variable " + id.spelling + ".", id.position);
		}

		id.decl = d;

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
