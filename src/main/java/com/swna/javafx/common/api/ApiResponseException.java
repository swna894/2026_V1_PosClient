package com.swna.javafx.common.api;

import java.util.Map;

/**
 * API 응답 예외 클래스
 */
public class ApiResponseException extends RuntimeException {
    
    private final String code;
    private final Map<String, Object> details;
    private final String suggestion;
    
    // 기본 생성자
    public ApiResponseException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
        this.suggestion = null;
    }
    
    // details를 포함한 생성자
    public ApiResponseException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
        this.suggestion = null;
    }
    
    // suggestion을 포함한 생성자
    public ApiResponseException(String code, String message, Map<String, Object> details, String suggestion) {
        super(message);
        this.code = code;
        this.details = details;
        this.suggestion = suggestion;
    }
    
    // 모든 필드를 포함한 생성자
    public ApiResponseException(String code, String message, Map<String, Object> details, String suggestion, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = details;
        this.suggestion = suggestion;
    }
    
    // Getter 메서드들
    public String getCode() {
        return code;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    @Override
    public String toString() {
        return String.format("ApiResponseException{code='%s', message='%s', suggestion='%s', details=%s}", 
            code, getMessage(), suggestion, details);
    }
}
