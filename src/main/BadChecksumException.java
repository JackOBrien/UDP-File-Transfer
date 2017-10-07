package main;

public class BadChecksumException extends Exception {
    private static final long serialVersionUID = 1L;

    int expected, received;

    public BadChecksumException(int expected, int received) {
        this.expected = expected;
        this.received = received;
    }

    @Override
    public String getMessage() {
        return "Bad Checksum. Expected: " + expected + " Got: " + received;
    }

    public int getExpected() {
        return expected;
    }

    public int getReceived() {
        return received;
    }

}
