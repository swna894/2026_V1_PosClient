package com.swna.javafx.admin.sale.viewmodel;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.api.SaleApiClient;
import com.swna.javafx.admin.sale.dto.SaleDto;
import com.swna.javafx.admin.sale.dto.SaleItemResponse;
import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 판매 화면 ViewModel
 *
 * 역할:
 * 1. 판매 목록 로딩
 * 2. 판매 상세 로딩
 * 3. Summary 계산
 * 4. 화면 상태 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesViewModel {

    private final SaleApiClient saleApiClient;

    // =========================================================
    // Observable Collections
    // =========================================================

    private final ObservableList<SaleModel> salesList = FXCollections.observableArrayList();
    private final ObservableList<SaleItemModel> saleItemsList = FXCollections.observableArrayList();

    // PrintReceiptDialog에서 사용하기 위한 FilteredList
    private final FilteredList<SaleModel> filteredSalesList = new FilteredList<>(salesList, 
        sale -> !"DELETED".equalsIgnoreCase(sale.getStatus())
    );

    // =========================================================
    // Selection
    // =========================================================

    private final ObjectProperty<SaleModel> selectedSale =  new SimpleObjectProperty<>();

    // =========================================================
    // Search Conditions
    // =========================================================

    private final ObjectProperty<LocalDate> startDate =  new SimpleObjectProperty<>(LocalDate.now());
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>(LocalDate.now());

    // =========================================================
    // UI State
    // =========================================================

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("Ready");

    // =========================================================
    // Main Summary
    // =========================================================

    private final ObjectProperty<BigDecimal> totalOriginalAmount =  new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalSalesAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalDiscountAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCostAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCashAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCreditAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalCashoutAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);

    // =========================================================
    // Selected Sale Item Summary
    // =========================================================

    private final ObjectProperty<BigDecimal> itemTotalAmount =  new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> itemTotalOriginal =  new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> itemTotalDiscount =  new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> itemTotalCost = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> itemTotalMargin = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty itemTotalQty =  new SimpleIntegerProperty(0);


    public FilteredList<SaleModel> getFilteredSalesList() {
        return filteredSalesList;
    }

    public void refreshFilteredList() {
        filteredSalesList.setPredicate(sale -> !"DELETED".equalsIgnoreCase(sale.getStatus()));
    }


    // =========================================================
    // Refresh
    // =========================================================

    public void refresh() {
        loadSalesByDateRange();
    }

    // =========================================================
    // Load Methods
    // =========================================================

    public void loadTodaySales() {

        LocalDate today = LocalDate.now();

        startDate.set(today);
        endDate.set(today);

        executeSalesLoad( saleApiClient.getTodaySales() );
    }

    public void loadThisWeekSales() {

        LocalDate now = LocalDate.now();

        LocalDate startOfWeek = now.with(DayOfWeek.MONDAY); // 이번 주의 월요일로 설정
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        startDate.set(startOfWeek);
        endDate.set(endOfWeek);

        executeSalesLoad(  saleApiClient.getThisWeekSales() );
    }

    public void loadThisMonthSales() {

        LocalDate now = LocalDate.now();

        startDate.set(now.withDayOfMonth(1));
        endDate.set(now.withDayOfMonth(now.lengthOfMonth()));

        executeSalesLoad(  saleApiClient.getThisMonthSales() );
    }

    public void loadSalesByDateRange() {

        if (startDate.get() == null || endDate.get() == null) {

            errorMessage.set("날짜를 선택해주세요.");
            return;
        }

        LocalDateTime start = startDate.get().atStartOfDay();
        LocalDateTime end = endDate.get().atTime(LocalTime.MAX);

        executeSalesLoad( saleApiClient.getSalesByDateRange(start, end) );
    }

    // =========================================================
    // Execute Sales Load
    // =========================================================

    private void executeSalesLoad(Mono<List<SaleDto>> salesMono) {

        loading.set(true);

        errorMessage.set("Loading...");

        salesMono
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(

                        sales -> Platform.runLater(() -> {

                            List<SaleModel> models = sales.stream() .map(this::convertToModel) .toList();

                            salesList.setAll(models);

                            calculateSalesTotals();

                            updateSelectedSale();

                            loading.set(false);

                            errorMessage.set("Ready");
                        }),

                        error -> Platform.runLater(() -> {

                            log.error("Failed to load sales", error);

                            clearAll();

                            errorMessage.set(
                                    "판매 데이터를 불러오지 못했습니다."
                            );

                            loading.set(false);
                        })
                );
    }

    // =========================================================
    // Load Sale Items
    // =========================================================

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

                            List<SaleItemModel> itemModels =
                                    items.stream()
                                            .map(this::convertToItemModel)
                                            .toList();

                            saleItemsList.setAll(itemModels);

                            calculateItemTotals();
                        }),

                        error -> Platform.runLater(() -> {

                            log.error(
                                    "Failed to load sale items",
                                    error
                            );

                            saleItemsList.clear();

                            clearItemTotals();

                            errorMessage.set(
                                    "상품 상세를 불러오지 못했습니다."
                            );
                        })
                );
    }

    public void onSelectedSaleChanged(SaleModel sale) {

        selectedSale.set(sale);

        if (sale == null) {

            saleItemsList.clear();

            clearItemTotals();

            return;
        }

        loadSaleItems(sale.getId());
    }

    // =========================================================
    // Main Summary Calculation
    // =========================================================

    private void calculateSalesTotals() {

        BigDecimal originalTotal = sumSales(SaleModel::getOriginalAmount);
        BigDecimal salesTotal =  sumSales(SaleModel::getSaleAmount);
        BigDecimal discountTotal = sumSales(SaleModel::getDiscountAmount);
        BigDecimal costTotal =  sumSales(SaleModel::getCostAmount);
        BigDecimal cashTotal = sumSales(SaleModel::getCashAmount);
        BigDecimal creditTotal = sumSales(SaleModel::getCreditAmount);
        BigDecimal cashoutTotal = sumSales(SaleModel::getCashoutAmount);

        totalOriginalAmount.set(originalTotal);
        totalSalesAmount.set(salesTotal);
        totalDiscountAmount.set(discountTotal);
        totalCostAmount.set(costTotal);
        totalCashAmount.set(cashTotal);
        totalCreditAmount.set(creditTotal);
        totalCashoutAmount.set(cashoutTotal);
        totalCount.set(salesList.size());
    }

    private BigDecimal sumSales(
            Function<SaleModel, BigDecimal> mapper
    ) {

        return salesList.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =========================================================
    // Item Summary Calculation
    // =========================================================

    private void calculateItemTotals() {

        BigDecimal totalAmount = sumItems(SaleItemModel::getSaleAmount);
        BigDecimal totalOriginal =  sumItems(SaleItemModel::getOriginalAmount);
        BigDecimal totalDiscount =  sumItems(SaleItemModel::getDiscountAmount);

        BigDecimal totalCost =
                saleItemsList.stream()
                        .map(item -> {

                            BigDecimal cost =
                                    item.costProperty().get();

                            BigDecimal qty =
                                    BigDecimal.valueOf(
                                            item.quantityProperty().get()
                                    );

                            if (cost == null) {
                                return BigDecimal.ZERO;
                            }

                            return cost.multiply(qty);
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMargin =
                totalAmount.subtract(totalCost);

        int qty =
                saleItemsList.stream()
                        .mapToInt(item ->
                                item.quantityProperty().get())
                        .sum();

        itemTotalAmount.set(totalAmount);
        itemTotalOriginal.set(totalOriginal);
        itemTotalDiscount.set(totalDiscount);
        itemTotalCost.set(totalCost);
        itemTotalMargin.set(totalMargin);
        itemTotalQty.set(qty);
    }

    private BigDecimal sumItems(
            Function<SaleItemModel, BigDecimal> mapper
    ) {

        return saleItemsList.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =========================================================
    // Selection
    // =========================================================

    private void updateSelectedSale() {

        if (salesList.isEmpty()) {
            selectedSale.set(null);
            saleItemsList.clear();
            clearItemTotals();
            return;
        }

        SaleModel firstSale = salesList.get(0);

        selectedSale.set(firstSale);

        loadSaleItems(firstSale.getId());
    }

    // =========================================================
    // Clear
    // =========================================================

    private void clearAll() {
        salesList.clear();
        saleItemsList.clear();
        selectedSale.set(null);
        clearSalesTotals();
        clearItemTotals();
    }

    private void clearSalesTotals() {

        totalOriginalAmount.set(BigDecimal.ZERO);
        totalSalesAmount.set(BigDecimal.ZERO);
        totalDiscountAmount.set(BigDecimal.ZERO);
        totalCostAmount.set(BigDecimal.ZERO);
        totalCashAmount.set(BigDecimal.ZERO);
        totalCreditAmount.set(BigDecimal.ZERO);
        totalCashoutAmount.set(BigDecimal.ZERO);
        totalCount.set(0);
    }

    private void clearItemTotals() {

        itemTotalAmount.set(BigDecimal.ZERO);
        itemTotalOriginal.set(BigDecimal.ZERO);
        itemTotalDiscount.set(BigDecimal.ZERO);
        itemTotalCost.set(BigDecimal.ZERO);
        itemTotalMargin.set(BigDecimal.ZERO);
        itemTotalQty.set(0);
    }

    // =========================================================
    // Convert Methods
    // =========================================================

    private SaleModel convertToModel(SaleDto dto) {

        SaleModel model = new SaleModel();

        if (dto.getId() != null && !dto.getId().isBlank()) {
            try {
                model.setId(Long.parseLong(dto.getId()));
            } catch (NumberFormatException e) {
                log.error("ID Parse Error", e);
            }
        }

        model.setReceiptNo(dto.getReceiptNo());
        model.setPaymentDateTime(dto.getPaymentDateTime());
        model.setOriginalAmount(dto.getOriginalAmount());
        model.setSaleAmount(dto.getSaleAmount());
        model.setDiscountAmount(dto.getDiscountAmount());
        model.setCostAmount(dto.getCostAmount());
        model.setCashAmount(dto.getCashAmount());
        model.setCreditAmount(dto.getCreditAmount());
        model.setCashoutAmount(dto.getCashoutAmount());
        model.setReceivedAmount(dto.getReceivedAmount());
        model.setChangeAmount(dto.getChangeAmount());
        model.setPaymentType(dto.getPaymentType());
        model.setCardNumber(dto.getCardNumber());
        model.setStatus(dto.getStatus());
        return model;
    }

    private SaleItemModel convertToItemModel( SaleItemResponse response ) {

        SaleItemModel model = new SaleItemModel();

        model.idProperty().set( response.id() != null ? response.id() : "");
        model.barcodeProperty().set( response.barcode() != null  ? response.barcode(): "" );
        model.descriptionProperty().set( response.description() != null ? response.description() : "");
        model.quantityProperty().set(response.quantity());

        BigDecimal salePrice = response.salePrice() != null ? response.salePrice(): BigDecimal.ZERO;
        BigDecimal discountPrice = response.discountPrice() != null ? response.discountPrice() : BigDecimal.ZERO;
        BigDecimal cost = response.cost() != null? response.cost() : BigDecimal.ZERO;

        model.salePriceProperty().set(salePrice);
        model.discountPriceProperty().set(discountPrice);
        model.originalPriceProperty().set(salePrice.add(discountPrice));
        model.costProperty().set(cost);
        
        BigDecimal qty = BigDecimal.valueOf(response.quantity());
        model.saleAmountProperty() .set(salePrice.multiply(qty));
        model.discountAmountProperty().set(discountPrice.multiply(qty));

        model.originalAmountProperty() .set( salePrice .add(discountPrice).multiply(qty));
        model.setComment( response.comment() != null ? response.comment()  : "" );

        return model;
    }

    // =========================================================
    // Getters
    // =========================================================

    public ObservableList<SaleModel> getSalesList() { return salesList; }
    public ObservableList<SaleItemModel> getSaleItemsList() { return saleItemsList; }
    public ObjectProperty<SaleModel> selectedSaleProperty() { return selectedSale; }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorMessageProperty() {  return errorMessage; }
    public IntegerProperty totalCountProperty() { return totalCount; }

    // =========================================================
    // Main Summary Getters
    // =========================================================

    public ObjectProperty<BigDecimal> totalOriginalAmountProperty() { return totalOriginalAmount; }
    public ObjectProperty<BigDecimal> totalSalesAmountProperty() { return totalSalesAmount; }
    public ObjectProperty<BigDecimal> totalDiscountAmountProperty() {  return totalDiscountAmount; }
    public ObjectProperty<BigDecimal> totalCostAmountProperty() {   return totalCostAmount; }
    public ObjectProperty<BigDecimal> totalCashAmountProperty() {  return totalCashAmount; }
    public ObjectProperty<BigDecimal> totalCreditAmountProperty() { return totalCreditAmount; }
    public ObjectProperty<BigDecimal> totalCashoutAmountProperty() { return totalCashoutAmount; }

    // =========================================================
    // Item Summary Getters
    // =========================================================

    public ObjectProperty<BigDecimal> itemTotalAmountProperty() { return itemTotalAmount; }
    public ObjectProperty<BigDecimal> itemTotalOriginalProperty() { return itemTotalOriginal; }
    public ObjectProperty<BigDecimal> itemTotalDiscountProperty() { return itemTotalDiscount; }
    public ObjectProperty<BigDecimal> itemTotalCostProperty() { return itemTotalCost;  }
    public ObjectProperty<BigDecimal> itemTotalMarginProperty() {  return itemTotalMargin; }
    public IntegerProperty itemTotalQtyProperty() { return itemTotalQty; }
}