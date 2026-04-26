package com.swna.javafx.view_ui.order;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.event.EventBus;
import com.swna.javafx.common.event.OrderSelectedEvent;
import com.swna.javafx.domain.order.OrderItem;
import com.swna.javafx.navigation.SceneManager;
import com.swna.javafx.view_ui.base.BaseController;
import com.swna.javafx.viewmodel.order.OrderItemViewModel;

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
