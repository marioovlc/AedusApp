package com.example.aedusapp.controllers.general;

import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Mensaje;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.ai.AIService;
import com.example.aedusapp.services.audio.AudioRecorderService;
import com.example.aedusapp.services.media.PostImagesService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConnectHubController {

    // --- LEFT PANE ---
    @FXML
    private Button btnTabTickets;
    @FXML
    private Button btnTabPersonas;
    @FXML
    private VBox boxListaTickets;
    @FXML
    private VBox boxListaPersonas;
    @FXML
    private ListView<Incidencia> listaTickets;
    @FXML
    private ListView<Usuario> listaUsuarios;
    @FXML
    private TextField txtBusqueda;
    @FXML
    private VBox itemAedusAI;

    private Set<String> usuariosActivos = new HashSet<>();
    private Timeline presenceTimeline;

    private List<Incidencia> todasIncidenciasCache = new ArrayList<>();
    private List<Usuario> todosUsuariosCache = new ArrayList<>();

    private final com.example.aedusapp.services.hub.ConnectHubService hubService = new com.example.aedusapp.services.hub.ConnectHubService();

    private Map<Integer, Timestamp> ticketLastActivityMap = new HashMap<>();
    private Map<String, Timestamp> userLastActivityMap = new HashMap<>();
    private Map<Integer, Integer> ticketUnreadCountMap = new HashMap<>();
    private Map<String, Integer> userUnreadCountMap = new HashMap<>();
    private Map<Integer, String> ticketLastMessagePreviewMap = new HashMap<>();
    private Map<String, String> userLastMessagePreviewMap = new HashMap<>();

    // --- CENTER PANE ---
    @FXML
    private Label lblChatDestino;
    @FXML
    private Label lblChatStatus;
    @FXML
    private ScrollPane scrollChat;
    @FXML
    private VBox chatContainer;
    @FXML
    private TextField txtMensaje;
    @FXML
    private Button btnEnviar;
    @FXML
    private Button btnAdjuntar;
    @FXML
    private Button btnGrabarVoz;
    @FXML
    private Label lblRecordingStatus;

    // --- RIGHT PANE ---
    @FXML
    private VBox paneDetalleTicket;
    @FXML
    private VBox paneDetalleUsuario;

    // Ticket Details
    @FXML
    private Label lblDetalleTitulo;
    @FXML
    private Text txtDetalleDescripcion;
    @FXML
    private Label lblDetalleEstado;
    @FXML
    private Label lblDetalleAula;
    @FXML
    private VBox vboxImagen;
    @FXML
    private ImageView imgDetalle;
    @FXML
    private Button btnCompartirTicket;

    // Profile Details
    @FXML
    private ImageView imgPerfilGrande;
    @FXML
    private Label lblPerfilNombre;
    @FXML
    private Label lblPerfilRol;
    @FXML
    private TextField txtPerfilEmail;
    @FXML
    private TextField txtPerfilTelefono;
    @FXML
    private TextArea txtPerfilBio;
    @FXML
    private Button btnGuardarPerfil;

    private Usuario usuarioActual;
    private Incidencia incidenciaActual;
    private Usuario usuarioDestino; // For direct chat
    private boolean isAedusAIChat = false;

    private final AIService aiService = new AIService();

    private CheckBox chkSoporte;
    private Button btnCompartirEnChat;

    // Audio recording
    private AudioRecorderService audioService = new AudioRecorderService();
    private File recordedAudioFile;

    @FXML
    public void initialize() {
        btnEnviar.setOnAction(e -> sendMessage());
        btnAdjuntar.setOnAction(e -> attachImage());

        // Dynamic Checkbox for Support Memos inserted before the Send button
        chkSoporte = new CheckBox("Soporte");
        chkSoporte.setVisible(false);
        chkSoporte.setManaged(false);
        chkSoporte.setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold;");

        // Dynamic Share Button for in-chat sharing
        btnCompartirEnChat = new Button("🚀 Compartir Ticket");
        btnCompartirEnChat.setVisible(false);
        btnCompartirEnChat.setManaged(false);
        btnCompartirEnChat.getStyleClass().add("action-button");
        btnCompartirEnChat.setOnAction(e -> compartirTicketDesdeChat());

        // Using runLater to ensure scene is built and txtMensaje has a parent
        Platform.runLater(() -> {
            if (txtMensaje.getParent() instanceof HBox) {
                HBox parent = (HBox) txtMensaje.getParent();
                int btnIndex = parent.getChildren().indexOf(btnEnviar);
                if (btnIndex >= 0) {
                    parent.getChildren().add(btnIndex, chkSoporte);
                    parent.getChildren().add(btnIndex, btnCompartirEnChat);
                } else {
                    parent.getChildren().add(chkSoporte);
                    parent.getChildren().add(btnCompartirEnChat);
                }
            }
        });

        // Configure list view cell factory
        listaTickets.setCellFactory(param -> new ListCell<Incidencia>() {
            @Override
            protected void updateItem(Incidencia item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox cell = new VBox(2);
                    cell.getStyleClass().add("user-list-row");
                    cell.setPadding(new Insets(8));

                    Label title = new Label(item.getTitulo());
                    title.getStyleClass().add("user-list-name");

                    HBox meta = new HBox(5);
                    Label status = new Label(item.getEstado());
                    status.getStyleClass().add("user-list-email");
                    Label lastMsg = new Label("");
                    lastMsg.getStyleClass().add("last-message-preview");
                    String last = ticketLastMessagePreviewMap.get(item.getId());
                    if (last != null) lastMsg.setText(" - " + last);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Label badge = new Label();
                    badge.getStyleClass().add("unread-badge");
                    int count = ticketUnreadCountMap.getOrDefault(item.getId(), 0);
                    if (count > 0) {
                        badge.setText(String.valueOf(count));
                        badge.setVisible(true);
                        badge.setManaged(true);
                    } else {
                        badge.setVisible(false);
                        badge.setManaged(false);
                    }

                    meta.getChildren().addAll(status, lastMsg, spacer, badge);

                    cell.getChildren().addAll(title, meta);
                    setGraphic(cell);
                }
            }
        });

        listaTickets.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seleccionarTicket(newVal);
            }
        });

        // Configure user list cell factory
        listaUsuarios.setCellFactory(param -> new ListCell<Usuario>() {
            @Override
            protected void updateItem(Usuario item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(10);
                    cell.getStyleClass().add("user-list-row");
                    cell.setAlignment(Pos.CENTER_LEFT);
                    cell.setPadding(new Insets(8));

                    Region dot = new Region();
                    dot.getStyleClass().add("presence-dot");
                    if (usuariosActivos.contains(item.getId()))
                        dot.getStyleClass().add("online");

                    VBox info = new VBox(2);
                    Label name = new Label(item.getNombre());
                    name.getStyleClass().add("user-list-name");

                    Label lastMsg = new Label("");
                    lastMsg.getStyleClass().add("last-message-preview");
                    String last = userLastMessagePreviewMap.get(item.getId());
                    if (last != null) lastMsg.setText(last);
                    else lastMsg.setText(item.getEmail());

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Label badge = new Label();
                    badge.getStyleClass().add("unread-badge");
                    int count = userUnreadCountMap.getOrDefault(item.getId(), 0);
                    if (count > 0) {
                        badge.setText(String.valueOf(count));
                        badge.setVisible(true);
                        badge.setManaged(true);
                    } else {
                        badge.setVisible(false);
                        badge.setManaged(false);
                    }

                    info.getChildren().addAll(name, lastMsg);
                    cell.getChildren().addAll(dot, info, spacer, badge);
                    setGraphic(cell);

                }
            }
        });

        listaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seleccionarUsuario(newVal);
            }
        });

        // Search logic
        txtBusqueda.textProperty().addListener((obs, oldVal, newVal) -> filtrarListas(newVal));

        // Auto-scroll
        chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> scrollChat.setVvalue(1.0));

        lblRecordingStatus.setVisible(false);
        // Initialization for right pane stack
        paneDetalleTicket.setVisible(true);
        paneDetalleUsuario.setVisible(false);

        iniciarPingPresencia();
        configurarDragAndDrop();
    }

    private void configurarDragAndDrop() {
        Platform.runLater(() -> {
            if (txtMensaje != null && txtMensaje.getScene() != null) {
                txtMensaje.getScene().setOnDragOver(event -> {
                    if (event.getDragboard().hasFiles()) {
                        boolean isImage = false;
                        for (File f : event.getDragboard().getFiles()) {
                            String name = f.getName().toLowerCase();
                            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
                                isImage = true;
                                break;
                            }
                        }
                        if (isImage && !isAedusAIChat) {
                            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                        }
                    }
                    event.consume();
                });
                
                txtMensaje.getScene().setOnDragDropped(event -> {
                    boolean success = false;
                    if (event.getDragboard().hasFiles() && !isAedusAIChat) {
                        for (File f : event.getDragboard().getFiles()) {
                            String name = f.getName().toLowerCase();
                            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
                                success = true;
                                lblChatStatus.setText("Subiendo imagen...");
                                Task<Void> uploadTask = new Task<>() {
                                    @Override
                                    protected Void call() {
                                        String url = PostImagesService.uploadImage(f);
                                        if (url != null) {
                                            boolean isSoportePrivado = chkSoporte != null && chkSoporte.isSelected();
                                            if (incidenciaActual != null) {
                                                hubService.sendTicketMessage(incidenciaActual.getId(), usuarioActual.getId(),
                                                        "Imagen adjunta", url, null, isSoportePrivado);
                                            } else if (usuarioDestino != null) {
                                                hubService.sendDirectMessageWithAttachment(usuarioActual.getId(), usuarioDestino.getId(),
                                                        "Imagen adjunta", url);
                                            }
                                        }
                                        return null;
                                    }
                                };
                                uploadTask.setOnSucceeded(e -> {
                                    lblChatStatus.setText("En línea");
                                    loadChatHistory();
                                });
                                new Thread(uploadTask).start();
                                break;
                            }
                        }
                    }
                    event.setDropCompleted(success);
                    event.consume();
                });
            }
        });
    }

    private void iniciarPingPresencia() {
        if (presenceTimeline != null)
            presenceTimeline.stop();

        presenceTimeline = new Timeline(new KeyFrame(Duration.seconds(20), e -> {
            if (usuarioActual != null) {
                Task<Void> pingTask = new Task<>() {
                    @Override
                    protected Void call() {
                        hubService.updateUserPresence(usuarioActual.getId());
                        List<String> activos = hubService.getRecentlyActiveUsers(45);
                        Platform.runLater(() -> {
                            usuariosActivos.clear();
                            usuariosActivos.addAll(activos);
                            usuariosActivos.add("system"); // AI is always online
                            listaUsuarios.refresh();
                            listaTickets.refresh();
                        });
                        return null;
                    }
                };
                new Thread(pingTask).start();
            }
        }));
        presenceTimeline.setCycleCount(Timeline.INDEFINITE);
        presenceTimeline.play();

        // Immediate first run
        if (usuarioActual != null) {
            new Thread(() -> {
                hubService.initPresenceSystem();
                hubService.updateUserPresence(usuarioActual.getId());
            }).start();
        }
    }

    @FXML
    private void mostrarPestanaTickets() {
        btnTabTickets.getStyleClass().add("active");
        btnTabPersonas.getStyleClass().remove("active");
        boxListaTickets.setVisible(true);
        boxListaTickets.setManaged(true);
        boxListaPersonas.setVisible(false);
        boxListaPersonas.setManaged(false);
    }

    @FXML
    private void mostrarPestanaPersonas() {
        btnTabPersonas.getStyleClass().add("active");
        btnTabTickets.getStyleClass().remove("active");
        boxListaPersonas.setVisible(true);
        boxListaPersonas.setManaged(true);
        boxListaTickets.setVisible(false);
        boxListaTickets.setManaged(false);
        cargarUsuarios();
    }

    private void cargarUsuarios() {
        if (usuarioActual == null) return;
        
        Task<com.example.aedusapp.services.hub.ConnectHubService.HubData<Usuario>> task = new Task<>() {
            @Override
            protected com.example.aedusapp.services.hub.ConnectHubService.HubData<Usuario> call() {
                return hubService.loadUserData(usuarioActual);
            }
        };
        task.setOnSucceeded(e -> {
            com.example.aedusapp.services.hub.ConnectHubService.HubData<Usuario> data = task.getValue();
            todosUsuariosCache = data.items;
            
            // Safe casts with @SuppressWarnings as we know the HubData types
            @SuppressWarnings("unchecked")
            Map<String, Timestamp> dMap = (Map<String, Timestamp>) data.dates;
            userLastActivityMap = dMap;
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> uMap = (Map<String, Integer>) data.unreadCounts;
            userUnreadCountMap = uMap;
            
            @SuppressWarnings("unchecked")
            Map<String, String> mMap = (Map<String, String>) data.lastMessages;
            userLastMessagePreviewMap = mMap;
            
            filtrarListas(txtBusqueda.getText());
        });
        new Thread(task).start();
    }

    private void filtrarListas(String term) {
        String lower = term.toLowerCase();

        // Sort and Filter tickets
        List<Incidencia> filteredTickets = todasIncidenciasCache.stream()
                .filter(i -> i.getTitulo().toLowerCase().contains(lower) ||
                        i.getEstado().toLowerCase().contains(lower))
                .sorted((a, b) -> {
                    Timestamp da = ticketLastActivityMap.get(a.getId());
                    Timestamp db = ticketLastActivityMap.get(b.getId());
                    if (da == null && db == null) return Integer.compare(b.getId(), a.getId());
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da);
                })
                .toList();
        listaTickets.getItems().setAll(filteredTickets);

        // Sort and Filter users
        List<Usuario> filteredUsers = todosUsuariosCache.stream()
                .filter(u -> u.getNombre().toLowerCase().contains(lower) ||
                        u.getEmail().toLowerCase().contains(lower))
                .sorted((a, b) -> {
                    Timestamp da = userLastActivityMap.get(a.getId());
                    Timestamp db = userLastActivityMap.get(b.getId());
                    if (da == null && db == null) return a.getNombre().compareTo(b.getNombre());
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da);
                })
                .toList();
        listaUsuarios.getItems().setAll(filteredUsers);
    }

    public void setUsuarioActual(Usuario user) {
        this.usuarioActual = user;

        // Show Support check if admin/mantenimiento
        if ("ADMIN".equalsIgnoreCase(user.getRole()) || "MANTENIMIENTO".equalsIgnoreCase(user.getRole())) {
            if (chkSoporte != null) {
                chkSoporte.setVisible(true);
                chkSoporte.setManaged(true);
            }
        }

        iniciarPingPresencia();
        cargarTickets();
    }

    private void cargarTickets() {
        if (usuarioActual == null) return;

        Task<com.example.aedusapp.services.hub.ConnectHubService.HubData<Incidencia>> task = new Task<>() {
            @Override
            protected com.example.aedusapp.services.hub.ConnectHubService.HubData<Incidencia> call() {
                return hubService.loadTicketData(usuarioActual);
            }
        };
        task.setOnSucceeded(e -> {
            com.example.aedusapp.services.hub.ConnectHubService.HubData<Incidencia> data = task.getValue();
            todasIncidenciasCache = data.items;
            
            @SuppressWarnings("unchecked")
            Map<Integer, Timestamp> dMap = (Map<Integer, Timestamp>) data.dates;
            ticketLastActivityMap = dMap;
            
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> uMap = (Map<Integer, Integer>) data.unreadCounts;
            ticketUnreadCountMap = uMap;
            
            @SuppressWarnings("unchecked")
            Map<Integer, String> mMap = (Map<Integer, String>) data.lastMessages;
            ticketLastMessagePreviewMap = mMap;
            
            filtrarListas(txtBusqueda.getText());
        });
        new Thread(task).start();
    }

    @FXML
    private void seleccionarAi() {
        isAedusAIChat = true;
        incidenciaActual = null;
        usuarioDestino = null;
        listaTickets.getSelectionModel().clearSelection();
        listaUsuarios.getSelectionModel().clearSelection();

        if (btnCompartirTicket != null) {
            btnCompartirTicket.setVisible(false);
            btnCompartirTicket.setManaged(false);
        }

        itemAedusAI.getStyleClass().add("selected");
        lblChatDestino.setText("Aedus AI");
        lblChatStatus.setText("Asistente Virtual");

        chatContainer.getChildren().clear();
        paneDetalleTicket.setVisible(false);
        paneDetalleUsuario.setVisible(false);

        if (chkSoporte != null) {
            chkSoporte.setVisible(false);
            chkSoporte.setManaged(false);
        }
        if (btnCompartirEnChat != null) {
            btnCompartirEnChat.setVisible(false);
            btnCompartirEnChat.setManaged(false);
        }

        addSystemMessage("Hola, soy Aedus AI. ¿En qué te puedo ayudar hoy con la aplicación?");
    }

    private void seleccionarTicket(Incidencia inc) {
        isAedusAIChat = false;
        incidenciaActual = inc;
        // Keep usuarioDestino if it's already set (to share ticket with them)
        itemAedusAI.getStyleClass().remove("selected");

        lblChatDestino.setText("Ticket #" + inc.getId() + ": " + inc.getTitulo());
        lblChatStatus.setText(inc.getEstado());

        // Contextual buttons
        if (chkSoporte != null) {
            boolean isAdmin = "ADMIN".equalsIgnoreCase(usuarioActual.getRole()) || "MANTENIMIENTO".equalsIgnoreCase(usuarioActual.getRole());
            chkSoporte.setVisible(isAdmin);
            chkSoporte.setManaged(isAdmin);
        }
        if (btnCompartirEnChat != null) {
            btnCompartirEnChat.setVisible(false);
            btnCompartirEnChat.setManaged(false);
        }

        // Sharing button visibility - always visible when a ticket is selected
        if (btnCompartirTicket != null) {
            btnCompartirTicket.setVisible(true);
            btnCompartirTicket.setManaged(true);
        }

        // Mark as read and refresh badge
        Task<Void> markReadTask = new Task<>() {
            @Override protected Void call() {
                hubService.markTicketAsRead(inc.getId(), usuarioActual.getId());
                return null;
            }
        };
        markReadTask.setOnSucceeded(e -> cargarTickets());
        new Thread(markReadTask).start();

        // Update Right Pane
        paneDetalleTicket.setVisible(true);
        paneDetalleUsuario.setVisible(false);

        lblDetalleTitulo.setText(inc.getTitulo());
        txtDetalleDescripcion.setText(inc.getDescripcion());
        lblDetalleEstado.setText(inc.getEstado());
        lblDetalleAula.setText(inc.getAulaNombre() != null ? inc.getAulaNombre() : "N/A");

        if (inc.getImagenRuta() != null && !inc.getImagenRuta().isEmpty()) {
            try {
                // Support Cloudinary URLs or local files
                if (inc.getImagenRuta().startsWith("http")) {
                    imgDetalle.setImage(new Image(inc.getImagenRuta(), true));
                } else {
                    File img = new File(inc.getImagenRuta());
                    if (img.exists()) {
                        imgDetalle.setImage(new Image(img.toURI().toString()));
                    }
                }
                vboxImagen.setVisible(true);
                vboxImagen.setManaged(true);
            } catch (Exception e) {
                vboxImagen.setVisible(false);
                vboxImagen.setManaged(false);
            }
        } else {
            vboxImagen.setVisible(false);
            vboxImagen.setManaged(false);
        }

        loadChatHistory();
    }

    private void seleccionarUsuario(Usuario user) {
        isAedusAIChat = false;
        incidenciaActual = null;
        usuarioDestino = user;
        itemAedusAI.getStyleClass().remove("selected");

        // Hide share button when starting user chat until a ticket is selected
        if (btnCompartirTicket != null) {
            btnCompartirTicket.setVisible(false);
            btnCompartirTicket.setManaged(false);
        }

        lblChatDestino.setText(user.getNombre());
        lblChatStatus.setText(usuariosActivos.contains(user.getId()) ? "En línea" : "Desconectado");

        // Contextual buttons
        if (chkSoporte != null) {
            chkSoporte.setVisible(false);
            chkSoporte.setManaged(false);
        }
        if (btnCompartirEnChat != null) {
            btnCompartirEnChat.setVisible(true);
            btnCompartirEnChat.setManaged(true);
        }

        // Mark as read and refresh
        Task<Void> markReadTask = new Task<>() {
            @Override protected Void call() {
                hubService.markDirectMessagesAsRead(usuarioActual.getId(), user.getId());
                return null;
            }
        };
        markReadTask.setOnSucceeded(e -> cargarUsuarios());
        new Thread(markReadTask).start();

        // Update Right Pane (Profile)
        paneDetalleTicket.setVisible(false);
        paneDetalleUsuario.setVisible(true);

        lblPerfilNombre.setText(user.getNombre());
        lblPerfilRol.setText(user.getRole());
        txtPerfilEmail.setText(user.getEmail());
        txtPerfilTelefono.setText(user.getTelefono() != null ? user.getTelefono() : "");
        txtPerfilBio.setText(user.getBio() != null ? user.getBio() : "");

        if (user.getFotoPerfilDatos() != null) {
            imgPerfilGrande.setImage(new Image(new java.io.ByteArrayInputStream(user.getFotoPerfilDatos())));
        } else {
            imgPerfilGrande.setImage(null);
        }

        // Only editable if it's my own profile
        boolean isMe = user.getId().equals(usuarioActual.getId());
        txtPerfilTelefono.setEditable(isMe);
        txtPerfilBio.setEditable(isMe);
        btnGuardarPerfil.setVisible(isMe);
        btnGuardarPerfil.setManaged(isMe);

        loadChatHistory();
    }

    @FXML
    private void guardarPerfilPropio() {
        if (usuarioDestino == null) return;

        usuarioDestino.setTelefono(txtPerfilTelefono.getText());
        usuarioDestino.setBio(txtPerfilBio.getText());

        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                return hubService.actualizarUsuario(usuarioDestino);
            }
        };
        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue()) {
                System.out.println("Perfil actualizado con éxito.");
                addSystemMessage("Perfil actualizado con éxito.");
                cargarUsuarios(); // Refresh list to show online status etc
            }
        });
        new Thread(saveTask).start();
    }

    @FXML
    private void compartirTicketActual() {
        if (incidenciaActual == null) return;

        Usuario target = usuarioDestino;

        // If no user is selected, or if we want to change recipient, show selector
        if (target == null) {
            target = showRecipientSelector();
        }

        if (target != null) {
            final Usuario finalTarget = target;
            String texto = "Te comparto este ticket: [" + incidenciaActual.getTitulo() + "]";
            int ticketId = incidenciaActual.getId();

            // Notify UI
            if (usuarioDestino != null && usuarioDestino.getId().equals(finalTarget.getId())) {
                addLocalMessage(texto, null, null, false, ticketId);
            } else {
                com.example.aedusapp.utils.ui.ToastNotification.success(btnCompartirTicket.getScene().getWindow(), 
                    "Ticket compartido con " + finalTarget.getNombre());
            }

            Task<Void> task = new Task<>() {
                protected Void call() {
                    hubService.sendDirectMessageWithTicket(usuarioActual.getId(), finalTarget.getId(), 
                            "Ticket compartido: " + incidenciaActual.getTitulo(), ticketId);
                    return null;
                }
            };
            task.setOnSucceeded(e -> cargarUsuarios());
            new Thread(task).start();
        }
    }

    private Usuario showRecipientSelector() {
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar Destinatario");
        dialog.setHeaderText("Elige a quién quieres enviar el ticket:");
        
        // Style
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().addAll(btnCompartirTicket.getScene().getStylesheets());
        dialogPane.getStyleClass().add("dialog-pane");

        // UI Components
        VBox content = new VBox(10);
        content.setPrefWidth(350);
        content.setPadding(new Insets(10));

        TextField search = new TextField();
        search.setPromptText("Buscar usuario...");
        search.getStyleClass().add("search-field");

        ListView<Usuario> list = new ListView<>();
        list.setPrefHeight(300);
        
        // Reuse same cell factory as the main list
        list.setCellFactory(listaUsuarios.getCellFactory());

        // Dynamic filtering
        List<Usuario> availableUsers = todosUsuariosCache.stream()
            .filter(u -> !u.getId().equals(usuarioActual.getId()))
            .toList();
        list.getItems().setAll(availableUsers);

        search.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase();
            list.getItems().setAll(availableUsers.stream()
                .filter(u -> u.getNombre().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q))
                .toList());
        });

        content.getChildren().addAll(search, list);
        dialogPane.setContent(content);

        // Buttons
        ButtonType btnSend = new ButtonType("Compartir", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(btnSend, ButtonType.CANCEL);

        dialog.setResultConverter(b -> b == btnSend ? list.getSelectionModel().getSelectedItem() : null);

        Optional<Usuario> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void compartirTicketDesdeChat() {
        if (usuarioDestino == null) return;

        Incidencia ticket = showTicketSelector();
        if (ticket != null) {
            String texto = "Te comparto este ticket: [" + ticket.getTitulo() + "]";
            int ticketId = ticket.getId();

            addLocalMessage(texto, null, null, false, ticketId);
            
            Task<Void> task = new Task<>() {
                protected Void call() {
                    hubService.sendDirectMessageWithTicket(usuarioActual.getId(), usuarioDestino.getId(), 
                            "Ticket compartido: " + ticket.getTitulo(), ticketId);
                    return null;
                }
            };
            task.setOnSucceeded(e -> cargarUsuarios());
            new Thread(task).start();
        }
    }

    private Incidencia showTicketSelector() {
        Dialog<Incidencia> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar Ticket");
        dialog.setHeaderText("Elige qué ticket quieres compartir:");
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().addAll(btnEnviar.getScene().getStylesheets());
        dialogPane.getStyleClass().add("dialog-pane");

        // UI Components
        VBox content = new VBox(10);
        content.setPrefWidth(400);
        content.setPadding(new Insets(10));

        TextField search = new TextField();
        search.setPromptText("Buscar ticket por título o estado...");
        search.getStyleClass().add("search-field");

        ListView<Incidencia> list = new ListView<>();
        list.setPrefHeight(350);
        list.setCellFactory(listaTickets.getCellFactory());

        // Fill with current cache
        list.getItems().setAll(todasIncidenciasCache);

        search.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase();
            list.getItems().setAll(todasIncidenciasCache.stream()
                .filter(i -> i.getTitulo().toLowerCase().contains(q) || i.getEstado().toLowerCase().contains(q))
                .toList());
        });

        content.getChildren().addAll(search, list);
        dialogPane.setContent(content);

        ButtonType btnSelect = new ButtonType("Compartir", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(btnSelect, ButtonType.CANCEL);

        dialog.setResultConverter(b -> b == btnSelect ? list.getSelectionModel().getSelectedItem() : null);

        Optional<Incidencia> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void loadChatHistory() {
        chatContainer.getChildren().clear();
        Task<List<Mensaje>> loadTask = new Task<>() {
            @Override
            protected List<Mensaje> call() {
                if (incidenciaActual != null) {
                    return hubService.getTicketMessages(incidenciaActual.getId(), 100);
                } else if (usuarioDestino != null) {
                    return hubService.getDirectMessages(usuarioActual.getId(), usuarioDestino.getId(), 100);
                }
                return new ArrayList<>();
            }
        };
        loadTask.setOnSucceeded(e -> {
            for (Mensaje m : loadTask.getValue()) {
                addMessageToChat(m);
            }
        });
        new Thread(loadTask).start();
    }

    private void sendMessage() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty())
            return;

        txtMensaje.clear();
        boolean isSoportePrivado = chkSoporte != null && chkSoporte.isSelected();
        if (chkSoporte != null)
            chkSoporte.setSelected(false); // reset after send

        if (isAedusAIChat) {
            // Echo user message
            addLocalMessage(texto, null, null, false, null);
            lblChatStatus.setText("Escribiendo...");

            // Call AI
            Task<String> aiTask = new Task<>() {
                @Override
                protected String call() {
                    return aiService.askAI("Tengo la siguiente pregunta sobre AedusApp: " + texto);
                }
            };
            aiTask.setOnSucceeded(e -> {
                lblChatStatus.setText("En línea");
                addSystemMessage(aiTask.getValue());
            });
            aiTask.setOnFailed(e -> {
                lblChatStatus.setText("Error");
                addSystemMessage("Lo siento, tuve un problema al procesar tu solicitud.");
            });
            new Thread(aiTask).start();

        } else if (usuarioDestino != null) {
            // Direct Chat message
            addLocalMessage(texto, null, null, false, null);
            Task<Void> sendTask = new Task<>() {
                @Override
                protected Void call() {
                    hubService.sendDirectMessageWithAttachment(usuarioActual.getId(), usuarioDestino.getId(), texto, null);
                    return null;
                }
            };
            sendTask.setOnSucceeded(e -> cargarUsuarios());
            new Thread(sendTask).start();

        } else if (incidenciaActual != null) {
            // Ticket Chat message
            addLocalMessage(texto, null, null, isSoportePrivado, null);
            Task<Void> sendTask = new Task<>() {
                @Override
                protected Void call() {
                    hubService.sendTicketMessage(incidenciaActual.getId(), usuarioActual.getId(), texto, null, null,
                            isSoportePrivado);
                    return null;
                }
            };
            new Thread(sendTask).start();
        }
    }

    private void attachImage() {
        if (isAedusAIChat) {
            addSystemMessage("Lo siento, Aedus AI todavía no puede ver imágenes. Escríbeme tu duda en texto.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.gif"));
        File imgFile = fileChooser.showOpenDialog(btnAdjuntar.getScene().getWindow());

        if (imgFile != null) {
            lblChatStatus.setText("Subiendo imagen...");
            Task<Void> uploadTask = new Task<>() {
                @Override
                protected Void call() {
                    String url = PostImagesService.uploadImage(imgFile);
                    if (url != null) {
                        boolean isSoportePrivado = chkSoporte != null && chkSoporte.isSelected();
                        if (incidenciaActual != null) {
                            hubService.sendTicketMessage(incidenciaActual.getId(), usuarioActual.getId(),
                                    "Imagen enviada", url, null, isSoportePrivado);
                        } else if (usuarioDestino != null) {
                            hubService.sendDirectMessageWithAttachment(usuarioActual.getId(), usuarioDestino.getId(),
                                    "Imagen enviada", url);
                        }
                    }
                    return null;
                }
            };
            uploadTask.setOnSucceeded(e -> {
                lblChatStatus.setText("En línea");
                loadChatHistory();
            });
            new Thread(uploadTask).start();
        }
    }

    @FXML
    private void iniciarGrabacion() {
        if (isAedusAIChat) {
            lblRecordingStatus.setText("La IA no recibe notas de voz.");
            lblRecordingStatus.setVisible(true);
            return;
        }

        lblRecordingStatus.setText("🔴 Grabando...");
        lblRecordingStatus.setVisible(true);
        btnGrabarVoz.setStyle("-fx-text-fill: white; -fx-background-color: #ef4444; -fx-font-size: 16px;");
        audioService.startRecording();
    }

    @FXML
    private void detenerGrabacion() {
        btnGrabarVoz.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px;");
        lblRecordingStatus.setVisible(false);

        if (isAedusAIChat)
            return;

        Task<File> stopTask = new Task<>() {
            @Override
            protected File call() {
                return audioService.stopRecording();
            }
        };

        stopTask.setOnSucceeded(e -> {
            recordedAudioFile = stopTask.getValue();
            if (recordedAudioFile != null) {
                boolean isSoportePrivado = chkSoporte != null && chkSoporte.isSelected();

                // Convert to relative path for portability: uploads/audio/filename.wav
                String relativePath = "uploads" + File.separator + "audio" + File.separator
                        + recordedAudioFile.getName();
                System.out.println("Saving voice message with relative path: " + relativePath);

                // Optimistic UI (using relative path now too)
                addLocalMessage("Mensaje de Voz \ud83c\udfa4", null, relativePath, isSoportePrivado, null);

                Task<Void> saveTask = new Task<>() {
                    @Override
                    protected Void call() {
                        if (incidenciaActual != null) {
                            hubService.sendTicketMessage(incidenciaActual.getId(), usuarioActual.getId(),
                                    "Mensaje de voz \ud83c\udfa4", null, relativePath, isSoportePrivado);
                        } else if (usuarioDestino != null) {
                            hubService.sendDirectMessageWithAttachment(usuarioActual.getId(), usuarioDestino.getId(),
                                    "Mensaje de voz \ud83c\udfa4", relativePath);
                        }
                        return null;
                    }
                };
                new Thread(saveTask).start();
            }
        });

        new Thread(stopTask).start();
    }

    // --- Message Rendering ---

    private void addLocalMessage(String text, String imageUrl, String audioUrl, boolean isSoporte, Integer ticketLinkId) {
        Mensaje m = new Mensaje(
                0,
                incidenciaActual != null ? incidenciaActual.getId() : 0,
                usuarioActual.getId(),
                usuarioActual.getNombre(),
                null,
                text,
                imageUrl,
                new java.sql.Timestamp(System.currentTimeMillis()),
                false,
                isSoporte);
        m.setAudioUrl(audioUrl);
        m.setTicketLinkId(ticketLinkId);
        addMessageToChat(m);
    }

    private void addSystemMessage(String text) {
        Mensaje m = new Mensaje(0, 0, "system", "Aedus AI", null, text, null,
                new java.sql.Timestamp(System.currentTimeMillis()), false, false);
        addMessageToChat(m);
    }

    private void addMessageToChat(Mensaje m) {
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
            } catch (Exception e) {
            }
        } else if ("system".equals(m.getUsuarioId())) {
            // Render AI avatar placeholder
            // omitted for simplicity, it defaults to empty
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

            // Mark as read in background if not system
            if (!m.isLeido() && !"system".equals(m.getUsuarioId()) && incidenciaActual != null
                    && usuarioActual != null) {
                Task<Void> readTask = new Task<>() {
                    protected Void call() {
                        hubService.markTicketAsRead(incidenciaActual.getId(), usuarioActual.getId());
                        return null;
                    }
                };
                new Thread(readTask).start();
            }
        }

        if (m.getTexto() != null && !m.getTexto().isEmpty()) {
            Label txt = new Label(m.getTexto());
            txt.setWrapText(true);
            txt.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 13px;");
            bubble.getChildren().add(txt);
        }

        // --- Render Shared Ticket Card ---
        if (m.getTicketLinkId() != null && m.getTicketLinkId() > 0) {
            Task<Incidencia> ticketTask = new Task<>() {
                @Override
                protected Incidencia call() {
                    return hubService.getTicketById(m.getTicketLinkId());
                }
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
                    tDesc.setEllipsisString("...");

                    ticketCard.getChildren().addAll(tTitle, tDesc);
                    ticketCard.setOnMouseClicked(click -> seleccionarTicket(inc));

                    Platform.runLater(() -> bubble.getChildren().add(1, ticketCard)); // Insert after text
                }
            });
            new Thread(ticketTask).start();
        }

        if (m.getImagenUrl() != null && !m.getImagenUrl().isEmpty()) {
            try {
                // Ensure Image downloads asynchronously and fits within chat bubble
                Image img = new Image(m.getImagenUrl(), 200, 0, true, true, true);
                ImageView imgMsg = new ImageView(img);
                imgMsg.setPreserveRatio(true);
                imgMsg.setCursor(javafx.scene.Cursor.HAND);
                imgMsg.setOnMouseClicked(e -> {
                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Visor de Imagen");
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

                    ImageView fullImgView = new ImageView(m.getImagenUrl());
                    fullImgView.setPreserveRatio(true);

                    // Adjust to screen if too big
                    double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
                    double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
                    fullImgView.setFitWidth(screenWidth * 0.7);
                    fullImgView.setFitHeight(screenHeight * 0.7);

                    dialog.getDialogPane().setContent(new ScrollPane(fullImgView));
                    dialog.showAndWait();
                });

                // Add a placeholder while loading if we want, or just wait.
                bubble.getChildren().add(imgMsg);
            } catch (Exception e) {
                System.err.println("Error rendering image: " + e.getMessage());
                Label errImg = new Label("[Imagen no disponible]");
                errImg.setStyle("-fx-text-fill: red; -fx-font-style: italic;");
                bubble.getChildren().add(errImg);
            }
        }

        if (m.getAudioUrl() != null && !m.getAudioUrl().isEmpty()) {
            Button btnPlay = new Button("▶ Reproducir Voz");
            btnPlay.getStyleClass().add("action-button");
            btnPlay.setOnAction(e -> {
                System.out.println("Requesting playback for: " + m.getAudioUrl());
                audioService.playAudio(
                        m.getAudioUrl(),
                        () -> Platform.runLater(() -> btnPlay.setText("\ud83d\udd0a Reproduciendo...")),
                        () -> Platform.runLater(() -> btnPlay.setText("\u25b6 Reproducir Voz")));
            });
            bubble.getChildren().add(btnPlay);
        }

        Label time = new Label(new SimpleDateFormat("HH:mm").format(m.getFecha()));
        time.setStyle("-fx-font-size: 9px; -fx-text-fill: #94a3b8;");
        time.setAlignment(Pos.CENTER_RIGHT);

        HBox timeRow = new HBox(5);
        timeRow.setAlignment(Pos.CENTER_RIGHT);
        timeRow.getChildren().add(time);

        if (isMe) {
            if (m.isSoporte()) {
                Label supportBadge = new Label("🔒 Soporte");
                supportBadge.setStyle("-fx-font-size: 8px; -fx-text-fill: #ca8a04; -fx-font-weight: bold;");
                timeRow.getChildren().add(supportBadge);
            }
            if (m.isLeido()) {
                Label check = new Label("✓✓");
                check.setStyle("-fx-font-size: 10px; -fx-text-fill: #3b82f6;"); // Blue double check
                timeRow.getChildren().add(check);
            } else {
                Label check = new Label("✓");
                check.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;"); // Gray single check
                timeRow.getChildren().add(check);
            }
        }

        bubble.getChildren().add(timeRow);

        if (isMe) {
            row.getChildren().addAll(bubble, avatarView);
        } else {
            row.getChildren().addAll(avatarView, bubble);
        }

        Platform.runLater(() -> chatContainer.getChildren().add(row));
    }
}
