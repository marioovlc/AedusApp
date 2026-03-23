package com.example.aedusapp.controllers.logs;

import com.example.aedusapp.database.daos.LogDAO;
import com.example.aedusapp.models.Log;
import com.example.aedusapp.models.Usuario;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vista de Logs – Timeline agrupada por fecha con chips de filtro.
 */
public class LogsController {

    // ── Stats ──────────────────────────────────────────────────────────
    @FXML
    private Label lblTotalLogs;
    @FXML
    private Label lblLogin;
    @FXML
    private Label lblIncidencia;
    @FXML
    private Label lblUsuario;
    @FXML
    private Label lblError;

    // ── Filtros ────────────────────────────────────────────────────────
    @FXML
    private Button chipTodas;
    @FXML
    private Button chipLogin;
    @FXML
    private Button chipIncidencia;
    @FXML
    private Button chipUsuario;
    @FXML
    private Button chipSistema;
    @FXML
    private Button chipError;
    @FXML
    private TextField txtBuscarUsuario;

    // ── Timeline ───────────────────────────────────────────────────────
    @FXML
    private VBox timelineContainer;

    private LogDAO logDAO;
    private String categoriaActiva = "Todas"; // estado del chip seleccionado

    @FXML
    public void initialize() {
        logDAO = new LogDAO();
    }

    public void setUsuarioActual(Usuario usuario) {
        cargarLogs();
        actualizarEstadisticas();
    }

    // ── Chip handlers ──────────────────────────────────────────────────
    @FXML
    private void onChipTodas() {
        activarChip("Todas");
        aplicarFiltros();
    }

    @FXML
    private void onChipLogin() {
        activarChip("LOGIN");
        aplicarFiltros();
    }

    @FXML
    private void onChipIncidencia() {
        activarChip("INCIDENCIA");
        aplicarFiltros();
    }

    @FXML
    private void onChipUsuario() {
        activarChip("USUARIO");
        aplicarFiltros();
    }

    @FXML
    private void onChipSistema() {
        activarChip("SISTEMA");
        aplicarFiltros();
    }

    @FXML
    private void onChipError() {
        activarChip("ERROR");
        aplicarFiltros();
    }

    private void activarChip(String categoria) {
        categoriaActiva = categoria;
        Button[] chips = { chipTodas, chipLogin, chipIncidencia, chipUsuario, chipSistema, chipError };
        String[] cats = { "Todas", "LOGIN", "INCIDENCIA", "USUARIO", "SISTEMA", "ERROR" };
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null)
                continue;
            chips[i].getStyleClass().remove("chip-active");
            if (cats[i].equals(categoria))
                chips[i].getStyleClass().add("chip-active");
        }
    }

    @FXML
    public void aplicarFiltros() {
        cargarLogs();
    }

    @FXML
    private void refrescarLogs() {
        cargarLogs();
        actualizarEstadisticas();
    }

    @FXML
    private void borrarLogs() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar borrado");
        alert.setHeaderText("¿Eliminar TODOS los logs?");
        alert.setContentText("Esta acción no se puede deshacer.");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (logDAO.deleteAllLogs()) {
                cargarLogs();
                actualizarEstadisticas();
            }
        }
    }

    // ── Carga y construcción de la timeline ────────────────────────────
    private void cargarLogs() {
        String busqueda = txtBuscarUsuario != null ? txtBuscarUsuario.getText() : "";

        javafx.concurrent.Task<List<Log>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Log> call() throws Exception {
                List<Log> logs = logDAO.getAllLogs();

                // Filtrar por categoría
                if (!"Todas".equals(categoriaActiva)) {
                    String cat = categoriaActiva;
                    logs = logs.stream()
                            .filter(l -> cat.equalsIgnoreCase(l.getCategoria()))
                            .collect(Collectors.toList());
                }

                // Filtrar por búsqueda de usuario
                if (busqueda != null && !busqueda.isBlank()) {
                    String lower = busqueda.toLowerCase();
                    logs = logs.stream()
                            .filter(l -> l.getUsuarioNombre() != null
                                    && l.getUsuarioNombre().toLowerCase().contains(lower))
                            .collect(Collectors.toList());
                }

                return logs;
            }
        };

        task.setOnSucceeded(e -> construirTimeline(task.getValue()));
        task.setOnFailed(e -> {
            timelineContainer.getChildren().clear();
            Label err = new Label("⚠ Error al cargar el historial.");
            err.setStyle("-fx-text-fill: #f87171; -fx-font-size: 14px; -fx-padding: 20;");
            timelineContainer.getChildren().add(err);
        });

        new Thread(task).start();
    }

    /** Construye la timeline agrupando logs por fecha (Hoy / Ayer / dd MMM yyyy) */
    private void construirTimeline(List<Log> logs) {
        timelineContainer.getChildren().clear();

        if (logs.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("📋");
            icon.setStyle("-fx-font-size: 48px; -fx-opacity: 0.3;");
            Label title = new Label("Sin registros");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #475569;");
            Label sub = new Label("No hay actividad que coincida con los filtros seleccionados.");
            sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
            empty.getChildren().addAll(icon, title, sub);
            timelineContainer.getChildren().add(empty);
            return;
        }

        // Agrupar por día
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

        Map<LocalDate, List<Log>> porDia = new LinkedHashMap<>();
        for (Log log : logs) {
            LocalDate fecha = log.getFechaCreacion() != null
                    ? log.getFechaCreacion().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.of(1970, 1, 1);
            porDia.computeIfAbsent(fecha, k -> new ArrayList<>()).add(log);
        }

        // Ordenar: más reciente primero
        List<LocalDate> diasOrdenados = porDia.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        SimpleDateFormat horaFmt = new SimpleDateFormat("HH:mm");

        for (LocalDate dia : diasOrdenados) {
            // ── Separador de fecha ──
            String diaLabel = dia.equals(hoy) ? "HOY"
                    : dia.equals(ayer) ? "AYER"
                            : dia.format(labelFmt).toUpperCase();

            HBox dateRow = new HBox(12);
            dateRow.setAlignment(Pos.CENTER_LEFT);
            dateRow.setPadding(new Insets(18, 0, 8, 0));

            Label lDate = new Label(diaLabel);
            lDate.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #344963;");

            Region lineLeft = new Region();
            lineLeft.setPrefHeight(1);
            lineLeft.setMaxHeight(1);
            lineLeft.setStyle("-fx-background-color: #1c2d47;");
            HBox.setHgrow(lineLeft, Priority.ALWAYS);

            dateRow.getChildren().addAll(lineLeft, lDate);
            timelineContainer.getChildren().add(dateRow);

            // ── Entradas del día ──
            for (Log log : porDia.get(dia)) {
                HBox row = crearFilaLog(log, horaFmt);
                timelineContainer.getChildren().add(row);
            }
        }
    }

    /**
     * Construye una fila individual de log con ícono de color, actor, acción y hora
     */
    private HBox crearFilaLog(Log log, SimpleDateFormat horaFmt) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 0));
        row.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-border-color: #0f1a2d;");

        // Puntos de color de la categoría
        String catColor = getCatColor(log.getCategoria());
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + catColor + "; -fx-font-size: 16px;");
        dot.setMinWidth(20);

        // Badge de categoría
        Label badge = new Label(getCatEmoji(log.getCategoria()) + " " + log.getCategoria());
        badge.setStyle(
                "-fx-background-color: " + hexToRgba(catColor, 0.12) + ";" +
                        "-fx-text-fill: " + catColor + ";" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 8;" +
                        "-fx-background-radius: 10;");
        badge.setMinWidth(90);

        // Avatar inicial del usuario
        String nombre = log.getUsuarioNombre() != null ? log.getUsuarioNombre() : "Sistema";
        Label avatar = new Label(nombre.substring(0, 1).toUpperCase());
        avatar.setMinSize(28, 28);
        avatar.setMaxSize(28, 28);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: " + hexToRgba(catColor, 0.2) + ";" +
                        "-fx-text-fill: " + catColor + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 14;");

        // Bloque de texto: usuario + descripción
        VBox textBlock = new VBox(2);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        Label lblNombre = new Label(nombre);
        lblNombre.setStyle("-fx-text-fill: #c8d5ea; -fx-font-weight: bold; -fx-font-size: 13px;");

        String desc = log.getDescripcion() != null ? log.getDescripcion() : log.getAccion();
        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        lblDesc.setWrapText(false);

        textBlock.getChildren().addAll(lblNombre, lblDesc);

        // Hora
        String hora = log.getFechaCreacion() != null ? horaFmt.format(log.getFechaCreacion()) : "--:--";
        Label lblHora = new Label(hora);
        lblHora.setStyle("-fx-text-fill: #344963; -fx-font-size: 11px;");
        lblHora.setMinWidth(40);
        lblHora.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(dot, badge, avatar, textBlock, lblHora);

        // Hover
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: rgba(79,142,247,0.04);" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-border-color: #1c2d47;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-border-color: #0f1a2d;"));

        return row;
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private String getCatColor(String cat) {
        if (cat == null)
            return "#64748b";
        return switch (cat.toUpperCase()) {
            case "LOGIN" -> "#4f8ef7";
            case "INCIDENCIA" -> "#a78bfa";
            case "USUARIO" -> "#34d399";
            case "SISTEMA" -> "#64748b";
            case "ERROR" -> "#f87171";
            default -> "#64748b";
        };
    }

    private String getCatEmoji(String cat) {
        if (cat == null)
            return "•";
        return switch (cat.toUpperCase()) {
            case "LOGIN" -> "🔵";
            case "INCIDENCIA" -> "🟣";
            case "USUARIO" -> "🟢";
            case "SISTEMA" -> "⚫";
            case "ERROR" -> "🔴";
            default -> "•";
        };
    }

    private String hexToRgba(String hex, double alpha) {
        try {
            hex = hex.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return "rgba(" + r + "," + g + "," + b + "," + alpha + ")";
        } catch (Exception e) {
            return "rgba(100,116,139,0.15)";
        }
    }

    // ── Estadísticas ───────────────────────────────────────────────────
    private void actualizarEstadisticas() {
        javafx.concurrent.Task<int[]> statsTask = new javafx.concurrent.Task<>() {
            @Override
            protected int[] call() throws Exception {
                return new int[] {
                        logDAO.countAllLogs(),
                        logDAO.countLogsByCategory("LOGIN"),
                        logDAO.countLogsByCategory("INCIDENCIA"),
                        logDAO.countLogsByCategory("USUARIO"),
                        logDAO.countLogsByCategory("ERROR")
                };
            }
        };
        statsTask.setOnSucceeded(e -> {
            int[] s = statsTask.getValue();
            if (lblTotalLogs != null)
                lblTotalLogs.setText(String.valueOf(s[0]));
            if (lblLogin != null)
                lblLogin.setText(String.valueOf(s[1]));
            if (lblIncidencia != null)
                lblIncidencia.setText(String.valueOf(s[2]));
            if (lblUsuario != null)
                lblUsuario.setText(String.valueOf(s[3]));
            if (lblError != null)
                lblError.setText(String.valueOf(s[4]));
        });
        new Thread(statsTask).start();
    }
}
