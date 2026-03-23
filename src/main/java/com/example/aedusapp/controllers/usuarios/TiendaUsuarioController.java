package com.example.aedusapp.controllers.usuarios;

import com.example.aedusapp.database.daos.TiendaDAO;
import com.example.aedusapp.database.daos.TransaccionAeduDAO;
import com.example.aedusapp.models.TiendaItem;
import com.example.aedusapp.models.Usuario;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.List;

public class TiendaUsuarioController {

    @FXML private VBox catalogoContainer;
    @FXML private Label lblSaldo;
    @FXML private Label lblEstado;

    private final TiendaDAO tiendaDAO = new TiendaDAO();
    private final TransaccionAeduDAO transaccionDAO = new TransaccionAeduDAO();

    private Usuario usuarioActual;

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        actualizarSaldo();
        cargarCatalogo();
    }

    private void actualizarSaldo() {
        if (usuarioActual != null) {
            lblSaldo.setText(String.valueOf(usuarioActual.getAeducoins()));
            lblSaldo.setGraphic(createCoinIcon(16, "#fcd34d"));
        }
    }

    private void cargarCatalogo() {
        catalogoContainer.getChildren().clear();
        List<TiendaItem> items = tiendaDAO.getAllItems();

        if (items.isEmpty()) {
            Label empty = new Label("📭 La tienda está vacía por el momento. ¡Vuelve pronto!");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-padding: 40;");
            empty.setWrapText(true);
            catalogoContainer.getChildren().add(empty);
            return;
        }

        for (TiendaItem item : items) {
            catalogoContainer.getChildren().add(crearTarjetaProducto(item));
        }
    }

    private HBox crearTarjetaProducto(TiendaItem item) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.getStyleClass().add("user-list-row");

        // Hover se delega a las hojas de estilo mediante :hover en .user-list-row

        // Icono del producto
        String icono = getIcono(item.getDescripcion());
        Label iconLbl = new Label(icono);
        iconLbl.setStyle("-fx-font-size: 32px;");

        // Info
        VBox info = new VBox(4);
        Label nameLbl = new Label(item.getNombre());
        nameLbl.getStyleClass().add("user-list-name");
        Label descLbl = new Label(getDescripcionLimpia(item.getDescripcion()));
        descLbl.getStyleClass().add("user-list-email");
        descLbl.setWrapText(true);
        info.getChildren().addAll(nameLbl, descLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Precio + botón
        VBox rightPanel = new VBox(8);
        rightPanel.setAlignment(Pos.CENTER);

        Label lblCoste = new Label(String.valueOf(item.getCoste()));
        lblCoste.setGraphic(createCoinIcon(14, "#fcd34d"));
        lblCoste.setStyle("-fx-text-fill: #fcd34d; -fx-font-weight: bold; -fx-font-size: 16px;");

        boolean puedeComprar = usuarioActual != null && usuarioActual.getAeducoins() >= item.getCoste();

        Button btnComprar = new Button(puedeComprar ? "🛒 Comprar" : "💸 Sin saldo");
        
        if (puedeComprar) {
            btnComprar.getStyleClass().addAll("action-button", "success");
        } else {
            btnComprar.getStyleClass().addAll("action-button");
            btnComprar.setDisable(true);
        }

        if (puedeComprar) {
            btnComprar.setOnAction(e -> handleComprar(item));
        }

        rightPanel.getChildren().addAll(lblCoste, btnComprar);
        card.getChildren().addAll(iconLbl, info, rightPanel);
        return card;
    }

    private void handleComprar(TiendaItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar compra");
        confirm.setHeaderText("¿Comprar \"" + item.getNombre() + "\"?");
        confirm.setContentText("Se descontarán " + item.getCoste() + " AeduCoins de tu saldo.\n" +
                "Saldo actual: " + usuarioActual.getAeducoins() + " AC");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                int nuevoSaldo = usuarioActual.getAeducoins() - item.getCoste();
                // TransaccionAeduDAO registers the transaction AND updates aeducoins in the DB atomically
                transaccionDAO.registerTransaction(usuarioActual.getId(), -item.getCoste(), "Compra: " + item.getNombre());
                // Update the local object for the current session (Binding updates the SIdebar UI automatically)
                usuarioActual.setAeducoins(nuevoSaldo);

                mostrarExito("¡Compra realizada! Has canjeado \"" + item.getNombre() + "\". Nuevo saldo: " + nuevoSaldo + " AC");
                actualizarSaldo();
                cargarCatalogo(); // Refresh to update button states
            }
        });
    }

    private void mostrarExito(String msg) {
        lblEstado.setText("✅ " + msg);
        lblEstado.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px;");
    }

    private String getIcono(String desc) {
        if (desc != null && desc.startsWith("ico:")) {
            int semi = desc.indexOf(';');
            return semi >= 0 ? desc.substring(4, semi) : desc.substring(4);
        }
        return "🎁";
    }

    private String getDescripcionLimpia(String desc) {
        if (desc == null) return "";
        if (desc.startsWith("ico:")) {
            int semi = desc.indexOf(';');
            return semi >= 0 ? desc.substring(semi + 1) : "";
        }
        return desc;
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
