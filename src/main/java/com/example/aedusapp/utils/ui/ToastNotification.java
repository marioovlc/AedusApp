package com.example.aedusapp.utils.ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Muestra notificaciones tipo "toast" en la esquina inferior derecha de la
 * ventana.
 * No bloquea la interfaz y desaparecen solos tras unos segundos.
 */
public class ToastNotification {

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    /**
     * Muestra un toast en la ventana indicada.
     * 
     * @param window  Ventana padre
     * @param message Mensaje a mostrar
     * @param type    Tipo de notificación (SUCCESS, ERROR, WARNING, INFO)
     */
    public static void show(Window window, String message, Type type) {
        Platform.runLater(() -> {
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setHideOnEscape(false);

            // Contenedor principal del toast
            HBox container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(12, 20, 12, 16));
            container.setMinWidth(280);
            container.setMaxWidth(420);

            // Estilo según el tipo
            String emoji;
            String bgColor;
            String borderColor;
            switch (type) {
                case SUCCESS -> {
                    emoji = "✅";
                    bgColor = "#0d2d1a";
                    borderColor = "#10b981";
                }
                case ERROR -> {
                    emoji = "❌";
                    bgColor = "#2d0d0d";
                    borderColor = "#ef4444";
                }
                case WARNING -> {
                    emoji = "⚠️";
                    bgColor = "#2d1e0d";
                    borderColor = "#f59e0b";
                }
                default -> {
                    emoji = "ℹ️";
                    bgColor = "#0d1a2d";
                    borderColor = "#3b82f6";
                }
            }

            container.setStyle(
                    "-fx-background-color: " + bgColor + ";" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: " + borderColor + ";" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 1.5;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 4);");

            // Icono
            Label icon = new Label(emoji);
            icon.setStyle("-fx-font-size: 18px;");

            // Texto
            Label text = new Label(message);
            text.setStyle(
                    "-fx-text-fill: #e2e8f0;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-family: 'Segoe UI';" +
                            "-fx-wrap-text: true;");
            text.setMaxWidth(340);
            text.setWrapText(true);

            container.getChildren().addAll(icon, text);
            popup.getContent().add(container);

            // Posición: esquina inferior derecha
            popup.show(window);
            double x = window.getX() + window.getWidth() - container.getMinWidth() - 30;
            double y = window.getY() + window.getHeight() - 90;
            popup.setX(x);
            popup.setY(y);

            // Animación de aparición
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), container);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // Auto-cierre tras 3.5 segundos con fade out
            Timeline timer = new Timeline(new KeyFrame(Duration.millis(3500), e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), container);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> popup.hide());
                fadeOut.play();
            }));
            timer.play();
        });
    }

    // Shortcuts estáticos
    public static void success(Window w, String msg) {
        show(w, msg, Type.SUCCESS);
    }

    public static void error(Window w, String msg) {
        show(w, msg, Type.ERROR);
    }

    public static void warning(Window w, String msg) {
        show(w, msg, Type.WARNING);
    }

    public static void info(Window w, String msg) {
        show(w, msg, Type.INFO);
    }
}
