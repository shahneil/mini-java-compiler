package miniJava.CodeGenerator;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Reg;
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
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class CodeGenerator implements Visitor<Integer, Integer> {

	private ErrorReporter reporter;
	private String objectCodeFileName;
	private String asmCodeFileName;
	private boolean debug = true; // TODO: Turn off after development

	private int mainAddr;
	private int frameOffset;

	// TODO: Patch list for methods
	// TODO: Add length field for arrays
	// TODO: Check return statements (see PA4 description)

	public CodeGenerator(String sourceName, ErrorReporter reporter) {
		this.reporter = reporter;
		objectCodeFileName = sourceName.substring(0, sourceName.indexOf('.')) + ".mJAM";
		asmCodeFileName = objectCodeFileName.replace(".mJAM", ".asm");
	}

	public void generate(AST prog) {
		Machine.initCodeGen();
		prog.visit(this, null);
		generateObjectCode();
		generateAssembly();
		if (debug) {
			runDebugger();
		}
	}

	private void generateObjectCode() {
		ObjectFile objectFile = new ObjectFile(objectCodeFileName);
		System.out.println("Generating object code file " + objectCodeFileName + "...");

		if (objectFile.write()) {
			error("Failed to generate object code file.");
		} else {
			System.out.println("Generated object code file " + objectCodeFileName + "...");
		}
	}

	private void generateAssembly() {
		System.out.println("Generating assembly file " + asmCodeFileName + "...");
		Disassembler d = new Disassembler(objectCodeFileName);

		if (d.disassemble()) {
			error("Failed to generate assembly file.");
		} else {
			System.out.println("Generated assembly file " + asmCodeFileName + "...");
		}
	}

	private void runDebugger() {
		System.out.println("Running code in debugger...");
		Interpreter.debug(objectCodeFileName, asmCodeFileName);
		System.out.println("Finished running code.");
	}

	private void error(String message) {
		reporter.reportError(message);
		throw new Error();
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	/////////////////////////////////////////////////////////////////////////////

	public Integer visitPackage(Package prog, Integer arg) {

		// Load static variables
		int offset = 0;
		for (ClassDecl cd : prog.classDeclList) {
			for (FieldDecl fd : cd.fieldDeclList) {
				if (fd.isStatic) {
					Machine.emit(Op.PUSH, 1);
					fd.entity = new KnownAddress(Machine.characterSize, offset);
					offset++;
				}
			}
		}

		// Record address where main is called
		mainAddr = Machine.nextInstrAddr();

		// Call main (patch)
		Machine.emit(Op.CALL, Reg.CB, -1);

		// End execution
		Machine.emit(Op.HALT, 0, 0, 0);

		// Decorate fields
		offset = 0;
		for (ClassDecl cd : prog.classDeclList) {
			for (FieldDecl fd : cd.fieldDeclList) {
				fd.visit(this, offset);
				offset++;
			}
		}

		// Visit classes
		for (ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Integer visitClassDecl(ClassDecl cd, Integer arg) {

		// Visit methods
		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}

		return null;
	}

	public Integer visitFieldDecl(FieldDecl fd, Integer offset) {

		// Decorate non-static fields
		if (!fd.isStatic) {
			fd.entity = new KnownAddress(Machine.characterSize, offset);
		}

		return null;
	}

	public Integer visitMethodDecl(MethodDecl md, Integer arg) {

		// Method code address
		md.entity = new KnownAddress(Machine.addressSize, Machine.nextInstrAddr());

		// Patch address of main()
		if (md.name.equals("main")) {
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
			Machine.emit(Op.RETURN, 0, 0, numParams);
		} else {
			Machine.emit(Op.RETURN, 1, 0, numParams);
		}

		return null;
	}

	public Integer visitParameterDecl(ParameterDecl pd, Integer offset) {
		pd.entity = new KnownAddress(Machine.addressSize, offset);
		return null;
	}

	public Integer visitVarDecl(VarDecl decl, Integer arg) {
		decl.entity = new KnownAddress(Machine.characterSize, frameOffset);
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	/////////////////////////////////////////////////////////////////////////////

	public Integer visitBaseType(BaseType type, Integer arg) {
		return null;
	}

	public Integer visitClassType(ClassType type, Integer arg) {
		return null;
	}

	public Integer visitArrayType(ArrayType type, Integer arg) {
		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////////////

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
		Machine.emit(Op.POP, 0, 0, numVars);

		// Reset frame offset
		// TODO: May have to change based on later code
		frameOffset -= numVars;

		return null;
	}

	public Integer visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null); // TODO: May have to change argument based on expression code.
		frameOffset++;
		return null;
	}

	public Integer visitAssignStmt(AssignStmt stmt, Integer arg) {

		return null;
	}

	public Integer visitCallStmt(CallStmt stmt, Integer arg) {

		return null;
	}

	public Integer visitReturnStmt(ReturnStmt stmt, Integer arg) {

		return null;
	}

	public Integer visitIfStmt(IfStmt stmt, Integer arg) {

		return null;
	}

	public Integer visitWhileStmt(WhileStmt stmt, Integer arg) {

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Integer visitUnaryExpr(UnaryExpr expr, Integer arg) {

		return null;
	}

	public Integer visitBinaryExpr(BinaryExpr expr, Integer arg) {

		return null;
	}

	public Integer visitRefExpr(RefExpr expr, Integer arg) {

		return null;
	}

	public Integer visitCallExpr(CallExpr expr, Integer arg) {

		return null;
	}

	public Integer visitLiteralExpr(LiteralExpr expr, Integer arg) {

		return null;
	}

	public Integer visitNewObjectExpr(NewObjectExpr expr, Integer arg) {

		return null;
	}

	public Integer visitNewArrayExpr(NewArrayExpr expr, Integer arg) {

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	/////////////////////////////////////////////////////////////////////////////

	public Integer visitThisRef(ThisRef ref, Integer arg) {

		return null;
	}

	public Integer visitIdRef(IdRef ref, Integer arg) {

		return null;
	}

	public Integer visitQRef(QualRef ref, Integer arg) {

		return null;
	}

	public Integer visitIxRef(IxRef ref, Integer arg) {

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Integer visitIdentifier(Identifier id, Integer arg) {

		return null;
	}

	public Integer visitOperator(Operator op, Integer arg) {

		return null;
	}

	public Integer visitIntLiteral(IntLiteral num, Integer arg) {

		return null;
	}

	public Integer visitBooleanLiteral(BooleanLiteral bool, Integer arg) {

		return null;
	}

	public Integer visitNullLiteral(NullLiteral nul, Integer arg) {

		return null;
	}

}
