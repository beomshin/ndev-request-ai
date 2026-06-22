package com.nice.qa.service.impl;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.mxgraph.io.mxCodec;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import com.nice.qa.service.FlowService;
import com.nice.qa.service.llm.promt.DiagramPromptBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * 종합 MD + 참고 링크 → LLM 으로 draw.io(mxGraph) 거래 흐름 시퀀스 XML 추론 → JGraphX 로 PNG 렌더링.
 *
 * <p>시퀀스 다이어그램은 좌표(라이프라인 x, 메시지 y)가 의미를 가지므로 자동 레이아웃을 적용하지 않고
 * LLM 이 생성한 좌표를 그대로 그린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String OPEN = "<mxGraphModel";
    private static final String CLOSE = "</mxGraphModel>";

    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(Part.fromText(
            "당신은 PG(전자결제대행) 연동 전문가이자 시스템 설계자입니다. "
                    + "개발요청서와 참고 문서를 분석해 기관 간 결제 거래 흐름을 "
                    + "draw.io 호환 시퀀스 다이어그램(mxGraph XML)으로 정확히 표현합니다."));

    private static final List<String> REFERENCE_LINKS = List.of(
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/pg_error_code_manual.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/pg_internal_policy.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/spec/nicepay_auth_spec.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/templates/standard_dev_request_output.md");

    /** 렌더링 실패 시 대체 다이어그램. */
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

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private final DiagramPromptBuilder diagramPromptBuilder;

    private Client client;
    private GenerateContentConfig config;

    @PostConstruct
    void initGemini() {
        this.client = Client.builder().apiKey(geminiApiKey).build();
        this.config = GenerateContentConfig.builder()
                .systemInstruction(SYSTEM_INSTRUCTION)
                .temperature(0.2f)
                .maxOutputTokens(8192)            // mxGraph XML 은 길어질 수 있어 넉넉히
                .tools(Tool.builder().googleSearch(GoogleSearch.builder()).build())
                .build();
    }

    @Override
    public byte[] renderPng(String markdown) {
        String xml = generateDiagramXml(markdown);   // 1) MD + 링크 → LLM → mxGraph XML
        return toPng(xml);                            // 2) mxGraph XML → PNG (JGraphX)
    }

    /** 종합 MD + 참고 링크로 프롬프트를 만들어 LLM 에게 거래 흐름 시퀀스 XML 을 추론하게 한다. */
    private String generateDiagramXml(String markdown) {
        String prompt = diagramPromptBuilder.build(
                markdown, REFERENCE_LINKS, DiagramPromptBuilder.DiagramType.SEQUENCE);

        GenerateContentResponse response = client.models.generateContent(MODEL, prompt, config);
        String xml = extractMxGraph(response.text());
        log.info("생성된 mxGraph XML:\n{}", xml);
        return xml;
    }

    /** mxGraph XML → PNG 바이트. 시퀀스는 좌표가 의미를 가지므로 레이아웃 없이 그대로 렌더링. */
    private byte[] toPng(String xml) {
        try {
            mxGraph graph = new mxGraph();
            graph.getModel().beginUpdate();
            try {
                Document doc = mxXmlUtils.parseXml(xml);
                new mxCodec(doc).decode(doc.getDocumentElement(), graph.getModel());
            } finally {
                graph.getModel().endUpdate();
            }

            BufferedImage image = mxCellRenderer.createBufferedImage(
                    graph, null, 2.0, Color.WHITE, true, null);
            if (image == null) {
                log.warn("렌더링 결과 이미지가 비었습니다. xml=\n{}", xml);
                return new byte[0];
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("draw.io(mxGraph) PNG 렌더링 실패. xml=\n{}", xml, e);
            return new byte[0];
        }
    }

    /** 응답에서 mxGraphModel 블록만 안전하게 추출. 없으면 대체본 사용. */
    private static String extractMxGraph(String text) {
        if (!StringUtils.hasText(text)) {
            return FALLBACK_XML;
        }
        String t = stripCodeFence(text.strip());
        int start = t.indexOf(OPEN);
        int end = t.lastIndexOf(CLOSE);
        if (start >= 0 && end > start) {
            return t.substring(start, end + CLOSE.length());
        }
        log.warn("mxGraphModel 블록을 찾지 못했습니다. raw=\n{}", text);
        return FALLBACK_XML;
    }

    /** 모델이 코드펜스(백틱 3개 + xml 등)를 붙였을 경우 제거. */
    private static String stripCodeFence(String text) {
        String t = text.strip();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t.strip();
    }
}