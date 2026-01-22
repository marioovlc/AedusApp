package com.example.aedusapp.controllers;

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

        List<Log> logs;

        // Estrategia de filtrado: Prioridad a fecha, luego usuario, luego categoría
        // Nota: Idealmente esto se haría con una sola consulta dinámica en el DAO.
        // Aquí combinamos DAO + filtrado en memoria para simplificar.

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

        // Aplicar filtros adicionales en memoria (intersección)
        if (fechaInicio != null && fechaFin != null) {
            // Ya filtrado por fecha en DAO si fue el primer if, pero si entramos por otro lado (no posible con if-else if)
            // Revisar lógica:
            // Si el usuario seleccionó rango, usamos eso como base. Luego filtramos por usuario y categoría.
        }
        
        // Mejor aproximación: Filtrar en memoria sobre la lista obtenida
        if (usuarioBusqueda != null && !usuarioBusqueda.trim().isEmpty()) {
            String lowerBusqueda = usuarioBusqueda.toLowerCase();
            logs = logs.stream()
                    .filter(l -> (l.getUsuarioNombre() != null && l.getUsuarioNombre().toLowerCase().contains(lowerBusqueda)))
                    .collect(Collectors.toList());
        }

        if (categoriaSeleccionada != null && !"Todas".equals(categoriaSeleccionada)) {
            String cat = categoriaSeleccionada;
            logs = logs.stream()
                    .filter(l -> l.getCategoria().equals(cat))
                    .collect(Collectors.toList());
        }

        tablaLogs.getItems().clear();
        tablaLogs.getItems().addAll(logs);
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
        mostrarAlerta("Logs actualizados", "Los registros se han recargado correctamente.",
                Alert.AlertType.INFORMATION);
    }

    private void actualizarEstadisticas() {
        int total = logDAO.contarTodosLogs();
        int login = logDAO.contarLogsPorCategoria("LOGIN");
        int incidencia = logDAO.contarLogsPorCategoria("INCIDENCIA");
        int usuario = logDAO.contarLogsPorCategoria("USUARIO");
        int error = logDAO.contarLogsPorCategoria("ERROR");

        lblTotalLogs.setText(String.valueOf(total));
        lblLogin.setText(String.valueOf(login));
        lblIncidencia.setText(String.valueOf(incidencia));
        lblUsuario.setText(String.valueOf(usuario));
        lblError.setText(String.valueOf(error));
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
