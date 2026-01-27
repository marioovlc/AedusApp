package com.example.aedusapp.database;

import com.example.aedusapp.models.Incidencia;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IncidenciaDAO {

    public boolean crearIncidencia(Incidencia incidencia) {
        // Asignamos estado inicial 'NO LEIDO'
        String sql = "INSERT INTO incidencias (titulo, descripcion, usuario_id, aula_id, categoria_id, aula_tipo, estado_id, fecha_creacion, imagen_ruta) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, (SELECT id FROM estados WHERE nombre = 'NO LEIDO'), NOW(), ?)";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Forzar esquema

            stmt.setString(1, incidencia.getTitulo());
            stmt.setString(2, incidencia.getDescripcion());
            stmt.setInt(3, incidencia.getUsuarioId());
            stmt.setInt(4, incidencia.getAulaId());
            stmt.setInt(5, incidencia.getCategoriaId());
            stmt.setString(6, incidencia.getAulaTipo());
            stmt.setString(7, incidencia.getImagenRuta()); // Puede ser null

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Incidencia> obtenerIncidenciasPorUsuarioPaginado(int usuarioId, int limit, int offset) {
        List<Incidencia> incidencias = new ArrayList<>();
        String sql = "SELECT i.id, i.titulo, i.descripcion, i.fecha_creacion, i.imagen_ruta, i.aula_tipo, " +
                "e.nombre as estado_nombre, c.nombre as categoria_nombre, a.nombre as aula_nombre " +
                "FROM incidencias i " +
                "JOIN estados e ON i.estado_id = e.id " +
                "JOIN categorias c ON i.categoria_id = c.id " +
                "JOIN aulas a ON i.aula_id = a.id " +
                "WHERE i.usuario_id = ? " +
                "ORDER BY i.fecha_creacion DESC " +
                "LIMIT ? OFFSET ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Incidencia inc = new Incidencia();
                inc.setId(rs.getInt("id"));
                inc.setTitulo(rs.getString("titulo"));
                inc.setDescripcion(rs.getString("descripcion"));
                inc.setEstado(rs.getString("estado_nombre"));
                inc.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                inc.setImagenRuta(rs.getString("imagen_ruta"));
                inc.setCategoriaNombre(rs.getString("categoria_nombre"));
                inc.setAulaNombre(rs.getString("aula_nombre"));
                inc.setAulaTipo(rs.getString("aula_tipo"));

                incidencias.add(inc);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return incidencias;
    }

    // Método para obtener todas (para admin)
    public List<Incidencia> obtenerTodasIncidencias() {
        List<Incidencia> incidencias = new ArrayList<>();
        String sql = "SELECT i.id, i.titulo, i.descripcion, i.fecha_creacion, i.imagen_ruta, " +
                "e.nombre as estado_nombre, u.nombre as usuario_nombre " +
                "FROM incidencias i " +
                "JOIN estados e ON i.estado_id = e.id " +
                "JOIN usuarios u ON i.usuario_id = u.id " +
                "ORDER BY " +
                "CASE WHEN e.nombre = 'NO LEIDO' THEN 0 ELSE 1 END, " + // Priorizar NO LEIDO
                "i.fecha_creacion DESC";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Incidencia inc = new Incidencia();
                inc.setId(rs.getInt("id"));
                inc.setTitulo(rs.getString("titulo"));
                inc.setDescripcion(rs.getString("descripcion"));
                inc.setEstado(rs.getString("estado_nombre"));
                inc.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                inc.setImagenRuta(rs.getString("imagen_ruta"));
                inc.setCreadorNombre(rs.getString("usuario_nombre"));
                incidencias.add(inc);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return incidencias;
    }

    public boolean actualizarEstado(int incidenciaId, String nuevoEstado) {
        String sql = "UPDATE incidencias SET estado_id = (SELECT id FROM estados WHERE nombre = ?) WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, incidenciaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarResolucion(int incidenciaId, String resolucion, String nuevoEstado) {
        String sql = "UPDATE incidencias SET resolucion = ?, estado_id = (SELECT id FROM estados WHERE nombre = ?) WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, resolucion);
            stmt.setString(2, nuevoEstado);
            stmt.setInt(3, incidenciaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Método para eliminar una incidencia
    public boolean eliminarIncidencia(int incidenciaId) {
        String sql = "DELETE FROM incidencias WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, incidenciaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
