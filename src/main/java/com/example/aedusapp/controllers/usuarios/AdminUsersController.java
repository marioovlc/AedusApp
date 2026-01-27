package com.example.aedusapp.controllers.usuarios;

import com.example.aedusapp.database.UsuarioDAO;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.LogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

// Controlador para la pantalla de Administración de Usuarios
public class AdminUsersController {

    // Tablas y columnas que se ven en la pantalla
    @FXML
    private TableView<Usuario> tableUsuarios; // Tabla de usuarios activos
    @FXML
    private TableColumn<Usuario, Integer> colId;
    @FXML
    private TableColumn<Usuario, String> colNombre;
    @FXML
    private TableColumn<Usuario, String> colEmail;
    @FXML
    private TableColumn<Usuario, Integer> colRol;
    @FXML
    private TableColumn<Usuario, String> colStatus;

    @FXML
    private TableView<Usuario> tableSolicitudes; // Tabla de solicitudes pendientes
    @FXML
    private TableColumn<Usuario, Integer> colReqId;
    @FXML
    private TableColumn<Usuario, String> colReqNombre;
    @FXML
    private TableColumn<Usuario, String> colReqEmail;
    @FXML
    private TableColumn<Usuario, String> colReqStatus;

    // Herramientas para manejar datos
    private UsuarioDAO usuarioDAO = new UsuarioDAO();
    // Listas que conectan los datos con las tablas
    private ObservableList<Usuario> activeUsersList = FXCollections.observableArrayList();
    private ObservableList<Usuario> pendingUsersList = FXCollections.observableArrayList();

    private Usuario usuarioActual;

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    // Método que se ejecuta automáticamente al abrir esta pantalla (como un
    // constructor)
    @FXML
    public void initialize() {
        // Configurar qué dato va en qué columna para Usuarios Activos
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("roleId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Configurar columnas para Solicitudes Pendientes
        colReqId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colReqNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colReqEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colReqStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Cargar los datos desde la base de datos
        loadData();
    }

    // Método para recargar los datos de las tablas
    private void loadData() {
        tableUsuarios.setPlaceholder(new Label("Cargando usuarios activos..."));
        tableSolicitudes.setPlaceholder(new Label("Cargando solicitudes..."));

        javafx.concurrent.Task<java.util.List<Usuario>> activeTask = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<Usuario> call() throws Exception {
                return usuarioDAO.obtenerUsuariosPorEstado("ACTIVE");
            }
        };

        javafx.concurrent.Task<java.util.List<Usuario>> pendingTask = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<Usuario> call() throws Exception {
                return usuarioDAO.obtenerUsuariosPorEstado("PENDING");
            }
        };

        activeTask.setOnSucceeded(e -> {
            activeUsersList.setAll(activeTask.getValue());
            tableUsuarios.setItems(activeUsersList);
            if (activeTask.getValue().isEmpty()) {
                tableUsuarios.setPlaceholder(new Label("No hay usuarios activos."));
            }
        });

        pendingTask.setOnSucceeded(e -> {
            pendingUsersList.setAll(pendingTask.getValue());
            tableSolicitudes.setItems(pendingUsersList);
            if (pendingTask.getValue().isEmpty()) {
                tableSolicitudes.setPlaceholder(new Label("No hay solicitudes pendientes."));
            }
        });

        new Thread(activeTask).start();
        new Thread(pendingTask).start();
    }

    // Acción para el botón "Editar Usuario"
    @FXML
    private void handleEditUser() {
        // Ver qué usuario está seleccionado
        Usuario selectedUser = tableUsuarios.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selección requerida", "Selecciona un usuario para editar.");
            return;
        }

        try {
            // Abrir la ventana emergente de edición (edit_user_dialog.fxml)
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/com/example/aedusapp/views/usuarios/edit_user_dialog.fxml"));
            javafx.scene.layout.VBox page = (javafx.scene.layout.VBox) loader.load();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Editar Usuario");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL); // Bloquea la ventana de atrás
            dialogStage.initOwner(tableUsuarios.getScene().getWindow());
            javafx.scene.Scene scene = new javafx.scene.Scene(page);
            String css = getClass().getResource("/com/example/aedusapp/styles/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
            dialogStage.setScene(scene);

            // Pasarle los datos del usuario al controlador de edición
            EditUserController controller = loader.getController();
            controller.setUsuario(selectedUser);

            // Mostrar el diálogo y esperar a que se cierre
            dialogStage.showAndWait();

            // Si se guardaron cambios, actualizar la base de datos
            if (controller.isSaveClicked()) {
                Usuario updatedUser = controller.getUsuario();
                if (usuarioDAO.actualizarUsuario(updatedUser)) {
                    if (usuarioActual != null) {
                        LogService.logEditarUsuario(usuarioActual, updatedUser.getNombre());
                    }
                    loadData(); // Refrescar la tabla
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Usuario actualizado correctamente.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo actualizar el usuario.");
                }
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Acción para el botón "Eliminar Usuario"
    @FXML
    private void handleDeleteUser() {
        Usuario selectedUser = tableUsuarios.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selección requerida", "Selecciona un usuario para eliminar.");
            return;
        }

        // Preguntar confirmación antes de borrar
        Alert alert = com.example.aedusapp.utils.AlertUtils.createConfirmationAlert(
                "Confirmar eliminación",
                "Eliminar usuario: " + selectedUser.getNombre(),
                "¿Estás seguro? Esta acción no se puede deshacer.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Si dice que SÍ, borrar de la base de datos
            if (usuarioDAO.eliminarUsuario(selectedUser.getId())) {
                if (usuarioActual != null) {
                    LogService.logEliminarUsuario(usuarioActual, selectedUser.getNombre());
                }
                loadData(); // Refrescar
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Error al eliminar usuario.");
            }
        }
    }

    // Acción para aprobar una solicitud de registro
    @FXML
    private void handleApproveRequest() {
        Usuario selectedUser = tableSolicitudes.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selección requerida", "Selecciona una solicitud para aprobar.");
            return;
        }

        // Validar roles
        java.util.List<Integer> roles = selectedUser.getRoles();
        if (roles == null || roles.isEmpty() || (roles.size() == 1 && roles.get(0) == 0)) {
            // Asignar rol de Profesor (2) por defecto si no tiene rol válido
            roles = new java.util.ArrayList<>();
            roles.add(2);
        }

        // Cambiar estado a "ACTIVE"
        Usuario approvedUser = new Usuario(selectedUser.getId(), selectedUser.getNombre(), selectedUser.getEmail(),
                selectedUser.getPassword(), "ACTIVE", roles);

        if (usuarioDAO.actualizarUsuario(approvedUser)) {
            if (usuarioActual != null) {
                LogService.logCrearUsuario(usuarioActual, approvedUser.getNombre());
            }
            showAlert(Alert.AlertType.INFORMATION, "Aprobado", "El usuario ha sido activado con rol de Profesor.");
            loadData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Error al aprobar usuario.");
        }
    }

    // Acción para rechazar una solicitud
    @FXML
    private void handleDenyRequest() {
        Usuario selectedUser = tableSolicitudes.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Selección requerida", "Selecciona una solicitud para rechazar.");
            return;
        }

        // Eliminar al usuario de la base de datos
        if (usuarioDAO.eliminarUsuario(selectedUser.getId())) {
            if (usuarioActual != null) {
                LogService.logEliminarUsuario(usuarioActual, selectedUser.getNombre() + " (Solicitud Rechazada)");
            }
            showAlert(Alert.AlertType.INFORMATION, "Rechazado", "La solicitud ha sido rechazada y eliminada.");
            loadData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Error al rechazar usuario.");
        }
    }

    // Método auxiliar para mostrar alertas
    private void showAlert(Alert.AlertType type, String title, String content) {
        com.example.aedusapp.utils.AlertUtils.showAlert(type, title, content);
    }
}
