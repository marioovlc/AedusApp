package com.example.aedusapp.controllers.general;

import com.example.aedusapp.database.daos.IncidenciaDAO;
import com.example.aedusapp.database.daos.UsuarioDAO;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.Map;

/**
 * Controlador optimizado para el Dashboard.
 * 
 * <p>
 * Optimizaciones implementadas:
 * 1. Carga de datos asíncrona para evitar congelar la UI (JavaFX Task).
 * 2. Uso de 'record' para transportar datos de forma inmutable y compacta.
 * 3. Separación de responsabilidades: Fetched Data vs UI Update.
 * </p>
 */
public class DashboardController {

    // --- Elementos de la UI ---
    @FXML
    private Label lblTotalIncidencias;
    @FXML
    private Label lblIncidenciasPendientes;
    @FXML
    private Label lblIncidenciasResueltas;
    @FXML
    private Label lblTotalUsuarios;
    @FXML
    private Label lblResumenTexto;
    @FXML
    private LineChart<String, Number> lineChartTendencia;
    @FXML
    private BarChart<String, Number> barChartCategorias;
    @FXML
    private VBox achievementsContainer; // Contenedor para los checks de logros
    @FXML
    private VBox missionsContainer;

    private com.example.aedusapp.models.Usuario currentUser;


    private final IncidenciaDAO incidenciaDAO = new IncidenciaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final com.example.aedusapp.services.ai.AIService aiService = new com.example.aedusapp.services.ai.AIService();
    private final com.example.aedusapp.database.daos.MisionesDAO misionesDAO = new com.example.aedusapp.database.daos.MisionesDAO();

    // --- Record para transporte de datos (Java 14+) ---
    private record DashboardData(
            int totalIncidencias,
            int totalUsuarios,
            Map<String, Integer> estadisticasEstado,
            Map<String, Integer> estadisticasCategoria,
            Map<String, Integer> tendenciaSemanal) {
    }

    @FXML
    public void initialize() {
        // Iniciar carga de datos en segundo plano
        cargarDatosAsincrono();
    }

    public void setUsuarioActual(com.example.aedusapp.models.Usuario user) {
        this.currentUser = user;
        cargarLogrosUsuario();
        cargarMisionesDiarias();
    }


    /**
     * Carga los datos en un hilo separado para mantener la UI responsiva.
     */
    private void cargarDatosAsincrono() {
        lblResumenTexto.setText("Cargando estadísticas...");

        Task<DashboardData> dataTask = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                // Ejecución en hilo secundario: Consultas a BD
                // Se realizan todas las operaciones costosas aquí
                return new DashboardData(
                        incidenciaDAO.countTotalTickets(),
                        usuarioDAO.countTotalUsers(),
                        incidenciaDAO.getStatusStatistics(),
                        incidenciaDAO.getCategoryStatistics(),
                        incidenciaDAO.getTicketsByDays(7));
            }
        };

        // Al completar con éxito (hilo UI FX Application Thread)
        dataTask.setOnSucceeded(e -> actualizarInterfaz(dataTask.getValue()));

        // Manejo de errores
        dataTask.setOnFailed(e -> {
            lblResumenTexto.setText("Error al cargar datos.");
            Throwable ex = dataTask.getException();
            if (ex != null)
                ex.printStackTrace();
        });

        // Iniciar el hilo
        new Thread(dataTask).start();
    }

    /**
     * Actualiza los elementos visuales con los datos ya cargados.
     * 
     * @param data Datos inmutables provenientes de la BD.
     */
    private void actualizarInterfaz(DashboardData data) {
        // 1. Actualizar KPIs y calcular pendientes/resueltas
        int pendientes = 0;
        int resueltas = 0;

        for (Map.Entry<String, Integer> entry : data.estadisticasEstado().entrySet()) {
            String estado = entry.getKey();
            int count = entry.getValue();

            if (esEstadoPendiente(estado)) {
                pendientes += count;
            } else if (esEstadoResuelto(estado)) {
                resueltas += count;
            }
        }

        lblTotalIncidencias.setText(String.valueOf(data.totalIncidencias()));
        lblTotalUsuarios.setText(String.valueOf(data.totalUsuarios()));
        lblIncidenciasPendientes.setText(String.valueOf(pendientes));
        lblIncidenciasResueltas.setText(String.valueOf(resueltas));

        // 2. Actualizar Gráficos
        actualizarGrafico(barChartCategorias, "Incidencias", data.estadisticasCategoria());
        actualizarGrafico(lineChartTendencia, "Incidencias Diarias", data.tendenciaSemanal());

        // 3. Resumen IA Asíncrono
        generarResumenIA(data, pendientes);
    }

    private void generarResumenIA(DashboardData data, int pendientes) {
        lblResumenTexto.setText("🤖 Aedus AI está analizando las métricas...");
        
        Task<String> aiTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                String statsContext = String.format(
                    "Métricas del sistema:\n- Total Incidencias: %d\n- Pendientes: %d\n- Usuarios: %d\n- Categorías: %s",
                    data.totalIncidencias(), pendientes, data.totalUsuarios(), data.estadisticasCategoria().toString()
                );
                return aiService.askAI("Genera un resumen ejecutivo muy breve (máximo 3 líneas) del estado actual del sistema para el dashboard.", statsContext);
            }
        };

        aiTask.setOnSucceeded(e -> lblResumenTexto.setText(aiTask.getValue()));
        aiTask.setOnFailed(e -> lblResumenTexto.setText("Sistema estable. " + pendientes + " pendientes de " + data.totalIncidencias() + " totales."));
        
        new Thread(aiTask).start();
    }

    private void cargarLogrosUsuario() {
        if (currentUser == null || achievementsContainer == null) return;
        
        Task<List<com.example.aedusapp.models.Achievement>> task = new Task<>() {
            @Override
            protected List<com.example.aedusapp.models.Achievement> call() throws Exception {
                return new com.example.aedusapp.database.daos.AchievementDAO().getUserAchievements(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            achievementsContainer.getChildren().clear();
            List<com.example.aedusapp.models.Achievement> achievements = task.getValue();
            if (achievements.isEmpty()) {
                Label lbl = new Label("Aún no has desbloqueado logros. ¡Sigue participando!");
                lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                achievementsContainer.getChildren().add(lbl);
            } else {
                for (com.example.aedusapp.models.Achievement a : achievements) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: rgba(30, 41, 59, 0.3); -fx-padding: 8; -fx-background-radius: 8;");
                    
                    org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon("fas-medal");
                    icon.setIconSize(16);
                    icon.setIconColor(Color.web("#fcd34d"));
                    
                    VBox texts = new VBox(2);
                    Label title = new Label(a.getTitle());
                    title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
                    Label desc = new Label(a.getDescription());
                    desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
                    
                    texts.getChildren().addAll(title, desc);
                    row.getChildren().addAll(icon, texts);
                    achievementsContainer.getChildren().add(row);
                }
            }
        });

        new Thread(task).start();
    }

    private void cargarMisionesDiarias() {
        if (currentUser == null || missionsContainer == null) return;
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Registrar misión de login
                boolean logueoNovedad = misionesDAO.registrarMisionDiaria(currentUser.getId(), "LOGIN_DIARIO", 5);
                if (logueoNovedad) {
                    javafx.application.Platform.runLater(() -> {
                        currentUser.setAeducoins(currentUser.getAeducoins() + 5);
                        com.example.aedusapp.controllers.general.MainController.showGlobalLoading(true, "¡Misión completada: Inicio de sesión diario (+5 Aedus)!");
                        new Thread(() -> { try { Thread.sleep(2000); } catch(Exception ignored){} javafx.application.Platform.runLater(()->com.example.aedusapp.controllers.general.MainController.showGlobalLoading(false,""));}).start();
                    });
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            missionsContainer.getChildren().clear();
            
            HBox mision1 = createMissionRow("🎮 Iniciar Sesión Diariamente", "+5 Aedus", true); // Porque o la acaba de hacer, o ya la tenía
            HBox mision2 = createMissionRow("🛠 Crear o Resolver un Ticket", "+15 Aedus", false); 
            
            missionsContainer.getChildren().addAll(mision1, mision2);
        });

        new Thread(task).start();
    }

    private HBox createMissionRow(String title, String reward, boolean completed) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(30, 41, 59, 0.3); -fx-padding: 10; -fx-background-radius: 8;");
        
        org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon(completed ? "fas-check-circle" : "fas-circle");
        icon.setIconSize(18);
        icon.setIconColor(Color.web(completed ? "#10b981" : "#64748b"));
        
        VBox texts = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        if (completed) {
            lblTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 14px; -fx-strikethrough: true;");
        }
        
        Label lblReward = new Label("Recompensa: " + reward);
        lblReward.setStyle("-fx-text-fill: #fcd34d; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        texts.getChildren().addAll(lblTitle, lblReward);
        row.getChildren().addAll(icon, texts);
        return row;
    }

    // --- Métodos Auxiliares ---

    private boolean esEstadoPendiente(String estado) {
        return "Abierta".equalsIgnoreCase(estado) || "En Proceso".equalsIgnoreCase(estado)
                || "No Leído".equalsIgnoreCase(estado);
    }

    private boolean esEstadoResuelto(String estado) {
        return "Cerrada".equalsIgnoreCase(estado) || "Resuelta".equalsIgnoreCase(estado);
    }

    private void actualizarGrafico(XYChart<String, Number> chart, String seriesName,
            Map<String, Integer> datos) {
        chart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);

        for (Map.Entry<String, Integer> entry : datos.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chart.getData().add(series);
    }
}
