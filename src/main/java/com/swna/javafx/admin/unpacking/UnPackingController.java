package com.swna.javafx.admin.unpacking;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.unpacking.model.Inspection;
import com.swna.javafx.admin.unpacking.model.InspectionItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;

/**
 * Pure View, now backed by Unpacking.fxml instead of hand-built Nodes.
 * Replaced top HBox containers with ToolBar controls for better layout management.
 */
@Component
@FxmlView("/view/admin/unpacking-view.fxml")
public class UnPackingController {

	//private final UnpackingViewModel viewModel;
	//@Autowired private ReadExcelController readExcelController;
	//@Autowired private ColumnUtil columnUtil;
	//@Autowired private FxWeaver fxWeaver;

	private static final String DARK_STYLESHEET = "/styles/black_mode.css";

	@FXML private BorderPane borderPane;
	@FXML private ToolBar mainToolBar; // Replaced HBox with ToolBar
	@FXML private BorderPane leftPane;
	@FXML private BorderPane rightPane;
	@FXML private SplitPane splitPane;

	@FXML public Button buttonReload;
	@FXML private Button buttonBackColor;
	@FXML private Button buttonExcelRead;
	@FXML private Button buttonAddStock;
	@FXML private Button buttonEPrice;
	@FXML private Button buttonDelete;

	@FXML private TextField textFieldSearch;
	@FXML private TextField textFieldPriceMultiplier;
	@FXML private DatePicker datePickerStart;
	@FXML private DatePicker datePickerEnd;
	@FXML private Label labelInspectionSummary;
	@FXML private Label labelProductSummary;
	@FXML private ComboBox<Supplier> comboBoxSupplier;
	@FXML private ComboBox<String> comboBoxConfirmFilter;
	@FXML private TableView<Inspection> tableViewInspections;
	@FXML private TableView<InspectionItem> tableViewItems;

	// ---------------- public API kept identical for existing callers ----------------

	// public Stage open() {
	// }

	public ObservableList<Supplier> getSuppliers() {
		//return viewModel.getSuppliers();
		return FXCollections.observableArrayList();
	}

	public void updateTableViewInspection(Inspection inspection) {
		//viewModel.addInspection(inspection);
	}

	public void updateTableViewInspection(List<Inspection> inspections) {
		//viewModel.reload();
	}

	// ---------------- FXML callback ----------------

	@FXML
	private void initialize() {
		wireButtons();
		wireControls();
		configureInspectionsTable();
		configureItemsTable();
	}

	private void wireButtons() {

		// buttonReload.setOnAction(e -> viewModel.reload());
		// //buttonExcelRead.setOnAction(e -> readExcelController.open());
		// buttonDelete.setOnAction(e -> viewModel.deleteSelectedInspections());
		// buttonAddStock.setOnAction(e -> viewModel.addStockForConfirmedItems(tableViewItems.getItems()));
		// buttonEPrice.setOnAction(e -> viewModel.applyEstimatedPriceMultiplier(textFieldPriceMultiplier.getText(), tableViewItems.getItems()));
		// buttonBackColor.setOnAction(e -> {
		// 	viewModel.darkThemeProperty().set(!viewModel.darkThemeProperty().get());
		// });
	}

	private void wireControls() {
		// datePickerStart.setValue(viewModel.startDateProperty().get());
		// datePickerEnd.setValue(viewModel.endDateProperty().get());
		// datePickerStart.valueProperty().bindBidirectional(viewModel.startDateProperty());
		// datePickerEnd.valueProperty().bindBidirectional(viewModel.endDateProperty());
		// datePickerStart.setOnAction(e -> viewModel.reload());
		// datePickerEnd.setOnAction(e -> viewModel.reload());

		// labelInspectionSummary.textProperty().bind(viewModel.inspectionSummaryProperty());
		// labelProductSummary.textProperty().bind(viewModel.productSummaryProperty());

		// comboBoxSupplier.setItems(viewModel.getSuppliers());
		// comboBoxSupplier.setButtonCell(supplierListCell());
		// comboBoxSupplier.setCellFactory(lv -> supplierListCell());
		// comboBoxSupplier.getSelectionModel().selectedItemProperty()
		// 		.addListener((obs, oldValue, newValue) -> viewModel.filterBySupplier(newValue));

		// comboBoxConfirmFilter.setItems(viewModel.getConfirmFilterOptions());
		// comboBoxConfirmFilter.getSelectionModel().selectFirst();
		// comboBoxConfirmFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
		// 	List<InspectionItem> filtered = viewModel.filterByConfirmStatus(newValue);
		// 	tableViewItems.setItems(FXCollections.observableArrayList(filtered));
		// });
	}

	private void onBarcodeScanned(String scanned) {
		// textFieldSearch.setText(scanned);
		// InspectionItem match = viewModel.findByBarcodeOrCode(scanned);
		// if (match != null) {
		// 	tableViewItems.getSelectionModel().selectFirst();
		// }
	}

	private ListCell<Supplier> supplierListCell() {
		return new ListCell<>() {
			@Override
			protected void updateItem(Supplier item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : item.getCompany());
			}
		};
	}

	// ---------------- Tables Config ----------------

	private void configureInspectionsTable() {
		tableViewInspections.setTableMenuButtonVisible(true);
		// tableViewInspections.setItems(viewModel.getInspections());
		// tableViewInspections.getSelectionModel().selectedItemProperty()
		// 		.addListener((obs, oldValue, newValue) -> viewModel.selectInspection(newValue));

		// columnUtil.columnNumner(tableViewInspections, 50);
		// columnUtil.columnCheckBoxHeader(tableViewInspections, Inspection::selectedProperty, headerCheckBox());
		// columnUtil.column(tableViewInspections, INVOICE, Inspection::invoiceProperty, getDefault(), false, true, LEFT, 250);
		// columnUtil.column(tableViewInspections, AMOUNT, Inspection::amountProperty, getCurrency(), false, true, RIGHT, 90);
		// columnUtil.column(tableViewInspections, COMMENT, Inspection::commentProperty, getDefault(), false, true, CENTER, 200);
	}

	private CheckBox headerCheckBox() {
		CheckBox checkBox = new CheckBox();
		checkBox.setOnAction(e -> tableViewInspections.getItems().forEach(item -> item.setSelected(checkBox.isSelected())));
		return checkBox;
	}

	private void configureItemsTable() {
		// FilteredList<InspectionItem> filtered = new FilteredList<>(viewModel.getInspectItems(), item -> true);
		// textFieldSearch.textProperty().addListener((obs, oldValue, newValue) ->
		// 		filtered.setPredicate(item -> !textFieldSearch.isEditable() || viewModel.matchesSearch(item, newValue)));
		// SortedList<InspectionItem> sorted = new SortedList<>(filtered);
		// sorted.comparatorProperty().bind(tableViewItems.comparatorProperty());
		// tableViewItems.setItems(sorted);

		// columnUtil.columnNumner(tableViewItems, 50);
		// columnUtil.columnCheckBox(tableViewItems, "CHECK", InspectionItem::confirmProperty, 80);
		// columnUtil.column(tableViewItems, BARCODE, InspectionItem::barcodeProperty, getDefault(), true, true, CENTER, 150);
		// columnUtil.column(tableViewItems, DESCRIPTION, InspectionItem::descriptionProperty, getDefault(), true, true, LEFT, 200);
		// TableColumn<InspectionItem, String> columnCategory =
		// 		columnUtil.column(tableViewItems, CATEGORY, InspectionItem::categoryProperty, viewModel.getCategories(), LEFT, 150);
		// columnUtil.column(tableViewItems, OLD_PRICE_IN, InspectionItem::oldPriceinProperty, getCurrency(), true, true, RIGHT, 90);
		// columnUtil.column(tableViewItems, PRICE_IN, InspectionItem::priceinProperty, getDecial(), true, true, RIGHT, 90);
		// columnUtil.column(tableViewItems, QTY, InspectionItem::qtyProperty, getNumber(), true, true, RIGHT, 90);
		// columnUtil.column(tableViewItems, "MIN STOCK", InspectionItem::minStockProperty, getNumber(), true, true, RIGHT, 90);
		// columnUtil.column(tableViewItems, EP, InspectionItem::priceoutEstimatedProperty, getDecial(), true, true, RIGHT, 90);
		// columnUtil.column(tableViewItems, RP, InspectionItem::priceoutProperty, getDecial(), true, true, RIGHT, 90);
		// columnUtil.columnCheckBox(tableViewItems, "ADDED", InspectionItem::isSavedProperty, 80);
		// columnUtil.column(tableViewItems, CODE, InspectionItem::codeProperty, getDefault(), true, true, LEFT, 150);
		// TableColumn<InspectionItem, String> columnSupplier =
		// 		columnUtil.column(tableViewItems, SUPPLIER, InspectionItem::supplierProperty, viewModel.getDisplaySupplierNames(), LEFT, 250);
		// columnUtil.column(tableViewItems, COMMENT, InspectionItem::commentProperty, getDefault(), true, true, LEFT, 150);

		// columnSupplier.setOnEditCommit(event -> {
		// 	InspectionItem item = event.getRowValue();
		// 	viewModel.updateSupplierOnItem(item, event.getNewValue(), event.getOldValue());
		// });
		// columnCategory.setOnEditCommit(event -> event.getRowValue().setCategory(event.getNewValue()));
	}
}