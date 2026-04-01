package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DatabaseHelper;
import com.example.aedusapp.models.TiendaItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TiendaDAO {
    private static final Logger logger = LoggerFactory.getLogger(TiendaDAO.class);
    private static final String TABLE_NAME = "gestion_incidencias.store_items";

    private static final DatabaseHelper.RowMapper<TiendaItem> ITEM_MAPPER = rs -> new TiendaItem(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("price"),
            rs.getString("description"),
            rs.getString("icon"),
            rs.getString("color")
    );

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                     "id SERIAL PRIMARY KEY, " +
                     "name VARCHAR(100) NOT NULL, " +
                     "price INT NOT NULL, " +
                     "description TEXT, " +
                     "icon VARCHAR(100) DEFAULT 'star', " +
                     "color VARCHAR(20) DEFAULT '#F2C94C')";
        DatabaseHelper.executeUpdate(sql);
        logger.info("Tabla '{}' verificada/creada con éxito.", TABLE_NAME);
    }

    public List<TiendaItem> getAllItems() {
        String sql = "SELECT id, name, price, description, icon, color FROM " + TABLE_NAME + " ORDER BY price ASC";
        return DatabaseHelper.queryForList(sql, ITEM_MAPPER);
    }

    public boolean saveItem(TiendaItem item) {
        String sql = "INSERT INTO " + TABLE_NAME + " (name, price, description, icon, color) VALUES (?, ?, ?, ?, ?)";
        return DatabaseHelper.executeUpdate(sql, item.getNombre(), item.getCoste(), item.getDescripcion(), item.getIcon(), item.getColor());
    }

    public boolean deleteItem(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        return DatabaseHelper.executeUpdate(sql, id);
    }
}
