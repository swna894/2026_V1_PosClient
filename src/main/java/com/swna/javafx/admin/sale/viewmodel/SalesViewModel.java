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
 * 판매 화면 ViewModel (메인 전표 통계 + 선택 전표 상세 아이템 통계 완성 버전)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesViewModel {
    
    private final SaleApiClient saleApiClient;
    
    // 리스트 관련 데이터 구조
    private final ObservableList<SaleModel> salesList = FXCollections.observableArrayList();
    private final ObjectProperty<SaleModel> selectedSale = new SimpleObjectProperty<>();
    private final ObservableList<SaleItemModel> saleItemsList = FXCollections.observableArrayList();
    
    // 조건 및 상태 제어 프로퍼티
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>(LocalDate.now());
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>(LocalDate.now());
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    
    // 1️⃣ [메인 판매 목록] 전체 요약 바 프로퍼티
    private final ObjectProperty<BigDecimal> totalSalesAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCostAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalDiscountAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalReceivedAmount = new SimpleObjectProperty<>(BigDecimal.ZERO); 
    private final ObjectProperty<BigDecimal> totalCashAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCreditAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCashoutAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    
    // 2️⃣ [💡 신설 - 상세 아이템 목록] 우측 상단 전표 상세 요약 바용 실시간 집계 프로퍼티
    private final ObjectProperty<BigDecimal> itemTotalAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> itemTotalOriginal = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> itemTotalDiscount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> itemTotalCost = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> itemTotalMargin = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty itemTotalQty = new SimpleIntegerProperty(0);

    public void refresh() {
        loadSalesByDateRange();
    }
    
    public void loadSalesByDateRange() {
        if (startDate.get() == null || endDate.get() == null) {
            errorMessage.set("시작 날짜와 종료 날짜를 선택해주세요.");
            return;
        }
        LocalDateTime startDateTime = startDate.get().atStartOfDay();
        LocalDateTime endDateTime = endDate.get().atTime(java.time.LocalTime.MAX);
        executeSalesLoad(saleApiClient.getSalesByDateRange(startDateTime, endDateTime));
    }
    
    public void loadTodaySales() {
        startDate.set(LocalDate.now());
        endDate.set(LocalDate.now());
        executeSalesLoad(saleApiClient.getTodaySales());
    }
    
    public void loadThisWeekSales() {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        startDate.set(startOfWeek);
        endDate.set(endOfWeek);
        executeSalesLoad(saleApiClient.getThisWeekSales());
    }
    
    public void loadThisMonthSales() {
        LocalDate now = LocalDate.now();
        startDate.set(now.withDayOfMonth(1));
        endDate.set(now.withDayOfMonth(now.lengthOfMonth()));
        executeSalesLoad(saleApiClient.getThisMonthSales());
    }
    
    private void executeSalesLoad(Mono<List<SaleDto>> salesMono) {
        loading.set(true);
        errorMessage.set("");
        
        salesMono
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                sales -> Platform.runLater(() -> {
                    List<SaleModel> models = sales.stream().map(this::convertToModel).toList();
                    salesList.setAll(models);
                    
                    if (!salesList.isEmpty()) {
                        SaleModel firstSale = salesList.get(0);
                        selectedSale.set(firstSale);
                        loadSaleItems(firstSale.getId());
                    } else {
                        selectedSale.set(null);
                        saleItemsList.clear();
                        clearItemTotals(); // 💡 초기화 안전장치
                    }
                    
                    calculateTotals(); 
                    loading.set(false);
                }),
                error -> Platform.runLater(() -> {
                    log.error("Failed to execute sales load", error);
                    errorMessage.set("서버로부터 판매 내역을 불러오지 못했습니다.");
                    salesList.clear();
                    selectedSale.set(null);
                    saleItemsList.clear();
                    clearItemTotals();
                    calculateTotals();
                    loading.set(false);
                })
            );
    }
    
    public void loadSaleItems(Long saleId) {
        if (saleId == null) {
            saleItemsList.clear();
            clearItemTotals();
            return;
        }
        
        saleApiClient.getSaleItemsBySaleId(saleId)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                items -> Platform.runLater(() -> {
                    List<SaleItemModel> itemModels = items.stream().map(this::convertToItemModel).toList();
                    saleItemsList.setAll(itemModels);
                    
                    calculateItemsTotals(); // 💡 데이터 적재 직후 실시간 집계 연산 가동
                }),
                error -> Platform.runLater(() -> {
                    log.error("Failed to load sale items", error);
                    errorMessage.set("상품 상세 목록을 가져오는데 실패했습니다.");
                    saleItemsList.clear();
                    clearItemTotals();
                })
            );
    }
    
    public void onSelectedSaleChanged(SaleModel newSale) {
        if (newSale != null) {
            loadSaleItems(newSale.getId());
        } else {
            saleItemsList.clear();
            clearItemTotals();
        }
    }
    
    /**
     * 💡 [신설 리팩토링] 선택된 전표의 품목 데이터 스트림 실시간 집계 공식 구현
     */
    private void calculateItemsTotals() {
        BigDecimal totalAmount = saleItemsList.stream()
            .map(SaleItemModel::getSaleAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOriginal = saleItemsList.stream()
            .map(SaleItemModel::getOriginalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = saleItemsList.stream()
            .map(SaleItemModel::getDiscountAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 원가 정합성 계산: 각 행의 (원가 단가 * 수량) 총합 계산
        BigDecimal totalCost = saleItemsList.stream()
            .map(item -> {
                BigDecimal unitCost = item.costProperty().get();
                BigDecimal qty = BigDecimal.valueOf(item.quantityProperty().get());
                return (unitCost != null && qty != null) ? unitCost.multiply(qty) : BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 마진 공식 = 실 판매액 합계 - 매입 원가 합계
        BigDecimal totalMargin = totalAmount.subtract(totalCost);

        // 총 상품 수량 합산
        int totalQty = saleItemsList.stream()
            .mapToInt(item -> item.quantityProperty().get())
            .sum();

        // 프로퍼티 세팅 및 동기화 공급
        itemTotalAmount.set(totalAmount);
        itemTotalOriginal.set(totalOriginal);
        itemTotalDiscount.set(totalDiscount);
        itemTotalCost.set(totalCost);
        itemTotalMargin.set(totalMargin);
        itemTotalQty.set(totalQty);
    }

    private void clearItemTotals() {
        itemTotalAmount.set(BigDecimal.ZERO);
        itemTotalOriginal.set(BigDecimal.ZERO);
        itemTotalDiscount.set(BigDecimal.ZERO);
        itemTotalCost.set(BigDecimal.ZERO);
        itemTotalMargin.set(BigDecimal.ZERO);
        itemTotalQty.set(0);
    }
    
    private void calculateTotals() {
        BigDecimal totalSales = salesList.stream().map(SaleModel::getSaleAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = salesList.stream().map(SaleModel::getCostAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = salesList.stream().map(SaleModel::getDiscountAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReceived = salesList.stream().map(SaleModel::getReceivedAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCash = salesList.stream().map(SaleModel::getCashAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = salesList.stream().map(SaleModel::getCreditAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCashout = salesList.stream().map(SaleModel::getCashoutAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalSalesAmount.set(totalSales);
        totalCostAmount.set(totalCost);
        totalDiscountAmount.set(totalDiscount);
        totalReceivedAmount.set(totalReceived);
        totalCashAmount.set(totalCash);
        totalCreditAmount.set(totalCredit);
        totalCashoutAmount.set(totalCashout);
        totalCount.set(salesList.size());
    }
    
    private SaleModel convertToModel(SaleDto dto) {
        SaleModel model = new SaleModel();
        if (dto.getId() != null && !dto.getId().isBlank()) {
            try { model.setId(Long.parseLong(dto.getId())); } catch (NumberFormatException e) { log.error("ID parse error", e); }
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
    
    // Getters
    public ObservableList<SaleModel> getSalesList() { return salesList; }
    public ObservableList<SaleItemModel> getSaleItemsList() { return saleItemsList; }
    public ObjectProperty<SaleModel> selectedSaleProperty() { return selectedSale; }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
    public BooleanProperty loadingProperty() { return loading; }
    
    public ObjectProperty<BigDecimal> totalSalesAmountProperty() { return totalSalesAmount; }
    public ObjectProperty<BigDecimal> totalCostAmountProperty() { return totalCostAmount; }         
    public ObjectProperty<BigDecimal> totalDiscountAmountProperty() { return totalDiscountAmount; }
    public ObjectProperty<BigDecimal> totalReceivedAmountProperty() { return totalReceivedAmount; } 
    public ObjectProperty<BigDecimal> totalCashAmountProperty() { return totalCashAmount; }         
    public ObjectProperty<BigDecimal> totalCreditAmountProperty() { return totalCreditAmount; }     
    public ObjectProperty<BigDecimal> totalCashoutAmountProperty() { return totalCashoutAmount; }
    public IntegerProperty totalCountProperty() { return totalCount; }
    public StringProperty errorMessageProperty() { return errorMessage; }

    // 💡 우측 아이템 전용 Getters 추가 공급
    public ObjectProperty<BigDecimal> itemTotalAmountProperty() { return itemTotalAmount; }
    public ObjectProperty<BigDecimal> itemTotalOriginalProperty() { return itemTotalOriginal; }
    public ObjectProperty<BigDecimal> itemTotalDiscountProperty() { return itemTotalDiscount; }
    public ObjectProperty<BigDecimal> itemTotalCostProperty() { return itemTotalCost; }
    public ObjectProperty<BigDecimal> itemTotalMarginProperty() { return itemTotalMargin; }
    public IntegerProperty itemTotalQtyProperty() { return itemTotalQty; }
}