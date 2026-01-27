package com.example.aedusapp.controllers.incidencias;

import com.example.aedusapp.database.IncidenciaDAO;
import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.LogService;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.List;

public class IncidenciasController {

    @FXML
    private VBox incidenciasContainer;
    @FXML
    private TextField tituloField;
    @FXML
    private TextField aulaField;
    @FXML
    private ComboBox<String> aulaTipoCombo;
    @FXML
    private TextArea descripcionField;
    @FXML
    private ComboBox<String> categoriaCombo;
    @FXML
    private Label statusLabel; // Para mensajes de error/éxito
    @FXML
    private Button adjuntarImagenBtn;
    @FXML
    private Label imagenLabel;

    private IncidenciaDAO incidenciaDAO;
    private File imagenSeleccionada;
    private Usuario usuarioActual;

    // Pagination
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 5;
    private Button loadMoreButton;

    public void initialize() {
        incidenciaDAO = new IncidenciaDAO();
        // Cargar categorias hardcodeadas por ahora o de BD
        categoriaCombo.getItems().addAll("Hardware", "Software", "Conectividad", "Mobiliario");

        // Cargar tipos de aula
        aulaTipoCombo.getItems().addAll("Informática", "Matemáticas", "Idiomas", "Ciencias", "General");
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
        // Estilo simple para el botón
        loadMoreButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #3b82f6; -fx-cursor: hand; -fx-underline: true;");
        loadMoreButton.setMaxWidth(Double.MAX_VALUE);
        loadMoreButton.setOnAction(e -> cargarPaginaIncidencias());

        cargarPaginaIncidencias();
    }

    private void cargarPaginaIncidencias() {
        // Remover botón si existe para evitar duplicados o clicks múltiples
        incidenciasContainer.getChildren().remove(loadMoreButton);

        Label loadingLabel = new Label("Cargando...");
        loadingLabel.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 10; -fx-alignment: center;");
        loadingLabel.setMaxWidth(Double.MAX_VALUE);
        loadingLabel.setAlignment(Pos.CENTER);
        incidenciasContainer.getChildren().add(loadingLabel);

        javafx.concurrent.Task<List<Incidencia>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Incidencia> call() throws Exception {
                return incidenciaDAO.obtenerIncidenciasPorUsuarioPaginado(usuarioActual.getId(), PAGE_SIZE,
                        currentOffset);
            }
        };

        task.setOnSucceeded(e -> {
            incidenciasContainer.getChildren().remove(loadingLabel);
            List<Incidencia> incidencias = task.getValue();

            if (incidencias.isEmpty() && currentOffset == 0) {
                Label noIncidencias = new Label("No tienes incidencias creadas aún.");
                noIncidencias.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
                incidenciasContainer.getChildren().add(noIncidencias);
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
            incidenciasContainer.getChildren().remove(loadingLabel);
            Label errorLabel = new Label("Error al cargar incidencias.");
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            incidenciasContainer.getChildren().add(errorLabel);
            task.getException().printStackTrace();

            // Si falló (ej. timeout), permitimos reintentar mostrando el botón de nuevo si
            // no es la primera carga
            if (currentOffset > 0) {
                incidenciasContainer.getChildren().add(loadMoreButton);
            }
        });

        new Thread(task).start();
    }

    private VBox crearTarjetaIncidencia(Incidencia inc) {
        VBox card = new VBox(12);
        card.getStyleClass().add("incidencia-card");

        // Encabezado: Título + Badge de estado
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label(inc.getTitulo());
        titulo.getStyleClass().add("incidencia-titulo");
        HBox.setHgrow(titulo, Priority.ALWAYS);

        Label estadoBadge = new Label(inc.getEstado());
        estadoBadge.getStyleClass().addAll("estado-badge", getEstadoClass(inc.getEstado()));

        header.getChildren().addAll(titulo, estadoBadge);

        // Metadatos: Categoría, Aula, Fecha
        HBox metadata = new HBox(15);
        metadata.getStyleClass().add("incidencia-metadata");

        if (inc.getCategoriaNombre() != null) {
            Label categoria = new Label("📁 " + inc.getCategoriaNombre());
            categoria.getStyleClass().add("metadata-item");
            metadata.getChildren().add(categoria);
        }

        if (inc.getAulaNombre() != null) {
            String aulaInfo = "🚪 Aula " + inc.getAulaNombre();
            if (inc.getAulaTipo() != null && !inc.getAulaTipo().isEmpty()) {
                aulaInfo += " (" + inc.getAulaTipo() + ")";
            }
            Label aula = new Label(aulaInfo);
            aula.getStyleClass().add("metadata-item");
            metadata.getChildren().add(aula);
        }

        if (inc.getFechaCreacion() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");
            Label fecha = new Label("📅 " + sdf.format(inc.getFechaCreacion()));
            fecha.getStyleClass().add("metadata-item");
            metadata.getChildren().add(fecha);
        }

        // Descripción
        Label desc = new Label(inc.getDescripcion());
        desc.setWrapText(true);
        desc.getStyleClass().add("incidencia-descripcion");
        desc.setMaxHeight(80);

        // Contenedor para imagen (si existe)
        HBox imagenContainer = new HBox();
        if (inc.getImagenRuta() != null && !inc.getImagenRuta().isEmpty()) {
            try {
                File imgFile = new File(inc.getImagenRuta());
                if (imgFile.exists()) {
                    Image img = new Image(imgFile.toURI().toString(), 100, 100, true, true);
                    ImageView imgView = new ImageView(img);
                    imgView.getStyleClass().add("imagen-miniatura");
                    imgView.setFitWidth(100);
                    imgView.setFitHeight(100);
                    imgView.setPreserveRatio(true);

                    // Click para ver imagen completa
                    imgView.setOnMouseClicked(e -> mostrarImagenCompleta(inc.getImagenRuta()));

                    Label imgLabel = new Label("📎 Imagen adjunta:");
                    imgLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                    VBox imgBox = new VBox(5, imgLabel, imgView);
                    imagenContainer.getChildren().add(imgBox);
                }
            } catch (Exception e) {
                System.err.println("Error cargando imagen: " + e.getMessage());
            }
        }

        // Barra de progreso
        HBox progressBar = crearBarraProgreso(inc.getEstado());

        card.getChildren().addAll(header, metadata, desc, imagenContainer, progressBar);
        return card;
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

    private void mostrarImagenCompleta(String imagenRuta) {
        try {
            File imgFile = new File(imagenRuta);
            if (imgFile.exists()) {
                Image img = new Image(imgFile.toURI().toString());
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
            }
        } catch (Exception e) {
            System.err.println("Error mostrando imagen: " + e.getMessage());
        }
    }

    // Amazon-style progress bar
    private HBox crearBarraProgreso(String estado) {
        HBox box = new HBox(0);
        box.getStyleClass().add("progress-bar-container");

        String[] estados = { "LEIDO", "EN REVISION", "ACABADO" };
        boolean active = true;

        for (int i = 0; i < estados.length; i++) {
            Label step = new Label(estados[i]);
            step.getStyleClass().add("progress-step");

            if (active) {
                step.getStyleClass().add("active");
            }

            // Si encontramos el estado actual, los siguientes no estarán activos
            if (estado.equalsIgnoreCase(estados[i])) {
                active = false;
            } else if (active && i == estados.length - 1) {
                // Si llegamos al final y sigue active (caso raro o estados intermedios no
                // mapeados), manejarlo
            }

            box.getChildren().add(step);

            // Separador (menos después del último)
            if (i < estados.length - 1) {
                Label separator = new Label(" > ");
                separator.getStyleClass().add("progress-separator");
                if (active)
                    separator.getStyleClass().add("active"); // Separador activo si el siguiente paso va a ser activo?
                                                             // No, simplifiquemos
                box.getChildren().add(separator);
            }
        }
        return box;
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
        String aulaTipo = aulaTipoCombo.getValue();

        String aulaTexto = aulaField.getText();

        if (titulo.isEmpty() || desc.isEmpty() || catNombre == null || aulaTexto.isEmpty() || aulaTipo == null) {
            statusLabel.setText("Por favor rellena todos los campos.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        int aulaId = 1;
        try {
            aulaId = Integer.parseInt(aulaTexto);
        } catch (NumberFormatException e) {
            statusLabel.setText("El aula debe ser un número válido.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Mapeo simple de nombre a ID de categoría (esto debería ser dinámico
        // idealmente)
        int catId = switch (catNombre) {
            case "Hardware" -> 1;
            case "Software" -> 2;
            case "Conectividad" -> 3;
            case "Mobiliario" -> 4;
            default -> 1;
        };

        Incidencia nueva = new Incidencia();
        nueva.setTitulo(titulo);
        nueva.setDescripcion(desc);
        nueva.setUsuarioId(usuarioActual.getId());
        nueva.setAulaId(aulaId);
        nueva.setCategoriaId(catId);
        nueva.setAulaTipo(aulaTipo);

        // Manejar imagen adjunta si existe
        if (imagenSeleccionada != null) {
            try {
                // Crear directorio uploads/incidencias si no existe
                Path uploadsDir = Paths.get("uploads", "incidencias");
                Files.createDirectories(uploadsDir);

                // Generar nombre único para la imagen
                String timestamp = String.valueOf(System.currentTimeMillis());
                String extension = imagenSeleccionada.getName()
                        .substring(imagenSeleccionada.getName().lastIndexOf("."));
                String nuevoNombre = "incidencia_" + timestamp + extension;

                // Copiar archivo
                Path destino = uploadsDir.resolve(nuevoNombre);
                Files.copy(imagenSeleccionada.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

                // Guardar ruta relativa en el modelo
                nueva.setImagenRuta(destino.toString());

            } catch (IOException e) {
                statusLabel.setText("Error al guardar la imagen: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        }

        if (incidenciaDAO.crearIncidencia(nueva)) {
            // Registrar en el sistema de logs
            LogService.logCrearIncidencia(usuarioActual, 0, titulo); // ID será asignado por BD

            statusLabel.setText("Incidencia creada con éxito.");
            statusLabel.setStyle("-fx-text-fill: green;");
            tituloField.clear();
            aulaField.clear();
            descripcionField.clear();
            categoriaCombo.setValue(null);
            aulaTipoCombo.setValue(null);
            imagenLabel.setText("");
            imagenSeleccionada = null;
            cargarIncidencias();
        } else {
            statusLabel.setText("Error al crear incidencia.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }
}
