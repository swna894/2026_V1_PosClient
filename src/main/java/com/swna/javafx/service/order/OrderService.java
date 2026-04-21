package com.swna.javafx.service.order;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.swna.javafx.domain.order.Order;
import com.swna.javafx.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
