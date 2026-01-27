package com.example.aedusapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    // Método de inicio
    @Override
    public void start(Stage stage) throws IOException {
        // Cargar la pantalla de Login al principio
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("views/auth/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Añadir estilos CSS
        String css = MainApp.class.getResource("styles/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        // Configurar título e icono de la ventana
        stage.setTitle("Aedus");
        stage.getIcons().add(new javafx.scene.image.Image(MainApp.class.getResourceAsStream("images/logo.png")));

        // Mostrar la ventana
        stage.setScene(scene);
        stage.show();
    }

    // Método main estándar de Java
    public static void main(String[] args) {
        // Inicializar base de datos
        com.example.aedusapp.database.DatabaseInitializer.main(args);

        launch(); // Lanza la aplicación JavaFX
    }
}
