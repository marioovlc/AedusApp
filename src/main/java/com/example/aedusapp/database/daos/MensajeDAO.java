package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DatabaseHelper;
import com.example.aedusapp.models.Mensaje;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MensajeDAO implements IMensajeDAO {
    private static final Logger logger = LoggerFactory.getLogger(MensajeDAO.class);

    private final AchievementDAO achievementDAO;

    public MensajeDAO(AchievementDAO achievementDAO) {
        this.achievementDAO = achievementDAO;
    }

    // --- SQL CONSTANTS ---
    private static final String CREATE_TABLE = 
        "CREATE TABLE IF NOT EXISTS mensajes (" +
        "id SERIAL PRIMARY KEY, " +
        "incidencia_id INT REFERENCES incidencias(id) ON DELETE CASCADE, " +
        "usuario_id UUID REFERENCES neon_auth.user(id) ON DELETE CASCADE, " +
        "texto TEXT, " +
        "imagen_url VARCHAR(500), " +
        "audio_url VARCHAR(500), " +
        "fecha TIMESTAMP DEFAULT NOW(), " +
        "leido BOOLEAN DEFAULT FALSE, " +
        "is_soporte BOOLEAN DEFAULT FALSE, " +
        "receptor_id UUID REFERENCES neon_auth.user(id) ON DELETE CASCADE, " +
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
        "LEFT JOIN neon_auth.user u ON CAST(m.usuario_id AS TEXT) = CAST(u.id AS TEXT) " +
        "WHERE m.incidencia_id = ? ORDER BY m.fecha ASC ";
        
    private static final String GET_DIRECT_MESSAGES_BASE = 
        "SELECT m.*, u.name as nombre, u.foto_perfil_datos FROM mensajes m " +
        "JOIN neon_auth.user u ON CAST(m.usuario_id AS TEXT) = CAST(u.id AS TEXT) " +
        "WHERE ((CAST(m.usuario_id AS TEXT) = ? AND CAST(m.receptor_id AS TEXT) = ?) OR " +
        "      (CAST(m.usuario_id AS TEXT) = ? AND CAST(m.receptor_id AS TEXT) = ?)) " +
        "AND m.incidencia_id IS NULL ORDER BY m.fecha ASC ";

    private static final String GET_ALL_TICKET_DATES = "SELECT id as incidencia_id, ultima_actividad as ultima FROM incidencias";
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
        DatabaseHelper.executeUpdate(CREATE_TABLE);
        logger.info("Tabla 'mensajes' verificada/creada con éxito.");
    }

    public void insertMessage(int ticketId, String from, String text, String imageUrl, String to, Integer ticketLinkId) {
        DatabaseHelper.executeUpdate(INSERT_MESSAGE,
            ticketId > 0 ? ticketId : null,
            UUID.fromString(from),
            text != null ? text : "",
            imageUrl,
            to != null ? UUID.fromString(to) : null,
            ticketLinkId
        );
        
        achievementDAO.grantAchievement(from, "Colaborador");
    }

    public void insertLocalMessage(int ticketId, String from, String text, String imageUrl, String audioUrl, boolean isSupport) {
        DatabaseHelper.executeUpdate(INSERT_LOCAL_MESSAGE,
            ticketId > 0 ? ticketId : null,
            UUID.fromString(from),
            text != null ? text : "",
            imageUrl,
            audioUrl,
            isSupport
        );
    }

    public void markAsRead(int ticketId, String currentUserId) {
        DatabaseHelper.executeUpdate(MARK_AS_READ, ticketId, currentUserId);
    }

    public void markDirectAsRead(String receiverId, String senderId) {
        DatabaseHelper.executeUpdate(MARK_DIRECT_AS_READ, senderId, receiverId);
    }

    public List<Mensaje> getMessages(int ticketId, int limit) {
        String query = GET_MESSAGES_BASE + (limit > 0 ? "LIMIT " + limit : "");
        return DatabaseHelper.queryForList(query, this::mapResultSetToMensaje, ticketId);
    }

    public List<Mensaje> getDirectMessages(String user1, String user2, int limit) {
        String query = GET_DIRECT_MESSAGES_BASE + (limit > 0 ? "LIMIT " + limit : "");
        return DatabaseHelper.queryForList(query, this::mapResultSetToMensaje, user1, user2, user2, user1);
    }

    private Mensaje mapResultSetToMensaje(ResultSet rs) throws SQLException {
        String nombre = rs.getString("nombre");
        if (nombre == null) nombre = "Usuario"; // Fallback
        
        Mensaje m = new Mensaje(
                rs.getInt("id"),
                rs.getInt("incidencia_id"),
                rs.getString("usuario_id"),
                nombre,
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
        DatabaseHelper.queryForList(GET_ALL_TICKET_DATES, rs -> map.put(rs.getInt("incidencia_id"), rs.getTimestamp("ultima")));
        return map;
    }

    public Map<Integer, Integer> getAllTicketUnreadCounts(String userId) {
        Map<Integer, Integer> map = new HashMap<>();
        DatabaseHelper.queryForList(GET_ALL_TICKET_UNREAD_COUNTS, rs -> map.put(rs.getInt("incidencia_id"), rs.getInt("cuenta")), userId);
        return map;
    }

    public Map<Integer, String> getAllTicketLastMessages() {
        Map<Integer, String> map = new HashMap<>();
        DatabaseHelper.queryForList(GET_ALL_TICKET_LAST_MESSAGES, rs -> map.put(rs.getInt("incidencia_id"), rs.getString("texto")));
        return map;
    }

    public Map<String, Timestamp> getAllContactDates(String currentUserId) {
        Map<String, Timestamp> map = new HashMap<>();
        DatabaseHelper.queryForList(GET_ALL_CONTACT_DATES, rs -> map.put(rs.getString("contacto_id"), rs.getTimestamp("ultima")), currentUserId, currentUserId, currentUserId);
        return map;
    }

    public Map<String, Integer> getAllContactUnreadCounts(String currentUserId) {
        Map<String, Integer> map = new HashMap<>();
        DatabaseHelper.queryForList(GET_ALL_CONTACT_UNREAD_COUNTS, rs -> map.put(rs.getString("emisor_id"), rs.getInt("cuenta")), currentUserId);
        return map;
    }

    public Map<String, String> getAllContactLastMessages(String currentUserId) {
        Map<String, String> map = new HashMap<>();
        DatabaseHelper.queryForList(GET_ALL_CONTACT_LAST_MESSAGES, rs -> map.put(rs.getString("contacto_id"), rs.getString("texto")), currentUserId, currentUserId, currentUserId);
        return map;
    }
}
