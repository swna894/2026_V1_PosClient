package com.swna.javafx.admin.unpacking.dialog;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.unpacking.model.Inspection;
import com.swna.javafx.admin.unpacking.model.InspectionItem;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

@Component
public class ReadExcelViewModel {

    // Properties
    private final StringProperty filePath = new SimpleStringProperty();
    private final StringProperty invoiceLabel = new SimpleStringProperty(" Invoice : ");
    private final StringProperty selectedSheet = new SimpleStringProperty("INVENTORY");
    private final BooleanProperty darkTheme = new SimpleBooleanProperty(false);
    private final BooleanProperty checkSupplier = new SimpleBooleanProperty(false);
    private final ObjectProperty<Supplier> selectedSupplier = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());

    // Collections
    private final ObservableList<String> sheets = FXCollections.observableArrayList();
    private final ObservableList<String> logs = FXCollections.observableArrayList();

    private String basicFolder;

    public void initialize() {
        // basicFolder = posController.getSetting().getPathUpload();
        // filePath.set(basicFolder + "/Excel Sample.xlsx");
        // darkTheme.set(posController.getSetting().getAutoColor());
    }

    public void selectExcelFile(String selectedFile) {
        if (selectedFile != null) {
            filePath.set(selectedFile);
            loadSheetNames();
        }
    }

    public String getInitialDirectory() {
        if (!(new File(basicFolder)).exists()) {
            return "C:/";
        }
        return basicFolder;
    }

    public void loadSheetNames() {
        // List<String> sheetList = excelReadProduct.getSheets(getExcelOption());
        // sheets.setAll(sheetList);
        // if (!sheets.isEmpty() && !sheets.contains(selectedSheet.get())) {
        //     selectedSheet.set(sheets.get(0));
        // }
    }

    public void uploadExcel(Runnable onSuccess, Consumer<String> onError) {
        if (!updateInvoice()) {
            onError.accept("Please select supplier");
            return;
        }

        // Task<List<InspectionItem>> task = new Task<>() {
        //     @Override
        //     protected List<InspectionItem> call() throws Exception {
        //         String file = filePath.get().trim();
        //         addLog(">> Reading excel : " + file);

        //         if (!PathUtil.isExist(file)) {
        //             throw new Exception("- No specified file found.\n\n " + file);
        //         }
        //         return excelReadProduct.read(getExcelOption());
        //     }
        // };

        // task.setOnFailed(wse -> {
        //     addLog("@@@ Fail uploading : " + wse.getSource().getException().toString());
        //     wse.getSource().getException().printStackTrace();
        // });

        // task.setOnSucceeded(wse -> {
        //     List<InspectionItem> items = task.getValue();
        //     if (items != null && !hasDuplicatesBarcode(items)) {
        //         Supplier supplier = selectedSupplier.get();
        //         items.forEach(item -> {
        //             item.setInvoice(generateInvoiceNumber(supplier));
        //             item.setAbbr(supplier.getAbbr());
        //             item.setSupplier(supplier.getCompany());
        //             item.setAmount(Double.valueOf(String.format("%.2f", item.getQty() * item.getPricein())));
        //             item.setIsSaved(false);
        //         });

        //         addLog("- Reading items : " + items.size());
        //         saveInspection(items);

        //         for (int i = 0; i < items.size(); i++) {
        //             addLog((i + 1) + ". " + items.get(i).getBarcode() + ", " + items.get(i).getDescription());
        //         }

        //         addLog("- End Saving to DBMS ");
        //         addLog("- Success read end \n ");
        //         if (onSuccess != null) onSuccess.run();
        //     }
        // });

        // new Thread(task).start();
    }

    private void saveInspection(List<InspectionItem> items) {
        Supplier supplier = selectedSupplier.get();
        // Inspection inspection = new Inspection(items, date.get());
        // if (!checkSupplier.get()) {
        //     inspection.setSync(true);
        //     inspection.getItems().forEach(item -> {
        //         item.setSupplier(supplier.getCompany());
        //         item.setAbbr(supplier.getAbbr());
        //     });
        // }
        // inspectionService.post(inspection);
    }

    private boolean updateInvoice() {
        Supplier supplier = selectedSupplier.get();
        if (supplier != null) {
            String inv = supplier.getCompany() + "_" + generateInvoiceTimestamp();
            invoiceLabel.set("INVOICE : " + inv);
            return true;
        }
        return false;
    }

    private String generateInvoiceNumber(Supplier supplier) {
        return supplier.getCompany() + "_" + generateInvoiceTimestamp();
    }

    private String generateInvoiceTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYMMddHHmm"));
    }

    private Boolean hasDuplicatesBarcode(List<InspectionItem> productList) {
        boolean duplicateFound = false;
        List<InspectionItem> duplicateList = new ArrayList<>();

        for (int i = 0; i < productList.size(); i++) {
            for (int j = i + 1; j < productList.size(); j++) {
                if (productList.get(i).getBarcode() != null && productList.get(i).getBarcode().equals(productList.get(j).getBarcode())) {
                    duplicateFound = true;
                    duplicateList.add(productList.get(i));
                    duplicateList.add(productList.get(j));
                }
            }
        }

        if (duplicateFound) {
            addLog("++++++++++ Barcode duplicate +++++++++++++");
            for (InspectionItem product : duplicateList) {
                addLog(" - " + product.getLineNo() + ". code : " + product.getCode() + ", " + product.getBarcode() + ", Qty : " + product.getQty() + ", " + product.getDescription());
            }
        }
        return duplicateFound;
    }

    // private ExcelOption getExcelOption() {
    //     ExcelOption option = new ExcelOption();
    //     option.setFileRead(filePath.get().trim());
    //     option.setSheetName(selectedSheet.get());
    //     option.setStartRow(1);
    //     return option;
    // }

    public void addLog(String message) {
        Platform.runLater(() -> logs.add(message));
    }

    // Properties Getters
    public StringProperty filePathProperty() { return filePath; }
    public ObjectProperty<LocalDate> dateProperty() { return date; }
    public StringProperty invoiceLabelProperty() { return invoiceLabel; }
    public BooleanProperty darkThemeProperty() { return darkTheme; }
    public BooleanProperty checkSupplierProperty() { return checkSupplier; }
    public ObjectProperty<Supplier> selectedSupplierProperty() { return selectedSupplier; }
    public StringProperty selectedSheetProperty() { return selectedSheet; }

    public ObservableList<String> getSheets() { return sheets; }
    public ObservableList<String> getLogs() { return logs; }
}
