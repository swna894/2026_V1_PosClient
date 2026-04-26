package com.swna.javafx.common.exception;

import java.util.Map;

public class ErrorPolicyResolver {

    private static final Map<ClientErrorCode, ErrorPolicy> POLICY_MAP = Map.of(

        // AUTH
        ClientErrorCode.AUTH_INVALID_PASSWORD,
        new ErrorPolicy("비밀번호가 틀렸습니다.", false),

        ClientErrorCode.AUTH_UNAUTHORIZED,
        new ErrorPolicy("로그인이 필요합니다.", true),

        ClientErrorCode.AUTH_TOKEN_EXPIRED,
        new ErrorPolicy("세션이 만료되었습니다.", true),

        // USER
        ClientErrorCode.USER_NOT_FOUND,
        new ErrorPolicy("사용자를 찾을 수 없습니다.", false)

    );

    public static ErrorPolicy resolve(ClientErrorCode code) {
        return POLICY_MAP.getOrDefault(
                code,
                new ErrorPolicy("알 수 없는 오류", false)
        );
    }
}
