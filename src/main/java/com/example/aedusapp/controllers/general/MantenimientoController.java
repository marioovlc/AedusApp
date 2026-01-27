package com.example.aedusapp.controllers.general;

import com.example.aedusapp.database.IncidenciaDAO;
import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.List;

public class MantenimientoController {

    @FXML
    private Label lblTotalPendientes;
    @FXML
    private Label lblEnRevision;
    @FXML
    private Label lblResueltasHoy;
    @FXML
    private VBox incidentGrid; // Contenedor para las incidencias

    private IncidenciaDAO incidenciaDAO;

    public void initialize() {
        incidenciaDAO = new IncidenciaDAO();
    }

    public void setUsuarioActual(Usuario usuario) {
        cargarDatos();
    }

    private void cargarDatos() {
        incidentGrid.getChildren().clear();
        Label loadingLabel = new Label("Cargando datos de mantenimiento...");
        incidentGrid.getChildren().add(loadingLabel);

        javafx.concurrent.Task<List<Incidencia>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Incidencia> call() throws Exception {
                return incidenciaDAO.obtenerTodasIncidencias();
            }
        };

        task.setOnSucceeded(e -> {
            List<Incidencia> incidencias = task.getValue();
            int pendientes = 0;
            int revision = 0;
            int resueltas = 0;

            incidentGrid.getChildren().clear();

            for (Incidencia inc : incidencias) {
                if ("NO LEIDO".equalsIgnoreCase(inc.getEstado()) || "LEIDO".equalsIgnoreCase(inc.getEstado()))
                    pendientes++;
                else if ("EN REVISION".equalsIgnoreCase(inc.getEstado()))
                    revision++;
                else if ("ACABADO".equalsIgnoreCase(inc.getEstado()))
                    resueltas++;

                if (!"ACABADO".equalsIgnoreCase(inc.getEstado())) {
                    incidentGrid.getChildren().add(crearTarjetaMantenimiento(inc));
                }
            }

            lblTotalPendientes.setText(String.valueOf(pendientes));
            lblEnRevision.setText(String.valueOf(revision));
            lblResueltasHoy.setText(String.valueOf(resueltas));
        });

        task.setOnFailed(e -> {
            incidentGrid.getChildren().clear();
            incidentGrid.getChildren().add(new Label("Error al cargar datos."));
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private VBox crearTarjetaMantenimiento(Incidencia inc) {
        VBox card = new VBox(10);
        card.getStyleClass().add("mantenimiento-card");

        // Resaltar si no leido
        if ("NO LEIDO".equalsIgnoreCase(inc.getEstado())) {
            card.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
        }

        Label titulo = new Label(inc.getTitulo() + " #" + inc.getId());
        titulo.getStyleClass().add("card-title");

        // Mostrar creador
        String creador = inc.getCreadorNombre() != null ? inc.getCreadorNombre() : "Desconocido";
        Label lblCreador = new Label("Reportado por: " + creador);
        lblCreador.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        Label desc = new Label(inc.getDescripcion());
        desc.setWrapText(true);

        Label estado = new Label("Estado: " + inc.getEstado());
        if ("NO LEIDO".equalsIgnoreCase(inc.getEstado())) {
            estado.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;"); // Rojo para no leido
        } else {
            estado.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22;");
        }

        TextArea resolucionArea = new TextArea();
        resolucionArea.setPromptText("Escribir resolución...");
        resolucionArea.setPrefRowCount(2);

        Button btnResolver = new Button("Marcar como ACABADO");
        btnResolver.getStyleClass().add("action-button");
        btnResolver.getStyleClass().add("success");
        btnResolver.setOnAction(e -> resolverIncidencia(inc, resolucionArea.getText()));

        Button btnRevision = new Button("Poner EN REVISION");
        btnRevision.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        btnRevision.setOnAction(e -> actualizarEstado(inc, "EN REVISION"));

        // Añadir botones de marcar como LEIDO si está en NO LEIDO
        if ("NO LEIDO".equalsIgnoreCase(inc.getEstado())) {
            Button btnLeido = new Button("Marcar LEIDO");
            btnLeido.setOnAction(e -> actualizarEstado(inc, "LEIDO"));
            card.getChildren().addAll(titulo, lblCreador, desc, estado, btnLeido, resolucionArea, new Separator(),
                    btnRevision, btnResolver);
        } else {
            card.getChildren().addAll(titulo, lblCreador, desc, estado, resolucionArea, new Separator(), btnRevision,
                    btnResolver);
        }

        return card;
    }

    private void resolverIncidencia(Incidencia inc, String resolucionText) {
        // Si no hay texto, poner uno por defecto
        if (resolucionText == null || resolucionText.isEmpty()) {
            resolucionText = "Resuelto por mantenimiento.";
        }

        if (incidenciaDAO.actualizarResolucion(inc.getId(), resolucionText, "ACABADO")) {
            cargarDatos(); // Recargar vista
        } else {
            // Mostrar error (opcional)
        }
    }

    private void actualizarEstado(Incidencia inc, String nuevoEstado) {
        if (incidenciaDAO.actualizarEstado(inc.getId(), nuevoEstado)) {
            cargarDatos();
        }
    }
}
