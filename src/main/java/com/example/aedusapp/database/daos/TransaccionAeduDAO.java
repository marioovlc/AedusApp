package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;

import java.sql.*;

public class TransaccionAeduDAO {

    public boolean registerTransaction(String usuarioId, int cantidad, String motivo) {
        String sqlInsert = "INSERT INTO transacciones_aeducoins (usuario_id, cantidad, motivo, fecha) VALUES (?::uuid, ?, ?, NOW())";
        String sqlUpdateUser = "UPDATE usuarios SET aeducoins = aeducoins + ? WHERE id = ?::uuid";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert);
                 PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdateUser)) {
                
                stmtInsert.setString(1, usuarioId);
                stmtInsert.setInt(2, cantidad);
                stmtInsert.setString(3, motivo);
                stmtInsert.executeUpdate();

                stmtUpdate.setInt(1, cantidad);
                stmtUpdate.setString(2, usuarioId);
                stmtUpdate.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
