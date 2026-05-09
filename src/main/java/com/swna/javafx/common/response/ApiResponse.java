package com.swna.javafx.common.response;

import java.util.Map;

/**
 * API 응답 래퍼 클래스
 * 
 * @param <T> 응답 데이터 타입
 * @param success 성공 여부
 * @param data 응답 데이터
 * @param code 에러 코드 (성공 시 null)
 * @param message 응답 메시지
 * @param timestamp 응답 시간
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Map<String, Object> details
) { 
    
    // =========================
    // 상수 (Common Error Codes)
    // =========================
    
    public static final String SUCCESS_CODE = "SUCCESS";
    public static final String ERROR_CODE_DEFAULT = "ERROR";
    public static final String ERROR_CODE_NOT_FOUND = "NOT_FOUND";
    public static final String ERROR_CODE_INVALID_INPUT = "INVALID_INPUT";
    public static final String ERROR_CODE_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ERROR_CODE_FORBIDDEN = "FORBIDDEN";
    public static final String ERROR_CODE_SERVER_ERROR = "SERVER_ERROR";
    public static final String ERROR_CODE_NETWORK_ERROR = "NETWORK_ERROR";
    public static final String ERROR_CODE_TIMEOUT = "TIMEOUT";
    
    
    // =========================
    // Instance Methods
    // =========================
    
    /**
     * 성공 여부 반환 (null-safe)
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 실패 여부 반환
     */
    public boolean isError() {
        return !success;
    }
    
    /**
     * 데이터 존재 여부
     */
    public boolean hasData() {
        return data != null;
    }
    
    /**
     * 데이터가 없거나 null인 경우 기본값 반환
     */
    public T orElse(T defaultValue) {
        return hasData() ? data : defaultValue;
    }
    
    /**
     * 데이터가 없을 때 Supplier로 기본값 제공
     */
    public T orElseGet(java.util.function.Supplier<T> supplier) {
        return hasData() ? data : supplier.get();
    }

    
    /**
     * 데이터가 없을 때 커스텀 예외 발생
     */
    public <X extends Throwable> T orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws X {
        if (!hasData()) {
            throw exceptionSupplier.get();
        }
        return data;
    }
    
    /**
     * 에러 코드가 특정 값과 일치하는지 확인
     */
    public boolean hasErrorCode(String errorCode) {
        return !success && code != null && code.equals(errorCode);
    }
    
    /**
     * 사용자 친화적 에러 메시지 반환
     */
    public String getUserFriendlyMessage() {
        if (success) {
            return message != null ? message : "성공";
        }
        
        if (code == null) {
            return message != null ? message : "오류가 발생했습니다.";
        }
        
        return switch (code) {
            case ERROR_CODE_NOT_FOUND -> "🔍 " + (message != null ? message : "데이터를 찾을 수 없습니다.");
            case ERROR_CODE_INVALID_INPUT -> "❌ " + (message != null ? message : "잘못된 입력입니다.");
            case ERROR_CODE_UNAUTHORIZED -> "🔒 " + (message != null ? message : "로그인이 필요합니다.");
            case ERROR_CODE_FORBIDDEN -> "🚫 " + (message != null ? message : "접근 권한이 없습니다.");
            case ERROR_CODE_SERVER_ERROR -> "🖥️ " + (message != null ? message : "서버 오류가 발생했습니다.");
            case ERROR_CODE_NETWORK_ERROR -> "🌐 " + (message != null ? message : "네트워크 연결을 확인해주세요.");
            case ERROR_CODE_TIMEOUT -> "⏰ " + (message != null ? message : "요청 시간이 초과되었습니다.");
            default -> (message != null ? message : "오류가 발생했습니다.");
        };
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, SUCCESS_CODE, message, data, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, SUCCESS_CODE, null, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, Map<String, Object> details) {
        return new ApiResponse<>(false, code, message, null, details);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, ERROR_CODE_DEFAULT, message, null, null);
    }
    
}