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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    private static final String OPEN = "<mxGraphModel";
    private static final String CLOSE = "</mxGraphModel>";

    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(Part.fromText(
            "당신은 PG(전자결제대행) 연동 전문가이자 시스템 설계자입니다. "
                    + "개발요청서와 참고 문서를 분석해 기관 간 결제 거래 흐름을 "
                    + "draw.io 호환 시퀀스 다이어그램(mxGraph XML)으로 정확히 표현합니다."));

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

    private final GeminiLlmClient llmClient;
    private final GeminiProperties geminiProperties;
    private final DiagramPromptBuilder diagramPromptBuilder;
    private final MxGraphRenderer mxGraphRenderer;

    @Override
    public byte[] renderPng(String markdown) {
        log.info("[FlowService] 다이어그램 PNG 생성 시작");
        String xml = generateDiagramXml(markdown);
        byte[] png = mxGraphRenderer.toPng(xml);
        log.info("[FlowService] 다이어그램 PNG 생성 완료 ({}bytes)", png.length);
        return png;
    }

    private String generateDiagramXml(String markdown) {
        String prompt = diagramPromptBuilder.build(
                markdown, ReferenceLinks.ALL, DiagramPromptBuilder.DiagramType.SEQUENCE);
        GenerateContentConfig config = buildConfig();
        String raw = llmClient.generate(prompt, config, "다이어그램생성");
        String xml = extractMxGraph(raw);
        log.debug("[FlowService] 생성된 mxGraph XML:\n{}", xml);
        return xml;
    }

    private GenerateContentConfig buildConfig() {
        return GenerateContentConfig.builder()
                .systemInstruction(SYSTEM_INSTRUCTION)
                .temperature(geminiProperties.temperature())
                .maxOutputTokens(geminiProperties.maxOutputTokens())
                .tools(Tool.builder().googleSearch(GoogleSearch.builder()).build())
                .build();
    }

    /** 응답에서 mxGraphModel 블록만 안전하게 추출. 없으면 대체본 반환. */
    private static String extractMxGraph(String text) {
        if (!StringUtils.hasText(text)) {
            return FALLBACK_XML;
        }
        String t = LlmResponseParser.stripCodeFence(text);
        int start = t.indexOf(OPEN);
        int end = t.lastIndexOf(CLOSE);
        if (start >= 0 && end > start) {
            return t.substring(start, end + CLOSE.length());
        }
        log.warn("[FlowService] mxGraphModel 블록을 찾지 못해 대체본을 사용합니다 (raw length={})", text.length());
        return FALLBACK_XML;
    }
}
