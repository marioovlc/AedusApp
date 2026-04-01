package com.example.aedusapp;

import com.example.aedusapp.utils.AppInitializer;
import com.example.aedusapp.utils.ConcurrencyManager;
import com.example.aedusapp.utils.ResourceLoader;
import com.example.aedusapp.utils.config.AppConfig;
import com.example.aedusapp.utils.config.ThemeManager;
import com.example.aedusapp.database.config.DBConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 * Delega la inicialización del sistema a AppInitializer.
 */
public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Inicializar sistema (DI, Excepciones, Logging)
        AppInitializer.init();

        logger.info("Iniciando interfaz gráfica de AedusApp...");
        
        // 2. Cargar Splash Screen
        URL splashUrl = ResourceLoader.getFXMLURL(AppConfig.getSplashPath());
        FXMLLoader fxmlLoader = new FXMLLoader(splashUrl);
        Scene scene = new Scene(fxmlLoader.load(), AppConfig.getAppWidth(), AppConfig.getAppHeight());

        // 3. Aplicar Estilos y Tema
        ThemeManager.applyTheme(scene);

        // 4. Configurar Ventana (Stage)
        stage.setTitle(AppConfig.getAppName());
        
        try (InputStream iconStream = ResourceLoader.getImageStream(AppConfig.getIconPath())) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        }

        stage.setResizable(true);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Cerrando recursos de AedusApp...");
        ConcurrencyManager.shutdown();
        DBConnection.closePool();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
