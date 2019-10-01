package main;

public class BadChecksumException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private static int expected;

	private static int received;
	
	public BadChecksumException(int expected, int received) {
		this.expected = expected;
		this.received = received;
	}
	
	@Override
	public String getMessage() {
		return "Bad Checksum. Expected: " + expected + " Got: " + received;
	}
}
