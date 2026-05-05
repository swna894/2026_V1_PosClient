package com.swna.javafx.login.dto;

public record LoginResponse( String accessToken, String refreshToken, String role) {}
