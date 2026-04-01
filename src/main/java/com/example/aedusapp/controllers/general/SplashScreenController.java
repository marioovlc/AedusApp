package com.example.aedusapp.controllers.general;

import com.example.aedusapp.MainApp;
import com.example.aedusapp.database.config.DatabaseSetup;
import com.example.aedusapp.utils.config.ThemeManager;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.example.aedusapp.utils.config.SessionManager;
import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.database.daos.UsuarioDAO;

public class SplashScreenController {

    @FXML
    private StackPane rootPane;
    @FXML
    private ImageView imgLogo;
    @FXML
    private Label lblStatus;
    @FXML
    private ProgressBar progressBar;

    @FXML
    public void initialize() {
        // Init animations
        startLogoAnimation();
        
        // Start load task
        startLoadingProcess();
    }

    private void startLogoAnimation() {
        ScaleTransition st = new ScaleTransition(Duration.millis(1500), imgLogo);
        st.setByX(0.1);
        st.setByY(0.1);
        st.setCycleCount(ScaleTransition.INDEFINITE);
        st.setAutoReverse(true);
        st.play();
    }

    private void startLoadingProcess() {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Conectando con la base de datos...");
                updateProgress(0.1, 1.0);
                Thread.sleep(100); 

                updateProgress(0.3, 1.0);
                DatabaseSetup.run(); 
                
                updateMessage("Verificando integridad...");
                updateProgress(0.6, 1.0);
                Thread.sleep(200);

                updateMessage("Cargando módulos...");
                updateProgress(0.9, 1.0);
                Thread.sleep(100);

                updateMessage("Listo!");
                updateProgress(1.0, 1.0);
                Thread.sleep(100);
                
                return null;
            }
        };

        lblStatus.textProperty().bind(loadTask.messageProperty());
        progressBar.progressProperty().bind(loadTask.progressProperty());

        loadTask.setOnSucceeded(e -> {
            checkAutoLogin();
        });
        loadTask.setOnFailed(e -> {
            lblStatus.textProperty().unbind();
            lblStatus.setText("Error al iniciar: " + loadTask.getException().getMessage());
            lblStatus.setStyle("-fx-text-fill: #ef4444;");
        });

        new Thread(loadTask).start();
    }

    private void checkAutoLogin() {
        String savedId = SessionManager.getInstance().getSavedUserId();
        if (savedId != null) {
            lblStatus.textProperty().unbind();
            lblStatus.setText("Reconectando sesión...");
            Task<Usuario> autoLoginTask = new Task<>() {
                @Override protected Usuario call() throws Exception {
                    return new UsuarioDAO().getUserById(savedId);
                }
            };
            autoLoginTask.setOnSucceeded(ev -> {
                Usuario user = autoLoginTask.getValue();
                if (user != null) {
                    SessionManager.getInstance().setUsuarioActual(user);
                    com.example.aedusapp.services.logging.LogService.logLogin(user, "auto-login");
                    transitionToMain();
                } else {
                    transitionToLogin();
                }
            });
            autoLoginTask.setOnFailed(ev -> transitionToLogin());
            new Thread(autoLoginTask).start();
        } else {
            transitionToLogin();
        }
    }

    private void transitionToMain() {
        FadeTransition ft = new FadeTransition(Duration.millis(800), rootPane);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            try {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("views/general/main.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
                ThemeManager.applyTheme(scene);
                stage.setTitle("Aedus App - Dashboard");
                stage.setScene(scene);
                stage.centerOnScreen();
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        ft.play();
    }

    private void transitionToLogin() {
        FadeTransition ft = new FadeTransition(Duration.millis(1000), rootPane);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            try {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("views/auth/login.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 800, 600);
                ThemeManager.applyTheme(scene);
                stage.setResizable(false); // Login usually fixed
                stage.setScene(scene);
                stage.centerOnScreen();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        ft.play();
    }
}
