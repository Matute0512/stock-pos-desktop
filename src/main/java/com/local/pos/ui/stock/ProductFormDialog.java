package com.local.pos.ui.stock;

import com.local.pos.core.AlertUtil;
import com.local.pos.core.BusinessException;
import com.local.pos.domain.model.Product;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;

public class ProductFormDialog extends Dialog<Product> {

    private final Product existingProduct;

    private final TextField nameField = new TextField();
    private final TextField barcodeField = new TextField();
    private final TextField skuField = new TextField();
    private final TextField costField = new TextField("0");
    private final TextField priceField = new TextField("0");
    private final TextField stockField = new TextField("0");
    private final TextField minStockField = new TextField("0");
    private final TextArea descriptionField = new TextArea();

    public ProductFormDialog(Product existingProduct) {
        this.existingProduct = existingProduct;

        setTitle(existingProduct == null ? "Nuevo producto" : "Editar producto");
        setHeaderText(existingProduct == null
            ? "Cargar un producto al stock"
            : "Modificar los datos del producto");

        URL css = getClass().getResource("/css/app.css");
        if (css != null) {
            getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        getDialogPane().getStyleClass().add("modern-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        nameField.setPrefWidth(280);
        barcodeField.setPrefWidth(280);
        skuField.setPrefWidth(280);
        descriptionField.setPrefRowCount(3);
        descriptionField.setPrefColumnCount(25);
        descriptionField.setWrapText(true);

        grid.add(label("Nombre *"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(label("Código de barras"), 0, 1);
        grid.add(barcodeField, 1, 1);

        grid.add(label("SKU"), 0, 2);
        grid.add(skuField, 1, 2);

        grid.add(label("Costo"), 0, 3);
        grid.add(costField, 1, 3);

        grid.add(label("Precio"), 0, 4);
        grid.add(priceField, 1, 4);

        grid.add(label("Stock inicial"), 0, 5);
        grid.add(stockField, 1, 5);

        grid.add(label("Stock mínimo"), 0, 6);
        grid.add(minStockField, 1, 6);

        grid.add(label("Descripción"), 0, 7);
        grid.add(descriptionField, 1, 7);
        GridPane.setVgrow(descriptionField, Priority.NEVER);

        if (existingProduct != null) {
            nameField.setText(existingProduct.getName());
            barcodeField.setText(existingProduct.getBarcode() != null ? existingProduct.getBarcode() : "");
            skuField.setText(existingProduct.getSku() != null ? existingProduct.getSku() : "");
            costField.setText(existingProduct.getCost().toPlainString());
            priceField.setText(existingProduct.getPrice().toPlainString());
            stockField.setText(existingProduct.getStockQty().toPlainString());
            minStockField.setText(existingProduct.getMinStockQty().toPlainString());
            descriptionField.setText(existingProduct.getDescription() != null ? existingProduct.getDescription() : "");

            // El stock inicial no se edita desde acá, se ajusta desde "Ajustar stock"
            stockField.setDisable(true);
            stockField.setTooltip(new javafx.scene.control.Tooltip(
                "El stock se modifica desde el botón 'Ajustar stock' para mantener el historial."
            ));
        }

        getDialogPane().setContent(grid);

        ButtonType saveButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return buildProduct();
            }
            return null;
        });
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private Product buildProduct() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                throw new BusinessException("El nombre del producto es obligatorio.");
            }

            Product product = existingProduct != null ? existingProduct : new Product();
            product.setName(name);
            product.setBarcode(blankToNull(barcodeField.getText()));
            product.setSku(blankToNull(skuField.getText()));
            product.setDescription(blankToNull(descriptionField.getText()));
            product.setCost(parseDecimal(costField.getText(), "Costo"));
            product.setPrice(parseDecimal(priceField.getText(), "Precio"));

            if (existingProduct == null) {
                product.setStockQty(parseDecimal(stockField.getText(), "Stock inicial"));
                product.setMinStockQty(parseDecimal(minStockField.getText(), "Stock mínimo"));
                product.setActive(true);
            } else {
                // En edición solo actualizamos el stock mínimo, no el stock real
                product.setMinStockQty(parseDecimal(minStockField.getText(), "Stock mínimo"));
            }

            return product;

        } catch (BusinessException e) {
            AlertUtil.error("Revisá los datos", e.getMessage());
            return null;
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal parseDecimal(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(text.trim().replace(',', '.'))
                .setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new BusinessException(fieldName + ": el valor no es un número válido.");
        }
    }
}
