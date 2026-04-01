package com.example.aedusapp.utils.ui;

import javafx.animation.FadeTransition;
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

            // Main container
            HBox root = new HBox(12);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(12, 22, 12, 22));
            root.setMinWidth(300);
            root.setMaxWidth(450);

            // Style mapping
            String color = switch (type) {
                case SUCCESS -> "#10b981";
                case ERROR -> "#ef4444";
                case WARNING -> "#f59e0b";
                default -> "#3b82f6";
            };

            root.setStyle("-fx-background-color: #1e293b; " +
                          "-fx-background-radius: 12; " +
                          "-fx-border-color: " + color + "; " +
                          "-fx-border-width: 0 0 0 4; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 15, 0, 0, 8);");

            // Emoji/Icon
            String emoji = switch (type) {
                case SUCCESS -> "✅";
                case ERROR -> "❌";
                case WARNING -> "⚠️";
                default -> "ℹ️";
            };
            Label icon = new Label(emoji);
            icon.setStyle("-fx-font-size: 16px;");

            // Message
            Label lbl = new Label(message);
            lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
            lbl.setWrapText(true);
            lbl.setMaxWidth(380);

            root.getChildren().addAll(icon, lbl);
            popup.getContent().add(root);

            // Position: Top Center of the parent window
            popup.show(window);
            double x = window.getX() + (window.getWidth() / 2) - (root.getWidth() / 2);
            double y = window.getY() + 60;
            popup.setX(x);
            popup.setY(y);

            // Animations
            root.setOpacity(0);
            root.setTranslateY(-20);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            javafx.animation.TranslateTransition slideIn = new javafx.animation.TranslateTransition(Duration.millis(300), root);
            slideIn.setByY(20);

            javafx.animation.ParallelTransition entry = new javafx.animation.ParallelTransition(fadeIn, slideIn);

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(3));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> popup.hide());

            new javafx.animation.SequentialTransition(entry, pause, fadeOut).play();
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
