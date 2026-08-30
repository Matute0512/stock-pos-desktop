package com.local.pos.ui.navigation;

import com.local.pos.ui.views.PlaceholderView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.Map;

public class NavigationService {

    private final StackPane contentArea;
    private final Map<AppView, Node> views = new EnumMap<>(AppView.class);
    private final ObjectProperty<AppView> currentView =
        new SimpleObjectProperty<>(this, "currentView", AppView.DASHBOARD);

    public NavigationService(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void register(AppView view, Node node) {
        views.put(view, node);
    }

    public void navigateTo(AppView view) {
        Node node = views.get(view);

        if (node == null) {
            node = new PlaceholderView(
                "Pantalla no disponible",
                "Esta vista todavía no fue implementada."
            );
            views.put(view, node);
        }

        contentArea.getChildren().setAll(node);
        currentView.set(view);
    }

    public AppView getCurrentView() {
        return currentView.get();
    }

    public ObjectProperty<AppView> currentViewProperty() {
        return currentView;
    }
}
