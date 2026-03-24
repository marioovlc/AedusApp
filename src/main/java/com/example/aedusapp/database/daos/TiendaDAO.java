package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DatabaseHelper;
import com.example.aedusapp.models.TiendaItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TiendaDAO {
    private static final Logger logger = LoggerFactory.getLogger(TiendaDAO.class);
    private static final String TABLE_NAME = "tienda_items";

    private static final DatabaseHelper.RowMapper<TiendaItem> ITEM_MAPPER = rs -> new TiendaItem(
            rs.getInt("id"),
            rs.getString("nombre"),
            rs.getInt("coste"),
            rs.getString("descripcion")
    );

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                     "id SERIAL PRIMARY KEY, " +
                     "nombre VARCHAR(100) NOT NULL, " +
                     "coste INT NOT NULL, " +
                     "descripcion TEXT)";
        DatabaseHelper.executeUpdate(sql);
        logger.info("Tabla '{}' verificada/creada con éxito.", TABLE_NAME);
    }

    public List<TiendaItem> getAllItems() {
        String sql = "SELECT id, nombre, coste, descripcion FROM " + TABLE_NAME + " ORDER BY coste ASC";
        return DatabaseHelper.queryForList(sql, ITEM_MAPPER);
    }

    public boolean saveItem(TiendaItem item) {
        String sql = "INSERT INTO " + TABLE_NAME + " (nombre, coste, descripcion) VALUES (?, ?, ?)";
        return DatabaseHelper.executeUpdate(sql, item.getNombre(), item.getCoste(), item.getDescripcion());
    }

    public boolean deleteItem(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        return DatabaseHelper.executeUpdate(sql, id);
    }
}
