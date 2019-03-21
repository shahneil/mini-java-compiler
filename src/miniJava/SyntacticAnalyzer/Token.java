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
		WHILE = 11,
		TRUE = 12,
		FALSE = 13,
		NEW = 14,
	
		// operators
		GT = 15,
		LT = 16,
		EQ = 17,
		NEQ = 18,
		LTE = 19,
		GTE = 20,
		AND = 21,
		OR = 22,
		NOT = 23,
		ADD = 24,
		MINUS = 25,
		MULT = 26,
		DIV = 27,
		
		// brackets
		LCURLY = 28,
		RCURLY = 29,
		LBRACKET = 30,
		RBRACKET = 31,
		LPAREN = 32,
		RPAREN = 33,
		
		// punctuation
		COMMA = 34,
		COLON = 35,
		SEMICOLON = 36,
		DOT = 37,
		BECOMES = 38,
		
		// literals, identifiers
		NUM = 39,
		ID = 40,
		
		// special
		EOT = 41,
		ERROR = 42,
		NULL = 43;

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