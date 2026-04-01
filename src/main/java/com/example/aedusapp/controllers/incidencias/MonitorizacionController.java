package com.example.aedusapp.controllers.incidencias;

import com.example.aedusapp.database.daos.IncidenciaDAO;
import com.example.aedusapp.database.daos.UsuarioDAO;
import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.ai.AIService;
import com.example.aedusapp.services.logging.LogService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import com.example.aedusapp.models.Mensaje;
import com.example.aedusapp.services.hub.IConnectHubService;
import com.example.aedusapp.utils.hub.MessageRenderer;
import com.example.aedusapp.services.audio.AudioRecorderService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.application.Platform;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MonitorizacionController {

    // ── FXML ──────────────────────────────────────────────────────────
    @FXML
    private VBox listaIncidencias; // reemplaza a tablaIncidencias

    @FXML
    private VBox panelDetalles;
    @FXML
    private Label lblDetallesTitulo;
    @FXML
    private Text txtDetallesDescripcion;
    @FXML
    private ImageView imgDetalles;
    @FXML
    private VBox vboxImagen;
    @FXML
    private Button btnMarcarLeido;
    @FXML
    private Button btnMarcarEnRevision;
    @FXML
    private Button btnMarcarAcabado;
    @FXML
    private Button btnExportarCSV;
    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnEliminarTodo;

    @FXML
    private Label lblAsignado;
    @FXML
    private Button btnAsignar;

    @FXML
    private VBox aiCard;
    @FXML
    private Label lblAiStatus;
    @FXML
    private TextArea txtAiResponse;
    @FXML
    private Button btnGuardarFAQ;

    // chips
    @FXML
    private Button chipTodos;
    @FXML
    private Button chipNoLeido;
    @FXML
    private Button chipLeido;
    @FXML
    private Button chipRevision;
    @FXML
    private Button chipAcabado;
    @FXML
    private TextField txtBuscarMonitor;

    // ── Estado ────────────────────────────────────────────────────────
    private final IncidenciaDAO incidenciaDAO = new IncidenciaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final AIService aiService = new AIService();
    private final com.example.aedusapp.database.daos.ConocimientoDAO conocimientoDAO = new com.example.aedusapp.database.daos.ConocimientoDAO();
    private final com.example.aedusapp.database.daos.MisionesDAO misionesDAO = new com.example.aedusapp.database.daos.MisionesDAO();
    private Usuario usuarioActual;
    private Incidencia incidenciaSeleccionada;
    private List<Incidencia> todasIncidencias = new java.util.ArrayList<>();
    private String filtroEstado = "Todos";

    @FXML
    public void initialize() {
        btnMarcarLeido.setOnAction(e -> cambiarEstado("LEIDO"));
        btnMarcarEnRevision.setOnAction(e -> cambiarEstado("EN REVISION"));
        btnMarcarAcabado.setOnAction(e -> cambiarEstado("ACABADO"));
        btnExportarCSV.setOnAction(e -> exportarACSV());
        btnEliminar.setOnAction(e -> eliminarTicket());
        btnEliminarTodo.setOnAction(e -> eliminarTodo());
        if (btnGuardarFAQ != null) {
            btnGuardarFAQ.setOnAction(e -> guardarEnFAQ());
        }

        panelDetalles.setVisible(false);
        panelDetalles.setManaged(false);

        // Real-time search with debounce-like behavior
        if (txtBuscarMonitor != null) {
            txtBuscarMonitor.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        }
    }

    private void guardarEnFAQ() {
        if (incidenciaSeleccionada == null || txtAiResponse.getText().isEmpty()) return;
        boolean exitoso = conocimientoDAO.insertArticulo(incidenciaSeleccionada.getTitulo(), txtAiResponse.getText());
        if (exitoso) {
            com.example.aedusapp.utils.ui.ToastNotification.success(btnExportarCSV.getScene().getWindow(), "Guardado en la Base de Conocimiento");
        } else {
            lblAiStatus.setText("Error al guardar en el FAQ.");
        }
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        if (lblUsuarioConectado != null && usuario != null) {
            lblUsuarioConectado.setText("👤 Conectado como: " + usuario.getNombre());
        }
        cargarIncidencias();
        inicializarRefrescoAutomatico();
    }

    private void inicializarRefrescoAutomatico() {
        refreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> {
                cargarIncidencias();
                if (incidenciaSeleccionada != null) {
                    loadTicketMessages(incidenciaSeleccionada);
                }
            })
        );
        refreshTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    public void setInitialFilter(String estado) {
        if (estado == null) return;
        activarChip(estado);
        aplicarFiltros();
    }

    // ── Chip handlers ──────────────────────────────────────────────────
    @FXML
    private void onChipTodos() {
        activarChip("Todos");
        aplicarFiltros();
    }
    @FXML private Label lblUsuarioConectado;
    @FXML private ScrollPane scrollChat;
    @FXML private VBox chatContainer;
    @FXML private TextField txtMensaje;
    @FXML private Button btnEnviar;
    private final IConnectHubService hubService = com.example.aedusapp.utils.DependencyInjector.get(IConnectHubService.class);
    private final AudioRecorderService audioService = new AudioRecorderService();
    private javafx.animation.Timeline refreshTimeline;
    private int lastMessageIdRendered = -1;
    private final java.util.Map<Integer, HBox> cardNodes = new java.util.HashMap<>();
    @FXML
    private void onChipNoLeido() {
        activarChip("NO LEIDO");
        aplicarFiltros();
    }

    @FXML
    private void onChipLeido() {
        activarChip("LEIDO");
        aplicarFiltros();
    }

    @FXML
    private void onChipRevision() {
        activarChip("EN REVISION");
        aplicarFiltros();
    }

    @FXML
    private void onChipAcabado() {
        activarChip("ACABADO");
        aplicarFiltros();
    }

    private void activarChip(String estado) {
        filtroEstado = estado;
        Button[] chips = { chipTodos, chipNoLeido, chipLeido, chipRevision, chipAcabado };
        String[] estados = { "Todos", "NO LEIDO", "LEIDO", "EN REVISION", "ACABADO" };
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null)
                continue;
            chips[i].getStyleClass().remove("chip-active");
            if (estados[i].equals(estado))
                chips[i].getStyleClass().add("chip-active");
        }
    }

    @FXML
    public void aplicarFiltros() {
        String busqueda = txtBuscarMonitor != null ? txtBuscarMonitor.getText().toLowerCase() : "";
        List<Incidencia> filtradas = todasIncidencias.stream()
                .filter(i -> "Todos".equals(filtroEstado) || filtroEstado.equalsIgnoreCase(i.getEstado()))
                .filter(i -> busqueda.isBlank()
                        || (i.getTitulo() != null && i.getTitulo().toLowerCase().contains(busqueda))
                        || (i.getCreadorNombre() != null && i.getCreadorNombre().toLowerCase().contains(busqueda)))
                .collect(Collectors.toList());
        construirListaCards(filtradas);
    }

    private void exportarACSV() {
        if (todasIncidencias == null || todasIncidencias.isEmpty()) {
            com.example.aedusapp.controllers.general.MainController.showGlobalLoading(true, "No hay incidencias para exportar.");
            new Thread(() -> { try { Thread.sleep(2000); } catch(Exception ignored){} javafx.application.Platform.runLater(()->com.example.aedusapp.controllers.general.MainController.showGlobalLoading(false,""));}).start();
            return;
        }
        
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Guardar Reporte CSV");
        fileChooser.setInitialFileName("reporte_incidencias.csv");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));
        
        File file = fileChooser.showSaveDialog(btnExportarCSV.getScene().getWindow());
        if (file != null) {
            try (java.io.FileWriter fw = new java.io.FileWriter(file);
                 java.io.BufferedWriter bw = new java.io.BufferedWriter(fw)) {
                 
                bw.write("ID,Titulo,Descripcion,Estado,Categoria,Creador ID,Fecha de Creacion\n");
                for (Incidencia i : todasIncidencias) {
                    String titulo = (i.getTitulo() != null) ? i.getTitulo().replace(",", ";").replace("\n", " ").replace("\r", "") : "";
                    String desc = (i.getDescripcion() != null) ? i.getDescripcion().replace(",", ";").replace("\n", " ").replace("\r", "") : "";
                    String estado = (i.getEstado() != null) ? i.getEstado() : "";
                    
                    bw.write(String.format("%d,%s,%s,%s,%d,%s,%s\n", 
                        i.getId(), titulo, desc, estado, i.getCategoriaId(), 
                        i.getUsuarioId() != null ? i.getUsuarioId() : "", 
                        i.getFechaCreacion() != null ? i.getFechaCreacion().toString() : ""));
                }
                
                com.example.aedusapp.utils.ui.ToastNotification.success(btnExportarCSV.getScene().getWindow(), "Reporte CSV exportado con éxito");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ── Carga ─────────────────────────────────────────────────────────
    private void cargarIncidencias() {
        if (todasIncidencias == null || todasIncidencias.isEmpty()) {
            listaIncidencias.getChildren().clear();
            Label loading = new Label("⏳ Cargando incidencias...");
            loading.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-padding: 20;");
            listaIncidencias.getChildren().add(loading);
        }

        javafx.concurrent.Task<List<Incidencia>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Incidencia> call() {
                return incidenciaDAO.getAllTickets();
            }
        };
        task.setOnSucceeded(e -> {
            todasIncidencias = task.getValue();
            aplicarFiltros();
        });
        task.setOnFailed(e -> {
            listaIncidencias.getChildren().clear();
            Label err = new Label("⚠ Error al cargar las incidencias.");
            err.setStyle("-fx-text-fill: #f87171; -fx-font-size: 14px; -fx-padding: 20;");
            listaIncidencias.getChildren().add(err);
        });
        new Thread(task).start();
    }

    // ── Construcción de cards (Smart Update) ─────────────────────────
    private void construirListaCards(List<Incidencia> lista) {
        if (lista.isEmpty()) {
            listaIncidencias.getChildren().clear();
            cardNodes.clear();
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("📋");
            icon.setStyle("-fx-font-size: 48px; -fx-opacity: 0.3;");
            Label msg = new Label("Sin incidencias que coincidan");
            msg.setStyle("-fx-font-size: 16px; -fx-text-fill: #475569;");
            empty.getChildren().addAll(icon, msg);
            listaIncidencias.getChildren().add(empty);
            return;
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yy HH:mm");
        java.util.Set<Integer> nuevosIds = lista.stream().map(Incidencia::getId).collect(java.util.stream.Collectors.toSet());
        
        // Eliminar tarjetas que ya no existen o fueron filtradas
        cardNodes.keySet().removeIf(id -> {
            if (!nuevosIds.contains(id)) {
                listaIncidencias.getChildren().remove(cardNodes.get(id));
                return true;
            }
            return false;
        });

        for (int i = 0; i < lista.size(); i++) {
            Incidencia inc = lista.get(i);
            if (!cardNodes.containsKey(inc.getId())) {
                HBox card = crearFilaIncidencia(inc, dateFmt);
                cardNodes.put(inc.getId(), card);
                listaIncidencias.getChildren().add(i, card);
            } else {
                actualizarFilaExistente(cardNodes.get(inc.getId()), inc);
                // Asegurar orden
                Node node = cardNodes.get(inc.getId());
                int currentIndex = listaIncidencias.getChildren().indexOf(node);
                if (currentIndex != i) {
                    listaIncidencias.getChildren().remove(node);
                    listaIncidencias.getChildren().add(i, node);
                }
            }
        }
    }

    private void actualizarFilaExistente(HBox row, Incidencia inc) {
        String color = getEstadoColor(inc.getEstado());
        row.setStyle("-fx-border-color: transparent transparent transparent " + color + "; -fx-border-width: 0 0 0 5; -fx-background-radius: 0 12 12 0;");
        
        // Buscar el badge de estado y actualizarlo
        for (Node n : row.getChildren()) {
            if (n instanceof Label lbl && lbl.getStyleClass().contains("estado-badge-modern")) {
                lbl.setText(getEstadoEmoji(inc.getEstado()) + " " + inc.getEstado());
                lbl.setStyle("-fx-background-color: " + hexToRgba(color, 0.15) + "; -fx-text-fill: " + color + ";");
            }
        }
    }

    private HBox crearFilaIncidencia(Incidencia inc, SimpleDateFormat dateFmt) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.getStyleClass().add("incidencia-row-modern");
        
        // Borde lateral basado en estado
        String estadoColor = getEstadoColor(inc.getEstado());
        row.setStyle("-fx-border-color: transparent transparent transparent " + estadoColor + "; -fx-border-width: 0 0 0 5; -fx-background-radius: 0 12 12 0;");

        // Info principal
        VBox mainInfo = new VBox(4);
        HBox.setHgrow(mainInfo, Priority.ALWAYS);
        
        HBox titleLine = new HBox(10);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        Label lblId = new Label("#" + inc.getId());
        lblId.setStyle("-fx-text-fill: " + estadoColor + "; -fx-font-weight: bold; -fx-font-size: 11px;");
        
        Label lblTitulo = new Label(inc.getTitulo());
        lblTitulo.getStyleClass().add("user-list-name");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        titleLine.getChildren().addAll(lblId, lblTitulo);

        Label lblMeta = new Label("👤 " + (inc.getCreadorNombre() != null ? inc.getCreadorNombre() : "Usuario") + 
                                  "  •  📅 " + (inc.getFechaCreacion() != null ? dateFmt.format(inc.getFechaCreacion()) : "--"));
        lblMeta.getStyleClass().add("user-list-email");
        mainInfo.getChildren().addAll(titleLine, lblMeta);

        // Estado Badge
        Label badge = new Label(getEstadoEmoji(inc.getEstado()) + " " + inc.getEstado());
        badge.getStyleClass().addAll("estado-badge-modern");
        badge.setStyle("-fx-background-color: " + hexToRgba(estadoColor, 0.15) + "; -fx-text-fill: " + estadoColor + ";");

        row.getChildren().addAll(mainInfo, badge);

        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                listaIncidencias.getChildren().forEach(n -> n.getStyleClass().remove("selected"));
                row.getStyleClass().add("selected");
                mostrarDetalles(inc);
            } else if (e.getClickCount() == 2) {
                ciclarEstado(inc);
            }
        });

        return row;
    }

    private String hexToRgba(String hex, double alpha) {
        try {
            hex = hex.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return "rgba(" + r + "," + g + "," + b + "," + alpha + ")";
        } catch (Exception e) {
            return "rgba(100,116,139," + alpha + ")";
        }
    }

    private void ciclarEstado(Incidencia inc) {
        String[] ciclo = { "NO LEIDO", "LEIDO", "EN REVISION", "ACABADO" };
        String actual = inc.getEstado() != null ? inc.getEstado().toUpperCase() : "NO LEIDO";
        int index = -1;
        for (int i = 0; i < ciclo.length; i++) {
            if (ciclo[i].equals(actual)) {
                index = i;
                break;
            }
        }
        String siguiente = ciclo[(index + 1) % ciclo.length];
        this.incidenciaSeleccionada = inc; // Aseguramos que está seleccionada para cambiarEstado
        cambiarEstado(siguiente);
    }

    // ── Helpers de colores ────────────────────────────────────────────
    private String getEstadoColor(String estado) {
        if (estado == null)
            return "#64748b";
        return switch (estado.toUpperCase()) {
            case "NO LEIDO" -> "#f87171";
            case "LEIDO" -> "#4f8ef7";
            case "EN REVISION" -> "#fbbf24";
            case "ACABADO" -> "#34d399";
            default -> "#64748b";
        };
    }

    private String getEstadoEmoji(String estado) {
        if (estado == null)
            return "•";
        return switch (estado.toUpperCase()) {
            case "NO LEIDO" -> "🔴";
            case "LEIDO" -> "🔵";
            case "EN REVISION" -> "🟡";
            case "ACABADO" -> "🟢";
            default -> "•";
        };
    }


    // ── Panel de detalles ─────────────────────────────────────────────
    private void mostrarDetalles(Incidencia incidencia) {
        if (this.incidenciaSeleccionada != null && this.incidenciaSeleccionada.getId() == incidencia.getId()) {
            // Si ya está seleccionada, solo actualizamos los badges de estado en el panel de detalles si es necesario
            actualizarBotonesEstado(incidencia.getEstado());
            return;
        }

        this.incidenciaSeleccionada = incidencia;
        panelDetalles.setVisible(true);
        panelDetalles.setManaged(true);

        lblDetallesTitulo.setText(incidencia.getTitulo());
        txtDetallesDescripcion.setText(incidencia.getDescripcion());

        String asignado = incidencia.getAsignadoNombre();
        lblAsignado.setText(asignado != null && !asignado.isEmpty() ? asignado : "Sin asignar");

        boolean tieneImagen = incidencia.getImagenRuta() != null && !incidencia.getImagenRuta().isEmpty();
        if (tieneImagen) {
            try {
                String ruta = incidencia.getImagenRuta();
                if (ruta.startsWith("http")) {
                    imgDetalles.setImage(new Image(ruta, true));
                } else {
                    File imgFile = new File(ruta);
                    if (imgFile.exists()) {
                        imgDetalles.setImage(new Image(imgFile.toURI().toString()));
                    } else {
                        vboxImagen.setVisible(false);
                        vboxImagen.setManaged(false);
                    }
                }
                vboxImagen.setVisible(true);
                vboxImagen.setManaged(true);
            } catch (Exception ex) {
                vboxImagen.setVisible(false);
                vboxImagen.setManaged(false);
            }
        } else {
            vboxImagen.setVisible(false);
            vboxImagen.setManaged(false);
        }

        actualizarBotonesEstado(incidencia.getEstado());

        lastMessageIdRendered = -1; // Resetear para que cargue todo al seleccionar uno nuevo
        chatContainer.getChildren().clear();
        loadTicketMessages(incidencia);

        txtAiResponse.setVisible(false);
        txtAiResponse.setManaged(false);
        if (btnGuardarFAQ != null) {
            btnGuardarFAQ.setVisible(false);
            btnGuardarFAQ.setManaged(false);
        }
        lblAiStatus.setText("🤖 Generando sugerencia...");

        javafx.concurrent.Task<String> aiTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() {
                String prompt = "Tengo una incidencia técnica con el título: \"" + incidencia.getTitulo()
                        + "\" y la siguiente descripción: \"" + incidencia.getDescripcion()
                        + "\". Dame pasos concisos y prácticos para solucionarla.";
                return aiService.askAI(prompt);
            }
        };
        aiTask.setOnSucceeded(e -> {
            String resp = aiTask.getValue();
            if (resp != null && !resp.startsWith("Groq Error") && !resp.startsWith("Connection")) {
                txtAiResponse.setText(resp);
                txtAiResponse.setVisible(true);
                txtAiResponse.setManaged(true);
                lblAiStatus.setText("✅ Sugerencia cargada.");
                if (btnGuardarFAQ != null) {
                    btnGuardarFAQ.setVisible(true);
                    btnGuardarFAQ.setManaged(true);
                }
            } else {
                lblAiStatus.setText("⚠ No se pudo obtener una sugerencia.");
            }
        });
        aiTask.setOnFailed(e -> lblAiStatus.setText("⚠ Error al conectar con la IA."));
        new Thread(aiTask).start();
    }

    @FXML
    private void handleAsignar() {
        if (incidenciaSeleccionada == null)
            return;
        List<Usuario> activos = usuarioDAO.getUsersByStatus("ACTIVE");
        List<String> nombres = activos.stream().map(Usuario::getNombre).collect(Collectors.toList());
        if (nombres.isEmpty()) {
            com.example.aedusapp.utils.ui.AlertUtils.showAlert(Alert.AlertType.WARNING, "Sin usuarios",
                    "No hay usuarios activos para asignar.");
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(nombres.get(0), nombres);
        dialog.setTitle("Asignar Incidencia");
        dialog.setHeaderText("¿A quién quieres asignar esta incidencia?");
        dialog.setContentText("Técnico:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombre -> {
            incidenciaSeleccionada.setAsignadoNombre(nombre);
            lblAsignado.setText(nombre);
            aplicarFiltros();
        });
    }


    private void actualizarBotonesEstado(String estado) {
        btnMarcarLeido.setDisable("LEIDO".equalsIgnoreCase(estado));
        btnMarcarEnRevision.setDisable("EN REVISION".equalsIgnoreCase(estado));
        btnMarcarAcabado.setDisable("ACABADO".equalsIgnoreCase(estado));
    }

    private void cambiarEstado(String nuevoEstado) {
        if (incidenciaSeleccionada == null) {
            com.example.aedusapp.utils.ui.ToastNotification.error(btnExportarCSV.getScene().getWindow(), "No hay incidencia seleccionada.");
            return;
        }
        if (incidenciaDAO.updateStatus(incidenciaSeleccionada.getId(), nuevoEstado)) {
            LogService.logCambiarEstado(usuarioActual, incidenciaSeleccionada.getId(),
                    incidenciaSeleccionada.getTitulo(), nuevoEstado);
                    
            if ("ACABADO".equalsIgnoreCase(nuevoEstado) && usuarioActual != null) {
                boolean completada = misionesDAO.registrarMisionDiaria(usuarioActual.getId(), "RESOLVER_TICKET", 15);
                if (completada) {
                    com.example.aedusapp.utils.ui.ToastNotification.success(btnExportarCSV.getScene().getWindow(), "¡Misión Cumplida! (+15 Aedus)");
                } else {
                    com.example.aedusapp.utils.ui.ToastNotification.info(btnExportarCSV.getScene().getWindow(), "Ticket finalizado");
                }
            } else {
                com.example.aedusapp.utils.ui.ToastNotification.info(btnExportarCSV.getScene().getWindow(), "Estado: " + nuevoEstado);
            }
            
            incidenciaSeleccionada.setEstado(nuevoEstado);
            actualizarBotonesEstado(nuevoEstado);
            cargarIncidencias();
        } else {
            com.example.aedusapp.utils.ui.ToastNotification.error(btnExportarCSV.getScene().getWindow(), "No se pudo actualizar el estado.");
        }
    }

    private void loadTicketMessages(Incidencia inc) {
        if (inc == null || chatContainer == null) return;
        
        javafx.concurrent.Task<List<Mensaje>> loadTask = new javafx.concurrent.Task<>() {
            @Override protected List<Mensaje> call() {
                return hubService.getTicketMessages(inc.getId(), 50);
            }
        };
        loadTask.setOnSucceeded(e -> {
            List<Mensaje> mensajes = loadTask.getValue();
            if (mensajes.isEmpty()) return;
            
            boolean appended = false;
            for (Mensaje m : mensajes) {
                if (m.getId() > lastMessageIdRendered) {
                    MessageRenderer.render(m, chatContainer, usuarioActual, incidenciaSeleccionada, hubService, audioService, null);
                    lastMessageIdRendered = m.getId();
                    appended = true;
                }
            }
            if (appended) {
                Platform.runLater(() -> {
                    if (scrollChat != null) scrollChat.setVvalue(1.0);
                });
            }
        });
        com.example.aedusapp.utils.ConcurrencyManager.submit(loadTask);
    }

    @FXML
    private void handleEnviarComentario() {
        if (incidenciaSeleccionada == null || txtMensaje == null || txtMensaje.getText().trim().isEmpty()) return;
        
        String texto = txtMensaje.getText().trim();
        txtMensaje.clear();
        
        javafx.concurrent.Task<Void> sendTask = new javafx.concurrent.Task<>() {
            @Override protected Void call() {
                hubService.sendTicketMessage(incidenciaSeleccionada.getId(), usuarioActual.getId(), texto, null, null, false);
                return null;
            }
        };
        sendTask.setOnSucceeded(e -> loadTicketMessages(incidenciaSeleccionada));
        com.example.aedusapp.utils.ConcurrencyManager.submit(sendTask);
    }

    @FXML
    private void eliminarTicket() {
        if (incidenciaSeleccionada == null) {
            com.example.aedusapp.utils.ui.AlertUtils.showAlert(Alert.AlertType.WARNING, "Error",
                    "No hay incidencia seleccionada.");
            return;
        }
        Alert confirm = com.example.aedusapp.utils.ui.AlertUtils.createConfirmationAlert("Confirmar eliminación",
                "¿Eliminar el ticket #" + incidenciaSeleccionada.getId() + "?", "Esta acción no se puede deshacer.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (incidenciaDAO.deleteTicket(incidenciaSeleccionada.getId())) {
                LogService.logEliminarIncidencia(usuarioActual, incidenciaSeleccionada.getId(),
                        incidenciaSeleccionada.getTitulo());
                panelDetalles.setVisible(false);
                panelDetalles.setManaged(false);
                incidenciaSeleccionada = null;
                cargarIncidencias();
            }
        }
    }

    private void eliminarTodo() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Esta acción borrará permanentemente todos los tickets.\nNo se puede deshacer.", ButtonType.OK,
                ButtonType.CANCEL);
        confirm.setTitle("Confirmar eliminación masiva");
        confirm.setHeaderText("¿Eliminar TODAS las incidencias?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (incidenciaDAO.deleteAllTickets()) {
                panelDetalles.setVisible(false);
                panelDetalles.setManaged(false);
                incidenciaSeleccionada = null;
                cargarIncidencias();
            }
        }
    }

}
