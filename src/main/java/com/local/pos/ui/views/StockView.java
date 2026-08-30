package com.local.pos.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StockView extends VBox {

    public StockView() {
        getStyleClass().add("view");
        setSpacing(18);
        setPadding(new Insets(28));

        VBox header = createHeader(
            "Stock",
            "Administrar productos, precios y existencias."
        );

        HBox toolbar = createToolbar();

        VBox tablePlaceholder = createTablePlaceholder();
        VBox.setVgrow(tablePlaceholder, Priority.ALWAYS);

        getChildren().addAll(header, toolbar, tablePlaceholder);
    }

    private VBox createHeader(String title, String subtitle) {
        VBox header = new VBox(4);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("view-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("view-subtitle");

        header.getChildren().addAll(titleLabel, subtitleLabel);

        return header;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("toolbar-card");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar por nombre, SKU o código de barras...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button newProductButton = new Button("Nuevo producto");
        newProductButton.getStyleClass().add("button-primary");

        Button editButton = new Button("Editar");
        editButton.getStyleClass().add("button-secondary");

        Button adjustStockButton = new Button("Ajustar stock");
        adjustStockButton.getStyleClass().add("button-secondary");

        toolbar.getChildren().addAll(
            searchField,
            newProductButton,
            editButton,
            adjustStockButton
        );

        return toolbar;
    }

    private VBox createTablePlaceholder() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "table-placeholder-card");

        Label titleLabel = new Label("Listado de productos");
        titleLabel.getStyleClass().add("card-title");

        Label textLabel = new Label(
            "En el próximo paso vamos a conectar esta pantalla con la base de datos para mostrar los productos."
        );
        textLabel.getStyleClass().add("card-text");
        textLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, textLabel);

        return card;
    }
}
