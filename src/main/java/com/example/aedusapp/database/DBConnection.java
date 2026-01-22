package com.example.aedusapp.database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static Connection connection;

    public static Connection getConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            Class.forName("org.postgresql.Driver");

            // Configuración de conexión para Neon
            String url = "jdbc:postgresql://ep-mute-frog-agiqzzew-pooler.c-2.eu-central-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require&currentSchema=gestion_incidencias";
            String user = "neondb_owner";
            String pass = "npg_r9CQd1vZMIVD";

            connection = DriverManager.getConnection(url, user, pass);

            // Forzar el esquema explícitamente por si el parámetro de URL falla
            try (java.sql.Statement callbackStmt = connection.createStatement()) {
                callbackStmt.execute("SET search_path TO gestion_incidencias");
            }

            System.out.println("Conexión exitosa a la base de datos Neon.");

        } catch (Exception e) {
            System.err.println("Error crítico de conexión:");
            e.printStackTrace();
        }
        return connection;
    }
}
