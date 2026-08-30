package com.local.pos.ui.main;

import com.local.pos.ui.navigation.AppView;
import com.local.pos.ui.navigation.NavigationService;
import com.local.pos.ui.views.DashboardView;
import com.local.pos.ui.views.PlaceholderView;
import com.local.pos.ui.views.PosView;
import com.local.pos.ui.views.SettingsView;
import com.local.pos.ui.views.StockView;
import com.local.pos.config.AppContext;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;

public class MainShellView {

    private final BorderPane root = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final NavigationService navigationService;
    private final ToggleGroup navToggleGroup = new ToggleGroup();
    private final Map<AppView, ToggleButton> navButtons = new EnumMap<>(AppView.class);

    private Label pageTitle;
    private AppContext appContext;

    public MainShellView(AppContext appContext) {
        this.appContext = appContext;
        navigationService = new NavigationService(contentArea);

        root.getStyleClass().add("app-shell");
        contentArea.getStyleClass().add("content-area");

        registerPlaceholderViews();

        root.setLeft(buildSidebar());
        root.setTop(buildTopbar());
        root.setCenter(contentArea);

        navigationService.currentViewProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                pageTitle.setText(newValue.getTitle());
                selectNavButton(newValue);
            }
        });

        navigationService.navigateTo(AppView.DASHBOARD);

        if (appContext != null) {
            initializeWithData(appContext);
        }
    }

    private void registerPlaceholderViews() {
        navigationService.register(
            AppView.DASHBOARD,
            new DashboardView()
        );
        navigationService.register(
            AppView.STOCK,
            new PlaceholderView(
                "Stock",
                "Cargando módulo de stock..."
            )
        );
        navigationService.register(
            AppView.POS,
            new PlaceholderView(
                "Punto de venta",
                "Cargando módulo de punto de venta..."
            )
        );
        navigationService.register(
            AppView.REPORTS,
            new PlaceholderView(
                "Reportes",
                "El módulo de reportes se implementará más adelante."
            )
        );
        navigationService.register(
            AppView.SETTINGS,
            new PlaceholderView(
                "Configuración",
                "La configuración del sistema se implementará más adelante."
            )
        );
    }

    public void initializeWithData(AppContext context) {
        this.appContext = context;

        navigationService.register(
            AppView.STOCK,
            new StockView(
                appContext.getProductService(),
                appContext.getStockService()
            )
        );

        navigationService.register(
            AppView.POS,
            new PosView(
                appContext.getProductService(),
                appContext.getSaleService(),
                appContext.getSettingsService()
            )
        );

        navigationService.register(
            AppView.SETTINGS,
            new SettingsView(
                appContext.getSettingsService(),
                appContext.getBackupService(),
                appContext::shutdown
            )
        );

        if (navigationService.getCurrentView() == AppView.STOCK) {
            navigationService.navigateTo(AppView.STOCK);
        }
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

        Label subtitle = new Label("Gestión del local");
        subtitle.getStyleClass().add("brand-subtitle");

        brand.getChildren().addAll(title, subtitle);

        VBox nav = new VBox(10);
        nav.getStyleClass().add("nav");

        ToggleButton dashboardButton = createNavButton(AppView.DASHBOARD);
        ToggleButton stockButton = createNavButton(AppView.STOCK);
        ToggleButton sellButton = createNavButton(AppView.POS);
        ToggleButton reportsButton = createNavButton(AppView.REPORTS);
        ToggleButton settingsButton = createNavButton(AppView.SETTINGS);

        nav.getChildren().addAll(
            dashboardButton,
            stockButton,
            sellButton,
            reportsButton,
            settingsButton
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label version = new Label("v1.0.0");
        version.getStyleClass().add("sidebar-version");

        sidebar.getChildren().addAll(brand, nav, spacer, version);

        return sidebar;
    }

    private ToggleButton createNavButton(AppView view) {
        ToggleButton button = new ToggleButton(view.getLabel());
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setToggleGroup(navToggleGroup);
        button.setUserData(view);

        button.setOnAction(event -> navigationService.navigateTo(view));

        navButtons.put(view, button);

        return button;
    }

    private HBox buildTopbar() {
        HBox topbar = new HBox(12);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setPadding(new Insets(18, 28, 18, 28));

        pageTitle = new Label("Panel principal");
        pageTitle.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label("Sistema listo");
        status.getStyleClass().add("status-pill");

        topbar.getChildren().addAll(pageTitle, spacer, status);

        return topbar;
    }

    private void selectNavButton(AppView view) {
        ToggleButton button = navButtons.get(view);
        if (button != null) {
            button.setSelected(true);
        }
    }
}
