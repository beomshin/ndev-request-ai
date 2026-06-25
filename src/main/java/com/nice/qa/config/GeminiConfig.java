package com.nice.qa.config;

import com.google.genai.Client;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Gemini API 클라이언트 Spring 설정 클래스 (Spring configuration for the Google Gemini API client).
 *
 * <p>{@code application.yml}의 {@code gemini.*} 프로퍼티를 {@link GeminiProperties}로 바인딩하고,
 * {@link com.google.genai.Client} 빈을 애플리케이션 컨텍스트에 등록한다.
 *
 * <p>빈 등록 후 서비스 계층에서 {@code @Autowired} 또는 생성자 주입으로
 * {@link com.google.genai.Client}를 사용하여 Gemini API를 호출할 수 있다.
 *
 * <p>필수 설정:
 * <pre>
 * gemini:
 *   api-key: ${GEMINI_API_KEY}   # 환경변수로 주입 권장
 *   model: gemini-2.0-flash
 *   temperature: 0.2
 *   max-output-tokens: 8192
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
// GeminiProperties 레코드를 @ConfigurationProperties 빈으로 활성화
// (activates GeminiProperties as a @ConfigurationProperties-bound bean)
public class GeminiConfig {

    /**
     * Gemini API 호출에 사용할 {@link com.google.genai.Client} 빈을 생성하여 등록한다
     * (Creates and registers a Gemini API {@link com.google.genai.Client} bean).
     *
     * <p>{@link GeminiProperties#apiKey()}로 API 키를 전달하여 클라이언트를 초기화한다.
     * API 키가 설정되지 않은 경우 {@link GeminiProperties} 생성자에서
     * {@link IllegalArgumentException}이 발생하여 애플리케이션 기동이 실패한다.
     *
     * @param props {@code gemini.*} 프로퍼티가 바인딩된 설정 객체 (bound Gemini configuration properties)
     * @return 초기화된 Gemini API 클라이언트 빈 (initialized Gemini API client bean)
     */
    @Bean
    public Client geminiClient(GeminiProperties props) {
        // API 키만으로 클라이언트를 빌드한다; 모델·온도 등 나머지 설정은 호출 시점에 적용된다
        // (only the API key is set here; model, temperature etc. are applied per-request)
        return Client.builder().apiKey(props.apiKey()).build();
    }
}
