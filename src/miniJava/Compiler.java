package miniJava;

import java.io.IOException;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.ContextualAnalyzer.TypeChecking;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourceFile;

public class Compiler {

	private static Scanner scanner;
	private static Parser parser;
	private static TypeChecking checker;
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
		checker = new TypeChecking(reporter);

		try {

			// Syntactic analysis
			System.out.println("Syntactic analysis...");
			ast = parser.parse();
			System.out.println("Syntactic analysis completed.");

			// Contextual analysis
			System.out.println("Contextual analysis...");
			checker.check(ast);
			System.out.println("Contextual analysis completed.");

		} catch (Error e) {
		}

		// Exit with appropriate code
		if (reporter.hasErrors()) {
			System.out.println("Compilation unsuccessful.");
			System.exit(4);
		} else {
			ASTDisplay display = new ASTDisplay();
			display.showTree(ast);
			System.out.println("Compilation successful.");
			System.exit(0);
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
