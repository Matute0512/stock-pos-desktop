package com.local.pos.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PlaceholderView extends VBox {

    public PlaceholderView(String title, String text) {
        getStyleClass().add("view");
        setSpacing(18);
        setPadding(new Insets(28));

        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "placeholder-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("card-text");
        textLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, textLabel);

        getChildren().add(card);
    }
}
