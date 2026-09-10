package com.swna.javafx.admin.inventory;

import com.swna.javafx.admin.inventory.factory.SalesTransactionTableFactory;
import com.swna.javafx.admin.inventory.model.Inventory;
import com.swna.javafx.admin.inventory.model.SalesTransactionDate;
import com.swna.javafx.admin.inventory.viewmodel.SalesTransactionViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/sales-transaction-view.fxml")
public class SalesTransactionController {

    private final SalesTransactionViewModel viewModel;
    private final SalesTransactionTableFactory tableFactory;

    @FXML private Label labelTitle;
    @FXML private ComboBox<Integer> comboBoxYear;
    @FXML private ComboBox<String> comboBoxPeriod;
    @FXML private Button buttonReload;

    // LineChart
    @FXML private LineChart<String, Number> lineChartSales;
    @FXML private CategoryAxis xAxisDate;
    @FXML private NumberAxis yAxisQty;

    // TableView
    @FXML private TableView<SalesTransactionDate> tableView;

    @FXML
    public void initialize() {
        initComboBoxes();
        initTableView();
        initChart();
        bindProperties();
    }

    public void setInventory(Inventory inventory) {
        if (inventory != null) {
            String displayTitle = inventory.getCode() + " : " + inventory.getDescription();
            viewModel.initializeData(inventory.getBarcode(), displayTitle);
            updateAxisLabels();
        }
    }

    private void initComboBoxes() {
        comboBoxYear.setItems(viewModel.getYears());
        comboBoxYear.valueProperty().bindBidirectional(viewModel.getSelectedYear());

        comboBoxPeriod.setItems(viewModel.getPeriods());
        comboBoxPeriod.valueProperty().bindBidirectional(viewModel.getSelectedPeriod());

        comboBoxYear.setOnAction(e -> handleReload());
        comboBoxPeriod.setOnAction(e -> handleReload());
    }

    private void initTableView() {
        // TableFactory를 사용하여 테이블 구성 설정
        tableFactory.initializeTable(tableView, viewModel);
    }

    private void initChart() {
        lineChartSales.getData().clear();
        viewModel.getChartSeries().setName("Quantity Sold");
        lineChartSales.getData().add(viewModel.getChartSeries());
    }

    private void bindProperties() {
        labelTitle.textProperty().bind(viewModel.getTitleInfo());
    }

    @FXML
    void actionButtonReload(ActionEvent event) {
        handleReload();
    }

    private void handleReload() {
        viewModel.reload();
        updateAxisLabels();
    }

    private void updateAxisLabels() {
        String period = viewModel.getSelectedPeriod().get();
        if (period == null) return;

        switch (period.toLowerCase()) {
            case "weekly" -> {
                xAxisDate.setLabel("Week");
                yAxisQty.setLabel("Weekly Qty Sold");
            }
            case "quarterly" -> {
                xAxisDate.setLabel("Quarter");
                yAxisQty.setLabel("Quarterly Qty Sold");
            }
            default -> {
                xAxisDate.setLabel("Month");
                yAxisQty.setLabel("Monthly Qty Sold");
            }
        }
    }

    public SalesTransactionViewModel getViewModel() {
        return this.viewModel;
    }
}