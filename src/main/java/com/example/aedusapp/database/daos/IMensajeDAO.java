package com.example.aedusapp.database.daos;

import com.example.aedusapp.models.Mensaje;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface IMensajeDAO {
    void createTable();
    void insertMessage(int ticketId, String from, String text, String imageUrl, String to, Integer ticketLinkId);
    void insertLocalMessage(int ticketId, String from, String text, String imageUrl, String audioUrl, boolean isSupport);
    void markAsRead(int ticketId, String currentUserId);
    void markDirectAsRead(String receiverId, String senderId);
    List<Mensaje> getMessages(int ticketId, int limit);
    List<Mensaje> getDirectMessages(String user1, String user2, int limit);
    Map<Integer, Timestamp> getAllTicketDates();
    Map<Integer, Integer> getAllTicketUnreadCounts(String userId);
    Map<Integer, String> getAllTicketLastMessages();
    Map<String, Timestamp> getAllContactDates(String currentUserId);
    Map<String, Integer> getAllContactUnreadCounts(String currentUserId);
    Map<String, String> getAllContactLastMessages(String currentUserId);
}
