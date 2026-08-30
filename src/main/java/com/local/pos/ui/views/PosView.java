package com.local.pos.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PosView extends VBox {

    public PosView() {
        getStyleClass().add("view");
        setSpacing(18);
        setPadding(new Insets(28));

        VBox header = createHeader(
            "Punto de venta",
            "Buscar productos, armar carrito y cobrar."
        );

        HBox searchCard = createSearchCard();

        HBox layout = new HBox(18);
        VBox.setVgrow(layout, Priority.ALWAYS);

        VBox productsPanel = createProductsPanel();
        HBox.setHgrow(productsPanel, Priority.ALWAYS);

        VBox cartPanel = createCartPanel();
        cartPanel.setPrefWidth(360);
        cartPanel.setMinWidth(320);

        layout.getChildren().addAll(productsPanel, cartPanel);

        getChildren().addAll(header, searchCard, layout);
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

    private HBox createSearchCard() {
        HBox searchCard = new HBox(12);
        searchCard.getStyleClass().add("toolbar-card");
        searchCard.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Escaneá un código de barras o buscá un producto...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button addButton = new Button("Agregar");
        addButton.getStyleClass().add("button-primary");

        searchCard.getChildren().addAll(searchField, addButton);

        return searchCard;
    }

    private VBox createProductsPanel() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "placeholder-card");

        Label titleLabel = new Label("Productos");
        titleLabel.getStyleClass().add("card-title");

        Label textLabel = new Label(
            "Acá se van a mostrar los productos disponibles para la venta."
        );
        textLabel.getStyleClass().add("card-text");
        textLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, textLabel);

        return card;
    }

    private VBox createCartPanel() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "placeholder-card", "pos-cart-card");

        Label titleLabel = new Label("Carrito");
        titleLabel.getStyleClass().add("card-title");

        Label textLabel = new Label(
            "Todavía no hay productos en el carrito."
        );
        textLabel.getStyleClass().add("card-text");
        textLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, textLabel);

        return card;
    }
}
