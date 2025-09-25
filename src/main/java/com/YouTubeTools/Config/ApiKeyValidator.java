package com.YouTubeTools.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiKeyValidator {

    @Value("${youtube.api.key}")
    private String apiKey;

    public boolean isApiKeyConfigured() {
        boolean isValid = apiKey != null &&
                !apiKey.isEmpty() &&
                !apiKey.equals("your_youtube_api_key_here") &&
                apiKey.startsWith("AIza");

        if (!isValid) {
            log.warn("YouTube API key is not properly configured. Current value: {}",
                    apiKey != null ? (apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : apiKey) : "null");
        }

        return isValid;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiKeyStatus() {
        if (apiKey == null || apiKey.isEmpty()) {
            return "API key is not set";
        } else if (apiKey.equals("your_youtube_api_key_here")) {
            return "API key is using default placeholder value";
        } else if (!apiKey.startsWith("AIza")) {
            return "API key format appears invalid (should start with 'AIza')";
        } else {
            return "API key appears to be configured correctly";
        }
    }
}