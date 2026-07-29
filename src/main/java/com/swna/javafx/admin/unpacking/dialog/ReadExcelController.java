package com.swna.javafx.admin.unpacking.dialog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;


@Component
@FxmlView("/view/admin/read-excel-view.fxml")
public class ReadExcelController {

    //private final ReadExcelViewModel viewModel;

    @FXML private Button buttonUpload;
    @FXML private Button buttonReload;
    @FXML private Button buttonBackColor;
    @FXML private Button buttonPath;
    @FXML private CheckBox checkBoxSupplier;
    @FXML private ComboBox<Supplier> comboBoxSupplier;
    @FXML private ComboBox<String> comboBoxSheet;
    @FXML private Label labelInvoice;
    @FXML private DatePicker datePicker;
    @FXML private ListView<String> listViewLog;
    @FXML private TextField textFieldFilePath;

    @FXML
    private void initialize() {
        wireDataBindings();
        configureControls();
        wireEvents();
    }

    private void wireDataBindings() {
        // textFieldFilePath.textProperty().bindBidirectional(viewModel.filePathProperty());
        // datePicker.valueProperty().bindBidirectional(viewModel.dateProperty());
        // labelInvoice.textProperty().bind(viewModel.invoiceLabelProperty());
        // checkBoxSupplier.selectedProperty().bindBidirectional(viewModel.checkSupplierProperty());
        
        // comboBoxSupplier.valueProperty().bindBidirectional(viewModel.selectedSupplierProperty());
        // comboBoxSheet.valueProperty().bindBidirectional(viewModel.selectedSheetProperty());

        // comboBoxSheet.setItems(viewModel.getSheets());
        // listViewLog.setItems(viewModel.getLogs());
    }

    private void configureControls() {

        // Configure Supplier ComboBox Display Cell
        //comboBoxSupplier.setItems(unPackingController.getSuppliers());
        ListCell<Supplier> cellFactory = new ListCell<>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getCompany());
            }
        };
        comboBoxSupplier.setButtonCell(cellFactory);
        comboBoxSupplier.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getCompany());
            }
        });
    }

    private void wireEvents() {
        buttonPath.setOnAction(e -> {
            // File Chooser Action
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            //fileChooser.setInitialDirectory(new java.io.File(viewModel.getInitialDirectory()));
           //java.io.File file = fileChooser.showOpenDialog(stage);
            // if (file != null) {
            //     viewModel.selectExcelFile(file.getAbsolutePath());
            // }
        });

        // buttonUpload.setOnAction(e -> viewModel.uploadExcel(
        //     () -> unPackingController.buttonReload.fire(),
        //     errorMsg -> alertDialog.dialogString(errorMsg, stage)
        // ));

        buttonBackColor.setOnAction(e -> {
            // viewModel.darkThemeProperty().set(!viewModel.darkThemeProperty().get());
            // applyTheme();
        });
    }

    private void applyTheme() {
       // boolean isDark = viewModel.darkThemeProperty().get();
       // String darkCss = getClass().getResource(DARK_STYLESHEET).toExternalForm();

        // if (isDark && !scene.getStylesheets().contains(darkCss)) {
        //     scene.getStylesheets().add(darkCss);
        // } else if (!isDark) {
        //     scene.getStylesheets().remove(darkCss);
        // }

        // buttonBackColor.setGraphic(ButtonUtil.getImage(isDark ? WHITE_24 : BLACK_24));
        //datePicker.setStyle(isDark ? datePickerBlack() : datePickerNormal());
    }

    private String datePickerNormal() {
        return "-fx-prompt-text-fill: derive(-fx-control-inner-background,-30%);" +
               "-fx-background-color: #707070, linear-gradient(#fcfcfc, #f3f3f3), linear-gradient(#f2f2f2 0.0%, #ebebeb 49.0%, #dddddd 50.0%, #cfcfcf 100.0%);" +
               "-fx-background-insets: 0.0,1.0,2.0; -fx-background-radius: 3.0,2.0,1.0; -fx-padding: 0.0 3.0 0.0 3.0; -fx-text-fill: black; -fx-font: 12px Arial; -fx-pref-width: 140.0px;";
    }

    private String datePickerBlack() {
        return "-fx-background-color: #414a4c; -fx-pref-width: 140.0px;";
    }
}
