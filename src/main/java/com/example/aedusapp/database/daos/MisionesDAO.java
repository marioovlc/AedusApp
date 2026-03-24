package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DatabaseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MisionesDAO {
    private static final Logger logger = LoggerFactory.getLogger(MisionesDAO.class);
    private static final String TABLE_NAME = "misiones_diarias";

    // El constructor ya NO crea la tabla. Eso lo hace DatabaseSetup al arrancar.

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "usuario_id UUID NOT NULL, " +
                "tipo_mision VARCHAR(50) NOT NULL, " +
                "fecha_completada DATE NOT NULL, " +
                "PRIMARY KEY (usuario_id, tipo_mision, fecha_completada)" +
                ")";
        DatabaseHelper.executeUpdate(sql);
        logger.info("Tabla '{}' verificada/creada con éxito.", TABLE_NAME);
    }

    /**
     * Intenta registrar una misión diaria. Si ya existe para la fecha de hoy, devuelve false.
     * Si no existe, la inserta, suma los AeduCoins al usuario y devuelve true.
     */
    public boolean registrarMisionDiaria(String usuarioId, String tipoMision, int recompensaAedus) {
        String sqlInsert = "INSERT INTO " + TABLE_NAME + " (usuario_id, tipo_mision, fecha_completada) " +
                "VALUES (?::uuid, ?, CURRENT_DATE) " +
                "ON CONFLICT (usuario_id, tipo_mision, fecha_completada) DO NOTHING";

        try {
            boolean misionRegistrada = DatabaseHelper.executeUpdate(sqlInsert, usuarioId, tipoMision);
            if (misionRegistrada && recompensaAedus > 0) {
                darRecompensaAedus(usuarioId, recompensaAedus);
            }
            return misionRegistrada;
        } catch (com.example.aedusapp.exceptions.DatabaseException e) {
            // El constraint de PRIMARY KEY lanza excepción si la misión ya fue completada hoy.
            // Esto es flujo normal, no un error real.
            logger.debug("Misión '{}' ya registrada hoy para usuario {}.", tipoMision, usuarioId);
            return false;
        }
    }

    private void darRecompensaAedus(String usuarioId, int aedus) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        try {
            com.example.aedusapp.models.Usuario u = usuarioDAO.getUserById(usuarioId);
            if (u != null) {
                int nuevasMonedas = u.getAeducoins() + aedus;
                usuarioDAO.updateUserAeduCoins(usuarioId, nuevasMonedas);
                logger.info("Misión cumplida: {} AeduCoins otorgados a '{}'.", aedus, u.getNombre());
            }
        } catch (Exception ex) {
            logger.error("Error al otorgar recompensa de AeduCoins a usuario {}", usuarioId, ex);
        }
    }
}

