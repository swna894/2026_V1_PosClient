package com.swna.javafx.admin.sale;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 판매 메인 컨트롤러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesController implements Initializable {
    
    private final SalesViewModel viewModel;
    
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button searchButton;
    @FXML private Button resetButton;
    @FXML private Button importButton;
    @FXML private Button excelButton;
    @FXML private Button reloadButton;
    @FXML private Button deleteButton;
    
    // Daily Summary Labels
    // @FXML private Label amountLabel;
    // @FXML private Label discountLabel;
    // @FXML private Label cashLabel;
    // @FXML private Label eftposLabel;
    // @FXML private Label cashoutLabel;
    
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBindings();
        setupDatePickers();
        setupButtonActions();
        
        // 초기 데이터 로드
        viewModel.loadTodaySales();
    }
    
    private void setupBindings() {
        // 날짜 바인딩
        startDatePicker.valueProperty().bindBidirectional(viewModel.startDateProperty());
        endDatePicker.valueProperty().bindBidirectional(viewModel.endDateProperty());
        
        
        // amountLabel.textProperty().bind(Bindings.createStringBinding(
        //     () -> String.format("%,.0f", viewModel.totalSalesAmountProperty().get()),
        //     viewModel.totalSalesAmountProperty()
        // ));
        
        // discountLabel.textProperty().bind(Bindings.createStringBinding(
        //     () -> String.format("%,.0f", viewModel.totalDiscountAmountProperty().get()),
        //     viewModel.totalDiscountAmountProperty()
        // ));
        
        // cashLabel.textProperty().bind(Bindings.createStringBinding(
        //     () -> {
        //         BigDecimal cashAmount = viewModel.totalReceivedAmountProperty().get()
        //             .subtract(viewModel.totalCashoutAmountProperty().get());
        //         return String.format("%,.0f", cashAmount);
        //     },
        //     viewModel.totalReceivedAmountProperty(),
        //     viewModel.totalCashoutAmountProperty()
        // ));
        
        // eftposLabel.textProperty().bind(Bindings.createStringBinding(
        //     () -> String.format("%,.0f", viewModel.totalReceivedAmountProperty().get()),
        //     viewModel.totalReceivedAmountProperty()
        // ));
        
        // cashoutLabel.textProperty().bind(Bindings.createStringBinding(
        //     () -> String.format("%,.0f", viewModel.totalCashoutAmountProperty().get()),
        //     viewModel.totalCashoutAmountProperty()
        // ));
    }
    
    private void setupDatePickers() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
    }
    
    private void setupButtonActions() {
        searchButton.setOnAction(e -> viewModel.loadSalesByDateRange());
        resetButton.setOnAction(e -> {
            startDatePicker.setValue(LocalDate.now());
            endDatePicker.setValue(LocalDate.now());
            viewModel.loadSalesByDateRange();
        });
        reloadButton.setOnAction(e -> viewModel.refresh());
        importButton.setOnAction(e -> handleImport());
        excelButton.setOnAction(e -> handleExportExcel());
        deleteButton.setOnAction(e -> handleDelete());
    }
    
    private void handleImport() {
        // TODO: 파일 import 구현
        log.info("Import button clicked");
    }
    
    private void handleExportExcel() {
        // TODO: Excel export 구현
        log.info("Excel export button clicked");
    }
    
    private void handleDelete() {
        // TODO: Delete 구현
        log.info("Delete button clicked");
    }
}