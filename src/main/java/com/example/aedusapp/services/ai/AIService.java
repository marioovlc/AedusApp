package com.example.aedusapp.services.ai;

import com.example.aedusapp.exceptions.AIException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    /** System Prompt por defecto: asistente de métricas del Dashboard */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "Actúa como una extensión de inteligencia artificial integrada en un software de gestión (Dashboard). Tu nombre es 'Aedus AI'.\n" +
            "Tus reglas de comportamiento son:\n" +
            "Brevedad extrema: Se educado y saluda al inicio unicamente. Ve directo a la respuesta.\n" +
            "Contexto técnico: Responde únicamente dudas sobre métricas, datos, tendencias o funciones del software.\n" +
            "Idioma: Responde siempre en español profesional y conciso.\n" +
            "Limitación: Si el usuario te pide tareas creativas, chistes o temas personales, responde: 'Solo estoy autorizado para realizar análisis de datos'.\n" +
            "Formato: Usa viñetas (puntos) si tienes que enumerar más de dos elementos.";

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 1500;

    private final HttpClient httpClient;
    private final Gson gson;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    /**
     * Consulta la IA usando el System Prompt por defecto (métricas del Dashboard).
     */
    public String askAI(String promptUsuario) {
        return askAI(promptUsuario, null, null);
    }

    /**
     * Consulta la IA añadiendo contexto extra al System Prompt por defecto.
     * @param context texto adicional que se anexa al system prompt base (ej. datos de métricas).
     */
    public String askAI(String promptUsuario, String context) {
        return askAI(promptUsuario, context, null);
    }

    /**
     * Consulta la IA con un System Prompt completamente personalizado.
     * Si se proporciona {@code systemPromptOverride}, REEMPLAZA completamente el prompt por defecto.
     * @param context         contexto de datos adicional (ignorado si se usa override).
     * @param systemPromptOverride si no es nulo, sustituye al prompt base completo.
     */
    public String askAI(String promptUsuario, String context, String systemPromptOverride) {
        String apiKey = com.example.aedusapp.utils.config.AppConfig.getAiApiKey();
        String url = com.example.aedusapp.utils.config.AppConfig.getAiApiUrl();
        String model = com.example.aedusapp.utils.config.AppConfig.getAiModel();

        String finalSystemPrompt;
        if (systemPromptOverride != null && !systemPromptOverride.isBlank()) {
            finalSystemPrompt = systemPromptOverride;
        } else {
            finalSystemPrompt = (context != null && !context.isBlank())
                    ? DEFAULT_SYSTEM_PROMPT + "\n\nINFORMACIÓN DE CONTEXTO ACTUAL:\n" + context
                    : DEFAULT_SYSTEM_PROMPT;
        }

        JsonObject requestBody = buildRequestBody(model, finalSystemPrompt, promptUsuario);

        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonRes = gson.fromJson(response.body(), JsonObject.class);
                    return jsonRes.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                }

                AIException ex = new AIException("La API de IA respondió con error HTTP " + response.statusCode(), response.statusCode());
                if (ex.isRetryable() && attempt <= MAX_RETRIES) {
                    logger.warn("AIService: intento {}/{} fallido (HTTP {}). Reintentando en {}ms...",
                            attempt, MAX_RETRIES + 1, response.statusCode(), RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                } else {
                    logger.error("AIService: error definitivo tras {} intentos. HTTP {}: {}",
                            attempt, response.statusCode(), response.body());
                    throw ex;
                }
            } catch (AIException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AIException("La petición a la IA fue interrumpida.", e);
            } catch (Exception e) {
                logger.error("AIService: error de red/conexión en intento {}/{}", attempt, MAX_RETRIES + 1, e);
                if (attempt > MAX_RETRIES) {
                    throw new AIException("No se pudo conectar con la API de IA tras " + attempt + " intentos.", e);
                }
                try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new AIException("Error inesperado en AIService.");
    }

    private JsonObject buildRequestBody(String model, String systemPrompt, String userMessage) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        JsonArray messages = new JsonArray();
        messages.add(createMsg("system", systemPrompt));
        messages.add(createMsg("user", userMessage));
        root.add("messages", messages);
        return root;
    }

    private JsonObject createMsg(String role, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", content);
        return msg;
    }
}

