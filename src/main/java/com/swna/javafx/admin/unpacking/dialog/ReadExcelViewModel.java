package com.swna.javafx.admin.unpacking.dialog;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.swna.javafx.admin.unpacking.api.UnpackApiClient;
import com.swna.javafx.admin.unpacking.dto.UnpackDto;
import com.swna.javafx.admin.unpacking.excel.ReaderUnpack;
import com.swna.javafx.admin.unpacking.model.Unpack;
import com.swna.javafx.admin.unpacking.model.UnpackItem;
import com.swna.javafx.common.response.ApiResponse;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadExcelViewModel {

    // UnpackApiClient 주입
    private final UnpackApiClient unpackApiClient;

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

        // 비동기로 Excel 파일 파싱 실행
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
        String invoiceNo = generateInvoiceNumber(supplier);

        // 1. UnpackItem 기본 값 보완 (수량/금액 및 송장/공급사 정보 바인딩)
        enrichItemData(items, supplier, invoiceNo);

        addLog("- Reading items : " + items.size());

        for (int i = 0; i < items.size(); i++) {
            UnpackItem item = items.get(i);
            addLog((i + 1) + ". " + "code : " + item.getCode() + " , cost : " + item.getPricein() + " , qty : " + item.getQty() + " , description : " + item.getDescription());
        }

        // 2. Unpack 상위 Model 객체 생성 및 금액 합산
        Unpack unpackModel = createUnpackModel(supplier, invoiceNo, items);

        // 3. Model -> DTO 변환 후 서버 전송 (postUnpack)
        saveUnpackToServer(unpackModel, onSuccess, onError);
    }

    /**
     * 서버 POST API 호출 처리 메서드
     */
    private void saveUnpackToServer(Unpack unpackModel, Runnable onSuccess, Consumer<String> onError) {
        UnpackDto dto = UnpackDto.fromModel(unpackModel);
        addLog(">> Saving to server (POST /api/unpack)...");

        unpackApiClient.postUnpack(dto)
            .subscribe(
                response -> Platform.runLater(() -> handleApiResponse(response, onSuccess, onError)),
                error -> Platform.runLater(() -> handleApiError(error, onError))
            );
    }

    private void handleApiResponse(ApiResponse<UnpackDto> response, Runnable onSuccess, Consumer<String> onError) {
        if (response != null && response.isSuccess()) {
            handleSuccess(onSuccess);
            return;
        }
        
        String errMsg = getErrorMessage(response);
        addLog("@@@ Server Error: " + errMsg);
        notifyError(onError, errMsg);
    }

    private void handleSuccess(Runnable onSuccess) {
        addLog("- Successfully saved to server!");
        addLog("- Success read end \n");
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    private void handleApiError(Throwable error, Consumer<String> onError) {
        log.error("Server API postUnpack failed", error);
        String errorMessage = "Server Error: " + error.getMessage();
        addLog("@@@ Network/Server Error: " + error.getMessage());
        notifyError(onError, errorMessage);
    }

    private String getErrorMessage(ApiResponse<UnpackDto> response) {
        if (response != null && response.message() != null) {
            return response.message();
        }
        return "Failed to save unpack to server.";
    }

    private void notifyError(Consumer<String> onError, String message) {
        if (onError != null) {
            onError.accept(message);
        }
    }

    /**
     * Unpack 상위 모델 생성 및 총 금액 BigDecimal 연산 처리
     */
    private Unpack createUnpackModel(Supplier supplier, String invoiceNo, List<UnpackItem> items) {
        Unpack unpack = new Unpack();
        unpack.setInvoice(invoiceNo);
        unpack.setSupplierAbbr(supplier != null ? supplier.getAbbr() : "");
        unpack.setUnpacked(LocalDateTime.now(ZoneId.systemDefault()));
        unpack.setSync(false);

        // BigDecimal 합산 연산 (qty * pricein)
        BigDecimal totalAmount = items.stream()
                .map(item -> {
                    BigDecimal qty = BigDecimal.valueOf(item.getQty());
                    BigDecimal priceIn = item.getPricein() != null ? item.getPricein() : BigDecimal.ZERO;
                    return priceIn.multiply(qty);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        unpack.setAmount(totalAmount.doubleValue());
        unpack.setItems(items);
        return unpack;
    }

    /**
     * Excel에서 읽어온 개별 Item 금액(amount) BigDecimal 연산 처리
     */
    private void enrichItemData(List<UnpackItem> items, Supplier supplier, String invoiceNo) {
        items.forEach(item -> {
            item.setInvoice(invoiceNo);
            item.setAbbr(supplier != null ? supplier.getAbbr() : "");
            item.setSupplier(supplier != null ? supplier.getCompany() : "");

            BigDecimal qty = BigDecimal.valueOf(item.getQty());
            BigDecimal priceIn = item.getPricein() != null ? item.getPricein() : BigDecimal.ZERO;
            BigDecimal amount = priceIn.multiply(qty).setScale(2, RoundingMode.HALF_UP);

            item.setAmount(amount);
            item.setConfirm(false);
            item.setIsSaved(false);
            item.setIsNew(true);
        });
    }

    private void handleReadFailure(Throwable ex, Consumer<String> onError) {
        log.error("Excel processing failed", ex);
        addLog("@@@ Fail uploading : " + ex.getMessage());
        if (onError != null) {
            onError.accept("Error processing Excel file: " + ex.getMessage());
        }
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
        return LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
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