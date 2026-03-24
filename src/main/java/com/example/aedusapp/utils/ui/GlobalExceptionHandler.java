package com.example.aedusapp.utils.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Captura excepciones no controladas y las muestra al usuario de forma amigable 
 * en lugar de fallar silenciosamente.
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.error("Excepción no capturada en hilo {}: {}", t.getName(), e.getMessage(), e);
        
        if (Platform.isFxApplicationThread()) {
            showErrorDialog(e);
        } else {
            Platform.runLater(() -> showErrorDialog(e));
        }
    }

    private void showErrorDialog(Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error inesperado");
        alert.setHeaderText("AedusApp ha encontrado un problema");
        alert.setContentText("Ocurrió un error inesperado al procesar la solicitud: " + e.getMessage());

        // StackTrace expanding section
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("Detalles técnicos:");
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }
}
