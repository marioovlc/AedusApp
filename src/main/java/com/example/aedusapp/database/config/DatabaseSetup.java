package com.example.aedusapp.database.config;

import com.example.aedusapp.database.daos.*;

import java.sql.Connection;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSetup {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSetup.class);

    public static void run() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            logger.info("Initializing Database Schema...");

            // Ensure schema exists
            stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS gestion_incidencias");
            stmt.executeUpdate("SET search_path TO gestion_incidencias, public, neon_auth");

            // Initialize tables using DAOs
            new MensajeDAO().createTable();
            new LogDAO().createTable();
            new AchievementDAO().initAchievementTables();
            new TiendaDAO().createTable();
            
            // --- MIGRACIONES DE ESQUEMA ---

            // 1. Columnas de Usuario (Personalización)
            try {
                stmt.executeUpdate("ALTER TABLE neon_auth.user ADD COLUMN IF NOT EXISTS telefono VARCHAR(20)");
                stmt.executeUpdate("ALTER TABLE neon_auth.user ADD COLUMN IF NOT EXISTS bio TEXT");
            } catch (Exception e) {
                logger.error("Error migrando tabla usuarios: {}", e.getMessage(), e);
            }

            // 2. Columnas de Mensajes (Chat Directo y Tickets Compartidos)
            try {
                stmt.executeUpdate("ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS receptor_id UUID REFERENCES neon_auth.user(id)");
                stmt.executeUpdate("ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS ticket_link_id INT REFERENCES incidencias(id)");
            } catch (Exception e) {
                logger.error("Error migrando tabla mensajes: {}", e.getMessage(), e);
            }

            // Add new missing columns to mensajes if they don't exist
            try {
                stmt.executeUpdate("ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS leido BOOLEAN DEFAULT FALSE");
            } catch (Exception e) { logger.debug("Columna leido omitida o fallo silencioso"); }
            try {
                stmt.executeUpdate("ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS is_soporte BOOLEAN DEFAULT FALSE");
            } catch (Exception e) { logger.debug("Columna is_soporte omitida o fallo silencioso"); }
            try {
                stmt.executeUpdate("ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS audio_url VARCHAR(500)");
            } catch (Exception e) { logger.debug("Columna audio_url omitida o fallo silencioso"); }

            // Create ESTADOS
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS estados (id SERIAL PRIMARY KEY, nombre VARCHAR(50) UNIQUE NOT NULL)");
            stmt.executeUpdate("INSERT INTO estados (nombre) VALUES ('NO LEIDO'), ('LEIDO'), ('EN REVISION'), ('ACABADO') ON CONFLICT (nombre) DO NOTHING");

            // Create CATEGORIAS
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS categorias (id SERIAL PRIMARY KEY, nombre VARCHAR(50) UNIQUE NOT NULL)");
            stmt.executeUpdate("INSERT INTO categorias (nombre) VALUES ('Hardware'), ('Software'), ('Conectividad'), ('Mobiliario') ON CONFLICT (nombre) DO NOTHING");

            // Create AULAS
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS aulas (id SERIAL PRIMARY KEY, nombre VARCHAR(50) UNIQUE NOT NULL)");
            try {
                stmt.executeUpdate("ALTER TABLE aulas ADD COLUMN IF NOT EXISTS tipo VARCHAR(50)");
            } catch (Exception e) { logger.debug("Columna tipo omitida en aulas"); }
            try {
                stmt.executeUpdate("ALTER TABLE aulas ADD COLUMN IF NOT EXISTS capacidad INT DEFAULT 30 NOT NULL");
            } catch (Exception e) { logger.debug("Columna capacidad omitida en aulas"); }

            // Seed some initial aulas if empty
            stmt.executeUpdate("INSERT INTO aulas (nombre, tipo, capacidad) VALUES ('Aula 101', 'General', 30), ('Aula 202', 'Informática', 25) ON CONFLICT (nombre) DO NOTHING");

            // Create INCIDENCIAS
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS incidencias (" +
                    "id SERIAL PRIMARY KEY, " +
                    "titulo VARCHAR(255), " +
                    "descripcion TEXT, " +
                    "usuario_id UUID, " + 
                    "aula_id INT REFERENCES aulas(id), " +
                    "categoria_id INT REFERENCES categorias(id), " +
                    "estado_id INT REFERENCES estados(id), " +
                    "aula_tipo VARCHAR(50), " +
                    "fecha_creacion TIMESTAMP DEFAULT NOW(), " +
                    "imagen_ruta VARCHAR(255), " +
                    "resolucion TEXT" + 
                    ")");
            
            try {
                stmt.executeUpdate("ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS resolucion TEXT");
            } catch (Exception e) { logger.debug("Columna resolucion omitida en incidencias"); }

            logger.info("Database schema initialized.");
            
            // Migration for Avatars
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT id, foto_perfil FROM neon_auth.user WHERE foto_perfil IS NOT NULL AND foto_perfil_datos IS NULL")) {
                while (rs.next()) {
                    String userId = rs.getString("id");
                    String path = rs.getString("foto_perfil");
                    if (path != null && !path.isEmpty()) {
                        java.io.File file = new java.io.File(path);
                        if (!file.exists()) file = new java.io.File(System.getProperty("user.dir"), path);

                        if (file.exists()) {
                            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
                            try (java.sql.PreparedStatement pstmt = conn.prepareStatement("UPDATE neon_auth.user SET foto_perfil_datos = ? WHERE id = ?")) {
                                pstmt.setBytes(1, data);
                                pstmt.setObject(2, java.util.UUID.fromString(userId));
                                pstmt.executeUpdate();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Database initialization failed: {}", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        run();
    }
}
