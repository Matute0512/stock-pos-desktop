package com.local.pos.ui.navigation;

public enum AppView {

    DASHBOARD("Panel", "Panel principal"),
    STOCK("Stock", "Stock"),
    POS("Vender", "Punto de venta"),
    REPORTS("Reportes", "Reportes"),
    SETTINGS("Configuración", "Configuración");

    private final String label;
    private final String title;

    AppView(String label, String title) {
        this.label = label;
        this.title = title;
    }

    public String getLabel() {
        return label;
    }
    public String getTitle() {
        return title;
    }
}
