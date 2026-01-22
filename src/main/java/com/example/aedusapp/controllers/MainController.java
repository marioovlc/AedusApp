package com.example.aedusapp.controllers;

import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.LogService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

// Controlador Principal del Dashboard (Panel de Control)
public class MainController {

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
    private Button btnLogs; // Solo visible para administradores (Logs)
    @FXML
    private Button btnMonitorizacion; // Solo visible para administradores
    @FXML
    private Button btnConfiguracion;
    @FXML
    private Button btnCerrarSesion;
    @FXML
    private Button btnMantenimiento;

    @FXML
    private javafx.scene.layout.VBox dashboardView; // La vista central

    // Configuración inicial al abrir la ventana
    @FXML
    public void initialize() {
        // Conectar botones con sus acciones
        btnDashboard.setOnAction(event -> mostrarDashboard());
        btnIncidencias.setOnAction(event -> cargarVistaIncidencias());
        btnUsuarios.setOnAction(event -> cargarVistaUsuarios());
        if (btnLogs != null)
             btnLogs.setOnAction(event -> cargarVistaLogs());
        btnMonitorizacion.setOnAction(event -> cargarVistaMonitorizacion());
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
            if (btnMonitorizacion != null)
                btnMonitorizacion.getStyleClass().remove("active");
            if (btnMantenimiento != null)
                btnMantenimiento.getStyleClass().remove("active");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Usuario currentUser;

    // Configurar quién es el usuario conectado
    public void setUsuario(Usuario usuario) {
        this.currentUser = usuario;
        if (lblUsuario != null && usuario != null) {
            lblUsuario.setText(usuario.getNombre());

            // Si NO es administrador (rol 1), ocultar botones de admin
            if (!usuario.hasRole(1)) {
                btnUsuarios.setVisible(false);
                btnUsuarios.setManaged(false);
                if (btnMonitorizacion != null) {
                    btnMonitorizacion.setVisible(false);
                    btnMonitorizacion.setManaged(false);
                }
                if (btnLogs != null) {
                    btnLogs.setVisible(false);
                    btnLogs.setManaged(false);
                }
            } else {
                btnUsuarios.setVisible(true);
                btnUsuarios.setManaged(true);
                if (btnMonitorizacion != null) {
                    btnMonitorizacion.setVisible(true);
                    btnMonitorizacion.setManaged(true);
                }
                if (btnLogs != null) {
                    btnLogs.setVisible(true);
                    btnLogs.setManaged(true);
                }
            }

            // Si es mantenimiento (rol 3) o tiene acceso
            if (usuario.hasRole(3) || usuario.hasRole(1)) {
                if (btnMantenimiento != null) {
                    btnMantenimiento.setVisible(true);
                    btnMantenimiento.setManaged(true);
                    btnMantenimiento.setOnAction(e -> cargarVistaMantenimiento());
                }
            } else {
                if (btnMantenimiento != null) {
                    btnMantenimiento.setVisible(false);
                    btnMantenimiento.setManaged(false);
                }
            }
        }
    }
    
    // Cargar vista de Logs
    private void cargarVistaLogs() {
        try {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) btnLogs.getScene().getRoot();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/logs.fxml"));
            root.setCenter(loader.load());

            // Pasar usuario
            LogsController controller = loader.getController();
            if (this.currentUser != null) {
                controller.setUsuarioActual(this.currentUser);
            }

            // Actualizar botones activos
            btnDashboard.getStyleClass().remove("active");
            btnIncidencias.getStyleClass().remove("active");
            btnUsuarios.getStyleClass().remove("active");
            if (btnMonitorizacion != null)
                btnMonitorizacion.getStyleClass().remove("active");
            if (btnMantenimiento != null)
                btnMantenimiento.getStyleClass().remove("active");
            if (btnLogs != null)
                btnLogs.getStyleClass().add("active");

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Cargar vista de Mantenimiento
    private void cargarVistaMantenimiento() {
        try {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) btnDashboard.getScene().getRoot();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/mantenimiento.fxml"));
            root.setCenter(loader.load());

            // Pasar usuario
            MantenimientoController controller = loader.getController();
            if (this.currentUser != null) {
                controller.setUsuarioActual(this.currentUser);
            }

            // Actualizar botones activos
            btnDashboard.getStyleClass().remove("active");
            btnIncidencias.getStyleClass().remove("active");
            btnUsuarios.getStyleClass().remove("active");
            if (btnMonitorizacion != null)
                btnMonitorizacion.getStyleClass().remove("active");
            if (btnMantenimiento != null)
                btnMantenimiento.getStyleClass().add("active");

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Cargar la pantalla de administración de usuarios en el centro
    private void cargarVistaUsuarios() {
        try {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) btnUsuarios.getScene().getRoot();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/admin_users.fxml"));
            root.setCenter(loader.load());
            
            // Pasar usuario
            AdminUsersController controller = loader.getController();
            if (this.currentUser != null) {
                controller.setUsuarioActual(this.currentUser);
            }

            // Actualizar botones activos
            btnDashboard.getStyleClass().remove("active");
            btnIncidencias.getStyleClass().remove("active");
            btnUsuarios.getStyleClass().add("active");
            if (btnMonitorizacion != null)
                btnMonitorizacion.getStyleClass().remove("active");
            if (btnMantenimiento != null)
                btnMantenimiento.getStyleClass().remove("active");
            if (btnLogs != null)
                btnLogs.getStyleClass().remove("active");

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Cargar vista de Monitorización
    private void cargarVistaMonitorizacion() {
        try {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) btnMonitorizacion.getScene()
                    .getRoot();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/monitorizacion.fxml"));
            root.setCenter(loader.load());

            // Pasar usuario
            MonitorizacionController controller = loader.getController();
            if (this.currentUser != null) {
                controller.setUsuarioActual(this.currentUser);
            }

            // Actualizar botones activos
            btnDashboard.getStyleClass().remove("active");
            btnIncidencias.getStyleClass().remove("active");
            btnUsuarios.getStyleClass().remove("active");
            if (btnMonitorizacion != null)
                btnMonitorizacion.getStyleClass().add("active");
            if (btnMantenimiento != null)
                btnMantenimiento.getStyleClass().remove("active");

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Cargar vista de Incidencias
    private void cargarVistaIncidencias() {
        try {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) btnIncidencias.getScene().getRoot();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/incidencias.fxml"));
            root.setCenter(loader.load());

            // Pasar el usuario actual al controlador de incidencias
            IncidenciasController controller = loader.getController();
            if (this.currentUser != null) {
                controller.setUsuarioActual(this.currentUser);
            }

            // Actualizar botones activos
            btnDashboard.getStyleClass().remove("active");
            btnIncidencias.getStyleClass().add("active");
            btnUsuarios.getStyleClass().remove("active");
            if (btnMonitorizacion != null)
                btnMonitorizacion.getStyleClass().remove("active");

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Cerrar sesión y volver al Login
    private void cerrarSesion() {
        if (currentUser != null) {
            LogService.logLogout(currentUser);
        }
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
