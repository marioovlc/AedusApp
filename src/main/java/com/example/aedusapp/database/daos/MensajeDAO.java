package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;
import com.example.aedusapp.exceptions.DatabaseException;
import com.example.aedusapp.models.Mensaje;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MensajeDAO {

    // --- SQL CONSTANTS ---
    private static final String CREATE_TABLE = 
        "CREATE TABLE IF NOT EXISTS mensajes (" +
        "id SERIAL PRIMARY KEY, " +
        "incidencia_id INT REFERENCES incidencias(id) ON DELETE CASCADE, " +
        "usuario_id UUID REFERENCES neon_auth.user(id), " +
        "texto TEXT, " +
        "imagen_url VARCHAR(500), " +
        "audio_url VARCHAR(500), " +
        "fecha TIMESTAMP DEFAULT NOW(), " +
        "leido BOOLEAN DEFAULT FALSE, " +
        "is_soporte BOOLEAN DEFAULT FALSE, " +
        "receptor_id UUID REFERENCES neon_auth.user(id), " +
        "ticket_link_id INT REFERENCES incidencias(id))";

    private static final String INSERT_MESSAGE = 
        "INSERT INTO mensajes (incidencia_id, usuario_id, texto, imagen_url, receptor_id, ticket_link_id) VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_LOCAL_MESSAGE = 
        "INSERT INTO mensajes (incidencia_id, usuario_id, texto, imagen_url, audio_url, is_soporte) VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String MARK_AS_READ = 
        "UPDATE mensajes SET leido = true WHERE incidencia_id = ? AND CAST(usuario_id AS TEXT) != ?";
    
    private static final String MARK_DIRECT_AS_READ = 
        "UPDATE mensajes SET leido = true WHERE CAST(usuario_id AS TEXT) = ? AND CAST(receptor_id AS TEXT) = ? AND leido = false AND incidencia_id IS NULL";
    
    private static final String GET_MESSAGES_BASE = 
        "SELECT m.*, u.name as nombre, u.foto_perfil_datos FROM mensajes m " +
        "JOIN neon_auth.user u ON CAST(m.usuario_id AS TEXT) = CAST(u.id AS TEXT) " +
        "WHERE m.incidencia_id = ? ORDER BY m.fecha ASC ";
        
    private static final String GET_DIRECT_MESSAGES_BASE = 
        "SELECT m.*, u.name as nombre, u.foto_perfil_datos FROM mensajes m " +
        "JOIN neon_auth.user u ON CAST(m.usuario_id AS TEXT) = CAST(u.id AS TEXT) " +
        "WHERE ((CAST(m.usuario_id AS TEXT) = ? AND CAST(m.receptor_id AS TEXT) = ?) OR " +
        "      (CAST(m.usuario_id AS TEXT) = ? AND CAST(m.receptor_id AS TEXT) = ?)) " +
        "AND m.incidencia_id IS NULL ORDER BY m.fecha ASC ";

    private static final String GET_ALL_TICKET_DATES = "SELECT incidencia_id, MAX(fecha) as ultima FROM mensajes WHERE incidencia_id IS NOT NULL GROUP BY incidencia_id";
    private static final String GET_ALL_TICKET_UNREAD_COUNTS = "SELECT incidencia_id, COUNT(*) as cuenta FROM mensajes WHERE incidencia_id IS NOT NULL AND CAST(usuario_id AS TEXT) != ? AND leido = false GROUP BY incidencia_id";
    private static final String GET_ALL_TICKET_LAST_MESSAGES = "SELECT DISTINCT ON (incidencia_id) incidencia_id, texto FROM mensajes WHERE incidencia_id IS NOT NULL ORDER BY incidencia_id, fecha DESC";
    
    private static final String GET_ALL_CONTACT_DATES = 
        "SELECT CASE WHEN CAST(usuario_id AS TEXT) = ? THEN CAST(receptor_id AS TEXT) ELSE CAST(usuario_id AS TEXT) END as contacto_id, " +
        "MAX(fecha) as ultima FROM mensajes WHERE incidencia_id IS NULL AND (CAST(usuario_id AS TEXT) = ? OR CAST(receptor_id AS TEXT) = ?) GROUP BY contacto_id";
        
    private static final String GET_ALL_CONTACT_UNREAD_COUNTS = 
        "SELECT CAST(usuario_id AS TEXT) as emisor_id, COUNT(*) as cuenta FROM mensajes WHERE incidencia_id IS NULL AND CAST(receptor_id AS TEXT) = ? AND leido = false GROUP BY emisor_id";
        
    private static final String GET_ALL_CONTACT_LAST_MESSAGES = 
        "SELECT DISTINCT ON (contacto_id) CASE WHEN CAST(usuario_id AS TEXT) = ? THEN CAST(receptor_id AS TEXT) ELSE CAST(usuario_id AS TEXT) END as contacto_id, " +
        "texto FROM mensajes WHERE incidencia_id IS NULL AND (CAST(usuario_id AS TEXT) = ? OR CAST(receptor_id AS TEXT) = ?) ORDER BY contacto_id, fecha DESC";


    public void createTable() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(CREATE_TABLE);
            System.out.println("Table 'mensajes' verified/created successfully.");
        } catch (SQLException e) {
            throw new DatabaseException("Error creando tabla 'mensajes'", e);
        }
    }

    public void insertMessage(int ticketId, String from, String text, String imageUrl, String to, Integer ticketLinkId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_MESSAGE)) {
            
            if (ticketId > 0) pstmt.setInt(1, ticketId); else pstmt.setNull(1, java.sql.Types.INTEGER);
            pstmt.setObject(2, UUID.fromString(from));
            pstmt.setString(3, text != null ? text : "");
            pstmt.setString(4, imageUrl);
            if (to != null) pstmt.setObject(5, UUID.fromString(to)); else pstmt.setNull(5, java.sql.Types.OTHER);
            if (ticketLinkId != null) pstmt.setInt(6, ticketLinkId); else pstmt.setNull(6, java.sql.Types.INTEGER);
            
            pstmt.executeUpdate();
            new AchievementDAO().grantAchievement(from, "Colaborador");
        } catch (SQLException e) {
            throw new DatabaseException("Error insertando mensaje", e);
        }
    }

    public void insertLocalMessage(int ticketId, String from, String text, String imageUrl, String audioUrl, boolean isSupport) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_LOCAL_MESSAGE)) {
            
            if (ticketId > 0) pstmt.setInt(1, ticketId); else pstmt.setNull(1, java.sql.Types.INTEGER);
            pstmt.setObject(2, UUID.fromString(from));
            pstmt.setString(3, text != null ? text : "");
            pstmt.setString(4, imageUrl);
            pstmt.setString(5, audioUrl);
            pstmt.setBoolean(6, isSupport);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error insertando mensaje local/soporte", e);
        }
    }

    public void markAsRead(int ticketId, String currentUserId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(MARK_AS_READ)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, currentUserId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error marcando mensajes de ticket como leídos", e);
        }
    }

    public void markDirectAsRead(String receiverId, String senderId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(MARK_DIRECT_AS_READ)) {
            pstmt.setString(1, senderId);
            pstmt.setString(2, receiverId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error marcando mensajes directos como leídos", e);
        }
    }

    public List<Mensaje> getMessages(int ticketId, int limit) {
        List<Mensaje> list = new ArrayList<>();
        String query = GET_MESSAGES_BASE + (limit > 0 ? "LIMIT " + limit : "");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToMensaje(rs));
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error obteniendo mensajes del ticket", e);
        }
        return list;
    }

    public List<Mensaje> getDirectMessages(String user1, String user2, int limit) {
        List<Mensaje> list = new ArrayList<>();
        String query = GET_DIRECT_MESSAGES_BASE + (limit > 0 ? "LIMIT " + limit : "");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user1);
            stmt.setString(2, user2);
            stmt.setString(3, user2);
            stmt.setString(4, user1);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToMensaje(rs));
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error obteniendo mensajes directos", e);
        }
        return list;
    }

    private Mensaje mapResultSetToMensaje(ResultSet rs) throws SQLException {
        Mensaje m = new Mensaje(
                rs.getInt("id"),
                rs.getInt("incidencia_id"),
                rs.getString("usuario_id"),
                rs.getString("nombre"),
                rs.getBytes("foto_perfil_datos"),
                rs.getString("texto"),
                rs.getString("imagen_url"),
                rs.getTimestamp("fecha"),
                rs.getBoolean("leido"),
                rs.getBoolean("is_soporte")
        );
        m.setAudioUrl(rs.getString("audio_url"));
        m.setReceptorId(rs.getString("receptor_id"));
        int linkId = rs.getInt("ticket_link_id");
        if (!rs.wasNull()) m.setTicketLinkId(linkId);
        return m;
    }

    // --- BATCH FETCHING FOR PERFORMANCE ---

    public Map<Integer, Timestamp> getAllTicketDates() {
        Map<Integer, Timestamp> map = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(GET_ALL_TICKET_DATES)) {
            while (rs.next()) map.put(rs.getInt("incidencia_id"), rs.getTimestamp("ultima"));
        } catch (SQLException e) { 
            throw new DatabaseException("Error obteniendo fechas de tickets", e);
        }
        return map;
    }

    public Map<Integer, Integer> getAllTicketUnreadCounts(String userId) {
        Map<Integer, Integer> map = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(GET_ALL_TICKET_UNREAD_COUNTS)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) map.put(rs.getInt("incidencia_id"), rs.getInt("cuenta"));
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error obteniendo recuentos de no leídos", e);
        }
        return map;
    }

    public Map<Integer, String> getAllTicketLastMessages() {
        Map<Integer, String> map = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(GET_ALL_TICKET_LAST_MESSAGES)) {
            while (rs.next()) map.put(rs.getInt("incidencia_id"), rs.getString("texto"));
        } catch (SQLException e) { 
            throw new DatabaseException("Error obteniendo últimos mensajes", e);
        }
        return map;
    }

    public Map<String, Timestamp> getAllContactDates(String currentUserId) {
        Map<String, Timestamp> map = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(GET_ALL_CONTACT_DATES)) {
            pstmt.setString(1, currentUserId);
            pstmt.setString(2, currentUserId);
            pstmt.setString(3, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) map.put(rs.getString("contacto_id"), rs.getTimestamp("ultima"));
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error obteniendo fechas de contactos", e);
        }
        return map;
    }

    public Map<String, Integer> getAllContactUnreadCounts(String currentUserId) {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(GET_ALL_CONTACT_UNREAD_COUNTS)) {
            pstmt.setString(1, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) map.put(rs.getString("emisor_id"), rs.getInt("cuenta"));
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error obteniendo recuento de no leídos de contactos", e);
        }
        return map;
    }

    public Map<String, String> getAllContactLastMessages(String currentUserId) {
        Map<String, String> map = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(GET_ALL_CONTACT_LAST_MESSAGES)) {
            pstmt.setString(1, currentUserId);
            pstmt.setString(2, currentUserId);
            pstmt.setString(3, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) map.put(rs.getString("contacto_id"), rs.getString("texto"));
            }
        } catch (SQLException e) { 
            throw new DatabaseException("Error obteniendo últimos mensajes de contactos", e);
        }
        return map;
    }
}
