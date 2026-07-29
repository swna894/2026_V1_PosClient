package com.swna.javafx.admin.unpacking.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class InspectionItem {
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final DoubleProperty oldPricein = new SimpleDoubleProperty();
    private final DoubleProperty pricein = new SimpleDoubleProperty();
    private final IntegerProperty qty = new SimpleIntegerProperty();
    private final IntegerProperty minStock = new SimpleIntegerProperty();
    private final DoubleProperty priceoutEstimated = new SimpleDoubleProperty();
    private final DoubleProperty priceout = new SimpleDoubleProperty();
    private final BooleanProperty confirm = new SimpleBooleanProperty();
    private final BooleanProperty isSaved = new SimpleBooleanProperty();
    private final BooleanProperty isNew = new SimpleBooleanProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty();
    private final StringProperty supplier = new SimpleStringProperty();
    private final StringProperty comment = new SimpleStringProperty();
    private final IntegerProperty lineNo = new SimpleIntegerProperty();

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

    public DoubleProperty oldPriceinProperty() { return oldPricein; }
    public DoubleProperty priceinProperty() { return pricein; }
    public IntegerProperty qtyProperty() { return qty; }
    public IntegerProperty minStockProperty() { return minStock; }
    public DoubleProperty priceoutEstimatedProperty() { return priceoutEstimated; }
    public DoubleProperty priceoutProperty() { return priceout; }

    public BooleanProperty confirmProperty() { return confirm; }
    public boolean getConfirm() { return confirm.get(); }
    public void setConfirm(boolean value) { confirm.set(value); }

    public BooleanProperty isSavedProperty() { return isSaved; }
    public boolean getIsSaved() { return isSaved.get(); }
    public void setIsSaved(boolean value) { isSaved.set(value); }

    public BooleanProperty isNewProperty() { return isNew; }
    public Boolean getIsNew() { return isNew.get(); }
    public void setIsNew(Boolean value) { isNew.set(value); }

    public BooleanProperty selectedProperty() { return selected; }
    public void setSelected(boolean value) { selected.set(value); }

    public StringProperty supplierProperty() { return supplier; }
    public String getSupplier() { return supplier.get(); }
    public void setSupplier(String value) { supplier.set(value); }

    public StringProperty commentProperty() { return comment; }
    public IntegerProperty lineNoProperty() { return lineNo; }
    public int getLineNo() { return lineNo.get(); }
    public int getQty() { return qty.get(); }
}
