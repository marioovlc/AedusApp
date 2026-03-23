package com.example.aedusapp.utils.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Clase que centraliza la configuración de la aplicación ("No-Hardcoding").
 * Extrae valores desde las variables de entorno o provee valores por defecto seguros.
 */
public class AppConfig {

    private static final Dotenv dotenv;

    static {
        // ignoreIfMissing() permite usar las variables de sistema directamente en Producción/Docker
        dotenv = Dotenv.configure().ignoreIfMissing().load();
    }

    // --- DATABASE ---
    public static String getDbUrl() {
        return getEnv("DB_URL", "jdbc:postgresql://localhost:5432/aedus");
    }

    public static String getDbUser() {
        return getEnv("DB_USER", "postgres");
    }

    public static String getDbPass() {
        return getEnv("DB_PASS", "");
    }
    
    public static String getDbSchema() {
        return getEnv("DB_SCHEMA", "gestion_incidencias, public, neon_auth");
    }
    
    public static int getDbPoolMax() {
        return Integer.parseInt(getEnv("DB_POOL_MAX", "15"));
    }

    // --- CLOUDINARY ---
    public static String getCloudinaryCloudName() {
        return getEnv("CLOUDINARY_CLOUD_NAME", "");
    }

    public static String getCloudinaryApiKey() {
        return getEnv("CLOUDINARY_API_KEY", "");
    }

    public static String getCloudinaryApiSecret() {
        return getEnv("CLOUDINARY_API_SECRET", "");
    }

    // --- AI ---
    public static String getAiApiUrl() {
        return getEnv("AI_API_URL", "https://api.groq.com/openai/v1/chat/completions");
    }

    public static String getAiApiKey() {
        return getEnv("AI_API_KEY", "");
    }
    
    public static String getAiModel() {
        return getEnv("AI_MODEL", "llama-3.3-70b-versatile");
    }

    // --- APP UI ---
    public static String getAppName() {
        return getEnv("APP_NAME", "Aedus");
    }

    public static int getAppWidth() {
        return Integer.parseInt(getEnv("APP_WIDTH", "600"));
    }

    public static int getAppHeight() {
        return Integer.parseInt(getEnv("APP_HEIGHT", "400"));
    }

    // --- MÉTODOS DE APOYO ---
    private static String getEnv(String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
