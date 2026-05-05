package com.swna.javafx.order.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swna.javafx.order.model.OrderItem;
import com.swna.javafx.order.repository.OrderItemApiRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemApiRepository repository;

    // ================= 조회 =================
    public List<OrderItem> findByOrderId(Long orderId) {
        return repository.fetchByOrderId(orderId);
    }

    // ================= 저장 =================
    public void saveAll(List<OrderItem> items) {

        if (items == null || items.isEmpty()) {
            return;
        }

        repository.saveAll(items);
    }
}