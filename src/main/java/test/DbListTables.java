package test;

import com.example.aedusapp.database.config.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbListTables {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== CURRENT SEARCH PATH ===");
            try (ResultSet rs = stmt.executeQuery("SHOW search_path")) {
                if (rs.next()) System.out.println(rs.getString(1));
            }
            
            System.out.println("\n=== TABLES IN DB ===");
            try (ResultSet rs = stmt.executeQuery("SELECT table_schema, table_name FROM information_schema.tables WHERE table_schema IN ('public', 'gestion_incidencias', 'neon_auth')")) {
                while (rs.next()) {
                    System.out.println(rs.getString("table_schema") + "." + rs.getString("table_name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
