package com.swna.javafx.common.exception;

public class ErrorPolicy {

    private final String message;
    private final boolean logout;

    public ErrorPolicy(String message, boolean logout) {
        this.message = message;
        this.logout = logout;
    }

    public String message() { return message; }
    public boolean logout() { return logout; }
}
