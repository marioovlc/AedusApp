package com.example.aedusapp.controllers.auth;

import com.example.aedusapp.MainApp;

import com.example.aedusapp.database.daos.UsuarioDAO;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.logging.LogService;
import com.example.aedusapp.utils.config.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;

// Controlador para la pantalla de inicio de sesión (Login)
public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;
    @FXML private ImageView loginLogo;

    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        ThemeManager.applyLogo(loginLogo);
    }

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

        btnLogin.setDisable(true); // Deshabilitar el botón para evitar múltiples clics
        lblError.setText("Iniciando sesión...");

        // Crear una tarea en segundo plano para no congelar la UI
        javafx.concurrent.Task<Usuario> loginTask = new javafx.concurrent.Task<>() {
            @Override
            protected Usuario call() throws Exception {
                // Consultar a la base de datos si el usuario existe y la contraseña es correcta
                return usuarioDAO.validateUser(email, password);
            }
        };

        // Si la tarea termina con éxito (haya encontrado o no al usuario)
        loginTask.setOnSucceeded(event -> {
            btnLogin.setDisable(false);
            Usuario usuario = loginTask.getValue();

            // Si el usuario existe (no es nulo)
            if (usuario != null) {
                // Guardar usuario en la sesión global y persistir
                com.example.aedusapp.utils.config.SessionManager.getInstance().saveSession(usuario);

                // Registrar evento de login en el sistema de logs
                LogService.logLogin(usuario, "localhost");

                lblError.setText(""); // Limpiar errores anteriores
                abrirDashboard(); // Entrar a la aplicación principal
            } else {
                lblError.setText("Credenciales incorrectas o usuario no activado."); // Mostrar error si falla
            }
        });

        // Si hay una excepción conectando a la base de datos
        loginTask.setOnFailed(event -> {
            btnLogin.setDisable(false);
            Throwable e = loginTask.getException();
            e.printStackTrace();
            lblError.setText("Error de conexión con la base de datos.");
        });

        // Iniciar la tarea en un hilo nuevo
        new Thread(loginTask).start();
    }

    // Método para cambiar de pantalla y abrir el panel principal (Dashboard)
    private void abrirDashboard() {
        try {
            // Cargar la vista principal (main.fxml)
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("views/general/main.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1024, 768);

            // Cargar y aplicar tema
            ThemeManager.applyTheme(scene);

            // Obtener la ventana actual
            Stage stage = (Stage) btnLogin.getScene().getWindow();

            // Nota: Ya no pasamos el usuario manualmente. MainController leerá de SessionManager

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
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("views/auth/register.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);

            // Cargar y aplicar tema
            ThemeManager.applyTheme(scene);

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
        txtEmail.setText("admin@aedus.es");
        txtPassword.setText("1234");
        handleLogin();
    }
}
