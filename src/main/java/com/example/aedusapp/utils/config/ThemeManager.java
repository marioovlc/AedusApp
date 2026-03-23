package com.example.aedusapp.utils.config;

import com.example.aedusapp.MainApp;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static final String PREF_NODE = "com.example.aedusapp.preferences";
    private static final String THEME_KEY = "app_theme";

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
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        prefs.put(THEME_KEY, theme.name());
    }

    public static Theme getSavedTheme() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        String themeStr = prefs.get(THEME_KEY, Theme.OSCURO.name());
        try {
            return Theme.valueOf(themeStr);
        } catch (IllegalArgumentException e) {
            return Theme.OSCURO;
        }
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        
        Theme currentTheme = getSavedTheme();
        
        // Asegurar que el estilo base (Oscuro) siempre esté primero
        String baseCss = MainApp.class.getResource("styles/styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(baseCss)) {
            scene.getStylesheets().add(0, baseCss); // Añadir al principio si no estaba
        }
        
        // Eliminar posibles temas previos aplicados a esta escena
        scene.getStylesheets().removeIf(css -> css.endsWith("theme-light.css") || css.endsWith("theme-colorblind.css"));
        
        // Aplicar el nuevo tema al final de la lista para que sobreescriba estilos
        try {
            if (currentTheme == Theme.CLARO) {
                String lightCss = MainApp.class.getResource("styles/theme-light.css").toExternalForm();
                scene.getStylesheets().add(lightCss);
            } else if (currentTheme == Theme.DALTONICO) {
                String colorblindCss = MainApp.class.getResource("styles/theme-colorblind.css").toExternalForm();
                scene.getStylesheets().add(colorblindCss);
            }
        } catch (Exception e) {
            System.err.println("Theme application failed. Missing css file?: " + e.getMessage());
        }
    }
    public static void applyThemeToDialog(javafx.scene.control.DialogPane dialogPane) {
        if (dialogPane == null) return;
        
        Theme currentTheme = getSavedTheme();
        
        String baseCss = MainApp.class.getResource("styles/styles.css").toExternalForm();
        if (!dialogPane.getStylesheets().contains(baseCss)) {
            dialogPane.getStylesheets().add(0, baseCss);
        }
        
        dialogPane.getStylesheets().removeIf(css -> css.endsWith("theme-light.css") || css.endsWith("theme-colorblind.css"));
        
        try {
            if (currentTheme == Theme.CLARO) {
                String lightCss = MainApp.class.getResource("styles/theme-light.css").toExternalForm();
                dialogPane.getStylesheets().add(lightCss);
            } else if (currentTheme == Theme.DALTONICO) {
                String colorblindCss = MainApp.class.getResource("styles/theme-colorblind.css").toExternalForm();
                dialogPane.getStylesheets().add(colorblindCss);
            }
        } catch (Exception e) {
            System.err.println("Theme application failed. Missing css file?: " + e.getMessage());
        }
    }

    /**
     * Switches the logo image to match the current theme.
     * Dark theme → logo.png (white logo on dark background).
     * Light/Colorblind→ logoblack.png (dark logo on light background).
     */
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
