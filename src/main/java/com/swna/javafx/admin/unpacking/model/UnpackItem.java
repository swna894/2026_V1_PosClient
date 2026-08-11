package com.swna.javafx.admin.unpacking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javafx.beans.property.*;
import lombok.ToString;

/**
 * 서버 UnpackItem 엔티티와 필드를 맞춘 JavaFX 모델.
 * 추가: invoice, stock, minOrderQty, amount, abbr, created, updated, unpackId
 * UI 전용(서버로 전송하지 않음): selected, lineNo, priceoutEstimated
 */
@ToString
public class UnpackItem {
    private final LongProperty id = new SimpleLongProperty();
    private final ObjectProperty<Long> unpackId = new SimpleObjectProperty<>(); // 상위 Unpack 연관관계
    private final ObjectProperty<LocalDateTime> created = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updated = new SimpleObjectProperty<>();

    private final StringProperty invoice = new SimpleStringProperty("");
    private final StringProperty barcode = new SimpleStringProperty("");
    private final StringProperty code = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty category = new SimpleStringProperty("");
    private final StringProperty abbr = new SimpleStringProperty("");
    private final IntegerProperty qty = new SimpleIntegerProperty(0);
    private final IntegerProperty stock = new SimpleIntegerProperty(0);
    private final IntegerProperty minOrderQty = new SimpleIntegerProperty(12);
    private final IntegerProperty minStock = new SimpleIntegerProperty(6);
    private final ObjectProperty<BigDecimal> amount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> pricein = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> oldPricein = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> priceout = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final BooleanProperty confirm = new SimpleBooleanProperty(false);
    private final BooleanProperty isSaved = new SimpleBooleanProperty(false);
    private final BooleanProperty isNew = new SimpleBooleanProperty(false);
    private final StringProperty supplier = new SimpleStringProperty("");
    private final StringProperty comment = new SimpleStringProperty("");

    // ---- UI 전용 (서버 미전송) ----
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final IntegerProperty lineNo = new SimpleIntegerProperty(0);
    private final ObjectProperty<BigDecimal> priceoutEstimated = new SimpleObjectProperty<>(BigDecimal.ZERO);

    public UnpackItem() {
        // JavaFX bean 규약을 위한 기본 생성자
    }

    public LongProperty idProperty() { return id; }
    public long getId() { return id.get(); }
    public void setId(long id) { this.id.set(id); }

    public ObjectProperty<Long> unpackIdProperty() { return unpackId; }
    public Long getUnpackId() { return unpackId.get(); }
    public void setUnpackId(Long value) { unpackId.set(value); }

    public ObjectProperty<LocalDateTime> createdProperty() { return created; }
    public LocalDateTime getCreated() { return created.get(); }
    public void setCreated(LocalDateTime value) { created.set(value); }

    public ObjectProperty<LocalDateTime> updatedProperty() { return updated; }
    public LocalDateTime getUpdated() { return updated.get(); }
    public void setUpdated(LocalDateTime value) { updated.set(value); }

    public StringProperty invoiceProperty() { return invoice; }
    public String getInvoice() { return invoice.get(); }
    public void setInvoice(String value) { invoice.set(value); }

    public StringProperty barcodeProperty() { return barcode; }
    public String getBarcode() { return barcode.get(); }
    public void setBarcode(String value) { barcode.set(value); }

    public StringProperty codeProperty() { return code; }
    public String getCode() { return code.get(); }
    public void setCode(String value) { code.set(value); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }

    public StringProperty categoryProperty() { return category; }
    public String getCategory() { return category.get(); }
    public void setCategory(String value) { category.set(value); }

    public StringProperty abbrProperty() { return abbr; }
    public String getAbbr() { return abbr.get(); }
    public void setAbbr(String value) { abbr.set(value); }

    public IntegerProperty qtyProperty() { return qty; }
    public int getQty() { return qty.get(); }
    public void setQty(int value) { qty.set(value); }

    public IntegerProperty stockProperty() { return stock; }
    public int getStock() { return stock.get(); }
    public void setStock(int value) { stock.set(value); }

    public IntegerProperty minOrderQtyProperty() { return minOrderQty; }
    public int getMinOrderQty() { return minOrderQty.get(); }
    public void setMinOrderQty(int value) { minOrderQty.set(value); }

    public IntegerProperty minStockProperty() { return minStock; }
    public int getMinStock() { return minStock.get(); }
    public void setMinStock(int value) { minStock.set(value); }

    public ObjectProperty<BigDecimal> amountProperty() { return amount; }
    public BigDecimal getAmount() { return amount.get(); }
    public void setAmount(BigDecimal value) { amount.set(value); }

    public ObjectProperty<BigDecimal> priceinProperty() { return pricein; }
    public BigDecimal getPricein() { return pricein.get(); }
    public void setPricein(BigDecimal value) { pricein.set(value); }

    public ObjectProperty<BigDecimal> oldPriceinProperty() { return oldPricein; }
    public BigDecimal getOldPricein() { return oldPricein.get(); }
    public void setOldPricein(BigDecimal value) { oldPricein.set(value); }

    public ObjectProperty<BigDecimal> priceoutProperty() { return priceout; }
    public BigDecimal getPriceout() { return priceout.get(); }
    public void setPriceout(BigDecimal value) { priceout.set(value); }

    public BooleanProperty confirmProperty() { return confirm; }
    public boolean getConfirm() { return confirm.get(); }
    public void setConfirm(boolean value) { confirm.set(value); }

    public BooleanProperty isSavedProperty() { return isSaved; }
    public boolean getIsSaved() { return isSaved.get(); }
    public void setIsSaved(boolean value) { isSaved.set(value); }

    public BooleanProperty isNewProperty() { return isNew; }
    public boolean getIsNew() { return isNew.get(); }
    public void setIsNew(boolean value) { isNew.set(value); }

    public StringProperty supplierProperty() { return supplier; }
    public String getSupplier() { return supplier.get(); }
    public void setSupplier(String value) { supplier.set(value); }

    public StringProperty commentProperty() { return comment; }
    public String getComment() { return comment.get(); }
    public void setComment(String value) { comment.set(value); }

    // ---- UI 전용 getter/setter ----
    public BooleanProperty selectedProperty() { return selected; }
    public boolean getSelected() { return selected.get(); }
    public void setSelected(boolean value) { selected.set(value); }

    public IntegerProperty lineNoProperty() { return lineNo; }
    public int getLineNo() { return lineNo.get(); }
    public void setLineNo(int value) { lineNo.set(value); }

    public ObjectProperty<BigDecimal> priceoutEstimatedProperty() { return priceoutEstimated; }
    public BigDecimal getPriceoutEstimated() { return priceoutEstimated.get(); }
    public void setPriceoutEstimated(BigDecimal value) { priceoutEstimated.set(value); }
}