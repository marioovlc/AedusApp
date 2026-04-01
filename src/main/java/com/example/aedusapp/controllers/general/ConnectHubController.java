package com.example.aedusapp.controllers.general;

import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Mensaje;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.ai.AIService;
import com.example.aedusapp.services.audio.AudioRecorderService;
import com.example.aedusapp.services.media.PostImagesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.Timestamp;
import java.util.*;

public class ConnectHubController {
    private static final Logger logger = LoggerFactory.getLogger(ConnectHubController.class);

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

    private com.example.aedusapp.utils.hub.PresenceManager presenceManager;

    private List<Incidencia> todasIncidenciasCache = new ArrayList<>();
    private List<Usuario> todosUsuariosCache = new ArrayList<>();

    private final com.example.aedusapp.services.hub.IConnectHubService hubService = com.example.aedusapp.utils.DependencyInjector.get(com.example.aedusapp.services.hub.IConnectHubService.class);

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

    private Button btnSoporteToggle;
    private Button btnCompartirEnChat;

    // Audio recording
    private final AudioRecorderService audioService = new AudioRecorderService();
    private File recordedAudioFile;
    private javafx.animation.Timeline recordingTimeline;
    private int recordingSeconds = 0;

    // Support Mode
    private boolean isSupportModeActive = false;
    private static final String SUPPORT_COLOR = "#fef9c3"; // Soft gold post-it color
    private static final String SUPPORT_BORDER = "#eab308";

    @FXML
    public void initialize() {
        btnEnviar.setOnAction(e -> sendMessage());
        btnAdjuntar.setOnAction(e -> attachImage());

        // Stylized Support Toggle instead of a simple CheckBox
        btnSoporteToggle = new Button("🔓");
        btnSoporteToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand;");
        btnSoporteToggle.setTooltip(new Tooltip("Modo Nota Interna (Privado)"));
        btnSoporteToggle.setVisible(false);
        btnSoporteToggle.setManaged(false);
        btnSoporteToggle.setOnAction(e -> toggleSupportMode());

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
                int msgIndex = parent.getChildren().indexOf(txtMensaje);
                if (msgIndex >= 0) {
                    parent.getChildren().add(msgIndex, btnSoporteToggle);
                    parent.getChildren().add(msgIndex + 2, btnCompartirEnChat);
                }
            }

            // Keyboard Shortcut: Alt + S to toggle Support Mode
            if (txtMensaje.getScene() != null) {
                txtMensaje.setOnKeyPressed(event -> {
                    if (event.isAltDown() && event.getCode().toString().equals("S")) {
                        toggleSupportMode();
                        event.consume();
                    }
                });
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
                    if (presenceManager != null && presenceManager.getUsuariosActivos().contains(item.getId()))
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

        presenceManager = new com.example.aedusapp.utils.hub.PresenceManager(hubService, activos -> {
            listaUsuarios.refresh();
            listaTickets.refresh();
        });
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
                                            boolean isSoportePrivado = isSupportModeActive;
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
                                com.example.aedusapp.utils.ConcurrencyManager.submit(uploadTask);
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
        if (usuarioActual != null) {
            presenceManager.start(usuarioActual);
        }
    }

    @FXML
    private void mostrarPestanaTickets() {
        btnTabTickets.getStyleClass().add("active");
        btnTabPersonas.getStyleClass().remove("active");
        com.example.aedusapp.utils.ui.TransitionUtils.switchViews(boxListaPersonas, boxListaTickets);
    }

    @FXML
    private void mostrarPestanaPersonas() {
        btnTabPersonas.getStyleClass().add("active");
        btnTabTickets.getStyleClass().remove("active");
        com.example.aedusapp.utils.ui.TransitionUtils.switchViews(boxListaTickets, boxListaPersonas);
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
        com.example.aedusapp.utils.ConcurrencyManager.submit(task);
    }

    private void filtrarListas(String term) {
        String lower = term.toLowerCase();

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
        com.example.aedusapp.utils.ConcurrencyManager.submit(task);
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

        if (btnSoporteToggle != null) {
            btnSoporteToggle.setVisible(false);
            btnSoporteToggle.setManaged(false);
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
        usuarioDestino = null;
        itemAedusAI.getStyleClass().remove("selected");

        lblChatDestino.setText("Ticket #" + inc.getId() + ": " + inc.getTitulo());
        lblChatStatus.setText(inc.getEstado());

        // Contextual buttons
        boolean isAdmin = "ADMIN".equalsIgnoreCase(usuarioActual.getRole()) || "MANTENIMIENTO".equalsIgnoreCase(usuarioActual.getRole());
        if (btnSoporteToggle != null) {
            btnSoporteToggle.setVisible(isAdmin);
            btnSoporteToggle.setManaged(isAdmin);
        }
        if (btnCompartirEnChat != null) {
            btnCompartirEnChat.setVisible(false);
            btnCompartirEnChat.setManaged(false);
        }

        if (btnCompartirTicket != null) {
            btnCompartirTicket.setVisible(true);
            btnCompartirTicket.setManaged(true);
        }

        Task<Void> markReadTask = new Task<>() {
            @Override protected Void call() {
                hubService.markTicketAsRead(inc.getId(), usuarioActual.getId());
                return null;
            }
        };
        markReadTask.setOnSucceeded(e -> cargarTickets());
        com.example.aedusapp.utils.ConcurrencyManager.submit(markReadTask);

        paneDetalleTicket.setVisible(true);
        paneDetalleUsuario.setVisible(false);

        lblDetalleTitulo.setText(inc.getTitulo());
        txtDetalleDescripcion.setText(inc.getDescripcion());
        lblDetalleEstado.setText(inc.getEstado());
        lblDetalleAula.setText(inc.getAulaNombre() != null ? inc.getAulaNombre() : "N/A");

        if (inc.getImagenRuta() != null && !inc.getImagenRuta().isEmpty()) {
            try {
                if (inc.getImagenRuta().startsWith("http")) {
                    imgDetalle.setImage(new Image(inc.getImagenRuta(), true));
                } else {
                    File img = new File(inc.getImagenRuta());
                    if (img.exists()) imgDetalle.setImage(new Image(img.toURI().toString()));
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

        if (btnCompartirTicket != null) {
            btnCompartirTicket.setVisible(false);
            btnCompartirTicket.setManaged(false);
        }

        lblChatDestino.setText(user.getNombre());
        lblChatStatus.setText(presenceManager != null && presenceManager.getUsuariosActivos().contains(user.getId()) ? "En línea" : "Desconectado");

        if (btnSoporteToggle != null) {
            btnSoporteToggle.setVisible(false);
            btnSoporteToggle.setManaged(false);
            isSupportModeActive = false;
            actualizarInterfazSoporte();
        }
        if (btnCompartirEnChat != null) {
            btnCompartirEnChat.setVisible(true);
            btnCompartirEnChat.setManaged(true);
        }

        Task<Void> markReadTask = new Task<>() {
            @Override protected Void call() {
                hubService.markDirectMessagesAsRead(usuarioActual.getId(), user.getId());
                return null;
            }
        };
        markReadTask.setOnSucceeded(e -> cargarUsuarios());
        com.example.aedusapp.utils.ConcurrencyManager.submit(markReadTask);

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
        String tlf = txtPerfilTelefono.getText();
        if (!com.example.aedusapp.utils.DataValidator.isValidPhone(tlf)) {
            com.example.aedusapp.utils.ui.ToastNotification.error(btnGuardarPerfil.getScene().getWindow(), "El formato del teléfono no es válido.");
            return;
        }
        usuarioDestino.setTelefono(tlf);
        usuarioDestino.setBio(txtPerfilBio.getText());

        Task<Boolean> saveTask = new Task<>() {
            @Override protected Boolean call() { return hubService.actualizarUsuario(usuarioDestino); }
        };
        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue()) {
                com.example.aedusapp.utils.ui.ToastNotification.success(btnGuardarPerfil.getScene().getWindow(), "Perfil actualizado.");
                cargarUsuarios();
            }
        });
        com.example.aedusapp.utils.ConcurrencyManager.submit(saveTask);
    }

    @FXML
    private void compartirTicketActual() {
        if (incidenciaActual == null) return;
        Usuario target = usuarioDestino == null ? showRecipientSelector() : usuarioDestino;
        if (target != null) {
            String texto = "Te comparto este ticket: [" + incidenciaActual.getTitulo() + "]";
            int ticketId = incidenciaActual.getId();
            if (usuarioDestino != null && usuarioDestino.getId().equals(target.getId())) {
                addLocalMessage(texto, null, null, false, ticketId);
            } else {
                com.example.aedusapp.utils.ui.ToastNotification.success(btnCompartirTicket.getScene().getWindow(), "Ticket compartido.");
            }
            Task<Void> task = new Task<>() {
                protected Void call() {
                    hubService.sendDirectMessageWithTicket(usuarioActual.getId(), target.getId(), "Ticket: " + incidenciaActual.getTitulo(), ticketId);
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
        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().addAll(btnEnviar.getScene().getStylesheets());
        VBox content = new VBox(10);
        content.setPrefWidth(350);
        TextField search = new TextField();
        search.setPromptText("Buscar...");
        ListView<Usuario> list = new ListView<>();
        list.setCellFactory(listaUsuarios.getCellFactory());
        list.getItems().setAll(todosUsuariosCache.stream().filter(u -> !u.getId().equals(usuarioActual.getId())).toList());
        content.getChildren().addAll(search, list);
        dp.setContent(content);
        ButtonType btnOk = new ButtonType("Compartir", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(btnOk, ButtonType.CANCEL);
        dialog.setResultConverter(b -> b == btnOk ? list.getSelectionModel().getSelectedItem() : null);
        return dialog.showAndWait().orElse(null);
    }

    private void compartirTicketDesdeChat() {
        if (usuarioDestino == null) return;
        Incidencia ticket = showTicketSelector();
        if (ticket != null) {
            addLocalMessage("Ticket compartido: [" + ticket.getTitulo() + "]", null, null, false, ticket.getId());
            Task<Void> task = new Task<>() {
                protected Void call() {
                    hubService.sendDirectMessageWithTicket(usuarioActual.getId(), usuarioDestino.getId(), "Ticket: " + ticket.getTitulo(), ticket.getId());
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
        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().addAll(btnEnviar.getScene().getStylesheets());
        VBox content = new VBox(10);
        content.setPrefWidth(400);
        ListView<Incidencia> list = new ListView<>();
        list.setCellFactory(listaTickets.getCellFactory());
        list.getItems().setAll(todasIncidenciasCache);
        content.getChildren().add(list);
        dp.setContent(content);
        ButtonType btnOk = new ButtonType("Compartir", ButtonBar.ButtonData.OK_DONE);
        dp.getButtonTypes().addAll(btnOk, ButtonType.CANCEL);
        dialog.setResultConverter(b -> b == btnOk ? list.getSelectionModel().getSelectedItem() : null);
        return dialog.showAndWait().orElse(null);
    }

    private void loadChatHistory() {
        chatContainer.getChildren().clear();
        Task<List<Mensaje>> loadTask = new Task<>() {
            @Override protected List<Mensaje> call() {
                if (incidenciaActual != null) return hubService.getTicketMessages(incidenciaActual.getId(), 100);
                if (usuarioDestino != null) return hubService.getDirectMessages(usuarioActual.getId(), usuarioDestino.getId(), 100);
                return new ArrayList<>();
            }
        };
        loadTask.setOnSucceeded(e -> {
            for (Mensaje m : loadTask.getValue()) addMessageToChat(m);
        });
        com.example.aedusapp.utils.ConcurrencyManager.submit(loadTask);
    }

    @FXML
    private void sendMessage() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty()) return;
        txtMensaje.clear();
        boolean isSoportePrivado = isSupportModeActive;

        if (isAedusAIChat) {
            addLocalMessage(texto, null, null, false, null);
            lblChatStatus.setText("Escribiendo...");
            Task<String> aiTask = new Task<>() {
                protected String call() { return aiService.askAI("Pregunta: " + texto); }
            };
            aiTask.setOnSucceeded(e -> {
                lblChatStatus.setText("En línea");
                addSystemMessage(aiTask.getValue());
            });
            new Thread(aiTask).start();
        } else if (usuarioDestino != null) {
            addLocalMessage(texto, null, null, false, null);
            Task<Void> sendTask = new Task<>() {
                protected Void call() { hubService.sendDirectMessageWithAttachment(usuarioActual.getId(), usuarioDestino.getId(), texto, null); return null; }
            };
            sendTask.setOnSucceeded(e -> cargarUsuarios());
            new Thread(sendTask).start();
        } else if (incidenciaActual != null) {
            addLocalMessage(texto, null, null, isSoportePrivado, null);
            Task<Void> sendTask = new Task<>() {
                protected Void call() { hubService.sendTicketMessage(incidenciaActual.getId(), usuarioActual.getId(), texto, null, null, isSoportePrivado); return null; }
            };
            new Thread(sendTask).start();
        }
    }

    private void attachImage() {
        if (isAedusAIChat) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.gif"));
        File f = fc.showOpenDialog(btnAdjuntar.getScene().getWindow());
        if (f != null) {
            lblChatStatus.setText("Subiendo...");
            Task<Void> task = new Task<>() {
                protected Void call() {
                    String url = PostImagesService.uploadImage(f);
                    if (url != null) {
                        if (incidenciaActual != null) hubService.sendTicketMessage(incidenciaActual.getId(), usuarioActual.getId(), "Imagen enviada", url, null, isSupportModeActive);
                        else if (usuarioDestino != null) hubService.sendDirectMessageWithAttachment(usuarioActual.getId(), usuarioDestino.getId(), "Imagen enviada", url);
                    }
                    return null;
                }
            };
            task.setOnSucceeded(e -> { lblChatStatus.setText("En línea"); loadChatHistory(); });
            new Thread(task).start();
        }
    }

    @FXML
    private void toggleSupportMode() {
        isSupportModeActive = !isSupportModeActive;
        logger.info("Support Mode toggled: {}", isSupportModeActive);
        actualizarInterfazSoporte();
    }

    private void actualizarInterfazSoporte() {
        if (isSupportModeActive) {
            btnSoporteToggle.setText("🔒");
            btnSoporteToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: " + SUPPORT_BORDER + "; -fx-font-size: 16px; -fx-cursor: hand;");
            txtMensaje.setStyle("-fx-background-color: " + SUPPORT_COLOR + "; -fx-border-color: " + SUPPORT_BORDER + "; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8; -fx-text-fill: #000000; -fx-prompt-text-fill: #71717a;");
            txtMensaje.setPromptText("Escribe una nota técnica interna...");
            btnEnviar.setText("Guardar Nota");
            btnEnviar.setStyle("-fx-background-color: " + SUPPORT_BORDER + "; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            btnSoporteToggle.setText("🔓");
            btnSoporteToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand;");
            txtMensaje.setStyle("-fx-font-size: 13px; -fx-padding: 8; -fx-background-radius: 20;");
            txtMensaje.setPromptText("Escribe un mensaje...");
            btnEnviar.setText("Enviar");
            btnEnviar.setStyle("");
        }
    }

    @FXML
    private void iniciarGrabacion() {
        if (isAedusAIChat) return;
        lblRecordingStatus.setText("🔴 00:00 | ○○○○○○○○○○");
        lblRecordingStatus.setVisible(true);
        btnGrabarVoz.setStyle("-fx-text-fill: white; -fx-background-color: #ef4444; -fx-font-size: 16px;");
        recordingSeconds = 0;
        if (recordingTimeline != null) recordingTimeline.stop();
        recordingTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), ev -> {
            recordingSeconds++;
            actualizarLabelGrabacion(0);
        }));
        recordingTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        recordingTimeline.play();
        audioService.startRecording(level -> Platform.runLater(() -> actualizarLabelGrabacion(level)));
    }

    private void actualizarLabelGrabacion(double level) {
        int mins = recordingSeconds / 60, secs = recordingSeconds % 60;
        lblRecordingStatus.setText(String.format("🔴 %02d:%02d | %s (Mover fuera para cancelar)", mins, secs, getVolumeVisualizer(level)));
    }

    private String getVolumeVisualizer(double level) {
        int bars = (int) (level * 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(i < bars ? "●" : "○");
        return sb.toString();
    }

    @FXML
    private void detenerGrabacion(javafx.scene.input.MouseEvent event) {
        btnGrabarVoz.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px;");
        lblRecordingStatus.setVisible(false);
        if (recordingTimeline != null) { recordingTimeline.stop(); recordingTimeline = null; }
        if (isAedusAIChat) return;
        boolean cancelled = !btnGrabarVoz.contains(event.getX(), event.getY());
        Task<File> stopTask = new Task<>() {
            protected File call() { return audioService.stopRecording(); }
        };
        stopTask.setOnSucceeded(e -> {
            recordedAudioFile = stopTask.getValue();
            if (recordedAudioFile != null) {
                if (cancelled) { recordedAudioFile.delete(); return; }
                String relativePath = "uploads" + File.separator + "audio" + File.separator + recordedAudioFile.getName();
                addLocalMessage("Voz 🎤", null, relativePath, isSupportModeActive, null);
                Task<Void> saveTask = new Task<>() {
                    protected Void call() {
                        if (incidenciaActual != null) hubService.sendTicketMessage(incidenciaActual.getId(), usuarioActual.getId(), "Voz 🎤", null, relativePath, isSupportModeActive);
                        else if (usuarioDestino != null) hubService.sendDirectMessageWithAttachment(usuarioActual.getId(), usuarioDestino.getId(), "Voz 🎤", relativePath);
                        return null;
                    }
                };
                com.example.aedusapp.utils.ConcurrencyManager.submit(saveTask);
            }
        });
        com.example.aedusapp.utils.ConcurrencyManager.submit(stopTask);
    }

    private void addLocalMessage(String text, String imageUrl, String audioUrl, boolean isSoporte, Integer ticketLinkId) {
        Mensaje m = new Mensaje(0, incidenciaActual != null ? incidenciaActual.getId() : 0, usuarioActual.getId(), usuarioActual.getNombre(), null, text, imageUrl, new java.sql.Timestamp(System.currentTimeMillis()), false, isSoporte);
        m.setAudioUrl(audioUrl); m.setTicketLinkId(ticketLinkId);
        addMessageToChat(m);
    }

    private void addSystemMessage(String text) {
        Mensaje m = new Mensaje(0, 0, "system", "Aedus AI", null, text, null, new java.sql.Timestamp(System.currentTimeMillis()), false, false);
        addMessageToChat(m);
    }

    private void addMessageToChat(Mensaje m) {
        com.example.aedusapp.utils.hub.MessageRenderer.render(m, chatContainer, usuarioActual, incidenciaActual, hubService, audioService, this::seleccionarTicket);
    }

}
