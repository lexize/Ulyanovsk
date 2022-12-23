package org.lexize.ulyanovsk.exceptions;

public class TimestampKeyNotFound extends Exception{
    private String _key;
    public TimestampKeyNotFound(String key) {_key = key;}

    @Override
    public String getMessage() {
        return "No standard key or alias matching %s".formatted(_key);
    }
}
