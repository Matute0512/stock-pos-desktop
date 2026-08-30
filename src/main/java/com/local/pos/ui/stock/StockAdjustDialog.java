package com.local.pos.ui.stock;

import com.local.pos.domain.model.MovementType;
import com.local.pos.domain.model.Product;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.net.URL;

public class StockAdjustDialog extends Dialog<StockAdjustDialog.Result> {

    private final ComboBox<MovementType> typeCombo = new ComboBox<>();
    private final TextField quantityField = new TextField("0");
    private final TextArea reasonField = new TextArea();
    private final Label stockLabel = new Label();

    public StockAdjustDialog(Product product) {
        setTitle("Ajustar stock");
        setHeaderText(product.getName());

        URL css = getClass().getResource("/css/app.css");
        if (css != null) {
            getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        getDialogPane().getStyleClass().add("modern-dialog");

        typeCombo.getItems().addAll(
            MovementType.PURCHASE,
            MovementType.RETURN,
            MovementType.ADJUST_IN,
            MovementType.ADJUST_OUT
        );
        typeCombo.setPromptText("Seleccionar tipo");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        stockLabel.setText("Stock actual: " + product.getStockQty().toPlainString());
        stockLabel.getStyleClass().add("form-info");

        reasonField.setPrefRowCount(3);
        reasonField.setPromptText("Ej: rotura, faltante inventario, mercadería recibida...");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        grid.add(label("Tipo de movimiento"), 0, 0);
        grid.add(typeCombo, 1, 0);

        grid.add(label("Cantidad"), 0, 1);
        grid.add(quantityField, 1, 1);

        grid.add(stockLabel, 0, 2, 2, 1);

        grid.add(label("Motivo *"), 0, 3);
        grid.add(reasonField, 1, 3);
        GridPane.setVgrow(reasonField, Priority.NEVER);

        getDialogPane().setContent(grid);

        ButtonType saveButton = new ButtonType("Aplicar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return new Result(
                    typeCombo.getValue(),
                    quantityField.getText(),
                    reasonField.getText()
                );
            }
            return null;
        });
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    public record Result(MovementType type, String quantityText, String reason) {
    }
}
