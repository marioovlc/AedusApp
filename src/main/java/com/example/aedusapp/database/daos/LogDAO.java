package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;

import com.example.aedusapp.models.Log;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// DAO para gestionar los logs del sistema
public class LogDAO {

    private static final Logger logger = LoggerFactory.getLogger(LogDAO.class);

    // Crear la tabla si no existe
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS logs (" +
                "id SERIAL PRIMARY KEY, " +
                "usuario_id UUID, " +
                "accion VARCHAR(255), " +
                "categoria VARCHAR(50), " +
                "descripcion TEXT, " +
                "ip_address VARCHAR(50), " +
                "fecha_creacion TIMESTAMP DEFAULT NOW()" +
                ")";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("Error al crear la tabla logs: {}", e.getMessage(), e);
        }
    }

    // Registrar un nuevo log
    public boolean registerLog(Log log) {
        String sql = "INSERT INTO logs (usuario_id, accion, categoria, descripcion, ip_address, fecha_creacion) " +
                "VALUES (?::uuid, ?, ?, ?, ?, NOW())";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, log.getUsuarioId());
            stmt.setString(2, log.getAccion());
            stmt.setString(3, log.getCategoria());
            stmt.setString(4, log.getDescripcion());
            stmt.setString(5, log.getIpAddress());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Error en registerLog", e);
            return false;
        }
    }

    // Obtener todos los logs ordenados por fecha descendente
    public List<Log> getAllLogs() {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.usuario_id, l.accion, l.categoria, l.descripcion, " +
                "l.ip_address, l.fecha_creacion, u.name as usuario_nombre " +
                "FROM logs l " +
                "LEFT JOIN neon_auth.user u ON CAST(l.usuario_id AS TEXT) = u.id::TEXT " +
                "ORDER BY l.fecha_creacion DESC " +
                "LIMIT 1000";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Log log = new Log();
                log.setId(rs.getInt("id"));
                log.setUsuarioId(rs.getString("usuario_id"));
                log.setUsuarioNombre(rs.getString("usuario_nombre"));
                log.setAccion(rs.getString("accion"));
                log.setCategoria(rs.getString("categoria"));
                log.setDescripcion(rs.getString("descripcion"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setFechaCreacion(rs.getTimestamp("fecha_creacion"));

                logs.add(log);
            }

        } catch (SQLException e) {
            logger.error("Error en getAllLogs", e);
        }

        return logs;
    }

    // Obtener estadísticas de logs por categoría
    public int countLogsByCategory(String categoria) {
        String sql = "SELECT COUNT(*) as total FROM logs WHERE categoria = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoria);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            logger.error("Error en countLogsByCategory", e);
        }

        return 0;
    }

    // Obtener total de logs
    public int countAllLogs() {
        String sql = "SELECT COUNT(*) as total FROM logs";

        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            logger.error("Error en countAllLogs", e);
        }

        return 0;
    }

    // Eliminar todos los logs (Limpiar historial)
    public boolean deleteAllLogs() {
        String sql = "DELETE FROM logs";

        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            return true;

        } catch (SQLException e) {
            logger.error("Error en deleteAllLogs", e);
            return false;
        }
    }
}
