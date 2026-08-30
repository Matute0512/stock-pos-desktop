package com.local.pos;

import com.local.pos.ui.main.MainShellView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class PosApp extends Application {

    @Override
    public void start(Stage stage) {
        MainShellView shellView = new MainShellView();

        Scene scene = new Scene(shellView.getRoot(), 1280, 800);

        URL css = getClass().getResource("/css/app.css");
        if  (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Stock y punto de Venta");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();

    }
}
