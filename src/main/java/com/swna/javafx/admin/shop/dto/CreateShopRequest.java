package com.swna.javafx.admin.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShopRequest(
        @NotBlank String name,
        @NotBlank String address,
        String phone,
        @NotBlank String businessNo

) {
}
