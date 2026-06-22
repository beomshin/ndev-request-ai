package com.nice.qa.service.impl;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.nice.qa.service.FlowService;
import com.nice.qa.service.llm.promt.DiagramPromptBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String START = "@startuml";
    private static final String END = "@enduml";

    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(Part.fromText(
            "당신은 PG(전자결제대행) 연동 전문가이자 시스템 설계자입니다. "
                    + "개발요청서와 참고 문서를 분석해 처리 흐름을 PlantUML 다이어그램으로 정확히 표현합니다."));

    private static final List<String> REFERENCE_LINKS = List.of(
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/pg_error_code_manual.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/pg_internal_policy.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/spec/nicepay_auth_spec.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/templates/standard_dev_request_output.md");

    private static final String FALLBACK = """
            @startuml
            start
            :다이어그램 생성 실패;
            stop
            @enduml""";

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
                .maxOutputTokens(4096)
                .tools(Tool.builder().googleSearch(GoogleSearch.builder()).build())
                .build();
    }

    @Override
    public byte[] renderPng(String markdown) {
        String puml = generatePlantUml(markdown);   // 1) MD + 링크 → LLM → PlantUML
        return toPng(puml);                          // 2) PlantUML → PNG
    }

    /** 종합 MD + 참고 링크로 프롬프트를 만들어 LLM 에게 PlantUML 소스를 추론하게 한다. */
    private String generatePlantUml(String markdown) {
        String prompt = diagramPromptBuilder.build(
                markdown, REFERENCE_LINKS, DiagramPromptBuilder.DiagramType.ACTIVITY);

        GenerateContentResponse response = client.models.generateContent(MODEL, prompt, config);
        String puml = extractPlantUml(response.text());
        log.info("생성된 PlantUML:\n{}", puml);
        return puml;
    }

    /** PlantUML 소스 → PNG 바이트. */
    private byte[] toPng(String puml) {
        try {
            SourceStringReader reader = new SourceStringReader(puml, StandardCharsets.UTF_8);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            reader.outputImage(out, new FileFormatOption(FileFormat.PNG));
            return out.toByteArray();
        } catch (IOException e) {
            log.error("PlantUML PNG 렌더링 실패. puml=\n{}", puml, e);
            return new byte[0];
        }
    }

    /** 응답에서 @startuml ... @enduml 블록만 안전하게 추출. 없으면 감싸거나 대체본 사용. */
    private static String extractPlantUml(String text) {
        if (!StringUtils.hasText(text)) {
            return FALLBACK;
        }
        String t = stripCodeFence(text.strip());
        int start = t.indexOf(START);
        int end = t.lastIndexOf(END);
        if (start >= 0 && end > start) {
            return t.substring(start, end + END.length()).strip();
        }
        return START + "\n" + t + "\n" + END;
    }

    /** 모델이 코드펜스(백틱 3개 + puml/plantuml)를 붙였을 경우 제거. */
    private static String stripCodeFence(String text) {
        String t = text.strip();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t.strip();
    }
}
