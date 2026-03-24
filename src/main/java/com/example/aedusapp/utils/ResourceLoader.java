package com.example.aedusapp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

public class ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class);

    /**
     * Obtiene de manera segura la URL del FXML.
     */
    public static URL getFXMLURL(String path) {
        URL url = loadWithFallback(path, ResourceLoader.class::getResource, Thread.currentThread().getContextClassLoader()::getResource);
        if (url == null) {
            logger.error("Error crítico: recurso FXML no encontrado: {}", path);
            throw new IllegalStateException("Recurso crítico FXML no encontrado: " + path);
        }
        return url;
    }

    /**
     * Obtiene de manera segura el InputStream de una imagen.
     */
    public static InputStream getImageStream(String path) {
        InputStream stream = loadWithFallback(path, ResourceLoader.class::getResourceAsStream, Thread.currentThread().getContextClassLoader()::getResourceAsStream);
        if (stream == null) {
            logger.error("Advertencia: No se pudo cargar la imagen. Archivo no encontrado o accesible: {}", path);
        }
        return stream;
    }

    /**
     * Método genérico para cargar recursos con fallbacks automáticos centralizando la lógica repetitiva.
     */
    private static <T> T loadWithFallback(String path, Function<String, T> classResolver, Function<String, T> classLoaderResolver) {
        T resource = classResolver.apply(path);
        if (resource == null) {
            logger.warn("No se pudo encontrar el recurso en la ruta principal: {}", path);
            if (path.startsWith("/")) {
                String relativePath = path.substring(1);
                resource = classResolver.apply(relativePath);
                if (resource == null) {
                    resource = classLoaderResolver.apply(relativePath);
                }
            }
        }
        return resource;
    }
}
