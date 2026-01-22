package com.example.aedusapp.controllers;

import com.example.aedusapp.MainApp;
import com.example.aedusapp.database.UsuarioDAO;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.LogService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

// Controlador para la pantalla de inicio de sesión (Login)
public class LoginController {

    // Campos de la interfaz (vinculados con el archivo FXML)
    @FXML
    private TextField txtEmail; // Campo para escribir el email

    @FXML
    private PasswordField txtPassword; // Campo para escribir la contraseña (se ven puntitos)

    @FXML
    private Label lblError; // Etiqueta para mensajes de error en rojo

    @FXML
    private Button btnLogin; // Botón de iniciar sesión

    // Asistente para hablar con la base de datos
    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    // Método que se activa al pulsar el botón "Iniciar Sesión"
    @FXML
    public void handleLogin() {
        // Recoger email y contraseña escritos
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        // Verificar que no estén vacíos
        if (email.isEmpty() || password.isEmpty()) {
            lblError.setText("Por favor, complete todos los campos.");
            return;
        }

        // Consultar a la base de datos si el usuario existe y la contraseña es correcta
        Usuario usuario = usuarioDAO.validarUsuario(email, password);

        // Si el usuario existe (no es nulo)
        if (usuario != null) {
            // Registrar evento de login en el sistema de logs
            LogService.logLogin(usuario, "localhost");

            lblError.setText(""); // Limpiar errores anteriores
            abrirDashboard(); // Entrar a la aplicación principal
        } else {
            lblError.setText("Credenciales incorrectas."); // Mostrar error si falla
        }
    }

    // Método para cambiar de pantalla y abrir el panel principal (Dashboard)
    private void abrirDashboard() {
        try {
            // Cargar la vista principal (main.fxml)
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1024, 768);

            // Cargar hoja de estilos
            String css = MainApp.class.getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            // Obtener la ventana actual
            Stage stage = (Stage) btnLogin.getScene().getWindow();

            // Pasarle el usuario conectado al controlador principal para que sepa quién es
            MainController controller = fxmlLoader.getController();
            controller.setUsuario(usuarioDAO.validarUsuario(txtEmail.getText(), txtPassword.getText()));

            // Configurar y mostrar la nueva escena
            stage.setTitle("Aedus App - Dashboard");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            lblError.setText("Error al cargar el dashboard.");
        }
    }

    // Método que se activa al pulsar "Regístrate aquí"
    @FXML
    protected void handleRegister() {
        try {
            // Cargar la pantalla de registro (register.fxml)
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("register.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);

            // Cargar estilos
            String css = MainApp.class.getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            // Cambiar a la pantalla de registro
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método secreto (o de desarrollo) para entrar como administrador
    // automáticamente
    @FXML
    protected void handleAutoAdmin() {
        txtEmail.setText("admin@aedus.com");
        txtPassword.setText("admin123");
        handleLogin();
    }
}
