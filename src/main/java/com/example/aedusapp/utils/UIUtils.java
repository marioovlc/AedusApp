package com.example.aedusapp.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class UIUtils {

    /**
     * Muestra de forma segura un diálogo de error en el hilo principal de JavaFX.
     * Puede ser llamado desde cualquier Task o Background Thread.
     */
    public static void showError(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            showAlert(title, message);
        } else {
            Platform.runLater(() -> showAlert(title, message));
        }
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error del Sistema");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }
}
