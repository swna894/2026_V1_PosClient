package com.swna.javafx.admin.unpacking.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Inspection {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty invoice = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty comment = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty();

    public LongProperty idProperty() { return id; }
    public long getId() { return id.get(); }
    public void setId(long id) { this.id.set(id); }

    public StringProperty invoiceProperty() { return invoice; }
    public String getInvoice() { return invoice.get(); }
    public void setInvoice(String invoice) { this.invoice.set(invoice); }

    public DoubleProperty amountProperty() { return amount; }
    public double getAmount() { return amount.get(); }
    public void setAmount(double amount) { this.amount.set(amount); }

    public StringProperty commentProperty() { return comment; }
    public String getComment() { return comment.get(); }
    public void setComment(String comment) { this.comment.set(comment); }

    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
}
