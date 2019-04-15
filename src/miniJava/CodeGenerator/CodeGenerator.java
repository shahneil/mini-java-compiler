package miniJava.CodeGenerator;

import static mJAM.Machine.Op.CALL;
import static mJAM.Machine.Op.CALLI;
import static mJAM.Machine.Op.HALT;
import static mJAM.Machine.Op.JUMP;
import static mJAM.Machine.Op.JUMPIF;
import static mJAM.Machine.Op.LOAD;
import static mJAM.Machine.Op.LOADA;
import static mJAM.Machine.Op.LOADL;
import static mJAM.Machine.Op.POP;
import static mJAM.Machine.Op.PUSH;
import static mJAM.Machine.Op.RETURN;
import static mJAM.Machine.Op.STORE;
import static mJAM.Machine.Reg.CB;
import static mJAM.Machine.Reg.LB;
import static mJAM.Machine.Reg.OB;
import static mJAM.Machine.Reg.SB;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.Machine;
import mJAM.Machine.Prim;
import mJAM.ObjectFile;
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
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.Token;

public class CodeGenerator implements Visitor<Integer, Integer> {

	private ErrorReporter reporter;
	private String objectCodeFileName;
	private String asmCodeFileName;
	private boolean debug = false;

	private boolean foundMain;
	private PatchList patchList;
	private int mainAddr;
	private int frameOffset;

	public CodeGenerator(String sourceName, ErrorReporter reporter) {
		this.reporter = reporter;
		this.frameOffset = 0;
		this.patchList = new PatchList();
		this.foundMain = false;
		objectCodeFileName = sourceName.substring(0, sourceName.indexOf('.')) + ".mJAM";
		asmCodeFileName = objectCodeFileName.replace(".mJAM", ".asm");
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// DRIVER
	//
	/////////////////////////////////////////////////////////////////////////////

	public void generate(AST prog) {
		Machine.initCodeGen();
		prog.visit(this, null);
		patchList.patch();
		generateObjectCode();
		generateAssembly();
		if (debug) {
			runDebugger();
		}
	}

	private void generateObjectCode() {
		ObjectFile objectFile = new ObjectFile(objectCodeFileName);
		print("Generating object code file " + objectCodeFileName + "...");

		if (objectFile.write()) {
			error("Failed to generate object code file.");
		} else {
			print("Generated object code file " + objectCodeFileName + "...");
		}
	}

	private void generateAssembly() {
		print("Generating assembly file " + asmCodeFileName + "...");
		Disassembler d = new Disassembler(objectCodeFileName);

		if (d.disassemble()) {
			error("Failed to generate assembly file.");
		} else {
			print("Generated assembly file " + asmCodeFileName + "...");
		}
	}

	private void runDebugger() {
		print("Running code in debugger...");
		Interpreter.debug(objectCodeFileName, asmCodeFileName);
		print("Finished running code.");
	}

	private void error(String message) {
		reporter.reportError(message);
		throw new Error();
	}

	private void print(String message) {
		System.out.println(message);
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	/////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitPackage(Package prog, Integer arg) {

		// Load static variables
		int staticOffset = 0;
		for (ClassDecl cd : prog.classDeclList) {
			for (FieldDecl fd : cd.fieldDeclList) {
				if (fd.isStatic) {
					Machine.emit(PUSH, 1);
					fd.entity = new KnownAddress(Machine.characterSize, staticOffset);
					staticOffset++;
				}
			}
		}

		// Record address where main is called
		mainAddr = Machine.nextInstrAddr();

		// Call main (patch)
		Machine.emit(CALL, CB, -1);

		// End execution
		Machine.emit(HALT, 0, 0, 0);

		// Decorate fields
		for (ClassDecl cd : prog.classDeclList) {
			int fieldOffset = 0;
			for (FieldDecl fd : cd.fieldDeclList) {
				fd.visit(this, fieldOffset);
				fieldOffset++;
			}
		}

		// Visit classes
		for (ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}

		if (!foundMain) {
			error("A miniJava program must contain a public static void main method.");
		}

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitClassDecl(ClassDecl cd, Integer arg) {

		// Visit methods
		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}

		return null;
	}

	@Override
	public Integer visitFieldDecl(FieldDecl fd, Integer offset) {

		// Decorate non-static fields
		if (!fd.isStatic) {
			fd.entity = new KnownAddress(Machine.characterSize, offset);
		}

		return null;
	}

	@Override
	public Integer visitMethodDecl(MethodDecl md, Integer arg) {

		// Method code address
		md.entity = new KnownAddress(Machine.addressSize, Machine.nextInstrAddr());

		// Check for main method (doesn't check arguments/visibility/access)
		if (md.name.equals("main")) {

			// Check for duplicate main method
			if (foundMain) {
				error("A miniJava program can only contain one main method.");
			}

			// Check for public, static, void, and one array type arg
			if (!md.isPrivate && md.isStatic && md.type.typeKind == TypeKind.VOID && md.parameterDeclList.size() == 1
					&& md.parameterDeclList.get(0).type instanceof ArrayType) {
				foundMain = true;
			}

			Machine.patch(mainAddr, Machine.nextInstrAddr());
		}

		// Decorate parameters
		int numParams = md.parameterDeclList.size();
		int paramOffset = -1 * numParams;
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, paramOffset);
			paramOffset++;
		}

		// Statements
		// Reserve space for dynamic link (LB) and return address (RB)
		// Static link is unused
		frameOffset = 3;
		for (Statement s : md.statementList) {
			s.visit(this, null);
		}

		// If the method is void, pop parameters.
		// Else, pop result and parameters, then push result back onto the stack.
		if (md.type.typeKind == TypeKind.VOID) {
			Machine.emit(RETURN, 0, 0, numParams);
		} else {
			Machine.emit(RETURN, 1, 0, numParams);
		}

		return null;
	}

	@Override
	public Integer visitParameterDecl(ParameterDecl pd, Integer offset) {
		pd.entity = new KnownAddress(Machine.addressSize, offset);
		return null;
	}

	@Override
	public Integer visitVarDecl(VarDecl decl, Integer arg) {
		decl.entity = new KnownAddress(Machine.characterSize, frameOffset);
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	/////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitBaseType(BaseType type, Integer arg) {
		return null;
	}

	@Override
	public Integer visitClassType(ClassType type, Integer arg) {
		return null;
	}

	@Override
	public Integer visitArrayType(ArrayType type, Integer arg) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitBlockStmt(BlockStmt stmt, Integer arg) {

		// Count variables
		int numVars = 0;
		for (Statement s : stmt.sl) {
			if (s instanceof VarDeclStmt) {
				numVars++;
			}
			s.visit(this, null);
		}

		// Pop variables
		Machine.emit(POP, 0, 0, numVars);

		// Reset frame offset
		frameOffset -= numVars;

		return null;
	}

	@Override
	public Integer visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, 1);
		frameOffset++;
		return null;
	}

	@Override
	public Integer visitAssignStmt(AssignStmt stmt, Integer arg) {
		Reference r = stmt.ref;
		Expression v = stmt.val;
		Declaration d = r.decl;
		int offset;

		// IdRef
		if (r instanceof IdRef) {
			// Get location of attached declaration
			offset = r.visit(this, 2);

			// Evaluate expression and push result onto stack
			v.visit(this, 1);

			if (d instanceof FieldDecl) {
				FieldDecl fd = (FieldDecl) d;

				// Update static field in global segment
				if (fd.isStatic) {
					Machine.emit(STORE, SB, offset);
				}

				// Update current object instance in heap
				else {
					Machine.emit(STORE, OB, offset);
				}
			} else {
				// Update local variable/parameter
				Machine.emit(STORE, LB, offset);
			}
		}

		// IxRef
		else if (r instanceof IxRef) {
			// Push address of array object (a) and element index (i) onto stack
			r.visit(this, 2);

			// Push new value (v) onto stack
			v.visit(this, 1);

			// Update according to stack operands.
			// a[i] = v;
			Machine.emit(Prim.arrayupd);
		}

		// QualRef
		else if (r instanceof QualRef) {

			// Update static field
			if (d instanceof FieldDecl && ((FieldDecl) d).isStatic) {
				// Find location of field, evaluate expression and push result onto stack
				offset = r.visit(this, 2);
				v.visit(this, 1);

				// Update static field in global segment
				Machine.emit(STORE, SB, offset);
			}

			// Update object instance
			else {
				// Push address of object (a) and field index (i)
				r.visit(this, 2);

				// Push new value (v)
				v.visit(this, 1);

				// Update according to stack operands.
				// a.i = v;
				Machine.emit(Prim.fieldupd);
			}
		}

		return null;
	}

	@Override
	public Integer visitCallStmt(CallStmt stmt, Integer arg) {
		Reference r = stmt.methodRef;
		MethodDecl md = (MethodDecl) stmt.methodRef.decl;

		// Load arguments onto stack
		for (Expression a : stmt.argList) {
			a.visit(this, 1);
		}

		// Println
		if (r instanceof QualRef && md.name.equals("println")) {
			Machine.emit(Prim.putintnl);
		}

		// IdRef
		// Ex: a()
		else if (r instanceof IdRef) {

			// Static method
			if (md.isStatic) {
				patchList.add(Machine.nextInstrAddr(), md);
				Machine.emit(CALL, CB, -1);
			}

			// Instance method
			else {
				// Push address of current object instance (this) onto stack
				Machine.emit(LOADA, OB, 0);
				patchList.add(Machine.nextInstrAddr(), md);
				Machine.emit(CALLI, CB, -1);
			}
		}

		// QualRef
		// Ex: a.b()
		else {
			// Push address of object instance onto stack
			// For a.b() -> get location of a (handle in visitQRef)
			// For a.b.c() -> get location of b (handle in visitQRef)
			r.visit(this, 1);
			patchList.add(Machine.nextInstrAddr(), md);
			Machine.emit(CALLI, CB, -1);
		}

		// If method is not void, pop the unused return value
		if (md.type.typeKind != TypeKind.VOID) {
			Machine.emit(POP, 0, 0, 1);
		}

		return null;
	}

	@Override
	public Integer visitReturnStmt(ReturnStmt stmt, Integer arg) {
		// If the return statement isn't empty,
		// evaluate the return expression and push the result onto the stack
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, 1);
		}
		return null;
	}

	@Override
	public Integer visitIfStmt(IfStmt stmt, Integer arg) {

		// Condition
		stmt.cond.visit(this, 1); // Evaluate condition and push result onto stack
		int condAddr = Machine.nextInstrAddr();
		Machine.emit(JUMPIF, 0, CB, -1); // Jump to else (patch) if false

		// Then
		stmt.thenStmt.visit(this, null); // Execute then statement
		int thenAddr = Machine.nextInstrAddr();
		Machine.emit(JUMP, CB, -1); // Jump to end (patch)

		// Else
		int elseAddr = Machine.nextInstrAddr();
		Machine.patch(condAddr, elseAddr); // Patch address of else clause
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, null);
		}

		// End
		int endAddr = Machine.nextInstrAddr();
		Machine.patch(thenAddr, endAddr); // Patch address of end

		return null;
	}

	@Override
	public Integer visitWhileStmt(WhileStmt stmt, Integer arg) {

		// Condition
		int condAddr = Machine.nextInstrAddr();
		stmt.cond.visit(this, 1); // Evaluate condition and push result onto stack
		int bodyAddr = Machine.nextInstrAddr();
		Machine.emit(JUMPIF, 0, CB, -1); // Jump to end (patch) if false

		// Body
		stmt.body.visit(this, null);
		Machine.emit(JUMP, CB, condAddr); // Jump to condition

		// End
		int endAddr = Machine.nextInstrAddr();
		Machine.patch(bodyAddr, endAddr); // Patch address of end

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitUnaryExpr(UnaryExpr expr, Integer arg) {

		// Evaluate inner expression and push result onto stack
		expr.expr.visit(this, 1);

		// Check operator and execute appropriate instructions
		if (expr.operator != null) {
			switch (expr.operator.kind) {
			case Token.MINUS:
				Machine.emit(Prim.neg);
				break;
			case Token.NOT:
				Machine.emit(Prim.not);
				break;
			default:
				error("Invalid unary operator at " + expr.operator.position + ".");
				break;
			}
		}

		return null;
	}

	@Override
	public Integer visitBinaryExpr(BinaryExpr expr, Integer arg) {
		Expression l = expr.left;
		Expression r = expr.right;
		Operator o = expr.operator;
		int endAddr;

		switch (o.kind) {

		case Token.AND:
			// Evaluate left expression and push result
			l.visit(this, 1);

			// Jump to short circuit if left is false
			int andAddr = Machine.nextInstrAddr();
			Machine.emit(JUMPIF, 0, CB, -1); // Jump to short circuit (patch)

			// Evaluate right expression and execute AND routine
			// Push 1 onto stack since left is true (previous JUMPIF instruction
			// popped result of left expression evaluation)
			Machine.emit(LOADL, 1);
			r.visit(this, 1);
			Machine.emit(Prim.and);
			endAddr = Machine.nextInstrAddr();
			Machine.emit(JUMP, CB, -1); // Jump to end (patch)

			// Short circuit: Push 0 (false) if left is false
			Machine.patch(andAddr, Machine.nextInstrAddr());
			Machine.emit(LOADL, 0);

			// End
			Machine.patch(endAddr, Machine.nextInstrAddr());
			return null;

		case Token.OR:

			// Evaluate left expression and push result
			l.visit(this, 1);

			// Jump to short circuit if left is true
			int orAddr = Machine.nextInstrAddr();
			Machine.emit(JUMPIF, 1, CB, -1); // Jump to short circuit (patch)

			// Evaluate right expression and execute OR routing
			// Push 0 onto stack since left is false (previous JUMPIF instruction
			// popped result of left expression evaluation)
			Machine.emit(LOADL, 0);
			r.visit(this, 1);
			Machine.emit(Prim.or);
			endAddr = Machine.nextInstrAddr();
			Machine.emit(JUMP, CB, -1); // Jump to end (patch)

			// Short circuit: Push 1 (true) if left is true
			Machine.patch(orAddr, Machine.nextInstrAddr());
			Machine.emit(LOADL, 1);

			// End
			Machine.patch(endAddr, Machine.nextInstrAddr());
			return null;

		default:

			// Evaluate both expressions and push results
			l.visit(this, 1);
			r.visit(this, 1);

			// Execute appropriate primitive routine
			switch (o.kind) {
			case Token.ADD:
				Machine.emit(Prim.add);
				break;
			case Token.MINUS:
				Machine.emit(Prim.sub);
				break;
			case Token.MULT:
				Machine.emit(Prim.mult);
				break;
			case Token.DIV:
				Machine.emit(Prim.div);
				break;
			case Token.EQ:
				Machine.emit(Prim.eq);
				break;
			case Token.NEQ:
				Machine.emit(Prim.ne);
				break;
			case Token.GT:
				Machine.emit(Prim.gt);
				break;
			case Token.GTE:
				Machine.emit(Prim.ge);
				break;
			case Token.LT:
				Machine.emit(Prim.lt);
				break;
			case Token.LTE:
				Machine.emit(Prim.le);
				break;
			default:
				error("Invalid operator at " + o.position);
				break;
			}
		}

		return null;
	}

	@Override
	public Integer visitRefExpr(RefExpr expr, Integer arg) {
		// Visit attached reference
		expr.ref.visit(this, arg);
		return null;
	}

	@Override
	public Integer visitCallExpr(CallExpr expr, Integer arg) {
		Reference r = expr.functionRef;
		MethodDecl md = (MethodDecl) r.decl;

		// Load arguments onto stack
		for (Expression a : expr.argList) {
			a.visit(this, 1);
		}

		// Println
		if (r instanceof QualRef && md.name.equals("println")) {
			Machine.emit(Prim.putintnl);
		}

		// IdRef
		// Ex: a()
		else if (r instanceof IdRef) {

			// Static method
			if (md.isStatic) {
				patchList.add(Machine.nextInstrAddr(), md);
				Machine.emit(CALL, CB, -1);
			}

			// Instance method
			else {
				// Push address of current object instance (this) onto stack
				Machine.emit(LOADA, OB, 0);
				patchList.add(Machine.nextInstrAddr(), md);
				Machine.emit(CALLI, CB, -1);
			}
		}

		// QualRef
		// Ex: a.b()
		else {
			// Push address of object instance onto stack
			r.visit(this, 1);
			patchList.add(Machine.nextInstrAddr(), md);
			Machine.emit(CALLI, CB, -1);
		}

		return null;
	}

	@Override
	public Integer visitLiteralExpr(LiteralExpr expr, Integer arg) {

		// Literal value to be loaded (arbitrary initial value)
		int value = 0;

		switch (expr.lit.kind) {
		case Token.NUM:
			value = Integer.parseInt(expr.lit.spelling);
			break;
		case Token.TRUE:
			value = 1;
			break;
		case Token.FALSE:
		case Token.NULL:
			value = 0;
			break;
		default:
			error("Unrecognized literal expression at " + expr.position);
		}

		// Push literal onto stack
		Machine.emit(LOADL, value);
		return null;
	}

	@Override
	public Integer visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
		ClassDecl cd = (ClassDecl) expr.classtype.className.decl;

		// Without inheritance, no class object is needed (therefore -1).
		Machine.emit(LOADL, -1);

		// Push size (n) (# of fields)
		Machine.emit(LOADL, cd.fieldDeclList.size());

		// Allocate new object and push its address
		// 1st word = -1 (no class object)
		// 2nd word = n (# of fields)
		// Remaining words = fields (initialized to 0)
		Machine.emit(Prim.newobj);
		return null;
	}

	@Override
	public Integer visitNewArrayExpr(NewArrayExpr expr, Integer arg) {

		// Push number of elements (n)
		expr.sizeExpr.visit(this, 1);

		// Allocate new array and push address of first element
		// 1st word = -2 (array indicator)
		// 2nd word = n (array length)
		// Remaining words = array elements (initialized to 0)
		Machine.emit(Prim.newarr);

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	/////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitThisRef(ThisRef ref, Integer arg) {

		// Push address of current object instance (this)
		Machine.emit(LOADA, OB, 0);

		return null;
	}

	@Override
	public Integer visitIdRef(IdRef ref, Integer arg) {
		Declaration d = ref.decl;
		int offset = ((KnownAddress) d.entity).offset;

		// Load address
		if (arg == 1) {

			// Static field
			if (d instanceof FieldDecl && ((FieldDecl) d).isStatic) {
				Machine.emit(LOAD, SB, offset);
			}

			// Instance field
			else if (d instanceof FieldDecl) {
				Machine.emit(LOAD, OB, offset);
			}

			// Local variable/parameter
			else {
				Machine.emit(LOAD, LB, offset);
			}

		} else {
			return offset;
		}

		return null;
	}

	@Override
	public Integer visitQRef(QualRef ref, Integer arg) {
		Declaration d = ref.id.decl;
		boolean isField = d instanceof FieldDecl;
		boolean isMethod = d instanceof MethodDecl;
		boolean isLength = ref.spelling.equals("length");
		int offset;

		// Load
		if (arg == 1) {
			// Array length
			if (isField && isLength) {

				// Load address of array
				ref.ref.visit(this, 1);

				// Return length
				Machine.emit(Prim.arraylen);
			}

			// Static field
			else if (isField && ((FieldDecl) d).isStatic) {
				offset = ((KnownAddress) d.entity).offset;
				Machine.emit(LOAD, SB, offset);
			}

			// Instance field
			else if (isField) {

				// Load address of object (a)
				ref.ref.visit(this, 1);

				// Load field offset (i)
				offset = ((KnownAddress) d.entity).offset;
				Machine.emit(LOADL, offset);

				// Return value of a.i
				Machine.emit(Prim.fieldref);
			}

			// Method
			// Ex: a.b()
			else if (isMethod) {

				// Load address of object instance
				ref.ref.visit(this, 1);
			}

			else {
				error("Invalid declaration in qualified reference at " + ref.position);
			}
		}

		// Store
		else {

			// Array length (read only)
			if (isField && isLength) {
				error("Cannot assign value to read-only length field of array at " + ref.position);
			}

			// Static field
			else if (isField && ((FieldDecl) d).isStatic) {
				offset = ((KnownAddress) d.entity).offset;
				return offset;
			}

			// Instance field
			else {

				// Load address of object instnace
				ref.ref.visit(this, 1);

				// Load field offset
				offset = ((KnownAddress) d.entity).offset;
				Machine.emit(LOADL, offset);
			}
		}

		return null;
	}

	@Override
	public Integer visitIxRef(IxRef ref, Integer arg) {

		// Load
		if (arg == 1) {

			// Load address of array (a)
			ref.ref.visit(this, 1);

			// Load element index (i)
			ref.indexExpr.visit(this, 1);

			// Push value of a[i]
			Machine.emit(Prim.arrayref);
		}

		// Store
		else {

			// Load address of array (a)
			ref.ref.visit(this, 1);

			// Load element index (i)
			ref.indexExpr.visit(this, 1);
		}

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	/////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitIdentifier(Identifier id, Integer arg) {
		return null;
	}

	@Override
	public Integer visitOperator(Operator op, Integer arg) {
		return null;
	}

	@Override
	public Integer visitIntLiteral(IntLiteral num, Integer arg) {
		return null;
	}

	@Override
	public Integer visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
		return null;
	}

	@Override
	public Integer visitNullLiteral(NullLiteral nul, Integer arg) {
		return null;
	}

}
