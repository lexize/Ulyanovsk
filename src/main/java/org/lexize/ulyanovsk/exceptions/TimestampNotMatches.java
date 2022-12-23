package org.lexize.ulyanovsk.exceptions;

public class TimestampNotMatches extends Exception {
    private String _timestamp;
    public TimestampNotMatches(String timestamp) {
        _timestamp = timestamp;
    }

    @Override
    public String getMessage() {
        return "Timestamp %s not matches timestamp format".formatted(_timestamp);
    }
}
