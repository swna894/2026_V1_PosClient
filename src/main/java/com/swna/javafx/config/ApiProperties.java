package com.swna.javafx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 외부 설정 파일(application.properties)의 데이터를 관리하는 프로퍼티 클래스
 */

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "api")
public class ApiProperties {
    /**
     * api.base-url 설정값이 이 변수에 할당됩니다.
     * 예: application.properties api: base-url: https://google.com 이 있으면 해당 값이 저장됩니다.
     */
    private String baseUrl;
}