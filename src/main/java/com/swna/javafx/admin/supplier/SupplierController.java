package com.swna.javafx.admin.supplier;

import com.swna.javafx.admin.supplier.domain.SupplierDomain;
import com.swna.javafx.admin.supplier.viewmodel.SupplierViewModel;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseButton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierController implements Initializable {
    
    private final SupplierViewModel viewModel;
    
    @FXML private TableView<SupplierDomain> supplierTable;
    @FXML private TableColumn<SupplierDomain, String> noColumn;
    @FXML private TableColumn<SupplierDomain, Long> idColumn;
    @FXML private TableColumn<SupplierDomain, String> abbrColumn;
    @FXML private TableColumn<SupplierDomain, String> nameColumn;
    @FXML private TableColumn<SupplierDomain, String> companyColumn;
    @FXML private TableColumn<SupplierDomain, String> phoneColumn;
    @FXML private TableColumn<SupplierDomain, String> emailColumn;
    @FXML private TableColumn<SupplierDomain, String> addressColumn;
    @FXML private TableColumn<SupplierDomain, Boolean> activeColumn;
    @FXML private TableColumn<SupplierDomain, Void> actionColumn;
    
    @FXML private TextField searchField;
    @FXML private CheckBox activeOnlyCheckBox;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label countLabel;
    
    @FXML private Button refreshButton;
    @FXML private Button addButton;
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
    
    @SuppressWarnings("unchecked")
    private void setupTableColumns() {
        // 1. 번호 컬럼
        TableColumnUtil.createNumberColumn(supplierTable, noColumn, 50);
        
        // 2. ID 컬럼
        TableColumnUtil.makeReadOnlyLongColumn(idColumn, SupplierDomain::idProperty, TableColumnUtil.CENTER);
        idColumn.setText("ID");
        idColumn.setPrefWidth(60);
        
        // 3. 약어 컬럼
        TableColumnUtil.makeStringColumn(
            abbrColumn,
            SupplierDomain::abbrProperty,
            null,
            false,
            TableColumnUtil.CENTER,
            null
        );
        abbrColumn.setText("Abbr");
        abbrColumn.setPrefWidth(80);
        
        // 4. 담당자명 컬럼
        TableColumnUtil.makeStringColumn(
            nameColumn,
            SupplierDomain::nameProperty,
            (supplier, newValue) -> supplier.setName(newValue),
            true,
            TableColumnUtil.LEFT,
            supplier -> viewModel.markAsDirty(supplier)
        );
        nameColumn.setText("Name");
        nameColumn.setPrefWidth(150);
        
        // 5. 회사명 컬럼
        TableColumnUtil.makeStringColumn(
            companyColumn,
            SupplierDomain::companyProperty,
            (supplier, newValue) -> supplier.setCompany(newValue),
            true,
            TableColumnUtil.LEFT,
            supplier -> viewModel.markAsDirty(supplier)
        );
        companyColumn.setText("Company");
        companyColumn.setPrefWidth(150);
        
        // 6. 전화번호 컬럼
        TableColumnUtil.makeStringColumn(
            phoneColumn,
            SupplierDomain::phoneProperty,
            (supplier, newValue) -> supplier.setPhone(newValue),
            true,
            TableColumnUtil.CENTER,
            supplier -> viewModel.markAsDirty(supplier)
        );
        phoneColumn.setText("Phone");
        phoneColumn.setPrefWidth(120);
        
        // 7. 이메일 컬럼
        TableColumnUtil.makeStringColumn(
            emailColumn,
            SupplierDomain::emailProperty,
            (supplier, newValue) -> supplier.setEmail(newValue),
            true,
            TableColumnUtil.LEFT,
            supplier -> viewModel.markAsDirty(supplier)
        );
        emailColumn.setText("Email");
        emailColumn.setPrefWidth(180);
        
        // 8. 주소 컬럼
        TableColumnUtil.makeStringColumn(
            addressColumn,
            SupplierDomain::addressProperty,
            (supplier, newValue) -> supplier.setAddress(newValue),
            true,
            TableColumnUtil.LEFT,
            supplier -> viewModel.markAsDirty(supplier)
        );
        addressColumn.setText("Address");
        addressColumn.setPrefWidth(200);
        
        // 9. 상태 컬럼
        TableColumnUtil.makeBooleanColumn(
            activeColumn,
            SupplierDomain::activeProperty,
            (supplier, newValue) -> {
                supplier.setActive(newValue);
                viewModel.markAsDirty(supplier);
            },
            true,
            viewModel::markAsDirty
        );
        activeColumn.setText("Status");
        activeColumn.setPrefWidth(70);
        
        // 10. 액션 버튼 컬럼 (Lambda로 변경)
        setupActionColumn();
        
        supplierTable.setItems(viewModel.getSuppliers());
        supplierTable.getSortOrder().add(idColumn);
    }
    
   /**
    * 액션 버튼 컬럼 설정 - 간결하고 경고 없는 버전
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
                  SupplierDomain s = getTableRow().getItem();
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
                  SupplierDomain s = getTableRow().getItem();
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
    * 테이블 속성 설정 - 플랫폼 독립적 줄 구분자 사용
    */
   private void setupTableProperties() {
      supplierTable.setRowFactory(tableView -> new TableRow<>() {
         @Override
         protected void updateItem(SupplierDomain supplier, boolean empty) {
               super.updateItem(supplier, empty);
               
               getStyleClass().removeAll("supplier-row", "supplier-active", "supplier-inactive");
               
               if (supplier == null || empty) {
                  setText(null);
                  setGraphic(null);
                  setTooltip(null);
               } else {
                  getStyleClass().addAll("supplier-row", 
                     supplier.isActive() ? "supplier-active" : "supplier-inactive");
                  
                  // %n 사용 - 플랫폼 독립적 줄 구분자
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
                SupplierDomain selected = supplierTable.getSelectionModel().getSelectedItem();
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
        editButton.setOnAction(e -> {
            SupplierDomain selected = viewModel.getSelectedSupplier();
            if (selected != null) showEditDialog(selected);
        });
        deleteButton.setOnAction(e -> {
            SupplierDomain selected = viewModel.getSelectedSupplier();
            if (selected != null) showDeleteConfirm(selected);
        });
    }
    
    // ===== 다이얼로그 메서드 =====
    
   /**
    * 거래처 상세 정보 표시 - %n 사용
    */
   private void showSupplierDetail(SupplierDomain supplier) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Supplier Details");
      alert.setHeaderText(supplier.getFullName());
      
      TextArea content = new TextArea();
      content.setEditable(false);
      content.setWrapText(true);
      
      // %n 사용 - 플랫폼 독립적 줄 구분자
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
         supplier.isActive() ? "Active" : "Inactive",
         supplier.getCreatedAt(),
         supplier.getUpdatedAt()
      ));
      
      alert.getDialogPane().setContent(content);
      alert.getDialogPane().setPrefWidth(500);
      alert.showAndWait();
   }
    
    private void showAddDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Add Supplier");
        alert.setHeaderText("Add New Supplier");
        alert.setContentText("Add supplier dialog - To be implemented");
        alert.showAndWait();
    }
    
    private void showEditDialog(SupplierDomain supplier) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Edit Supplier");
        alert.setHeaderText("Edit Supplier: " + supplier.getFullName());
        alert.setContentText("Edit supplier dialog - To be implemented");
        alert.showAndWait();
    }
    
    private void showDeleteConfirm(SupplierDomain supplier) {
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