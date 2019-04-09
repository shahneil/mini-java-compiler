package miniJava.SyntacticAnalyzer;

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
import miniJava.AbstractSyntaxTrees.ExprList;
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
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.Terminal;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.WhileStmt;

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

	private SourcePosition getPosition() {
		if (currentToken == null) {
			return new SourcePosition();
		}
		return new SourcePosition(currentToken.position);
	}

	/**
	 * Parse source program.
	 */
	public AST parse() throws SyntaxError {
		currentToken = scanner.scan();
		return parseProgram();
	}

	/**
	 * Program -> (ClassDeclaration)* eot
	 */
	Package parseProgram() throws SyntaxError {

		// Parse every class in package
		ClassDeclList cdl = new ClassDeclList();
		Package p = new Package(cdl, getPosition());

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
		SourcePosition p = getPosition();
		accept(Token.CLASS);
		String cn = currentToken.spelling;
		Identifier cid = new Identifier(currentToken);
		accept(Token.ID);
		accept(Token.LCURLY);

		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();

		while (startsMemberDeclaration(currentToken)) {
			MemberDecl md = parseMemberDeclaration();
			if (md instanceof FieldDecl) {
				fdl.add((FieldDecl) md);
			} else if (md instanceof MethodDecl) {
				mdl.add((MethodDecl) md);
			}
		}
		accept(Token.RCURLY);

		ClassDecl cd = new ClassDecl(cn, fdl, mdl, p);
		cd.type = new ClassType(cid, p);
		return cd;
	}

	/**
	 * FieldDeclaration -> Visibility Access Type id;
	 * 
	 * MethodDeclaration -> Visibility Access (Type|void) id '(' ParameterList? ')'
	 * { Statement* }
	 */
	MemberDecl parseMemberDeclaration() throws SyntaxError {
		SourcePosition p = getPosition();
		boolean isPrivate = !parseVisibility();
		boolean isStatic = parseAccess();
		boolean isVoid = false;

		TypeDenoter td;
		if (startsType(currentToken)) {
			td = parseType();
		} else {
			SourcePosition pos = getPosition();
			accept(Token.VOID);
			isVoid = true;
			td = new BaseType(TypeKind.VOID, pos);
		}

		String name = currentToken.spelling;
		accept(Token.ID);

		// Need for both FieldDeclaration and MethodDeclaration
		FieldDecl fd = new FieldDecl(isPrivate, isStatic, td, name, p);

		// Resolve ambiguity
		if (currentToken.kind == Token.SEMICOLON && !isVoid) { // FieldDeclaration
			acceptIt();
			return fd;
		} else { // MethodDeclaration
			ParameterDeclList pl = new ParameterDeclList();
			StatementList sl = new StatementList();

			accept(Token.LPAREN);
			if (startsType(currentToken)) {
				pl = parseParameterList();
			}
			accept(Token.RPAREN);

			accept(Token.LCURLY);
			while (startsStatement(currentToken)) {
				Statement s = parseStatement();
				sl.add(s);
			}
			accept(Token.RCURLY);

			return new MethodDecl(fd, pl, sl, p);
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
	 * Type -> int | boolean | id | null | (int|id)[]
	 */
	TypeDenoter parseType() throws SyntaxError {
		SourcePosition p = getPosition();

		if (currentToken.kind == Token.INT) {
			BaseType intType = new BaseType(TypeKind.INT, p);
			acceptIt();

			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
				return new ArrayType(intType, p);
			} else {
				return intType;
			}

		} else if (currentToken.kind == Token.ID) {
			Identifier cn = new Identifier(currentToken);
			acceptIt();
			ClassType ct = new ClassType(cn, p);

			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
				return new ArrayType(ct, p);
			} else {
				return ct;
			}

		} else if (currentToken.kind == Token.BOOLEAN) {
			acceptIt();
			return new BaseType(TypeKind.BOOLEAN, p);

		} else {
			accept(Token.NULL);
			return new BaseType(TypeKind.NULL, p);
		}
	}

	/**
	 * ParameterList -> Type id (, Type id)*
	 */
	ParameterDeclList parseParameterList() throws SyntaxError {
		SourcePosition p = getPosition();
		ParameterDeclList pl = new ParameterDeclList();
		TypeDenoter t = parseType();
		String name = currentToken.spelling;
		accept(Token.ID);
		ParameterDecl pd = new ParameterDecl(t, name, p);
		pl.add(pd);

		while (currentToken.kind == Token.COMMA) {
			SourcePosition pos = getPosition();
			acceptIt();
			t = parseType();
			name = currentToken.spelling;
			accept(Token.ID);
			pd = new ParameterDecl(t, name, pos);
			pl.add(pd);
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
		SourcePosition p = getPosition();
		Reference ref = null;

		// Base reference
		if (currentToken.kind == Token.ID) {
			Identifier id = new Identifier(currentToken);
			acceptIt();
			ref = new IdRef(id, p);
		} else if (currentToken.kind == Token.THIS) {
			acceptIt();
			ref = new ThisRef(p);
		}

		// Qualified references
		while (currentToken.kind == Token.DOT) {
			acceptIt();
			ref = new QualRef(ref, new Identifier(currentToken), p);
			accept(Token.ID);
		}

		// Index references
		if (currentToken.kind == Token.LBRACKET) {
			acceptIt();
			Expression e = parseExpression();
			accept(Token.RBRACKET);
			ref = new IxRef(ref, e, p);
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
		SourcePosition p = getPosition();

		// Block statement
		if (currentToken.kind == Token.LCURLY) {
			acceptIt();
			StatementList sl = new StatementList();
			while (startsStatement(currentToken)) {
				sl.add(parseStatement());
			}
			accept(Token.RCURLY);
			return new BlockStmt(sl, p);
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
				return new IfStmt(b, t, e, p);
			}
			return new IfStmt(b, t, p);
		}

		// Return statement
		else if (currentToken.kind == Token.RETURN) {
			acceptIt();

			Expression e = null;
			if (startsExpression(currentToken)) {
				e = parseExpression();
			}

			accept(Token.SEMICOLON);
			return new ReturnStmt(e, p);
		}

		// While statement
		else if (currentToken.kind == Token.WHILE) {
			acceptIt();

			accept(Token.LPAREN);
			Expression e = parseExpression();
			accept(Token.RPAREN);

			Statement s = parseStatement();
			return new WhileStmt(e, s, p);
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
				VarDecl vd = new VarDecl(t, name, p);

				accept(Token.BECOMES);
				Expression e = parseExpression();
				accept(Token.SEMICOLON);
				return new VarDeclStmt(vd, e, p);
			}

			Reference r = parseReference();

			// Assign statement
			if (currentToken.kind == Token.BECOMES) {
				acceptIt();
				Expression e = parseExpression();
				accept(Token.SEMICOLON);
				return new AssignStmt(r, e, p);
			}

			// Call statement
			else {
				if (r instanceof IxRef) {
					parseError("Invalid call statement - cannot call index reference.");
				}
				accept(Token.LPAREN);
				ExprList el = new ExprList();
				if (startsExpression(currentToken)) {
					el = parseArgumentList();
				}
				accept(Token.RPAREN);
				accept(Token.SEMICOLON);
				return new CallStmt(r, el, p);
			}
		}
		throw new SyntaxError();
	}

	/**
	 * Expression -> Expression'
	 */
	Expression parseExpression() throws SyntaxError {
		Expression e = parseExpressionP1();
		return e;
	}

	/**
	 * Expression' -> Expression'' (|| Expression'')*
	 */
	Expression parseExpressionP1() throws SyntaxError {
		SourcePosition p = getPosition();
		Expression e1 = parseExpressionP2();
		while (currentToken.kind == Token.OR) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseExpressionP2();
			e1 = new BinaryExpr(o, e1, e2, p);
		}
		return e1;
	}

	/**
	 * Expression'' -> Expression'3 (&& Expression'3)*
	 */
	Expression parseExpressionP2() throws SyntaxError {
		SourcePosition p = getPosition();
		Expression e1 = parseExpressionP3();
		while (currentToken.kind == Token.AND) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseExpressionP3();
			e1 = new BinaryExpr(o, e1, e2, p);
		}
		return e1;
	}

	/**
	 * Expression'3 -> Expression'4 ((==|!=) Expression'4)*
	 */
	Expression parseExpressionP3() throws SyntaxError {
		SourcePosition p = getPosition();
		Expression e1 = parseExpressionP4();
		while (currentToken.kind == Token.EQ || currentToken.kind == Token.NEQ) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseExpressionP4();
			e1 = new BinaryExpr(o, e1, e2, p);
		}
		return e1;
	}

	/**
	 * Expression'4 -> Expression'5 ((<=|<|>|>=) Expression'5)*
	 */
	Expression parseExpressionP4() throws SyntaxError {
		SourcePosition p = getPosition();
		Expression e1 = parseExpressionP5();
		while (currentToken.kind == Token.LTE || currentToken.kind == Token.LT || currentToken.kind == Token.GT
				|| currentToken.kind == Token.GTE) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseExpressionP5();
			e1 = new BinaryExpr(o, e1, e2, p);
		}
		return e1;
	}

	/**
	 * Expression'5 -> Expression'6 ((+|-) Expression'6)*
	 */
	Expression parseExpressionP5() throws SyntaxError {
		SourcePosition p = getPosition();
		Expression e1 = parseExpressionP6();
		while (currentToken.kind == Token.ADD || currentToken.kind == Token.MINUS) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseExpressionP6();
			e1 = new BinaryExpr(o, e1, e2, p);
		}
		return e1;
	}

	/**
	 * Expression'6 -> Expression'7 ((*|/) Expression'7)*
	 */
	Expression parseExpressionP6() throws SyntaxError {
		SourcePosition p = getPosition();
		Expression e1 = parseExpressionP7();
		while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
			Operator o = new Operator(currentToken);
			acceptIt();
			Expression e2 = parseExpressionP7();
			e1 = new BinaryExpr(o, e1, e2, p);
		}
		return e1;
	}

	/**
	 * Expression '7 -> unop* Expression'8 [UnaryExpr]
	 */
	Expression parseExpressionP7() throws SyntaxError {
		SourcePosition p = getPosition();
		while (currentToken.kind == Token.NOT || currentToken.kind == Token.MINUS) {
			Operator o = new Operator(currentToken);
			acceptIt();
			return new UnaryExpr(o, parseExpressionP7(), p);
		}
		return parseExpressionP8();
	}

	/**
	 * @formatter:off
	 * Expression'8 ->
	 * 		Reference												[RefExpr]
	 * 	|	Reference '(' ArgumentList? ')'							[CallExpr]
	 * 	| 	'(' Expression ')'										[Explicit Precedence]
	 * 	| 	num | true | false										[LiteralExpr]
	 *  | 	new id '('')'											[NewObjectExpr]
	 *  |	new ( int [Expression] | id [Expression] ) 				[NewArrayExpr]
	 * @formatter:on
	 */
	Expression parseExpressionP8() throws SyntaxError {
		SourcePosition p = getPosition();

		// Reference/call expression
		if (startsReference(currentToken)) {
			Reference r = parseReference();

			// Call expression
			if (currentToken.kind == Token.LPAREN) {
				if (r instanceof IxRef) {
					parseError("Invalid call expression - cannot call index reference.");
				}
				acceptIt();
				ExprList el = new ExprList();
				if (startsExpression(currentToken)) {
					el = parseArgumentList();
				}
				accept(Token.RPAREN);
				return new CallExpr(r, el, p);
			}
			return new RefExpr(r, p);
		}

		// Explicit precedence
		else if (currentToken.kind == Token.LPAREN) {
			acceptIt();
			Expression e = parseExpression();
			accept(Token.RPAREN);
			return e;
		}

		// Integer literal expression
		else if (currentToken.kind == Token.NUM) {
			Terminal t = new IntLiteral(currentToken);
			acceptIt();
			return new LiteralExpr(t, p);
		}

		// Boolean literal expression
		else if (currentToken.kind == Token.TRUE || currentToken.kind == Token.FALSE) {
			Terminal t = new BooleanLiteral(currentToken);
			acceptIt();
			return new LiteralExpr(t, p);
		}

		// Null literal expression
		else if (currentToken.kind == Token.NULL) {
			Terminal t = new NullLiteral(currentToken);
			acceptIt();
			return new LiteralExpr(t, p);
		}

		// New expression
		else if (currentToken.kind == Token.NEW) {
			acceptIt();

			if (currentToken.kind == Token.ID) {
				Identifier id = new Identifier(currentToken);
				acceptIt();
				ClassType ct = new ClassType(id, p);

				// New object expression
				if (currentToken.kind == Token.LPAREN) {
					acceptIt();
					accept(Token.RPAREN);
					return new NewObjectExpr(ct, p);
				}

				// New object array expression
				else if (currentToken.kind == Token.LBRACKET) {
					acceptIt();
					Expression e = parseExpression();
					accept(Token.RBRACKET);
					return new NewArrayExpr(ct, e, p);
				}
			}

			// New integer array expression
			else if (currentToken.kind == Token.INT) {
				acceptIt();
				TypeDenoter t = new BaseType(TypeKind.INT, p);
				accept(Token.LBRACKET);
				Expression e = parseExpression();
				accept(Token.RBRACKET);

				return new NewArrayExpr(t, e, p);
			}

			// Invalid type
			else {
				String found = currentToken.spelling;
				String position = currentToken.position.toString();
				parseError("Expected valid type but found " + found + " at " + position + ".");
			}
		}

		String found = currentToken.spelling;
		String position = currentToken.position.toString();
		parseError("Invalid token " + found + " at " + position + ".");
		throw new SyntaxError();
	}

	boolean startsMemberDeclaration(Token token) {
		return token.kind == Token.PUBLIC || token.kind == Token.PRIVATE || token.kind == Token.STATIC
				|| token.kind == Token.INT || token.kind == Token.BOOLEAN || token.kind == Token.ID
				|| token.kind == Token.VOID;
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