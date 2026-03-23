package com.example.aedusapp.controllers.general;

import com.example.aedusapp.database.daos.UsuarioDAO;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.logging.LogService;
import com.example.aedusapp.utils.ui.AlertUtils;
import com.example.aedusapp.utils.config.SessionManager;
import com.example.aedusapp.utils.config.ThemeManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;

public class ConfiguracionController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblRoleDisplay;
    @FXML private Label lblMensaje;
    @FXML private ComboBox<ThemeManager.Theme> cmbTheme;
    @FXML private ImageView imgAvatarConf;
    @FXML private Label lblAvatarInitialsConf;
    @FXML private Label lblFotoNombreConf;
    @FXML private StackPane avatarContainerConf;

    private String nuevaFotoPath = null;
    private byte[] nuevaFotoDatos = null;
    private UsuarioDAO usuarioDAO = new UsuarioDAO();
    private Usuario usuarioActual;

    @FXML
    public void initialize() {
        if (cmbTheme != null) {
            cmbTheme.setItems(FXCollections.observableArrayList(ThemeManager.Theme.values()));
            cmbTheme.setValue(ThemeManager.getSavedTheme());
            cmbTheme.setOnAction(e -> {
                ThemeManager.Theme selected = cmbTheme.getValue();
                ThemeManager.saveTheme(selected);
                ThemeManager.applyTheme(cmbTheme.getScene());
                // Refresh theme-aware logo in sidebar
                if (MainController.onThemeChanged != null) {
                    MainController.onThemeChanged.run();
                }
            });
        }
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        nuevaFotoPath = usuario != null ? usuario.getFotoPerfil() : null;
        nuevaFotoDatos = usuario != null ? usuario.getFotoPerfilDatos() : null;
        if (usuario != null) {
            txtNombre.setText(usuario.getNombre());
            txtEmail.setText(usuario.getEmail());
            lblRoleDisplay.setText("Rol Actual: " + usuario.getRole().toUpperCase());
            actualizarPreviewAvatar();
        }
    }
    
    private void actualizarPreviewAvatar() {
        if (imgAvatarConf == null) return;
        lblAvatarInitialsConf.setVisible(false);
        imgAvatarConf.setImage(null);
        
        if (nuevaFotoDatos != null && nuevaFotoDatos.length > 0) {
            try {
                Image img = new Image(new java.io.ByteArrayInputStream(nuevaFotoDatos), 80, 80, true, true);
                imgAvatarConf.setImage(img);
                imgAvatarConf.setClip(new Circle(40, 40, 40));
                lblFotoNombreConf.setText("Imagen personalizada");
            } catch(Exception e) {
                mostrarIniciales();
            }
        } else if (nuevaFotoPath != null && !nuevaFotoPath.isEmpty()) {
            try {
                String fotoUrl = nuevaFotoPath.startsWith("file:") ? nuevaFotoPath : "file:" + nuevaFotoPath;
                Image img = new Image(fotoUrl, 80, 80, true, true);
                imgAvatarConf.setImage(img);
                imgAvatarConf.setClip(new Circle(40, 40, 40));
                lblFotoNombreConf.setText(new File(nuevaFotoPath).getName());
            } catch(Exception e) {
                mostrarIniciales();
            }
        } else {
            mostrarIniciales();
        }
    }
    
    private void mostrarIniciales() {
        if (lblAvatarInitialsConf == null) return;
        String nombre = txtNombre.getText();
        String initial = (nombre != null && !nombre.isEmpty()) ? nombre.substring(0, 1).toUpperCase() : "?";
        lblAvatarInitialsConf.setText(initial);
        lblAvatarInitialsConf.setVisible(true);
        imgAvatarConf.setImage(null);
        lblFotoNombreConf.setText("Sin foto");
    }
    
    @FXML
    private void handleSeleccionarFoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Foto de Perfil");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(txtNombre.getScene().getWindow());
        if (selectedFile != null) {
            try {
                nuevaFotoPath = selectedFile.getName(); // Just the name for ref
                nuevaFotoDatos = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                actualizarPreviewAvatar();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleGuardar() {
        if (usuarioActual == null)
            return;

        String nuevoNombre = txtNombre.getText().trim();
        String nuevaPass = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (nuevoNombre.isEmpty()) {
            mostrarMensaje("El nombre no puede estar vacío.", true);
            return;
        }

        // Check if password change is requested
        String passToSave = usuarioActual.getPassword(); // Keep old password by default

        if (!nuevaPass.isEmpty()) {
            if (!nuevaPass.equals(confirmPass)) {
                mostrarMensaje("Las contraseñas no coinciden.", true);
                return;
            }
            passToSave = nuevaPass;
        }

        // Crear objeto actualizado
        Usuario usuarioActualizado = new Usuario(
                usuarioActual.getId(),
                nuevoNombre,
                usuarioActual.getEmail(),
                passToSave,
                usuarioActual.getStatus(),
                usuarioActual.getRole(),
                usuarioActual.getAeducoins(),
                nuevaFotoPath,
                nuevaFotoDatos);  // <-- incluir foto y datos

        // Guardar en base de datos
        if (usuarioDAO.updateUser(usuarioActualizado)) {
            LogService.logEditarUsuario(usuarioActualizado, usuarioActual.getNombre() + " (Autoedición)");

            // Actualizar referencia local y sesión
            this.usuarioActual = usuarioActualizado;
            SessionManager.getInstance().getUsuarioActual().setFotoPerfil(nuevaFotoPath);
            SessionManager.getInstance().getUsuarioActual().setFotoPerfilDatos(nuevaFotoDatos);
            SessionManager.getInstance().getUsuarioActual().setAeducoins(usuarioActualizado.getAeducoins());
            MainController.refreshSidebarAvatar();  // <-- refresh sidebar avatar

            // Limpiar campos de contraseña
            txtPassword.clear();
            txtConfirmPassword.clear();

            mostrarMensaje("Perfil actualizado correctamente.", false);
            AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Éxito", "Tus datos han sido guardados.");
        } else {
            mostrarMensaje("Ocurrió un error al guardar los cambios.", true);
        }
    }

    @FXML
    private void handleRevertir() {
        // Recargar los datos originales
        setUsuarioActual(this.usuarioActual);
        txtPassword.clear();
        txtConfirmPassword.clear();
        mostrarMensaje("Cambios descartados.", false);
    }

    private void mostrarMensaje(String msj, boolean esError) {
        lblMensaje.setText(msj);
        if (esError) {
            lblMensaje.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Red
        } else {
            lblMensaje.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;"); // Green
        }
    }
}
