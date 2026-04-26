package com.swna.javafx.repository.order;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.domain.order.OrderItem;

@Repository
public class OrderItemApiRepository {

    private final WebClient client;

    public OrderItemApiRepository(WebClient client) {
        this.client = client;
    }

    public List<OrderItem> fetchByOrderId(Long orderId) {

        return client.get()
                .uri("/orders/{id}/items", orderId)
                .retrieve()
                .bodyToFlux(OrderItem.class)
                .collectList()
                .block();
    }

    public void saveAll(List<OrderItem> items) {

        client.post()
                .uri("/order-items/bulk")
                .bodyValue(items)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}