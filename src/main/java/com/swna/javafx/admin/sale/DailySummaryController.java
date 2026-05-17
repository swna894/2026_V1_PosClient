package com.swna.javafx.admin.sale;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.text.NumberFormat;

public class DailySummaryController {

    @FXML private Label dateLabel;      // 날짜 표시 Label (GridPane 0,1)
    @FXML private Label amountLabel;    // AMOUNT (0,3)
    @FXML private Label costLabel;      // COST (0,5)
    @FXML private Label dcLabel;        // D/C (0,8)
    @FXML private Label cashLabel;      // CASH (0,10)
    @FXML private Label eftposLabel;    // Eftpos (0,12)
    @FXML private Label cashoutLabel;   // CASH OUT (0,14)

    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

    public void updateSummary(double totalAmount, double totalCash, double totalEftpos,
                              double totalCashout, double totalDC) {
        amountLabel.setText(currencyFormat.format(totalAmount));
        cashLabel.setText(currencyFormat.format(totalCash));
        eftposLabel.setText(currencyFormat.format(totalEftpos));
        cashoutLabel.setText(currencyFormat.format(totalCashout));
        dcLabel.setText(currencyFormat.format(totalDC));
    }

    public void setDateRangeText(String startDate, String endDate) {
        dateLabel.setText(startDate + " ~ " + endDate);
    }
}
