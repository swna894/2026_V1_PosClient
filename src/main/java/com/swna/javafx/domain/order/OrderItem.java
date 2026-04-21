package com.swna.javafx.domain.order;

import lombok.Data;

@Data
public class OrderItem {

    private Long id;
    private Long orderId;

    private String productName;
    private int qty;
    private double price;

    private Long version;
}
