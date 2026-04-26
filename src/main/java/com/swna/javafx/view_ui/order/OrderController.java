package com.swna.javafx.view_ui.order;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.event.EventBus;
import com.swna.javafx.common.event.OrderSelectedEvent;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.domain.order.Order;
import com.swna.javafx.navigation.SceneManager;
import com.swna.javafx.view_ui.base.BaseController;
import com.swna.javafx.viewmodel.order.OrderViewModel;

import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

@Component

public class OrderController extends BaseController {

    private final OrderViewModel viewModel;
    private final EventBus eventBus;


   public OrderController(SceneManager sceneManager, OrderViewModel viewModel, EventBus eventBus) {
        super(sceneManager); // ⭐ 반드시 필요

        this.viewModel = viewModel;
        this.eventBus = eventBus;
   }

    @FXML private TableView<Order> orderTable;

    @FXML private TableColumn<Order, Long> colId;
    @FXML private TableColumn<Order, String> colCustomer;
    @FXML private TableColumn<Order, LocalDateTime> colDate;
    @FXML private TableColumn<Order, LocalDateTime> colDatePicker;

    @FXML
    public void initialize() {

        // ================= Data binding =================
        orderTable.setItems(viewModel.getOrders());

        // ================= Columns =================
        TableColumnUtil.makeReadOnlyIntegerColumn( colId, Order::idProperty);
        TableColumnUtil.makeStringColumn(colCustomer,Order::customerNameProperty, Order::setCustomerName, true,viewModel::markDirty );
        TableColumnUtil.<Order>makeDateTimeColumn( colDate, Order::getOrderDate, Order::setOrderDate, false, viewModel::markDirty );
        TableColumnUtil.makeDatePickerColumn(colDate, Order::orderDateProperty, Order::setOrderDate,viewModel::markDirty);

        setupTable();
        setupPriceColumn();

        // ================= load =================
        viewModel.loadOrders();
    }

    @FXML
    private void onRefresh() {
        viewModel.loadOrders();
    }

    @FXML
    private void onSave() {
        viewModel.saveDirty();
    }

    private void setupTable() {
 
        // ================= 선택 이벤트 =================
        orderTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, selected) -> {

                    if (selected != null) {
                     eventBus.publish(new OrderSelectedEvent(selected.getId()));
                    }
                });

        // ⭐ dirty tracking row 배경색 변경 
        orderTable.setRowFactory(tv -> new TableRow<>() {

            @Override
            protected void updateItem(Order item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                } else if (viewModel.isDirty(item)) {
                    setStyle("-fx-background-color: #fff3cd;");
                } else {
                    setStyle("");
                }
            }
        });
    }


    private void setupPriceColumn() {

        colCustomer.setCellFactory(column -> new TableCell<>() {

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(String.valueOf(value));

                Order row = getTableView().getItems().get(getIndex());

                if (viewModel.isDirty(row)) {
                    setStyle("-fx-background-color: #ffeeba;");
                } else {
                    setStyle("");
                }
            }
        });
    }
}
