package com.swna.javafx.common.response;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        String code,
        String message,
        LocalDateTime timestamp
) {}
