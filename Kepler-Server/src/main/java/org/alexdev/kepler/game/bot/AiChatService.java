package org.alexdev.kepler.game.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.alexdev.kepler.util.config.GameConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AiChatService {
    private static volatile AiChatService instance;
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final HttpClient httpClient;
    private final ExecutorService executor;

    private AiChatService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.executor = Executors.newFixedThreadPool(2);
    }

    public void generateResponse(Bot bot, String playerName, String message, Consumer<String> callback) {
        this.executor.submit(() -> {
            try {
                String apiKey = GameConfiguration.getInstance().getString("ai.chat.api_key");
                if (apiKey == null || apiKey.isEmpty()) {
                    callback.accept(null);
                    return;
                }

                String model = GameConfiguration.getInstance().getString("ai.chat.model");
                int maxTokens = GameConfiguration.getInstance().getInteger("ai.chat.max_tokens");

                String systemPrompt = bot.getBotData().getAiSystemPrompt();
                if (systemPrompt == null || systemPrompt.isEmpty()) {
                    systemPrompt = "Eres " + bot.getDetails().getName() + ", un personaje en Habbo Hotel. "
                            + "Responde en español, máximo 2 frases cortas y amigables. "
                            + "No uses markdown ni emojis especiales.";
                }

                String jsonBody = buildRequestJson(model, systemPrompt, playerName, message, maxTokens);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String aiText = parseResponse(response.body());
                    callback.accept(aiText);
                } else {
                    log.warn("AI chat API returned status {}: {}", response.statusCode(), response.body());
                    callback.accept(null);
                }
            } catch (Exception e) {
                log.warn("AI chat request failed: {}", e.getMessage());
                callback.accept(null);
            }
        });
    }

    private String buildRequestJson(String model, String systemPrompt, String playerName, String message, int maxTokens) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("max_tokens", maxTokens);
        root.addProperty("temperature", 0.8);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", playerName + ": " + message);
        messages.add(userMsg);

        root.add("messages", messages);
        return root.toString();
    }

    private String parseResponse(String jsonBody) {
        try {
            JsonObject root = JsonParser.parseString(jsonBody).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                String content = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                return content.trim();
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", e.getMessage());
        }
        return null;
    }

    public static AiChatService getInstance() {
        if (instance == null) {
            synchronized (AiChatService.class) {
                if (instance == null) {
                    instance = new AiChatService();
                }
            }
        }
        return instance;
    }
}
