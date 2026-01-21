package com.example.aedusapp.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Clase utilitaria para conectar con la base de datos MySQL
public class DBConnection {
    private static Connection connection;

    // Método estático para obtener la conexión (Singleton simple)
    public static Connection getConnection() {
        try {
            // 1. Cargar el "driver" (el traductor entre Java y MySQL)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Establecer la conexión con la URL, usuario y contraseña
            // Asegúrate de que tu base de datos se llame 'aedusdb' y las credenciales sean
            // correctas
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/aedusdb",
                    "root",
                    "root");

            System.out.println("Conexión exitosa a la base de datos.");

        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver de MySQL.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error al conectar con la base de datos.");
            e.printStackTrace();
        }

        return connection;
    }
}
