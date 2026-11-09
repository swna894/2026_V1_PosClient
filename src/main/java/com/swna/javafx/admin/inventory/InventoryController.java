package com.swna.javafx.admin.inventory;

import com.swna.javafx.admin.inventory.factory.InventoryTableFactory;
import com.swna.javafx.admin.inventory.model.Inventory;
import com.swna.javafx.admin.inventory.viewmodel.InventoryViewModel;
import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.infrastructure.scanner.SafeBarcodeScanner;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxmlView;

import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/inventroy-view.fxml")
public class InventoryController {

    private final InventoryViewModel viewModel;
    private final InventoryTableFactory tableFactory;
    private final SafeBarcodeScanner barcodeScanner;

    @FXML private BorderPane borderPane;
    @FXML private ToolBar mainToolBar;

    @FXML private ComboBox<Supplier> comboBoxSupplier; // 👈 String -> Supplier
    @FXML private TextField textFieldSearch;
    @FXML private Label labelInformStock;

    @FXML private Button buttonSave;
    @FXML private Button buttonReload;
    @FXML private Button buttonDelete;
    //@FXML private Button buttonPrintBarcode;
    @FXML private Button buttonPrintStock;
    @FXML private Button buttonUploadExcel;
    @FXML private Button buttonDownExcelSampleFile;
    @FXML private Button buttonStock;

    @FXML private TableView<Inventory> tableView;

    @FXML public void initialize() {
        initTableView();
        initSupplierComboBox();
        bindProperties();
        bindButtonActions();

        // 🔥 SafeBarcodeScanner 연동 설정
        initSafeBarcodeScanner();
        // Load initial data (supplier list is fetched from the API here)
        viewModel.initializeData();
    }

    /**
     * SafeBarcodeScanner를 Scene에 등록하고 스캔된 바코드를 확실하게 단일 값으로 처리
     */
    private void initSafeBarcodeScanner() {
        // Scene이 로드된 이후에 등록하기 위해 borderPane 또는 scene 속성 이용
        borderPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                barcodeScanner.register(newScene);
            }
        });

        // 바코드가 완성되어 들어올 때 호출되는 Consumer 설정
        barcodeScanner.setBarcodeConsumer(scannedBarcode -> {
            Platform.runLater(() -> {
                if (scannedBarcode != null && !scannedBarcode.isBlank()) {
                    String trimmed = scannedBarcode.trim();
                    
                    //log.info("Scanned Barcode Received: {}", trimmed);
                    
                    // 1. 텍스트필드를 먼저 완전히 비우고 새 값 세팅
                    textFieldSearch.clear();
                    textFieldSearch.setText(trimmed);
                    
                    // 2. 뷰모델의 검색어 상태를 새 바코드 값으로 설정 (기존 내역 덮어쓰기)
                    viewModel.getSearchText().set(trimmed);
                    
                    // 3. 포커스 및 전체 선택 유지
                    textFieldSearch.requestFocus();
                    textFieldSearch.selectAll();
                    tableView.requestFocus();
                }
            });
        });
    }

    // ---------------- Setup: TableView ----------------

    private void initTableView() {
        tableFactory.initializeTable(tableView);

        // FilteredList → SortedList 순으로 감싼 뷰를 TableView에 연결.
        // SortedList의 comparator를 TableView의 comparatorProperty에 바인딩해야
        // 헤더 클릭 시 정렬이 실제로 동작함 (SortedList 단독으로는 정렬 트리거가 없음).
        viewModel.getSortedInventoryList().comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(viewModel.getSortedInventoryList());
    }

    // ---------------- Setup: Supplier ComboBox ----------------

    private void initSupplierComboBox() {
        comboBoxSupplier.setConverter(createSupplierConverter());
        comboBoxSupplier.setItems(viewModel.getSuppliers());
        viewModel.getSelectedSupplier().bind(comboBoxSupplier.valueProperty());

        // Suppliers are loaded asynchronously (API call), so select the default
        // item once the list actually has data.
        viewModel.getSuppliers().addListener((ListChangeListener<Supplier>) change -> {
            selectDefaultSupplier();
        });
    }


    /**
     * Selects the first supplier whose company name does not contain "@".
     * Falls back to the very first item if no such supplier exists.
     */
    private void selectDefaultSupplier() {
        if (comboBoxSupplier.getItems().isEmpty()) {
            return;
        }

        // 🔥 목록의 첫 번째 항목("All")을 기본 선택
        comboBoxSupplier.getSelectionModel().selectFirst();
    }

    /**
     * Converts a Supplier to/from its display text (company name) in the ComboBox.
     */
    private StringConverter<Supplier> createSupplierConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Supplier supplier) {
                if (supplier == null) {
                    return "";
                }
                // "All" 항목인 경우 company 또는 toString 결과 반환
                if ("ALL".equalsIgnoreCase(supplier.getAbbr())) {
                    return supplier.getCompany() != null ? supplier.getCompany() : "All";
                }
                return supplier.getCompany() != null ? supplier.getCompany() : "";
            }

            @Override
            public Supplier fromString(String company) {
                return findSupplierByCompany(company);
            }
        };
    }

    private Supplier findSupplierByCompany(String company) {
        return comboBoxSupplier.getItems().stream()
                .filter(s -> Objects.equals(s.getCompany(), company) || 
                             (s.getCompany() != null && s.getCompany().equals(company)))
                .findFirst()
                .orElse(null);
    }


    // ---------------- Setup: Property Bindings ----------------

    private void bindProperties() {
        // ❌ 기존 양방향 바인딩 제거 (누적 현상의 주원인)
        textFieldSearch.textProperty().bindBidirectional(viewModel.getSearchText()); 
     

        labelInformStock.textProperty().bind(viewModel.getInformStockMessage());
    }

    // ---------------- Setup: Button Actions ----------------

    private void bindButtonActions() {
        buttonSave.setOnAction(e -> viewModel.handleSave());
        buttonReload.setOnAction(e -> viewModel.loadInventoryList());
        buttonDelete.setOnAction(e -> viewModel.handleDelete());
        //buttonPrintBarcode.setOnAction(e -> viewModel.handlePrintBarcode());
        buttonUploadExcel.setOnAction(e -> viewModel.handleUploadExcel());
    }


       @FXML
    void actionButtonDelete(ActionEvent event) {

    }

    @FXML
    void actionButtonDonwExcelSample(ActionEvent event) {

    }

    @FXML
    void actionButtonPrintStock(ActionEvent event) {

    }

    @FXML
    void actionButtonReload(ActionEvent event) {

    }

    @FXML
    void actionButtonSave(ActionEvent event) {

    }

    @FXML
    void actionButtonUploadExcel(ActionEvent event) {

    }

    @FXML
    void actionComboBoxSupplier(ActionEvent event) {

    }

}