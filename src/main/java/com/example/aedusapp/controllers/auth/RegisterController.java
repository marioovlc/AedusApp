package com.example.aedusapp.controllers.auth;

import com.example.aedusapp.MainApp;
import com.example.aedusapp.database.daos.UsuarioDAO;
import com.example.aedusapp.models.Usuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

// Controlador para la pantalla de registro de nuevos usuarios
public class RegisterController {

    // Campos de la interfaz gráfica (vinculados con el FXML)
    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private Label lblError; // Etiqueta para mostrar errores en rojo

    // Objeto para manejar la base de datos
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    // Método que se ejecuta al pulsar "Registrarse"
    @FXML
    protected void handleRegister() {
        // Recoger los datos del formulario
        String nombre = txtNombre.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // Verificar que no haya campos vacíos
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            lblError.setText("Por favor, rellena todos los campos.");
            return;
        }

        // Verificar que las contraseñas coincidan
        if (!password.equals(confirmPassword)) {
            lblError.setText("Las contraseñas no coinciden.");
            return;
        }

        // Crear el nuevo usuario (ID null para insert, Rol 2 = usuario
        // estándar/profesor, Estado = PENDING)
        Usuario newUser = new Usuario(null, nombre, email, password, "PENDING", 2);

        // Intentar guardar en base de datos
        if (usuarioDAO.registerUser(newUser)) {
            com.example.aedusapp.utils.ui.AlertUtils.showAlert(Alert.AlertType.INFORMATION, "Registro Exitoso",
                    "Tu cuenta ha sido creada y está pendiente de aprobación por un administrador.");
            handleBackToLogin(); // Volver al login si todo sale bien
        } else {
            lblError.setText("Error al registrar. El email podría estar en uso.");
        }
    }

    // Método para volver a la pantalla de Login
    @FXML
    protected void handleBackToLogin() {
        try {
            // Cargar la vista de Login
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("views/auth/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);

            // Añadir hoja de estilos
            String css = MainApp.class.getResource("styles/styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            // Cambiar la escena actual
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para mostrar alertas emergentes
    // Delegado en AlertUtils
}
