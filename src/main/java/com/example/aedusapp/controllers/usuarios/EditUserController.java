package com.example.aedusapp.controllers.usuarios;

import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.utils.config.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

// Controlador para la ventana de edición de usuario
public class EditUserController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private javafx.scene.control.CheckBox checkAdmin;
    @FXML private javafx.scene.control.CheckBox checkProfesor;
    @FXML private javafx.scene.control.CheckBox checkMantenimiento;
    @FXML private PasswordField txtPassword;
    @FXML private ImageView imgAvatar;
    @FXML private Label lblAvatarInitials;
    @FXML private Label lblFotoRuta;
    @FXML private StackPane avatarContainer;
    
    private String fotaPerfilPath = null;  // ruta local seleccionada

    private Usuario usuario; // El usuario que se está editando
    private boolean saveClicked = false; // Bandera para saber si se pulsó "Guardar"

    // Configuración inicial
    @FXML
    public void initialize() {
    }

    // Método para cargar los datos del usuario en los campos
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        txtNombre.setText(usuario.getNombre());
        txtEmail.setText(usuario.getEmail());
        String rol = usuario.getRole();
        checkAdmin.setSelected("admin".equalsIgnoreCase(rol));
        checkProfesor.setSelected("user".equalsIgnoreCase(rol));
        checkMantenimiento.setSelected("mantenimiento".equalsIgnoreCase(rol));
        
        fotaPerfilPath = usuario.getFotoPerfil();
        actualizarPreviewAvatar();
    }
    
    private void actualizarPreviewAvatar() {
        lblAvatarInitials.setVisible(false);
        imgAvatar.setImage(null);
        if (fotaPerfilPath != null && !fotaPerfilPath.isEmpty()) {
            try {
                String fotoUrl = fotaPerfilPath.startsWith("file:") ? fotaPerfilPath : "file:" + fotaPerfilPath;
                Image img = new Image(fotoUrl, 70, 70, true, true);
                imgAvatar.setImage(img);
                imgAvatar.setClip(new Circle(35, 35, 35));
                lblFotoRuta.setText(new File(fotaPerfilPath).getName());
            } catch (Exception e) {
                mostrarIniciales();
            }
        } else {
            mostrarIniciales();
        }
    }
    
    private void mostrarIniciales() {
        String nombre = txtNombre.getText();
        String initial = (nombre != null && !nombre.isEmpty()) ? nombre.substring(0, 1).toUpperCase() : "?";
        lblAvatarInitials.setText(initial);
        lblAvatarInitials.setVisible(true);
        imgAvatar.setImage(null);
        lblFotoRuta.setText("Sin foto seleccionada");
    }
    
    @FXML
    private void handleSeleccionarFoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Foto de Perfil");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(txtNombre.getScene().getWindow());
        if (selectedFile != null) {
            fotaPerfilPath = selectedFile.getAbsolutePath();
            actualizarPreviewAvatar();
        }
    }

    // Devuelve true si el usuario pulsó Guardar, false si canceló
    public boolean isSaveClicked() {
        return saveClicked;
    }

    // Devuelve el usuario con los datos modificados
    public Usuario getUsuario() {
        return usuario;
    }

    // Acción al pulsar el botón "Guardar"
    @FXML
    private void handleSave() {
        if (isInputValid()) {
            // Actualizar el objeto usuario con los nuevos datos
            String nombre = txtNombre.getText();
            String email = txtEmail.getText();
            String password = txtPassword.getText().isEmpty() ? usuario.getPassword() : txtPassword.getText();

            String role = "user"; // Por defecto
            if (checkAdmin.isSelected())
                role = "admin";
            else if (checkMantenimiento.isSelected())
                role = "mantenimiento";
            else if (checkProfesor.isSelected())
                role = "user";

            usuario = new Usuario(
                    usuario.getId(),
                    nombre,
                    email,
                    password,
                    usuario.getStatus(),
                    role,
                    usuario.getAeducoins(),
                    fotaPerfilPath);

            // Update session if editing the current user
            Usuario sesionUsuario = SessionManager.getInstance().getUsuarioActual();
            if (sesionUsuario != null && sesionUsuario.getId().equals(usuario.getId())) {
                sesionUsuario.setFotoPerfil(fotaPerfilPath);
            }

            saveClicked = true; // Marcar como guardado
            closeStage(); // Cerrar ventana
        }
    }

    // Acción al pulsar "Cancelar"
    @FXML
    private void handleCancel() {
        closeStage();
    }

    // Método para cerrar la ventana actual
    private void closeStage() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

    // Validar que los campos estén bien rellenos
    private boolean isInputValid() {
        String errorMessage = "";

        if (txtNombre.getText() == null || txtNombre.getText().length() == 0) {
            errorMessage += "Nombre inválido!\n";
        }
        if (txtEmail.getText() == null || txtEmail.getText().length() == 0) {
            errorMessage += "Email inválido!\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            // Mostrar error si algo falla
            // Mostrar error si algo falla
            com.example.aedusapp.utils.ui.AlertUtils.showAlert(Alert.AlertType.ERROR, "Campos Inválidos", errorMessage);
            return false;
        }
    }
}
