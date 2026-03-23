package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;

import com.example.aedusapp.models.Aula;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AulaDAO {

    public List<Aula> getAll() {
        List<Aula> aulas = new ArrayList<>();
        // Handle optional column 'capacidad' gracefully if it doesn't exist in older schemas (though we try to add it)
        String sql = "SELECT * FROM aulas ORDER BY nombre";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                int cap = 30;
                try { cap = rs.getInt("capacidad"); } catch (SQLException ignored) {}
                
                aulas.add(new Aula(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        cap
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return aulas;
    }

    public boolean create(Aula aula) {
        String sql = "INSERT INTO aulas (nombre, tipo, capacidad) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, aula.getNombre());
            stmt.setString(2, aula.getTipo());
            stmt.setInt(3, aula.getCapacidad() > 0 ? aula.getCapacidad() : 30);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        aula.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
