package com.swna.javafx.admin.sale.viewmodel;

import com.swna.javafx.admin.sale.api.SaleApiClient;
import com.swna.javafx.admin.sale.dto.SaleDto;
import com.swna.javafx.admin.sale.dto.SaleItemResponse;
import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 판매 화면 ViewModel (정산 및 합계 데이터 정밀화 리팩토링 버전)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesViewModel {
    
    private final SaleApiClient saleApiClient;
    
    // 판매 목록 (JavaFX Property를 위한 Model)
    private final ObservableList<SaleModel> salesList = FXCollections.observableArrayList();
    private final ObjectProperty<SaleModel> selectedSale = new SimpleObjectProperty<>();
    
    // 판매 아이템 목록
    private final ObservableList<SaleItemModel> saleItemsList = FXCollections.observableArrayList();
    
    // 검색 조건 (날짜)
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>(LocalDate.now());
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>(LocalDate.now());
    
    // 상태 표현 프로퍼티
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    
    // =========================================================================
    // 💡 [리팩토링] 상단 통계/집계 내역의 정합성을 위한 프로퍼티 완성
    // =========================================================================
    private final ObjectProperty<BigDecimal> totalSalesAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCostAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalDiscountAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalReceivedAmount = new SimpleObjectProperty<>(BigDecimal.ZERO); // 컴파일 에러 해결용 유지
    private final ObjectProperty<BigDecimal> totalCashAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCreditAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCashoutAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    
    public void refresh() {
        loadSalesByDateRange();
    }
    
    /**
     * 기본 검색창 조회 (시작 날짜 ~ 종료 날짜 범위 검색)
     */
    public void loadSalesByDateRange() {
        if (startDate.get() == null || endDate.get() == null) {
            errorMessage.set("시작 날짜와 종료 날짜를 선택해주세요.");
            return;
        }
        LocalDateTime startDateTime = startDate.get().atStartOfDay();
        LocalDateTime endDateTime = endDate.get().atTime(java.time.LocalTime.MAX);
        
        log.info("[ViewModel] Loading sales for range: {} ~ {}", startDateTime, endDateTime);
        executeSalesLoad(saleApiClient.getSalesByDateRange(startDateTime, endDateTime));
    }
    
    /**
     * 오늘 판매 목록 조회 (오늘 버튼 연동)
     */
    public void loadTodaySales() {
        log.info("[ViewModel] Loading today's sales");
        startDate.set(LocalDate.now());
        endDate.set(LocalDate.now());
        executeSalesLoad(saleApiClient.getTodaySales());
    }
    
    /**
     * 이번주 판매 목록 조회 (이번주 버튼 연동)
     */
    public void loadThisWeekSales() {
        log.info("[ViewModel] Loading this week's sales");
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        startDate.set(startOfWeek);
        endDate.set(endOfWeek);
        
        executeSalesLoad(saleApiClient.getThisWeekSales());
    }
    
    /**
     * 이번달 판매 목록 조회 (이번달 버튼 연동)
     */
    public void loadThisMonthSales() {
        log.info("[ViewModel] Loading this month's sales");
        LocalDate now = LocalDate.now();
        startDate.set(now.withDayOfMonth(1));
        endDate.set(now.withDayOfMonth(now.lengthOfMonth()));
        
        executeSalesLoad(saleApiClient.getThisMonthSales());
    }
    
    /**
     * [공통 메서드] 판매 API 비동기 처리 파이프라인 통합 관리 공통 함수
     */
    private void executeSalesLoad(Mono<List<SaleDto>> salesMono) {
        loading.set(true);
        errorMessage.set("");
        
        salesMono
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                sales -> Platform.runLater(() -> {
                    List<SaleModel> models = sales.stream()
                        .map(this::convertToModel)
                        .toList();
                    salesList.setAll(models);
                    
                    if (!salesList.isEmpty()) {
                        SaleModel firstSale = salesList.get(0);
                        selectedSale.set(firstSale);
                        loadSaleItems(firstSale.getId());
                    } else {
                        selectedSale.set(null);
                        saleItemsList.clear();
                    }
                    
                    calculateTotals(); 
                    loading.set(false);
                    log.info("[ViewModel] Sales load finished. Total count: {}", models.size());
                }),
                error -> Platform.runLater(() -> {
                    log.error("[ViewModel] Failed to execute sales load", error);
                    errorMessage.set("서버로부터 판매 내역을 불러오지 못했습니다.");
                    salesList.clear();
                    selectedSale.set(null);
                    saleItemsList.clear();
                    calculateTotals();
                    loading.set(false);
                })
            );
    }
    
    /**
     * 특정 saleId의 아이템 상세 목록을 서버에서 조회하여 바인딩
     */
    public void loadSaleItems(Long saleId) {
        if (saleId == null) {
            saleItemsList.clear();
            return;
        }
        
        log.debug("[ViewModel] Requesting items for saleId: {}", saleId);
        
        saleApiClient.getSaleItemsBySaleId(saleId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                items -> Platform.runLater(() -> {
                    List<SaleItemModel> itemModels = items.stream()
                        .map(this::convertToItemModel)
                        .toList();
                    saleItemsList.setAll(itemModels);
                    log.debug("[ViewModel] Loaded {} items for saleId: {}", itemModels.size(), saleId);
                }),
                error -> Platform.runLater(() -> {
                    log.error("[ViewModel] Failed to load sale items for saleId: {}", saleId, error);
                    errorMessage.set("상품 상세 목록을 가져오는데 실패했습니다.");
                    saleItemsList.clear();
                })
            );
    }
    
    /**
     * 사용자가 UI에서 직접 테이블 행을 클릭하거나 변경했을 때 호출되는 트리거 메서드
     */
    public void onSelectedSaleChanged(SaleModel newSale) {
        if (newSale != null) {
            log.debug("[ViewModel] Selection changed to Sale ID: {}", newSale.getId());
            loadSaleItems(newSale.getId());
        } else {
            saleItemsList.clear();
        }
    }
    
    private SaleModel convertToModel(SaleDto dto) {
        SaleModel model = new SaleModel();
        
        if (dto.getId() != null && !dto.getId().isBlank()) {
            try {
                model.setId(Long.parseLong(dto.getId()));
            } catch (NumberFormatException e) {
                log.error("Failed to parse sale id to Long: {}", dto.getId());
            }
        }
        
        model.setReceiptNo(dto.getReceiptNo());
        model.setPaymentDateTime(dto.getPaymentDateTime());
        model.setOriginalAmount(dto.getOriginalAmount());
        model.setCashAmount(dto.getCashAmount());
        model.setCashoutAmount(dto.getCashoutAmount());
        model.setCreditAmount(dto.getCreditAmount());
        model.setDiscountAmount(dto.getDiscountAmount());
        model.setCostAmount(dto.getCostAmount());
        model.setSaleAmount(dto.getSaleAmount());
        model.setReceivedAmount(dto.getReceivedAmount());
        model.setChangeAmount(dto.getChangeAmount());
        model.setPaymentType(dto.getPaymentType());
        model.setCardNumber(dto.getCardNumber());
        return model;
    }
    
    private SaleItemModel convertToItemModel(SaleItemResponse response) {
        SaleItemModel model = new SaleItemModel();
        
        model.idProperty().set(response.id() != null ? response.id() : "");
        model.barcodeProperty().set(response.barcode() != null ? response.barcode() : "");
        model.quantityProperty().set(response.quantity());
        
        BigDecimal salePrice = response.salePrice() != null ? response.salePrice() : BigDecimal.ZERO;
        BigDecimal discountPrice = response.discountPrice() != null ? response.discountPrice() : BigDecimal.ZERO;
        BigDecimal cost = response.cost() != null ? response.cost() : BigDecimal.ZERO;
        
        model.salePriceProperty().set(salePrice);
        model.discountPriceProperty().set(discountPrice);
        model.originalPriceProperty().set(salePrice.add(discountPrice));
        model.costProperty().set(cost);
        
        BigDecimal qty = BigDecimal.valueOf(response.quantity());
        model.saleAmountProperty().set(salePrice.multiply(qty));
        model.discountAmountProperty().set(discountPrice.multiply(qty));
        model.originalAmountProperty().set(salePrice.add(discountPrice).multiply(qty));
        
        model.setComment(response.comment() != null ? response.comment() : "");
        return model;
    }
    
    /**
     * 💡 [핵심 리팩토링] 모든 정산 항목이 테이블 리스트 총합과 정확히 일치하도록 집계식 정밀화
     */
    private void calculateTotals() {
        BigDecimal totalSales = salesList.stream()
            .map(SaleModel::getSaleAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = salesList.stream()
            .map(SaleModel::getCostAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalDiscount = salesList.stream()
            .map(SaleModel::getDiscountAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReceived = salesList.stream()
            .map(SaleModel::getReceivedAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalCash = salesList.stream()
            .map(SaleModel::getCashAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = salesList.stream()
            .map(SaleModel::getCreditAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalCashout = salesList.stream()
            .map(SaleModel::getCashoutAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalSalesAmount.set(totalSales);
        totalCostAmount.set(totalCost);
        totalDiscountAmount.set(totalDiscount);
        totalReceivedAmount.set(totalReceived);
        totalCashAmount.set(totalCash);
        totalCreditAmount.set(totalCredit);
        totalCashoutAmount.set(totalCashout);
        totalCount.set(salesList.size());
    }
    
    // ==========================================
    // JavaFX UI 바인딩용 Getters
    // ==========================================
    public ObservableList<SaleModel> getSalesList() { return salesList; }
    public ObservableList<SaleItemModel> getSaleItemsList() { return saleItemsList; }
    public ObjectProperty<SaleModel> selectedSaleProperty() { return selectedSale; }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
    public BooleanProperty loadingProperty() { return loading; }
    
    public ObjectProperty<BigDecimal> totalSalesAmountProperty() { return totalSalesAmount; }
    public ObjectProperty<BigDecimal> totalCostAmountProperty() { return totalCostAmount; }         
    public ObjectProperty<BigDecimal> totalDiscountAmountProperty() { return totalDiscountAmount; }
    public ObjectProperty<BigDecimal> totalReceivedAmountProperty() { return totalReceivedAmount; } // 복구완료
    public ObjectProperty<BigDecimal> totalCashAmountProperty() { return totalCashAmount; }         
    public ObjectProperty<BigDecimal> totalCreditAmountProperty() { return totalCreditAmount; }     
    public ObjectProperty<BigDecimal> totalCashoutAmountProperty() { return totalCashoutAmount; }
    
    public IntegerProperty totalCountProperty() { return totalCount; }
    public StringProperty errorMessageProperty() { return errorMessage; }
}