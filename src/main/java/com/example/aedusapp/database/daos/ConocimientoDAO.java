package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConocimientoDAO {
    private static final Logger logger = LoggerFactory.getLogger(ConocimientoDAO.class);
    private static final String TABLE_NAME = "base_conocimiento";

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id SERIAL PRIMARY KEY, " +
                "titulo VARCHAR(255) NOT NULL, " +
                "contenido TEXT NOT NULL" +
                ")";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(sql);
            logger.info("Tabla '{}' verificada/creada con éxito.", TABLE_NAME);
        } catch (Exception e) {
            logger.error("Error creando tabla {}: {}", TABLE_NAME, e.getMessage(), e);
        }
    }

    public boolean insertArticulo(String titulo, String contenido) {
        String sql = "INSERT INTO " + TABLE_NAME + " (titulo, contenido) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, titulo);
            pstmt.setString(2, contenido);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error al insertar conocimiento: {}", e.getMessage(), e);
            return false;
        }
    }

    public String[] buscarArticuloSimilar(String query) {
        if (query == null || query.isBlank()) return null;
        
        // Full-Text Search de PostgreSQL: Priorizamos la inteligencia nativa
        String sql = "SELECT titulo, contenido FROM " + TABLE_NAME + " " +
                     "WHERE to_tsvector('spanish', titulo || ' ' || contenido) @@ plainto_tsquery('spanish', ?) " +
                     "ORDER BY ts_rank(to_tsvector('spanish', titulo || ' ' || contenido), plainto_tsquery('spanish', ?)) DESC " +
                     "LIMIT 1";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, query);
            pstmt.setString(2, query);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{ rs.getString("titulo"), rs.getString("contenido") };
                }
            }
        } catch (Exception e) {
            logger.error("Error buscando conocimiento similar con FTS: {}", e.getMessage(), e);
        }
        return null; // Not found
    }
}
