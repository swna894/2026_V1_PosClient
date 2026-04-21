package com.swna.javafx.repository.order;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.domain.order.Order;

import javafx.application.Platform;
import reactor.core.publisher.Flux;

@Repository
public class OrderRepository {

    private final WebClient client =
            WebClient.create("http://localhost:8080");

    public List<Order> fetchOrders() {

        return client.get()
                .uri("/orders")
                .retrieve()
                .bodyToFlux(Order.class)
                .collectList()
                .block();
    }

    public Flux<Order> fetchOrdersReactive() {
        return client.get()
                .uri("/orders")
                .retrieve()
                .bodyToFlux(Order.class);
    }

    public List<Order> saveAll(List<Order> orders) {
        return client.post()
                .uri("/orders/batch")
                .bodyValue(orders)
                .retrieve()
                .bodyToFlux(Order.class)
                .collectList()
                .block();
    }
}
