package com.local.pos.ui.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MainShellView {

    private final BorderPane root = new BorderPane();

    public MainShellView() {
        root.getStyleClass().add("app-shell");
        root.setLeft(buildSidebar());
        root.setTop(buildTopbar());
        root.setCenter(buildDashboardPlaceholder());
    }

    public BorderPane getRoot() {
        return root;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(18);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setPrefWidth(240);

        VBox brand = new VBox(4);

        Label title = new Label("Stock POS");
        title.getStyleClass().add("brand-title");

        Label subTitle = new Label("Gestión del local");
        subTitle.getStyleClass().add("brand-subtitle");

        brand.getChildren().addAll(title, subTitle);

        VBox nav = new VBox(10);
        nav.getStyleClass().add("nav");

        Button stockButton = createNavButton("Stock", false);
        Button sellButton = createNavButton("Vender", true);
        Button reportsButton = createNavButton("Reportes", false);
        Button settingsButton = createNavButton("Configuración", false);

        nav.getChildren().addAll(stockButton, sellButton, reportsButton, settingsButton);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label version = new Label("v1.0.0");
        version.getStyleClass().add("sidebar-version");

        sidebar.getChildren().addAll(brand, nav, spacer, version);

        return sidebar;
    }

    private Button createNavButton(String text, boolean active) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);

        if (active) {
            button.getStyleClass().add("nav-button-active");
        }

        return button;
    }

    private HBox buildTopbar() {
        HBox topbar = new HBox(12);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setPadding(new Insets(18, 28, 18, 28));

        Label pageTitle = new Label("Panel principal");
        pageTitle.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label("Sistema listo");
        status.getStyleClass().add("status-pill");

        topbar.getChildren().addAll(pageTitle, spacer, status);

        return topbar;
    }

    private VBox buildDashboardPlaceholder() {
        VBox content = new VBox(18);
        content.getStyleClass().add("content");
        content.setPadding(new Insets(28, 28, 28, 28));

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
            "Este es el esqueleto inicial de la aplicación. En los próximos pasos vamos a reemplazar esta pantalla por los módulos de Stock y Punto de Venta."
        );

        content.getChildren().addAll(sectionTitle, cards, welcome);

        return content;
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
