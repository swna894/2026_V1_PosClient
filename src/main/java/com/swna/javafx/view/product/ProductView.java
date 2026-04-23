package com.swna.javafx.view.product;

import org.springframework.stereotype.Component;

import com.swna.javafx.common.event.AddOrderItemEvent;
import com.swna.javafx.common.event.EventBus;
import com.swna.javafx.common.ui.table.TableViewHelper;
import com.swna.javafx.domain.product.Product;
import com.swna.javafx.navigation.SceneManager;
import com.swna.javafx.view.base.BaseController;
import com.swna.javafx.viewmodel.product.ProductViewModel;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/view/product/ProductView.fxml")
public class ProductView extends BaseController {

    private final ProductViewModel viewModel;
    private final EventBus eventBus;


    public ProductView(SceneManager sceneManager, ProductViewModel viewModel,  EventBus eventBus) {
        super(sceneManager);
        this.viewModel = viewModel;
        this.eventBus = eventBus;
    }

    @FXML private ProgressIndicator progress;
    @FXML private Label pageLabel;

    @FXML private void onPrev() {}
    @FXML private void onNext() {}

    @FXML private TextField searchField;
    @FXML private TableView<Product> table;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Integer> colPrice;

    @FXML
    public void initialize() {

        // 컬럼 바인딩
        TableViewHelper.bindColumn(colName, "name");
        TableViewHelper.bindColumn(colPrice, "price");

        // 데이터 바인딩
        table.setItems(viewModel.getProducts());

        // 검색어 바인딩
        searchField.textProperty().bindBidirectional(viewModel.keywordProperty());

        // ================= LOADING UI 연결 =================
        progress.visibleProperty().bind(viewModel.loadingProperty());
        progress.managedProperty().bind(viewModel.loadingProperty());

        // ================= ERROR UI 연결 =================
        viewModel.errorProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.isEmpty()) {
                showError(newV);   // AlertSupport
            }
        });
    }

    // 검색
    @FXML
    private void onSearch() {
        viewModel.search();
    }

    // 주문 추가 (EventBus 활용)
    @FXML
    private void onAdd() {

        Product selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showInfo("상품을 선택하세요"); // BaseController
            return;
        }

        eventBus.publish(new AddOrderItemEvent(selected));

        showInfo("주문에 추가되었습니다");
    }

    // 화면 이동
    @FXML
    private void onMoveOrder() {
        move("/view/order/OrderView.fxml"); // NavigationSupport
    }
}
