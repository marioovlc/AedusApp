package com.example.aedusapp.components;

import com.example.aedusapp.models.Incidencia;
import com.example.aedusapp.models.EstadoIncidencia;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

/**
 * Custom Component para renderizar una tarjeta de incidencia.
 * Al extender de VBox y usar <fx:root>, maneja su propio FXML.
 */
public class TarjetaIncidencia extends VBox {

    @FXML private VBox rootBox;
    @FXML private Label lblId;
    @FXML private Label lblTitulo;
    @FXML private Label lblEstado;
    @FXML private HBox chipsContainer;
    @FXML private Label lblDescripcion;
    @FXML private HBox imagenContainer;
    @FXML private VBox footerContainer;
    @FXML private HBox stepperContainer;

    private final Incidencia incidencia;
    private final Consumer<Integer> onEliminarCallback;
    private final Consumer<String> onImageClickCallback;

    public TarjetaIncidencia(Incidencia incidencia, Consumer<Integer> onEliminar, Consumer<String> onImageClick) {
        this.incidencia = incidencia;
        this.onEliminarCallback = onEliminar;
        this.onImageClickCallback = onImageClick;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/aedusapp/components/TarjetaIncidencia.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException("Error cargando el FXML de TarjetaIncidencia", exception);
        }

        inicializarUi();
    }

    private void inicializarUi() {
        String catColor = getCategoryColor(incidencia.getCategoriaNombre());
        String catIcon = getCategoryIcon(incidencia.getCategoriaNombre());

        // Borde izquierdo dinámico
        String borderStyle = "-fx-border-width: 0 0 0 4; -fx-border-radius: 14 0 0 14; -fx-border-color: " + catColor + ";";
        this.setStyle(borderStyle);
        this.setOnMouseEntered(e -> this.setStyle(borderStyle));
        this.setOnMouseExited(e -> this.setStyle(borderStyle));

        // Cabecera
        lblId.setText("#" + incidencia.getId());
        lblId.setStyle("-fx-font-size: 11px; -fx-text-fill: " + catColor + "; -fx-font-weight: bold; -fx-opacity: 0.8;");
        lblTitulo.setText(incidencia.getTitulo());
        
        lblEstado.setText(getEstadoEmoji(incidencia.getEstado()) + " " + incidencia.getEstado());
        lblEstado.getStyleClass().addAll(getEstadoClass(incidencia.getEstado()));

        // Descripción
        lblDescripcion.setText(incidencia.getDescripcion());

        // Chips
        if (incidencia.getCategoriaNombre() != null) {
            chipsContainer.getChildren().add(crearChip(catIcon + " " + incidencia.getCategoriaNombre(), catColor));
        }
        if (incidencia.getAulaNombre() != null) {
            String aula = "🚪 Aula " + incidencia.getAulaNombre();
            if (incidencia.getAulaTipo() != null && !incidencia.getAulaTipo().isEmpty()) {
                aula += "  " + incidencia.getAulaTipo();
            }
            chipsContainer.getChildren().add(crearChip(aula, "#64748b"));
        }
        if (incidencia.getFechaCreacion() != null) {
            String fecha = new SimpleDateFormat("dd MMM yyyy, HH:mm").format(incidencia.getFechaCreacion());
            chipsContainer.getChildren().add(crearChip("📅 " + fecha, "#475569"));
        }

        // Resolución / Footer si está acabado
        EstadoIncidencia estadoActual = EstadoIncidencia.fromDbValue(incidencia.getEstado());
        if (EstadoIncidencia.ACABADO == estadoActual && incidencia.getResolucion() != null && !incidencia.getResolucion().isEmpty()) {
            VBox resolucionBox = new VBox(4);
            resolucionBox.getStyleClass().add("resolucion-box");
            Label lblResTitulo = new Label("✅ Resolución:");
            lblResTitulo.getStyleClass().add("resolucion-titulo");
            Label lblResCuerpo = new Label(incidencia.getResolucion());
            lblResCuerpo.setWrapText(true);
            lblResCuerpo.getStyleClass().add("resolucion-cuerpo");
            resolucionBox.getChildren().addAll(lblResTitulo, lblResCuerpo);
            
            Button btnBorrar = new Button("Borrar Ticket / Archivar");
            btnBorrar.setMaxWidth(Double.MAX_VALUE);
            btnBorrar.getStyleClass().addAll("action-button", "danger");
            btnBorrar.setOnAction(e -> onEliminarCallback.accept(incidencia.getId()));
            
            footerContainer.getChildren().addAll(resolucionBox, btnBorrar);
        }

        // Imagen
        if (incidencia.getImagenUrl() != null && !incidencia.getImagenUrl().isEmpty()) {
            try {
                String ruta = incidencia.getImagenUrl();
                Image img = null;
                if (ruta.startsWith("http")) {
                    img = new Image(ruta, 110, 110, true, true, true);
                } else {
                    File imgFile = new File(ruta);
                    if (imgFile.exists()) {
                        img = new Image(imgFile.toURI().toString(), 110, 110, true, true, true);
                    }
                }
                
                if (img != null && !img.isError()) {
                    ImageView imgView = new ImageView(img);
                    imgView.getStyleClass().add("imagen-miniatura");
                    imgView.setFitWidth(110);
                    imgView.setFitHeight(110);
                    imgView.setPreserveRatio(true);
                    imgView.setOnMouseClicked(e -> onImageClickCallback.accept(incidencia.getImagenUrl()));
                    
                    Label imgLbl = new Label("📎 Adjunto");
                    imgLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");
                    imagenContainer.getChildren().add(new VBox(4, imgLbl, imgView));
                }
            } catch (Exception ignored) {
            }
        }

        renderStepperVisual(estadoActual, catColor);
    }

    private Label crearChip(String texto, String color) {
        Label chip = new Label(texto);
        chip.setStyle(
                "-fx-background-color: " + hexToRgba(color, 0.12) + ";" +
                "-fx-text-fill: " + color + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 4 10;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: " + hexToRgba(color, 0.3) + ";" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 1;"
        );
        return chip;
    }

    private void renderStepperVisual(EstadoIncidencia estadoActual, String accentColor) {
        String estadoString = estadoActual == null ? "NO LEIDO" : estadoActual.getDbValue();
        String[] pasos = { "LEIDO", "EN REVISION", "ACABADO" };
        String[] etiquetas = { "Leído", "En Revisión", "Acabado" };
        boolean passed = true;

        if ("NO LEIDO".equalsIgnoreCase(estadoString)) passed = false;

        for (int i = 0; i < pasos.length; i++) {
            boolean isActive = pasos[i].equalsIgnoreCase(estadoString);
            boolean isDone = passed && !isActive;

            StackPane circulo = new StackPane();
            circulo.setMinSize(22, 22);
            circulo.setMaxSize(22, 22);

            if (isActive) {
                circulo.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 11; -fx-effect: dropshadow(gaussian," + hexToRgba(accentColor, 0.5) + ",8,0,0,0);");
                Label check = new Label("●");
                check.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
                circulo.getChildren().add(check);
            } else if (isDone) {
                circulo.setStyle("-fx-background-color: " + hexToRgba(accentColor, 0.3) + "; -fx-background-radius: 11; -fx-border-color: " + accentColor + "; -fx-border-radius: 11; -fx-border-width: 1;");
                Label check = new Label("✓");
                check.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 10px; -fx-font-weight: bold;");
                circulo.getChildren().add(check);
            } else {
                circulo.getStyleClass().add("stepper-circle-inactive");
            }

            Label lbl = new Label(etiquetas[i]);
            lbl.setStyle("-fx-font-size: 10px; -fx-padding: 0 0 0 4;");
            if (isActive) {
                lbl.setStyle(lbl.getStyle() + "-fx-text-fill: " + accentColor + "; -fx-font-weight: bold;");
            } else if (isDone) {
                lbl.getStyleClass().add("stepper-label-done");
            } else {
                lbl.getStyleClass().add("stepper-label-inactive");
            }

            VBox paso = new VBox(3, circulo, lbl);
            paso.setAlignment(Pos.CENTER);
            stepperContainer.getChildren().add(paso);

            if (i < pasos.length - 1) {
                Separator linea = new Separator();
                linea.setPrefWidth(40);
                if (isDone) {
                    linea.setStyle("-fx-background-color: " + accentColor + "; -fx-padding: 0 6;");
                } else {
                    linea.getStyleClass().add("stepper-line-inactive");
                    linea.setStyle("-fx-padding: 0 6;");
                }
                HBox lineBox = new HBox(linea);
                lineBox.setAlignment(Pos.CENTER);
                lineBox.setPrefHeight(22);
                lineBox.setStyle("-fx-padding: 0 4;");
                stepperContainer.getChildren().add(lineBox);
            }

            if (isActive) passed = false;
        }
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

    // Helper functions from the original controller
    private String getCategoryColor(String categoria) {
        if (categoria == null) return "#3b82f6";
        return switch (categoria.toLowerCase()) {
            case "hardware" -> "#ef4444";
            case "software" -> "#3b82f6";
            case "conectividad" -> "#eab308";
            case "mobiliario" -> "#a855f7";
            default -> "#64748b";
        };
    }

    private String getCategoryIcon(String categoria) {
        if (categoria == null) return "📌";
        return switch (categoria.toLowerCase()) {
            case "hardware" -> "💻";
            case "software" -> "⚙️";
            case "conectividad" -> "🌐";
            case "mobiliario" -> "🪑";
            default -> "📌";
        };
    }

    private String getEstadoEmoji(String estado) {
        if (estado == null) return "🕒";
        return switch (estado.toUpperCase()) {
            case "LEIDO" -> "👀";
            case "EN REVISION" -> "🔧";
            case "ACABADO" -> "✅";
            default -> "🕒";
        };
    }

    private String getEstadoClass(String estado) {
        if (estado == null) return "badge-noleido";
        return switch (estado.toUpperCase()) {
            case "LEIDO" -> "badge-leido";
            case "EN REVISION" -> "badge-revision";
            case "ACABADO" -> "badge-acabado";
            default -> "badge-noleido";
        };
    }
}
