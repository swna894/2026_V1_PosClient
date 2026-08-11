package com.swna.javafx.admin.unpacking.dialog;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.viewmodel.SupplierViewModel;
import com.swna.javafx.admin.unpacking.UnpackingViewModel;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/view/admin/read-excel-view.fxml")
@RequiredArgsConstructor
public class ReadExcelController {

    private final ReadExcelViewModel viewModel;
    private final UnpackingViewModel unpackingViewModel;
    private final SupplierViewModel supplierViewModel;

    @FXML private Button buttonReadExcel;
    @FXML private Button buttonReload;
    @FXML private Button buttonPath;
    @FXML private CheckBox checkBoxSupplier;
    @FXML private ComboBox<Supplier> comboBoxSupplier;
    @FXML private ComboBox<String> comboBoxSheet;
    @FXML private DatePicker datePicker;
    @FXML private Label labelInvoice;
    @FXML private ListView<String> listViewLog;
    @FXML private TextField textFieldFilePath;

    @FXML
    private void initialize() {
        // ViewModel 초기화 실행 (기본 경로 설정 등)
        viewModel.initialize();
        configureControls();
        wireDataBindings();
    }

    private void configureControls() {
        // 1. Set default DatePicker value
        datePicker.setValue(LocalDate.now(ZoneId.systemDefault()));

        // 2. Load supplier data
        supplierViewModel.load();
        comboBoxSupplier.setItems(supplierViewModel.getAllSuppliers());

        // 3. Configure Supplier ComboBox converter
        StringConverter<Supplier> supplierConverter = new StringConverter<>() {
            @Override
            public String toString(Supplier supplier) {
                return (supplier != null && supplier.getCompany() != null) ? supplier.getCompany() : "";
            }

            @Override
            public Supplier fromString(String string) {
                if (string == null || string.isBlank()) return null;
                return comboBoxSupplier.getItems().stream()
                        .filter(s -> string.equals(s.getCompany()))
                        .findFirst()
                        .orElse(null);
            }
        };

        comboBoxSupplier.setConverter(supplierConverter);
        
        // Configure CellFactory for dropdown list display
        comboBoxSupplier.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : supplierConverter.toString(item));
            }
        });
    }

    private void wireDataBindings() {
        // ViewModel - UI Property Bindings
        textFieldFilePath.textProperty().bindBidirectional(viewModel.filePathProperty());
        datePicker.valueProperty().bindBidirectional(viewModel.dateProperty());
        labelInvoice.textProperty().bind(viewModel.invoiceLabelProperty());
        checkBoxSupplier.selectedProperty().bindBidirectional(viewModel.checkSupplierProperty());
        
        comboBoxSupplier.valueProperty().bindBidirectional(viewModel.selectedSupplierProperty());
        comboBoxSheet.valueProperty().bindBidirectional(viewModel.selectedSheetProperty());

        // ObservableList Bindings
        comboBoxSheet.setItems(viewModel.getSheets());
        listViewLog.setItems(viewModel.getLogs());

        // 💡 로그 목록이 변경될 때 맨 마지막 행으로 자동 스크롤
        viewModel.getLogs().addListener((javafx.collections.ListChangeListener<String>) change -> {
            int size = viewModel.getLogs().size();
            if (size > 0) {
                listViewLog.scrollTo(size - 1);
            }
        });
    }

    // =========================================================================
    // FXML Action Handlers
    // =========================================================================

    /**
     * Action for [Select excel] button: onAction="#handleSelectFilePath"
     */
    @FXML
    private void handleSelectFilePath() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files (*.xlsx, *.xls)", "*.xlsx", "*.xls")
        );
        
        String initialDir = viewModel.getInitialDirectory();
        if (initialDir != null && new File(initialDir).exists()) {
            fileChooser.setInitialDirectory(new File(initialDir));
        }

        Stage stage = (Stage) buttonPath.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            viewModel.selectExcelFile(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Action for [_Read] button: onAction="#handleReadExcel"
     */
    @FXML
    private void handleReadExcel() {
        String path = viewModel.filePathProperty().get();
        if (path == null || path.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select an Excel file first.");
            return;
        }

        buttonReadExcel.setDisable(true); // Prevent duplicate clicks

        viewModel.readExcel(
            () -> { // On success
                buttonReadExcel.setDisable(false);
                unpackingViewModel.reload();
                /* showAlert(Alert.AlertType.INFORMATION, "Success", "Excel file processed successfully."); */
            },
            errorMsg -> { // On failure / error
                buttonReadExcel.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Error", errorMsg);
            }
        );
    }

    /**
     * Action for [Reload] button: onAction="#handleReload"
     */
    @FXML
    private void handleReload() {
        supplierViewModel.load();
        viewModel.loadSheetNames();
        viewModel.addLog(">> Reloaded Supplier list and Sheet list.");
    }

    /**
     * Helper method to display alert dialogs
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}