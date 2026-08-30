package com.local.pos.ui.views;

import com.local.pos.core.AlertUtil;
import com.local.pos.core.BusinessException;
import com.local.pos.domain.model.Product;
import com.local.pos.domain.model.Sale;
import com.local.pos.domain.model.SaleItem;
import com.local.pos.domain.service.ProductService;
import com.local.pos.domain.service.SaleService;
import com.local.pos.ui.pos.CartItem;
import com.local.pos.ui.pos.PaymentDialog;
import com.local.pos.ui.pos.SalesHistoryDialog;
import com.local.pos.ui.pos.TicketPrinter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PosView extends VBox {

    private final ProductService productService;
    private final SaleService saleService;

    private final TextField searchField = new TextField();
    private final Button addButton = new Button("Agregar");
    private final ObservableList<Product> results = FXCollections.observableArrayList();
    private final TableView<Product> resultsTable = new TableView<>();

    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();
    private final TableView<CartItem> cartTable = new TableView<>();

    private final Label subtotalLabel = new Label("$ 0.00");
    private final TextField discountField = new TextField("0");
    private final Label totalLabel = new Label("$ 0.00");

    public PosView(ProductService productService, SaleService saleService) {
        this.productService = productService;
        this.saleService = saleService;

        getStyleClass().add("view");
        setSpacing(18);
        setPadding(new Insets(28));

        VBox header = createHeader(
            "Punto de venta",
            "Buscá un producto, armá el carrito y cobrá con F4."
        );

        HBox searchCard = createSearchCard();

        HBox layout = new HBox(18);
        VBox.setVgrow(layout, Priority.ALWAYS);

        VBox left = createLeftPanel();
        HBox.setHgrow(left, Priority.ALWAYS);

        VBox right = createTotalsPanel();
        right.setPrefWidth(340);
        right.setMinWidth(300);

        layout.getChildren().addAll(left, right);

        getChildren().addAll(header, searchCard, layout);

        cart.addListener((ListChangeListener<CartItem>) change -> updateTotals());
        discountField.textProperty().addListener((obs, oldValue, newValue) -> updateTotals());

        setupSearch();
        setupCartColumns();
        setupKeyboardShortcuts();

        // Foco automático al entrar a la vista
        parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                Platform.runLater(searchField::requestFocus);
            }
        });

        search("");
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

        searchField.setPromptText("Escaneá un código de barras o buscá un producto...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        addButton.getStyleClass().add("button-primary");

        searchCard.getChildren().addAll(searchField, addButton);

        return searchCard;
    }

    private VBox createLeftPanel() {
        VBox panel = new VBox(18);

        VBox resultsCard = new VBox(8);
        resultsCard.getStyleClass().addAll("card", "table-card");

        Label resultsTitle = new Label("Resultados");
        resultsTitle.getStyleClass().add("card-title");

        resultsTable.getStyleClass().add("modern-table");
        resultsTable.setItems(results);
        resultsTable.setPlaceholder(new Label("Escribí para buscar o escaneá un código de barras."));

        TableColumn<Product, String> nameCol = new TableColumn<>("Nombre");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(240);

        TableColumn<Product, String> codeCol = new TableColumn<>("Código");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        codeCol.setPrefWidth(120);

        TableColumn<Product, BigDecimal> priceCol = new TableColumn<>("Precio");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(col -> moneyCell());
        priceCol.setPrefWidth(100);

        TableColumn<Product, BigDecimal> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stockQty"));
        stockCol.setCellFactory(col -> stockCell());
        stockCol.setPrefWidth(90);

        resultsTable.getColumns().addAll(nameCol, codeCol, priceCol, stockCol);

        resultsTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    addToCart(row.getItem());
                }
            });
            return row;
        });

        resultsTable.setPrefHeight(220);

        resultsCard.getChildren().addAll(resultsTitle, resultsTable);

        VBox cartCard = new VBox(8);
        cartCard.getStyleClass().addAll("card", "table-card");

        Label cartTitle = new Label("Carrito");
        cartTitle.getStyleClass().add("card-title");

        cartTable.getStyleClass().add("modern-table");
        cartTable.setItems(cart);
        cartTable.setEditable(true);
        cartTable.setPlaceholder(new Label("El carrito está vacío."));

        cartCard.getChildren().addAll(cartTitle, cartTable);

        panel.getChildren().addAll(resultsCard, cartCard);
        VBox.setVgrow(cartCard, Priority.ALWAYS);

        return panel;
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> search(newValue));

        searchField.setOnAction(event -> {
            String text = searchField.getText().trim();

            // Enter con barcode exacto: agrega directo
            for (Product product : results) {
                if (text.equalsIgnoreCase(product.getBarcode())) {
                    addToCart(product);
                    return;
                }
            }

            // Sino, agrega el producto seleccionado
            Product selected = resultsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                addToCart(selected);
            }
        });

        addButton.setOnAction(event -> {
            Product selected = resultsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                addToCart(selected);
            } else {
                AlertUtil.info("Agregar", "Seleccioná un producto de la lista.");
            }
        });
    }

    private void search(String term) {
        try {
            results.setAll(productService.searchProducts(term));
        } catch (Exception e) {
            AlertUtil.error("Error", "No se pudo realizar la búsqueda.");
        }
    }

    private void setupCartColumns() {
        TableColumn<CartItem, String> nameCol = new TableColumn<>("Nombre");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<CartItem, BigDecimal> qtyCol = new TableColumn<>("Cantidad");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setEditable(true);
        qtyCol.setCellFactory(col -> quantityCell());
        qtyCol.setPrefWidth(90);

        TableColumn<CartItem, BigDecimal> priceCol = new TableColumn<>("Precio");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceCol.setCellFactory(col -> moneyCell());
        priceCol.setPrefWidth(100);

        TableColumn<CartItem, BigDecimal> lineCol = new TableColumn<>("Total");
        lineCol.setCellValueFactory(cellData -> Bindings.createObjectBinding(
            cellData.getValue()::lineTotal,
            cellData.getValue().quantityProperty(),
            cellData.getValue().unitPriceProperty()
        ));
        lineCol.setCellFactory(col -> moneyCell());
        lineCol.setPrefWidth(110);

        TableColumn<CartItem, Void> removeCol = new TableColumn<>("Quitar");
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeButton = new Button("Quitar");

            {
                removeButton.getStyleClass().add("button-danger");
                removeButton.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    cart.remove(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeButton);
            }
        });
        removeCol.setPrefWidth(80);

        cartTable.getColumns().addAll(nameCol, qtyCol, priceCol, lineCol, removeCol);
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

    private TableCell<CartItem, BigDecimal> quantityCell() {
        return new TableCell<>() {
            private final TextField field = new TextField();

            {
                field.setOnAction(event -> {
                    try {
                        BigDecimal value = new BigDecimal(field.getText().trim().replace(',', '.'));
                        commitEdit(value);
                    } catch (NumberFormatException e) {
                        cancelEdit();
                    }
                });
                field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if (wasFocused && !isFocused && isEditing()) {
                        cancelEdit();
                    }
                });
            }

            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item.stripTrailingZeros().toPlainString());
                setGraphic(null);
            }

            @Override
            public void startEdit() {
                super.startEdit();
                field.setText(getItem() == null ? "" : getItem().stripTrailingZeros().toPlainString());
                setGraphic(field);
                setText(null);
                field.requestFocus();
                field.selectAll();
            }

            @Override
            public void commitEdit(BigDecimal value) {
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    cancelEdit();
                    return;
                }

                CartItem item = getTableView().getItems().get(getIndex());

                if (value == null || value.signum() <= 0) {
                    AlertUtil.error("Cantidad inválida", "La cantidad debe ser mayor a cero.");
                    cancelEdit();
                    return;
                }

                if (value.compareTo(item.getAvailableStock()) > 0) {
                    AlertUtil.error("Stock insuficiente",
                        "No hay stock suficiente para: " + item.getName());
                    cancelEdit();
                    return;
                }

                item.setQuantity(value.setScale(3, RoundingMode.HALF_UP));
                updateTotals();
                super.commitEdit(value);
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                if (getItem() != null) {
                    setText(getItem().stripTrailingZeros().toPlainString());
                }
                setGraphic(null);
            }
        };
    }

    private VBox createTotalsPanel() {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "pos-totals");

        Label title = new Label("Resumen");
        title.getStyleClass().add("card-title");

        HBox subtotalRow = new HBox();
        subtotalRow.getStyleClass().add("pos-row");
        Label subtotalCaption = new Label("Subtotal");
        subtotalCaption.getStyleClass().add("pos-row-caption");
        HBox.setHgrow(subtotalCaption, Priority.ALWAYS);
        subtotalLabel.getStyleClass().add("pos-row-value");
        subtotalRow.getChildren().addAll(subtotalCaption, subtotalLabel);

        HBox discountRow = new HBox();
        discountRow.getStyleClass().add("pos-row");
        Label discountCaption = new Label("Descuento");
        discountCaption.getStyleClass().add("pos-row-caption");
        discountField.getStyleClass().add("search-field");
        discountField.setPrefWidth(110);
        discountField.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(discountCaption, Priority.ALWAYS);
        discountRow.getChildren().addAll(discountCaption, discountField);

        HBox totalRow = new HBox();
        totalRow.getStyleClass().addAll("pos-row", "pos-total-row");
        Label totalCaption = new Label("TOTAL");
        totalCaption.getStyleClass().add("pos-total-caption");
        HBox.setHgrow(totalCaption, Priority.ALWAYS);
        totalLabel.getStyleClass().add("pos-total-amount");
        totalRow.getChildren().addAll(totalCaption, totalLabel);

        Button payButton = new Button("Cobrar (F4)");
        payButton.getStyleClass().add("button-primary");
        payButton.setMaxWidth(Double.MAX_VALUE);
        payButton.setOnAction(event -> checkout());

        Button cancelButton = new Button("Cancelar venta (ESC)");
        cancelButton.getStyleClass().add("button-danger");
        cancelButton.setMaxWidth(Double.MAX_VALUE);
        cancelButton.setOnAction(event -> cancelSale());

        Button historyButton = new Button("Historial de ventas");
        historyButton.getStyleClass().add("button-secondary");
        historyButton.setMaxWidth(Double.MAX_VALUE);
        historyButton.setOnAction(event -> new SalesHistoryDialog(saleService).showAndWait());

        card.getChildren().addAll(
            title, subtotalRow, discountRow, totalRow,
            payButton, cancelButton, historyButton
        );

        return card;
    }

    private void addToCart(Product product) {
        if (product == null) {
            return;
        }

        BigDecimal addQty = BigDecimal.ONE;
        Optional<CartItem> existing = cart.stream()
            .filter(item -> item.getProductId() == product.getId())
            .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            BigDecimal newQty = item.getQuantity().add(addQty);
            if (newQty.compareTo(product.getStockQty()) > 0) {
                AlertUtil.error("Stock insuficiente",
                    "No hay stock suficiente para: " + product.getName());
                return;
            }
            item.setQuantity(newQty);
        } else {
            if (addQty.compareTo(product.getStockQty()) > 0) {
                AlertUtil.error("Stock insuficiente",
                    "No hay stock suficiente para: " + product.getName());
                return;
            }
            cart.add(new CartItem(product.getId(), product.getName(),
                product.getPrice(), addQty, product.getStockQty()));
        }

        updateTotals();
        searchField.clear();
        searchField.requestFocus();
    }

    private void checkout() {
        if (cart.isEmpty()) {
            AlertUtil.info("Cobrar", "El carrito está vacío.");
            return;
        }

        BigDecimal subtotal = subtotal();
        BigDecimal discount = parseDiscount();

        if (discount.compareTo(subtotal) > 0) {
            AlertUtil.error("Descuento inválido", "El descuento no puede superar el subtotal.");
            return;
        }

        PaymentDialog dialog = new PaymentDialog(subtotal, discount);
        Optional<PaymentDialog.PaymentResult> result = dialog.showAndWait();

        result.ifPresent(payment -> {
            try {
                List<CartItem> sold = new ArrayList<>(cart);
                Sale sale = saleService.createSale(sold, discount, payment.method(), payment.cashReceived());
                List<SaleItem> saleItems = saleService.getSaleItems(sale.getId());

                cart.clear();
                searchField.clear();
                updateTotals();

                boolean print = AlertUtil.confirm("Ticket", "¿Imprimir el ticket de la venta?");
                if (print) {
                    TicketPrinter.print(TicketPrinter.buildTicket(sale, saleItems));
                }

            } catch (BusinessException e) {
                AlertUtil.error("No se pudo cobrar", e.getMessage());
            } catch (Exception e) {
                AlertUtil.error("Error", "Ocurrió un error inesperado al registrar la venta.");
            }
        });
    }

    private void cancelSale() {
        if (cart.isEmpty()) {
            return;
        }

        boolean confirmed = AlertUtil.confirm("Cancelar venta", "¿Vaciar el carrito?");
        if (confirmed) {
            cart.clear();
            searchField.clear();
            updateTotals();
        }
    }

    private void updateTotals() {
        BigDecimal subtotal = subtotal();
        BigDecimal discount = parseDiscount();
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        subtotalLabel.setText("$ " + subtotal.toPlainString());
        totalLabel.setText("$ " + total.toPlainString());
    }

    private BigDecimal subtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart) {
            subtotal = subtotal.add(item.lineTotal());
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseDiscount() {
        try {
            String text = discountField.getText().trim().replace(',', '.');
            if (text.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void setupKeyboardShortcuts() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F4) {
                checkout();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                cancelSale();
                event.consume();
            }
        });
    }
}
