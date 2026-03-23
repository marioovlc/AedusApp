package com.example.aedusapp.services.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AIService {

    public String askAI(String promptUsuario) {
        return askAI(promptUsuario, null);
    }

    public String askAI(String promptUsuario, String context) {
        String apiKey = com.example.aedusapp.utils.config.AppConfig.getAiApiKey();
        String url = com.example.aedusapp.utils.config.AppConfig.getAiApiUrl();
        String model = com.example.aedusapp.utils.config.AppConfig.getAiModel();

        try {
            // System Prompt: Direct instructions on what to do
            String systemPrompt = "Actúa como una extensión de inteligencia artificial integrada en un software de gestión (Dashboard). Tu nombre es 'Aedus AI'.\n"
                    +
                    "Tus reglas de comportamiento son:\n" +
                    "Brevedad extrema: Se educado y saluda al inicio unicamente. Ve directo a la respuesta.\n" +
                    "Contexto técnico: Responde únicamente dudas sobre métricas, datos, tendencias o funciones del software.\n"
                    +
                    "Idioma: Responde siempre en español profesional y conciso.\n" +
                    "Limitación: Si el usuario te pide tareas creativas, chistes o temas personales, responde: 'Solo estoy autorizado para realizar análisis de datos'.\n"
                    +
                    "Formato: Usa viñetas (puntos) si tienes que enumerar más de dos elementos.";

            // JSON construction (Standard OpenAI/Groq format)
            JsonObject root = new JsonObject();
            root.addProperty("model", model);

            if (context != null && !context.isEmpty()) {
                systemPrompt += "\n\nINFORMACIÓN DE CONTEXTO ACTUAL:\n" + context;
            }

            JsonArray messages = new JsonArray();
            messages.add(createMsg("system", systemPrompt));
            messages.add(createMsg("user", promptUsuario));
            root.add("messages", messages);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(root)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonRes = new Gson().fromJson(response.body(), JsonObject.class);
                return jsonRes.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else {
                return "Groq Error (" + response.statusCode() + "): " + response.body();
            }
        } catch (Exception e) {
            return "Connection exception: " + e.getMessage();
        }
    }

    // Helper method to create JSON messages
    private JsonObject createMsg(String role, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", content);
        return msg;
    }

}
