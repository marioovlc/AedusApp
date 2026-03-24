package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;
import com.example.aedusapp.database.config.DatabaseHelper;
import com.example.aedusapp.exceptions.DatabaseException;
import com.example.aedusapp.models.Achievement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.UUID;

public class AchievementDAO {
    private static final Logger logger = LoggerFactory.getLogger(AchievementDAO.class);

    // --- SQL CONSTANTS ---
    private static final String CREATE_TABLE_ACHIEVEMENT = 
        "CREATE TABLE IF NOT EXISTS neon_auth.achievement (" +
        "id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
        "title VARCHAR(255) NOT NULL," +
        "description TEXT," +
        "reward INTEGER DEFAULT 0," +
        "icon_path VARCHAR(255))";

    private static final String CREATE_TABLE_USER_ACHIEVEMENT = 
        "CREATE TABLE IF NOT EXISTS neon_auth.user_achievement (" +
        "user_id UUID REFERENCES neon_auth.user(id) ON DELETE CASCADE," +
        "achievement_id UUID REFERENCES neon_auth.achievement(id) ON DELETE CASCADE," +
        "unlocked_at TIMESTAMP DEFAULT NOW()," +
        "PRIMARY KEY (user_id, achievement_id))";

    private static final String CHECK_ACHIEVEMENTS_EXIST = "SELECT COUNT(*) FROM neon_auth.achievement";
    private static final String INSERT_ACHIEVEMENT = "INSERT INTO neon_auth.achievement (title, description, reward) VALUES (?, ?, ?)";
    
    private static final String FIND_ACHIEVEMENT_BY_TITLE = "SELECT id, reward FROM neon_auth.achievement WHERE title = ?";
    private static final String GRANT_ACHIEVEMENT = "INSERT INTO neon_auth.user_achievement (user_id, achievement_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
    private static final String UPDATE_USER_COINS = "UPDATE neon_auth.user SET aeducoins = aeducoins + ? WHERE id = ?";
    
    private static final String GET_USER_ACHIEVEMENTS = "SELECT a.* FROM neon_auth.achievement a " +
                     "JOIN neon_auth.user_achievement ua ON a.id = ua.achievement_id " +
                     "WHERE ua.user_id = ?";

    private static final DatabaseHelper.RowMapper<Achievement> ACHIEVEMENT_MAPPER = rs -> new Achievement(
            rs.getString("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getInt("reward"),
            rs.getString("icon_path")
    );

    public void initAchievementTables() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(CREATE_TABLE_ACHIEVEMENT);
            stmt.executeUpdate(CREATE_TABLE_USER_ACHIEVEMENT);
            insertDefaultAchievements(conn);
            logger.info("Tablas de logros inicializadas.");

        } catch (SQLException e) {
            String msg = "Error inicializando tablas de logros";
            logger.error(msg, e);
            throw new DatabaseException(msg, e);
        }
    }

    private void insertDefaultAchievements(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(CHECK_ACHIEVEMENTS_EXIST)) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement pst = conn.prepareStatement(INSERT_ACHIEVEMENT)) {
                    pst.setString(1, "Primer Paso");
                    pst.setString(2, "Has creado tu primera incidencia.");
                    pst.setInt(3, 50);
                    pst.addBatch();

                    pst.setString(1, "Solucionador");
                    pst.setString(2, "Has cerrado 5 incidencias.");
                    pst.setInt(3, 200);
                    pst.addBatch();

                    pst.setString(1, "Colaborador");
                    pst.setString(2, "Has escrito tu primer comentario.");
                    pst.setInt(3, 30);
                    pst.addBatch();

                    pst.executeBatch();
                }
            }
        }
    }

    public boolean grantAchievement(String userId, String achievementTitle) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement findStmt = conn.prepareStatement(FIND_ACHIEVEMENT_BY_TITLE)) {
                findStmt.setString(1, achievementTitle);
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        UUID achievementId = (UUID) rs.getObject("id");
                        int reward = rs.getInt("reward");

                        try (PreparedStatement grantStmt = conn.prepareStatement(GRANT_ACHIEVEMENT)) {
                            grantStmt.setObject(1, UUID.fromString(userId));
                            grantStmt.setObject(2, achievementId);
                            int rows = grantStmt.executeUpdate();

                            if (rows > 0) { // Nuevo logro para el user
                                try (PreparedStatement coinStmt = conn.prepareStatement(UPDATE_USER_COINS)) {
                                    coinStmt.setInt(1, reward);
                                    coinStmt.setObject(2, UUID.fromString(userId));
                                    coinStmt.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { logger.error("Error rollback", ex); }
            }
            throw new DatabaseException("Error otorgando el logro: " + achievementTitle, e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { logger.error("Error close", e); }
            }
        }
    }

    public List<Achievement> getUserAchievements(String userId) {
        return DatabaseHelper.queryForList(GET_USER_ACHIEVEMENTS, ACHIEVEMENT_MAPPER, UUID.fromString(userId));
    }
}
