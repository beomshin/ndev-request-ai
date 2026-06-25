package com.nice.qa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@code application.yml}의 {@code gemini.*} 프로퍼티를 바인딩하는 설정 레코드
 * (Immutable configuration record bound to the {@code gemini.*} properties prefix).
 *
 * <p>Java Record로 선언되어 불변(immutable)이며 Lombok 없이도 getter, equals, hashCode, toString이 자동 생성된다.
 * {@link ConfigurationProperties}를 통해 Spring Boot가 애플리케이션 기동 시 자동으로 값을 주입한다.
 *
 * <p>컴팩트 생성자(compact constructor)에서 {@code apiKey} 유효성을 검증하여
 * 설정 누락 시 애플리케이션이 즉시 실패하도록 한다(fail-fast).
 *
 * <p>예시 설정 ({@code application.yml}):
 * <pre>
 * gemini:
 *   api-key: ${GEMINI_API_KEY}       # 환경변수로 주입 — 소스에 직접 하드코딩 금지
 *   model: gemini-2.0-flash          # 호출할 Gemini 모델 ID
 *   temperature: 0.2                 # 생성 무작위성 (0.0 ~ 1.0); 낮을수록 결정적
 *   max-output-tokens: 8192          # 응답 최대 토큰 수
 * </pre>
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        /**
         * Gemini API 인증 키 (Gemini API authentication key).
         * 환경변수 {@code GEMINI_API_KEY}를 통해 주입하는 것을 권장한다.
         * 빈 값이거나 null이면 컴팩트 생성자에서 {@link IllegalArgumentException}이 발생한다.
         */
        String apiKey,

        /**
         * 호출할 Gemini 모델의 ID (Gemini model identifier to invoke).
         * 예: {@code "gemini-2.0-flash"}, {@code "gemini-1.5-pro"}
         */
        String model,

        /**
         * 텍스트 생성 무작위성 조절 파라미터 (Temperature controlling generation randomness).
         * 범위: 0.0(결정적, deterministic) ~ 1.0(창의적, creative).
         * 요건 정의서처럼 일관성이 중요한 용도에는 낮은 값(0.1 ~ 0.3)을 권장한다.
         */
        float temperature,

        /**
         * 단일 응답에서 생성 가능한 최대 토큰 수 (Maximum number of output tokens per response).
         * 긴 마크다운 문서 생성을 위해 충분히 큰 값(예: 8192)으로 설정한다.
         */
        int maxOutputTokens
) {
    /**
     * 컴팩트 생성자 — {@code apiKey} 유효성 검사 (Compact constructor for fail-fast API key validation).
     *
     * <p>Spring Boot가 프로퍼티를 바인딩할 때 자동으로 호출된다.
     * {@code apiKey}가 null이거나 빈 문자열이면 즉시 {@link IllegalArgumentException}을 던져
     * 잘못된 설정으로 애플리케이션이 기동되는 것을 방지한다.
     */
    public GeminiProperties {
        // apiKey가 없으면 기동 즉시 실패 — 설정 누락을 런타임에 늦게 발견하는 것을 방지
        // (fail fast at startup so a missing API key is caught immediately, not at first API call)
        Assert.hasText(apiKey, "GEMINI_API_KEY 환경변수가 설정 필요 (application.yml의 gemini.api-key)");
    }
}
