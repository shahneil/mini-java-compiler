package miniJava.SyntacticAnalyzer;

public class SourcePosition {

	public int line, col;

	public SourcePosition() {
		line = 0;
		col = 0;
	}

	public SourcePosition(int l, int c) {
		line = l;
		col = c;
	}

	public String toString() {
		return "line " + line + ", col " + col;
	}
}
