package com.example.aedusapp.database;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Script para aplicar la migración de la base de datos
 * Agrega la columna imagen_ruta a la tabla incidencias
 */
public class MigrateDatabase {
    public static void main(String[] args) {
        try {
            Connection conn = DBConnection.getConnection();

            if (conn != null) {
                // Migración 1: Columna imagen_ruta
                String sqlImg = "ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS imagen_ruta VARCHAR(500)";

                // Migración 2: Tabla logs
                String sqlLogs = "CREATE TABLE IF NOT EXISTS logs (" +
                    "id SERIAL PRIMARY KEY, " +
                    "usuario_id INTEGER REFERENCES usuarios(id) ON DELETE SET NULL, " +
                    "accion VARCHAR(50) NOT NULL, " +
                    "categoria VARCHAR(30) NOT NULL, " +
                    "descripcion TEXT NOT NULL, " +
                    "ip_address VARCHAR(45), " +
                    "fecha_creacion TIMESTAMP DEFAULT NOW())";

                // Indices for logs
                String sqlIdx1 = "CREATE INDEX IF NOT EXISTS idx_logs_categoria ON logs(categoria)";
                String sqlIdx2 = "CREATE INDEX IF NOT EXISTS idx_logs_usuario_id ON logs(usuario_id)";
                String sqlIdx3 = "CREATE INDEX IF NOT EXISTS idx_logs_fecha ON logs(fecha_creacion DESC)";
                
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sqlImg);
                    System.out.println("✓ Migración imagen_ruta verificada");
                    
                    stmt.execute(sqlLogs);
                    stmt.execute(sqlIdx1);
                    stmt.execute(sqlIdx2);
                    stmt.execute(sqlIdx3);
                    System.out.println("✓ Tabla logs y sus índices verificados");
                }
            } else {
                System.err.println("✗ No se pudo conectar a la base de datos");
            }

        } catch (Exception e) {
            System.err.println("✗ Error en la migración:");
            e.printStackTrace();
        }
    }
}
