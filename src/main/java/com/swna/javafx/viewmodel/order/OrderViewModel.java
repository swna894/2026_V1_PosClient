package com.swna.javafx.viewmodel.order;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.tracking.DirtyTracker;
import com.swna.javafx.domain.order.Order;
import com.swna.javafx.domain.order.OrderItem;
import com.swna.javafx.service.order.OrderService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
public class OrderViewModel {

   private final OrderService service;
   private final DirtyTracker<Order> dirtyTracker = new DirtyTracker<>();

   public OrderViewModel(OrderService service) {
        this.service = service;
    }

    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final ObservableList<OrderItem> items = FXCollections.observableArrayList();

    public ObservableList<Order> getOrders() {
        return orders;
    }

    public ObservableList<OrderItem> getItems() {
        return items;
    }

    // public void selectOrder(Order order) {
    //     items.setAll(order.getItems());
    // }

    public boolean isDirty(Order order) {
        return dirtyTracker.contains(order);
    }

    public void markDirty(Order order) {
        dirtyTracker.markDirty(order);
    }

        // 🔥 Reactive async loading
    public void loadOrders() {
        service.loadOrders()
                .collectList()
                .subscribe(result -> {
                    Platform.runLater(() -> {
                        orders.setAll(result);
                    });
                }, Throwable::printStackTrace);

    }

    public void saveDirty() {
        service.saveAll(List.copyOf(dirtyTracker.getDirtyItems()));
        dirtyTracker.clear();
    }

//     public void loadOrders() {

//       List<Order> result = service.findAllOrders();

//       orders.setAll(result);
//    }
}
