package com.example.aedusapp.database;

import com.example.aedusapp.models.Log;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// DAO para gestionar los logs del sistema
public class LogDAO {

    // Registrar un nuevo log
    public boolean registrarLog(Log log) {
        String sql = "INSERT INTO logs (usuario_id, accion, categoria, descripcion, ip_address, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, log.getUsuarioId());
            stmt.setString(2, log.getAccion());
            stmt.setString(3, log.getCategoria());
            stmt.setString(4, log.getDescripcion());
            stmt.setString(5, log.getIpAddress());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Obtener todos los logs ordenados por fecha descendente
    public List<Log> obtenerTodosLogs() {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.usuario_id, l.accion, l.categoria, l.descripcion, " +
                "l.ip_address, l.fecha_creacion, u.nombre as usuario_nombre " +
                "FROM logs l " +
                "LEFT JOIN usuarios u ON l.usuario_id = u.id " +
                "ORDER BY l.fecha_creacion DESC " +
                "LIMIT 1000"; // Limitar a 1000 registros más recientes

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Log log = new Log();
                log.setId(rs.getInt("id"));
                log.setUsuarioId(rs.getInt("usuario_id"));
                log.setUsuarioNombre(rs.getString("usuario_nombre"));
                log.setAccion(rs.getString("accion"));
                log.setCategoria(rs.getString("categoria"));
                log.setDescripcion(rs.getString("descripcion"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setFechaCreacion(rs.getTimestamp("fecha_creacion"));

                logs.add(log);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    // Obtener logs filtrados por categoría
    public List<Log> obtenerLogsPorCategoria(String categoria) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.usuario_id, l.accion, l.categoria, l.descripcion, " +
                "l.ip_address, l.fecha_creacion, u.nombre as usuario_nombre " +
                "FROM logs l " +
                "LEFT JOIN usuarios u ON l.usuario_id = u.id " +
                "WHERE l.categoria = ? " +
                "ORDER BY l.fecha_creacion DESC " +
                "LIMIT 1000";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoria);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Log log = new Log();
                log.setId(rs.getInt("id"));
                log.setUsuarioId(rs.getInt("usuario_id"));
                log.setUsuarioNombre(rs.getString("usuario_nombre"));
                log.setAccion(rs.getString("accion"));
                log.setCategoria(rs.getString("categoria"));
                log.setDescripcion(rs.getString("descripcion"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setFechaCreacion(rs.getTimestamp("fecha_creacion"));

                logs.add(log);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    // Obtener logs por usuario
    public List<Log> obtenerLogsPorUsuario(int usuarioId) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.usuario_id, l.accion, l.categoria, l.descripcion, " +
                "l.ip_address, l.fecha_creacion, u.nombre as usuario_nombre " +
                "FROM logs l " +
                "LEFT JOIN usuarios u ON l.usuario_id = u.id " +
                "WHERE l.usuario_id = ? " +
                "ORDER BY l.fecha_creacion DESC " +
                "LIMIT 1000";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Log log = new Log();
                log.setId(rs.getInt("id"));
                log.setUsuarioId(rs.getInt("usuario_id"));
                log.setUsuarioNombre(rs.getString("usuario_nombre"));
                log.setAccion(rs.getString("accion"));
                log.setCategoria(rs.getString("categoria"));
                log.setDescripcion(rs.getString("descripcion"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setFechaCreacion(rs.getTimestamp("fecha_creacion"));

                logs.add(log);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    // Obtener logs por nombre de usuario (aproximado)
    public List<Log> obtenerLogsPorNombreUsuario(String nombre) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.usuario_id, l.accion, l.categoria, l.descripcion, " +
                "l.ip_address, l.fecha_creacion, u.nombre as usuario_nombre " +
                "FROM logs l " +
                "LEFT JOIN usuarios u ON l.usuario_id = u.id " +
                "WHERE u.nombre LIKE ? " +
                "ORDER BY l.fecha_creacion DESC " +
                "LIMIT 1000";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Log log = new Log();
                log.setId(rs.getInt("id"));
                log.setUsuarioId(rs.getInt("usuario_id"));
                log.setUsuarioNombre(rs.getString("usuario_nombre"));
                log.setAccion(rs.getString("accion"));
                log.setCategoria(rs.getString("categoria"));
                log.setDescripcion(rs.getString("descripcion"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setFechaCreacion(rs.getTimestamp("fecha_creacion"));

                logs.add(log);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }

    // Obtener estadísticas de logs por categoría
    public int contarLogsPorCategoria(String categoria) {
        String sql = "SELECT COUNT(*) as total FROM logs WHERE categoria = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoria);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // Obtener total de logs
    public int contarTodosLogs() {
        String sql = "SELECT COUNT(*) as total FROM logs";

        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // Obtener logs por rango de fechas
    public List<Log> obtenerLogsPorRangoFechas(java.util.Date inicio, java.util.Date fin) {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.usuario_id, l.accion, l.categoria, l.descripcion, " +
                "l.ip_address, l.fecha_creacion, u.nombre as usuario_nombre " +
                "FROM logs l " +
                "LEFT JOIN usuarios u ON l.usuario_id = u.id " +
                "WHERE l.fecha_creacion BETWEEN ? AND ? " +
                "ORDER BY l.fecha_creacion DESC";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(inicio.getTime()));
            // Ajustar fecha fin al final del día
            Timestamp fechaFin = new Timestamp(fin.getTime() + (24 * 60 * 60 * 1000) - 1);
            stmt.setTimestamp(2, fechaFin);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Log log = new Log();
                log.setId(rs.getInt("id"));
                log.setUsuarioId(rs.getInt("usuario_id"));
                log.setUsuarioNombre(rs.getString("usuario_nombre"));
                log.setAccion(rs.getString("accion"));
                log.setCategoria(rs.getString("categoria"));
                log.setDescripcion(rs.getString("descripcion"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setFechaCreacion(rs.getTimestamp("fecha_creacion"));

                logs.add(log);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return logs;
    }
}
