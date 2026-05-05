package com.swna.javafx.order;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.event.EventBus;
import com.swna.javafx.common.event.OrderSelectedEvent;
import com.swna.javafx.common.navigation.SceneManager;
import com.swna.javafx.controller.base.BaseController;
import com.swna.javafx.order.model.OrderItem;
import com.swna.javafx.order.viewModel.OrderItemViewModel;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;

@Component
public class OrderItemController extends BaseController {

    private final OrderItemViewModel viewModel;
    private final EventBus eventBus;

   public OrderItemController(SceneManager sceneManager, OrderItemViewModel viewModel,EventBus eventBus) {
        super(sceneManager); // ⭐ 반드시 필요

        this.viewModel = viewModel;
        this.eventBus = eventBus;
   }

    @FXML private TableView<OrderItem> itemTable;

    @FXML
    public void initialize() {

        itemTable.setItems(viewModel.getItems());

        // ================= Order 선택 수신 =================
         eventBus.subscribe(OrderSelectedEvent.class, event -> {
            viewModel.loadItems(event.orderId());
         });
    }

    @FXML
    private void onSave() {
        viewModel.saveDirtyItems();
    }
}
