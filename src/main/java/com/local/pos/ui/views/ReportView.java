package com.local.pos.ui.views;

import com.local.pos.core.AlertUtil;
import com.local.pos.domain.model.LowStockProduct;
import com.local.pos.domain.model.SalesSummary;
import com.local.pos.domain.model.StockValuation;
import com.local.pos.domain.model.TopSellingProduct;
import com.local.pos.domain.service.ReportService;
import com.local.pos.util.CsvExporter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportView extends VBox {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportService reportService;

    private final ObservableList<SalesSummary> summaryData = FXCollections.observableArrayList();
    private final ObservableList<TopSellingProduct> topData = FXCollections.observableArrayList();
    private final ObservableList<StockValuation> valuationData = FXCollections.observableArrayList();
    private final ObservableList<LowStockProduct> lowStockData = FXCollections.observableArrayList();

    private final DatePicker fromPicker = new DatePicker(LocalDate.now().minusDays(6));
    private final DatePicker toPicker = new DatePicker(LocalDate.now());
    private final ComboBox<Integer> limitCombo = new ComboBox<>();
    private final Button exportTopButton = new Button("Exportar CSV");
    private final Label valuationSummaryLabel = new Label();

    public ReportView(ReportService reportService) {
        this.reportService = reportService;

        getStyleClass().add("view");
        setSpacing(18);
        setPadding(new Insets(28));

        VBox header = createHeader("Reportes", "Resumen del día, ranking y stock.");

        VBox salesCard = createSalesCard();
        VBox topCard = createTopCard();
        VBox valuationCard = createValuationCard();
        VBox lowStockCard = createLowStockCard();

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        col.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col, col);

        RowConstraints row = new RowConstraints();
        row.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(row, row);

        grid.add(salesCard, 0, 0);
        grid.add(topCard, 1, 0);
        grid.add(valuationCard, 0, 1);
        grid.add(lowStockCard, 1, 1);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("report-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, scroll);

        loadAll();
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

    private void loadAll() {
        try {
            summaryData.setAll(reportService.getSalesSummary(LocalDate.now()));
            valuationData.setAll(reportService.getStockValuation());
            lowStockData.setAll(reportService.getLowStockProducts());
            updateValuationSummary();
        } catch (Exception e) {
            AlertUtil.error("Error", "No se pudieron cargar los reportes.");
        }
    }

    private VBox createSalesCard() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "table-card");

        Label title = new Label("Ventas del día");
        title.getStyleClass().add("card-title");

        Label dateLabel = new Label("Fecha: " + LocalDate.now().format(DATE_FMT));
        dateLabel.getStyleClass().add("form-info");

        TableView<SalesSummary> table = new TableView<>();
        table.getStyleClass().add("modern-table");
        table.setItems(summaryData);
        table.setPlaceholder(new Label("Sin datos."));

        TableColumn<SalesSummary, Integer> countCol = new TableColumn<>("Ventas");
        countCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().totalSales()));
        countCol.setPrefWidth(70);

        TableColumn<SalesSummary, BigDecimal> cashCol = new TableColumn<>("Efectivo");
        cashCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().cashTotal()));
        cashCol.setCellFactory(col -> moneyCell());
        cashCol.setPrefWidth(110);

        TableColumn<SalesSummary, BigDecimal> cardCol = new TableColumn<>("Tarjeta");
        cardCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().cardTotal()));
        cardCol.setCellFactory(col -> moneyCell());
        cardCol.setPrefWidth(110);

        TableColumn<SalesSummary, BigDecimal> otherCol = new TableColumn<>("Otros");
        otherCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().otherTotal()));
        otherCol.setCellFactory(col -> moneyCell());
        otherCol.setPrefWidth(100);

        TableColumn<SalesSummary, BigDecimal> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().grandTotal()));
        totalCol.setCellFactory(col -> moneyCell());
        totalCol.setPrefWidth(110);

        table.getColumns().addAll(countCol, cashCol, cardCol, otherCol, totalCol);
        table.setPrefHeight(150);

        Button exportButton = new Button("Exportar CSV");
        exportButton.getStyleClass().add("button-secondary");
        exportButton.setOnAction(event -> {
            Path file = askCsvFile("ventas_del_dia.csv");
            if (file != null) {
                try {
                    CsvExporter.exportSales(summaryData, file);
                    AlertUtil.info("Exportar", "CSV exportado a:\n" + file.toAbsolutePath());
                } catch (Exception e) {
                    AlertUtil.error("Exportar", "No se pudo exportar: " + e.getMessage());
                }
            }
        });

        card.getChildren().addAll(title, dateLabel, table, exportButton);

        return card;
    }

    private VBox createTopCard() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "table-card");

        Label title = new Label("Productos más vendidos");
        title.getStyleClass().add("card-title");

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        Label fromLabel = new Label("Desde");
        Label toLabel = new Label("Hasta");
        Label limitLabel = new Label("Límite");
        for (Label label : List.of(fromLabel, toLabel, limitLabel)) {
            label.getStyleClass().add("form-label");
        }

        fromPicker.setPrefWidth(125);
        toPicker.setPrefWidth(125);
        limitCombo.setItems(FXCollections.observableArrayList(5, 10, 20, 50));
        limitCombo.setValue(5);
        limitCombo.setPrefWidth(70);

        Button queryButton = new Button("Consultar");
        queryButton.getStyleClass().add("button-secondary");
        queryButton.setOnAction(event -> queryTop());

        filters.getChildren().addAll(fromLabel, fromPicker, toLabel, toPicker,
            limitLabel, limitCombo, queryButton);

        TableView<TopSellingProduct> table = new TableView<>();
        table.getStyleClass().add("modern-table");
        table.setItems(topData);
        table.setPlaceholder(new Label("Sin datos. Consultá un rango de fechas."));

        TableColumn<TopSellingProduct, String> nameCol = new TableColumn<>("Producto");
        nameCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().productName()));
        nameCol.setPrefWidth(200);

        TableColumn<TopSellingProduct, BigDecimal> qtyCol = new TableColumn<>("Cantidad vendida");
        qtyCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().quantitySold()));
        qtyCol.setCellFactory(col -> qtyCell());
        qtyCol.setPrefWidth(130);

        TableColumn<TopSellingProduct, BigDecimal> revenueCol = new TableColumn<>("Total vendido");
        revenueCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().totalRevenue()));
        revenueCol.setCellFactory(col -> moneyCell());
        revenueCol.setPrefWidth(130);

        table.getColumns().addAll(nameCol, qtyCol, revenueCol);
        table.setPrefHeight(150);

        exportTopButton.getStyleClass().add("button-secondary");
        exportTopButton.setDisable(true);
        exportTopButton.setOnAction(event -> {
            Path file = askCsvFile("mas_vendidos.csv");
            if (file != null) {
                try {
                    CsvExporter.exportTopSelling(topData, file);
                    AlertUtil.info("Exportar", "CSV exportado a:\n" + file.toAbsolutePath());
                } catch (Exception e) {
                    AlertUtil.error("Exportar", "No se pudo exportar: " + e.getMessage());
                }
            }
        });

        card.getChildren().addAll(title, filters, table, exportTopButton);

        return card;
    }

    private VBox createValuationCard() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "table-card");

        Label title = new Label("Valor de stock");
        title.getStyleClass().add("card-title");

        TableView<StockValuation> table = new TableView<>();
        table.getStyleClass().add("modern-table");
        table.setItems(valuationData);
        table.setPlaceholder(new Label("Sin productos activos."));

        TableColumn<StockValuation, String> nameCol = new TableColumn<>("Producto");
        nameCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().productName()));
        nameCol.setPrefWidth(160);

        TableColumn<StockValuation, BigDecimal> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().stockQty()));
        stockCol.setCellFactory(col -> qtyCell());
        stockCol.setPrefWidth(65);

        TableColumn<StockValuation, BigDecimal> costCol = new TableColumn<>("Costo");
        costCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().cost()));
        costCol.setCellFactory(col -> moneyCell());
        costCol.setPrefWidth(85);

        TableColumn<StockValuation, BigDecimal> priceCol = new TableColumn<>("Precio");
        priceCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().price()));
        priceCol.setCellFactory(col -> moneyCell());
        priceCol.setPrefWidth(85);

        TableColumn<StockValuation, BigDecimal> costValueCol = new TableColumn<>("Valor a costo");
        costValueCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().valueAtCost()));
        costValueCol.setCellFactory(col -> moneyCell());
        costValueCol.setPrefWidth(100);

        TableColumn<StockValuation, BigDecimal> priceValueCol = new TableColumn<>("Valor a precio");
        priceValueCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().valueAtPrice()));
        priceValueCol.setCellFactory(col -> moneyCell());
        priceValueCol.setPrefWidth(100);

        table.getColumns().addAll(nameCol, stockCol, costCol, priceCol, costValueCol, priceValueCol);
        table.setPrefHeight(200);

        valuationSummaryLabel.getStyleClass().add("form-info");
        valuationSummaryLabel.setWrapText(true);

        Button exportButton = new Button("Exportar CSV");
        exportButton.getStyleClass().add("button-secondary");
        exportButton.setOnAction(event -> {
            Path file = askCsvFile("valor_de_stock.csv");
            if (file != null) {
                try {
                    CsvExporter.exportStockValuation(valuationData, file);
                    AlertUtil.info("Exportar", "CSV exportado a:\n" + file.toAbsolutePath());
                } catch (Exception e) {
                    AlertUtil.error("Exportar", "No se pudo exportar: " + e.getMessage());
                }
            }
        });

        card.getChildren().addAll(title, table, valuationSummaryLabel, exportButton);

        return card;
    }

    private VBox createLowStockCard() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "table-card");

        Label title = new Label("Productos bajo stock mínimo");
        title.getStyleClass().add("card-title");

        TableView<LowStockProduct> table = new TableView<>();
        table.getStyleClass().add("modern-table");
        table.setItems(lowStockData);
        table.setPlaceholder(new Label("No hay productos bajo el mínimo."));

        TableColumn<LowStockProduct, String> nameCol = new TableColumn<>("Producto");
        nameCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().productName()));
        nameCol.setPrefWidth(200);

        TableColumn<LowStockProduct, BigDecimal> stockCol = new TableColumn<>("Stock actual");
        stockCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().stockQty()));
        stockCol.setCellFactory(col -> qtyCell());
        stockCol.setPrefWidth(110);

        TableColumn<LowStockProduct, BigDecimal> minCol = new TableColumn<>("Stock mínimo");
        minCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().minStockQty()));
        minCol.setCellFactory(col -> qtyCell());
        minCol.setPrefWidth(110);

        TableColumn<LowStockProduct, BigDecimal> diffCol = new TableColumn<>("Diferencia");
        diffCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
            cd.getValue().minStockQty().subtract(cd.getValue().stockQty())));
        diffCol.setCellFactory(col -> qtyCell());
        diffCol.setPrefWidth(100);

        table.getColumns().addAll(nameCol, stockCol, minCol, diffCol);
        table.setPrefHeight(200);

        Button exportButton = new Button("Exportar CSV");
        exportButton.getStyleClass().add("button-secondary");
        exportButton.setOnAction(event -> {
            Path file = askCsvFile("bajo_stock.csv");
            if (file != null) {
                try {
                    CsvExporter.exportLowStock(lowStockData, file);
                    AlertUtil.info("Exportar", "CSV exportado a:\n" + file.toAbsolutePath());
                } catch (Exception e) {
                    AlertUtil.error("Exportar", "No se pudo exportar: " + e.getMessage());
                }
            }
        });

        card.getChildren().addAll(title, table, exportButton);

        return card;
    }

    private void queryTop() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        int limit = limitCombo.getValue() == null ? 5 : limitCombo.getValue();

        if (from == null || to == null || from.isAfter(to)) {
            AlertUtil.error("Fechas inválidas", "Seleccioná un rango de fechas válido.");
            return;
        }

        try {
            topData.setAll(reportService.getTopSellingProducts(from, to, limit));
            exportTopButton.setDisable(topData.isEmpty());
        } catch (Exception e) {
            AlertUtil.error("Error", "No se pudo consultar el ranking.");
        }
    }

    private void updateValuationSummary() {
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (StockValuation valuation : valuationData) {
            totalCost = totalCost.add(valuation.valueAtCost());
            totalPrice = totalPrice.add(valuation.valueAtPrice());
        }

        BigDecimal cost = totalCost.setScale(2, RoundingMode.HALF_UP);
        BigDecimal price = totalPrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal gain = price.subtract(cost).setScale(2, RoundingMode.HALF_UP);

        valuationSummaryLabel.setText(
            "Total inversión: $ " + cost.toPlainString()
                + "   •   Total potencial de venta: $ " + price.toPlainString()
                + "   •   Ganancia estimada: $ " + gain.toPlainString()
        );
    }

    private Path askCsvFile(String defaultName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar a CSV");
        chooser.setInitialFileName(defaultName);
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv")
        );

        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        return file == null ? null : file.toPath();
    }

    private <S> TableCell<S, BigDecimal> moneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("$ " + item.setScale(2, RoundingMode.HALF_UP).toPlainString());
                }
            }
        };
    }

    private <S> TableCell<S, BigDecimal> qtyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.stripTrailingZeros().toPlainString());
                }
            }
        };
    }
}
