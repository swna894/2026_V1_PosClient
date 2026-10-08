package com.swna.javafx.admin.user.domain;

public record UserRecordDto(
    Long id,
    String email,
    String name,
    Role role,
    String city,
    String street,
    String surburb,
    String phone,
    String mobile
) {
    // User 도메인 객체를 받아서 DTO로 변환해 주는 팩토리 메서드
    public static UserRecordDto from(User user) {
        return new UserRecordDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole(),
            user.getCity(),
            user.getStreet(),
            user.getSurburb(),
            user.getPhone(),
            user.getMobile()
        );
    }
}
