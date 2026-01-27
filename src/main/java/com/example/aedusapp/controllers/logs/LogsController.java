package com.example.aedusapp.controllers.logs;

import com.example.aedusapp.database.LogDAO;
import com.example.aedusapp.models.Log;
import com.example.aedusapp.models.Usuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

// Controlador para la vista de Logs del sistema
public class LogsController {

    @FXML
    private TableView<Log> tablaLogs;
    @FXML
    private TableColumn<Log, String> colFecha;
    @FXML
    private TableColumn<Log, String> colUsuario;
    @FXML
    private TableColumn<Log, String> colCategoria;
    @FXML
    private TableColumn<Log, String> colAccion;
    @FXML
    private TableColumn<Log, String> colDescripcion;

    @FXML
    private ComboBox<String> filtroCategoria;
    @FXML
    private TextField txtBuscarUsuario;
    @FXML
    private DatePicker dpFechaInicio;
    @FXML
    private DatePicker dpFechaFin;

    @FXML
    private Label lblTotalLogs;
    @FXML
    private Label lblLogin;
    @FXML
    private Label lblIncidencia;
    @FXML
    private Label lblUsuario;
    @FXML
    private Label lblError;

    private LogDAO logDAO;
    private Usuario usuarioActual;

    @FXML
    public void initialize() {
        logDAO = new LogDAO();

        // Configurar ComboBox de filtros
        filtroCategoria.getItems().addAll("Todas", "LOGIN", "INCIDENCIA", "USUARIO", "SISTEMA", "ERROR");
        filtroCategoria.setValue("Todas");

        // Configurar columnas de la tabla
        colFecha.setCellValueFactory(data -> {
            if (data.getValue().getFechaCreacion() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                return new SimpleStringProperty(sdf.format(data.getValue().getFechaCreacion()));
            }
            return new SimpleStringProperty("");
        });

        colUsuario.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUsuarioNombre() != null ? data.getValue().getUsuarioNombre() : "Sistema"));

        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria()));

        colAccion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAccion()));

        colDescripcion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescripcion()));

        // Aplicar estilo a la columna de categoría con badges
        colCategoria.setCellFactory(column -> new TableCell<Log, String>() {
            @Override
            protected void updateItem(String categoria, boolean empty) {
                super.updateItem(categoria, empty);
                if (empty || categoria == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Label badge = new Label(categoria);
                    badge.getStyleClass().add("log-badge");

                    // Aplicar clase CSS según la categoría
                    switch (categoria.toUpperCase()) {
                        case "LOGIN" -> badge.getStyleClass().add("log-login");
                        case "INCIDENCIA" -> badge.getStyleClass().add("log-incidencia");
                        case "USUARIO" -> badge.getStyleClass().add("log-usuario");
                        case "SISTEMA" -> badge.getStyleClass().add("log-sistema");
                        case "ERROR" -> badge.getStyleClass().add("log-error");
                    }

                    HBox container = new HBox(badge);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        // Listener para filtro de categoría
        filtroCategoria.setOnAction(e -> aplicarFiltros());
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        cargarLogs();
        actualizarEstadisticas();
    }

    private void cargarLogs() {
        String categoriaSeleccionada = filtroCategoria.getValue();
        String usuarioBusqueda = txtBuscarUsuario.getText();
        LocalDate fechaInicio = dpFechaInicio.getValue();
        LocalDate fechaFin = dpFechaFin.getValue();

        tablaLogs.setPlaceholder(new Label("Cargando logs..."));
        tablaLogs.getItems().clear();

        javafx.concurrent.Task<List<Log>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Log> call() throws Exception {
                List<Log> logs;
                if (fechaInicio != null && fechaFin != null) {
                    Date inicio = Date.from(fechaInicio.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date fin = Date.from(fechaFin.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    logs = logDAO.obtenerLogsPorRangoFechas(inicio, fin);
                } else if (usuarioBusqueda != null && !usuarioBusqueda.isEmpty()) {
                    logs = logDAO.obtenerLogsPorNombreUsuario(usuarioBusqueda);
                } else if (categoriaSeleccionada != null && !"Todas".equals(categoriaSeleccionada)) {
                    logs = logDAO.obtenerLogsPorCategoria(categoriaSeleccionada);
                } else {
                    logs = logDAO.obtenerTodosLogs();
                }

                if (usuarioBusqueda != null && !usuarioBusqueda.trim().isEmpty()) {
                    String lowerBusqueda = usuarioBusqueda.toLowerCase();
                    logs = logs.stream()
                            .filter(l -> (l.getUsuarioNombre() != null
                                    && l.getUsuarioNombre().toLowerCase().contains(lowerBusqueda)))
                            .collect(Collectors.toList());
                }

                if (categoriaSeleccionada != null && !"Todas".equals(categoriaSeleccionada)) {
                    String cat = categoriaSeleccionada;
                    logs = logs.stream()
                            .filter(l -> l.getCategoria().equals(cat))
                            .collect(Collectors.toList());
                }
                return logs;
            }
        };

        task.setOnSucceeded(e -> {
            tablaLogs.getItems().setAll(task.getValue());
            if (task.getValue().isEmpty()) {
                tablaLogs.setPlaceholder(new Label("No se encontraron registros."));
            }
            actualizarEstadisticas();
        });

        task.setOnFailed(e -> {
            tablaLogs.setPlaceholder(new Label("Error al cargar registros."));
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    @FXML
    private void aplicarFiltros() {
        cargarLogs();
        actualizarEstadisticas();
    }

    @FXML
    private void refrescarLogs() {
        cargarLogs();
        actualizarEstadisticas();
        com.example.aedusapp.utils.AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Logs actualizados",
                "Los registros se han recargado correctamente.");
    }

    private void actualizarEstadisticas() {
        javafx.concurrent.Task<int[]> statsTask = new javafx.concurrent.Task<>() {
            @Override
            protected int[] call() throws Exception {
                return new int[] {
                        logDAO.contarTodosLogs(),
                        logDAO.contarLogsPorCategoria("LOGIN"),
                        logDAO.contarLogsPorCategoria("INCIDENCIA"),
                        logDAO.contarLogsPorCategoria("USUARIO"),
                        logDAO.contarLogsPorCategoria("ERROR")
                };
            }
        };

        statsTask.setOnSucceeded(e -> {
            int[] stats = statsTask.getValue();
            lblTotalLogs.setText(String.valueOf(stats[0]));
            lblLogin.setText(String.valueOf(stats[1]));
            lblIncidencia.setText(String.valueOf(stats[2]));
            lblUsuario.setText(String.valueOf(stats[3]));
            lblError.setText(String.valueOf(stats[4]));
        });

        new Thread(statsTask).start();
    }

}
