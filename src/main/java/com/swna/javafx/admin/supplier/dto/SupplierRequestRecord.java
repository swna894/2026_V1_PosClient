package com.swna.javafx.admin.supplier.dto;

import com.swna.javafx.admin.supplier.domain.Supplier;

public record SupplierRequestRecord(
        Long id,
        String abbr,
        String name,
        String company,
        String phone,
        String cellphone,
        String email,
        String address,
        Boolean active
) {
    public static SupplierRequestRecord from(Supplier supplier) {
        return new SupplierRequestRecord(
                supplier.getId(),
                supplier.getAbbr(),
                supplier.getName(),
                supplier.getCompany(),
                supplier.getPhone(),
                supplier.getCellphone(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.isActive()
        );
    }
}