package com.local.pos.ui.pos;

import com.local.pos.core.AlertUtil;
import com.local.pos.domain.model.PaymentMethod;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;

public class PaymentDialog extends Dialog<PaymentDialog.PaymentResult> {

    public record PaymentResult(PaymentMethod method, BigDecimal cashReceived) {
    }

    private static final int MONEY_SCALE = 2;

    private final BigDecimal total;

    private final ComboBox<PaymentMethod> methodCombo = new ComboBox<>();
    private final TextField cashField = new TextField();
    private final Label changeLabel = new Label();
    private final VBox cashBox = new VBox(10);

    public PaymentDialog(BigDecimal subtotal, BigDecimal discount) {
        this.total = subtotal.subtract(discount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        setTitle("Cobrar");
        setHeaderText("Total a cobrar: $ " + this.total.toPlainString());

        URL css = getClass().getResource("/css/app.css");
        if (css != null) {
            getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        getDialogPane().getStyleClass().add("modern-dialog");

        methodCombo.getItems().addAll(PaymentMethod.CASH, PaymentMethod.CARD, PaymentMethod.TRANSFER);
        methodCombo.setValue(PaymentMethod.CASH);
        methodCombo.setMaxWidth(Double.MAX_VALUE);

        cashField.setPromptText("Monto recibido");
        cashField.setText(this.total.toPlainString());

        changeLabel.getStyleClass().add("form-info");

        HBox quickRow = new HBox(8);
        quickRow.setAlignment(Pos.CENTER_LEFT);
        quickRow.getChildren().addAll(
            quickButton("Exacto"),
            quickButton("1000"),
            quickButton("2000"),
            quickButton("5000"),
            quickButton("10000")
        );

        Label changeCaption = new Label("Cambio:");
        changeCaption.getStyleClass().add("form-label");

        cashBox.getChildren().addAll(cashField, quickRow, changeCaption, changeLabel);

        methodCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateMode());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        grid.add(label("Método de pago"), 0, 0);
        grid.add(methodCombo, 1, 0);
        grid.add(label("Recibido"), 0, 1);
        grid.add(cashBox, 1, 1);

        getDialogPane().setContent(grid);

        ButtonType payButtonType = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);

        Button payButton = (Button) getDialogPane().lookupButton(payButtonType);
        payButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validate()) {
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                PaymentMethod method = methodCombo.getValue();
                return new PaymentResult(method, method == PaymentMethod.CASH ? parseReceived() : null);
            }
            return null;
        });

        cashField.textProperty().addListener((obs, oldValue, newValue) -> updateChange());

        updateMode();
    }

    private Button quickButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("button-secondary");
        button.setOnAction(event -> {
            if ("Exacto".equals(text)) {
                cashField.setText(total.toPlainString());
            } else {
                cashField.setText(text);
            }
        });
        return button;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private void updateMode() {
        boolean cash = methodCombo.getValue() == PaymentMethod.CASH;
        cashBox.setManaged(cash);
        cashBox.setVisible(cash);
        updateChange();
    }

    private void updateChange() {
        if (methodCombo.getValue() != PaymentMethod.CASH) {
            changeLabel.setText("");
            changeLabel.setStyle("");
            return;
        }

        BigDecimal received = parseReceived();
        if (received == null) {
            changeLabel.setText("Ingresá un monto válido.");
            changeLabel.setStyle("-fx-text-fill: -danger;");
            return;
        }

        BigDecimal change = received.subtract(total);
        if (change.signum() < 0) {
            changeLabel.setText("Faltan $ " + change.negate().toPlainString());
            changeLabel.setStyle("-fx-text-fill: -danger;");
        } else {
            changeLabel.setText("$ " + change.toPlainString());
            changeLabel.setStyle("");
        }
    }

    private BigDecimal parseReceived() {
        try {
            String text = cashField.getText().trim().replace(',', '.');
            if (text.isEmpty()) {
                return null;
            }
            return new BigDecimal(text).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean validate() {
        PaymentMethod method = methodCombo.getValue();
        if (method == null) {
            AlertUtil.error("Método de pago", "Seleccioná un método de pago.");
            return false;
        }

        if (method == PaymentMethod.CASH) {
            BigDecimal received = parseReceived();
            if (received == null) {
                AlertUtil.error("Monto recibido", "Ingresá un monto válido.");
                return false;
            }
            if (received.compareTo(total) < 0) {
                AlertUtil.error("Monto insuficiente", "El efectivo recibido no alcanza el total.");
                return false;
            }
        }

        return true;
    }
}
