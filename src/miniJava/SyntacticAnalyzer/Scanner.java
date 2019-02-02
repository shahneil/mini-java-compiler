package miniJava.SyntacticAnalyzer;

import java.util.LinkedList;
import java.util.Queue;

public class Scanner {
	private SourceFile sourceFile;
	private char currentChar;
	private StringBuilder currentSpelling;
	private Queue<Token> peekedTokens;

	public Scanner(SourceFile source) {
		sourceFile = source;
		currentChar = sourceFile.read();
		currentSpelling = new StringBuilder();
		peekedTokens = new LinkedList<>();
	}

	/**
	 * Scans next token in source file.
	 */
	public Token scan() {
		if (!peekedTokens.isEmpty()) {
			return peekedTokens.remove();
		}

		currentSpelling = new StringBuilder();

		Token found = scanSeparator();
		if (found != null) {
			return found;
		}

		SourcePosition position = sourceFile.getCurrentPosition();
		int kind = scanToken();
		String spelling = currentSpelling.toString();

		return new Token(kind, spelling, position);
	}

	/**
	 * Peeks next token in source file.
	 */
	public Token peek() {
		currentSpelling = new StringBuilder();
		Token peekedToken;

		Token found = scanSeparator();
		if (found != null) {
			peekedToken = found;
			peekedTokens.add(peekedToken);
			return peekedToken;
		}

		SourcePosition position = sourceFile.getCurrentPosition();
		int kind = scanToken();
		String spelling = currentSpelling.toString();

		peekedToken = new Token(kind, spelling, position);
		peekedTokens.add(peekedToken);
		return peekedToken;
	}

	/**
	 * Scans whitespace and comments between tokens. Returns a token if found (due
	 * to ambiguity between /, //, and /*), otherwise returns null.
	 */
	private Token scanSeparator() {
		while (currentChar == ' ' || currentChar == '\n' || currentChar == '\t' || currentChar == '\r'
				|| currentChar == '/') {

			if (currentChar == '/') {
				skipIt();

				if (currentChar == '/') {
					while (currentChar != '\n' && currentChar != '\r' && currentChar != SourceFile.eot) {
						skipIt();
					}
					skipIt();
				} else if (currentChar == '*') {
					char prevChar = ' ';
					while (prevChar != '*' || currentChar != '/' && currentChar != SourceFile.eot) {
						prevChar = currentChar;
						skipIt();
					}
					skipIt();
				} else {
					SourcePosition position = sourceFile.getCurrentPosition();
					return new Token(Token.DIV, "/", position);
				}
			} else {
				skipIt();
			}
		}
		return null;
	}

	/**
	 * Scans token and returns its kind. Updates currentSpelling accordingly.
	 */
	private int scanToken() {

		if (isLetter(currentChar)) {
			takeIt();
			while (isLetter(currentChar) || isDigit(currentChar) || currentChar == '_') {
				takeIt();
			}
			return Token.ID;
		}

		if (isDigit(currentChar)) {
			takeIt();
			while (isDigit(currentChar)) {
				takeIt();
			}
			return Token.NUM;
		}

		switch (currentChar) {

		case SourceFile.eot:
			return Token.EOT;

		case '>':
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return Token.GTE;
			}
			return Token.GT;

		case '<':
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return Token.LTE;
			}
			return Token.LT;

		case '=':
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return Token.EQ;
			}
			return Token.BECOMES;

		case '!':
			takeIt();
			if (currentChar == '=') {
				takeIt();
				return Token.NEQ;
			}
			return Token.NOT;

		case '&':
			takeIt();
			if (currentChar == '&') {
				takeIt();
				return Token.AND;
			}
			return Token.ERROR;

		case '|':
			takeIt();
			if (currentChar == '|') {
				takeIt();
				return Token.OR;
			}
			return Token.ERROR;

		case '+':
			takeIt();
			return Token.ADD;

		case '-':
			takeIt();
			return Token.MINUS;

		case '*':
			takeIt();
			return Token.MULT;

		case '/':
			takeIt();
			return Token.DIV;

		case '{':
			takeIt();
			return Token.LCURLY;

		case '}':
			takeIt();
			return Token.RCURLY;

		case '[':
			takeIt();
			return Token.LBRACKET;

		case ']':
			takeIt();
			return Token.RBRACKET;

		case '(':
			takeIt();
			return Token.LPAREN;

		case ')':
			takeIt();
			return Token.RPAREN;

		case ',':
			takeIt();
			return Token.COMMA;

		case ':':
			takeIt();
			return Token.COLON;

		case ';':
			takeIt();
			return Token.SEMICOLON;

		case '.':
			takeIt();
			return Token.DOT;

		default:
			takeIt();
			return Token.ERROR;
		}
	}

	/**
	 * Reads the next character from the source file, without appending the current
	 * character to the current token.
	 */
	private void skipIt() {
		currentChar = sourceFile.read();
	}

	/**
	 * Appends the current character to the current token, and reads the next
	 * character from the source file.
	 */
	private void takeIt() {
		currentSpelling.append(currentChar);
		currentChar = sourceFile.read();
	}

	private boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	private boolean isDigit(char c) {
		return (c >= '0' && c <= '9');
	}
}
