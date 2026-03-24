package com.example.aedusapp.services.hub;

import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Mensaje;
import com.example.aedusapp.models.Usuario;
import java.util.List;

public interface IConnectHubService {
    ConnectHubService.HubData<Incidencia> loadTicketData(Usuario usuarioActual);
    ConnectHubService.HubData<Usuario> loadUserData(Usuario usuarioActual);
    void markTicketAsRead(int ticketId, String currentUserId);
    void markDirectMessagesAsRead(String currentUserId, String senderId);
    List<Mensaje> getTicketMessages(int ticketId, int limit);
    List<Mensaje> getDirectMessages(String user1, String user2, int limit);
    void sendDirectMessageWithTicket(String from, String to, String text, Integer ticketLinkId);
    void sendDirectMessageWithAttachment(String from, String to, String text, String attachmentUrl);
    void sendTicketMessage(int ticketId, String from, String text, String imageUrl, String audioUrl, boolean isSupport);
    boolean actualizarUsuario(Usuario user);
    Incidencia getTicketById(int ticketId);
    void updateUserPresence(String userId);
    List<String> getRecentlyActiveUsers(int seconds);
    void initPresenceSystem();
}
