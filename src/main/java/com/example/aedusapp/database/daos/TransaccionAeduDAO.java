package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransaccionAeduDAO {
    private static final Logger logger = LoggerFactory.getLogger(TransaccionAeduDAO.class);
    private static final String TABLE_TRANSACTIONS = "transacciones_aeducoins";
    private static final String TABLE_USERS = "usuarios";

    public boolean registerTransaction(String usuarioId, int cantidad, String motivo) {
        String sqlInsert = "INSERT INTO " + TABLE_TRANSACTIONS + " (usuario_id, cantidad, motivo, fecha) VALUES (?::uuid, ?, ?, NOW())";
        String sqlUpdateUser = "UPDATE " + TABLE_USERS + " SET aeducoins = aeducoins + ? WHERE id = ?::uuid";

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
                logger.error("Fallo al ejecutar transacción para usuario {} (rollback efectuado): {}", usuarioId, e.getMessage(), e);
            }
        } catch (SQLException e) {
            logger.error("Error SQL al establecer conexión o durante commit para usuario {}: {}", usuarioId, e.getMessage(), e);
        }
        return false;
    }
}
