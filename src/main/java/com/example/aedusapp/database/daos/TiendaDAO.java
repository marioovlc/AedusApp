package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;

import com.example.aedusapp.models.TiendaItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TiendaDAO {

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tienda_items (" +
                     "id SERIAL PRIMARY KEY, " +
                     "nombre VARCHAR(100) NOT NULL, " +
                     "coste INT NOT NULL, " +
                     "descripcion TEXT)";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Table 'tienda_items' verified/created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<TiendaItem> getAllItems() {
        List<TiendaItem> items = new ArrayList<>();
        String sql = "SELECT id, nombre, coste, descripcion FROM tienda_items ORDER BY coste ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new TiendaItem(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getInt("coste"),
                    rs.getString("descripcion")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public boolean saveItem(TiendaItem item) {
        String sql = "INSERT INTO tienda_items (nombre, coste, descripcion) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getNombre());
            stmt.setInt(2, item.getCoste());
            stmt.setString(3, item.getDescripcion());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteItem(int id) {
        String sql = "DELETE FROM tienda_items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
