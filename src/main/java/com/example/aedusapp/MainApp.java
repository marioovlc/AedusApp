  package com.example.aedusapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.example.aedusapp.utils.config.ThemeManager;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage stage) throws java.io.IOException {
        // Registrar manejador de excepciones global
        Thread.setDefaultUncaughtExceptionHandler(new com.example.aedusapp.utils.ui.GlobalExceptionHandler());

        logger.info("Iniciando AedusApp...");
        
        com.example.aedusapp.utils.DependencyInjector.register(com.example.aedusapp.database.daos.IMensajeDAO.class, new com.example.aedusapp.database.daos.MensajeDAO(new com.example.aedusapp.database.daos.AchievementDAO()));
        com.example.aedusapp.utils.DependencyInjector.register(com.example.aedusapp.services.hub.IConnectHubService.class, new com.example.aedusapp.services.hub.ConnectHubService());
        
        URL splashUrl = com.example.aedusapp.utils.ResourceLoader.getFXMLURL(com.example.aedusapp.utils.config.AppConfig.getSplashPath());
        FXMLLoader fxmlLoader = new FXMLLoader(splashUrl);
        Scene scene = new Scene(fxmlLoader.load(), com.example.aedusapp.utils.config.AppConfig.getAppWidth(), com.example.aedusapp.utils.config.AppConfig.getAppHeight());

        ThemeManager.applyTheme(scene);

        stage.setTitle(com.example.aedusapp.utils.config.AppConfig.getAppName());
        
        java.io.InputStream iconStream = com.example.aedusapp.utils.ResourceLoader.getImageStream(com.example.aedusapp.utils.config.AppConfig.getIconPath());
        if (iconStream != null) {
            stage.getIcons().add(new javafx.scene.image.Image(iconStream));
        }

        stage.setResizable(false);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        com.example.aedusapp.utils.ConcurrencyManager.shutdown();
        com.example.aedusapp.database.config.DBConnection.closePool();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
