package com.example.aedusapp.controllers.usuarios;

import com.example.aedusapp.models.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Controlador para la ventana de edición de usuario
public class EditUserController {

    @FXML
    private TextField txtNombre; // Campo para editar nombre
    @FXML
    private TextField txtEmail; // Campo para editar email
    @FXML
    private javafx.scene.control.CheckBox checkAdmin;
    @FXML
    private javafx.scene.control.CheckBox checkProfesor;
    @FXML
    private javafx.scene.control.CheckBox checkMantenimiento;
    @FXML
    private PasswordField txtPassword; // Campo para cambiar contraseña

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

        checkAdmin.setSelected(usuario.hasRole(1));
        checkProfesor.setSelected(usuario.hasRole(2));
        checkMantenimiento.setSelected(usuario.hasRole(3));
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

            java.util.List<Integer> roles = new java.util.ArrayList<>();
            if (checkAdmin.isSelected())
                roles.add(1);
            if (checkProfesor.isSelected())
                roles.add(2);
            if (checkMantenimiento.isSelected())
                roles.add(3);

            usuario = new Usuario(
                    usuario.getId(),
                    nombre,
                    email,
                    password,
                    usuario.getStatus(),
                    roles);

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
            com.example.aedusapp.utils.AlertUtils.showAlert(Alert.AlertType.ERROR, "Campos Inválidos", errorMessage);
            return false;
        }
    }
}
