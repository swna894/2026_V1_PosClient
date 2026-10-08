package com.swna.javafx.admin.supplier;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.factory.SupplierTableFactory;
import com.swna.javafx.admin.supplier.viewmodel.SupplierViewModel;
import com.swna.javafx.common.navigation.NavigationService;
import com.swna.javafx.pos.PosViewController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/supplier-view.fxml")
public class SupplierController implements Initializable {
    
    private final SupplierViewModel viewModel;
    private final NavigationService navigationService;
    private final SupplierTableFactory tableFactory; // 👈 Factory 주입
    
    @FXML private TextField searchField;
    @FXML private CheckBox activeOnlyCheckBox;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label countLabel;
    
    @FXML private Button refreshButton;
    @FXML private Button newButton;
    @FXML private Button backButton;
    @FXML private Button deleteButton;

    @FXML private TableView<Supplier> tableView;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupTableProperties();
        setupTableSelection();
        setupBindings();
        setupEventHandlers();
        viewModel.initialize();
    }
    
    /**
     * POS 화면으로 되돌아가는 메서드
     */
    @FXML
    private void onBackToPos() {
        log.info("Returning to POS Main Screen");
        navigationService.openWindow(PosViewController.class);
    }

    private void setupTableColumns() {
        // Factory를 통한 테이블 컬럼 초기화 위임 (Edit/Delete 콜백 전달)
        tableFactory.initializeTable(tableView );
    }
    
    /**
     * 테이블 속성 설정 (RowFactory, Editable 등)
     */
    private void setupTableProperties() {
        tableView.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(Supplier supplier, boolean empty) {
                super.updateItem(supplier, empty);
                
                getStyleClass().removeAll("supplier-row", "supplier-active", "supplier-inactive");
                
                if (supplier == null || empty) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    getStyleClass().addAll("supplier-row", 
                        supplier.isActive() ? "supplier-active" : "supplier-inactive");
                    
                    Tooltip tooltip = new Tooltip(String.format(
                        "ID: %d%n" +
                        "Name: %s (%s)%n" +
                        "Company: %s%n" +
                        "Phone: %s%n" +
                        "Email: %s%n" +
                        "Address: %s%n" +
                        "Status: %s",
                        supplier.getId(), 
                        supplier.getName(), 
                        supplier.getAbbr(),
                        supplier.getCompany(), 
                        supplier.getPhone(), 
                        supplier.getEmail(),
                        supplier.getAddress(), 
                        supplier.isActive() ? "Active" : "Inactive"
                    ));
                    setTooltip(tooltip);
                }
            }
        });
        
        tableView.setEditable(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }
    
    private void setupTableSelection() {
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setSelectedSupplier(newVal);
            deleteButton.setDisable(newVal == null);
        });
        
        tableView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Supplier selected = tableView.getSelectionModel().getSelectedItem();
                //if (selected != null) showSupplierDetail(selected);
            }
        });
    }
    
    private void setupBindings() {
        searchField.textProperty().bindBidirectional(viewModel.searchKeywordProperty());
        activeOnlyCheckBox.selectedProperty().bindBidirectional(viewModel.showActiveOnlyProperty());
        progressIndicator.visibleProperty().bind(viewModel.loadingProperty());
        progressIndicator.managedProperty().bind(viewModel.loadingProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        
        countLabel.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> String.format("Total: %d / Filtered: %d", 
                    viewModel.getTotalCount(), viewModel.getFilteredCount()),
                viewModel.showActiveOnlyProperty(),
                viewModel.searchKeywordProperty()
            )
        );
    }
    
    private void setupEventHandlers() {
        refreshButton.setOnAction(e -> viewModel.reload());
        newButton.setOnAction(e -> showAddDialog());
        if (backButton != null) {
            backButton.setOnAction(e -> onBackToPos());
        }
        deleteButton.setOnAction(e -> {
            Supplier selected = viewModel.getSelectedSupplier();
            if (selected != null) showDeleteConfirm(selected);
        });
    }
    
    // ===== 다이얼로그 메서드 =====
    

    
    /**
     * 신규 거래처 입력 모달 (SupplierAddDialogController).
     * 저장에 성공하면 ViewModel 이 allSuppliers 에 추가하므로 테이블은 자동 갱신된다.
     */
    private void showAddDialog() {
        navigationService.openModalWindow(SupplierAddDialogController.class, "Add Supplier");
    }
    
    
    private void showDeleteConfirm(Supplier supplier) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Supplier");
        confirm.setHeaderText("Delete " + supplier.getFullName() + "?");
        confirm.setContentText("This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                viewModel.deleteSupplier(supplier);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Deleted");
                alert.setHeaderText(null);
                alert.setContentText("Supplier " + supplier.getFullName() + " has been deleted.");
                alert.showAndWait();
            }
        });
    }
}