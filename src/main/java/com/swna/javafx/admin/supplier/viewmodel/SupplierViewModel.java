package com.swna.javafx.admin.supplier.viewmodel;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.SupplierDomain;
import com.swna.javafx.admin.supplier.dto.SupplierResponseRecord;
import com.swna.javafx.admin.supplier.service.SupplierService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierViewModel {
    
    private final SupplierService supplierService;
    
    // ===== Observable Properties =====
    private final ObservableList<SupplierDomain> allSuppliers = FXCollections.observableArrayList();
    private final FilteredList<SupplierDomain> filteredSuppliers = new FilteredList<>(allSuppliers, p -> true);
    private final SortedList<SupplierDomain> sortedSuppliers = new SortedList<>(filteredSuppliers);
    
    private final StringProperty searchKeyword = new SimpleStringProperty("");
    private final BooleanProperty showActiveOnly = new SimpleBooleanProperty(false);
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final ObjectProperty<SupplierDomain> selectedSupplier = new SimpleObjectProperty<>();
    
    // ===== 생성자 =====
    public void initialize() {
        setupFilterListener();
        loadAllSuppliers();
    }
    
    // ===== 필터 설정 =====
    private void setupFilterListener() {
        // 검색어 + 활성화 필터 조합
        searchKeyword.addListener((obs, oldVal, newVal) -> applyFilter());
        showActiveOnly.addListener((obs, oldVal, newVal) -> applyFilter());
    }
    
    private void applyFilter() {
        String keyword = searchKeyword.get();
        boolean activeOnly = showActiveOnly.get();
        
        filteredSuppliers.setPredicate(supplier -> {
            // 활성화 필터
            if (activeOnly && !supplier.isActive()) {
                return false;
            }
            
            // 검색어 필터
            if (keyword != null && !keyword.isBlank()) {
                String lowerKeyword = keyword.toLowerCase();
                return supplier.getAbbr().toLowerCase().contains(lowerKeyword) ||
                       supplier.getName().toLowerCase().contains(lowerKeyword) ||
                       supplier.getCompany().toLowerCase().contains(lowerKeyword) ||
                       (supplier.getPhone() != null && supplier.getPhone().contains(keyword));
            }
            
            return true;
        });
        
        statusMessage.set(String.format("Showing %d of %d suppliers", 
            filteredSuppliers.size(), allSuppliers.size()));
    }

        /**
     * DirtyConsumer 인터페이스 구현
     * TableColumnUtil에서 변경 사항 발생 시 호출
     */
    public void markAsDirty(SupplierDomain supplier) {
        log.debug("[ViewModel] Supplier marked as dirty: {}", supplier.getFullName());
        // 변경된 내용을 추적하거나 서버에 저장
        statusMessage.set("Changes detected for: " + supplier.getFullName());
        
        // 필요시 자동 저장 또는 UI 표시
        // autoSave(supplier);
    }
    
    // ===== 데이터 로드 =====
    public void loadAllSuppliers() {
        loading.set(true);
        statusMessage.set("Loading suppliers...");
        
        supplierService.getAllSuppliersAsync(
            this::onLoadStart,
            this::onLoadSuccess,
            this::onLoadError
        );
    }
    
    public void reload() {
        loadAllSuppliers();
    }
    
    private void onLoadStart() {
        Platform.runLater(() -> {
            allSuppliers.clear();
            loading.set(true);
        });
    }
    
    private void onLoadSuccess(List<SupplierResponseRecord> suppliers) {
        Platform.runLater(() -> {
            List<SupplierDomain> domains = suppliers.stream()
                .map(SupplierDomain::from)
                .collect(Collectors.toList());
            
            allSuppliers.setAll(domains);
            statusMessage.set(String.format("Loaded %d suppliers", allSuppliers.size()));
            loading.set(false);
        });
    }
    
    private void onLoadError(Throwable error) {
        Platform.runLater(() -> {
            statusMessage.set("Error: " + error.getMessage());
            loading.set(false);
            log.error("Failed to load suppliers", error);
        });
    }
    
    // ===== CRUD 작업 =====
    public void addSupplier(SupplierDomain supplier) {
        // TODO: API 호출 후 목록 갱신
        allSuppliers.add(supplier);
        statusMessage.set("Supplier added successfully");
    }
    
    public void updateSupplier(SupplierDomain supplier) {
        // TODO: API 호출 후 목록 갱신
        int index = allSuppliers.indexOf(supplier);
        if (index >= 0) {
            allSuppliers.set(index, supplier);
            statusMessage.set("Supplier updated successfully");
        }
    }
    
    public void deleteSupplier(SupplierDomain supplier) {
        // TODO: API 호출 후 목록 갱신
        allSuppliers.remove(supplier);
        statusMessage.set("Supplier deleted successfully");
    }
    
    // ===== Getter for UI Binding =====
    public ObservableList<SupplierDomain> getSuppliers() {
        return sortedSuppliers;
    }
    
    public SortedList<SupplierDomain> getSortedSuppliers() {
        return sortedSuppliers;
    }
    
    public StringProperty searchKeywordProperty() {
        return searchKeyword;
    }
    
    public BooleanProperty showActiveOnlyProperty() {
        return showActiveOnly;
    }
    
    public BooleanProperty loadingProperty() {
        return loading;
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public ObjectProperty<SupplierDomain> selectedSupplierProperty() {
        return selectedSupplier;
    }
    
    public void setSelectedSupplier(SupplierDomain supplier) {
        selectedSupplier.set(supplier);
    }
    
    public SupplierDomain getSelectedSupplier() {
        return selectedSupplier.get();
    }
    
    public int getFilteredCount() {
        return filteredSuppliers.size();
    }
    
    public int getTotalCount() {
        return allSuppliers.size();
    }
}