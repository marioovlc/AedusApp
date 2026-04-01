package com.example.aedusapp.utils.hub;

import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Mensaje;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.audio.AudioRecorderService;
import com.example.aedusapp.services.hub.IConnectHubService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.text.SimpleDateFormat;
import java.util.function.Consumer;

/**
 * Encapsula la lógica de renderizado de burbujas de chat.
 * Extraído de ConnectHubController para mejorar la mantenibilidad.
 */
public class MessageRenderer {

    public static void render(Mensaje m, 
                              VBox chatContainer, 
                              Usuario usuarioActual, 
                              Incidencia incidenciaActual,
                              IConnectHubService hubService,
                              AudioRecorderService audioService,
                              Consumer<Incidencia> onTicketSelect) {
        
        boolean isMe = m.getUsuarioId() != null && usuarioActual != null
                && m.getUsuarioId().equals(usuarioActual.getId());

        HBox row = new HBox(10);
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Avatar
        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(32);
        avatarView.setFitHeight(32);
        Circle clip = new Circle(16, 16, 16);
        avatarView.setClip(clip);

        if (m.getAvatarDatos() != null) {
            try {
                Image img = new Image(new java.io.ByteArrayInputStream(m.getAvatarDatos()));
                avatarView.setImage(img);
            } catch (Exception ignored) {}
        }

        // Bubble
        VBox bubble = new VBox(5);
        bubble.getStyleClass().add("chat-bubble");
        
        if (m.isSoporte()) {
            bubble.getStyleClass().add("chat-bubble-support");
        } else {
            bubble.getStyleClass().add(isMe ? "chat-bubble-me" : "chat-bubble-other");
        }

        if (!isMe) {
            Label name = new Label(m.getNombre() != null ? m.getNombre() : "Usuario");
            name.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            bubble.getChildren().add(name);

            // Mark as read in background
            if (!m.isLeido() && !"system".equals(m.getUsuarioId()) && incidenciaActual != null) {
                com.example.aedusapp.utils.ConcurrencyManager.submit(() -> 
                    hubService.markTicketAsRead(incidenciaActual.getId(), usuarioActual.getId()));
            }
        }

        if (m.getTexto() != null && !m.getTexto().isEmpty()) {
            Label txt = new Label(m.getTexto());
            txt.setWrapText(true);
            txt.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 13px;");
            bubble.getChildren().add(txt);
        }

        // --- Shared Ticket Card ---
        Integer ticketLinkId = m.getTicketLinkId();
        if ((ticketLinkId == null || ticketLinkId <= 0) && m.getTexto() != null && m.getTexto().startsWith("[TICKET_LINK]:")) {
            try {
                String idStr = m.getTexto().split(":")[1].replace("]", "").trim();
                ticketLinkId = Integer.parseInt(idStr);
            } catch (Exception ignored) {}
        }

        if (ticketLinkId != null && ticketLinkId > 0) {
            final int idToFetch = ticketLinkId;
            Task<Incidencia> ticketTask = new Task<>() {
                @Override protected Incidencia call() { return hubService.getTicketById(idToFetch); }
            };
            ticketTask.setOnSucceeded(ev -> {
                Incidencia inc = ticketTask.getValue();
                if (inc != null) {
                    VBox ticketCard = new VBox(5);
                    ticketCard.getStyleClass().add("shared-ticket-card");
                    Label tTitle = new Label(" " + inc.getTitulo());
                    tTitle.getStyleClass().add("shared-ticket-title");
                    Label tDesc = new Label(inc.getDescripcion());
                    tDesc.getStyleClass().add("shared-ticket-desc");
                    tDesc.setMaxWidth(220);
                    ticketCard.getChildren().addAll(tTitle, tDesc);
                    ticketCard.setOnMouseClicked(click -> onTicketSelect.accept(inc));
                    Platform.runLater(() -> { 
                        if (bubble.getChildren().size() > 1) bubble.getChildren().add(1, ticketCard); 
                        else bubble.getChildren().add(ticketCard); 
                    });
                }
            });
            com.example.aedusapp.utils.ConcurrencyManager.submit(ticketTask);
        }

        // Image Attachment
        if (m.getImagenUrl() != null && !m.getImagenUrl().isEmpty()) {
            try {
                Image img = new Image(m.getImagenUrl(), 200, 0, true, true, true);
                ImageView imgMsg = new ImageView(img);
                imgMsg.setPreserveRatio(true);
                imgMsg.setCursor(javafx.scene.Cursor.HAND);
                imgMsg.setOnMouseClicked(e -> showImageLarge(m.getImagenUrl()));
                bubble.getChildren().add(imgMsg);
            } catch (Exception e) {
                Label errImg = new Label("[Imagen no disponible]");
                errImg.setStyle("-fx-text-fill: red; -fx-font-style: italic;");
                bubble.getChildren().add(errImg);
            }
        }

        // Audio Attachment
        if (m.getAudioUrl() != null && !m.getAudioUrl().isEmpty()) {
            HBox audioBubble = new HBox(10);
            audioBubble.setAlignment(Pos.CENTER_LEFT);
            audioBubble.getStyleClass().add("audio-message-container");
            audioBubble.setPadding(new Insets(5, 10, 5, 10));
            audioBubble.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 15;");

            Button btnPlay = new Button("▶");
            btnPlay.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 30; -fx-min-height: 30; -fx-max-width: 30; -fx-max-height: 30; -fx-font-size: 12px;");
            
            Label lblDuration = new Label("Mensaje de Voz");
            String audioLabelColor;
            if (m.isSoporte()) audioLabelColor = "#854d0e";
            else audioLabelColor = isMe ? "#e2edf9" : "#94a3b8";
            lblDuration.setStyle("-fx-text-fill: " + audioLabelColor + "; -fx-font-size: 11px;");

            btnPlay.setOnAction(e -> audioService.playAudio(m.getAudioUrl(), 
                () -> Platform.runLater(() -> {
                    btnPlay.setText("⏸");
                    btnPlay.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 30; -fx-min-height: 30; -fx-max-width: 30; -fx-max-height: 30; -fx-font-size: 12px;");
                    lblDuration.setText("Reproduciendo...");
                }),
                () -> Platform.runLater(() -> {
                    btnPlay.setText("▶");
                    btnPlay.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 50%; -fx-min-width: 30; -fx-min-height: 30; -fx-max-width: 30; -fx-max-height: 30; -fx-font-size: 12px;");
                    lblDuration.setText("Mensaje de Voz");
                })));
            
            audioBubble.getChildren().addAll(btnPlay, lblDuration);
            bubble.getChildren().add(audioBubble);
        }

        // Metadata (Time and Checks)
        Label time = new Label(new SimpleDateFormat("HH:mm").format(m.getFecha()));
        String timeColor;
        if (m.isSoporte()) {
            timeColor = "#854d0e"; // Dark color for yellow background
        } else {
            timeColor = isMe ? "#e2edf9" : "#94a3b8"; // White-ish for blue, Grey for dark
        }
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: " + timeColor + ";");
        HBox timeRow = new HBox(5);
        timeRow.setAlignment(Pos.CENTER_RIGHT);
        timeRow.getChildren().add(time);

        if (isMe) {
            if (m.isSoporte()) {
                Label supportBadge = new Label("🔒 Soporte");
                supportBadge.setStyle("-fx-font-size: 8px; -fx-text-fill: #854d0e; -fx-font-weight: bold;");
                timeRow.getChildren().add(supportBadge);
            }
            Label check = new Label(m.isLeido() ? "✓✓" : "✓");
            String checkColor;
            if (m.isSoporte()) {
                checkColor = m.isLeido() ? "#1d4ed8" : "#854d0e";
            } else {
                checkColor = m.isLeido() ? "#a5f3fc" : "#e2edf9"; // Bright cyan for read on blue, White-ish for unread
            }
            check.setStyle("-fx-font-size: 10px; -fx-text-fill: " + checkColor + ";");
            timeRow.getChildren().add(check);
        }
        bubble.getChildren().add(timeRow);

        if (isMe) row.getChildren().addAll(bubble, avatarView);
        else row.getChildren().addAll(avatarView, bubble);

        Platform.runLater(() -> chatContainer.getChildren().add(row));
    }

    private static void showImageLarge(String url) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Visor de Imagen");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ImageView fullImgView = new ImageView(url);
        fullImgView.setPreserveRatio(true);
        double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
        fullImgView.setFitWidth(screenWidth * 0.7);
        fullImgView.setFitHeight(screenHeight * 0.7);
        dialog.getDialogPane().setContent(new ScrollPane(fullImgView));
        dialog.showAndWait();
    }
}
