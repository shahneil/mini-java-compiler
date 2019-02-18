package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.Terminal;
import miniJava.AbstractSyntaxTrees.TypeDenoter;

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
			StatementList sl;

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
	 * TODO: Implement
	 * 
	 * Type -> (int|id)([])? | boolean
	 */
	TypeDenoter parseType() throws SyntaxError {
		if (currentToken.kind == Token.INT || currentToken.kind == Token.ID) {
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
			}
		} else if (currentToken.kind == Token.BOOLEAN) {
			acceptIt();
		}
	}

	/**
	 * TODO: Implement
	 * 
	 * ParameterList -> Type id (, Type id)*
	 */
	ParameterDeclList parseParameterList() throws SyntaxError {
		parseType();
		accept(Token.ID);
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			parseType();
			accept(Token.ID);
		}
	}

	/**
	 * TODO: Implement
	 * 
	 * ArgumentList -> Expression (, Expression)*
	 */
	private void parseArgumentList() throws SyntaxError {
		parseExpression();
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			parseExpression();
		}
	}

	/**
	 * TODO: Implement
	 * 
	 * TODO: Split up into IdxReference
	 * 
	 * Reference -> (id|this) (. id)* ( [ Expression ] )?
	 */
	private void parseReference() throws SyntaxError {
		if (currentToken.kind == Token.ID || currentToken.kind == Token.THIS) {
			acceptIt();
			while (currentToken.kind == Token.DOT) {
				acceptIt();
				accept(Token.ID);
			}
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				parseExpression();
				accept(Token.RBRACKET);
			}
		}
	}

	/** 
	 * TODO: Implement
	 * 
	 * @formatter:off
	 * Statement -> 
	 * 		{ Statement* }
	 * 	|	Type id = Expression;
	 *	| 	Reference ( = Expression; | '('ArgumentList?')' )
	 *	|	return Expression?;
	 *	| 	if '('Expression')' Statement (else Statement)?
	 *	| 	while '('Expression')' Statement
	 * @formatter:on
	 */
	Statement parseStatement() throws SyntaxError {
		switch (currentToken.kind) {

		case Token.LCURLY:
			acceptIt();
			while (startsStatement(currentToken)) {
				parseStatement();
			}
			accept(Token.RCURLY);
			return;

		case Token.INT:
		case Token.BOOLEAN:
		case Token.ID:
		case Token.THIS:

			// Type id = Expression;
			if (startsType(currentToken)) {
				Token nextToken = scanner.peek();
				Token nextNextToken = scanner.peek();

				boolean isBracket = nextToken.kind == Token.LBRACKET;
				boolean isId = nextToken.kind == Token.ID;
				boolean isIndex = nextNextToken.kind != Token.RBRACKET;

				// p a = 3; -> Type id = Expression;
				// p[] a = 3; -> Type id = Expression;
				// p = 3; -> Reference = Expression;
				// p[1+2] = 3; -> Reference = Expression;
				if (isId || (isBracket && !isIndex)) {
					parseType();
					accept(Token.ID);
					accept(Token.BECOMES);
					parseExpression();
					accept(Token.SEMICOLON);
					return;
				}
			}

			parseReference();

			// Reference = Expression;
			if (currentToken.kind == Token.BECOMES) {
				acceptIt();
				parseExpression();
				accept(Token.SEMICOLON);
				return;
			}

			// Reference '(' ArgumentList? ')';
			if (currentToken.kind == Token.LPAREN) {
				acceptIt();
				if (startsExpression(currentToken)) {
					parseArgumentList();
				}
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
				return;
			}

		case Token.RETURN:
			acceptIt();
			if (startsExpression(currentToken)) {
				parseExpression();
			}
			accept(Token.SEMICOLON);
			return;

		case Token.IF:
			acceptIt();
			accept(Token.LPAREN);
			parseExpression();
			accept(Token.RPAREN);
			parseStatement();

			if (currentToken.kind == Token.ELSE) {
				acceptIt();
				parseStatement();
			}
			return;

		case Token.WHILE:
			acceptIt();
			accept(Token.LPAREN);
			parseExpression();
			accept(Token.RPAREN);
			parseStatement();
			return;
		}
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

}