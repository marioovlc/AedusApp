package test;

import com.example.aedusapp.database.config.DBConnection;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;

public class DbListTables {
    public static void main(String[] args) {
        String testUrl = "https://res.cloudinary.com/dbdpkml2m/image/upload/v1775029269/oe15vz59gemmqt5v1mxe.png";
        System.out.println("Testing connection to: " + testUrl);
        try {
            URI uri = new URI(testUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            System.out.println("HTTP Response Code: " + responseCode);
            if (responseCode == 200) {
                try (InputStream in = conn.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int bytesRead = in.read(buffer);
                    System.out.println("Successfully read " + bytesRead + " bytes from the image.");
                }
            } else {
                System.out.println("Failed to load image. Response Message: " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred during connection test:");
            e.printStackTrace();
        }
    }
}
