package com.example.aedusapp.controllers;

import com.example.aedusapp.models.Incidencia;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Label;
import com.example.aedusapp.models.Usuario;

// Controlador Principal del Dashboard (Panel de Control)
public class MainController {

    // Campos para añadir nueva incidencia
    @FXML
    private TextField txtTitulo;
    @FXML
    private TextField txtDescripcion;
    @FXML
    private Button btnAgregar;

    // Tabla de Incidencias
    @FXML
    private TableView<Incidencia> tableIncidencias;
    @FXML
    private TableColumn<Incidencia, Integer> colId;
    @FXML
    private TableColumn<Incidencia, String> colTitulo;
    @FXML
    private TableColumn<Incidencia, String> colDescripcion;
    @FXML
    private TableColumn<Incidencia, String> colEstado;

    @FXML
    private TextField txtSearch; // Campo de búsqueda (aún no implementado del todo)
    @FXML
    private Label lblUsuario; // Etiqueta con el nombre del usuario conectado

    // Botones del menú lateral
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnIncidencias;
    @FXML
    private Button btnUsuarios; // Solo visible para administradores
    @FXML
    private Button btnConfiguracion;
    @FXML
    private Button btnCerrarSesion;

    @FXML
    private javafx.scene.layout.VBox dashboardView; // La vista central

    // Configuración inicial al abrir la ventana
    @FXML
    public void initialize() {
        // Configurar columnas de la tabla
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Datos de prueba (falsos) para ver algo en la tabla
        ObservableList<Incidencia> mockData = FXCollections.observableArrayList();
        tableIncidencias.setItems(mockData);

        // Conectar botones con sus acciones
        btnAgregar.setOnAction(event -> agregarIncidencia());
        btnDashboard.setOnAction(event -> mostrarDashboard());
        btnUsuarios.setOnAction(event -> cargarVistaUsuarios());
        btnCerrarSesion.setOnAction(event -> cerrarSesion());
    }

    // Volver a la vista principal
    private void mostrarDashboard() {
        try {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) btnDashboard.getScene().getRoot();
            root.setCenter(dashboardView);

            // Cambiar estilo de botones para mostrar cual está activo
            btnDashboard.getStyleClass().add("active");
            btnIncidencias.getStyleClass().remove("active");
            btnUsuarios.getStyleClass().remove("active");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Añadir una nueva incidencia (por ahora solo en la tabla visual)
    private void agregarIncidencia() {
        String titulo = txtTitulo.getText();
        String descripcion = txtDescripcion.getText();

        if (titulo != null && !titulo.isEmpty() && descripcion != null && !descripcion.isEmpty()) {
            int newId = tableIncidencias.getItems().size() + 1;
            Incidencia nueva = new Incidencia(newId, titulo, descripcion, "Pendiente");
            tableIncidencias.getItems().add(nueva);
            // Limpiar campos
            txtTitulo.clear();
            txtDescripcion.clear();
        }
    }

    // Configurar quién es el usuario conectado
    public void setUsuario(Usuario usuario) {
        if (lblUsuario != null && usuario != null) {
            lblUsuario.setText(usuario.getNombre());

            // Si NO es administrador (rol 1), ocultar botón de Usuarios
            if (usuario.getRoleId() != 1) {
                btnUsuarios.setVisible(false);
                btnUsuarios.setManaged(false);
            } else {
                btnUsuarios.setVisible(true);
                btnUsuarios.setManaged(true);
            }
        }
    }

    // Cargar la pantalla de administración de usuarios en el centro
    private void cargarVistaUsuarios() {
        try {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) btnUsuarios.getScene().getRoot();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/admin_users.fxml"));
            root.setCenter(loader.load());

            // Actualizar botones activos
            btnDashboard.getStyleClass().remove("active");
            btnIncidencias.getStyleClass().remove("active");
            btnUsuarios.getStyleClass().add("active");

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Cerrar sesión y volver al Login
    private void cerrarSesion() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 800, 600);
            String css = getClass().getResource("/com/example/aedusapp/styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            javafx.stage.Stage stage = (javafx.stage.Stage) btnCerrarSesion.getScene().getWindow();
            stage.setTitle("Aedus");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
