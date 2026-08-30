package com.local.pos.ui.views;

import com.local.pos.core.AlertUtil;
import com.local.pos.core.BusinessException;
import com.local.pos.domain.model.Product;
import com.local.pos.domain.service.ProductService;
import com.local.pos.ui.stock.ProductFormDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class StockView extends VBox {

    private final ProductService productService;
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final TableView<Product> table = new TableView<>();

    public StockView(ProductService productService) {
        this.productService = productService;

        getStyleClass().add("view");
        setSpacing(18);
        setPadding(new Insets(28));

        VBox header = createHeader(
            "Stock",
            "Administrar productos, precios y existencias."
        );

        HBox toolbar = createToolbar();
        VBox tableCard = createTableCard();
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        getChildren().addAll(header, toolbar, tableCard);

        setupTable();
        refreshProducts();
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

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                products.setAll(productService.searchProducts(newValue));
            } catch (Exception e) {
                AlertUtil.error("Error", "No se pudo realizar la búsqueda.");
            }
        });

        Button newProductButton = new Button("Nuevo producto");
        newProductButton.getStyleClass().add("button-primary");
        newProductButton.setOnAction(event -> openNewProductDialog());

        Button editButton = new Button("Editar");
        editButton.getStyleClass().add("button-secondary");
        editButton.setDisable(true);

        Button adjustStockButton = new Button("Ajustar stock");
        adjustStockButton.getStyleClass().add("button-secondary");
        adjustStockButton.setDisable(true);

        toolbar.getChildren().addAll(
            searchField,
            newProductButton,
            editButton,
            adjustStockButton
        );

        return toolbar;
    }

    private VBox createTableCard() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "table-card");

        table.getStyleClass().add("modern-table");
        table.setItems(products);
        table.setPlaceholder(new Label("No hay productos cargados todavía."));

        card.getChildren().add(table);

        return card;
    }

    private void setupTable() {
        TableColumn<Product, String> codeColumn = new TableColumn<>("Código");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        codeColumn.setPrefWidth(150);

        TableColumn<Product, String> nameColumn = new TableColumn<>("Nombre");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(340);

        TableColumn<Product, BigDecimal> priceColumn = new TableColumn<>("Precio");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(column -> moneyCell(true));

        TableColumn<Product, BigDecimal> stockColumn = new TableColumn<>("Stock");
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stockQty"));
        stockColumn.setCellFactory(column -> moneyCell(false));

        TableColumn<Product, BigDecimal> minColumn = new TableColumn<>("Mínimo");
        minColumn.setCellValueFactory(new PropertyValueFactory<>("minStockQty"));
        minColumn.setCellFactory(column -> moneyCell(false));

        table.getColumns().addAll(
            codeColumn,
            nameColumn,
            priceColumn,
            stockColumn,
            minColumn
        );
    }

    private TableCell<Product, BigDecimal> moneyCell(boolean withSymbol) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else if (withSymbol) {
                    setText("$ " + item.setScale(2, RoundingMode.HALF_UP).toPlainString());
                } else {
                    setText(item.stripTrailingZeros().toPlainString());
                }
            }
        };
    }

    private void openNewProductDialog() {
        ProductFormDialog dialog = new ProductFormDialog();
        Optional<Product> result = dialog.showAndWait();

        result.ifPresent(product -> {
            try {
                productService.createProduct(product);
                refreshProducts();
            } catch (BusinessException e) {
                AlertUtil.error("No se pudo guardar", e.getMessage());
            } catch (Exception e) {
                AlertUtil.error("Error", "Ocurrió un error inesperado al guardar el producto.");
            }
        });
    }

    private void refreshProducts() {
        try {
            products.setAll(productService.listProducts());
        } catch (Exception e) {
            AlertUtil.error("Error", "No se pudieron cargar los productos.");
        }
    }
}
