package com.local.pos.ui.pos;

import com.local.pos.domain.model.PaymentMethod;
import com.local.pos.domain.model.Sale;
import com.local.pos.domain.model.SaleItem;
import com.local.pos.domain.service.SaleService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SalesHistoryDialog extends Dialog<Void> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public SalesHistoryDialog(SaleService saleService) {
        setTitle("Historial de ventas");
        setHeaderText("Ventas de hoy");

        getDialogPane().getStylesheets().add(
            getClass().getResource("/css/app.css").toExternalForm()
        );
        getDialogPane().getStyleClass().add("modern-dialog");

        List<Sale> sales = saleService.listSalesToday();

        TableView<Sale> salesTable = new TableView<>();
        salesTable.getStyleClass().add("modern-table");
        salesTable.setPlaceholder(new Label("No hay ventas registradas hoy."));
        salesTable.setItems(FXCollections.observableArrayList(sales));

        TableColumn<Sale, String> numberCol = new TableColumn<>("Número");
        numberCol.setCellValueFactory(new PropertyValueFactory<>("saleNumber"));
        numberCol.setPrefWidth(110);

        TableColumn<Sale, LocalDateTime> dateCol = new TableColumn<>("Fecha/Hora");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DATE_FMT.format(item));
            }
        });
        dateCol.setPrefWidth(150);

        TableColumn<Sale, String> methodCol = new TableColumn<>("Método");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        methodCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        setText(PaymentMethod.valueOf(item).getLabel());
                    } catch (IllegalArgumentException e) {
                        setText(item);
                    }
                }
            }
        });
        methodCol.setPrefWidth(110);

        TableColumn<Sale, BigDecimal> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setCellFactory(col -> moneyCell());
        totalCol.setPrefWidth(110);

        salesTable.getColumns().addAll(numberCol, dateCol, methodCol, totalCol);

        TableView<SaleItem> itemsTable = new TableView<>();
        itemsTable.getStyleClass().add("modern-table");
        itemsTable.setPlaceholder(new Label("Seleccioná una venta para ver sus items."));

        TableColumn<SaleItem, String> nameCol = new TableColumn<>("Producto");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        nameCol.setPrefWidth(240);

        TableColumn<SaleItem, BigDecimal> qtyCol = new TableColumn<>("Cant.");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(col -> qtyCell());
        qtyCol.setPrefWidth(70);

        TableColumn<SaleItem, BigDecimal> priceCol = new TableColumn<>("Precio");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceCol.setCellFactory(col -> moneyCell());
        priceCol.setPrefWidth(100);

        TableColumn<SaleItem, BigDecimal> lineCol = new TableColumn<>("Total");
        lineCol.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        lineCol.setCellFactory(col -> moneyCell());
        lineCol.setPrefWidth(110);

        itemsTable.getColumns().addAll(nameCol, qtyCol, priceCol, lineCol);

        salesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSale, sale) -> {
            if (sale != null) {
                itemsTable.setItems(FXCollections.observableArrayList(
                    saleService.getSaleItems(sale.getId())
                ));
            } else {
                itemsTable.setItems(FXCollections.observableArrayList());
            }
        });

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(salesTable, itemsTable);
        VBox.setVgrow(salesTable, Priority.ALWAYS);
        VBox.setVgrow(itemsTable, Priority.ALWAYS);

        salesTable.setPrefHeight(240);
        itemsTable.setPrefHeight(200);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
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

    private TableCell<SaleItem, BigDecimal> qtyCell() {
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
