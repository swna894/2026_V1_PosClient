package com.swna.javafx.pos.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import com.swna.javafx.admin.sale.model.SaleItemModel;
import com.swna.javafx.admin.sale.model.SaleModel;

import lombok.Getter;

@Getter
public class ReceiptPrintEvent extends ApplicationEvent  {

   private static final long serialVersionUID = 1L;

    private final transient SaleModel saleModel;
    private final transient List<SaleItemModel> items;

    public ReceiptPrintEvent(Object source, SaleModel saleModel, List<SaleItemModel> itemsModels) {
        super(source);
        this.saleModel = saleModel;
        this.items = itemsModels;
    }
    // Getters...
}
