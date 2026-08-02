package com.swna.javafx.admin.unpacking.model;

import java.time.LocalDateTime;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * 서버 Unpack 엔티티와 1:1로 필드를 맞춘 JavaFX 모델.
 * created/updated/unpacked/supplierAbbr/sync 를 추가로 반영했다.
 * selected 는 화면 체크박스용 UI 전용 상태로 서버로 전송되지 않는다.
 */
public class Unpack {
    private final LongProperty id = new SimpleLongProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty(); // UI 전용
    private final ObjectProperty<LocalDateTime> created = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updated = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> unpacked = new SimpleObjectProperty<>();
    private final StringProperty invoice = new SimpleStringProperty();
    private final StringProperty supplierAbbr = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty comment = new SimpleStringProperty();
    private final BooleanProperty sync = new SimpleBooleanProperty();
    private final ObservableList<UnpackItem> items = FXCollections.observableArrayList();

    public LongProperty idProperty() { return id; }
    public long getId() { return id.get(); }
    public void setId(long id) { this.id.set(id); }

    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }

    public ObjectProperty<LocalDateTime> createdProperty() { return created; }
    public LocalDateTime getCreated() { return created.get(); }
    public void setCreated(LocalDateTime created) { this.created.set(created); }

    public ObjectProperty<LocalDateTime> updatedProperty() { return updated; }
    public LocalDateTime getUpdated() { return updated.get(); }
    public void setUpdated(LocalDateTime updated) { this.updated.set(updated); }

    public ObjectProperty<LocalDateTime> unpackedProperty() { return unpacked; }
    public LocalDateTime getUnpacked() { return unpacked.get(); }
    public void setUnpacked(LocalDateTime unpacked) { this.unpacked.set(unpacked); }

    public StringProperty invoiceProperty() { return invoice; }
    public String getInvoice() { return invoice.get(); }
    public void setInvoice(String invoice) { this.invoice.set(invoice); }

    public StringProperty supplierAbbrProperty() { return supplierAbbr; }
    public String getSupplierAbbr() { return supplierAbbr.get(); }
    public void setSupplierAbbr(String supplierAbbr) { this.supplierAbbr.set(supplierAbbr); }

    public DoubleProperty amountProperty() { return amount; }
    public double getAmount() { return amount.get(); }
    public void setAmount(double amount) { this.amount.set(amount); }

    public StringProperty commentProperty() { return comment; }
    public String getComment() { return comment.get(); }
    public void setComment(String comment) { this.comment.set(comment); }

    public BooleanProperty syncProperty() { return sync; }
    public boolean getSync() { return sync.get(); }
    public void setSync(boolean sync) { this.sync.set(sync); }

    public ObservableList<UnpackItem> getItems() { return items; }
    public void setItems(java.util.List<UnpackItem> newItems) {
        items.setAll(newItems);
    }
}