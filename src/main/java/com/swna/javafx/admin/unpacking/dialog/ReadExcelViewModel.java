package com.swna.javafx.admin.unpacking.dialog;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.unpacking.excel.ReaderUnpack;
import com.swna.javafx.admin.unpacking.model.UnpackItem;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ReadExcelViewModel {

    // Properties
    private final StringProperty filePath = new SimpleStringProperty();
    private final StringProperty invoiceLabel = new SimpleStringProperty(" Invoice : ");
    private final StringProperty selectedSheet = new SimpleStringProperty("Unpack");
    private final BooleanProperty darkTheme = new SimpleBooleanProperty(false);
    private final BooleanProperty checkSupplier = new SimpleBooleanProperty(false);
    private final ObjectProperty<Supplier> selectedSupplier = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now(ZoneId.systemDefault()));

    // Collections
    private final ObservableList<String> sheets = FXCollections.observableArrayList();
    private final ObservableList<String> logs = FXCollections.observableArrayList();

    private String basicFolder;

    public void initialize() {
        Path defaultPath = Paths.get(System.getProperty("user.home"), "Downloads", "unpack.xlsx");
        filePath.set(defaultPath.toAbsolutePath().toString());
        basicFolder = defaultPath.getParent().toString();
    }

    public void selectExcelFile(String selectedFile) {
        if (selectedFile != null) {
            filePath.set(selectedFile);
            loadSheetNames();
        }
    }

    public String getInitialDirectory() {
        File dir = new File(basicFolder);
        return dir.exists() ? basicFolder : "C:/";
    }

    public void loadSheetNames() {
        // Sheet loading logic if needed
    }

    /**
     * CompletableFuture 기반 비동기 Excel 업로드 처리
     */
    public void readExcel(Runnable onSuccess, Consumer<String> onError) {
        if (!updateInvoice()) {
            onError.accept("Please select a supplier.");
            return;
        }

        String targetPath = filePath.get() != null ? filePath.get().trim() : "";
        File file = new File(targetPath);
        
        if (!file.exists()) {
            onError.accept("No specified file found:\n" + targetPath);
            return;
        }
        
        clearLogs();
        
        addLog(">> Reading excel : " + targetPath);

        // 비동기로 Excel 파일 처리 실행
        CompletableFuture.supplyAsync(() -> ReaderUnpack.read(file))
            .thenAcceptAsync(items -> handleReadSuccess(items, onSuccess, onError), Platform::runLater)
            .exceptionally(ex -> {
                Platform.runLater(() -> handleReadFailure(ex, onError));
                return null;
            });
    }

    private void handleReadSuccess(List<UnpackItem> items, Runnable onSuccess, Consumer<String> onError) {
        if (items == null || items.isEmpty()) {
            addLog("@@@ File is empty or no valid items found.");
            onError.accept("No valid data found in the Excel file.");
            return;
        }

        if (hasDuplicatesBarcode(items)) {
            onError.accept("Duplicate barcodes found in the Excel file.");
            return;
        }

        Supplier supplier = selectedSupplier.get();
        enrichItemData(items, supplier);

        addLog("- Reading items : " + items.size());
        saveUnpack(items);

        for (int i = 0; i < items.size(); i++) {
            UnpackItem item = items.get(i);
            addLog((i + 1) + ". " + "code : "  + item.getCode() + " , cost : " + item.getPricein() + " , qty : " + item.getQty() + " , description : " + item.getDescription());
        }

        addLog("- Success read end \n");

        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    private void handleReadFailure(Throwable ex, Consumer<String> onError) {
        log.error("Excel processing failed", ex);
        addLog("@@@ Fail uploading : " + ex.getMessage());
        if (onError != null) {
            onError.accept("Error processing Excel file: " + ex.getMessage());
        }
    }

    private void enrichItemData(List<UnpackItem> items, Supplier supplier) {
        String invoiceNo = generateInvoiceNumber(supplier);
        items.forEach(item -> {
            //item.setInvoice(invoiceNo);
            //item.setAbbr(supplier != null ? supplier.getAbbr() : "");
            item.setSupplier(supplier != null ? supplier.getCompany() : "");
            //item.setAmount(Double.valueOf(String.format("%.2f", item.getQty() * item.getPricein())));
            item.setIsSaved(false);
        });
    }

    private void saveUnpack(List<UnpackItem> items) {
        // TODO: Repository 또는 Service 계층을 통한 DB 저장 로직 연동
    }

    private boolean updateInvoice() {
        Supplier supplier = selectedSupplier.get();
        if (supplier != null) {
            String inv = supplier.getCompany() + "_" + generateInvoiceTimestamp();
            invoiceLabel.set("Invoice : " + inv);
            return true;
        }
        return false;
    }

    private String generateInvoiceNumber(Supplier supplier) {
        return (supplier != null ? supplier.getCompany() : "UNKNOWN") + "_" + generateInvoiceTimestamp();
    }

    private String generateInvoiceTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYMMddHHmm"));
    }

    private boolean hasDuplicatesBarcode(List<UnpackItem> productList) {
        List<UnpackItem> duplicateList = new ArrayList<>();

        for (int i = 0; i < productList.size(); i++) {
            for (int j = i + 1; j < productList.size(); j++) {
                String b1 = productList.get(i).getBarcode();
                String b2 = productList.get(j).getBarcode();
                if (b1 != null && !b1.isBlank() && b1.equals(b2)) {
                    duplicateList.add(productList.get(i));
                    duplicateList.add(productList.get(j));
                }
            }
        }

        if (!duplicateList.isEmpty()) {
            addLog("++++++++++ Barcode duplicate +++++++++++++");
            for (UnpackItem product : duplicateList) {
                addLog(" - " + product.getLineNo() + ". code : " + product.getCode() 
                        + ", " + product.getBarcode() + ", Qty : " + product.getQty() 
                        + ", " + product.getDescription());
            }
            return true;
        }
        return false;
    }

    public void addLog(String message) {
        if (Platform.isFxApplicationThread()) {
            logs.add(message);
        } else {
            Platform.runLater(() -> logs.add(message));
        }
    }

    public void clearLogs() {
        if (Platform.isFxApplicationThread()) {
            logs.clear();
        } else {
            Platform.runLater(logs::clear);
        }
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