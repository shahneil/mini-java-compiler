package miniJava.SyntacticAnalyzer;

public class Token {
	public int kind;
	public String spelling;
	public SourcePosition position;

	public Token(int kind, String spelling, SourcePosition position) {
		// If token is an identifier, check if its spelling matches any reserved words.
		if (kind == ID) {
			for (int i = 0; i < tokenTable.length; i++) {
				if (spelling.equals(tokenTable[i])) {
					kind = i;
					break;
				}
			}
		}

		this.kind = kind;
		this.spelling = spelling;
		this.position = position;
	}

	public static String spell(int kind) {
		return tokenTable[kind];
	}

	/**
	 * Token classes
	 */
	//@formatter:off
	public static final int
		
		// keywords
		CLASS = 0,
		VOID = 1,
		PUBLIC = 2,
		PRIVATE = 3,
		STATIC = 4,
		INT = 5,
		BOOLEAN = 6,
		THIS = 7,
		RETURN = 8,
		IF = 9,
		ELSE = 10,
		FOR = 11,
		WHILE = 12,
		TRUE = 13,
		FALSE = 14,
		NEW = 15,
	
		// operators
		GT = 16,
		LT = 17,
		EQ = 18,
		NEQ = 19,
		LTE = 20,
		GTE = 21,
		AND = 22,
		OR = 23,
		NOT = 24,
		ADD = 25,
		MINUS = 26,
		MULT = 27,
		DIV = 28,
		
		// brackets
		LCURLY = 29,
		RCURLY = 30,
		LBRACKET = 31,
		RBRACKET = 32,
		LPAREN = 33,
		RPAREN = 34,
		
		// punctuation
		COMMA = 35,
		COLON = 36,
		SEMICOLON = 37,
		DOT = 38,
		BECOMES = 39,
		
		// literals, identifiers
		NUM = 40,
		ID = 41,
		
		// special
		EOT = 42,
		ERROR = 43,
		NULL = 44;

	/**
	 * Returns a map of keyword spellings to token kinds.
	 */
	private static String[] tokenTable = new String[] {
		"class",
		"void",
		"public",
		"private",
		"static",
		"int",
		"boolean",
		"this",
		"return",
		"if",
		"else",
		"for",
		"while",
		"true",
		"false",
		"new",
		">",
		"<",
		"==",
		"!=",
		"<=",
		">=",
		"&&",
		"||",
		"!",
		"+",
		"-",
		"*",
		"/",
		"{",
		"}",
		"[",
		"]",
		"(",
		")",
		",",
		":",
		";",
		".",
		"=",
		"<num>",
		"<id>",
		"",
		"<error>",
		"null"
	};
	//@formatter:on

}