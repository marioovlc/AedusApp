package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class MisionesDAO {

    public MisionesDAO() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS misiones_diarias (" +
                "usuario_id UUID NOT NULL, " +
                "tipo_mision VARCHAR(50) NOT NULL, " +
                "fecha_completada DATE NOT NULL, " +
                "PRIMARY KEY (usuario_id, tipo_mision, fecha_completada)" +
                ")";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla 'misiones_diarias' verificada/creada con éxito.");
        } catch (Exception e) {
            System.err.println("Error creando tabla misiones_diarias: " + e.getMessage());
        }
    }

    /**
     * Intenta registrar una misión diaria. Si ya existe para la fecha de hoy, devuelve false.
     * Si no existe, la inserta, suma los AeduCoins al usuario y devuelve true.
     */
    public boolean registrarMisionDiaria(String usuarioId, String tipoMision, int recompensaAedus) {
        String sqlInsert = "INSERT INTO misiones_diarias (usuario_id, tipo_mision, fecha_completada) VALUES (?::uuid, ?, CURRENT_DATE)";
        boolean misionRegistrada = false;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sqlInsert)) {
             
            pstmt.setString(1, usuarioId);
            pstmt.setString(2, tipoMision);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                misionRegistrada = true;
            }
        } catch (Exception e) {
            // El constraint de PRIMARY KEY lanzará una excepción si ya se ha completado hoy. Esto es seguro y lo usamos como flujo normal.
            return false;
        }
        
        // Si llegamos a insertar, procedemos a dar la recompensa
        if (misionRegistrada && recompensaAedus > 0) {
            darRecompensaAedus(usuarioId, recompensaAedus);
        }
        
        return misionRegistrada;
    }

    private void darRecompensaAedus(String usuarioId, int aedus) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        try {
            com.example.aedusapp.models.Usuario u = usuarioDAO.getUserById(usuarioId);
            if (u != null) {
                int nuevasMonedas = u.getAeducoins() + aedus;
                usuarioDAO.updateUserAeduCoins(usuarioId, nuevasMonedas);
                System.out.println("Misión cumplida: Otorgados " + aedus + " AeduCoins a " + u.getNombre());
            }
        } catch (Exception ex) {
            System.err.println("Error al otorgar recompensa de AedusCoins: " + ex.getMessage());
        }
    }
}
