package com.example.aedusapp.utils;

import com.example.aedusapp.database.daos.*;
import com.example.aedusapp.services.hub.*;
import com.example.aedusapp.utils.ui.GlobalExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centra la inicialización del sistema, registro de dependencias y configuraciones globales.
 * Evita el "hardcoding" excesivo en MainApp.
 */
public class AppInitializer {
    private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

    public static void init() {
        logger.info("Inicializando componentes del sistema...");

        // 1. Configuración de Excepciones Globales
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());

        // 2. Registro de DAOs (Inyección de Dependencias)
        DependencyInjector.register(IMensajeDAO.class, new MensajeDAO(new AchievementDAO()));
        DependencyInjector.register(IncidenciaDAO.class, new IncidenciaDAO());
        DependencyInjector.register(UsuarioDAO.class, new UsuarioDAO());
        DependencyInjector.register(ConocimientoDAO.class, new ConocimientoDAO());
        DependencyInjector.register(MisionesDAO.class, new MisionesDAO());
        
        // 3. Registro de Servicios
        DependencyInjector.register(IConnectHubService.class, new ConnectHubService());

        logger.info("Sistema inicializado correctamente.");
    }
}
