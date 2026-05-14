package com.swna.javafx.admin.supplier.viewmodel;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.service.SupplierService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class SupplierViewModel {

    private final SupplierService supplierService;

    // ===== 원본 데이터 =====
    private final ObservableList<Supplier> allSuppliers = FXCollections.observableArrayList();
    
    // ===== 필터링된 데이터 (테이블에 표시) =====
    private final FilteredList<Supplier> filteredSuppliers = new FilteredList<>(allSuppliers, supplier -> true);
    
    @Getter
    private final ObservableList<Supplier> suppliers = FXCollections.unmodifiableObservableList(filteredSuppliers);

    // ===== Property =====
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty searchKeyword = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final BooleanProperty showActiveOnly = new SimpleBooleanProperty(false);
    private final ObjectProperty<Supplier> selectedSupplier = new SimpleObjectProperty<>();
    
    // ===== 더티 트래킹 =====
    private final Set<Supplier> dirtySuppliers = new HashSet<>();
    
    // ===== 카운트 =====
    private final ReadOnlyIntegerWrapper totalCount = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper filteredCount = new ReadOnlyIntegerWrapper(0);

    // ===== 생성자 =====
    public SupplierViewModel(SupplierService supplierService) {
        this.supplierService = supplierService;
        
        // 필터 조건 설정
        filteredSuppliers.predicateProperty().bind(
            javafx.beans.binding.Bindings.createObjectBinding(
                this::createPredicate,
                searchKeyword, showActiveOnly
            )
        );
        
        // 카운트 업데이트 리스너
        filteredSuppliers.addListener((javafx.collections.ListChangeListener<Supplier>) c -> updateCounts());
        allSuppliers.addListener((javafx.collections.ListChangeListener<Supplier>) c -> updateCounts());
    }

    // =================================================
    // PROPERTY GETTERS (Controller 바인딩용)
    // =================================================

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public StringProperty searchKeywordProperty() {
        return searchKeyword;
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public BooleanProperty showActiveOnlyProperty() {
        return showActiveOnly;
    }
    
    public ObjectProperty<Supplier> selectedSupplierProperty() {
        return selectedSupplier;
    }
    
    public ReadOnlyIntegerProperty totalCountProperty() {
        return totalCount.getReadOnlyProperty();
    }
    
    public ReadOnlyIntegerProperty filteredCountProperty() {
        return filteredCount.getReadOnlyProperty();
    }
    
    // ===== 일반 Getter/Setter =====
    
    public Supplier getSelectedSupplier() {
        return selectedSupplier.get();
    }
    
    public void setSelectedSupplier(Supplier supplier) {
        selectedSupplier.set(supplier);
    }
    
    public int getTotalCount() {
        return totalCount.get();
    }
    
    public int getFilteredCount() {
        return filteredCount.get();
    }

    // =================================================
    // INITIALIZE & RELOAD (Controller에서 호출)
    // =================================================

    public void initialize() {
        load();
    }
    
    public void reload() {
        load();
    }

    public void load() {
        loading.set(true);
        statusMessage.set("Loading suppliers...");

        supplierService.getAllSuppliers()
                .subscribe(
                        result -> Platform.runLater(() -> {
                            allSuppliers.setAll(result);
                            loading.set(false);
                            statusMessage.set("Loaded: " + result.size() + " suppliers");
                            log.info("Loaded {} suppliers", result.size());
                        }),
                        error -> Platform.runLater(() -> {
                            loading.set(false);
                            statusMessage.set("Error: " + error.getMessage());
                            log.error("Failed to load suppliers", error);
                        })
                );
    }

    // =================================================
    // SEARCH (자동 필터링)
    // =================================================

    public void search() {
        // searchKeyword 속성만 변경하면 필터가 자동으로 적용됨
        statusMessage.set("Searching: \"" + searchKeyword.get() + "\"");
    }
    
    /**
     * 서버 측 검색이 필요한 경우 호출
     */
    public void searchOnServer() {
        loading.set(true);
        statusMessage.set("Searching on server...");

        supplierService.searchSuppliers(searchKeyword.get())
                .subscribe(
                        result -> Platform.runLater(() -> {
                            allSuppliers.setAll(result);
                            loading.set(false);
                            statusMessage.set("Search result: " + result.size() + " items");
                        }),
                        error -> Platform.runLater(() -> {
                            loading.set(false);
                            statusMessage.set("Search error: " + error.getMessage());
                            log.error("Search failed", error);
                        })
                );
    }

    // =================================================
    // FILTER PREDICATE
    // =================================================

    private java.util.function.Predicate<Supplier> createPredicate() {
        String keywordLower = searchKeyword.get() == null ? "" : searchKeyword.get().toLowerCase().trim();
        boolean activeOnly = showActiveOnly.get();

        return supplier -> {
            // 활성 상태 필터
            if (activeOnly && !supplier.isActive()) {
                return false;
            }

            // 키워드 필터 (빈 키워드면 통과)
            if (keywordLower.isEmpty()) {
                return true;
            }

            // 여러 필드에서 키워드 검색
            return matchesKeyword(supplier, keywordLower);
        };
    }

    private boolean matchesKeyword(Supplier supplier, String keyword) {
        if (supplier.getAbbr() != null && supplier.getAbbr().toLowerCase().contains(keyword)) return true;
        if (supplier.getName() != null && supplier.getName().toLowerCase().contains(keyword)) return true;
        if (supplier.getCompany() != null && supplier.getCompany().toLowerCase().contains(keyword)) return true;
        if (supplier.getPhone() != null && supplier.getPhone().contains(keyword)) return true;
        if (supplier.getEmail() != null && supplier.getEmail().toLowerCase().contains(keyword)) return true;
        if (supplier.getAddress() != null && supplier.getAddress().toLowerCase().contains(keyword)) return true;
        return false;
    }

    private void updateCounts() {
        totalCount.set(allSuppliers.size());
        filteredCount.set(filteredSuppliers.size());
    }

    // =================================================
    // DIRTY TRACKING
    // =================================================

    public void markAsDirty(Supplier supplier) {
        if (supplier != null && !dirtySuppliers.contains(supplier)) {
            dirtySuppliers.add(supplier);
            statusMessage.set("Modified: " + supplier.getFullName());
            log.debug("Marked as dirty: {}", supplier.getFullName());
        }
    }

    public boolean isDirty(Supplier supplier) {
        return dirtySuppliers.contains(supplier);
    }

    public Set<Supplier> getDirtySuppliers() {
        return new HashSet<>(dirtySuppliers);
    }

    public void clearDirty() {
        dirtySuppliers.clear();
    }

    public void saveAllChanges() {
        if (dirtySuppliers.isEmpty()) {
            statusMessage.set("No changes to save");
            return;
        }

        loading.set(true);
        statusMessage.set("Saving " + dirtySuppliers.size() + " changes...");

        // TODO: 실제 저장 로직 구현
        Platform.runLater(() -> {
            loading.set(false);
            statusMessage.set("Saved " + dirtySuppliers.size() + " changes");
            clearDirty();
        });
    }

    // =================================================
    // DELETE
    // =================================================

    public void deleteSupplier(Supplier supplier) {
        if (supplier == null) return;

        loading.set(true);
        statusMessage.set("Deleting " + supplier.getFullName() + "...");

        supplierService.deleteSupplier(supplier.getId())
                .subscribe(
                        result -> Platform.runLater(() -> {
                            allSuppliers.remove(supplier);
                            dirtySuppliers.remove(supplier);
                            
                            if (selectedSupplier.get() == supplier) {
                                selectedSupplier.set(null);
                            }
                            
                            loading.set(false);
                            statusMessage.set("Deleted: " + supplier.getFullName());
                            log.info("Deleted supplier: {}", supplier.getFullName());
                        }),
                        error -> Platform.runLater(() -> {
                            loading.set(false);
                            statusMessage.set("Delete failed: " + error.getMessage());
                            log.error("Delete failed for supplier: {}", supplier.getFullName(), error);
                        })
                );
    }

    // =================================================
    // SAVE (단일 항목)
    // =================================================

    public void saveSupplier(Supplier supplier) {
        if (supplier == null) return;

        loading.set(true);
        statusMessage.set("Saving " + supplier.getFullName() + "...");

        supplierService.saveSupplier(supplier)
                .subscribe(
                        saved -> Platform.runLater(() -> {
                            int index = allSuppliers.indexOf(supplier);
                            if (index >= 0) {
                                allSuppliers.set(index, saved);
                            }
                            dirtySuppliers.remove(supplier);
                            loading.set(false);
                            statusMessage.set("Saved: " + saved.getFullName());
                            log.info("Saved supplier: {}", saved.getFullName());
                        }),
                        error -> Platform.runLater(() -> {
                            loading.set(false);
                            statusMessage.set("Save failed: " + error.getMessage());
                            log.error("Save failed for supplier: {}", supplier.getFullName(), error);
                        })
                );
    }

    // =================================================
    // ADD
    // =================================================

    public void addSupplier(Supplier supplier) {
        if (supplier == null) return;

        loading.set(true);
        statusMessage.set("Adding new supplier...");

        supplierService.createSupplier(supplier)
                .subscribe(
                        created -> Platform.runLater(() -> {
                            allSuppliers.add(created);
                            loading.set(false);
                            statusMessage.set("Added: " + created.getFullName());
                            log.info("Added new supplier: {}", created.getFullName());
                        }),
                        error -> Platform.runLater(() -> {
                            loading.set(false);
                            statusMessage.set("Add failed: " + error.getMessage());
                            log.error("Failed to add supplier", error);
                        })
                );
    }
}