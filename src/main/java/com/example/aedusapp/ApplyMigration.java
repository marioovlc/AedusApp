package com.example.aedusapp;

import com.example.aedusapp.database.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class ApplyMigration {
    public static void main(String[] args) {
        System.out.println("Aplicando migración de base de datos...");

        try {
            Connection conn = DBConnection.getConnection();

            if (conn != null) {
                String sql = "ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS imagen_ruta VARCHAR(500)";

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    System.out.println("✓ Migración completada: columna 'imagen_ruta' agregada");
                    System.out.println("✓ Ahora puedes ejecutar la aplicación normalmente");
                }
            } else {
                System.err.println("✗ Error: No se pudo conectar a la base de datos");
            }

        } catch (Exception e) {
            System.err.println("✗ Error al aplicar migración:");
            e.printStackTrace();
        }
    }
}
