package com.example.aedusapp.controllers.general;

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

    private java.util.Map<String, javafx.scene.Node> viewCache = new java.util.HashMap<>();
    private Usuario currentUser;

    // Configuración inicial al abrir la ventana
    @FXML
    public void initialize() {
        // Cachear el dashboard inicial
        viewCache.put("dashboard", dashboardView);

        // Conectar botones con sus acciones
        btnDashboard.setOnAction(event -> mostrarDashboard());
        btnIncidencias.setOnAction(
                event -> cargarVista("/com/example/aedusapp/views/incidencias/incidencias.fxml", "incidencias",
                        btnIncidencias));
        btnUsuarios.setOnAction(
                event -> cargarVista("/com/example/aedusapp/views/usuarios/admin_users.fxml", "usuarios", btnUsuarios));
        if (btnLogs != null)
            btnLogs.setOnAction(event -> cargarVista("/com/example/aedusapp/views/logs/logs.fxml", "logs", btnLogs));
        btnMonitorizacion
                .setOnAction(event -> cargarVista("/com/example/aedusapp/views/incidencias/monitorizacion.fxml",
                        "monitorizacion", btnMonitorizacion));
        btnCerrarSesion.setOnAction(event -> cerrarSesion());
        if (btnMantenimiento != null)
            btnMantenimiento.setOnAction(e -> cargarVista("/com/example/aedusapp/views/general/mantenimiento.fxml",
                    "mantenimiento", btnMantenimiento));

    }

    // Volver a la vista principal
    private void mostrarDashboard() {
        cambiarVista("dashboard", btnDashboard);
    }

    // Configurar quién es el usuario conectado
    public void setUsuario(Usuario usuario) {
        this.currentUser = usuario;
        if (lblUsuario != null && usuario != null) {
            lblUsuario.setText(usuario.getNombre());

            // Si NO es administrador (rol 1), ocultar botones de admin
            boolean isAdmin = usuario.hasRole(1);
            boolean isMantenimiento = usuario.hasRole(3);

            configurarBotonVisible(btnUsuarios, isAdmin);
            configurarBotonVisible(btnMonitorizacion, isAdmin);
            configurarBotonVisible(btnLogs, isAdmin);
            configurarBotonVisible(btnMantenimiento, isAdmin || isMantenimiento);
        }
    }

    private void configurarBotonVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void cargarVista(String fxmlPath, String key, Button sourceButton) {
        try {
            if (!viewCache.containsKey(key)) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
                javafx.scene.Node view = loader.load();
                viewCache.put(key, view);

                // Configurar el controlador recién cargado
                Object controller = loader.getController();
                inyectarUsuario(controller);
            } else {
                // Si ya está en cache, refrescar datos
                inyectarUsuario(viewCache.get(key).getProperties().get("controller"));
                // Nota: Verificaremos si el controlador tiene un método de refresco
                refrescarDatos(key);
            }

            cambiarVista(key, sourceButton);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void inyectarUsuario(Object controller) {
        if (controller == null || currentUser == null)
            return;

        try {
            java.lang.reflect.Method method = controller.getClass().getMethod("setUsuarioActual", Usuario.class);
            method.invoke(controller, currentUser);
        } catch (Exception e) {
            // El controlador podría no tener este método o ya haber sido inyectado
        }
    }

    private void refrescarDatos(String key) {
        // Implementación opcional para forzar recarga de datos al volver a una pestaña
    }

    private void cambiarVista(String key, Button activeBtn) {
        javafx.scene.Node view = viewCache.get(key);
        if (view != null) {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) btnDashboard.getScene().getRoot();
            root.setCenter(view);
            actualizarEstilosBotones(activeBtn);
        }
    }

    private void actualizarEstilosBotones(Button activeBtn) {
        Button[] buttons = { btnDashboard, btnIncidencias, btnUsuarios, btnLogs, btnMonitorizacion, btnMantenimiento };
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("active");
            }
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active");
        }
    }

    // Cerrar sesión y volver al Login
    private void cerrarSesion() {
        if (currentUser != null) {
            LogService.logLogout(currentUser);
        }
        try {
            viewCache.clear(); // Limpiar cache al cerrar sesión
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/views/auth/login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 800, 600);
            String css = getClass().getResource("/com/example/aedusapp/styles/styles.css").toExternalForm();
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
