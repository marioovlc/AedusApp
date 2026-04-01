package com.example.aedusapp.utils.config;

import com.example.aedusapp.MainApp;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static final String PREF_NODE = "com.example.aedusapp.preferences";
    private static final String THEME_KEY = "app_theme";
    private static final String LARGE_TEXT_KEY = "large_text";

    public enum Theme {
        OSCURO("Oscuro (Predeterminado)"), 
        CLARO("Claro"), 
        DALTONICO("Accesibilidad Daltónicos");

        private String label;

        Theme(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static void saveTheme(Theme theme) {
        Preferences.userRoot().node(PREF_NODE).put(THEME_KEY, theme.name());
    }

    public static Theme getSavedTheme() {
        String themeStr = Preferences.userRoot().node(PREF_NODE).get(THEME_KEY, Theme.OSCURO.name());
        try {
            return Theme.valueOf(themeStr);
        } catch (IllegalArgumentException e) {
            return Theme.OSCURO;
        }
    }

    public static void saveLargeText(boolean enabled) {
        Preferences.userRoot().node(PREF_NODE).putBoolean(LARGE_TEXT_KEY, enabled);
    }

    public static boolean isLargeTextEnabled() {
        return Preferences.userRoot().node(PREF_NODE).getBoolean(LARGE_TEXT_KEY, false);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        Theme currentTheme = getSavedTheme();

        // Asegurar que el estilo base (Oscuro) siempre esté primero
        String baseCss = MainApp.class.getResource("styles/styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(baseCss)) {
            scene.getStylesheets().add(0, baseCss);
        }

        // Eliminar temas previos
        scene.getStylesheets().removeIf(css ->
            css.endsWith("theme-light.css") ||
            css.endsWith("theme-colorblind.css") ||
            css.endsWith("theme-accessibility.css"));

        try {
            if (currentTheme == Theme.CLARO) {
                scene.getStylesheets().add(MainApp.class.getResource("styles/theme-light.css").toExternalForm());
            } else if (currentTheme == Theme.DALTONICO) {
                scene.getStylesheets().add(MainApp.class.getResource("styles/theme-colorblind.css").toExternalForm());
            }

            // Modo Lectura: se aplica encima de cualquier tema de color
            if (isLargeTextEnabled()) {
                scene.getStylesheets().add(MainApp.class.getResource("styles/theme-accessibility.css").toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Theme application failed: " + e.getMessage());
        }
    }

    public static void applyThemeToDialog(javafx.scene.control.DialogPane dialogPane) {
        if (dialogPane == null) return;

        Theme currentTheme = getSavedTheme();

        String baseCss = MainApp.class.getResource("styles/styles.css").toExternalForm();
        if (!dialogPane.getStylesheets().contains(baseCss)) {
            dialogPane.getStylesheets().add(0, baseCss);
        }

        dialogPane.getStylesheets().removeIf(css ->
            css.endsWith("theme-light.css") ||
            css.endsWith("theme-colorblind.css") ||
            css.endsWith("theme-accessibility.css"));

        try {
            if (currentTheme == Theme.CLARO) {
                dialogPane.getStylesheets().add(MainApp.class.getResource("styles/theme-light.css").toExternalForm());
            } else if (currentTheme == Theme.DALTONICO) {
                dialogPane.getStylesheets().add(MainApp.class.getResource("styles/theme-colorblind.css").toExternalForm());
            }

            if (isLargeTextEnabled()) {
                dialogPane.getStylesheets().add(MainApp.class.getResource("styles/theme-accessibility.css").toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Theme application failed: " + e.getMessage());
        }
    }

    public static void applyLogo(ImageView logoView) {
        if (logoView == null) return;
        Theme currentTheme = getSavedTheme();
        String imageName = (currentTheme == Theme.CLARO)
                ? "images/logoblack.png"
                : "images/logo.png";
        try {
            Image img = new Image(MainApp.class.getResourceAsStream(imageName));
            logoView.setImage(img);
        } catch (Exception e) {
            System.err.println("Could not load logo image: " + imageName + " – " + e.getMessage());
        }
    }
}
