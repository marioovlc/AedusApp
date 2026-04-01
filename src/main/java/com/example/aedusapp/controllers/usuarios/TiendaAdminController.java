package com.example.aedusapp.controllers.usuarios;

import com.example.aedusapp.database.daos.TiendaDAO;
import com.example.aedusapp.models.TiendaItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.prefs.Preferences;

public class TiendaAdminController {

    @FXML private TextField txtAdminTiendaIcono;
    @FXML private TextField txtAdminTiendaNombre;
    @FXML private TextField txtAdminTiendaCoste;
    @FXML private TextField txtAdminTiendaDesc;
    @FXML private TextField txtRecompensaTicket;
    @FXML private VBox listaAdminTienda;

    private static final String PREF_RECOMPENSA = "aedu.recompensa.ticket";
    private static final Preferences prefs = Preferences.userNodeForPackage(TiendaAdminController.class);

    private final TiendaDAO tiendaDAO = new TiendaDAO();
    private final ObservableList<TiendaItem> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Load saved reward value
        int saved = prefs.getInt(PREF_RECOMPENSA, 10);
        if (txtRecompensaTicket != null) {
            txtRecompensaTicket.setText(String.valueOf(saved));
        }
        cargarDatos();
    }

    /** Returns how many AeduCoins to award when a ticket is created. */
    public static int getRecompensaTicket() {
        return Preferences.userNodeForPackage(TiendaAdminController.class)
                .getInt(PREF_RECOMPENSA, 10);
    }

    @FXML
    private void handleGuardarRecompensa() {
        String val = txtRecompensaTicket.getText().trim();
        try {
            int recompensa = Integer.parseInt(val);
            if (recompensa < 0) throw new NumberFormatException();
            prefs.putInt(PREF_RECOMPENSA, recompensa);
            mostrarAlerta("Guardado", "Recompensa actualizada a " + recompensa + " AC por ticket.");
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Introduce un número entero positivo.");
        }
    }

    private void actualizarUI() {
        listaAdminTienda.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("📭 No hay productos en el catálogo todavía.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
            listaAdminTienda.getChildren().add(empty);
            return;
        }
        for (TiendaItem item : items) {
            HBox box = new HBox(12);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(12));
            box.getStyleClass().add("user-list-row");

            // Icon / emoji label
            String icono = (item.getDescripcion() != null && item.getDescripcion().startsWith("ico:"))
                    ? item.getDescripcion().substring(4).split(";")[0]
                    : "🎁";
            Label iconLbl = new Label(icono);
            iconLbl.setStyle("-fx-font-size: 22px;");

            VBox info = new VBox(2);
            Label nameLbl = new Label(item.getNombre());
            nameLbl.getStyleClass().add("user-list-name");
            Label descLbl = new Label(getDescWithoutIcon(item.getDescripcion()));
            descLbl.getStyleClass().add("user-list-email");
            info.getChildren().addAll(nameLbl, descLbl);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label costeLbl = new Label(String.valueOf(item.getCoste()));
            costeLbl.setGraphic(createCoinIcon(14, "#fcd34d"));
            costeLbl.setStyle("-fx-text-fill: #fcd34d; -fx-font-weight: bold; -fx-font-size: 12px;");

            Button btnDel = new Button("× Eliminar");
            btnDel.getStyleClass().addAll("action-button", "danger", "danger-button");
            btnDel.setOnAction(e -> eliminarItem(item));

            box.getChildren().addAll(iconLbl, info, costeLbl, btnDel);
            listaAdminTienda.getChildren().add(box);
        }
    }

    private String getDescWithoutIcon(String desc) {
        if (desc == null) return "";
        if (desc.startsWith("ico:")) {
            int semi = desc.indexOf(';');
            return semi >= 0 ? desc.substring(semi + 1) : "";
        }
        return desc;
    }

    private void cargarDatos() {
        items.setAll(tiendaDAO.getAllItems());
        actualizarUI();
    }

    @FXML
    private void handleAdminTiendaCrear() {
        String icono = txtAdminTiendaIcono.getText().trim();
        String nombre = txtAdminTiendaNombre.getText().trim();
        String costeStr = txtAdminTiendaCoste.getText().trim();
        String desc = txtAdminTiendaDesc.getText().trim();

        if (nombre.isEmpty() || costeStr.isEmpty()) {
            mostrarAlerta("Error", "Nombre y coste son obligatorios.");
            return;
        }

        try {
            int coste = Integer.parseInt(costeStr);
            // Encode icon in description: "ico:🎨;La descripción real"
            String descFinal = icono.isEmpty() ? desc : "ico:" + icono + ";" + desc;
            TiendaItem nuevo = new TiendaItem(0, nombre, coste, descFinal, "star", "#F2C94C");
            if (tiendaDAO.saveItem(nuevo)) {
                limpiarCampos();
                cargarDatos();
                mostrarAlerta("✅ Publicado", "Producto \"" + nombre + "\" añadido al catálogo.");
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El coste debe ser un número entero.");
        }
    }

    private void eliminarItem(TiendaItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar producto");
        confirm.setHeaderText("¿Eliminar \"" + item.getNombre() + "\"?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK && tiendaDAO.deleteItem(item.getId())) {
                cargarDatos();
            }
        });
    }

    private void limpiarCampos() {
        txtAdminTiendaIcono.clear();
        txtAdminTiendaNombre.clear();
        txtAdminTiendaCoste.clear();
        txtAdminTiendaDesc.clear();
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle(titulo);
        info.setHeaderText(null);
        info.setContentText(contenido);
        info.showAndWait();
    }

    private javafx.scene.layout.Region createCoinIcon(int size, String colorHex) {
        javafx.scene.layout.Region icon = new javafx.scene.layout.Region();
        icon.setStyle(
            "-fx-shape: 'M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm178 555h-46.9c-10.2 0-19.9-4.9-25.9-13.2L512 460.4 406.8 605.8c-6 8.3-15.6 13.2-25.9 13.2H334c-6.5 0-10.3-7.4-6.5-12.7l178-246c3.2-4.4 9.7-4.4 12.9 0l178 246c3.9 5.3.1 12.7-6.4 12.7z';" +
            "-fx-background-color: " + colorHex + ";" +
            "-fx-min-width: " + size + "px; -fx-min-height: " + size + "px;" +
            "-fx-max-width: " + size + "px; -fx-max-height: " + size + "px;"
        );
        return icon;
    }
}
