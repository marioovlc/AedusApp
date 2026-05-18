package com.example.aedusapp.controllers.usuarios;

import com.example.aedusapp.database.daos.UsuarioDAO;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.logging.LogService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import java.io.ByteArrayInputStream;

/**
 * AdminUsersController – cards dinámicas estilo timeline para Usuarios y
 * Solicitudes.
 */
public class AdminUsersController {

    // ── FXML ──────────────────────────────────────────────────────────
    @FXML
    private VBox listaUsuarios;

    // filtros tab usuarios
    @FXML
    private Button chipRolTodos;
    @FXML
    private Button chipRolAdmin;
    @FXML
    private Button chipRolMant;
    @FXML
    private Button chipRolUser;
    @FXML
    private TextField txtBuscarUsuario;

    // ── Estado ────────────────────────────────────────────────────────
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private Usuario usuarioActual;
    private Usuario usuarioSeleccionado;

    private List<Usuario> todosUsuarios = new ArrayList<>();
    private String filtroRol = "Todos";

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    @FXML
    public void initialize() {
        loadData();
    }

    // ── Chips de rol ──────────────────────────────────────────────────
    @FXML
    private void onChipRolTodos() {
        activarChipRol("Todos");
        buscarUsuario();
    }

    @FXML
    private void onChipRolAdmin() {
        activarChipRol("admin");
        buscarUsuario();
    }

    @FXML
    private void onChipRolMant() {
        activarChipRol("mantenimiento");
        buscarUsuario();
    }

    @FXML
    private void onChipRolUser() {
        activarChipRol("user");
        buscarUsuario();
    }

    private void activarChipRol(String rol) {
        filtroRol = rol;
        Button[] chips = { chipRolTodos, chipRolAdmin, chipRolMant, chipRolUser };
        String[] roles = { "Todos", "admin", "mantenimiento", "user" };
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null)
                continue;
            chips[i].getStyleClass().remove("chip-active");
            if (roles[i].equalsIgnoreCase(rol))
                chips[i].getStyleClass().add("chip-active");
        }
    }

    @FXML
    public void buscarUsuario() {
        String q = txtBuscarUsuario != null ? txtBuscarUsuario.getText().toLowerCase() : "";

        List<Usuario> filtrados = todosUsuarios.stream()
                .filter(u -> "Todos".equalsIgnoreCase(filtroRol) || filtroRol.equalsIgnoreCase(u.getRole()))
                .filter(u -> q.isBlank()
                        || (u.getNombre() != null && u.getNombre().toLowerCase().contains(q))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        construirListaUsuarios(filtrados);
    }

    // ── Carga ─────────────────────────────────────────────────────────
    private void loadData() {
        todosUsuarios.clear();
        
        // Usuarios activos
        javafx.concurrent.Task<List<Usuario>> activeTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Usuario> call() {
                return usuarioDAO.getUsersByStatus("ACTIVE");
            }
        };
        activeTask.setOnSucceeded(e -> {
            todosUsuarios.addAll(activeTask.getValue());
            
            // Inactivos (Pendientes + Baneados)
            javafx.concurrent.Task<List<Usuario>> inactiveTask = new javafx.concurrent.Task<>() {
                @Override
                protected List<Usuario> call() {
                    return usuarioDAO.getUsersByStatus("PENDING"); // Returns GET_USERS_INACTIVE
                }
            };
            inactiveTask.setOnSucceeded(ev -> {
                todosUsuarios.addAll(inactiveTask.getValue());
                buscarUsuario();
            });
            inactiveTask.setOnFailed(ev -> mostrarError(listaUsuarios, "Error al cargar usuarios inactivos."));
            new Thread(inactiveTask).start();
        });
        activeTask.setOnFailed(e -> mostrarError(listaUsuarios, "Error al cargar usuarios activos."));
        new Thread(activeTask).start();
    }

    // ── Cards de Usuarios Activos ─────────────────────────────────────
    private void construirListaUsuarios(List<Usuario> lista) {
        listaUsuarios.getChildren().clear();
        if (lista.isEmpty()) {
            mostrarVacio(listaUsuarios, "👤", "No hay usuarios que coincidan");
            return;
        }
        for (Usuario u : lista)
            listaUsuarios.getChildren().add(crearFilaUsuario(u));
    }

    private HBox crearFilaUsuario(Usuario u) {
        String rolColor = getRolColor(u.getRole());
        
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));
        
        boolean isPending = "PENDING".equalsIgnoreCase(u.getStatus());
        boolean isBanned = "INACTIVE".equalsIgnoreCase(u.getStatus());
        
        row.getStyleClass().addAll("user-list-row", isPending ? "row-role-pending" : "row-role-" + getRolClass(u.getRole()));

        // Avatar circular con foto real o inicial de fallback
        StackPane avatarStack = buildAvatar(u, isPending ? "#fbbf24" : rolColor);

        // Texto: nombre + email
        VBox textBlock = new VBox(2);
        HBox.setHgrow(textBlock, Priority.ALWAYS);
        Label lblNombre = new Label(u.getNombre() != null ? u.getNombre() : "(sin nombre)");
        lblNombre.getStyleClass().add("user-list-name");
        Label lblEmail = new Label(u.getEmail() != null ? u.getEmail() : "");
        lblEmail.getStyleClass().add("user-list-email");
        textBlock.getChildren().addAll(lblNombre, lblEmail);

        // Badge de rol
        Label rolBadge = new Label(getRolEmoji(u.getRole()) + " " + getRolLabel(u.getRole()));
        rolBadge.setStyle(
                "-fx-background-color: " + hexToRgba(rolColor, 0.15) + ";" +
                        "-fx-text-fill: " + rolColor + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-padding: 3 10; -fx-background-radius: 10;");

        // Badge de Estado
        Label statusBadge = new Label(isPending ? "⏳ Pendiente" : (isBanned ? "● Baneado" : "● Activo"));
        String statusColor = isPending ? "#fbbf24" : (isBanned ? "#ef4444" : "#22c55e");
        statusBadge.setStyle(
                "-fx-text-fill: " + statusColor + ";" +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 0 10;");

        // Acciones
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        if (isPending) {
            Button btnApprove = createIconButton("✓", "success-icon-btn");
            btnApprove.setOnAction(e -> {
                usuarioSeleccionado = u;
                handleApproveRequest();
            });
            
            Button btnDeny = createIconButton("✕", "danger-icon-btn");
            btnDeny.setOnAction(e -> {
                usuarioSeleccionado = u;
                handleDenyRequest();
            });
            
            actions.getChildren().addAll(btnApprove, btnDeny);
        } else {
            Button btnEdit = createIconButton("✏", "primary-icon-btn");
            btnEdit.setOnAction(e -> {
                usuarioSeleccionado = u;
                handleEditUser();
            });
            
            Button btnBan = createIconButton(isBanned ? "🛡" : "🚫", isBanned ? "success-icon-btn" : "warning-icon-btn");
            btnBan.setOnAction(e -> handleToggleBan(u));
            
            Button btnDelete = createIconButton("🗑", "danger-icon-btn");
            btnDelete.setOnAction(e -> {
                usuarioSeleccionado = u;
                handleDeleteUser();
            });

            actions.getChildren().addAll(btnEdit, btnBan, btnDelete);
        }

        row.getChildren().addAll(avatarStack, textBlock, rolBadge, statusBadge, actions);

        // Selección visual
        row.setOnMouseClicked(e -> {
            listaUsuarios.getChildren().forEach(n -> {
                if (n instanceof HBox h)
                    h.getStyleClass().remove("selected");
            });
            row.getStyleClass().add("selected");
            usuarioSeleccionado = u;
        });

        return row;
    }

    /** Builds a 36×36 circular avatar: photo (bytes or URL) → initial fallback */
    private StackPane buildAvatar(Usuario u, String rolColor) {
        String inicial = (u.getNombre() != null && !u.getNombre().isEmpty())
                ? String.valueOf(u.getNombre().charAt(0)).toUpperCase() : "?";

        // Fallback: initial letter
        Label lblInicial = new Label(inicial);
        lblInicial.setAlignment(Pos.CENTER);
        lblInicial.setMinSize(36, 36);
        lblInicial.setMaxSize(36, 36);
        lblInicial.setStyle(
                "-fx-background-color: " + hexToRgba(rolColor, 0.2) + ";" +
                        "-fx-text-fill: " + rolColor + ";" +
                        "-fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 18;");

        StackPane stack = new StackPane(lblInicial);
        stack.setMinSize(36, 36);
        stack.setMaxSize(36, 36);

        // Try to load the real photo asynchronously
        boolean hasBytes = u.getFotoPerfilDatos() != null;
        boolean hasUrl   = u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty();
        if (hasBytes || hasUrl) {
            ImageView imgView = new ImageView();
            imgView.setFitWidth(36);
            imgView.setFitHeight(36);
            imgView.setPreserveRatio(true);
            Circle clip = new Circle(18, 18, 18);
            imgView.setClip(clip);
            stack.getChildren().add(imgView);

            javafx.concurrent.Task<Image> loadTask = new javafx.concurrent.Task<>() {
                @Override protected Image call() {
                    try {
                        if (hasBytes)
                            return new Image(new ByteArrayInputStream(u.getFotoPerfilDatos()));
                        return new Image(u.getAvatarUrl(), 36, 36, true, true);
                    } catch (Exception ignored) { return null; }
                }
            };
            loadTask.setOnSucceeded(e -> {
                Image img = loadTask.getValue();
                if (img != null && !img.isError()) {
                    imgView.setImage(img);
                    lblInicial.setVisible(false);
                    lblInicial.setManaged(false);
                }
            });
            new Thread(loadTask).start();
        }
        return stack;
    }


    private Button createIconButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("icon-button", styleClass);
        btn.setPadding(new Insets(5, 8, 5, 8));
        btn.setCursor(javafx.scene.Cursor.HAND);
        return btn;
    }

    private void handleToggleBan(Usuario u) {
        boolean wasBanned = "INACTIVE".equalsIgnoreCase(u.getStatus());
        String newStatus = wasBanned ? "ACTIVE" : "INACTIVE";
        String actionMsg = wasBanned ? "desbanear" : "banear";
        
        Alert alert = com.example.aedusapp.utils.ui.AlertUtils.createConfirmationAlert(
                "Confirmar acción", actionMsg.toUpperCase() + " usuario: " + u.getNombre(),
                "¿Estás seguro de que deseas " + actionMsg + " a este usuario?");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            u.setStatus(newStatus);
            if (usuarioDAO.updateUser(u)) {
                if (usuarioActual != null) {
                    if (wasBanned) LogService.logEditarUsuario(usuarioActual, u.getNombre() + " (Desbaneado)");
                    else LogService.logEditarUsuario(usuarioActual, u.getNombre() + " (Baneado)");
                }
                loadData();
                showAlert(Alert.AlertType.INFORMATION, "Éxito", "Usuario " + (wasBanned ? "desbaneado" : "baneado") + " correctamente.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo realizar la acción.");
            }
        }
    }

    // ── Acciones ──────────────────────────────────────────────────────
    @FXML
    private void handleEditUser() {
        if (usuarioSeleccionado == null) {
            showAlert(Alert.AlertType.WARNING, "Selección requerida", "Selecciona un usuario para editar.");
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/com/example/aedusapp/views/usuarios/edit_user_dialog.fxml"));
            javafx.scene.layout.VBox page = loader.load();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Editar Usuario");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(listaUsuarios.getScene().getWindow());
            javafx.scene.Scene scene = new javafx.scene.Scene(page);
            String css = getClass().getResource("/com/example/aedusapp/styles/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
            dialogStage.setScene(scene);

            EditUserController controller = loader.getController();
            controller.setUsuario(usuarioSeleccionado);
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                Usuario updated = controller.getUsuario();
                if (usuarioDAO.updateUser(updated)) {
                    if (usuarioActual != null)
                        LogService.logEditarUsuario(usuarioActual, updated.getNombre());
                    loadData();
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Usuario actualizado correctamente.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo actualizar el usuario.");
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteUser() {
        if (usuarioSeleccionado == null) {
            showAlert(Alert.AlertType.WARNING, "Selección requerida", "Selecciona un usuario para eliminar.");
            return;
        }
        Alert alert = com.example.aedusapp.utils.ui.AlertUtils.createConfirmationAlert(
                "Confirmar eliminación", "Eliminar usuario: " + usuarioSeleccionado.getNombre(),
                "¿Estás seguro? Esta acción no se puede deshacer.");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (usuarioDAO.deleteUser(usuarioSeleccionado.getId())) {
                if (usuarioActual != null)
                    LogService.logEliminarUsuario(usuarioActual, usuarioSeleccionado.getNombre());
                usuarioSeleccionado = null;
                loadData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Error al eliminar usuario.");
            }
        }
    }

    @FXML
    private void handleApproveRequest() {
        if (usuarioSeleccionado == null) {
            showAlert(Alert.AlertType.WARNING, "Selección requerida", "Selecciona una solicitud para aprobar.");
            return;
        }
        String role = usuarioSeleccionado.getRole();
        if (role == null || role.isEmpty() || role.equals("0"))
            role = "user";

        Usuario approvedUser = new Usuario(usuarioSeleccionado.getId(), usuarioSeleccionado.getNombre(),
                usuarioSeleccionado.getEmail(), usuarioSeleccionado.getPassword(), "ACTIVE", role,
                usuarioSeleccionado.getAeducoins());
        if (usuarioDAO.updateUser(approvedUser)) {
            if (usuarioActual != null)
                LogService.logCrearUsuario(usuarioActual, approvedUser.getNombre());
            showAlert(Alert.AlertType.INFORMATION, "Aprobado", "El usuario ha sido activado.");
            usuarioSeleccionado = null;
            loadData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Error al aprobar usuario.");
        }
    }

    @FXML
    private void handleDenyRequest() {
        if (usuarioSeleccionado == null) {
            showAlert(Alert.AlertType.WARNING, "Selección requerida", "Selecciona una solicitud para rechazar.");
            return;
        }
        if (usuarioDAO.deleteUser(usuarioSeleccionado.getId())) {
            if (usuarioActual != null)
                LogService.logEliminarUsuario(usuarioActual, usuarioSeleccionado.getNombre() + " (Rechazado)");
            showAlert(Alert.AlertType.INFORMATION, "Rechazado", "La solicitud ha sido rechazada.");
            usuarioSeleccionado = null;
            loadData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Error al rechazar usuario.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private String getRolClass(String rol) {
        if (rol == null)
            return "default";
        return switch (rol.toLowerCase()) {
            case "admin" -> "admin";
            case "mantenimiento" -> "mantenimiento";
            case "user" -> "user";
            default -> "default";
        };
    }

    private String getRolColor(String rol) {
        if (rol == null)
            return "#64748b";
        return switch (rol.toLowerCase()) {
            case "admin" -> "#f87171";
            case "mantenimiento" -> "#fbbf24";
            case "user" -> "#4f8ef7";
            default -> "#64748b";
        };
    }

    private String getRolEmoji(String rol) {
        if (rol == null)
            return "•";
        return switch (rol.toLowerCase()) {
            case "admin" -> "🔴";
            case "mantenimiento" -> "🟡";
            case "user" -> "🔵";
            default -> "•";
        };
    }

    private String getRolLabel(String rol) {
        if (rol == null)
            return "Usuario";
        return switch (rol.toLowerCase()) {
            case "admin" -> "Administrador";
            case "mantenimiento" -> "Mantenimiento";
            case "user" -> "Usuario";
            default -> rol;
        };
    }

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

    private void mostrarVacio(VBox container, String emoji, String msg) {
        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60));
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 42px; -fx-opacity: 0.3;");
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #475569;");
        empty.getChildren().addAll(icon, lbl);
        container.getChildren().add(empty);
    }

    private void mostrarError(VBox container, String msg) {
        container.getChildren().clear();
        Label err = new Label("⚠ " + msg);
        err.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-padding: 20;");
        container.getChildren().add(err);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        com.example.aedusapp.utils.ui.AlertUtils.showAlert(type, title, content);
    }
}
