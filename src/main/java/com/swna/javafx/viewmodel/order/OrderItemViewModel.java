package com.swna.javafx.viewmodel.order;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.tracking.DirtyTracker;
import com.swna.javafx.domain.order.OrderItem;
import com.swna.javafx.service.order.OrderItemService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
public class OrderItemViewModel {

    private final OrderItemService service;
    private final DirtyTracker<OrderItem> dirtyTracker = new DirtyTracker<>();

    private final ObservableList<OrderItem> items = FXCollections.observableArrayList();

    public OrderItemViewModel(OrderItemService service) {
        this.service = service;
    }

    public ObservableList<OrderItem> getItems() {
        return items;
    }

    // ================= MASTER → DETAIL =================
    public void loadItems(Long orderId) {
        items.setAll(service.findByOrderId(orderId));
    }

    // ================= DIRTY SAVE =================
    public void saveDirtyItems() {

        List<OrderItem> changed = new ArrayList<>(dirtyTracker.getDirtyItems());

        service.saveAll(changed);

        dirtyTracker.clear();
    }

    public void updateItem(OrderItem item) {

        //service.updateCache(item);   // optional

        dirtyTracker.markDirty(item); // 여기서만 dirty 처리
    }
}
