package com.swna.javafx.admin.supplier.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record SupplierResponseRecord(
    Long id,
    String abbr,
    String name,
    String company,
    String email,
    String phone,
    String cellphone,
    String address,
    boolean active,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updatedAt
) {
    public String getDisplayName() {
        return String.format("%s (%s)", name, abbr);
    }
    
    public String getFullContact() {
        return String.format("%s / %s / %s", name, phone, email);
    }
    
    public boolean hasPhone() {
        return phone != null && !phone.isBlank();
    }
    
    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}