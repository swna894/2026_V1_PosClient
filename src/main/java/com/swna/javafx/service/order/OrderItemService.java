package com.swna.javafx.service.order;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swna.javafx.domain.order.OrderItem;
import com.swna.javafx.repository.order.OrderItemApiRepository;

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