package miniJava;

import java.io.IOException;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourceFile;

public class Compiler {

	private static Scanner scanner;
	private static Parser parser;
	private static ErrorReporter reporter;
	private static AST ast;

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Missing argument filename.");
			System.exit(3);
		}

		String sourceName = args[0];
		compile(sourceName);
	}

	private static void compile(String sourceName) {
		SourceFile sourceFile = new SourceFile(sourceName);

		if (!sourceFile.opened) {
			System.out.println("Failed to open source file " + sourceName + ".");
			System.exit(3);
		}

		reporter = new ErrorReporter();
		scanner = new Scanner(sourceFile);
		parser = new Parser(scanner, reporter);

		ast = parser.parse();

		boolean success = !reporter.hasErrors();
		if (success) {
			// Display constructed AST using showTree method in ASTDisplay class
			ASTDisplay display = new ASTDisplay();
			display.showTree(ast);

			System.out.println("Compilation successful.");
			System.exit(0);
		} else {
			System.out.println("Compilation unsuccessful.");
			System.exit(4);
		}

	}

	/**
	 * @formatter:off
	 * Modifications to AST package:
	 * NullLiteral (and ASTDisplay)
	 * Identifier -> Declaration decl
	 * Reference -> Declaration decl, String spelling
	 * Terminal -> parameter names
	 * ClassDecl -> idTable
	 * 
	 * @formatter:on
	 */
}
