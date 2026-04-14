package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;
import com.example.aedusapp.exceptions.DatabaseException;
import com.example.aedusapp.models.Incidencia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

public class IncidenciaDAO {

    // --- SQL CONSTANTS ---
    private static final String CREATE_TICKET = 
        "INSERT INTO incidencias (titulo, descripcion, usuario_id, aula_id, categoria_id, aula_tipo, estado_id, fecha_creacion, imagen_url) " +
        "VALUES (?, ?, ?::uuid, ?, ?, ?, (SELECT id FROM estados WHERE nombre = 'NO LEIDO'), NOW(), ?)";

    private static final String GET_TICKET_BY_ID = 
        "SELECT i.id, i.titulo, i.descripcion, i.fecha_creacion, i.imagen_url, i.aula_tipo, i.resolucion, i.usuario_id," +
        "e.nombre as estado_nombre, c.nombre as categoria_nombre, a.nombre as aula_nombre " +
        "FROM incidencias i " +
        "JOIN estados e ON i.estado_id = e.id JOIN categorias c ON i.categoria_id = c.id JOIN aulas a ON i.aula_id = a.id " +
        "WHERE i.id = ?";

    private static final String GET_TICKETS_PAGINATED = 
        "SELECT i.id, i.titulo, i.descripcion, i.fecha_creacion, i.imagen_url, i.aula_tipo, i.resolucion, " +
        "e.nombre as estado_nombre, c.nombre as categoria_nombre, a.nombre as aula_nombre " +
        "FROM incidencias i " +
        "JOIN estados e ON i.estado_id = e.id JOIN categorias c ON i.categoria_id = c.id JOIN aulas a ON i.aula_id = a.id " +
        "WHERE i.usuario_id = ?::uuid ORDER BY i.fecha_creacion DESC LIMIT ? OFFSET ?";

    private static final String GET_ALL_TICKETS = 
        "SELECT i.id, i.titulo, i.descripcion, i.fecha_creacion, i.imagen_url, i.resolucion, i.usuario_id, " +
        "e.nombre as estado_nombre, u.name as usuario_nombre " +
        "FROM incidencias i " +
        "JOIN estados e ON i.estado_id = e.id LEFT JOIN neon_auth.user u ON CAST(i.usuario_id AS TEXT) = u.id::TEXT " +
        "ORDER BY CASE WHEN e.nombre = 'NO LEIDO' THEN 0 ELSE 1 END, i.fecha_creacion DESC";

    private static final String UPDATE_STATUS = 
        "UPDATE incidencias SET estado_id = (SELECT id FROM estados WHERE UPPER(nombre) = UPPER(?)) WHERE id = ?";

    private static final String UPDATE_RESOLUTION = 
        "UPDATE incidencias SET resolucion = ?, estado_id = (SELECT id FROM estados WHERE UPPER(nombre) = UPPER(?)) WHERE id = ?";

    private static final String DELETE_TICKET = "DELETE FROM incidencias WHERE id = ?";
    private static final String DELETE_ALL_TICKETS = "DELETE FROM incidencias";
    private static final String NULLIFY_TICKET_LINKS = "UPDATE mensajes SET ticket_link_id = NULL WHERE ticket_link_id = ?";
    private static final String NULLIFY_ALL_TICKET_LINKS = "UPDATE mensajes SET ticket_link_id = NULL";
    private static final String COUNT_TOTAL = "SELECT COUNT(*) as total FROM incidencias";
    private static final String STATS_STATUS = "SELECT e.nombre, COUNT(i.id) as total FROM incidencias i JOIN estados e ON i.estado_id = e.id GROUP BY e.nombre";
    private static final String STATS_CATEGORY = "SELECT c.nombre, COUNT(i.id) as total FROM incidencias i JOIN categorias c ON i.categoria_id = c.id GROUP BY c.nombre";
    
    private static final String STATS_DAYS = 
        "SELECT DATE(fecha_creacion) as fecha, COUNT(*) as total FROM incidencias " +
        "WHERE fecha_creacion >= CURRENT_DATE - (? * INTERVAL '1 DAY') " +
        "GROUP BY DATE(fecha_creacion) ORDER BY fecha";

    private static final String GET_TICKETS_BY_USER = 
        "SELECT i.id, i.titulo, i.descripcion, i.fecha_creacion, i.imagen_url, i.aula_tipo, i.resolucion, i.usuario_id, " +
        "e.nombre as estado_nombre, c.nombre as categoria_nombre, a.nombre as aula_nombre " +
        "FROM incidencias i " +
        "JOIN estados e ON i.estado_id = e.id JOIN categorias c ON i.categoria_id = c.id JOIN aulas a ON i.aula_id = a.id " +
        "WHERE i.usuario_id = ?::uuid ORDER BY i.fecha_creacion DESC";

    // Optimized Single N+1 Query Fix
    private static final String CHECK_SOLUCIONADOR = 
        "SELECT i1.usuario_id, (SELECT COUNT(*) FROM incidencias i2 JOIN estados e ON i2.estado_id = e.id WHERE i2.usuario_id = i1.usuario_id AND e.nombre = 'ACABADO') as resueltas " +
        "FROM incidencias i1 WHERE i1.id = ?";


    public boolean createTicket(Incidencia incidencia) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CREATE_TICKET)) {

            stmt.setString(1, incidencia.getTitulo());
            stmt.setString(2, incidencia.getDescripcion());
            stmt.setString(3, incidencia.getUsuarioId());
            stmt.setInt(4, incidencia.getAulaId());
            stmt.setInt(5, incidencia.getCategoriaId());
            stmt.setString(6, incidencia.getAulaTipo());
            stmt.setString(7, incidencia.getImagenUrl());

            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                new AchievementDAO().grantAchievement(incidencia.getUsuarioId(), "Primer Paso");
            }
            return success;
        } catch (SQLException e) {
            throw new DatabaseException("Error creando ticket", e);
        }
    }

    public Incidencia getTicketById(int incidenciaId) {
        Incidencia inc = null;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TICKET_BY_ID)) {
            
            stmt.setInt(1, incidenciaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    inc = mapResultSetToIncidencia(rs, true);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo ticket por ID: " + incidenciaId, e);
        }
        return inc;
    }

    public List<Incidencia> getTicketsByUserPaginated(String usuarioId, int limit, int offset) {
        List<Incidencia> incidencias = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TICKETS_PAGINATED)) {

            stmt.setString(1, usuarioId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    incidencias.add(mapResultSetToIncidencia(rs, false));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo tickets paginados para usuario: " + usuarioId, e);
        }
        return incidencias;
    }

    public List<Incidencia> getAllTickets() {
        List<Incidencia> incidencias = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ALL_TICKETS);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Incidencia inc = new Incidencia();
                inc.setId(rs.getInt("id"));
                inc.setTitulo(rs.getString("titulo"));
                inc.setDescripcion(rs.getString("descripcion"));
                inc.setEstado(rs.getString("estado_nombre"));
                inc.setResolucion(rs.getString("resolucion"));
                inc.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                inc.setImagenUrl(rs.getString("imagen_url"));
                inc.setCreadorNombre(rs.getString("usuario_nombre"));
                inc.setUsuarioId(rs.getString("usuario_id"));
                incidencias.add(inc);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo todos los tickets", e);
        }
        return incidencias;
    }

    public boolean updateStatus(int incidenciaId, String nuevoEstado) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_STATUS)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, incidenciaId);
            boolean success = stmt.executeUpdate() > 0;
            if (success && "ACABADO".equalsIgnoreCase(nuevoEstado)) {
                checkSolucionadorAchievement(incidenciaId);
            }
            return success;
        } catch (SQLException e) {
            throw new DatabaseException("Error actualizando estado del ticket a: " + nuevoEstado, e);
        }
    }

    public boolean updateResolution(int incidenciaId, String resolucion, String nuevoEstado) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_RESOLUTION)) {

            stmt.setString(1, resolucion);
            stmt.setString(2, nuevoEstado);
            stmt.setInt(3, incidenciaId);
            boolean success = stmt.executeUpdate() > 0;
            if (success && "ACABADO".equalsIgnoreCase(nuevoEstado)) {
                checkSolucionadorAchievement(incidenciaId);
            }
            return success;
        } catch (SQLException e) {
            throw new DatabaseException("Error actualizando resolución del ticket", e);
        }
    }

    public boolean deleteTicket(int incidenciaId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement nullifyStmt = conn.prepareStatement(NULLIFY_TICKET_LINKS);
             PreparedStatement stmt = conn.prepareStatement(DELETE_TICKET)) {
            
            nullifyStmt.setInt(1, incidenciaId);
            nullifyStmt.executeUpdate();
            
            stmt.setInt(1, incidenciaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error eliminando ticket: " + incidenciaId, e);
        }
    }

    public boolean deleteAllTickets() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
             
            stmt.executeUpdate(NULLIFY_ALL_TICKET_LINKS);
            stmt.executeUpdate(DELETE_ALL_TICKETS);
            return true;
        } catch (SQLException e) {
            throw new DatabaseException("Error eliminando todos los tickets", e);
        }
    }

    public int countTotalTickets() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_TOTAL)) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            throw new DatabaseException("Error contando los tickets totales", e);
        }
        return 0;
    }

    public Map<String, Integer> getStatusStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(STATS_STATUS)) {
            while (rs.next()) stats.put(rs.getString("nombre"), rs.getInt("total"));
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo estadísticas de estado", e);
        }
        return stats;
    }

    public Map<String, Integer> getCategoryStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(STATS_CATEGORY)) {
            while (rs.next()) stats.put(rs.getString("nombre"), rs.getInt("total"));
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo estadísticas de categoría", e);
        }
        return stats;
    }

    public Map<String, Integer> getTicketsByDays(int dias) {
        Map<String, Integer> stats = new TreeMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(STATS_DAYS)) {
            stmt.setInt(1, dias);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) stats.put(rs.getDate("fecha").toString(), rs.getInt("total"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo estadísticas por días", e);
        }
        return stats;
    }

    public List<Incidencia> getTicketsByUser(String usuarioId) {
        List<Incidencia> incidencias = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TICKETS_BY_USER)) {
            stmt.setString(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) incidencias.add(mapResultSetToIncidencia(rs, true));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo tickets para el usuario", e);
        }
        return incidencias;
    }

    private void checkSolucionadorAchievement(int incidenciaId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_SOLUCIONADOR)) {
            stmt.setInt(1, incidenciaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String userId = rs.getString("usuario_id");
                    if (rs.getInt("resueltas") >= 5) {
                        new AchievementDAO().grantAchievement(userId, "Solucionador");
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error procesando logros de resolución de ticket", e);
        }
    }

    private Incidencia mapResultSetToIncidencia(ResultSet rs, boolean complete) throws SQLException {
        Incidencia inc = new Incidencia();
        inc.setId(rs.getInt("id"));
        inc.setTitulo(rs.getString("titulo"));
        inc.setDescripcion(rs.getString("descripcion"));
        inc.setEstado(rs.getString("estado_nombre"));
        inc.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        inc.setImagenUrl(rs.getString("imagen_url"));
        inc.setCategoriaNombre(rs.getString("categoria_nombre"));
        inc.setAulaNombre(rs.getString("aula_nombre"));
        inc.setAulaTipo(rs.getString("aula_tipo"));
        inc.setResolucion(rs.getString("resolucion"));
        if (complete) {
            inc.setUsuarioId(rs.getString("usuario_id"));
        }
        return inc;
    }
}
