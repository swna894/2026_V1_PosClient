package com.swna.javafx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "api")
@Getter
@Setter
public class ApiProperties {
    private String baseUrl;
}
