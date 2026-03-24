package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DatabaseHelper;
import com.example.aedusapp.models.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

// DAO para gestionar los logs del sistema
public class LogDAO {

    private static final Logger logger = LoggerFactory.getLogger(LogDAO.class);
    private static final String TABLE_NAME = "logs";

    private static final DatabaseHelper.RowMapper<Log> LOG_MAPPER = rs -> {
        Log log = new Log();
        log.setId(rs.getInt("id"));
        log.setUsuarioId(rs.getString("usuario_id"));
        // El join con neon_auth.user puede devolver nulo nombre si el usuario no existe
        try { log.setUsuarioNombre(rs.getString("usuario_nombre")); } catch (SQLException e) {} 
        log.setAccion(rs.getString("accion"));
        log.setCategoria(rs.getString("categoria"));
        log.setDescripcion(rs.getString("descripcion"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        return log;
    };

    // Crear la tabla si no existe
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id SERIAL PRIMARY KEY, " +
                "usuario_id UUID, " +
                "accion VARCHAR(255), " +
                "categoria VARCHAR(50), " +
                "descripcion TEXT, " +
                "ip_address VARCHAR(50), " +
                "fecha_creacion TIMESTAMP DEFAULT NOW()" +
                ")";
        DatabaseHelper.executeUpdate(sql);
        logger.info("Tabla '{}' verificada/creada con éxito.", TABLE_NAME);
    }

    // Registrar un nuevo log
    public boolean registerLog(Log log) {
        String sql = "INSERT INTO " + TABLE_NAME + " (usuario_id, accion, categoria, descripcion, ip_address, fecha_creacion) " +
                "VALUES (?::uuid, ?, ?, ?, ?, NOW())";
        return DatabaseHelper.executeUpdate(sql, log.getUsuarioId(), log.getAccion(), log.getCategoria(), log.getDescripcion(), log.getIpAddress());
    }

    // Obtener todos los logs ordenados por fecha descendente
    public List<Log> getAllLogs() {
        String sql = "SELECT l.id, l.usuario_id, l.accion, l.categoria, l.descripcion, " +
                "l.ip_address, l.fecha_creacion, u.name as usuario_nombre " +
                "FROM " + TABLE_NAME + " l " +
                "LEFT JOIN neon_auth.user u ON CAST(l.usuario_id AS TEXT) = u.id::TEXT " +
                "ORDER BY l.fecha_creacion DESC " +
                "LIMIT 1000";
        return DatabaseHelper.queryForList(sql, LOG_MAPPER);
    }

    // Obtener estadísticas de logs por categoría
    public int countLogsByCategory(String categoria) {
        String sql = "SELECT COUNT(*) as total FROM " + TABLE_NAME + " WHERE categoria = ?";
        Integer count = DatabaseHelper.queryForObject(sql, rs -> rs.getInt("total"), categoria);
        return count != null ? count : 0;
    }

    // Obtener total de logs
    public int countAllLogs() {
        String sql = "SELECT COUNT(*) as total FROM " + TABLE_NAME;
        Integer count = DatabaseHelper.queryForObject(sql, rs -> rs.getInt("total"));
        return count != null ? count : 0;
    }

    // Eliminar todos los logs (Limpiar historial)
    public boolean deleteAllLogs() {
        String sql = "DELETE FROM " + TABLE_NAME;
        return DatabaseHelper.executeUpdate(sql);
    }
}
