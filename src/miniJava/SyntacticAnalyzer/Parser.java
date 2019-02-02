package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class Parser {

	private boolean trace = false;
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

	// Program ::= (ClassDeclaration)* eot
	private void parseProgram() throws SyntaxError {
		while (currentToken.kind == Token.CLASS) {
			parseClassDeclaration();
		}
		accept(Token.EOT);
	}

	// ClassDeclaration ::= class id { (FieldDeclaration | MethodDeclaration)* }
	private void parseClassDeclaration() throws SyntaxError {
		accept(Token.CLASS);
		accept(Token.ID);
		accept(Token.LCURLY);

		// starters of (FieldDeclaration | MethodDeclaration)
		while (currentToken.kind == Token.PUBLIC || currentToken.kind == Token.PRIVATE
				|| currentToken.kind == Token.STATIC || currentToken.kind == Token.INT
				|| currentToken.kind == Token.BOOLEAN || currentToken.kind == Token.ID
				|| currentToken.kind == Token.VOID) {
			parseFieldOrMethodDeclaration();
		}

		accept(Token.RCURLY);
	}

	// (FieldDeclaration | MethodDeclaration) ::=
	// (Visibility Access (Type|void) id ('('ParameterList?')' {Statement*})*)*
	private void parseFieldOrMethodDeclaration() throws SyntaxError {
		parseVisibility();
		parseAccess();

		// (Type|void) id
		if (startsType(currentToken)) {
			parseType();
		} else {
			accept(Token.VOID);
			// If void, we require parameter list and statement.
		}
		accept(Token.ID);

		// '(' ParameterList? ')' { Statement* }
		if (currentToken.kind == Token.LPAREN) {
			acceptIt();
			// starters for ParameterList
			if (currentToken.kind == Token.INT || currentToken.kind == Token.BOOLEAN || currentToken.kind == Token.ID) {
				parseParameterList();
			}
			accept(Token.RPAREN);
			accept(Token.LCURLY);

			while (startsStatement(currentToken)) {
				parseStatement();
			}
			accept(Token.RCURLY);
		} else {
			accept(Token.SEMICOLON);
		}
	}

	// Visibility ::= (public|private)?
	private void parseVisibility() throws SyntaxError {
		if (currentToken.kind == Token.PUBLIC || currentToken.kind == Token.PRIVATE) {
			acceptIt();
		}
	}

	// Access ::= static?
	private void parseAccess() throws SyntaxError {
		if (currentToken.kind == Token.STATIC) {
			acceptIt();
		}
	}

	// Type ::= (int|id)([])? | boolean
	private void parseType() throws SyntaxError {
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

	// ParameterList ::= Type id (, Type id)*
	private void parseParameterList() throws SyntaxError {
		parseType();
		accept(Token.ID);
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			parseType();
			accept(Token.ID);
		}
	}

	// ArgumentList ::= Expression (, Expression)*
	private void parseArgumentList() throws SyntaxError {
		parseExpression();
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			parseExpression();
		}
	}

	// Reference ::= (id|this) (. id)* ( [ Expression ] )?
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

	/* @formatter:off
	 * Statement ::= 
	 * 		{ Statement* }
	 * 	|	Type id = Expression;
	 *	| 	Reference ( = Expression; | '('ArgumentList?')' )
	 *	|	return Expression?;
	 *	| 	if '('Expression')' Statement (else Statement)?
	 *	| 	while '('Expression')' Statement
	 * @formatter:on
	 */
	private void parseStatement() throws SyntaxError {
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

	// Expression ::= Expression' (binop Expression)*
	private void parseExpression() throws SyntaxError {
		parseExpressionPrime();
		while (startsBinop(currentToken)) {
			acceptIt();
			parseExpression();
		}
	}

	/* @formatter:off
	 * Expression' ::=
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
	private void parseUnop() throws SyntaxError {
		switch (currentToken.kind) {
		case Token.NOT:
		case Token.MINUS:
			acceptIt();
		}
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