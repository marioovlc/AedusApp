package com.example.aedusapp.utils.hub;

import com.example.aedusapp.models.Usuario;
import com.example.aedusapp.services.hub.IConnectHubService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Encapsula la lógica de presencia (pings) para no saturar el controlador principal.
 */
public class PresenceManager {
    private final IConnectHubService hubService;
    private final Set<String> usuariosActivos = new HashSet<>();
    private Timeline presenceTimeline;
    private final Consumer<Set<String>> onUpdate;

    public PresenceManager(IConnectHubService hubService, Consumer<Set<String>> onUpdate) {
        this.hubService = hubService;
        this.onUpdate = onUpdate;
    }

    public void start(Usuario usuarioActual) {
        if (presenceTimeline != null) presenceTimeline.stop();

        presenceTimeline = new Timeline(new KeyFrame(Duration.seconds(20), e -> {
            if (usuarioActual != null) {
                Task<Void> pingTask = new Task<>() {
                    @Override
                    protected Void call() {
                        hubService.updateUserPresence(usuarioActual.getId());
                        List<String> activos = hubService.getRecentlyActiveUsers(45);
                        Platform.runLater(() -> {
                            usuariosActivos.clear();
                            usuariosActivos.addAll(activos);
                            usuariosActivos.add("system");
                            onUpdate.accept(usuariosActivos);
                        });
                        return null;
                    }
                };
                com.example.aedusapp.utils.ConcurrencyManager.submit(pingTask);
            }
        }));
        presenceTimeline.setCycleCount(Timeline.INDEFINITE);
        presenceTimeline.play();

        // Primer ping inmediato
        com.example.aedusapp.utils.ConcurrencyManager.submit(() -> {
            hubService.initPresenceSystem();
            hubService.updateUserPresence(usuarioActual.getId());
        });
    }

    public void stop() {
        if (presenceTimeline != null) presenceTimeline.stop();
    }

    public Set<String> getUsuariosActivos() {
        return usuariosActivos;
    }
}
