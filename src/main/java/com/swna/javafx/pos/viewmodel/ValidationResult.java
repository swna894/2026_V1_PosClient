package com.swna.javafx.pos.viewmodel;

import java.util.function.Consumer;
import java.util.function.Function;

import lombok.Getter;

@Getter
public class ValidationResult {
    
    private final boolean valid;
    private final String errorMessage;
    
    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }
    
    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    // ✅ 체이닝 메서드 추가
    public ValidationResult ifInvalid(Runnable action) {
        if (!valid) {
            action.run();
        }
        return this;
    }
    
    public ValidationResult ifInvalid(Consumer<String> action) {
        if (!valid) {
            action.accept(errorMessage);
        }
        return this;
    }
    
    public ValidationResult ifValid(Runnable action) {
        if (valid) {
            action.run();
        }
        return this;
    }
    
    public <T> T map(Function<ValidationResult, T> mapper, T defaultValue) {
        return valid ? mapper.apply(this) : defaultValue;
    }
    
    // 예외를 던지는 버전
    public ValidationResult orElseThrow() throws IllegalStateException {
        if (!valid) {
            throw new IllegalStateException(errorMessage);
        }
        return this;
    }
    
    public ValidationResult orElseThrow(Function<String, ? extends RuntimeException> exceptionProvider) {
        if (!valid) {
            throw exceptionProvider.apply(errorMessage);
        }
        return this;
    }
}