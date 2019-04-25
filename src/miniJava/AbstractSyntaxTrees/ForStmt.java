package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ForStmt extends Statement {

	public ForStmt(Statement init, Expression cond, Statement update, Statement body, SourcePosition position) {
		super(position);
		this.init = init;
		this.cond = cond;
		this.update = update;
		this.body = body;
		this.position = position;
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitForStmt(this, o);
	}

	public Statement init;
	public Expression cond;
	public Statement update;
	public Statement body;
}
