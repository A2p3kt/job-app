package dev.toluwalase.jobapp.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Google Gemini AI integration.
 * Enables type-safe property resolution and provides Spring IDE metadata
 * to eliminate unknown property warnings.
 */
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /**
     * Google Gemini API key (defaults to GEMINI_API_KEY environment variable).
     */
    private String apiKey = "";

    /**
     * Target Google Gemini model identifier (e.g., "gemini-3.1-flash-lite").
     */
    private String model = "gemini-3.1-flash-lite";

    public GeminiProperties() {
    }

    public GeminiProperties(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gemini-3.1-flash-lite";
    }
}

