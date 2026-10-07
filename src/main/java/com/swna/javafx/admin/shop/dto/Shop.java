package com.swna.javafx.admin.shop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@ToString
@Component
public class Shop {

    private Long id;

    @NotBlank
    private String company;

    @NotBlank
    private String businessNo;

    @NotBlank
    private String name;

    @Email
    private String email;

    @NotBlank
    private String password;

    private String ccEmail;
    private String mobilePhone;
    private String phone;
    private String street;
    private String suburb;
    private String city;
    private String comment;
    private String backupFolder;
    private String reportFolder;
    @JsonIgnore
    private String address; 

    private boolean active = true;

    // =========================
    // Factory Method
    // =========================
    public static Shop create(
            String company,
            String businessNo,
            String name,
            String email,
            String password,
            String ccEmail,
            String mobilePhone,
            String phone,
            String street,
            String suburb,
            String city,
            String comment,
            String backupFolder,
            String reportFolder
    ) {
        Shop shop = new Shop();
        shop.company = company;
        shop.businessNo = businessNo;
        shop.name = name;
        shop.email = email;
        shop.password = password;
        shop.ccEmail = ccEmail;
        shop.mobilePhone = mobilePhone;
        shop.phone = phone;
        shop.street = street;
        shop.suburb = suburb;
        shop.city = city;
        shop.comment = comment;
        shop.backupFolder = backupFolder;
        shop.reportFolder = reportFolder;
        shop.active = true;

        return shop;
    }

    // =========================
    // Business Methods
    // =========================
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public String getAddress() {
        StringBuilder addressBuilder = new StringBuilder();
        if (street != null && !street.isEmpty()) {
            addressBuilder.append(street);
        }
        if (suburb != null && !suburb.isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(suburb);
        }
        if (city != null && !city.isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(city);
        }
        return addressBuilder.toString();
    }

    public Shop createDefaultShop() {
        return Shop.create(
            "Default Company",     // company
            "000-00-00000",        // businessNo
            "My Store",            // name
            "default@store.com",   // email
            "1234",                // password (또는 초기 암호)
            "",                    // ccEmail
            "010-0000-0000",       // mobilePhone
            "02-000-0000",         // phone
            "Default Street",      // street
            "Default Suburb",      // suburb
            "Default City",        // city
            "Initial setup shop",  // comment
            "/backup",             // backupFolder
            "/report"              // reportFolder
        );
    }
}