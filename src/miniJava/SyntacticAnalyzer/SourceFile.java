package miniJava.SyntacticAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SourceFile {
	private File sourceFile;
	private FileInputStream source;
	private int currentCol;
	private int currentLine;
	public boolean opened;

	public static final char eot = '\u0000';

	public SourceFile(String fileName) {
		try {
			sourceFile = new File(fileName);
			source = new FileInputStream(sourceFile);
			currentCol = 1;
			currentLine = 1;
			opened = true;
		} catch (IOException e) {
			sourceFile = null;
			source = null;
			currentLine = 0;
			opened = false;
		}
	}

	/**
	 * Returns the next character in the source file.
	 */
	public char read() {
		try {
			int c = source.read();
			if (c == -1) {
				c = eot;
			} else if (c == '\n') {
				currentLine++;
				currentCol = 1;
			} else {
				currentCol++;
			}
			return (char) c;
		} catch (IOException e) {
			return eot;
		}
	}

	public SourcePosition getCurrentPosition() {
		return new SourcePosition(currentLine, currentCol);
	}

}
