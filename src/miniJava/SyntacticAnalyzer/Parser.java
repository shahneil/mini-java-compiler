package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IxRef;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.Terminal;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class Parser {

	private boolean trace = true;
	private Scanner scanner;
	private ErrorReporter reporter;
	private Token currentToken;
	private SourcePosition previousTokenPosition;

	public Parser(Scanner s, ErrorReporter r) {
		scanner = s;
		reporter = r;
		previousTokenPosition = new SourcePosition();
	}

	/**
	 * If the current token matches the expected token, fetches the next token.
	 */
	private void accept(int expectedToken) throws SyntaxError {
		if (currentToken.kind == expectedToken) {
			if (trace) {
				trace();
			}
			previousTokenPosition = currentToken.position;
			currentToken = scanner.scan();
		} else {
			String expected = Token.spell(expectedToken);
			String found = currentToken.spelling;
			String position = currentToken.position.toString();
			parseError("Expected " + expected + ", but found " + found + " on " + position + ".");
		}
	}

	/**
	 * Fetches the next token.
	 */
	private void acceptIt() {
		accept(currentToken.kind);
	}

	/**
	 * Reports a syntax error with the specified message.
	 */
	private void parseError(String e) throws SyntaxError {
		reporter.reportError(e);
		throw new SyntaxError();
	}

	/**
	 * Print parse stack whenever terminal is accepted.
	 */
	private void trace() {
		StackTraceElement[] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0; i--) {
			if (stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + currentToken.spelling);
		System.out.println();
	}

	/**
	 * Parse source program.
	 */
	public void parse() {
		currentToken = scanner.scan();
		try {
			parseProgram();
		} catch (SyntaxError e) {
			// Compiler driver will take care of error handling/reporting.
		}
	}

	/**
	 * Program -> (ClassDeclaration)* eot
	 */
	Package parseProgram() throws SyntaxError {
		ClassDeclList cdl = new ClassDeclList();
		Package p = new Package(cdl, null);

		while (currentToken.kind == Token.CLASS) {
			ClassDecl cd = parseClassDeclaration();
			cdl.add(cd);
		}

		accept(Token.EOT);
		return p;
	}

	/**
	 * ClassDeclaration -> class id { (FieldDeclaration | MethodDeclaration)* }
	 */
	ClassDecl parseClassDeclaration() throws SyntaxError {
		String cn = currentToken.spelling;
		accept(Token.CLASS);
		accept(Token.ID);
		accept(Token.LCURLY);

		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		ClassDecl cd = new ClassDecl(cn, fdl, mdl, null);

		while (startsMemberDeclaration(currentToken)) {
			MemberDecl md = parseMemberDeclaration();
			if (md instanceof FieldDecl) {
				fdl.add((FieldDecl) md);
			} else if (md instanceof MethodDecl) {
				mdl.add((MethodDecl) md);
			}
		}
		accept(Token.RCURLY);
		return cd;
	}

	/**
	 * FieldDeclaration -> Visibility Access Type id;
	 * 
	 * MethodDeclaration -> Visibility Access (Type|void) id '(' ParameterList? ')'
	 * { Statement* }
	 */
	MemberDecl parseMemberDeclaration() throws SyntaxError {
		boolean isPrivate = !parseVisibility();
		boolean isStatic = parseAccess();

		TypeDenoter td;
		if (startsType(currentToken)) {
			td = parseType();
		} else {
			accept(Token.VOID);
			td = new BaseType(TypeKind.VOID, null);
		}

		String name = currentToken.spelling;
		accept(Token.ID);

		// Need for both FieldDeclaration and MethodDeclaration
		FieldDecl fd = new FieldDecl(isPrivate, isStatic, td, name, null);

		// Resolve ambiguity
		if (currentToken.kind == Token.SEMICOLON) { // FieldDeclaration
			acceptIt();
			return fd;
		} else { // MethodDeclaration
			ParameterDeclList pl;
			StatementList sl = new StatementList();

			accept(Token.LPAREN);
			pl = parseParameterList();
			accept(Token.RPAREN);

			accept(Token.LCURLY);
			while (startsStatement(currentToken)) {
				Statement s = parseStatement();
				sl.add(s);
			}
			accept(Token.RCURLY);

			return new MethodDecl(fd, pl, sl, null);
		}
	}

	/**
	 * Visibility -> (public|private)?
	 * 
	 * @return false if private
	 */
	boolean parseVisibility() throws SyntaxError {
		if (currentToken.kind == Token.PRIVATE) {
			acceptIt();
			return false;
		} else if (currentToken.kind == Token.PUBLIC) {
			acceptIt();
		}
		return true;
	}

	/**
	 * Access -> static?
	 * 
	 * @return true if static
	 */
	boolean parseAccess() throws SyntaxError {
		if (currentToken.kind == Token.STATIC) {
			acceptIt();
			return true;
		}
		return false;
	}

	/**
	 * Type -> int | boolean | id | (int|id)[]
	 */
	TypeDenoter parseType() throws SyntaxError {
		if (currentToken.kind == Token.INT) {
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
				return new ArrayType(new BaseType(TypeKind.INT, null), null);
			} else {
				return new BaseType(TypeKind.INT, null);
			}
		} else if (currentToken.kind == Token.ID) {
			Identifier cn = new Identifier(currentToken);
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
				return new ArrayType(new ClassType(cn, null), null);
			} else {
				return new ClassType(cn, null);
			}
		} else {
			accept(Token.BOOLEAN);
			return new BaseType(TypeKind.BOOLEAN, null);
		}
	}

	/**
	 * ParameterList -> Type id (, Type id)*
	 */
	ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList pl = new ParameterDeclList();
		ParameterDecl p;
		String name;
		TypeDenoter t;

		t = parseType();
		name = currentToken.spelling;
		accept(Token.ID);
		p = new ParameterDecl(t, name, null);
		pl.add(p);

		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			t = parseType();
			name = currentToken.spelling;
			accept(Token.ID);
			pl.add(p);
		}

		return pl;
	}

	/**
	 * ArgumentList -> Expression (, Expression)*
	 */
	ExprList parseArgumentList() throws SyntaxError {
		ExprList el = new ExprList();
		Expression e;

		e = parseExpression();
		el.add(e);

		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			e = parseExpression();
			el.add(e);
		}

		return el;
	}

	/**
	 * Reference -> id | this | Reference.id
	 * 
	 * IxReference -> Reference [ Expression ]
	 */
	Reference parseReference() throws SyntaxError {
		Reference ref;

		// Base reference
		if (currentToken.kind == Token.ID) {
			ref = new IdRef(new Identifier(currentToken), null);
			acceptIt();
		} else if (currentToken.kind == Token.THIS) {
			ref = new ThisRef(null);
		} else {
			throw new SyntaxError();
		}

		// Qualified references
		while (currentToken.kind == Token.DOT) {
			acceptIt();
			ref = new QualRef(ref, new Identifier(currentToken), null);
			accept(Token.ID);
		}

		// Index references
		if (currentToken.kind == Token.LBRACKET) {
			acceptIt();
			Expression e = parseExpression();
			accept(Token.RBRACKET);
			ref = new IxRef(ref, e, null);
		}

		return ref;
	}

	/** 
	 * @formatter:off
	 * Statement -> 
	 * 		{ Statement* }										[BlockStmt]
	 * 	|	Type id = Expression;								[VarDeclStmt]
	 *	| 	Reference = Expression;								[AssignStmt]
	 *	|	Reference '(' ArgumentList? ')';					[CallStmt]			
	 *	|	return Expression?;									[ReturnStmt]
	 *	| 	if '('Expression')' Statement (else Statement)?		[IfStmt]
	 *	| 	while '('Expression')' Statement					[WhileStmt]
	 * @formatter:on
	 */
	Statement parseStatement() throws SyntaxError {
		// Block statement
		if (currentToken.kind == Token.LCURLY) {
			acceptIt();
			StatementList sl = new StatementList();
			while (startsStatement(currentToken)) {
				sl.add(parseStatement());
			}
			accept(Token.RCURLY);
			return new BlockStmt(sl, null);
		}

		// If statement
		else if (currentToken.kind == Token.IF) {
			acceptIt();

			accept(Token.LPAREN);
			Expression b = parseExpression();
			accept(Token.RPAREN);

			Statement t = parseStatement();

			if (currentToken.kind == Token.ELSE) {
				acceptIt();
				Statement e = parseStatement();
				return new IfStmt(b, t, e, null);
			}
			return new IfStmt(b, t, null);
		}

		// Return statement
		else if (currentToken.kind == Token.RETURN) {
			acceptIt();

			Expression e = null; // TODO: Check Piazza
			if (startsExpression(currentToken)) {
				e = parseExpression();
			}

			accept(Token.SEMICOLON);
			return new ReturnStmt(e, null);
		}

		// While statement
		else if (currentToken.kind == Token.WHILE) {
			acceptIt();

			accept(Token.LPAREN);
			Expression e = parseExpression();
			accept(Token.RPAREN);

			Statement s = parseStatement();
			return new WhileStmt(e, s, null);
		}

		else if (startsType(currentToken) || startsReference(currentToken)) {
			/* 
			 * @formatter:off
			 * 
			 * T a = 3; 	[VarDeclStmt]
			 * T[] a = 3; 	[VarDeclStmt]
			 * R = 3; 		[AssignStmt]
			 * R[1+2] = 3; 	[AssignStmt]
			 * R(...);		[CallStmt]
			 * 
			 * @formatter:on
			 */

			Token next = scanner.peek();
			Token nextNext = scanner.peek();

			// Variable declaration statement
			if (next.kind == Token.ID || next.kind == Token.LBRACKET && nextNext.kind == Token.RBRACKET) {
				TypeDenoter t = parseType();
				String name = currentToken.spelling;
				accept(Token.ID);
				VarDecl vd = new VarDecl(t, name, null);

				accept(Token.BECOMES);
				Expression e = parseExpression();
				accept(Token.SEMICOLON);
				return new VarDeclStmt(vd, e, null);
			}

			Reference r = parseReference();

			// Assign statement
			if (currentToken.kind == Token.BECOMES) {
				acceptIt();
				Expression e = parseExpression();
				accept(Token.SEMICOLON);
				return new AssignStmt(r, e, null);
			}

			// Call statement
			if (currentToken.kind == Token.LPAREN) {
				acceptIt();
				ExprList el = new ExprList();
				if (startsExpression(currentToken)) {
					el = parseArgumentList();
				}
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
				return new CallStmt(r, el, null);
			}
		}
		throw new SyntaxError();
	}

	/**
	 * TODO: Implement
	 * 
	 * Expression -> Expression' (binop Expression)*
	 */
	Expression parseExpression() throws SyntaxError {
		parseExpressionPrime();
		while (startsBinop(currentToken)) {
			acceptIt();
			parseExpression();
		}
	}

	/** 
	 * TODO: Implement
	 * 
	 * @formatter:off
	 * Expression' ->
	 * 		Reference ( '(' ArgumentList? ')' )?
	 * 	| 	unop Expression'
	 * 	| 	'(' Expression' ')'
	 * 	| 	num | true | false
	 *  | 	new ( id '(' ')' | int [ Expression' ] | id [ Expression' ] )
	 * @formatter:on
	 * 
	 */
	private void parseExpressionPrime() throws SyntaxError {
		switch (currentToken.kind) {

		case Token.ID:
		case Token.THIS:
			parseReference();
			if (currentToken.kind == Token.LPAREN) {
				acceptIt();
				if (startsExpression(currentToken)) {
					parseArgumentList();
				}
				accept(Token.RPAREN);
			}
			return;

		case Token.NOT:
		case Token.MINUS:
			parseUnop();
			parseExpression();
			return;

		case Token.LPAREN:
			acceptIt();
			parseExpression();
			accept(Token.RPAREN);
			return;

		case Token.NUM:
		case Token.TRUE:
		case Token.FALSE:
			acceptIt();
			return;

		case Token.NEW:
			acceptIt();
			if (currentToken.kind == Token.ID) {
				acceptIt();
				if (currentToken.kind == Token.LPAREN) {
					acceptIt();
					accept(Token.RPAREN);
				} else if (currentToken.kind == Token.LBRACKET) {
					acceptIt();
					parseExpression();
					accept(Token.RBRACKET);
				}
			} else if (currentToken.kind == Token.INT) {
				acceptIt();
				accept(Token.LBRACKET);
				parseExpression();
				accept(Token.RBRACKET);
			}
			return;
		}
	}

	// unop ::= !|-
	Terminal parseUnop() throws SyntaxError {
		switch (currentToken.kind) {
		case Token.NOT:
		case Token.MINUS:
			acceptIt();
		}
	}

	boolean startsMemberDeclaration(Token token) {
		return token.kind == Token.PUBLIC || token.kind == Token.PRIVATE || token.kind == Token.STATIC
				|| token.kind == Token.INT || token.kind == Token.BOOLEAN || token.kind == Token.ID
				|| token.kind == Token.VOID;
	}

	// binop ::= >|<|==|<=|>=|!=|&&|+|-|*|/| ||
	private boolean startsBinop(Token token) {
		return token.kind == Token.GT || token.kind == Token.LT || token.kind == Token.EQ || token.kind == Token.LTE
				|| token.kind == Token.GTE || token.kind == Token.NEQ || token.kind == Token.AND
				|| token.kind == Token.OR || token.kind == Token.ADD || token.kind == Token.MINUS
				|| token.kind == Token.MULT || token.kind == Token.DIV;
	}

	private boolean startsStatement(Token token) {
		return token.kind == Token.LCURLY || currentToken.kind == Token.INT || token.kind == Token.BOOLEAN
				|| token.kind == Token.ID || token.kind == Token.THIS || token.kind == Token.RETURN
				|| token.kind == Token.IF || token.kind == Token.WHILE;
	}

	private boolean startsExpression(Token token) {
		return token.kind == Token.ID || token.kind == Token.THIS || token.kind == Token.NOT
				|| token.kind == Token.MINUS || token.kind == Token.LPAREN || token.kind == Token.NUM
				|| token.kind == Token.TRUE || token.kind == Token.FALSE || token.kind == Token.NEW;
	}

	private boolean startsType(Token token) {
		return token.kind == Token.INT || token.kind == Token.ID || token.kind == Token.BOOLEAN;
	}

	private boolean startsReference(Token token) {
		return token.kind == Token.ID || token.kind == Token.THIS;
	}

}