package com.example.aedusapp.controllers;

import com.example.aedusapp.models.Usuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
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
    private ComboBox<String> comboRole; // Lista desplegable para elegir rol (Admin/Profesor)
    @FXML
    private PasswordField txtPassword; // Campo para cambiar contraseña

    private Usuario usuario; // El usuario que se está editando
    private boolean saveClicked = false; // Bandera para saber si se pulsó "Guardar"

    // Configuración inicial
    @FXML
    public void initialize() {
        // Llenar el desplegable con las opciones posibles
        comboRole.setItems(FXCollections.observableArrayList("Administrador", "Profesor"));
    }

    // Método para cargar los datos del usuario en los campos
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;

        txtNombre.setText(usuario.getNombre());
        txtEmail.setText(usuario.getEmail());

        // Seleccionar el rol correcto en el desplegable
        if (usuario.getRoleId() == 1) {
            comboRole.setValue("Administrador");
        } else {
            comboRole.setValue("Profesor");
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
            usuario = new Usuario(
                    usuario.getId(),
                    txtNombre.getText(),
                    txtEmail.getText(),
                    // Si la contraseña está vacía, mantener la vieja. Si no, usar la nueva.
                    txtPassword.getText().isEmpty() ? usuario.getPassword() : txtPassword.getText(),
                    usuario.getStatus(),
                    comboRole.getValue().equals("Administrador") ? 1 : 2);

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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(txtNombre.getScene().getWindow());
            alert.setTitle("Campos Inválidos");
            alert.setHeaderText("Por favor corrige los campos inválidos");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}
