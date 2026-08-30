package com.local.pos.domain.model;

public enum MovementType {

    INITIAL("Carga inicial"),
    PURCHASE("Ingreso / Compra"),
    SALE("Venta"),
    RETURN("Devolución"),
    ADJUST_IN("Ajuste de ingreso"),
    ADJUST_OUT("Ajuste de egreso");

    private final String label;

    MovementType(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
