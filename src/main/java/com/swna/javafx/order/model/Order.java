package com.swna.javafx.order.model;

import java.time.LocalDateTime;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Order {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty customerName = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> orderDate = new SimpleObjectProperty<>();

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public String getCustomerName() { return customerName.get(); }
    public void setCustomerName(String v) { customerName.set(v); }
    public StringProperty customerNameProperty() { return customerName; }

    public LocalDateTime getOrderDate() { return orderDate.get(); }
    public void setOrderDate(LocalDateTime v) { orderDate.set(v); }
    public ObjectProperty<LocalDateTime> orderDateProperty() { return orderDate; }
}