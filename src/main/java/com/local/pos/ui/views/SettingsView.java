package com.local.pos.ui.views;

import com.local.pos.core.AlertUtil;
import com.local.pos.domain.service.BackupService;
import com.local.pos.domain.service.SettingsService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class SettingsView extends VBox {

    private final SettingsService settingsService;
    private final BackupService backupService;
    private final Runnable shutdown;

    private final TextField nameField = new TextField();
    private final TextField addressField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField footerField = new TextField();
    private final Label backupInfoLabel = new Label();

    public SettingsView(SettingsService settingsService, BackupService backupService, Runnable shutdown) {
        this.settingsService = settingsService;
        this.backupService = backupService;
        this.shutdown = shutdown;

        getStyleClass().add("view");
        setSpacing(18);
        setPadding(new Insets(28));

        VBox header = createHeader(
            "Configuración",
            "Datos del local y copias de seguridad."
        );

        VBox storeCard = createStoreCard();
        VBox backupCard = createBackupCard();

        getChildren().addAll(header, storeCard, backupCard);
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

    private VBox createStoreCard() {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "card-large");

        Label title = new Label("Datos del local");
        title.getStyleClass().add("card-title");

        nameField.setText(settingsService.getStoreName());
        addressField.setText(settingsService.getStoreAddress());
        phoneField.setText(settingsService.getStorePhone());
        footerField.setText(settingsService.getTicketFooter());

        VBox form = new VBox(10);
        form.getChildren().addAll(
            fieldRow("Nombre del local", nameField),
            fieldRow("Dirección", addressField),
            fieldRow("Teléfono", phoneField),
            fieldRow("Texto de cierre del ticket", footerField)
        );

        Button saveButton = new Button("Guardar");
        saveButton.getStyleClass().add("button-primary");
        saveButton.setOnAction(event -> {
            settingsService.saveAll(
                nameField.getText(),
                addressField.getText(),
                phoneField.getText(),
                footerField.getText()
            );
            AlertUtil.info("Configuración", "Configuración guardada.");
        });

        card.getChildren().addAll(title, form, saveButton);

        return card;
    }

    private VBox createBackupCard() {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "card-large");

        Label title = new Label("Copias de seguridad");
        title.getStyleClass().add("card-title");

        refreshBackupInfo();
        backupInfoLabel.getStyleClass().add("form-info");
        backupInfoLabel.setWrapText(true);

        Button backupButton = new Button("Hacer copia ahora");
        backupButton.getStyleClass().add("button-secondary");
        backupButton.setOnAction(event -> {
            try {
                Path file = backupService.backup();
                backupService.pruneOldBackups();
                refreshBackupInfo();
                AlertUtil.info("Copia de seguridad",
                    "Copia creada en:\n" + file.toAbsolutePath());
            } catch (Exception e) {
                AlertUtil.error("Copia de seguridad",
                    "No se pudo crear la copia: " + e.getMessage());
            }
        });

        Button restoreButton = new Button("Restaurar copia...");
        restoreButton.getStyleClass().add("button-danger");
        restoreButton.setOnAction(event -> restoreFlow());

        card.getChildren().addAll(title, backupInfoLabel, backupButton, restoreButton);

        return card;
    }

    private HBox fieldRow(String caption, TextField field) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(caption);
        label.getStyleClass().add("form-label");
        label.setMinWidth(220);

        field.setPrefWidth(340);

        row.getChildren().addAll(label, field);

        return row;
    }

    private void refreshBackupInfo() {
        List<Path> backups = backupService.listBackups();
        backupInfoLabel.setText(
            "Directorio: " + backupService.getBackupDir().toAbsolutePath()
                + "  •  Copias existentes: " + backups.size()
        );
    }

    private void restoreFlow() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar copia de seguridad");
        chooser.setInitialDirectory(backupService.getBackupDir().toFile());
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos de backup (*.zip)", "*.zip")
        );

        File selected = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected == null) {
            return;
        }

        boolean confirmed = AlertUtil.confirm(
            "Restaurar copia",
            "Se reemplazarán TODOS los datos actuales y la aplicación se cerrará.\n\n¿Continuar?"
        );
        if (!confirmed) {
            return;
        }

        AlertUtil.info("Restauración", "Reiniciá la aplicación para completar la restauración.");

        try {
            backupService.restore(selected.toPath(), shutdown);
            Platform.exit();
        } catch (Exception e) {
            AlertUtil.error("Restauración", "No se pudo restaurar: " + e.getMessage());
        }
    }
}
