package com.swna.javafx.admin.shop;

import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class Shop {

    private Long id;

    private String name;

    private String address;

    private String phone;

    private String businessNo;

    private boolean active = true;

    // =========================
    // Factory
    // =========================
    public static Shop create(String name, String address, String phone, String businessNo) {

        Shop shop = new Shop();

        shop.name = name;
        shop.address = address;
        shop.phone = phone;
        shop.businessNo = businessNo;
        shop.active = true;

        return shop;
    }

    // =========================
    // Business
    // =========================
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
