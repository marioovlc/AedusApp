package com.example.aedusapp.controllers.incidencias;

import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Usuario;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
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

    private final com.example.aedusapp.services.IncidenciasService incidenciasService = new com.example.aedusapp.services.IncidenciasService();
    private File imagenSeleccionada;
    private Usuario usuarioActual;

    // Pagination
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 5;
    private Button loadMoreButton;



    public void initialize() {
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
                return incidenciasService.buscarSugerenciaFAQ(text);
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
        com.example.aedusapp.utils.ConcurrencyManager.submit(task);
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
                return incidenciasService.pedirSugerenciaIA(consulta);
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
            Throwable ex = task.getException();
            if (ex instanceof com.example.aedusapp.exceptions.AIException) {
                lblSugerenciaIA.setText("⚠️ No se pudo obtener sugerencia de la IA. Inténtalo de nuevo.");
            } else {
                lblSugerenciaIA.setText("⚠️ Error inesperado: " + ex.getMessage());
            }
        }));
        com.example.aedusapp.utils.ConcurrencyManager.submit(task);
    }

    private void cargarAulas() {
        List<com.example.aedusapp.models.Aula> aulas = incidenciasService.obtenerAulas();
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
                if(incidenciasService.crearAula(newAula)) {
                    cargarAulas();
                    // Seleccionar la nueva aula
                    for(com.example.aedusapp.models.Aula a : aulaCombo.getItems()) {
                        if(a.getNombre().equals(newAula.getNombre())) {
                            aulaCombo.setValue(a);
                            break;
                        }
                    }
                } else {
                    org.controlsfx.control.Notifications.create()
                            .title("Error")
                            .text("No se pudo crear el aula. Verifique que el nombre no exista ya.")
                            .showError();
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
                return incidenciasService.obtenerIncidencias(usuarioActual.getId(), PAGE_SIZE, currentOffset);
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
                    com.example.aedusapp.components.TarjetaIncidencia tarjeta = new com.example.aedusapp.components.TarjetaIncidencia(inc, this::eliminarIncidencia, this::mostrarImagenCompleta);
                    incidenciasContainer.getChildren().add(tarjeta);
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

        com.example.aedusapp.utils.ConcurrencyManager.submit(task);
    }



    private void eliminarIncidencia(int id) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar borrado");
        alert.setHeaderText("Eliminar incidencia resuelta");
        alert.setContentText("¿Estás seguro de que quieres borrar este ticket del historial?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (incidenciasService.borrarIncidencia(id)) {
                    cargarIncidencias();
                } else {
                    org.controlsfx.control.Notifications.create()
                            .title("Error")
                            .text("No se pudo eliminar la incidencia.")
                            .showError();
                }
            }
        });
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
        int catId;
        try {
            catId = com.example.aedusapp.models.CategoriaIncidencia.fromNombre(catNombre).getId();
        } catch (Exception ex) {
            catId = 1; // Fallback por defecto: Hardware
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
                if (imagenSeleccionada != null) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Subiendo imagen a la nube y creando incidencia...");
                        statusLabel.setStyle("-fx-text-fill: #3b82f6;");
                    });
                }
                return incidenciasService.crearIncidenciaCompleta(nueva, usuarioActual, imagenSeleccionada);
            }
        };

        task.setOnSucceeded(e -> {
            com.example.aedusapp.controllers.general.MainController.showGlobalLoading(false, "");
            if (task.getValue()) {
                statusLabel.setText("Incidencia creada con éxito.");
                statusLabel.setStyle("-fx-text-fill: green;");
                
                // Misión Diaria
                boolean misionCompletada = incidenciasService.verificarYOtorgarMisionDiaria(usuarioActual.getId());
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

        com.example.aedusapp.utils.ConcurrencyManager.submit(task);
    }
}
