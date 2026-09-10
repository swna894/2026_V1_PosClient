package com.swna.javafx.admin.inventory.viewmodel;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.inventory.api.SalesTransactionApiClient;
import com.swna.javafx.admin.inventory.model.SalesTransactionDate;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesTransactionViewModel {

    private final SalesTransactionApiClient transactionApiClient;

    @Getter private final StringProperty titleInfo = new SimpleStringProperty("");
    @Getter private final ObjectProperty<Integer> selectedYear = new SimpleObjectProperty<>(Year.now().getValue());
    @Getter private final StringProperty selectedPeriod = new SimpleStringProperty("monthly");

    @Getter private final ObservableList<Integer> years = FXCollections.observableArrayList(
            IntStream.rangeClosed(Year.now(ZoneId.systemDefault()).getValue() - 5, Year.now(ZoneId.systemDefault()).getValue())
                    .boxed()
                    .toList()
    );
    @Getter private final ObservableList<String> periods = FXCollections.observableArrayList("weekly", "monthly", "quarterly");

    @Getter private final ObservableList<SalesTransactionDate> salesDataList = FXCollections.observableArrayList();
    @Getter private final XYChart.Series<String, Number> chartSeries = new XYChart.Series<>();

    private String currentBarcode;

    public void initializeData(String barcode, String title) {
        this.currentBarcode = barcode;
        this.titleInfo.set(title);
        chartSeries.setName("Quantity Sold");
        reload();
    }

    public void reload() {
        if (currentBarcode == null || currentBarcode.trim().isEmpty()) {
            log.warn("Cannot reload sales data: barcode is empty");
            return;
        }

        LocalDate startDate = LocalDate.of(selectedYear.get(), 1, 1);

        transactionApiClient.getSalesByPeriod(currentBarcode, selectedPeriod.get(), startDate)
                .subscribe(
                        responseList -> {
                            // ApiResponse 감싸기가 제거되었으므로 responseList를 직접 받아서 변환
                            List<SalesTransactionDate> list = (responseList != null)
                                    ? responseList.stream()
                                            .map(SalesTransactionDate::fromResponse)
                                            .toList()
                                    : List.of();

                            Platform.runLater(() -> {
                                salesDataList.setAll(list);

                                chartSeries.getData().clear();
                                for (SalesTransactionDate item : list) {
                                    chartSeries.getData().add(new XYChart.Data<>(item.getDate(), item.getCount()));
                                }
                            });
                        },
                        error -> {
                            log.error("[API Error] Failed to fetch product sales data for barcode: {}", currentBarcode, error);
                            Platform.runLater(() -> {
                                salesDataList.clear();
                                chartSeries.getData().clear();
                            });
                        }
                );
    }
}