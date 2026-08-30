package com.local.pos.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DashboardView extends VBox {

    public DashboardView() {
        getStyleClass().add("view");
        setSpacing(18);
        setPadding(new Insets(28));

        Label sectionTitle = new Label("Resumen");
        sectionTitle.getStyleClass().add("section-title");

        HBox cards = new HBox(18);
        cards.getStyleClass().add("cards-row");

        cards.getChildren().addAll(
            createCard("Productos", "Cargar, editar y consultar stock."),
            createCard("Ventas", "Registrar ventas del día."),
            createCard("Bajo stock", "Ver productos a reponer.")
        );

        VBox welcome = createCardLarge(
            "Bienvenida",
            "Este es el panel inicial. En los próximos pasos vamos a conectar las pantallas de Stock y Punto de Venta con la base de datos."
        );

        getChildren().addAll(sectionTitle, cards, welcome);
    }

    private VBox createCard(String title, String text) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("card-text");
        textLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, textLabel);

        return card;
    }

    private VBox createCardLarge(String title, String text) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "card-large");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("card-text");
        textLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, textLabel);

        return card;
    }
}
