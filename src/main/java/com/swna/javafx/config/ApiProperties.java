package com.swna.javafx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "api")
@Getter
@Setter
public class ApiProperties {
    private String baseUrl;
}