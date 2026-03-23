package com.example.aedusapp.controllers.incidencias;

import com.example.aedusapp.database.daos.IncidenciaDAO;
import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.logging.LogService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

public class IncidenciasController {

    @FXML private VBox incidenciasContainer;
    @FXML private TextField tituloField;
    @FXML private ComboBox<com.example.aedusapp.models.Aula> aulaCombo;
    @FXML private TextArea descripcionField;
    @FXML private ComboBox<String> categoriaCombo;
    @FXML private Label statusLabel;
    @FXML private Button adjuntarImagenBtn;
    @FXML private Label imagenLabel;

    // AI Suggestion panel
    @FXML private VBox panelSugerencia;
    @FXML private Label lblSugerenciaIA;
    @FXML private javafx.scene.control.ProgressIndicator aiSpinner;
    @FXML private Button btnSugerirIA;

    private IncidenciaDAO incidenciaDAO;
    private com.example.aedusapp.database.daos.AulaDAO aulaDAO;
    private final com.example.aedusapp.database.daos.ConocimientoDAO conocimientoDAO = new com.example.aedusapp.database.daos.ConocimientoDAO();
    private final com.example.aedusapp.database.daos.MisionesDAO misionesDAO = new com.example.aedusapp.database.daos.MisionesDAO();
    private File imagenSeleccionada;
    private Usuario usuarioActual;

    // Pagination
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 5;
    private Button loadMoreButton;



    public void initialize() {
        incidenciaDAO = new IncidenciaDAO();
        aulaDAO = new com.example.aedusapp.database.daos.AulaDAO();

        // Cargar categorias hardcodeadas por ahora o de BD
        categoriaCombo.getItems().addAll("Hardware", "Software", "Conectividad", "Mobiliario");

        cargarAulas();
        configurarDragAndDrop();
        
        tituloField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Lost focus
                String text = tituloField.getText().trim();
                if (text.length() > 5) {
                    buscarSugerenciaFAQ(text);
                }
            }
        });
    }

    private void buscarSugerenciaFAQ(String text) {
        javafx.concurrent.Task<String[]> task = new javafx.concurrent.Task<>() {
            @Override
            protected String[] call() {
                return conocimientoDAO.buscarArticuloSimilar(text);
            }
        };
        task.setOnSucceeded(e -> {
            String[] result = task.getValue();
            if (result != null) {
                panelSugerencia.setVisible(true);
                panelSugerencia.setManaged(true);
                lblSugerenciaIA.setText("💡 Sugerencia FAQ: " + result[0] + " (Clic para ver)");
                lblSugerenciaIA.setStyle("-fx-text-fill: #3b82f6; -fx-cursor: hand; -fx-underline: true;");
                lblSugerenciaIA.setOnMouseClicked(click -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Solución en Base de Conocimientos");
                    alert.setHeaderText(result[0]);
                    alert.setContentText(result[1]);
                    alert.showAndWait();
                });
            }
        });
        new Thread(task).start();
    }

    private void configurarDragAndDrop() {
        Platform.runLater(() -> {
            if (tituloField != null && tituloField.getScene() != null) {
                tituloField.getScene().setOnDragOver(event -> {
                    if (event.getDragboard().hasFiles()) {
                        boolean isImage = false;
                        for (File f : event.getDragboard().getFiles()) {
                            String name = f.getName().toLowerCase();
                            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
                                isImage = true;
                                break;
                            }
                        }
                        if (isImage) {
                            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                        }
                    }
                    event.consume();
                });
                
                tituloField.getScene().setOnDragDropped(event -> {
                    boolean success = false;
                    if (event.getDragboard().hasFiles()) {
                        for (File f : event.getDragboard().getFiles()) {
                            String name = f.getName().toLowerCase();
                            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
                                imagenSeleccionada = f;
                                imagenLabel.setText(f.getName());
                                imagenLabel.setStyle("-fx-text-fill: #10b981;");
                                adjuntarImagenBtn.setStyle("-fx-border-color: #10b981; -fx-border-width: 2px; -fx-border-radius: 4px;");
                                success = true;
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

    // ── AI Suggestion handler ───────────────────────────────────────────

    @FXML
    private void handleSugerirIA() {
        String titulo = tituloField.getText().trim();
        String desc = descripcionField.getText().trim();

        String consulta = titulo.isBlank() && desc.isBlank()
                ? null
                : (titulo.isBlank() ? desc : titulo + ": " + desc);

        if (consulta == null) {
            panelSugerencia.setVisible(true);
            panelSugerencia.setManaged(true);
            lblSugerenciaIA.setText("⚠️ Rellena al menos el título o la descripción para obtener una sugerencia.");
            return;
        }

        // Show spinner
        panelSugerencia.setVisible(true);
        panelSugerencia.setManaged(true);
        lblSugerenciaIA.setText("");
        aiSpinner.setVisible(true);
        aiSpinner.setManaged(true);
        btnSugerirIA.setDisable(true);

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                // Build a focused tech-support prompt — NO DB context passed
                String systemPrompt =
                    "Eres un asistente técnico de soporte IT para un centro educativo. " +
                    "El usuario va a describir un problema técnico. " +
                    "Tu misión es dar pasos concretos y prácticos para que el profesor RESUELVA EL PROBLEMA POR SÍ MISMO sin necesidad de abrir un ticket de soporte. " +
                    "Responde SIEMPRE en español. Sé breve y usa viñetas (•) para los pasos. " +
                    "No menciones incidencias anteriores ni bases de datos. Solo responde al problema descrito.";

                String userMsg = "Tengo este problema técnico en el aula:\n\"" + consulta + "\"\n\n¿Cómo puedo solucionarlo?";

                return new com.example.aedusapp.services.ai.AIService().askAI(userMsg, systemPrompt);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            aiSpinner.setVisible(false);
            aiSpinner.setManaged(false);
            btnSugerirIA.setDisable(false);
            lblSugerenciaIA.setText(task.getValue());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            aiSpinner.setVisible(false);
            aiSpinner.setManaged(false);
            btnSugerirIA.setDisable(false);
            lblSugerenciaIA.setText("⚠️ No se pudo conectar con la IA: " + task.getException().getMessage());
        }));
        new Thread(task).start();
    }

    private void cargarAulas() {
        List<com.example.aedusapp.models.Aula> aulas = aulaDAO.getAll();
        aulaCombo.getItems().setAll(aulas);
    }

    @FXML
    private void handleNuevaAula() {
        // Dialogo simple para crear aula
        Dialog<com.example.aedusapp.models.Aula> dialog = new Dialog<>();
        dialog.setTitle("Nueva Aula");
        dialog.setHeaderText("Introduzca los datos del aula");

        ButtonType loginButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nombre = new TextField();
        nombre.setPromptText("Nombre (Ej. Aula 101)");
        ComboBox<String> tipo = new ComboBox<>();
        tipo.getItems().addAll("Informática", "Matemáticas", "Idiomas", "Ciencias", "General");
        tipo.setValue("General");

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombre, 1, 0);
        grid.add(new Label("Tipo:"), 0, 1);
        grid.add(tipo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                // Default capacity 30 for now to satisfy DB constraint
                return new com.example.aedusapp.models.Aula(0, nombre.getText(), tipo.getValue(), 30);
            }
            return null;
        });

        java.util.Optional<com.example.aedusapp.models.Aula> result = dialog.showAndWait();

        result.ifPresent(newAula -> {
            if (newAula.getNombre() != null && !newAula.getNombre().trim().isEmpty()) {
                if(aulaDAO.create(newAula)) {
                    cargarAulas();
                    // Seleccionar la nueva aula
                    for(com.example.aedusapp.models.Aula a : aulaCombo.getItems()) {
                        if(a.getNombre().equals(newAula.getNombre())) {
                            aulaCombo.setValue(a);
                            break;
                        }
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("No se pudo crear el aula");
                    alert.setContentText("Verifique que el nombre no exista ya.");
                    alert.showAndWait();
                }
            }
        });
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        cargarIncidencias();
    }

    private void cargarIncidencias() {
        if (usuarioActual == null)
            return;

        incidenciasContainer.getChildren().clear();
        currentOffset = 0;

        // Inicializar botón de cargar más
        loadMoreButton = new Button("Cargar más...");
        loadMoreButton.getStyleClass().add("load-more-btn");
        loadMoreButton.setMaxWidth(Double.MAX_VALUE);
        loadMoreButton.setOnAction(e -> cargarPaginaIncidencias());

        cargarPaginaIncidencias();
    }

    private void cargarPaginaIncidencias() {
        // Remover botón si existe para evitar duplicados o clicks múltiples
        incidenciasContainer.getChildren().remove(loadMoreButton);

        // Spinner de carga animado
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(36, 36);
        spinner.setStyle("-fx-accent: #3b82f6;");
        javafx.scene.control.Label loadingLabel = new javafx.scene.control.Label("Cargando incidencias...");
        loadingLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        javafx.scene.layout.VBox loadingBox = new javafx.scene.layout.VBox(10, spinner, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new javafx.geometry.Insets(40));
        incidenciasContainer.getChildren().add(loadingBox);

        javafx.concurrent.Task<List<Incidencia>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Incidencia> call() throws Exception {
                return incidenciaDAO.getTicketsByUserPaginated(usuarioActual.getId(), PAGE_SIZE,
                        currentOffset);
            }
        };

        task.setOnSucceeded(e -> {
            incidenciasContainer.getChildren().remove(loadingBox);
            List<Incidencia> incidencias = task.getValue();

            if (incidencias.isEmpty() && currentOffset == 0) {
                // Empty state con icono
                javafx.scene.layout.VBox emptyState = new javafx.scene.layout.VBox(10);
                emptyState.getStyleClass().add("empty-state");
                emptyState.setAlignment(Pos.CENTER);
                emptyState.setMaxWidth(Double.MAX_VALUE);
                Label icon = new Label("📋");
                icon.getStyleClass().add("empty-state-icon");
                Label title = new Label("Sin incidencias");
                title.getStyleClass().add("empty-state-title");
                Label subtitle = new Label(
                        "Todavía no has registrado ninguna incidencia. Usa el formulario de arriba para crear la primera.");
                subtitle.getStyleClass().add("empty-state-subtitle");
                subtitle.setMaxWidth(360);
                emptyState.getChildren().addAll(icon, title, subtitle);
                incidenciasContainer.getChildren().add(emptyState);
            } else {
                for (Incidencia inc : incidencias) {
                    incidenciasContainer.getChildren().add(crearTarjetaIncidencia(inc));
                }

                // Si hemos cargado una página completa, asumimos que puede haber más (o justo
                // acabamos)
                // Se podría optimizar haciendo count, pero "load more" es suficiente patrón
                if (incidencias.size() == PAGE_SIZE) {
                    currentOffset += PAGE_SIZE;
                    incidenciasContainer.getChildren().add(loadMoreButton);
                }
            }
        });

        task.setOnFailed(e -> {
            incidenciasContainer.getChildren().remove(loadingBox);
            javafx.scene.layout.VBox errorState = new javafx.scene.layout.VBox(8);
            errorState.setAlignment(Pos.CENTER);
            errorState.setPadding(new javafx.geometry.Insets(30));
            Label errIcon = new Label("⚠️");
            errIcon.setStyle("-fx-font-size: 36px; -fx-opacity: 0.5;");
            Label errLabel = new Label("Error al cargar incidencias");
            errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-font-weight: bold;");
            Label errHint = new Label("Comprueba la conexión con la base de datos.");
            errHint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            errorState.getChildren().addAll(errIcon, errLabel, errHint);
            incidenciasContainer.getChildren().add(errorState);
            task.getException().printStackTrace();
            if (currentOffset > 0)
                incidenciasContainer.getChildren().add(loadMoreButton);
        });

        new Thread(task).start();
    }

    private VBox crearTarjetaIncidencia(Incidencia inc) {
        String catColor = getCategoryColor(inc.getCategoriaNombre());
        String catIcon = getCategoryIcon(inc.getCategoriaNombre());

        VBox card = new VBox(14);
        card.getStyleClass().add("incidencia-card");
        // Borde izquierdo del color de la categoría (dinámico por categoría, se mantiene inline)
        String borderStyle = "-fx-border-width: 0 0 0 4; -fx-border-radius: 14 0 0 14; -fx-border-color: " + catColor + ";";
        card.setStyle(borderStyle);
        card.setOnMouseEntered(e -> card.setStyle(borderStyle));
        card.setOnMouseExited(e -> card.setStyle(borderStyle));

        // ── Cabecera: ID + Título + Badge estado ──
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("#" + inc.getId());
        idLabel.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: " + catColor + "; -fx-font-weight: bold; -fx-opacity: 0.8;");

        Label titulo = new Label(inc.getTitulo());
        titulo.getStyleClass().add("incidencia-titulo");
        HBox.setHgrow(titulo, Priority.ALWAYS);

        Label estadoBadge = new Label(getEstadoEmoji(inc.getEstado()) + " " + inc.getEstado());
        estadoBadge.getStyleClass().addAll("estado-badge", getEstadoClass(inc.getEstado()));

        header.getChildren().addAll(idLabel, titulo, estadoBadge);

        // ── Chips de metadata ──
        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);

        if (inc.getCategoriaNombre() != null) {
            chips.getChildren().add(crearChip(catIcon + " " + inc.getCategoriaNombre(), catColor));
        }
        if (inc.getAulaNombre() != null) {
            String aula = "🚪 Aula " + inc.getAulaNombre();
            if (inc.getAulaTipo() != null && !inc.getAulaTipo().isEmpty())
                aula += "  " + inc.getAulaTipo();
            chips.getChildren().add(crearChip(aula, "#64748b"));
        }
        if (inc.getFechaCreacion() != null) {
            String fecha = new SimpleDateFormat("dd MMM yyyy, HH:mm").format(inc.getFechaCreacion());
            chips.getChildren().add(crearChip("📅 " + fecha, "#475569"));
        }

        // ── Descripción ──
        Label desc = new Label(inc.getDescripcion());
        desc.setWrapText(true);
        desc.getStyleClass().add("incidencia-descripcion");
        desc.setMaxHeight(72);

        // ── Resolución y Botón Borrar ──
        VBox footer = new VBox(8);
        if ("ACABADO".equalsIgnoreCase(inc.getEstado()) && inc.getResolucion() != null && !inc.getResolucion().isEmpty()) {
            VBox resolucionBox = new VBox(4);
            resolucionBox.getStyleClass().add("resolucion-box");
            Label lblResTitulo = new Label("✅ Resolución:");
            lblResTitulo.getStyleClass().add("resolucion-titulo");
            Label lblResCuerpo = new Label(inc.getResolucion());
            lblResCuerpo.setWrapText(true);
            lblResCuerpo.getStyleClass().add("resolucion-cuerpo");
            resolucionBox.getChildren().addAll(lblResTitulo, lblResCuerpo);
            footer.getChildren().add(resolucionBox);

            // Botón de borrado
            Button btnBorrar = new Button("Borrar Ticket / Archivar");
            btnBorrar.setMaxWidth(Double.MAX_VALUE);
            btnBorrar.getStyleClass().addAll("action-button", "danger");
            btnBorrar.setOnAction(e -> eliminarIncidencia(inc.getId()));
            footer.getChildren().add(btnBorrar);
        }


        // ── Imagen miniatura ──
        HBox imagenContainer = new HBox();
        if (inc.getImagenRuta() != null && !inc.getImagenRuta().isEmpty()) {
            try {
                String ruta = inc.getImagenRuta();
                Image img;
                if (ruta.startsWith("http")) {
                    img = new Image(ruta, 110, 110, true, true, true);
                } else {
                    File imgFile = new File(ruta);
                    if (imgFile.exists()) {
                        img = new Image(imgFile.toURI().toString(), 110, 110, true, true, true);
                    } else {
                        img = null;
                    }
                }
                
                if (img != null) {
                    ImageView imgView = new ImageView(img);
                    imgView.getStyleClass().add("imagen-miniatura");
                    imgView.setFitWidth(110);
                    imgView.setFitHeight(110);
                    imgView.setPreserveRatio(true);
                    imgView.setOnMouseClicked(e -> mostrarImagenCompleta(inc.getImagenRuta()));
                    Label imgLbl = new Label("📎 Adjunto");
                    imgLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");
                    imagenContainer.getChildren().add(new VBox(4, imgLbl, imgView));
                }
            } catch (Exception ignored) {
            }
        }

        // ── Stepper visual ──
        HBox stepper = crearStepperVisual(inc.getEstado(), catColor);

        card.getChildren().addAll(header, chips, desc, imagenContainer, footer, stepper);
        return card;
    }

    private void eliminarIncidencia(int id) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar borrado");
        alert.setHeaderText("Eliminar incidencia resuelta");
        alert.setContentText("¿Estás seguro de que quieres borrar este ticket del historial?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (incidenciaDAO.deleteTicket(id)) {
                    cargarIncidencias();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setContentText("No se pudo eliminar la incidencia.");
                    error.show();
                }
            }
        });
    }



    /** Crea un chip/pill con fondo semitransparente del color dado */
    private Label crearChip(String texto, String color) {
        Label chip = new Label(texto);
        chip.setStyle(
                "-fx-background-color: " + hexToRgba(color, 0.12) + ";" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 10;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + hexToRgba(color, 0.3) + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;");
        return chip;
    }

    /** Convierte hex a rgba string para JavaFX inline style */
    private String hexToRgba(String hex, double alpha) {
        try {
            hex = hex.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return "rgba(" + r + "," + g + "," + b + "," + alpha + ")";
        } catch (Exception e) {
            return "rgba(100,116,139,0.15)";
        }
    }

    /** Stepper visual con círculos y líneas de conexión */
    private HBox crearStepperVisual(String estadoActual, String accentColor) {
        HBox stepper = new HBox(0);
        stepper.setAlignment(Pos.CENTER_LEFT);
        stepper.setStyle("-fx-padding: 10 0 0 0;");

        String[] pasos = { "LEIDO", "EN REVISION", "ACABADO" };
        String[] etiquetas = { "Leído", "En Revisión", "Acabado" };
        boolean passed = true;

        // NO LEIDO = ningún paso completado
        if ("NO LEIDO".equalsIgnoreCase(estadoActual))
            passed = false;

        for (int i = 0; i < pasos.length; i++) {
            boolean isActive = pasos[i].equalsIgnoreCase(estadoActual);
            boolean isDone = passed && !isActive;

            // Círculo del paso
            javafx.scene.layout.StackPane circulo = new javafx.scene.layout.StackPane();
            circulo.setMinSize(22, 22);
            circulo.setMaxSize(22, 22);

            if (isActive) {
                circulo.setStyle("-fx-background-color: " + accentColor
                        + "; -fx-background-radius: 11; -fx-effect: dropshadow(gaussian," + hexToRgba(accentColor, 0.5)
                        + ",8,0,0,0);");
                Label check = new Label("●");
                check.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
                circulo.getChildren().add(check);
            } else if (isDone) {
                circulo.setStyle("-fx-background-color: " + hexToRgba(accentColor, 0.3)
                        + "; -fx-background-radius: 11; -fx-border-color: " + accentColor
                        + "; -fx-border-radius: 11; -fx-border-width: 1;");
                Label check = new Label("✓");
                check.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 10px; -fx-font-weight: bold;");
                circulo.getChildren().add(check);
            } else {
                circulo.getStyleClass().add("stepper-circle-inactive");
            }

            // Etiqueta del paso
            Label lbl = new Label(etiquetas[i]);
            lbl.setStyle("-fx-font-size: 10px; -fx-padding: 0 0 0 4;");
            if (isActive) {
                lbl.setStyle(lbl.getStyle() + "-fx-text-fill: " + accentColor + "; -fx-font-weight: bold;");
            } else if (isDone) {
                lbl.getStyleClass().add("stepper-label-done");
            } else {
                lbl.getStyleClass().add("stepper-label-inactive");
            }

            VBox paso = new VBox(3);
            paso.setAlignment(Pos.CENTER);
            paso.getChildren().addAll(circulo, lbl);

            stepper.getChildren().add(paso);

            // Línea de conexión entre pasos
            if (i < pasos.length - 1) {
                javafx.scene.control.Separator linea = new javafx.scene.control.Separator();
                linea.setPrefWidth(40);
                if (isDone) {
                    linea.setStyle("-fx-background-color: " + accentColor + "; -fx-padding: 0 6;");
                } else {
                    linea.getStyleClass().add("stepper-line-inactive");
                    linea.setStyle("-fx-padding: 0 6;");
                }
                linea.setValignment(javafx.geometry.VPos.CENTER);
                HBox.setHgrow(linea, Priority.SOMETIMES);
                javafx.scene.layout.HBox lineBox = new javafx.scene.layout.HBox(linea);
                lineBox.setAlignment(Pos.CENTER);
                lineBox.setPrefHeight(22);
                lineBox.setStyle("-fx-padding: 0 4;");
                stepper.getChildren().add(lineBox);
            }

            if (isActive)
                passed = false;
        }

        return stepper;
    }

    private String getEstadoClass(String estado) {
        return switch (estado.toUpperCase()) {
            case "NO LEIDO" -> "no-leido";
            case "LEIDO" -> "leido";
            case "EN REVISION" -> "en-revision";
            case "ACABADO" -> "acabado";
            default -> "";
        };
    }

    private String getEstadoEmoji(String estado) {
        return switch (estado.toUpperCase()) {
            case "NO LEIDO" -> "🔴";
            case "LEIDO" -> "🔵";
            case "EN REVISION" -> "🟡";
            case "ACABADO" -> "🟢";
            default -> "⚪";
        };
    }

    private String getCategoryColor(String categoria) {
        if (categoria == null)
            return "#64748b";
        return switch (categoria.toLowerCase()) {
            case "hardware" -> "#fb923c";
            case "software" -> "#60a5fa";
            case "conectividad" -> "#22d3ee";
            case "mobiliario" -> "#34d399";
            default -> "#818cf8";
        };
    }

    private String getCategoryIcon(String categoria) {
        if (categoria == null)
            return "📌";
        return switch (categoria.toLowerCase()) {
            case "hardware" -> "🔧";
            case "software" -> "💻";
            case "conectividad" -> "📡";
            case "mobiliario" -> "🪑";
            default -> "📌";
        };
    }

    private void mostrarImagenCompleta(String imagenRuta) {
        try {
            String ruta = imagenRuta;
            Image img;
            if (ruta.startsWith("http")) {
                img = new Image(ruta, true);
            } else {
                File imgFile = new File(ruta);
                if (imgFile.exists()) {
                    img = new Image(imgFile.toURI().toString(), true);
                } else {
                    return;
                }
            }
                ImageView imgView = new ImageView(img);
                imgView.setPreserveRatio(true);
                imgView.setFitWidth(800);
                imgView.setFitHeight(600);

                Stage dialog = new Stage();
                dialog.setTitle("Imagen de Incidencia");

                ScrollPane scrollPane = new ScrollPane(imgView);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);

                javafx.scene.Scene scene = new javafx.scene.Scene(scrollPane, 820, 620);
                dialog.setScene(scene);
                dialog.show();
        } catch (Exception e) {
            System.err.println("Error mostrando imagen: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdjuntarImagen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));

        Stage stage = (Stage) adjuntarImagenBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            imagenSeleccionada = selectedFile;
            imagenLabel.setText(selectedFile.getName());
            imagenLabel.setStyle("-fx-text-fill: #10b981;"); // Verde para indicar selección exitosa
        }
    }

    @FXML
    private void handleCrearIncidencia() {
        String titulo = tituloField.getText();
        String desc = descripcionField.getText();
        String catNombre = categoriaCombo.getValue();
        com.example.aedusapp.models.Aula aulaSeleccionada = aulaCombo.getValue();

        if (titulo.isEmpty() || desc.isEmpty() || catNombre == null || aulaSeleccionada == null) {
            statusLabel.setText("Por favor rellena todos los campos.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        int aulaId = aulaSeleccionada.getId();
        int catId = 1; // Default ID
        if (catNombre != null) {
             switch (catNombre.toLowerCase()) {
                case "hardware": catId = 1; break;
                case "software": catId = 2; break;
                case "conectividad": catId = 3; break;
                case "mobiliario": catId = 4; break;
            }
        }

        String aulaTipo = aulaSeleccionada.getTipo();

        Incidencia nueva = new Incidencia();
        nueva.setTitulo(titulo);
        nueva.setDescripcion(desc);
        nueva.setUsuarioId(usuarioActual.getId());
        nueva.setAulaId(aulaId);
        nueva.setCategoriaId(catId);
        nueva.setAulaTipo(aulaTipo);

        com.example.aedusapp.controllers.general.MainController.showGlobalLoading(true, "Enviando incidencia...");

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                // Manejar imagen adjunta si existe
                if (imagenSeleccionada != null) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Subiendo imagen a la nube...");
                        statusLabel.setStyle("-fx-text-fill: #3b82f6;");
                    });
                    String url = com.example.aedusapp.services.media.PostImagesService.uploadImage(imagenSeleccionada);
                    if (url != null) {
                        nueva.setImagenRuta(url);
                    } else {
                        throw new RuntimeException("Error al subir imagen a Cloudinary.");
                    }
                }

                if (incidenciaDAO.createTicket(nueva)) {
                    // Registrar en el sistema de logs
                    LogService.logCrearIncidencia(usuarioActual, 0, titulo); // ID será asignado por BD
                    return true;
                }
                return false;
            }
        };

        task.setOnSucceeded(e -> {
            com.example.aedusapp.controllers.general.MainController.showGlobalLoading(false, "");
            if (task.getValue()) {
                statusLabel.setText("Incidencia creada con éxito.");
                statusLabel.setStyle("-fx-text-fill: green;");
                
                // Misión Diaria
                boolean misionCompletada = misionesDAO.registrarMisionDiaria(usuarioActual.getId(), "CREAR_TICKET", 10);
                if (misionCompletada) {
                    com.example.aedusapp.utils.ui.AlertUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "¡Misión Cumplida!", "+10 Aedus por crear tu primer ticket de hoy.");
                }

                tituloField.clear();
                aulaCombo.setValue(null);
                descripcionField.clear();
                categoriaCombo.setValue(null);
                imagenLabel.setText("");
                imagenSeleccionada = null;
                adjuntarImagenBtn.setStyle("");
                cargarIncidencias();
            } else {
                statusLabel.setText("Error al crear incidencia.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        task.setOnFailed(e -> {
            com.example.aedusapp.controllers.general.MainController.showGlobalLoading(false, "");
            statusLabel.setText(task.getException().getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        });

        new Thread(task).start();
    }
}
