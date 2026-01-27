package com.example.aedusapp.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseInitializer {
    public static void main(String[] args) {

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.err.println("CRÍTICO: Fallo al establecer conexión con la base de datos.");
                return;
            }

            InputStream is = DatabaseInitializer.class.getResourceAsStream("/database_schema.sql");
            if (is == null) {
                System.err.println("ERROR: ¡No se encontró /database_schema.sql en los recursos!");
                return;
            }

            String sqlScript = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));

            String[] statements = sqlScript.split(";");

            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    if (sql.trim().isEmpty())
                        continue;

                    try {
                        stmt.execute(sql);
                    } catch (Exception e) {
                        // Silenced error/warning output
                    }
                }
            }

            System.out.println("Base de datos conectada!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
