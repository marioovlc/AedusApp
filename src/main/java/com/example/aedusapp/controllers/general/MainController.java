package com.example.aedusapp.controllers.general;

import com.example.aedusapp.database.daos.IncidenciaDAO;
import com.example.aedusapp.database.daos.UsuarioDAO;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.logging.LogService;
import com.example.aedusapp.utils.config.SessionManager;
import com.example.aedusapp.utils.config.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

// Controlador Principal del Dashboard (Panel de Control)
public class MainController {

    @FXML
    private Label lblUsuario;
    @FXML
    private Label lblAvatarInicial; // Inicial del nombre en el avatar circular
    @FXML
    private Label lblUsuarioRol; // Rol del usuario (Admin / Usuario)

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnIncidencias;
    @FXML
    private Button btnConnectHub;
    @FXML
    private Button btnUsuarios;
    @FXML
    private Button btnLogs;
    @FXML
    private Button btnMonitorizacion;
    @FXML
    private Button btnConfiguracion;
    @FXML
    private Button btnCerrarSesion;
    @FXML
    private Button btnTienda;


    // Badges de contadores en sidebar
    @FXML
    private Label badgeIncidencias;
    @FXML
    private Label badgeSolicitudes;
    @FXML
    private Label lblAeduCoins;

    @FXML
    public javafx.scene.layout.BorderPane mainBorderPane;
    @FXML
    private javafx.scene.layout.VBox dashboardView;
    @FXML
    private ImageView logoView;
    @FXML
    private ImageView imgSidebarAvatar;

    public static MainController instance;

    private java.util.Map<String, javafx.scene.Node> viewCache = new java.util.HashMap<>();
    // Cache de controladores paralelo al de vistas
    private java.util.Map<String, Object> controllerCache = new java.util.HashMap<>();
    private Usuario currentUser;

    public static Runnable onThemeChanged;
    private static javafx.scene.layout.StackPane loadingOverlay;

    // Vistas que deben recargar datos frescos cada vez que se navega a ellas
    private static final java.util.Set<String> ALWAYS_REFRESH = java.util.Set.of(
            "incidencias", "monitorizacion", "dashboard", "usuarios", "connect_hub");

    private final IncidenciaDAO incidenciaDAO = new IncidenciaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        // Apply theme-aware logo
        ThemeManager.applyLogo(logoView);
        instance = this;
        // When the theme changes later, refresh the logo too
        onThemeChanged = () -> ThemeManager.applyLogo(logoView);
 
        // Configurar listener para pantalla completa (F11)
        javafx.application.Platform.runLater(this::setupFullScreenListener);

        // Inicializar el usuario desde la sesión global
        Usuario sessionUser = com.example.aedusapp.utils.config.SessionManager.getInstance().getUsuarioActual();
        if(sessionUser != null) {
            setUsuario(sessionUser);
        }

        cargarVista("/com/example/aedusapp/views/general/dashboard.fxml", "dashboard", btnDashboard);

        btnDashboard.setOnAction(
                e -> cargarVista("/com/example/aedusapp/views/general/dashboard.fxml", "dashboard", btnDashboard));
        btnIncidencias.setOnAction(e -> cargarVista("/com/example/aedusapp/views/incidencias/incidencias.fxml",
                "incidencias", btnIncidencias));
        if(btnConnectHub != null)
            btnConnectHub.setOnAction(e -> cargarVista("/com/example/aedusapp/views/general/connect_hub.fxml", "connect_hub", btnConnectHub));
        btnUsuarios.setOnAction(
                e -> cargarVista("/com/example/aedusapp/views/usuarios/admin_users.fxml", "usuarios", btnUsuarios));
        if (btnLogs != null)
            btnLogs.setOnAction(e -> cargarVista("/com/example/aedusapp/views/logs/logs.fxml", "logs", btnLogs));
        btnMonitorizacion.setOnAction(e -> cargarVista("/com/example/aedusapp/views/incidencias/monitorizacion.fxml",
                "monitorizacion", btnMonitorizacion));
        btnConfiguracion.setOnAction(e -> cargarVista("/com/example/aedusapp/views/general/configuracion.fxml",
                "configuracion", btnConfiguracion));

        btnCerrarSesion.setOnAction(e -> cerrarSesion());
        if (btnTienda != null)
            btnTienda.setOnAction(e -> {
                if (currentUser != null && currentUser.hasRole("admin")) {
                    cargarVista("/com/example/aedusapp/views/usuarios/tienda_admin.fxml", "tienda", btnTienda);
                } else {
                    cargarVista("/com/example/aedusapp/views/usuarios/tienda_usuario.fxml", "tienda", btnTienda);
                }
            });


        // Tooltips descriptivos en el sidebar
        configurarTooltips();
    }

    /** Añade tooltips a todos los botones del sidebar para claridad */
    private void configurarTooltips() {
        setTooltip(btnDashboard, "Ver resumen general y estadísticas");
        setTooltip(btnIncidencias, "Crear y consultar tus incidencias");
        setTooltip(btnConnectHub, "Connect Hub: Chat de tickets y Aedus AI");
        setTooltip(btnUsuarios, "Gestionar usuarios y aprobar solicitudes");
        setTooltip(btnMonitorizacion, "Monitorizar todos los tickets en tiempo real");
        setTooltip(btnLogs, "Historial completo de acciones del sistema");
        setTooltip(btnConfiguracion, "Cambiar tu nombre y contraseña");
        setTooltip(btnCerrarSesion, "Salir de la sesión actual");
    }

    private void setTooltip(Button btn, String text) {
        if (btn == null)
            return;
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(400));
        tip.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 10;");
        Tooltip.install(btn, tip);
    }

    public void setUsuario(Usuario usuario) {
        this.currentUser = usuario;
        if (usuario == null)
            return;

        boolean isAdmin = usuario.hasRole("admin");
        boolean isMantenimiento = usuario.hasRole("mantenimiento");

        // Nombre y avatar
        if (lblUsuario != null)
            lblUsuario.setText(usuario.getNombre());
        if (lblAvatarInicial != null) {
            String nombre = usuario.getNombre();
            lblAvatarInicial.setText(nombre != null && !nombre.isEmpty()
                    ? String.valueOf(nombre.charAt(0)).toUpperCase()
                    : "?");
        }
        if (lblUsuarioRol != null) {
            lblUsuarioRol.setText(isAdmin ? "Administrador" : isMantenimiento ? "Mantenimiento" : "Usuario");
        }
        
        if (lblAeduCoins != null) {
            lblAeduCoins.textProperty().bind(currentUser.aeducoinsProperty().asString());
        }
        
        if (lblAeduCoins != null) {
            lblAeduCoins.setGraphic(createCoinIcon(14, "#fcd34d"));
        }

        configurarBotonVisible(btnUsuarios, isAdmin);
        configurarBotonVisible(btnMonitorizacion, isAdmin);
        configurarBotonVisible(btnLogs, isAdmin);

        cargarBadges(isAdmin);
        actualizarAvatarSidebar(usuario.getFotoPerfil(), usuario.getNombre(), usuario.getFotoPerfilDatos());
    }

    /** Refresh the sidebar avatar from outside (e.g. after saving in Configuracion) */
    public static void refreshSidebarAvatar() {
        if (instance == null) return;
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        if (u != null) instance.actualizarAvatarSidebar(u.getFotoPerfil(), u.getNombre(), u.getFotoPerfilDatos());
    }

    private void actualizarAvatarSidebar(String fotoRuta, String nombre, byte[] fotoDatos) {
        if (imgSidebarAvatar == null) return;
        imgSidebarAvatar.setImage(null);
        
        if (fotoDatos != null && fotoDatos.length > 0) {
            try {
                Image img = new Image(new java.io.ByteArrayInputStream(fotoDatos), 36, 36, true, true);
                imgSidebarAvatar.setImage(img);
                imgSidebarAvatar.setClip(new Circle(18, 18, 18));
                if (lblAvatarInicial != null) lblAvatarInicial.setVisible(false);
            } catch (Exception e) {
                imgSidebarAvatar.setImage(null);
                if (lblAvatarInicial != null) lblAvatarInicial.setVisible(true);
            }
        } else if (fotoRuta != null && !fotoRuta.isEmpty()) {
            try {
                String fotoUrl = fotoRuta.startsWith("file:") ? fotoRuta : "file:" + fotoRuta;
                Image img = new Image(fotoUrl, 36, 36, true, true);
                imgSidebarAvatar.setImage(img);
                imgSidebarAvatar.setClip(new Circle(18, 18, 18));
                if (lblAvatarInicial != null) lblAvatarInicial.setVisible(false);
            } catch (Exception e) {
                imgSidebarAvatar.setImage(null);
                if (lblAvatarInicial != null) lblAvatarInicial.setVisible(true);
            }
        } else {
            if (lblAvatarInicial != null) {
                String initial = (nombre != null && !nombre.isEmpty()) ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?";
                lblAvatarInicial.setText(initial);
                lblAvatarInicial.setVisible(true);
            }
        }
    }

    private javafx.scene.layout.Region createCoinIcon(int size, String colorHex) {
        javafx.scene.layout.Region icon = new javafx.scene.layout.Region();
        icon.setStyle(
            "-fx-shape: 'M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm178 555h-46.9c-10.2 0-19.9-4.9-25.9-13.2L512 460.4 406.8 605.8c-6 8.3-15.6 13.2-25.9 13.2H334c-6.5 0-10.3-7.4-6.5-12.7l178-246c3.2-4.4 9.7-4.4 12.9 0l178 246c3.9 5.3.1 12.7-6.4 12.7z';" +
            "-fx-background-color: " + colorHex + ";" +
            "-fx-min-width: " + size + "px; -fx-min-height: " + size + "px;" +
            "-fx-max-width: " + size + "px; -fx-max-height: " + size + "px;"
        );
        return icon;
    }

    /**
     * Carga los contadores del sidebar (incidencias NO LEIDAS, solicitudes
     * pendientes)
     */
    private void cargarBadges(boolean isAdmin) {
        javafx.concurrent.Task<int[]> task = new javafx.concurrent.Task<>() {
            @Override
            protected int[] call() {
                int incidenciasPendientes = 0;
                int solicitudesPendientes = 0;
                
                // Solo mostrar badge de incidencias a soporte
                boolean esSoporte = currentUser != null && (currentUser.hasRole("admin") || currentUser.hasRole("mantenimiento"));
                if (esSoporte) {
                    try {
                        var stats = incidenciaDAO.getStatusStatistics();
                        incidenciasPendientes = stats.getOrDefault("NO LEIDO", 0);
                    } catch (Exception ignored) {}
                }

                if (isAdmin) {
                    try {
                        solicitudesPendientes = usuarioDAO.getUsersByStatus("PENDING").size();
                    } catch (Exception ignored) {
                    }
                }
                return new int[] { incidenciasPendientes, solicitudesPendientes };
            }
        };

        task.setOnSucceeded(e -> {
            int[] counts = task.getValue();
            actualizarBadge(badgeIncidencias, counts[0]);
            if (isAdmin)
                actualizarBadge(badgeSolicitudes, counts[1]);
        });

        new Thread(task).start();
    }

    private void actualizarBadge(Label badge, int count) {
        if (badge == null)
            return;
        if (count > 0) {
            badge.setText(String.valueOf(count > 99 ? "99+" : count));
            badge.setVisible(true);
            badge.setManaged(true);
        } else {
            badge.setVisible(false);
            badge.setManaged(false);
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
                // Primera carga: crear la vista y guardar controlador
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
                javafx.scene.Node view = loader.load();
                viewCache.put(key, view);
                Object controller = loader.getController();
                if (controller != null)
                    controllerCache.put(key, controller);
                inyectarUsuario(controller);
            } else if (ALWAYS_REFRESH.contains(key)) {
                // Vista ya cacheada pero que necesita datos frescos: llamar refresh
                Object controller = controllerCache.get(key);
                inyectarUsuario(controller);
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
            java.lang.reflect.Method m = controller.getClass().getMethod("setUsuarioActual", Usuario.class);
            m.invoke(controller, currentUser);
        } catch (Exception ignored) {
        }
    }

    private void cambiarVista(String key, Button activeBtn) {
        javafx.scene.Node view = viewCache.get(key);
        if (view != null && mainBorderPane != null) {
            javafx.scene.Node current = mainBorderPane.getCenter();
            if (current != null && current != view) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(80), current);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    mainBorderPane.setCenter(view);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(180), view);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                mainBorderPane.setCenter(view);
            }
            actualizarEstilosBotones(activeBtn);
        }
    }

    private void actualizarEstilosBotones(Button activeBtn) {
        Button[] buttons = { btnDashboard, btnIncidencias, btnConnectHub, btnUsuarios, btnLogs, btnMonitorizacion };
        for (Button b : buttons) {
            if (b != null)
                b.getStyleClass().remove("active");
        }
        if (activeBtn != null)
            activeBtn.getStyleClass().add("active");
    }

    public void navigateToMonitorizacion(String filter) {
        cargarVista("/com/example/aedusapp/views/incidencias/monitorizacion.fxml", "monitorizacion", btnMonitorizacion);
        Object controller = controllerCache.get("monitorizacion");
        if (controller instanceof com.example.aedusapp.controllers.incidencias.MonitorizacionController mc) {
            mc.setInitialFilter(filter);
        }
    }

    private void cerrarSesion() {
        if (currentUser != null)
            LogService.logLogout(currentUser);
        com.example.aedusapp.utils.config.SessionManager.getInstance().cleanSession();
        try {
            viewCache.clear();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/aedusapp/views/auth/login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 800, 600);
            ThemeManager.applyTheme(scene);
            javafx.stage.Stage stage = (javafx.stage.Stage) btnCerrarSesion.getScene().getWindow();
            stage.setTitle("Aedus");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static void showGlobalLoading(boolean show, String message) {
        javafx.application.Platform.runLater(() -> {
            if (instance == null || instance.mainBorderPane == null || instance.mainBorderPane.getScene() == null) return;
            javafx.scene.Scene scene = instance.mainBorderPane.getScene();
            
            if (loadingOverlay == null) {
                loadingOverlay = new javafx.scene.layout.StackPane();
                loadingOverlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
                
                javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
                spinner.setStyle("-fx-accent: #3b82f6;");
                spinner.setMaxSize(50, 50);
                
                javafx.scene.control.Label lblMsg = new javafx.scene.control.Label(message != null ? message : "Cargando...");
                lblMsg.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");
                
                javafx.scene.layout.VBox centerBox = new javafx.scene.layout.VBox(15, spinner, lblMsg);
                centerBox.setAlignment(javafx.geometry.Pos.CENTER);
                loadingOverlay.getChildren().add(centerBox);
                
                javafx.scene.Parent originalRoot = scene.getRoot();
                if (!(originalRoot instanceof javafx.scene.layout.StackPane && ((javafx.scene.layout.StackPane)originalRoot).getChildren().contains(loadingOverlay))) {
                    javafx.scene.layout.StackPane newRoot = new javafx.scene.layout.StackPane(originalRoot, loadingOverlay);
                    scene.setRoot(newRoot);
                }
            } else {
                if (loadingOverlay.getChildren().size() > 0) {
                    javafx.scene.layout.VBox centerBox = (javafx.scene.layout.VBox) loadingOverlay.getChildren().get(0);
                    if (centerBox.getChildren().size() > 1) {
                        ((javafx.scene.control.Label)centerBox.getChildren().get(1)).setText(message != null ? message : "Cargando...");
                    }
                }
                
                javafx.scene.Parent originalRoot = scene.getRoot();
                if (originalRoot instanceof javafx.scene.layout.StackPane && !((javafx.scene.layout.StackPane)originalRoot).getChildren().contains(loadingOverlay)) {
                    ((javafx.scene.layout.StackPane)originalRoot).getChildren().add(loadingOverlay);
                }
            }
            
            loadingOverlay.setVisible(show);
            loadingOverlay.setManaged(show);
        });
    }

    private void setupFullScreenListener() {
        if (mainBorderPane != null && mainBorderPane.getScene() != null) {
            mainBorderPane.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.F11) {
                    javafx.stage.Stage stage = (javafx.stage.Stage) mainBorderPane.getScene().getWindow();
                    stage.setFullScreen(!stage.isFullScreen());
                }
            });
        }
    }

    public void refreshCurrentView(String viewId) {
        viewCache.remove(viewId);
        controllerCache.remove(viewId);
        
        String fxmlPath;
        Button targetBtn;
        
        switch(viewId) {
            case "dashboard":
                fxmlPath = "/com/example/aedusapp/views/general/dashboard.fxml";
                targetBtn = btnDashboard;
                break;
            case "incidencias":
                fxmlPath = "/com/example/aedusapp/views/incidencias/incidencias.fxml";
                targetBtn = btnIncidencias;
                break;
            case "usuarios":
                fxmlPath = "/com/example/aedusapp/views/usuarios/admin_users.fxml";
                targetBtn = btnUsuarios;
                break;
            case "monitorizacion":
                fxmlPath = "/com/example/aedusapp/views/incidencias/monitorizacion.fxml";
                targetBtn = btnMonitorizacion;
                break;
            case "tienda":
                fxmlPath = "/com/example/aedusapp/views/tienda/tienda.fxml";
                targetBtn = btnTienda;
                break;
            case "connect_hub":
                fxmlPath = "/com/example/aedusapp/views/general/connect_hub.fxml";
                targetBtn = btnConnectHub;
                break;
            case "configuracion":
                fxmlPath = "/com/example/aedusapp/views/general/configuracion.fxml";
                targetBtn = btnConfiguracion;
                break;
            default:
                fxmlPath = "/com/example/aedusapp/views/general/dashboard.fxml";
                targetBtn = btnDashboard;
        }
        cargarVista(fxmlPath, viewId, targetBtn);
    }
}
