package com.swna.javafx.admin.inventory.viewmodel;

import com.swna.javafx.admin.inventory.api.InventoryApiClient;
import com.swna.javafx.admin.inventory.model.Inventory;
import com.swna.javafx.admin.inventory.model.InventoryUpdateRequest;
import com.swna.javafx.admin.supplier.api.SupplierApiClient;
import com.swna.javafx.admin.supplier.domain.Supplier;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Getter
@RequiredArgsConstructor
public class InventoryViewModel {

    private final SupplierApiClient supplierApiClient; // 👈 injected API client
    private final InventoryApiClient inventoryApiClient; 

    // Observable data for ComboBox / TableView
    private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList(); // 👈 String -> Supplier
    private final ObservableList<String> categories = FXCollections.observableArrayList();
    private final ObservableList<Inventory> inventoryList = FXCollections.observableArrayList();

    // FilteredList 추가 — inventoryList보다 아래에 선언 + 즉시 초기화 (초기화 순서 중요)
    private final FilteredList<Inventory> filteredInventoryList =
            new FilteredList<>(inventoryList, item -> true);

    // SortedList 추가 — 헤더 클릭 정렬 지원. comparator는 Controller에서
    // tableView.comparatorProperty()와 바인딩해야 실제로 동작함.
    // 반드시 filteredInventoryList보다 아래에 선언 (초기화 순서 중요).
    private final SortedList<Inventory> sortedInventoryList =
            new SortedList<>(filteredInventoryList);

    // UI input / state properties
    private final ObjectProperty<Supplier> selectedSupplier = new SimpleObjectProperty<>(); // 👈 StringProperty -> ObjectProperty<Supplier>
    private final StringProperty searchText = new SimpleStringProperty();
    private final StringProperty informStockMessage = new SimpleStringProperty();

    // Initial data load
    public void initializeData() {
        categories.setAll("Category 1", "Category 2");
        registerSelectedSupplierListener();
        registerSearchTextListener(); // 👈 검색어 변경 시 필터 predicate 갱신
        loadSuppliers(); // 👈 replaced hardcoded list with API call
        loadInventoryList();
    }


    /**
     * Supplier 선택이 바뀔 때마다 해당 abbr 기준으로 재고 목록을 재조회.
     */
    private void registerSelectedSupplierListener() {
        selectedSupplier.addListener((obs, oldSupplier, newSupplier) -> {
            if (newSupplier == null || newSupplier.getAbbr() == null || newSupplier.getAbbr().isBlank()) {
                log.warn("Selected supplier has no abbr — skipping inventory reload.");
                return;
            }
            loadInventoryByAbbr(newSupplier.getAbbr());
        });
    }

    /**
     * textFieldSearch 입력값(searchText)이 바뀔 때마다 FilteredList의 predicate를 재설정.
     */
    private void registerSearchTextListener() {
        searchText.addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));
    }

    /**
     * 검색어 기준으로 inventory 항목을 필터링.
     * 검색 대상 필드: code, barcode, description, comment (OR 조건)
     */
    private void applySearchFilter(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            filteredInventoryList.setPredicate(item -> true);
            return;
        }

        String lower = keyword.trim().toLowerCase();
        filteredInventoryList.setPredicate(item ->
                containsIgnoreCase(item.getCode(), lower)
                || containsIgnoreCase(item.getBarcode(), lower)
                || containsIgnoreCase(item.getDescription(), lower)
                || containsIgnoreCase(item.getComment(), lower));
    }

    /**
     * null-safe 대소문자 무시 부분 일치 체크.
     */
    private boolean containsIgnoreCase(String value, String keywordLower) {
        return value != null && value.toLowerCase().contains(keywordLower);
    }

    /**
     * Fetches the supplier list from the API and populates the ComboBox data.
     */
    public void loadSuppliers() {
        supplierApiClient.getAllSuppliers()
                .subscribe(response -> {
                    if (!response.isSuccess() || !response.hasData()) {
                        log.warn("Failed to load supplier list: {}", response.getUserFriendlyMessage());
                        return;
                    }

                    List<Supplier> supplierList = response.data().stream()
                            .map(Supplier::from)
                            .toList();

                    // The WebClient callback runs on a different thread —
                    // JavaFX UI updates must happen on the FX Application Thread.
                    Platform.runLater(() -> suppliers.setAll(supplierList));
                }, error -> log.error("Failed to load supplier list", error));
    }

    /**
     * 선택된 Supplier의 abbr을 기준으로 상품(재고) 목록을 조회하여 TableView 데이터를 갱신.
     */
    public void loadInventoryByAbbr(String abbr) {
        log.info("Loading inventory list for abbr: {}", abbr);

        inventoryApiClient.getProductsByAbbr(abbr)
                .subscribe(response -> {
                    if (!response.isSuccess() || !response.hasData()) {
                        log.warn("Failed to load inventory list for abbr [{}]: {}",
                                abbr, response.getUserFriendlyMessage());
                        Platform.runLater(inventoryList::clear);
                        return;
                    }

                    List<Inventory> items = response.data().stream()
                            .map(Inventory::from)
                            .toList();

                    Platform.runLater(() -> inventoryList.setAll(items));
                }, error -> log.error("Failed to load inventory list for abbr [{}]", abbr, error));
    }

    /**
     * RELOAD 버튼 등에서 사용 — 현재 선택된 supplier 기준으로 다시 조회.
     */
    public void loadInventoryList() {
        Supplier current = selectedSupplier.get();
        if (current == null || current.getAbbr() == null || current.getAbbr().isBlank()) {
            log.info("No supplier selected — skipping inventory reload.");
            return;
        }
        loadInventoryByAbbr(current.getAbbr());
    }

    /**
     * 🔥 단건 재고/상품 수정 서버 반영 및 UI 모델 갱신
     */
    public void updateProduct(Inventory item) {
        if (item == null || item.getId() == null) {
            log.warn("Cannot update item with null ID: {}", item);
            return;
        }

        log.info("Updating product on server for item ID: {}", item.getId());
        InventoryUpdateRequest request = item.toUpdateRequest();

        inventoryApiClient.updateProduct(item.getId(), request)
                .subscribe(
                        response -> {
                            if (response != null && response.data() != null) {
                                var updatedDto = response.data();
                                log.info("Successfully updated product ID: {}", updatedDto.id());

                                // JavaFX 스레드에서 UI 모델 필드 반영
                                Platform.runLater(() -> {
                                    item.setQuantity(updatedDto.quantity());
                                    item.setLastOrderedAt(updatedDto.lastOrderedAt());
                                    item.setSelected(false);
                                });
                            }
                        },
                        error -> log.error("Failed to update product ID: {}", item.getId(), error)
                        
                );
    }
    // ---------------- Commands (Action Handlers) ----------------

    public void handleSave() {
        log.info("Executing save logic");
    }

    public void handleDelete() {
        log.info("Executing delete logic");
        inventoryList.removeIf(Inventory::isSelected);
    }

    public void handlePrintBarcode() {
        log.info("Executing barcode print logic");
    }

    public void handleUploadExcel() {
        log.info("Executing Excel upload logic");
    }

    public void handleActionClick(Inventory item) {
        log.info("Table action clicked: {}", item.getCode());
    }
}