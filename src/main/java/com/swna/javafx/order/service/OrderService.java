package com.swna.javafx.order.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swna.javafx.order.model.Order;
import com.swna.javafx.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;

    public Flux<Order> loadOrders() {
        return repository.fetchOrdersReactive();
    }

    // public Mono<List<Order>> saveAll(List<Order> orders) {
    //     return repository.saveAll(orders);

    // }

    public List<Order> findAllOrders() {
        return repository.fetchOrders();
    }

    public void saveAll(List<Order> orders) {
        repository.saveAll(orders);
    }
}
