package com.swna.javafx.admin.supplier;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.viewmodel.SupplierViewModel;
import com.swna.javafx.common.navigation.NavigationService;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.pos.PosViewController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
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
    
    @FXML private TableView<Supplier> supplierTable;
    @FXML private TableColumn<Supplier, String> noColumn;
    @FXML private TableColumn<Supplier, Long> idColumn;
    @FXML private TableColumn<Supplier, String> abbrColumn;
    @FXML private TableColumn<Supplier, String> nameColumn;
    @FXML private TableColumn<Supplier, String> companyColumn;
    @FXML private TableColumn<Supplier, String> phoneColumn;
    @FXML private TableColumn<Supplier, String> emailColumn;
    @FXML private TableColumn<Supplier, String> addressColumn;
    @FXML private TableColumn<Supplier, Boolean> activeColumn;
    @FXML private TableColumn<Supplier, Void> actionColumn;
    
    @FXML private TextField searchField;
    @FXML private CheckBox activeOnlyCheckBox;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label countLabel;
    
    @FXML private Button refreshButton;
    @FXML private Button addButton;
    @FXML private Button backButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    
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
        // 4. NavigationService를 사용하여 POS 스테이지로 이동 
        navigationService.navigateStage(PosViewController.class)
        ;
    }

    private void setupTableColumns() {
        // 1. 번호 컬럼
        TableColumnUtil.createNumberColumn(supplierTable, noColumn, 50);
    
        // 3. 약어 컬럼
        TableColumnUtil.makeStringColumn( abbrColumn, Supplier::abbrProperty, null, false,true,TableColumnUtil.CENTER, null );
        
        // 4. 담당자명 컬럼
        TableColumnUtil.makeStringColumn( nameColumn, Supplier::nameProperty, Supplier::setName, true, true,TableColumnUtil.LEFT, viewModel::markAsDirty );
        
        // 5. 회사명 컬럼
        TableColumnUtil.makeStringColumn( companyColumn, Supplier::companyProperty, Supplier::setCompany, true, true,TableColumnUtil.LEFT, viewModel::markAsDirty );
        
        // 6. 전화번호 컬럼
        TableColumnUtil.makeStringColumn( phoneColumn, Supplier::phoneProperty, Supplier::setPhone, true, true,TableColumnUtil.CENTER, viewModel::markAsDirty );
        
        // 7. 이메일 컬럼
        TableColumnUtil.makeStringColumn( emailColumn, Supplier::emailProperty, Supplier::setEmail, true, true,TableColumnUtil.LEFT, viewModel::markAsDirty );
        
        // 8. 주소 컬럼
        TableColumnUtil.makeStringColumn( addressColumn, Supplier::addressProperty, Supplier::setAddress, true, true,TableColumnUtil.LEFT, viewModel::markAsDirty );

        
        // 9. 상태 컬럼
        TableColumnUtil.makeBooleanColumn(
            activeColumn,
            Supplier::activeProperty,
            (supplier, newValue) -> {
                supplier.setActive(newValue);
                viewModel.markAsDirty(supplier);
            },
            true,
            viewModel::markAsDirty
        );
        
        // 10. 액션 버튼 컬럼
        setupActionColumn();
        
        supplierTable.setItems(viewModel.getSuppliers());
        supplierTable.getSortOrder().add(idColumn);
    }
    
    /**
     * 액션 버튼 컬럼 설정
     */
    private void setupActionColumn() {
        actionColumn.setText("Actions");
        actionColumn.setPrefWidth(100);
        actionColumn.setSortable(false);
        actionColumn.setStyle("-fx-alignment: CENTER;");
        
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editButton = createEditButton();
            private final Button deleteButton = createDeleteButton();
            private final HBox container = new HBox(5, editButton, deleteButton);
            
            {
                container.setAlignment(Pos.CENTER);
            }
            
            private Button createEditButton() {
                Button btn = new Button("Edit");
                String baseStyle = "-fx-font-size: 11px; -fx-padding: 2px 8px; -fx-cursor: hand;";
                btn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;" + baseStyle);
                btn.setOnAction(e -> {
                    Supplier s = getTableRow().getItem();
                    if (s != null) showEditDialog(s);
                });
                btn.setOnMouseEntered(e -> 
                    btn.setStyle("-fx-background-color: #45a049; -fx-text-fill: white;" + baseStyle));
                btn.setOnMouseExited(e -> 
                    btn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;" + baseStyle));
                return btn;
            }
            
            private Button createDeleteButton() {
                Button btn = new Button("Del");
                String baseStyle = "-fx-font-size: 11px; -fx-padding: 2px 8px; -fx-cursor: hand;";
                btn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;" + baseStyle);
                btn.setOnAction(e -> {
                    Supplier s = getTableRow().getItem();
                    if (s != null) showDeleteConfirm(s);
                });
                btn.setOnMouseEntered(e -> 
                    btn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white;" + baseStyle));
                btn.setOnMouseExited(e -> 
                    btn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;" + baseStyle));
                return btn;
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getTableRow() == null || getTableRow().getItem() == null) ? null : container);
            }
        });
    }
    
    /**
     * 테이블 속성 설정
     */
    private void setupTableProperties() {
        supplierTable.setRowFactory(tableView -> new TableRow<>() {
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
        
        supplierTable.setEditable(true);
        supplierTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }
    
    private void setupTableSelection() {
        supplierTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setSelectedSupplier(newVal);
            editButton.setDisable(newVal == null);
            deleteButton.setDisable(newVal == null);
        });
        
        supplierTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Supplier selected = supplierTable.getSelectionModel().getSelectedItem();
                if (selected != null) showSupplierDetail(selected);
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
        addButton.setOnAction(e -> showAddDialog());
        if (backButton != null) {
            backButton.setOnAction(e -> onBackToPos());
        }
        editButton.setOnAction(e -> {
            Supplier selected = viewModel.getSelectedSupplier();
            if (selected != null) showEditDialog(selected);
        });
        deleteButton.setOnAction(e -> {
            Supplier selected = viewModel.getSelectedSupplier();
            if (selected != null) showDeleteConfirm(selected);
        });
    }
    
    // ===== 다이얼로그 메서드 =====
    
    private void showSupplierDetail(Supplier supplier) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Supplier Details");
        alert.setHeaderText(supplier.getFullName());
        
        TextArea content = new TextArea();
        content.setEditable(false);
        content.setWrapText(true);
        
        content.setText(String.format(
            "ID: %d%n" +
            "Abbreviation: %s%n" +
            "Name: %s%n" +
            "Company: %s%n" +
            "Phone: %s%n" +
            "Cellphone: %s%n" +
            "Email: %s%n" +
            "Address: %s%n" +
            "Status: %s%n" +
            "Created: %s%n" +
            "Updated: %s",
            supplier.getId(),
            supplier.getAbbr(),
            supplier.getName(),
            supplier.getCompany(),
            supplier.getPhone(),
            supplier.getCellphone(),
            supplier.getEmail(),
            supplier.getAddress(),
            supplier.isActive() ? "Active" : "Inactive"
        ));
        
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }
    
    private void showAddDialog() {
        // TODO: 구현 예정 - 입력 폼 다이얼로그
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Add Supplier");
        alert.setHeaderText("Add New Supplier");
        alert.setContentText("Add supplier dialog - To be implemented");
        alert.showAndWait();
    }
    
    private void showEditDialog(Supplier supplier) {
        // TODO: 구현 예정 - 입력 폼 다이얼로그
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Edit Supplier");
        alert.setHeaderText("Edit Supplier: " + supplier.getFullName());
        alert.setContentText("Edit supplier dialog - To be implemented");
        alert.showAndWait();
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