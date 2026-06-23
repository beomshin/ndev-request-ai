package com.nice.qa.service.llm;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.nice.qa.config.GeminiProperties;
import com.nice.qa.exception.LlmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Gemini API 호출을 캡슐화하는 단일 진입점.
 * 소요시간 로깅과 예외 변환을 담당하며, 서비스 계층은 이 클래스만 의존한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiLlmClient {

    private final Client geminiClient;
    private final GeminiProperties geminiProperties;

    /**
     * 프롬프트와 설정을 받아 LLM 응답 텍스트를 반환한다.
     *
     * @param prompt  LLM에 전달할 프롬프트
     * @param config  온도·토큰 제한·툴 등 호출별 설정
     * @param purpose 로그 식별용 호출 목적 (예: "메타데이터추출", "다이어그램생성")
     * @return LLM 원본 응답 텍스트 (코드펜스 미제거 상태)
     * @throws LlmException API 호출 실패 시
     */
    public String generate(String prompt, GenerateContentConfig config, String purpose) {
        log.info("[LLM:{}] 요청 시작 (model={})", purpose, geminiProperties.model());
        long start = System.currentTimeMillis();
        try {
            String result = geminiClient.models
                    .generateContent(geminiProperties.model(), prompt, config)
                    .text();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[LLM:{}] 응답 완료 ({}ms)", purpose, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[LLM:{}] 호출 실패 ({}ms): {}", purpose, elapsed, e.getMessage(), e);
            throw new LlmException("LLM 호출 실패: " + purpose, e);
        }
    }
}
