package com.swna.javafx.dto.auth;

public record LoginResponse( String accessToken, String refreshToken, String role) {}
