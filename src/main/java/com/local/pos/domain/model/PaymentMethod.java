package com.local.pos.domain.model;

public enum PaymentMethod {

    CASH("Efectivo"),
    CARD("Tarjeta"),
    TRANSFER("Transferencia"),
    OTHER("Otro");

    private final String label;

    PaymentMethod(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
