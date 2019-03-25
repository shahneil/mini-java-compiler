package miniJava;

public class ErrorReporter {

	public int numErrors;

	public ErrorReporter() {
		numErrors = 0;
	}

	public void reportError(String message) {
		System.out.println(message);
		numErrors++;
	}

	public boolean hasErrors() {
		return numErrors > 0;
	}
}
