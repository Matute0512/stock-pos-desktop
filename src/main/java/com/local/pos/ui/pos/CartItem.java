package com.local.pos.ui.pos;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CartItem {

    private final LongProperty productId = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> unitPrice = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> quantity = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> availableStock = new SimpleObjectProperty<>();

    public CartItem(long productId, String name, BigDecimal unitPrice,
                    BigDecimal quantity, BigDecimal availableStock) {
        this.productId.set(productId);
        this.name.set(name);
        this.unitPrice.set(unitPrice);
        this.quantity.set(quantity);
        this.availableStock.set(availableStock);
    }

    public long getProductId() {
        return productId.get();
    }

    public void setProductId(long productId) {
        this.productId.set(productId);
    }

    public LongProperty productIdProperty() {
        return productId;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice.get();
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice.set(unitPrice);
    }

    public ObjectProperty<BigDecimal> unitPriceProperty() {
        return unitPrice;
    }

    public BigDecimal getQuantity() {
        return quantity.get();
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity.set(quantity);
    }

    public ObjectProperty<BigDecimal> quantityProperty() {
        return quantity;
    }

    public BigDecimal getAvailableStock() {
        return availableStock.get();
    }

    public void setAvailableStock(BigDecimal availableStock) {
        this.availableStock.set(availableStock);
    }

    public ObjectProperty<BigDecimal> availableStockProperty() {
        return availableStock;
    }

    public BigDecimal lineTotal() {
        return unitPrice.get().multiply(quantity.get())
            .setScale(2, RoundingMode.HALF_UP);
    }
}
