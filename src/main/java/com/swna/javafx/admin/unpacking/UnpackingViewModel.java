package com.swna.javafx.admin.unpacking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.unpacking.api.UnpackApiClient;
import com.swna.javafx.admin.unpacking.dto.UnpackDto;
import com.swna.javafx.admin.unpacking.dto.UnpackItemDto;
import com.swna.javafx.admin.unpacking.model.Unpack;
import com.swna.javafx.admin.unpacking.model.UnpackItem;
import com.swna.javafx.common.util.AlertDialog;

import javafx.application.Platform;
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
import javafx.scene.control.ButtonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnpackingViewModel {

    private final UnpackApiClient unpackApiClient;
    private final AlertDialog alertDialog;

    // ---------------- State Properties ----------------
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>(LocalDate.now().withDayOfMonth(1));
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>(LocalDate.now());
    private final StringProperty priceMultiplier = new SimpleStringProperty("2.3");
    private final StringProperty unpacksSummary = new SimpleStringProperty("  $0.00 | 0 ITEMS");
    private final StringProperty itemsSummary = new SimpleStringProperty("  $0.00 | 0 ITEMS");
    private final BooleanProperty darkTheme = new SimpleBooleanProperty(false);

    private String currentFilterStatus = "ALL";

    // ---------------- Collections ----------------
    private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
    private final ObservableList<String> categories = FXCollections.observableArrayList();
    private final ObservableList<String> displaySupplierNames = FXCollections.observableArrayList();
    private final ObservableList<String> confirmFilterOptions = FXCollections.observableArrayList("ALL", "Checked", "Unchecked", "Added", "Unadded");

    private final ObservableList<Unpack> unpacks = FXCollections.observableArrayList();
    private final ObservableList<UnpackItem> unpackItems = FXCollections.observableArrayList(item -> new Observable[] {
        item.qtyProperty(), item.commentProperty(), item.confirmProperty(), item.priceoutProperty(), item.priceinProperty()
    });

    public void initialize() {
        initItemChangeListener();
        reload();
    }

    /** 1. READ: 기간별 Unpack 및 UnpackItem 목록 조회 */
    public void reload() {
        Map<String, Object> param = Map.of(
            "start", startDate.get().toString(),
            "end", endDate.get().toString()
        );

        unpackApiClient.getUnpacks(param)
            .subscribe(
                response -> {
                    if (response != null && response.isSuccess() && response.data() != null) {
                        List<Unpack> list = response.data().stream()
                                .map(UnpackDto::toModel)
                                .toList();
                        Platform.runLater(() -> {
                            unpacks.setAll(list);
                            unpackItems.clear();

                            if (!unpacks.isEmpty()) {
                                selectInspection(unpacks.get(0));
                            } else {
                                calculateResults();
                            }
                        });
                    }
                },
                error -> log.error("Unpacks 조회 실패", error)
            );
    }

    /** 2. CREATE: Unpack 단건 저장 */
    public void addInspection(Unpack unpack) {
        if (unpack == null) return;

        UnpackDto dto = UnpackDto.fromModel(unpack);
        unpackApiClient.postUnpack(dto)
            .subscribe(
                response -> {
                    if (response != null && response.isSuccess() && response.data() != null) {
                        Unpack savedModel = response.data().toModel();
                        Platform.runLater(() -> {
                            unpacks.add(0, savedModel);
                            selectInspection(savedModel);
                        });
                    }
                },
                error -> log.error("Unpack 생성 실패", error)
            );
    }

    /** 3. UPDATE: UnpackItem 단건 인라인 수정 처리 */
    private void initItemChangeListener() {
        unpackItems.addListener((ListChangeListener<UnpackItem>) change -> {
            while (change.next()) {
                if (change.wasUpdated()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        UnpackItem item = unpackItems.get(i);
                        
                        BigDecimal qty = BigDecimal.valueOf(item.getQty());
                        BigDecimal priceIn = item.getPricein() != null ? item.getPricein() : BigDecimal.ZERO;
                        
                        // 수량 * 단가 연산 및 반올림 처리
                        BigDecimal newAmount = priceIn.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                        
                        if (item.getAmount() == null || item.getAmount().compareTo(newAmount) != 0) {
                            item.setAmount(newAmount);
                        }

                        updateSingleUnpackItem(item);
                    }
                    
                    calculateResults();
                }
            }
        });
    }

    public void updateSingleUnpackItem(UnpackItem item) {
        if (item == null || item.getId() == 0) return;

        UnpackItemDto dto = UnpackItemDto.fromModel(item);
        unpackApiClient.updateUnpackItem(dto)
            .subscribe(
                response -> log.info("UnpackItem 수정 성공: id={}", item.getId()),
                error -> log.error("UnpackItem 수정 실패", error)
            );
    }



    /** 선택된 확정 품목 재고 등록 처리 (확인 다이얼로그 적용) */
    public void addStockForConfirmedItems() {
        if (unpackItems.isEmpty()) {
            alertDialog.dialogString("No products were found to add  Or check the RP.");
            return;
        }

        // 1. 라인 번호(LineNo) 순차 부여
        for (int i = 0; i < unpackItems.size(); i++) {
            unpackItems.get(i).setLineNo(i + 1);
        }

        // 2. 대상 품목 필터링 (확정, 미저장, 판매가 또는 예상가 존재)
        List<UnpackItem> targetItems = unpackItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getConfirm()) && !Boolean.TRUE.equals(item.getIsSaved()))
                .filter(item -> (item.getPriceoutEstimated() != null && item.getPriceoutEstimated().compareTo(BigDecimal.ZERO) != 0) 
                             || (item.getPriceout() != null && item.getPriceout().compareTo(BigDecimal.ZERO) != 0))
                .toList();

        // 대상 품목이 없는 경우 알림창 표시
        if (targetItems.isEmpty()) {
            alertDialog.dialogString("No products were found to add  Or check the RP.");
            return;
        }

        // 3. 바코드 중복 체크
        if (hasDuplicatesBarcode(targetItems)) {
            log.warn("중복된 바코드가 존재합니다.");
            return;
        }

        // 4. 재고 등록 여부 confirmation 다이얼로그 호출
        Optional<ButtonType> result = alertDialog.dialogDecisionString(
                "  Did you want to add checked." + targetItems.size() + " items to inventory ?"
        );

        // 사용자가 OK를 누르지 않고 취소한 경우 중단
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        // 5. 판매가 미입력 시 예상 판매가로 자동 대입
        targetItems.forEach(item -> {
            if (item.getPriceout() == null || item.getPriceout().compareTo(BigDecimal.ZERO) == 0) {
                item.setPriceout(item.getPriceoutEstimated());
            }
        });

        // 6. DTO 변환 및 API 호출
        List<UnpackItemDto> dtos = targetItems.stream()
                .map(UnpackItemDto::fromModel)
                .toList();

        unpackApiClient.updateUnpackItems(dtos)
            .subscribe(
                response -> {
                    if (response != null && response.isSuccess()) {
                        log.info("재고 등록 성공");
                        Platform.runLater(this::reload);
                    } else {
                        // 실패 시 UI 스레드에서 알림창 표시
                        Platform.runLater(() -> 
                            alertDialog.dialogString("Barcodes are registered with other companies.")
                        );
                    }
                },
                error -> log.error("재고 등록 실패", error)
            );
    }

    /** 5. DELETE: 선택된 Unpack 삭제 */
    public void deleteSelectedInspections() {
        List<Unpack> deleteList = unpacks.stream()
                .filter(Unpack::isSelected)
                .toList();

        if (deleteList.isEmpty()) return;

        List<UnpackDto> dtos = deleteList.stream()
                .map(UnpackDto::fromModel)
                .toList();

        unpackApiClient.deleteUnpacks(dtos)
            .subscribe(
                response -> {
                    if (response != null && response.isSuccess()) {
                        Platform.runLater(this::reload);
                    }
                },
                error -> log.error("Unpack 삭제 실패", error)
            );
    }

    // ---------------- UI Helper & Logic ----------------

    public void selectInspection(Unpack selected) {
        unpacks.forEach(item -> item.setSelected(false));
        unpackItems.clear();

        if (selected != null) {
            selected.setSelected(true);
            if (selected.getItems() != null) {
                unpackItems.addAll(selected.getItems());
            }
        }
        calculateResults();
    }

    /** 예상 판매가 배수 적용 (BigDecimal 연산 적용) */
    public void applyEstimatedPriceMultiplier() {
        String multiplierStr = priceMultiplier.get();
        if (multiplierStr == null || !multiplierStr.matches("^\\d+(\\.\\d+)?$")) return;
        
        BigDecimal multiplier = new BigDecimal(multiplierStr);

        // unpackItems 내부 객체들의 priceoutEstimated 변경
        unpackItems.forEach(item -> {
            BigDecimal priceIn = item.getPricein() != null ? item.getPricein() : BigDecimal.ZERO;
            BigDecimal estimated = priceIn.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            item.setPriceoutEstimated(estimated);
        });
    }

    public List<UnpackItem> filterByConfirmStatus(String status) {
        this.currentFilterStatus = status;
        List<UnpackItem> filtered = switch (status) {
            case "Checked" -> unpackItems.stream().filter(UnpackItem::getConfirm).toList();
            case "Unchecked" -> unpackItems.stream().filter(i -> !i.getConfirm()).toList();
            case "Added" -> unpackItems.stream().filter(UnpackItem::getIsSaved).toList();
            case "Unadded" -> unpackItems.stream().filter(i -> !i.getIsSaved()).toList();
            default -> unpackItems;
        };
        calculateResults();
        return filtered;
    }

    public boolean hasDuplicatesBarcode(List<UnpackItem> productList) {
        for (int i = 0; i < productList.size(); i++) {
            for (int j = i + 1; j < productList.size(); j++) {
                if (productList.get(i).getBarcode() != null &&
                    productList.get(i).getBarcode().trim().equals(productList.get(j).getBarcode().trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 합계 금액 계산 (BigDecimal 합산 적용) */
    public void calculateResults() {
        double inspAmount = unpacks.stream()
                .mapToDouble(Unpack::getAmount).sum();
        unpacksSummary.set(String.format("  $%.2f | %d ITEMS", inspAmount, unpacks.size()));

        long checkCount = unpackItems.stream().filter(UnpackItem::getConfirm).count();
        long uncheckCount = unpackItems.stream().filter(i -> !i.getConfirm()).count();

        // Stream에서 BigDecimal 안전하게 합산
        BigDecimal prodAmount = unpackItems.stream()
                .map(UnpackItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String itemText = String.format("  $%.2f | %d ITEMS (Checked:%d, Unchecked:%d)", 
                prodAmount, unpackItems.size(), checkCount, uncheckCount);
        itemsSummary.set(itemText);
    }

    public void filterBySupplier(Supplier supplier) {
        if (supplier == null) return;
        String abbr = supplier.getAbbr();
        List<Unpack> filtered = unpacks.stream()
                .filter(item -> abbr != null && abbr.equals(item.getSupplierAbbr()))
                .toList();

        if (!filtered.isEmpty()) {
            selectInspection(filtered.get(0));
        }
    }

    // ---------------- Getters & Properties ----------------
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
    public StringProperty inspectionSummaryProperty() { return unpacksSummary; }
    public StringProperty productSummaryProperty() { return itemsSummary; }
    public StringProperty priceMultiplierProperty() {  return priceMultiplier;  }
    public BooleanProperty darkThemeProperty() { return darkTheme; }

    public ObservableList<Supplier> getSuppliers() { return suppliers; }
    public ObservableList<String> getCategories() { return categories; }
    public ObservableList<String> getDisplaySupplierNames() { return displaySupplierNames; }
    public ObservableList<String> getConfirmFilterOptions() { return confirmFilterOptions; }
    public ObservableList<Unpack> getUnpacks() { return unpacks; }
    public ObservableList<UnpackItem> getUnpackItems() { return unpackItems; }
}