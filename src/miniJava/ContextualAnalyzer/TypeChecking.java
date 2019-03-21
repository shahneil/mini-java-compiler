package miniJava.ContextualAnalyzer;

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

public class TypeChecking implements Visitor<ArgType, ResultType> {

	@Override
	public ResultType visitPackage(Package prog, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitClassDecl(ClassDecl cd, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitFieldDecl(FieldDecl fd, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitMethodDecl(MethodDecl md, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitParameterDecl(ParameterDecl pd, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitVarDecl(VarDecl decl, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitBaseType(BaseType type, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitClassType(ClassType type, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitArrayType(ArrayType type, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitBlockStmt(BlockStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitVardeclStmt(VarDeclStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitAssignStmt(AssignStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitCallStmt(CallStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitReturnStmt(ReturnStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIfStmt(IfStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitWhileStmt(WhileStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitUnaryExpr(UnaryExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitBinaryExpr(BinaryExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitRefExpr(RefExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitCallExpr(CallExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitLiteralExpr(LiteralExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitNewObjectExpr(NewObjectExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitNewArrayExpr(NewArrayExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitThisRef(ThisRef ref, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIdRef(IdRef ref, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitQRef(QualRef ref, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIxRef(IxRef ref, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIdentifier(Identifier id, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitOperator(Operator op, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIntLiteral(IntLiteral num, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitBooleanLiteral(BooleanLiteral bool, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitPackage(Package prog, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitClassDecl(ClassDecl cd, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitFieldDecl(FieldDecl fd, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitMethodDecl(MethodDecl md, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitParameterDecl(ParameterDecl pd, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitVarDecl(VarDecl decl, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitBaseType(BaseType type, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitClassType(ClassType type, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitArrayType(ArrayType type, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitBlockStmt(BlockStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitVardeclStmt(VarDeclStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitAssignStmt(AssignStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitCallStmt(CallStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitReturnStmt(ReturnStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIfStmt(IfStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitWhileStmt(WhileStmt stmt, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitUnaryExpr(UnaryExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitBinaryExpr(BinaryExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitRefExpr(RefExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitCallExpr(CallExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitLiteralExpr(LiteralExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitNewObjectExpr(NewObjectExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitNewArrayExpr(NewArrayExpr expr, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitThisRef(ThisRef ref, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIdRef(IdRef ref, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitQRef(QualRef ref, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIxRef(IxRef ref, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIdentifier(Identifier id, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitOperator(Operator op, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitIntLiteral(IntLiteral num, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultType visitBooleanLiteral(BooleanLiteral bool, ArgType arg) {
		// TODO Auto-generated method stub
		return null;
	}

}
