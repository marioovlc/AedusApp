package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConocimientoDAO {

    public ConocimientoDAO() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS base_conocimiento (" +
                "id SERIAL PRIMARY KEY, " +
                "titulo VARCHAR(255) NOT NULL, " +
                "contenido TEXT NOT NULL" +
                ")";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla 'base_conocimiento' verificada/creada con éxito.");
        } catch (Exception e) {
            System.err.println("Error creando tabla base_conocimiento: " + e.getMessage());
        }
    }

    public boolean insertArticulo(String titulo, String contenido) {
        String sql = "INSERT INTO base_conocimiento (titulo, contenido) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, titulo);
            pstmt.setString(2, contenido);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error al insertar conocimiento: " + e.getMessage());
            return false;
        }
    }

    public String[] buscarArticuloSimilar(String query) {
        if (query == null || query.isBlank()) return null;
        
        // Simple word matching based on ILIKE or splitting words.
        // For simplicity, we just split the query and look for matches in title.
        String[] words = query.toLowerCase().split("\\s+");
        if (words.length == 0) return null;

        StringBuilder sqlBuilder = new StringBuilder("SELECT titulo, contenido FROM base_conocimiento WHERE ");
        for (int i = 0; i < words.length; i++) {
            sqlBuilder.append("titulo ILIKE ?");
            if (i < words.length - 1) sqlBuilder.append(" OR ");
        }
        sqlBuilder.append(" LIMIT 1");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sqlBuilder.toString())) {
            
            for (int i = 0; i < words.length; i++) {
                pstmt.setString(i + 1, "%" + words[i] + "%");
            }
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new String[]{ rs.getString("titulo"), rs.getString("contenido") };
            }
        } catch (Exception e) {
            System.err.println("Error buscando conocimiento similar: " + e.getMessage());
        }
        return null; // Not found
    }
}
