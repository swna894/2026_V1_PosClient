package com.swna.javafx.admin.inventory;

import com.swna.javafx.admin.inventory.factory.InventoryTableFactory;
import com.swna.javafx.admin.inventory.model.Inventory;
import com.swna.javafx.admin.inventory.viewmodel.InventoryViewModel;
import com.swna.javafx.admin.supplier.domain.Supplier;

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

        // Load initial data (supplier list is fetched from the API here)
        viewModel.initializeData();
    }

    // ---------------- Setup: TableView ----------------

    private void initTableView() {
        tableFactory.initializeTable(tableView);
        tableView.setItems(viewModel.getInventoryList());
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

        comboBoxSupplier.getItems().stream()
                .filter(s -> s.getCompany() != null && !s.getCompany().contains("@"))
                .findFirst()
                .ifPresentOrElse(
                        comboBoxSupplier.getSelectionModel()::select,
                        () -> comboBoxSupplier.getSelectionModel().selectFirst()
                );
    }

    /**
     * Converts a Supplier to/from its display text (company name) in the ComboBox.
     */
    private StringConverter<Supplier> createSupplierConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Supplier supplier) {
                return supplier != null ? supplier.getCompany() : "";
            }

            @Override
            public Supplier fromString(String company) {
                return findSupplierByCompany(company);
            }
        };
    }

    private Supplier findSupplierByCompany(String company) {
        return comboBoxSupplier.getItems().stream()
                .filter(s -> Objects.equals(s.getCompany(), company))
                .findFirst()
                .orElse(null);
    }


    // ---------------- Setup: Property Bindings ----------------

    private void bindProperties() {
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