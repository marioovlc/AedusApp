package com.example.aedusapp.database.config;

import com.example.aedusapp.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstracción de JDBC que elimina la necesidad de gestionar manualmente las conexiones,
 * sentencias (PreparedStatements) y bloques try-catch (Boilerplate) en cada DAO.
 */
public class DatabaseHelper {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);

    @FunctionalInterface
    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }

    /**
     * Ejecuta una consulta SELECT cuyo resultado es una lista de objetos Mapeados.
     */
    public static <T> List<T> queryForList(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            String errorMsg = "Error ejecutando queryForList en la Base de Datos: " + sql;
            logger.error(errorMsg, e);
            throw new DatabaseException(errorMsg, e);
        }
        return results;
    }

    /**
     * Ejecuta una consulta SELECT cuyo resultado es 1 solo objeto Mapeado.
     */
    public static <T> T queryForObject(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapper.mapRow(rs);
                }
            }
        } catch (SQLException e) {
            String errorMsg = "Error ejecutando queryForObject en la Base de Datos: " + sql;
            logger.error(errorMsg, e);
            throw new DatabaseException(errorMsg, e);
        }
        return null;
    }

    /**
     * Ejecuta instrucciones INSERT, UPDATE o DELETE y retorna si alguna fila fue afectada.
     */
    public static boolean executeUpdate(String sql, Object... params) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            setParameters(stmt, params);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            String errorMsg = "Error ejecutando executeUpdate en la Base de Datos: " + sql;
            logger.error(errorMsg, e);
            throw new DatabaseException(errorMsg, e);
        }
    }

    private static void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            // El driver JDBC de PostgreSQL asume los tipos correctamente
            // utilizando el setObject standard.
            stmt.setObject(i + 1, params[i]);
        }
    }
}
