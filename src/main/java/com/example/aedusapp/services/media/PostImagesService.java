package com.example.aedusapp.services.media;

import java.io.File;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostImagesService {

    // API KEY for IMGBB

    /**
     * Sube una imagen a postimages.org simulando la llamada AJAX que hace la web
     * y devuelve la URL directa de la imagen subida. En caso de error, retorna null.
     */
    public static String uploadImage(File file) {
        HttpURLConnection httpConn = null;

        try {
            // Cloudinary Credentials loaded from global AppConfig
            String cloudName = com.example.aedusapp.utils.config.AppConfig.getCloudinaryCloudName();
            String apiKey = com.example.aedusapp.utils.config.AppConfig.getCloudinaryApiKey();
            String apiSecret = com.example.aedusapp.utils.config.AppConfig.getCloudinaryApiSecret();
            
            // 1. Generate timestamp and signature required by Cloudinary for authenticated uploads
            long timestamp = System.currentTimeMillis() / 1000L;
            String stringToSign = "timestamp=" + timestamp + apiSecret;
            
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(stringToSign.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            String signature = sb.toString();

            // 2. Prepare HTTP connection to Cloudinary generic image upload endpoint
            String cloudinaryUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
            URL url = java.net.URI.create(cloudinaryUrl).toURL();
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setUseCaches(false);
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            httpConn.setRequestMethod("POST");
            
            // 3. Convert image to a safe Data-URI (base64) so we don't need unstable Multipart Boundaries
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
            String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);
            String mimeType = java.net.URLConnection.guessContentTypeFromName(file.getName());
            if (mimeType == null) mimeType = "image/jpeg";
            String fileDataUri = "data:" + mimeType + ";base64," + base64Image;

            // 4. Construct application/x-www-form-urlencoded body
            String postData = "api_key=" + java.net.URLEncoder.encode(apiKey, "UTF-8") +
                              "&timestamp=" + java.net.URLEncoder.encode(String.valueOf(timestamp), "UTF-8") +
                              "&signature=" + java.net.URLEncoder.encode(signature, "UTF-8") +
                              "&file=" + java.net.URLEncoder.encode(fileDataUri, "UTF-8");
            
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Content-Length", String.valueOf(postData.length()));
            httpConn.setRequestProperty("User-Agent", "AedusApp/1.0");

            // 5. Send payload
            try (OutputStream os = httpConn.getOutputStream()) {
                os.write(postData.getBytes("UTF-8"));
                os.flush();
            }

            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                String response = "";
                try (java.util.Scanner scanner = new java.util.Scanner(httpConn.getInputStream(), "UTF-8").useDelimiter("\\A")) {
                    response = scanner.hasNext() ? scanner.next() : "";
                }
                
                // Cloudinary returns a secure_url in its JSON response: {"secure_url":"https://res.cloudinary.com/..."}
                if (response.contains("\"secure_url\"")) {
                    String[] parts = response.split("\"secure_url\":\"");
                    if (parts.length > 1) {
                         String extractedUrl = parts[1].split("\"")[0].replace("\\/", "/");
                         return extractedUrl;
                    }
                }
                return response;
            } else {
                // If 400 or 401, print the error stream for detailed Cloudinary debug logic
                String errorInfo = "";
                java.io.InputStream errorStream = httpConn.getErrorStream();
                if (errorStream != null) {
                    try (java.util.Scanner scanner = new java.util.Scanner(errorStream, "UTF-8").useDelimiter("\\A")) {
                        errorInfo = scanner.hasNext() ? scanner.next() : "";
                    }
                }
                System.err.println("Error from Cloudinary API (" + status + "): " + errorInfo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return null;
    }

}
