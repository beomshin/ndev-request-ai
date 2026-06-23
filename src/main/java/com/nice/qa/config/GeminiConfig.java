package com.nice.qa.config;

import com.google.genai.Client;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    public Client geminiClient(GeminiProperties props) {
        return Client.builder().apiKey(props.apiKey()).build();
    }
}
