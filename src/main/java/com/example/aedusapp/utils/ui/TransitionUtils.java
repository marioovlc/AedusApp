package com.example.aedusapp.utils.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Utilidades para animaciones y transiciones suaves entre vistas.
 * Mejora la experiencia de usuario (UX) dando un aspecto más profesional.
 */
public class TransitionUtils {

    /**
     * Hace que un nodo aparezca suavemente (Fade In).
     */
    public static void fadeIn(Node node) {
        node.setOpacity(0);
        node.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(0);
        ft.setToValue(1.0);
        ft.play();
    }

    /**
     * Alterna entre dos nodos con una transición de desvanecimiento coordinada.
     */
    public static void switchViews(Node from, Node to) {
        if (from == to) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), from);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            from.setVisible(false);
            from.setManaged(false);
            
            to.setOpacity(0);
            to.setVisible(true);
            to.setManaged(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), to);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    /**
     * Animación de entrada con ligero deslizamiento lateral.
     */
    public static void slideInRight(Node node) {
        node.setVisible(true);
        node.setManaged(true);
        
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(0);
        ft.setToValue(1.0);
        
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), node);
        tt.setFromX(20);
        tt.setToX(0);
        
        new ParallelTransition(ft, tt).play();
    }
}
