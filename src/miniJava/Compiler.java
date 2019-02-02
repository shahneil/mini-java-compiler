package miniJava;

import java.io.IOException;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourceFile;

public class Compiler {

	private static Scanner scanner;
	private static Parser parser;
	private static ErrorReporter reporter;

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Missing argument filename");
			System.exit(3);
		}

		String sourceName = args[0];
		compile(sourceName);
	}

	private static void compile(String sourceName) {
		SourceFile sourceFile = new SourceFile(sourceName);

		if (!sourceFile.opened) {
			System.out.println("Source file " + sourceName + " not found.");
			System.exit(3);
		}

		reporter = new ErrorReporter();
		scanner = new Scanner(sourceFile);
		parser = new Parser(scanner, reporter);

		parser.parse();

		boolean success = !reporter.hasErrors();
		if (success) {
			System.out.println("Valid miniJava program " + sourceName);
			System.exit(0);
		} else {
			System.out.println("Invalid miniJava program " + sourceName);
			System.exit(4);
		}

	}
}
