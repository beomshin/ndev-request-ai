package com.nice.qa.service.llm;

import org.springframework.util.StringUtils;

/**
 * LLM 응답 텍스트의 공통 후처리 유틸리티 클래스.
 *
 * <p>Gemini를 비롯한 대부분의 LLM 모델은 코드 블록을 반환할 때
 * 아래와 같이 코드펜스(백틱 3개)와 언어명을 자동으로 붙이는 경향이 있다.
 *
 * <pre>
 * ```json
 * { "key": "value" }
 * ```
 * </pre>
 *
 * <p>{@code DocService}와 {@code FlowService} 양쪽이 동일한 처리를 필요로 하므로
 * 이 클래스에서 일괄 관리한다. 인스턴스화가 불필요한 순수 유틸리티 클래스이므로
 * 생성자를 {@code private}으로 막아 둔다.
 *
 * <p>사용 예:
 * <pre>
 * String raw = geminiLlmClient.generate(prompt, config, "다이어그램생성");
 * String xml  = LlmResponseParser.stripCodeFence(raw); // 코드펜스 제거 후 순수 XML
 * </pre>
 */
public final class LlmResponseParser {

    /** 인스턴스화 방지 — 정적 유틸리티 클래스 */
    private LlmResponseParser() {}

    /**
     * LLM 응답 텍스트에서 코드펜스(``` 언어명 ... ```)를 제거하고 순수 텍스트만 반환한다.
     *
     * <p>처리 규칙:
     * <ol>
     *   <li>입력이 {@code null}이거나 공백만 있으면 빈 문자열 반환.</li>
     *   <li>앞뒤 공백(개행 포함) 제거 후, 문자열이 {@code ```}로 시작하는 경우:
     *       <ul>
     *         <li>첫 줄의 ``` + 언어명(선택) + 개행 공백 제거</li>
     *         <li>마지막의 공백 + ``` 제거</li>
     *       </ul>
     *   </li>
     *   <li>코드펜스가 없으면 앞뒤 공백만 제거하고 그대로 반환.</li>
     * </ol>
     *
     * <p>지원 형태 예시:
     * <pre>
     * ```json\n{...}\n```  →  {...}
     * ```xml\n&lt;mxGraphModel&gt;\n```  →  &lt;mxGraphModel&gt;
     * ```\nsome text\n```  →  some text
     * plain text          →  plain text (변경 없음)
     * </pre>
     *
     * @param text LLM으로부터 받은 원본 응답 텍스트. {@code null} 허용.
     * @return 코드펜스가 제거된 순수 텍스트. 입력이 비어 있으면 빈 문자열.
     */
    public static String stripCodeFence(String text) {
        // null 또는 공백 전용 문자열 조기 반환
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.strip();
        // LLM이 코드펜스를 붙인 경우에만 제거 처리
        // 정규식 설명:
        //   ^```[a-zA-Z]*\s*  → 시작 코드펜스 (```json, ```xml, ``` 등) + 이후 공백/개행
        //   \s*```$           → 끝 코드펜스 앞 공백/개행 + ```
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return trimmed.strip();
    }
}
