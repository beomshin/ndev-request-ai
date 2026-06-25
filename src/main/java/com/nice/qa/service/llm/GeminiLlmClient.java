package com.nice.qa.service.llm;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.nice.qa.config.GeminiProperties;
import com.nice.qa.exception.LlmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Gemini LLM API 호출을 캡슐화하는 단일 진입점 컴포넌트.
 *
 * <p>이 클래스는 Google Gemini API와의 통신을 전담하며, 서비스 계층이
 * LLM 호출 방식에 의존하지 않도록 추상화한다.
 *
 * <p>주요 책임:
 * <ul>
 *   <li>LLM API 호출 및 응답 텍스트 반환</li>
 *   <li>호출 소요 시간(ms) 로깅 — 성능 모니터링 목적</li>
 *   <li>{@link Exception} → {@link LlmException} 변환 — 서비스 계층에 일관된 예외 전파</li>
 * </ul>
 *
 * <p>서비스 계층({@code DocService}, {@code FlowService} 등)은 이 클래스만 의존하면 되므로,
 * 향후 모델 교체(Gemini → 다른 LLM)도 이 클래스만 수정하면 된다.
 *
 * <p>LLM 응답은 코드펜스(백틱 3개)가 포함된 원본 텍스트 그대로 반환된다.
 * 후처리가 필요한 경우 {@link LlmResponseParser#stripCodeFence(String)}를 사용한다.
 *
 * @see LlmResponseParser
 * @see GeminiProperties
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiLlmClient {

    /** Google Gemini SDK 클라이언트 — API 인증 및 HTTP 통신 담당 */
    private final Client geminiClient;

    /** application.yml에서 주입되는 Gemini 설정 (모델명, API 키 등) */
    private final GeminiProperties geminiProperties;

    /**
     * 프롬프트와 설정을 Gemini API에 전달하고 LLM 응답 텍스트를 반환한다.
     *
     * <p>호출 흐름:
     * <ol>
     *   <li>요청 시작 로그 출력 (모델명, 목적 포함)</li>
     *   <li>{@code geminiClient.models.generateContent()} 호출 — 동기(블로킹) 방식</li>
     *   <li>응답에서 {@code .text()} 추출하여 반환</li>
     *   <li>성공/실패 모두 경과 시간(ms) 로그 출력</li>
     * </ol>
     *
     * <p>반환 값은 LLM이 생성한 원본 텍스트이며, 코드펜스 제거 등의 후처리는
     * 호출자({@link LlmResponseParser})가 수행한다.
     *
     * @param prompt  LLM에 전달할 완성된 프롬프트 문자열.
     *                {@link com.nice.qa.service.llm.promt.ProjectPromptBuilder} 또는
     *                {@link com.nice.qa.service.llm.promt.DiagramPromptBuilder}가 조립한 값을 사용한다.
     * @param config  온도(temperature), 최대 토큰 수, 툴 설정 등 호출별 GenerateContentConfig.
     *                동일한 목적이라도 다이어그램/문서 생성 시 다른 설정을 사용할 수 있다.
     * @param purpose 로그 식별용 호출 목적 레이블. 예: {@code "메타데이터추출"}, {@code "다이어그램생성"}.
     *                로그에 {@code [LLM:purpose]} 형식으로 출력되어 어떤 작업에서 호출됐는지 추적 가능.
     * @return LLM이 생성한 원본 응답 텍스트 (코드펜스 미제거 상태).
     *         빈 응답인 경우 빈 문자열이 반환될 수 있다.
     * @throws LlmException Gemini API 호출 중 예외 발생 시.
     *                      원인 예외(cause)는 래핑되어 함께 전파된다.
     */
    public String generate(String prompt, GenerateContentConfig config, String purpose) {
        // 요청 시작 시점 로그 — 어떤 모델로 어떤 목적의 LLM 호출인지 기록
        log.info("[LLM:{}] 요청 시작 (model={})", purpose, geminiProperties.model());
        long start = System.currentTimeMillis();
        try {
            // Gemini SDK를 통해 동기 방식으로 콘텐츠 생성 요청
            // generateContent(모델명, 프롬프트, 설정) → GenerateContentResponse
            // .text() 는 첫 번째 후보(candidate)의 텍스트를 추출하는 헬퍼 메서드
            String result = geminiClient.models
                    .generateContent(geminiProperties.model(), prompt, config)
                    .text();
            long elapsed = System.currentTimeMillis() - start;
            // 성공 응답 로그 — 소요 시간(ms) 기록으로 성능 추이 모니터링 가능
            log.info("[LLM:{}] 응답 완료 ({}ms)", purpose, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            // 실패 시 경과 시간과 원인 메시지를 함께 로깅
            log.error("[LLM:{}] 호출 실패 ({}ms): {}", purpose, elapsed, e.getMessage(), e);
            // SDK/네트워크/인증 예외를 서비스 계층이 처리하기 쉬운 LlmException으로 래핑하여 전파
            throw new LlmException("LLM 호출 실패: " + purpose, e);
        }
    }
}
