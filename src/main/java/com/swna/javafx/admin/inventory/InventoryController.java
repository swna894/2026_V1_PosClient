package com.swna.javafx.admin.inventory;

import org.springframework.stereotype.Component;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/InventoryView.fxml")
public class InventoryController {

    @FXML private BorderPane borderPane;
    @FXML private ToolBar mainToolBar;

    @FXML private ComboBox<String> comboBoxSupplier;
    @FXML private ComboBox<String> comboBoxCategory;
    @FXML private TextField textFieldSearch;
    @FXML private Label labelInformStock;

    @FXML private Button buttonSave;
    @FXML private Button buttonReload;
    @FXML private Button buttonDelete;
    @FXML private Button buttonPrintBarcode;
    @FXML private Button buttonPrintStock;
    @FXML private Button buttonUploadExcel;
    @FXML private Button buttonDownExcelSampleFile;
    @FXML private Button buttonStock;

    @FXML private TableView<?> tableView;

    @FXML void actionComboBoxSupplier(ActionEvent event) {}
    @FXML void actionComboBoxCategory(ActionEvent event) {}

    @FXML void actionButtonSave(ActionEvent event) {}
    @FXML void actionButtonReload(ActionEvent event) {}
    @FXML void actionButtonDelete(ActionEvent event) {}
    @FXML void actionButtonPrintBarcode(ActionEvent event) {}
    @FXML void actionButtonPrintStock(ActionEvent event) {}
    @FXML void actionButtonUploadExcel(ActionEvent event) {}
    @FXML void actionButtonDonwExcelSample(ActionEvent event) {}
    @FXML void actionButtonStock(ActionEvent event) {}

}