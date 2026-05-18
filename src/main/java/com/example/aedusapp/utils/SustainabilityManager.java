package com.example.aedusapp.utils;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// =============================================
// ==== CLASE SUSTAINABILITYMANAGER =====
// Descripción: Gestor de inactividad que activa un protector de pantalla /
// modo de ahorro de energía sostenible tras 5 minutos de inactividad del usuario.
// =============================================
public class SustainabilityManager {
    private static final Logger logger = LoggerFactory.getLogger(SustainabilityManager.class);
    private static final Duration TIMEOUT = Duration.minutes(5);
    
    private final Stage stage;
    private Timeline inactivityTimer;
    private StackPane overlay;
    private boolean isSuspended = false;

    public SustainabilityManager(Stage stage) {
        this.stage = stage;
        initInactivityTimer();
        initOverlay();
        attachListeners();
    }

    private void initInactivityTimer() {
        inactivityTimer = new Timeline(new KeyFrame(TIMEOUT, e -> suspend()));
        inactivityTimer.setCycleCount(1);
        inactivityTimer.play();
    }

    private void initOverlay() {
        overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");
        overlay.setVisible(false);
        overlay.setOpacity(0);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);

        FontIcon ecoIcon = new FontIcon("fas-leaf");
        ecoIcon.setIconSize(80);
        ecoIcon.setIconColor(Color.web("#52f39c"));

        Label title = new Label("MODO SOSTENIBILIDAD");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-letter-spacing: 4px;");

        Label subtitle = new Label("Mueve el ratón o presiona una tecla para despertar");
        subtitle.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 14px;");

        Circle pulse = new Circle(10, Color.web("#52f39c", 0.5));
        Timeline pulseAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(pulse.radiusProperty(), 5), new javafx.animation.KeyValue(pulse.opacityProperty(), 0.8)),
            new KeyFrame(Duration.seconds(1.5), new javafx.animation.KeyValue(pulse.radiusProperty(), 20), new javafx.animation.KeyValue(pulse.opacityProperty(), 0))
        );
        pulseAnim.setCycleCount(Timeline.INDEFINITE);
        pulseAnim.play();

        content.getChildren().addAll(ecoIcon, title, subtitle, pulse);
        overlay.getChildren().add(content);
        
        // Prevent clicking through the overlay when suspended
        overlay.setOnMouseClicked(e -> wakeUp());
    }

    private void attachListeners() {
        // Listen for scene changes on the stage
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerInputFilter(newScene);
            }
        });

        // Initial registration if scene already exists
        if (stage.getScene() != null) {
            registerInputFilter(stage.getScene());
        }
    }

    private void registerInputFilter(Scene scene) {
        scene.addEventFilter(InputEvent.ANY, e -> {
            if (isSuspended) {
                wakeUp();
            } else {
                resetTimer();
            }
        });
    }

    private void resetTimer() {
        inactivityTimer.stop();
        inactivityTimer.playFromStart();
    }

    private void suspend() {
        if (isSuspended) return;
        logger.info("Activando modo sostenibilidad por inactividad.");
        isSuspended = true;

        Platform.runLater(() -> {
            Scene scene = stage.getScene();
            if (scene == null) return;

            Parent root = scene.getRoot();
            StackPane stackRoot;

            if (root instanceof StackPane) {
                stackRoot = (StackPane) root;
            } else {
                // Wrap the existing root in a StackPane to allow overlaying
                stackRoot = new StackPane(root);
                scene.setRoot(stackRoot);
            }

            if (!stackRoot.getChildren().contains(overlay)) {
                stackRoot.getChildren().add(overlay);
            }
            overlay.setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(800), overlay);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
    }

    private void wakeUp() {
        if (!isSuspended) return;
        logger.info("Despertando de modo sostenibilidad.");
        isSuspended = false;

        Platform.runLater(() -> {
            FadeTransition ft = new FadeTransition(Duration.millis(400), overlay);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                overlay.setVisible(false);
                resetTimer();
            });
            ft.play();
        });
    }
    
    public static void setup(Stage stage) {
        new SustainabilityManager(stage);
    }
}
