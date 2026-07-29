package com.swna.javafx.admin.unpacking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.unpacking.model.Inspection;
import com.swna.javafx.admin.unpacking.model.InspectionItem;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

@Component
public class UnpackingViewModel {

	// @Autowired private InspectionService inspectionService;
	// @Autowired private SupplierService supplierService;
	// @Autowired private PromotionController promotionController;

	// ---------------- State Properties ----------------
	private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>(LocalDate.now().withDayOfMonth(1));
	private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>(LocalDate.now());
	private final StringProperty inspectionSummary = new SimpleStringProperty("  $00.00 | 0 ITEMS");
	private final StringProperty productSummary = new SimpleStringProperty("  $00.00 | 0 ITEMS");
	private final BooleanProperty darkTheme = new SimpleBooleanProperty(false);

	private String currentFilterStatus = "ALL";

	// ---------------- Collections ----------------
	private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
	private final ObservableList<String> categories = FXCollections.observableArrayList();
	private final ObservableList<String> displaySupplierNames = FXCollections.observableArrayList();
	private final ObservableList<String> confirmFilterOptions = FXCollections.observableArrayList("ALL", "Checked", "Unchecked", "Added", "Unadded");
	
	private final ObservableList<Inspection> inspections = FXCollections.observableArrayList();
	private final ObservableList<InspectionItem> inspectItems = FXCollections.observableArrayList(item -> new Observable[] {
		item.qtyProperty(), item.commentProperty(), item.confirmProperty(), item.priceoutProperty(),
		// item.barcodeProperty(), item.priceoutProperty(), item.minOrderQtyProperty(), item.supplierProperty()
	});

	// ---------------- Initialization ----------------
	public void initialize() {
		initArrayListListener();
		loadSuppliers();
		loadCategories();
		reload();
	}

	public void reload() {
		HashMap<String, Object> param = new HashMap<>();
		param.put("start", startDate.get().toString());
		param.put("end", endDate.get().toString());

		// List<Inspection> list = inspectionService.gets(param);
		// inspections.clear();
		// inspectItems.clear();

		// if (list != null && !list.isEmpty()) {
		// 	inspections.addAll(list);
		// 	inspectItems.addAll(list.get(0).getItems());
		// }
		calculateResults();
	}

	private void initArrayListListener() {
		// inspectItems.addListener((ListChangeListener<InspectionItem>) change -> {
		// 	while (change.next()) {
		// 		if (change.wasUpdated()) {
		// 			InspectionItem item = inspectItems.get(change.getFrom());
		// 			item.setSelected(true);
		// 			if (item.getIsSaved()) {
		// 				item.setConfirm(true);
		// 			}
		// 			item.setAmount(Double.valueOf(String.format("%.2f", item.getQty() * item.getPricein())));
		// 			calculateResults();
		// 			// inspectionService.put(item);
		// 			updateAbbr(item);
		// 		}
		// 	}
		// });
	}

	// ---------------- Business Logic & Actions ----------------

	public void selectInspection(Inspection selected) {
		inspectItems.clear();
		//if (selected != null) {
		// 	inspections.forEach(item -> item.setSelected(false));
		// 	selected.setSelected(true);
		// 	inspectItems.addAll(selected.getItems());
		// }
		calculateResults();
	}

	public void addInspection(Inspection inspection) {
		if (inspection != null) {
			inspections.forEach(item -> item.setSelected(false));
			inspection.setSelected(true);
			inspections.add(0, inspection);
			inspectItems.clear();
			calculateResults();
		}
	}

	public void deleteSelectedInspections() {
		// List<Inspection> deleteList = inspections.stream().filter(Inspection::getSelected).toList();
		// if (!deleteList.isEmpty()) {
		// 	inspectionService.deleteAll(deleteList);
		// 	reload();
		// }
	}

	public List<InspectionItem> addStockForConfirmedItems(List<InspectionItem> items) {
		if (items == null) return null;

		for (int i = 0; i < items.size(); i++) {
			// items.get(i).setLineNo(i + 1);
		}

		// List<InspectionItem> targetItems = items.stream()
		// 		.filter(item -> item.getConfirm() && !item.getIsSaved() && (item.getPriceoutEstimated() != 0.0 || item.getPriceout() != 0.0))
		// 		.toList();

		// if (targetItems.isEmpty()) {
		// 	return null;
		// }

		// if (hasDuplicatesBarcode(targetItems)) {
		// 	throw new IllegalArgumentException("DUPLICATE_BARCODE");
		// }

		// targetItems.forEach(item -> {
		// 	// if (item.getPriceout() == 0) {
		// 	// 	item.setPriceout(Double.valueOf(String.format("%.2f", item.getPriceoutEstimated())));
		// 	// }
		// });

		// List<InspectionItem> result = inspectionService.puts(targetItems);
		// if (result != null) {
		// 	reload();
		// }
		//return result;
		return null;
	}

	public boolean hasDuplicatesBarcode(List<InspectionItem> productList) {
		for (int i = 0; i < productList.size(); i++) {
			for (int j = i + 1; j < productList.size(); j++) {
				if (productList.get(i).getBarcode().trim().equals(productList.get(j).getBarcode().trim())) {
					return true;
				}
			}
		}
		return false;
	}

	public void applyEstimatedPriceMultiplier(String multiplierStr, List<InspectionItem> items) {
		// if (items == null || multiplierStr == null || !NumberUtil.isStringDouble(multiplierStr)) return;
		// double multiplier = Double.parseDouble(multiplierStr);
		// items.forEach(item -> item.setPriceoutEstimated(item.getPricein() * multiplier));
	}

	public void filterBySupplier(Supplier supplier) {
		if (supplier == null) return;
		String abbr = supplier.getAbbr();
		// List<Inspection> filtered = inspections.stream()
		// 		.filter(item -> abbr.equals(item.getSupplierAbbr()))
		// 		.toList();

		// if (!filtered.isEmpty()) {
		// 	inspections.setAll(filtered);
		// 	inspectItems.clear();
		// 	inspectItems.addAll(filtered.get(0).getItems());
		// 	calculateResults();
		// }
	}

	public List<InspectionItem> filterByConfirmStatus(String status) {
		this.currentFilterStatus = status;
		List<InspectionItem> filtered = switch (status) {
			case "Checked" -> inspectItems.stream().filter(InspectionItem::getConfirm).toList();
			case "Unchecked" -> inspectItems.stream().filter(item -> !item.getConfirm()).toList();
			case "Added" -> inspectItems.stream().filter(InspectionItem::getIsSaved).toList();
			case "Unadded" -> inspectItems.stream().filter(item -> !item.getIsSaved()).toList();
			default -> inspectItems;
		};
		calculateResults();
		return filtered;
	}

	public boolean matchesSearch(InspectionItem item, String searchText) {
		if (searchText == null || searchText.isBlank()) return true;
		String lower = searchText.toLowerCase();

		return (item.getCode() != null && item.getCode().toLowerCase().contains(lower)) ||
				(item.getBarcode() != null && item.getBarcode().toLowerCase().contains(lower)) ||
				(item.getDescription() != null && item.getDescription().toLowerCase().contains(lower)) ||
				(item.getCategory() != null && item.getCategory().toLowerCase().contains(lower)) ||
				(item.getSupplier() != null && item.getSupplier().toLowerCase().contains(lower));
	}

	public InspectionItem findByBarcodeOrCode(String scanned) {
		if (scanned == null || scanned.isBlank()) return null;
		InspectionItem match = inspectItems.stream()
				.filter(item -> scanned.equalsIgnoreCase(item.getBarcode()))
				.findAny()
				.orElseGet(() -> inspectItems.stream()
						.filter(item -> scanned.equalsIgnoreCase(item.getCode()))
						.findAny()
						.orElse(null));

		if (match != null) {
			match.setSelected(true);
			match.setConfirm(true);
			inspectItems.remove(match);
			inspectItems.add(0, match);
		}
		return match;
	}

	public void updateSupplierOnItem(InspectionItem item, String newSupplierName, String oldSupplierName) {
		if (item == null) return;
		if (!item.getIsSaved()) {
			item.setSupplier(newSupplierName);
			updateAbbr(item);
		} else {
			item.setSupplier(oldSupplierName);
		}
	}

	public void updateAbbr(InspectionItem item) {
		if (item == null || item.getSupplier() == null) return;
		// suppliers.stream()
		// 		.filter(s -> s.getCompany().equals(item.getSupplier()))
		// 		.map(Supplier::getAbbr)
		// 		.findFirst()
		// 		.ifPresent(item::setAbbr);
	}

	public void calculateResults() {
		double inspAmount = inspections.stream().mapToDouble(Inspection::getAmount).sum();
		inspectionSummary.set(String.format("  $%.2f | %d ITEMS", inspAmount, inspections.size()));

		long checkCount = inspectItems.stream().filter(InspectionItem::getConfirm).count();
		long uncheckCount = inspectItems.stream().filter(item -> !item.getConfirm()).count();

		String itemText = inspectItems.size() + " ITEMS (Checked:" + checkCount + ", Unchecked:" + uncheckCount + ")";
		if (!"ALL".equals(currentFilterStatus)) {
			List<InspectionItem> filtered = filterByConfirmStatus(currentFilterStatus);
			itemText = filtered.size() + " of " + inspectItems.size() + " ITEMS";
		}

		// double prodAmount = inspectItems.stream().mapToDouble(InspectionItem::getAmount).sum();
		// productSummary.set(String.format("  $%.2f | %s", prodAmount, itemText));
	}

	private void loadSuppliers() {
		suppliers.clear();
		displaySupplierNames.clear();
		//List<Supplier> list = supplierService.findAll();
		// if (list != null) {
		// 	suppliers.addAll(list);
		// 	list.forEach(s -> displaySupplierNames.add(s.getCompany()));
		// }
	}

	private void loadCategories() {
		categories.clear();
		// ObservableList<String> list = promotionController.getComboBoxCategory();
		// if (list != null) {
		// 	categories.addAll(list);
		// }
	}

	// ---------------- Getters & Properties ----------------

	public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
	public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
	public StringProperty inspectionSummaryProperty() { return inspectionSummary; }
	public StringProperty productSummaryProperty() { return productSummary; }
	public BooleanProperty darkThemeProperty() { return darkTheme; }

	public ObservableList<Supplier> getSuppliers() { return suppliers; }
	public ObservableList<String> getCategories() { return categories; }
	public ObservableList<String> getDisplaySupplierNames() { return displaySupplierNames; }
	public ObservableList<String> getConfirmFilterOptions() { return confirmFilterOptions; }
	public ObservableList<Inspection> getInspections() { return inspections; }
	public ObservableList<InspectionItem> getInspectItems() { return inspectItems; }
}