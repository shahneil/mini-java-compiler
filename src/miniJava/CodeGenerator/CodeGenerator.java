package miniJava.CodeGenerator;

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
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class CodeGenerator implements Visitor<Object, Object> {

	private ErrorReporter reporter;
	private int patchAddr_Call_main;

	// TODO: Patch list for methods
	// TODO: Add length field for arrays
	// TODO: Check return statements (see PA4 description)

	public CodeGenerator(ErrorReporter reporter) {
		this.reporter = reporter;
	}

	public void generate(String sourceName, AST prog) {
		Machine.initCodeGen();
		prog.visit(this, null);

		// Write code to object file
		String objectCodeFileName = sourceName.substring(0, sourceName.indexOf('.')) + ".mJAM";
		ObjectFile objectFile = new ObjectFile(objectCodeFileName);
		System.out.println("Generating object code file " + objectCodeFileName + "...");
		if (objectFile.write()) {
			System.out.println("Generated object code file " + objectCodeFileName + "...");
		} else {
			error("Failed to generate object code file.");
		}
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

	public Object visitPackage(Package prog, Object arg) {

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
		patchAddr_Call_main = Machine.nextInstrAddr();

		// Call main (patch)
		Machine.emit(Op.CALL, Reg.CB, -1);

		// End execution
		Machine.emit(Op.HALT, 0, 0, 0);

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

	public Object visitClassDecl(ClassDecl cd, Object arg) {

		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, Object arg) {

		return null;
	}

	public Object visitMethodDecl(MethodDecl md, Object arg) {

		return null;
	}

	public Object visitParameterDecl(ParameterDecl pd, Object arg) {

		return null;
	}

	public Object visitVarDecl(VarDecl decl, Object arg) {

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitBaseType(BaseType type, Object arg) {

		return null;
	}

	public Object visitClassType(ClassType type, Object arg) {

		return null;
	}

	public Object visitArrayType(ArrayType type, Object arg) {

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitBlockStmt(BlockStmt stmt, Object arg) {

		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {

		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, Object arg) {

		return null;
	}

	public Object visitCallStmt(CallStmt stmt, Object arg) {

		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {

		return null;
	}

	public Object visitIfStmt(IfStmt stmt, Object arg) {

		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, Object arg) {

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {

		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {

		return null;
	}

	public Object visitRefExpr(RefExpr expr, Object arg) {

		return null;
	}

	public Object visitCallExpr(CallExpr expr, Object arg) {

		return null;
	}

	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {

		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {

		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitThisRef(ThisRef ref, Object arg) {

		return null;
	}

	public Object visitIdRef(IdRef ref, Object arg) {

		return null;
	}

	public Object visitQRef(QualRef ref, Object arg) {

		return null;
	}

	public Object visitIxRef(IxRef ref, Object arg) {

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	/////////////////////////////////////////////////////////////////////////////

	public Object visitIdentifier(Identifier id, Object arg) {

		return null;
	}

	public Object visitOperator(Operator op, Object arg) {

		return null;
	}

	public Object visitIntLiteral(IntLiteral num, Object arg) {

		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {

		return null;
	}

	public Object visitNullLiteral(NullLiteral nul, Object arg) {

		return null;
	}

}
