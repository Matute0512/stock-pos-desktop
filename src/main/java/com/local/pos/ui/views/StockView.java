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
import javafx.scene.control.TableRow;
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
    private final TextField searchField = new TextField();

    private Button editButton;
    private Button deactivateButton;

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
        setupSelectionListener();
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

        editButton = new Button("Editar");
        editButton.getStyleClass().add("button-secondary");
        editButton.setDisable(true);
        editButton.setOnAction(event -> openEditProductDialog());

        deactivateButton = new Button("Desactivar");
        deactivateButton.getStyleClass().add("button-danger");
        deactivateButton.setDisable(true);
        deactivateButton.setOnAction(event -> deactivateSelectedProduct());

        Button refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-secondary");
        refreshButton.setOnAction(event -> refreshProducts());

        toolbar.getChildren().addAll(
            searchField,
            newProductButton,
            editButton,
            deactivateButton,
            refreshButton
        );

        return toolbar;
    }

    private VBox createTableCard() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "table-card");

        Label titleLabel = new Label("Productos activos");
        titleLabel.getStyleClass().add("card-title");

        table.getStyleClass().add("modern-table");
        table.setItems(products);
        table.setPlaceholder(new Label("No hay productos cargados todavía."));

        // Doble clic para editar
        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openEditProductDialog();
                }
            });
            return row;
        });

        VBox content = new VBox(8, titleLabel, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        card.getChildren().add(content);

        return card;
    }

    private void setupTable() {
        TableColumn<Product, String> codeColumn = new TableColumn<>("Código");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        codeColumn.setPrefWidth(160);

        TableColumn<Product, String> skuColumn = new TableColumn<>("SKU");
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        skuColumn.setPrefWidth(100);

        TableColumn<Product, String> nameColumn = new TableColumn<>("Nombre");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(300);

        TableColumn<Product, BigDecimal> costColumn = new TableColumn<>("Costo");
        costColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
        costColumn.setPrefWidth(110);
        costColumn.setCellFactory(column -> moneyCell(false));

        TableColumn<Product, BigDecimal> priceColumn = new TableColumn<>("Precio");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setPrefWidth(110);
        priceColumn.setCellFactory(column -> moneyCell(true));

        TableColumn<Product, BigDecimal> stockColumn = new TableColumn<>("Stock");
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stockQty"));
        stockColumn.setPrefWidth(90);
        stockColumn.setCellFactory(column -> stockCell());

        TableColumn<Product, BigDecimal> minColumn = new TableColumn<>("Mínimo");
        minColumn.setCellValueFactory(new PropertyValueFactory<>("minStockQty"));
        minColumn.setPrefWidth(90);
        minColumn.setCellFactory(column -> numberCell());

        table.getColumns().addAll(
            codeColumn,
            skuColumn,
            nameColumn,
            costColumn,
            priceColumn,
            stockColumn,
            minColumn
        );
    }

    private void setupSelectionListener() {
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            boolean hasSelection = newValue != null;
            editButton.setDisable(!hasSelection);
            deactivateButton.setDisable(!hasSelection);
        });
    }

    private TableCell<Product, BigDecimal> moneyCell(boolean withSymbol) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("cell-warning");

                if (empty || item == null) {
                    setText(null);
                } else if (withSymbol) {
                    setText("$ " + item.setScale(2, RoundingMode.HALF_UP).toPlainString());
                } else {
                    setText(item.setScale(2, RoundingMode.HALF_UP).toPlainString());
                }
            }
        };
    }

    private TableCell<Product, BigDecimal> numberCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("cell-warning");

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.stripTrailingZeros().toPlainString());
                }
            }
        };
    }

    private TableCell<Product, BigDecimal> stockCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("cell-warning");

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(item.stripTrailingZeros().toPlainString());

                TableRow<Product> row = getTableRow();
                if (row != null && row.getItem() != null) {
                    Product product = row.getItem();
                    if (product.getMinStockQty() != null
                        && item.compareTo(product.getMinStockQty()) < 0) {
                        getStyleClass().add("cell-warning");
                    }
                }
            }
        };
    }

    private void openNewProductDialog() {
        ProductFormDialog dialog = new ProductFormDialog(null);
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

    private void openEditProductDialog() {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.info("Editar", "Seleccioná un producto para editar.");
            return;
        }

        ProductFormDialog dialog = new ProductFormDialog(selected);
        Optional<Product> result = dialog.showAndWait();

        result.ifPresent(product -> {
            try {
                productService.updateProduct(product);
                refreshProducts();
            } catch (BusinessException e) {
                AlertUtil.error("No se pudo actualizar", e.getMessage());
            } catch (Exception e) {
                AlertUtil.error("Error", "Ocurrió un error inesperado al actualizar el producto.");
            }
        });
    }

    private void deactivateSelectedProduct() {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.info("Desactivar", "Seleccioná un producto para desactivar.");
            return;
        }

        boolean confirmed = AlertUtil.confirm(
            "Desactivar producto",
            "¿Estás segura de que querés desactivar '" + selected.getName() + "'?\n\n"
                + "El producto no se borrará, pero dejará de aparecer en el stock y en el punto de venta."
        );

        if (confirmed) {
            try {
                productService.deactivateProduct(selected);
                refreshProducts();
            } catch (BusinessException e) {
                AlertUtil.error("No se pudo desactivar", e.getMessage());
            } catch (Exception e) {
                AlertUtil.error("Error", "Ocurrió un error inesperado al desactivar el producto.");
            }
        }
    }

    private void refreshProducts() {
        try {
            products.setAll(productService.listProducts());
        } catch (Exception e) {
            AlertUtil.error("Error", "No se pudieron cargar los productos.");
        }
    }
}
