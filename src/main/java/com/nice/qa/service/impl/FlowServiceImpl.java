package com.nice.qa.service.impl;

import com.google.genai.types.*;
import com.nice.qa.config.GeminiProperties;
import com.nice.qa.service.FlowService;
import com.nice.qa.service.llm.GeminiLlmClient;
import com.nice.qa.service.llm.LlmResponseParser;
import com.nice.qa.service.llm.MxGraphRenderer;
import com.nice.qa.service.llm.ReferenceLinks;
import com.nice.qa.service.llm.promt.DiagramPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 마크다운 → LLM으로 mxGraph XML 추론 → PNG 렌더링.
 *
 * <p>{@link FlowService}의 Gemini 기반 구현체.
 * PG 연동 시퀀스 다이어그램 전문 시스템 프롬프트를 사용하여
 * 개발요청서 마크다운을 draw.io 호환 mxGraph XML로 변환한 뒤,
 * 필요에 따라 PNG 이미지로 렌더링한다.
 *
 * <h3>LLM 응답 처리 전략</h3>
 * <ul>
 *   <li>응답 텍스트에서 {@code <mxGraphModel>}~{@code </mxGraphModel>} 블록을 직접 추출</li>
 *   <li>블록을 찾지 못한 경우 경고 로그를 남기고 {@link #FALLBACK_XML}을 반환</li>
 * </ul>
 *
 * @see FlowService
 * @see GeminiLlmClient
 * @see MxGraphRenderer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    /** mxGraph XML 시작 태그 — 응답에서 XML 블록 시작 위치 탐색에 사용 */
    private static final String OPEN = "<mxGraphModel";

    /** mxGraph XML 종료 태그 — 응답에서 XML 블록 끝 위치 탐색에 사용 */
    private static final String CLOSE = "</mxGraphModel>";

    /**
     * Gemini에 전달하는 시스템 지시문(system instruction).
     * 모델이 PG 연동 전문가이자 시스템 설계자 역할을 수행하도록 페르소나를 정의하며,
     * 개발요청서를 분석해 기관 간 결제 거래 흐름을 draw.io 호환 시퀀스 다이어그램으로 표현하도록 지시한다.
     */
    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(Part.fromText(
            "당신은 PG(전자결제대행) 연동 전문가이자 시스템 설계자입니다. "
                    + "개발요청서와 참고 문서를 분석해 기관 간 결제 거래 흐름을 "
                    + "draw.io 호환 시퀀스 다이어그램(mxGraph XML)으로 정확히 표현합니다."));

    /**
     * mxGraph XML 파싱에 실패했을 때 반환하는 대체(fallback) XML.
     * 다이어그램 영역에 "다이어그램 생성 실패" 메시지를 담은 빨간색 박스를 표시한다.
     */
    private static final String FALLBACK_XML = """
            <mxGraphModel>
              <root>
                <mxCell id="0"/>
                <mxCell id="1" parent="0"/>
                <mxCell id="err" value="다이어그램 생성 실패" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;" vertex="1" parent="1">
                  <mxGeometry x="200" y="80" width="220" height="60" as="geometry"/>
                </mxCell>
              </root>
            </mxGraphModel>""";

    /** Gemini API 호출 클라이언트 */
    private final GeminiLlmClient llmClient;

    /** Gemini 호출 파라미터(온도, 최대 토큰 수 등) 설정 */
    private final GeminiProperties geminiProperties;

    /** 다이어그램 생성용 LLM 프롬프트를 조립하는 빌더 */
    private final DiagramPromptBuilder diagramPromptBuilder;

    /** mxGraph XML을 PNG 이미지로 렌더링하는 렌더러 */
    private final MxGraphRenderer mxGraphRenderer;

    /**
     * 마크다운 기반으로 시퀀스 다이어그램 PNG를 생성한다.
     *
     * <p>내부적으로 {@link #generateXml}을 호출해 mxGraph XML을 얻은 뒤,
     * {@link MxGraphRenderer#toPng}로 PNG 이미지 바이트 배열로 변환한다.
     *
     * @param markdown 다이어그램 생성의 입력이 되는 개발요청서 마크다운 문자열
     * @return PNG 이미지 바이트 배열
     */
    @Override
    public byte[] renderPng(String markdown) {
        log.info("[FlowService] 다이어그램 PNG 생성 시작");
        String xml = generateXml(markdown);
        byte[] png = mxGraphRenderer.toPng(xml);
        log.info("[FlowService] 다이어그램 PNG 생성 완료 ({}bytes)", png.length);
        return png;
    }

    /**
     * 마크다운으로부터 draw.io 호환 mxGraph XML을 생성한다.
     *
     * <p>처리 순서:
     * <ol>
     *   <li>{@link DiagramPromptBuilder}로 시퀀스 다이어그램 생성 프롬프트 구성</li>
     *   <li>Gemini API 호출로 XML 텍스트 획득</li>
     *   <li>{@link #extractMxGraph}로 응답에서 mxGraph 블록 추출</li>
     * </ol>
     *
     * @param markdown 다이어그램 생성의 입력이 되는 개발요청서 마크다운 문자열
     * @return draw.io 호환 mxGraph XML 문자열 (파싱 실패 시 {@link #FALLBACK_XML})
     */
    @Override
    public String generateXml(String markdown) {
        // 시퀀스 다이어그램 타입으로 프롬프트를 구성하고 참조 링크 전체를 포함
        String prompt = diagramPromptBuilder.build(
                markdown, ReferenceLinks.ALL, DiagramPromptBuilder.DiagramType.SEQUENCE);
        GenerateContentConfig config = buildConfig();
        // LLM 호출: "다이어그램생성" 레이블은 로그 추적용
        String raw = llmClient.generate(prompt, config, "다이어그램생성");
        String xml = extractMxGraph(raw);
        log.debug("[FlowService] 생성된 mxGraph XML:\n{}", xml);
        return xml;
    }

    /**
     * Gemini 생성 요청에 사용할 {@link GenerateContentConfig}를 구성한다.
     *
     * <p>시스템 지시문, 온도, 최대 출력 토큰을 설정하고
     * Google Search 도구를 활성화하여 모델이 최신 PG 스펙을 참조할 수 있게 한다.
     *
     * @return 완성된 Gemini 콘텐츠 생성 설정 객체
     */
    private GenerateContentConfig buildConfig() {
        return GenerateContentConfig.builder()
                .systemInstruction(SYSTEM_INSTRUCTION)
                .temperature(geminiProperties.temperature())
                .maxOutputTokens(geminiProperties.maxOutputTokens())
                // Google Search 도구 활성화: 최신 PG 연동 문서를 검색에 활용 가능
                .tools(Tool.builder().googleSearch(GoogleSearch.builder()).build())
                .build();
    }

    /**
     * 응답에서 mxGraphModel 블록만 안전하게 추출. 없으면 대체본 반환.
     *
     * <p>처리 단계:
     * <ol>
     *   <li>응답이 비어 있으면 즉시 {@link #FALLBACK_XML} 반환</li>
     *   <li>코드 펜스(```) 제거 후 {@code <mxGraphModel} 시작 위치와
     *       {@code </mxGraphModel>} 마지막 위치를 탐색</li>
     *   <li>유효한 범위를 찾으면 해당 부분만 잘라내어 반환</li>
     *   <li>찾지 못하면 경고 로그를 남기고 {@link #FALLBACK_XML} 반환</li>
     * </ol>
     *
     * @param text LLM이 반환한 원본 텍스트
     * @return 추출된 mxGraph XML 문자열, 또는 실패 시 대체 XML
     */
    private static String extractMxGraph(String text) {
        // 응답 자체가 비어있으면 대체본 사용
        if (!StringUtils.hasText(text)) {
            return FALLBACK_XML;
        }
        // 마크다운 코드 펜스(```xml ... ```) 제거
        String t = LlmResponseParser.stripCodeFence(text);
        int start = t.indexOf(OPEN);
        // lastIndexOf를 사용해 중복 태그가 있어도 가장 바깥쪽 블록을 추출
        int end = t.lastIndexOf(CLOSE);
        if (start >= 0 && end > start) {
            // CLOSE 태그 자체를 포함하도록 길이만큼 더함
            return t.substring(start, end + CLOSE.length());
        }
        log.warn("[FlowService] mxGraphModel 블록을 찾지 못해 대체본을 사용합니다 (raw length={})", text.length());
        return FALLBACK_XML;
    }
}
