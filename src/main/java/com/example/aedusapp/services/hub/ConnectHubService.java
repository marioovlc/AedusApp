package com.example.aedusapp.services.hub;

import com.example.aedusapp.database.daos.IncidenciaDAO;
import com.example.aedusapp.database.daos.MensajeDAO;
import com.example.aedusapp.database.daos.AchievementDAO;
import com.example.aedusapp.database.daos.UsuarioDAO;
import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Mensaje;
import com.example.aedusapp.models.Usuario;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// =============================================
// ==== CLASE CONNECTHUBSERVICE =====
// Descripción: Servicio de negocio para el centro de comunicación
// Connect Hub. Desacopla la lógica de mensajería y datos de la UI.
// =============================================
public class ConnectHubService implements IConnectHubService {

    private final IncidenciaDAO incidenciaDAO;
    private final MensajeDAO mensajeDAO;
    private final UsuarioDAO usuarioDAO;

    public ConnectHubService() {
        this.incidenciaDAO = new IncidenciaDAO();
        this.mensajeDAO = new MensajeDAO(new AchievementDAO());
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Obtiene todos los datos relevantes de los tickets según el rol del usuario.
     */
    public HubData<Incidencia> loadTicketData(Usuario usuarioActual) {
        List<Incidencia> tickets;
        if ("ADMIN".equalsIgnoreCase(usuarioActual.getRole()) || "MANTENIMIENTO".equalsIgnoreCase(usuarioActual.getRole())) {
            tickets = incidenciaDAO.getAllTickets();
        } else {
            tickets = incidenciaDAO.getTicketsByUser(usuarioActual.getId());
        }

        // Obtención en paralelo de metadatos por lotes
        CompletableFuture<Map<Integer, Timestamp>> datesFuture = CompletableFuture.supplyAsync(mensajeDAO::getAllTicketDates);
        CompletableFuture<Map<Integer, Integer>> unreadFuture = CompletableFuture.supplyAsync(() -> mensajeDAO.getAllTicketUnreadCounts(usuarioActual.getId()));
        CompletableFuture<Map<Integer, String>> msgsFuture = CompletableFuture.supplyAsync(mensajeDAO::getAllTicketLastMessages);

        CompletableFuture.allOf(datesFuture, unreadFuture, msgsFuture).join();

        return new HubData<>(tickets, datesFuture.join(), unreadFuture.join(), msgsFuture.join());
    }

    /**
     * Obtiene todos los usuarios y sus metadatos de chat.
     */
    public HubData<Usuario> loadUserData(Usuario usuarioActual) {
        List<Usuario> users = usuarioDAO.getAllUsers();

        CompletableFuture<Map<String, Timestamp>> datesFuture = CompletableFuture.supplyAsync(() -> mensajeDAO.getAllContactDates(usuarioActual.getId()));
        CompletableFuture<Map<String, Integer>> unreadFuture = CompletableFuture.supplyAsync(() -> mensajeDAO.getAllContactUnreadCounts(usuarioActual.getId()));
        CompletableFuture<Map<String, String>> msgsFuture = CompletableFuture.supplyAsync(() -> mensajeDAO.getAllContactLastMessages(usuarioActual.getId()));

        CompletableFuture.allOf(datesFuture, unreadFuture, msgsFuture).join();

        return new HubData<>(users, datesFuture.join(), unreadFuture.join(), msgsFuture.join());
    }

    /**
     * Marca todos los mensajes de un ticket como leídos.
     */
    public void markTicketAsRead(int ticketId, String currentUserId) {
        mensajeDAO.markAsRead(ticketId, currentUserId);
    }

    /**
     * Marca todos los mensajes directos de un remitente como leídos.
     */
    public void markDirectMessagesAsRead(String currentUserId, String senderId) {
        mensajeDAO.markDirectAsRead(currentUserId, senderId);
    }

    /**
     * Obtiene el historial de chat para un ticket.
     */
    public List<Mensaje> getTicketMessages(int ticketId, int limit) {
        return mensajeDAO.getMessages(ticketId, limit);
    }

    /**
     * Obtiene el historial de chat para una conversación directa.
     */
    public List<Mensaje> getDirectMessages(String user1, String user2, int limit) {
        return mensajeDAO.getDirectMessages(user1, user2, limit);
    }

    /**
     * Envía un mensaje directo con un enlace a un ticket.
     */
    public void sendDirectMessageWithTicket(String from, String to, String text, Integer ticketLinkId) {
        mensajeDAO.insertMessage(0, from, text, null, to, ticketLinkId);
    }

    /**
     * Envía un mensaje directo con un adjunto opcional (audio/imagen).
     */
    public void sendDirectMessageWithAttachment(String from, String to, String text, String attachmentUrl) {
        if (attachmentUrl != null && attachmentUrl.toLowerCase().endsWith(".wav")) {
            // Para mensajes de voz, usamos la inserción local que maneja audio_url
            mensajeDAO.insertLocalMessage(0, from, text, null, attachmentUrl, false); 
        } else {
             mensajeDAO.insertMessage(0, from, text, attachmentUrl, to, null);
        }
    }

    /**
     * Envía un mensaje en un ticket.
     */
    public void sendTicketMessage(int ticketId, String from, String text, String imageUrl, String audioUrl, boolean isSupport) {
        mensajeDAO.insertComentarioIncidencia(ticketId, from, text, isSupport);
    }

    /**
     * Actualiza los datos de perfil de un usuario.
     */
    public boolean actualizarUsuario(Usuario user) {
        return usuarioDAO.updateUser(user);
    }

    /**
     * Obtiene un ticket por su ID.
     */
    public Incidencia getTicketById(int ticketId) {
        return incidenciaDAO.getTicketById(ticketId);
    }

    /**
     * Actualiza la marca de tiempo de última conexión del usuario.
     */
    public void updateUserPresence(String userId) {
        usuarioDAO.updateLastSeen(userId);
    }

    /**
     * Obtiene los IDs de los usuarios activos en los últimos N segundos.
     */
    public List<String> getRecentlyActiveUsers(int seconds) {
        return usuarioDAO.getRecentlyActiveUsers(seconds);
    }

    /**
     * Inicializa la tabla o sistema de presencia.
     */
    public void initPresenceSystem() {
        usuarioDAO.initPresenceSystem();
    }

    /**
     * DTO interno para resultados por lotes.
     */
    public static class HubData<T> {
        public final List<T> items;
        public final Map<?, Timestamp> dates;
        public final Map<?, Integer> unreadCounts;
        public final Map<?, String> lastMessages;

        public HubData(List<T> items, Map<?, Timestamp> dates, Map<?, Integer> unreadCounts, Map<?, String> lastMessages) {
            this.items = items;
            this.dates = dates;
            this.unreadCounts = unreadCounts;
            this.lastMessages = lastMessages;
        }
    }
}
