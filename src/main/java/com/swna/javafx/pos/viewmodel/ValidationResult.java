// ValidationResult.java
package com.swna.javafx.pos.viewmodel;

public final class ValidationResult {
    private final boolean valid;
    private final String errorMessage;
    
    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }
    
    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isSuccess() {
        return valid;
    }
}