package com.nice.qa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        float temperature,
        int maxOutputTokens
) {
    public GeminiProperties {
        Assert.hasText(apiKey, "GEMINI_API_KEY 환경변수가 설정 필요 (application.yml의 gemini.api-key)");
    }
}
