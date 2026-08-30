package com.local.pos.ui.stock;

import com.local.pos.domain.model.MovementType;
import com.local.pos.domain.model.StockMovement;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StockHistoryDialog extends Dialog<Void> {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public StockHistoryDialog(String productName, List<StockMovement> movements) {
        setTitle("Historial de movimientos");
        setHeaderText(productName);

        getDialogPane().getStylesheets().add(
            getClass().getResource("/css/app.css").toExternalForm()
        );
        getDialogPane().getStyleClass().add("modern-dialog");

        TableView<StockMovement> table = new TableView<>();
        table.getStyleClass().add("modern-table");
        table.setPlaceholder(new Label("No hay movimientos registrados."));
        table.setItems(FXCollections.observableArrayList(movements));

        // Declarar la columna con LocalDateTime
        TableColumn<StockMovement, LocalDateTime> dateColumn = new TableColumn<>("Fecha");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DATE_FMT.format(item));
            }
        });
        dateColumn.setPrefWidth(150);

        TableColumn<StockMovement, String> typeColumn = new TableColumn<>("Tipo");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("movementType"));
        typeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        setText(MovementType.valueOf(item).getLabel());
                    } catch (IllegalArgumentException e) {
                        setText(item);
                    }
                }
            }
        });
        typeColumn.setPrefWidth(160);

        TableColumn<StockMovement, BigDecimal> qtyColumn = new TableColumn<>("Cantidad");
        qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyColumn.setCellFactory(col -> numberCell());
        qtyColumn.setPrefWidth(110);

        TableColumn<StockMovement, BigDecimal> resultColumn = new TableColumn<>("Stock resultante");
        resultColumn.setCellValueFactory(new PropertyValueFactory<>("resultStockQty"));
        resultColumn.setCellFactory(col -> numberCell());
        resultColumn.setPrefWidth(140);

        TableColumn<StockMovement, String> reasonColumn = new TableColumn<>("Motivo");
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonColumn.setPrefWidth(260);

        table.getColumns().addAll(dateColumn, typeColumn, qtyColumn, resultColumn, reasonColumn);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label summary = new Label("Total de movimientos: " + movements.size());
        summary.getStyleClass().add("form-info");

        content.getChildren().addAll(summary, table);

        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        table.setPrefHeight(380);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    private TableCell<StockMovement, BigDecimal> numberCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.setScale(2, RoundingMode.HALF_UP).toPlainString());
                }
            }
        };
    }
}
