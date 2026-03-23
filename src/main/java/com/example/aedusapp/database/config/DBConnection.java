package com.example.aedusapp.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.example.aedusapp.utils.config.AppConfig;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection {

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(AppConfig.getDbUrl());
            config.setUsername(AppConfig.getDbUser());
            config.setPassword(AppConfig.getDbPass());
            
            // Optimizations for PostgreSQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            // Neon / PostgreSQL Pool limits
            config.setMaximumPoolSize(AppConfig.getDbPoolMax());
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000); // 30 seconds

            // Note: NeonDB uses an external PgBouncer in transaction-mode (`-pooler` URL).
            // A static `setConnectionInitSql` search_path gets scrambled by pooler interleaving.
            // We must enforce search_path explicitly on every connection retrieval.

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP Database Pool initialized successfully.");
        } catch (Exception e) {
            logger.error("Fatal Error: Could not initialize database connection pool.", e);
        }
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing HikariCP Database Pool...");
            dataSource.close();
            logger.info("HikariCP Database Pool closed successfully.");
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database pool is not initialized.");
        }
        Connection conn = dataSource.getConnection();
        
        // Anti-Pooler safeguard: Enforce namespace manually to survive PgBouncer session stripping
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + AppConfig.getDbSchema());
        }
        
        return conn;
    }
}
