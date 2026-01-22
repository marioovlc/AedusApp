package com.example.aedusapp.controllers;

import com.example.aedusapp.database.IncidenciaDAO;
import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.LogService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

// Controlador para la vista de Monitorización de Tickets
public class MonitorizacionController {

    @FXML
    private TableView<Incidencia> tablaIncidencias;
    @FXML
    private TableColumn<Incidencia, String> colId;
    @FXML
    private TableColumn<Incidencia, String> colTitulo;
    @FXML
    private TableColumn<Incidencia, String> colEstado;
    @FXML
    private TableColumn<Incidencia, String> colCreador;
    @FXML
    private TableColumn<Incidencia, String> colFecha;

    @FXML
    private VBox panelDetalles;
    @FXML
    private Label lblDetallesTitulo;
    @FXML
    private Text txtDetallesDescripcion;
    @FXML
    private ImageView imgDetalles;
    @FXML
    private Button btnMarcarLeido;
    @FXML
    private Button btnMarcarEnRevision;
    @FXML
    private Button btnMarcarAcabado;
    @FXML
    private Button btnEliminar;

    private IncidenciaDAO incidenciaDAO;
    private Usuario usuarioActual;
    private Incidencia incidenciaSeleccionada;

    @FXML
    public void initialize() {
        incidenciaDAO = new IncidenciaDAO();

        // Configurar columnas de la tabla
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colTitulo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitulo()));
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEstado()));
        colCreador.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreadorNombre() != null ? data.getValue().getCreadorNombre() : "Desconocido"));
        colFecha.setCellValueFactory(data -> {
            if (data.getValue().getFechaCreacion() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                return new SimpleStringProperty(sdf.format(data.getValue().getFechaCreacion()));
            }
            return new SimpleStringProperty("");
        });

        // Aplicar estilo personalizado a la columna de estado
        colEstado.setCellFactory(column -> new TableCell<Incidencia, String>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Label badge = new Label(estado);
                    badge.getStyleClass().add("status-badge");

                    // Aplicar clase CSS según el estado
                    switch (estado.toUpperCase()) {
                        case "NO LEIDO" -> badge.getStyleClass().add("status-no-leido");
                        case "LEIDO" -> badge.getStyleClass().add("status-leido");
                        case "EN REVISION" -> badge.getStyleClass().add("status-en-revision");
                        case "ACABADO" -> badge.getStyleClass().add("status-acabado");
                    }

                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Listener para cuando se selecciona una fila
        tablaIncidencias.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                mostrarDetalles(newSelection);
            }
        });

        // Configurar botones de acción
        btnMarcarLeido.setOnAction(e -> cambiarEstado("LEIDO"));
        btnMarcarEnRevision.setOnAction(e -> cambiarEstado("EN REVISION"));
        btnMarcarAcabado.setOnAction(e -> cambiarEstado("ACABADO"));
        btnEliminar.setOnAction(e -> eliminarTicket());

        // Ocultar panel de detalles inicialmente
        panelDetalles.setVisible(false);
        panelDetalles.setManaged(false);
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        cargarIncidencias();
    }

    private void cargarIncidencias() {
        List<Incidencia> incidencias = incidenciaDAO.obtenerTodasIncidencias();
        tablaIncidencias.getItems().clear();
        tablaIncidencias.getItems().addAll(incidencias);
    }

    private void mostrarDetalles(Incidencia incidencia) {
        this.incidenciaSeleccionada = incidencia;

        // Mostrar panel de detalles
        panelDetalles.setVisible(true);
        panelDetalles.setManaged(true);

        // Actualizar información
        lblDetallesTitulo.setText(incidencia.getTitulo());
        txtDetallesDescripcion.setText(incidencia.getDescripcion());

        // Mostrar imagen si existe
        if (incidencia.getImagenRuta() != null && !incidencia.getImagenRuta().isEmpty()) {
            try {
                File imgFile = new File(incidencia.getImagenRuta());
                if (imgFile.exists()) {
                    Image image = new Image(imgFile.toURI().toString());
                    imgDetalles.setImage(image);
                    imgDetalles.setVisible(true);
                    imgDetalles.setManaged(true);
                } else {
                    imgDetalles.setVisible(false);
                    imgDetalles.setManaged(false);
                }
            } catch (Exception e) {
                imgDetalles.setVisible(false);
                imgDetalles.setManaged(false);
            }
        } else {
            imgDetalles.setVisible(false);
            imgDetalles.setManaged(false);
        }

        // Habilitar/deshabilitar botones según el estado actual
        actualizarBotonesEstado(incidencia.getEstado());
    }

    private void actualizarBotonesEstado(String estadoActual) {
        // Deshabilitar el botón del estado actual
        btnMarcarLeido.setDisable("LEIDO".equalsIgnoreCase(estadoActual));
        btnMarcarEnRevision.setDisable("EN REVISION".equalsIgnoreCase(estadoActual));
        btnMarcarAcabado.setDisable("ACABADO".equalsIgnoreCase(estadoActual));
    }

    private void cambiarEstado(String nuevoEstado) {
        if (incidenciaSeleccionada == null) {
            mostrarAlerta("Error", "No hay ninguna incidencia seleccionada.", Alert.AlertType.WARNING);
            return;
        }

        if (incidenciaDAO.actualizarEstado(incidenciaSeleccionada.getId(), nuevoEstado)) {
            // Registrar en el sistema de logs
            LogService.logCambiarEstado(usuarioActual, incidenciaSeleccionada.getId(),
                    incidenciaSeleccionada.getTitulo(), nuevoEstado);

            mostrarAlerta("Éxito", "Estado actualizado correctamente a: " + nuevoEstado,
                    Alert.AlertType.INFORMATION);
            cargarIncidencias();

            // Actualizar el estado en la selección actual
            incidenciaSeleccionada.setEstado(nuevoEstado);
            actualizarBotonesEstado(nuevoEstado);
        } else {
            mostrarAlerta("Error", "No se pudo actualizar el estado.", Alert.AlertType.ERROR);
        }
    }

    private void eliminarTicket() {
        if (incidenciaSeleccionada == null) {
            mostrarAlerta("Error", "No hay ninguna incidencia seleccionada.", Alert.AlertType.WARNING);
            return;
        }

        // Confirmar eliminación
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro de eliminar este ticket?");
        confirmacion.setContentText("Ticket #" + incidenciaSeleccionada.getId() + ": "
                + incidenciaSeleccionada.getTitulo() + "\n\nEsta acción no se puede deshacer.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            if (incidenciaDAO.eliminarIncidencia(incidenciaSeleccionada.getId())) {
                // Registrar en el sistema de logs
                LogService.logEliminarIncidencia(usuarioActual, incidenciaSeleccionada.getId(),
                        incidenciaSeleccionada.getTitulo());

                mostrarAlerta("Éxito", "Ticket eliminado correctamente.", Alert.AlertType.INFORMATION);

                // Ocultar panel de detalles
                panelDetalles.setVisible(false);
                panelDetalles.setManaged(false);
                incidenciaSeleccionada = null;

                // Recargar tabla
                cargarIncidencias();
            } else {
                mostrarAlerta("Error", "No se pudo eliminar el ticket.", Alert.AlertType.ERROR);
            }
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
