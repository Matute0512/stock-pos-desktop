package com.local.pos;

import com.local.pos.config.AppContext;
import com.local.pos.core.AlertUtil;
import com.local.pos.ui.main.MainShellView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class PosApp extends Application {

    private AppContext appContext;

    @Override
    public void start(Stage stage) {
        // 1. Crear la ventana PRIMERO (sin datos todavía)
        MainShellView shellView = new MainShellView(null);

        Scene scene = new Scene(shellView.getRoot(), 1280, 800);

        URL css = getClass().getResource("/css/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Stock y Punto de Venta");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();

        // 2. Inicializar la base de datos en segundo plano
        Task<AppContext> initTask = new Task<>() {
            @Override
            protected AppContext call() {
                AppContext context = new AppContext();
                context.init();
                return context;
            }
        };

        initTask.setOnSucceeded(event -> {
            appContext = initTask.getValue();
            shellView.initializeWithData(appContext);
        });

        initTask.setOnFailed(event -> {
            Throwable error = initTask.getException();
            AlertUtil.error(
                "Error de inicio",
                "No se pudo inicializar la base de datos del sistema: " + error.getMessage()
            );
            Platform.exit();
        });

        Thread thread = new Thread(initTask);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop() {
        if (appContext != null) {
            appContext.shutdown();
        }
    }
}
