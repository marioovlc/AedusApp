package com.example.aedusapp.services.hub;

import com.example.aedusapp.database.daos.IncidenciaDAO;
import com.example.aedusapp.database.daos.MensajeDAO;
import com.example.aedusapp.database.daos.UsuarioDAO;
import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Mensaje;
import com.example.aedusapp.models.Usuario;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for Connect Hub business logic.
 * Decouples data fetching and processing from the UI controller.
 * Adheres to professional English naming and Clean Code standards.
 */
public class ConnectHubService {

    private final IncidenciaDAO incidenciaDAO;
    private final MensajeDAO mensajeDAO;
    private final UsuarioDAO usuarioDAO;

    public ConnectHubService() {
        this.incidenciaDAO = new IncidenciaDAO();
        this.mensajeDAO = new MensajeDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Fetches all relevant data for tickets based on user role.
     */
    public HubData<Incidencia> loadTicketData(Usuario usuarioActual) {
        List<Incidencia> tickets;
        if ("ADMIN".equalsIgnoreCase(usuarioActual.getRole()) || "MANTENIMIENTO".equalsIgnoreCase(usuarioActual.getRole())) {
            tickets = incidenciaDAO.getAllTickets();
        } else {
            tickets = incidenciaDAO.getTicketsByUser(usuarioActual.getId());
        }

        // Parallel batch metadata fetching
        CompletableFuture<Map<Integer, Timestamp>> datesFuture = CompletableFuture.supplyAsync(mensajeDAO::getAllTicketDates);
        CompletableFuture<Map<Integer, Integer>> unreadFuture = CompletableFuture.supplyAsync(() -> mensajeDAO.getAllTicketUnreadCounts(usuarioActual.getId()));
        CompletableFuture<Map<Integer, String>> msgsFuture = CompletableFuture.supplyAsync(mensajeDAO::getAllTicketLastMessages);

        CompletableFuture.allOf(datesFuture, unreadFuture, msgsFuture).join();

        return new HubData<>(tickets, datesFuture.join(), unreadFuture.join(), msgsFuture.join());
    }

    /**
     * Fetches all users and their chat metadata.
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
     * Marks all messages in a ticket as read.
     */
    public void markTicketAsRead(int ticketId, String currentUserId) {
        mensajeDAO.markAsRead(ticketId, currentUserId);
    }

    /**
     * Marks all direct messages from a sender as read.
     */
    public void markDirectMessagesAsRead(String currentUserId, String senderId) {
        mensajeDAO.markDirectAsRead(currentUserId, senderId);
    }

    /**
     * Gets chat history for a ticket.
     */
    public List<Mensaje> getTicketMessages(int ticketId, int limit) {
        return mensajeDAO.getMessages(ticketId, limit);
    }

    /**
     * Gets chat history for a direct conversation.
     */
    public List<Mensaje> getDirectMessages(String user1, String user2, int limit) {
        return mensajeDAO.getDirectMessages(user1, user2, limit);
    }

    /**
     * Sends a direct message with a ticket link.
     */
    public void sendDirectMessageWithTicket(String from, String to, String text, Integer ticketLinkId) {
        mensajeDAO.insertMessage(0, from, text, null, to, ticketLinkId);
    }

    /**
     * Sends a direct message with an optional attachment (audio/image).
     */
    public void sendDirectMessageWithAttachment(String from, String to, String text, String attachmentUrl) {
        if (attachmentUrl != null && attachmentUrl.toLowerCase().endsWith(".wav")) {
            // For voice messages, we use the local insert which handles audio_url
            mensajeDAO.insertLocalMessage(0, from, text, null, attachmentUrl, false); 
        } else {
             mensajeDAO.insertMessage(0, from, text, attachmentUrl, to, null);
        }
    }

    /**
     * Sends a ticket message.
     */
    public void sendTicketMessage(int ticketId, String from, String text, String imageUrl, String audioUrl, boolean isSupport) {
        mensajeDAO.insertLocalMessage(ticketId, from, text, imageUrl, audioUrl, isSupport);
    }

    /**
     * Updates user profile data.
     */
    public boolean actualizarUsuario(Usuario user) {
        return usuarioDAO.updateUser(user);
    }

    /**
     * Gets a ticket by its ID.
     */
    public Incidencia getTicketById(int ticketId) {
        return incidenciaDAO.getTicketById(ticketId);
    }

    /**
     * Updates the user's last connection timestamp.
     */
    public void updateUserPresence(String userId) {
        usuarioDAO.updateLastSeen(userId);
    }

    /**
     * Gets IDs of users active in the last N seconds.
     */
    public List<String> getRecentlyActiveUsers(int seconds) {
        return usuarioDAO.getRecentlyActiveUsers(seconds);
    }

    /**
     * Initializes presence table.
     */
    public void initPresenceSystem() {
        usuarioDAO.initPresenceSystem();
    }

    /**
     * Internal DTO for batch results.
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
