package com.swna.javafx.admin.sale.viewmodel;

import com.swna.javafx.admin.sale.api.SaleApiClient;
import com.swna.javafx.admin.sale.dto.SaleDto;
import com.swna.javafx.admin.sale.dto.SaleItemDto;
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
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 판매 화면 ViewModel
 * 제공된 일반 클래스 형태의 SaleDto 스펙에 맞춰 리팩토링 완료
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
    
    // 날짜 필터
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>(LocalDate.now());
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>(LocalDate.now());
    
    // 로딩 상태
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    
    // 요약 정보
    private final ObjectProperty<BigDecimal> totalSalesAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalDiscountAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalReceivedAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCashoutAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    
    // 에러 메시지
    private final StringProperty errorMessage = new SimpleStringProperty();
    
    // ==============================
    // Public Methods
    // ==============================
    
    /**
     * 기간별 판매 데이터 로드
     */
    public void loadSalesByDateRange() {
        if (startDate.get() == null || endDate.get() == null) {
            errorMessage.set("날짜를 선택해주세요.");
            return;
        }
        
        LocalDateTime startDateTime = startDate.get().atStartOfDay();
        LocalDateTime endDateTime = endDate.get().atTime(LocalTime.MAX);
        
        loading.set(true);
        salesList.clear();
        
        saleApiClient.getSalesByDateRange(startDateTime, endDateTime)
            .doOnTerminate(() -> Platform.runLater(() -> loading.set(false)))
            .subscribe(
                sales -> Platform.runLater(() -> {
                    updateSalesList(sales);
                    calculateSummary();
                }),
                error -> Platform.runLater(() -> {
                    log.error("Failed to load sales", error);
                    errorMessage.set("데이터 로드 실패: " + error.getMessage());
                })
            );
    }
    
    /**
     * 오늘 판매 데이터 로드
     */
    public void loadTodaySales() {
        startDate.set(LocalDate.now());
        endDate.set(LocalDate.now());
        loadSalesByDateRange();
    }
    
    /**
     * 이번주 판매 데이터 로드 (월요일 ~ 일요일)
     */
    public void loadThisWeekSales() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        startDate.set(monday);
        endDate.set(today);
        loadSalesByDateRange();
    }
    
    /**
     * 이번달 판매 데이터 로드
     */
    public void loadThisMonthSales() {
        LocalDate today = LocalDate.now();
        startDate.set(today.withDayOfMonth(1));
        endDate.set(today);
        loadSalesByDateRange();
    }
    
    /**
     * 선택된 판매 변경 시 처리
     */
    public void onSelectedSaleChanged(SaleModel sale) {
        selectedSale.set(sale);
        loadSaleItems(sale);
    }
    
    /**
     * 데이터 새로고침
     */
    public void refresh() {
        loadSalesByDateRange();
    }
    
    // ==============================
    // Private Methods
    // ==============================
    
    /**
     * SaleDto 목록을 Model 목록으로 변환
     */
    private void updateSalesList(List<SaleDto> sales) {
        List<SaleModel> models = sales.stream()
            .map(this::convertToModel)
            .toList();
        salesList.setAll(models);
    }
    
    /**
     * ✅ 수정 완료: 일반 클래스 형태의 SaleDto(Lombok Getter) 스펙 적용 및 데이터 바인딩 일치
     */
    private SaleModel convertToModel(SaleDto dto) {
        SaleModel model = new SaleModel();
        // 1. 기본 정보 설정
        model.setId(parseId(dto.getId()));
        model.setReceiptNo(dto.getReceiptNo());
        //model.setCashier(dto.getCashier());
        model.setPaymentDateTime(dto.getPaymentDateTime());
        
        // 2. 금액 정보 설정 (Null 방어 코드를 추가하면 더욱 안전합니다)
        model.setOriginalAmount(dto.getOriginalAmount() != null ? dto.getOriginalAmount() : BigDecimal.ZERO);
        model.setDiscountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO);
        model.setCreditAmount(dto.getCreditAmount() != null ? dto.getCreditAmount() : BigDecimal.ZERO);
        model.setSaleAmount(dto.getSaleAmount() != null ? dto.getSaleAmount() : BigDecimal.ZERO);
        model.setCashAmount(dto.getSaleAmount() != null ? dto.getCashAmount() : BigDecimal.ZERO);
        //model.setReceivedAmount(dto.getReceivedAmount() != null ? dto.getReceivedAmount() : BigDecimal.ZERO);
        //model.setChangeAmount(dto.getChangeAmount() != null ? dto.getChangeAmount() : BigDecimal.ZERO);
        
        // 💡 기존의 클라이언트 단 수동 연산 제거: 서버에서 넘어오는 정확한 cashoutAmount 매핑
        model.setCashoutAmount(dto.getCashoutAmount() != null ? dto.getCashoutAmount() : BigDecimal.ZERO);
        
        // 3. 결제 방식 및 상세 정보 설정
        model.setPaymentType(dto.getPaymentType());
        
        // 💡 이제 서버 응답 데이터(SaleDto)에 포함되어 있으므로 정상 바인딩 처리 가능
        model.setCardNumber(dto.getCardNumber() != null ? dto.getCardNumber() : "");
        model.setApprovalNo(""); // 필요시 수동 매핑 혹은 빈 값 유지
        
        return model;
    }
    
    /**
     * ID 파싱 (String -> Long)
     */
    private Long parseId(String saleId) {
        try {
            return Long.parseLong(saleId);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse saleId: {}", saleId);
            return 0L;
        }
    }
    
    /**
     * 결제 방식 매핑
     */
    private String mapPaymentType(String paymentMethod) {
        if (paymentMethod == null) return "UNKNOWN";
        // 복합 결제인 경우 (예: "CASH+CARD") 그대로 노출하거나 가공 처리
        return paymentMethod.toUpperCase();
    }
    
    /**
     * 판매 아이템 목록 로드 (판매 선택 시)
     */
    private void loadSaleItems(SaleModel sale) {
        saleItemsList.clear();
        if (sale == null) return;
        
        // TODO: 실제 API 호출로 변경
        // saleApiClient.getSaleItems(sale.getId())
        //     .subscribe(items -> Platform.runLater(() -> updateSaleItemsList(items)));
        
        // 임시 샘플 데이터
        saleItemsList.addAll(createSampleItems(sale));
    }
    
    /**
     * SaleItemDto(Record) 목록을 Model 목록으로 변환
     */
    private void updateSaleItemsList(List<SaleItemDto> items) {
        List<SaleItemModel> models = items.stream()
            .map(this::convertToItemModel)
            .collect(Collectors.toList());
        saleItemsList.setAll(models);
    }
    
    /**
     * SaleItemDto(Record)를 SaleItemModel로 변환
     */
    private SaleItemModel convertToItemModel(SaleItemDto dto) {
        return new SaleItemModel(
            dto.barcode(),
            dto.productName(),
            dto.quantity(),
            dto.unitPrice(),
            dto.discount(),
            dto.brand()  // supplier 대신 brand 사용
        );
    }
    
    /**
     * 통계 계산
     */
    private void calculateSummary() {
        BigDecimal totalSales = salesList.stream()
            .map(SaleModel::getSaleAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDiscount = salesList.stream()
            .map(SaleModel::getDiscountAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalReceived = salesList.stream()
            .map(SaleModel::getReceivedAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCashout = salesList.stream()
            .map(SaleModel::getCashoutAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalSalesAmount.set(totalSales);
        totalDiscountAmount.set(totalDiscount);
        totalReceivedAmount.set(totalReceived);
        totalCashoutAmount.set(totalCashout);
        totalCount.set(salesList.size());
    }
    
    /**
     * 샘플 아이템 생성 (임시 - API 연동 후 제거)
     */
    private List<SaleItemModel> createSampleItems(SaleModel sale) {
        return List.of(
            new SaleItemModel("8801234567890", "상품 A", 2, new BigDecimal("10000"), BigDecimal.ZERO, "공급사 A"),
            new SaleItemModel("8801234567891", "상품 B", 1, new BigDecimal("20000"), new BigDecimal("1000"), "공급사 B"),
            new SaleItemModel("8801234567892", "상품 C", 3, new BigDecimal("15000"), new BigDecimal("500"), "공급사 C")
        );
    }
    
    // ==============================
    // Getters for JavaFX Binding
    // ==============================
    
    public ObservableList<SaleModel> getSalesList() { 
        return salesList; 
    }
    
    public ObservableList<SaleItemModel> getSaleItemsList() { 
        return saleItemsList; 
    }
    
    public ObjectProperty<SaleModel> selectedSaleProperty() { 
        return selectedSale; 
    }
    
    public ObjectProperty<LocalDate> startDateProperty() { 
        return startDate; 
    }
    
    public ObjectProperty<LocalDate> endDateProperty() { 
        return endDate; 
    }
    
    public BooleanProperty loadingProperty() { 
        return loading; 
    }
    
    public ObjectProperty<BigDecimal> totalSalesAmountProperty() { 
        return totalSalesAmount; 
    }
    
    public ObjectProperty<BigDecimal> totalDiscountAmountProperty() { 
        return totalDiscountAmount; 
    }
    
    public ObjectProperty<BigDecimal> totalReceivedAmountProperty() { 
        return totalReceivedAmount; 
    }
    
    public ObjectProperty<BigDecimal> totalCashoutAmountProperty() { 
        return totalCashoutAmount; 
    }
    
    public IntegerProperty totalCountProperty() { 
        return totalCount; 
    }
    
    public StringProperty errorMessageProperty() { 
        return errorMessage; 
    }
}