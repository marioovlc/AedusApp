  package com.example.aedusapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.example.aedusapp.utils.config.ThemeManager;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws java.io.IOException {
        
        URL splashUrl = MainApp.class.getResource("/com/example/aedusapp/views/general/splash_screen.fxml");
        if (splashUrl == null) {
            System.err.println("CRÍTICO: No se puede encontrar /com/example/aedusapp/views/general/splash_screen.fxml");
            // Fallback a ruta relativa por si el empaquetado inicial difiere
            splashUrl = MainApp.class.getResource("views/general/splash_screen.fxml");
        }
        
        if (splashUrl == null) {
            throw new IllegalStateException("Recurso crítico FXML no encontrado: splash_screen.fxml");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(splashUrl);
        Scene scene = new Scene(fxmlLoader.load(), com.example.aedusapp.utils.config.AppConfig.getAppWidth(), com.example.aedusapp.utils.config.AppConfig.getAppHeight());

        ThemeManager.applyTheme(scene);

        stage.setTitle(com.example.aedusapp.utils.config.AppConfig.getAppName());
        
        java.io.InputStream iconStream = MainApp.class.getResourceAsStream("/com/example/aedusapp/images/logo.png");
        if (iconStream == null) {
            iconStream = MainApp.class.getResourceAsStream("images/logo.png");
        }
        
        if (iconStream != null) {
            stage.getIcons().add(new javafx.scene.image.Image(iconStream));
        } else {
            System.err.println("Advertencia: No se pudo cargar el logo de la aplicación. Archivo no encontrado.");
        }

        stage.setResizable(false);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        com.example.aedusapp.database.config.DBConnection.closePool();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
