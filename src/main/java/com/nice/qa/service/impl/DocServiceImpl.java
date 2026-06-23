package com.nice.qa.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.*;
import com.nice.qa.config.GeminiProperties;
import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.DocService;
import com.nice.qa.service.llm.GeminiLlmClient;
import com.nice.qa.service.llm.LlmResponseParser;
import com.nice.qa.service.llm.ReferenceLinks;
import com.nice.qa.service.llm.dto.ProjectMdResult;
import com.nice.qa.service.llm.md.StandardMarkdownRenderer;
import com.nice.qa.service.llm.promt.ProjectPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(Part.fromText(
            "당신은 국내외 PG(전자결제대행) 연동 전문가입니다. "
                    + "결제창 연동, 인증/승인/취소/환불, 정산, 웹훅(노티) 처리, 시큐어 코딩(PCI-DSS) 등 "
                    + "PG 도메인 전반을 깊이 이해하고 있습니다. "
                    + "역할은 개발요청서를 분석해 프로젝트 메타데이터를 정확하고 구현 가능한 형태의 JSON으로 추출/추론하는 것입니다."));

    private final GeminiLlmClient llmClient;
    private final GeminiProperties geminiProperties;
    private final ProjectPromptBuilder promptBuilder;
    private final StandardMarkdownRenderer markdownRenderer;

    @Override
    public String assembleMarkdown(DevRequestRequest request) {
        log.info("[DocService] 마크다운 생성 시작 (author={}, serviceName={})",
                request.author(), request.serviceName());
        String prompt = promptBuilder.build(request, ReferenceLinks.ALL);
        ProjectMdResult result = requestProjectMd(prompt);
        applyDeterministicFields(result, request);
        String markdown = markdownRenderer.render(result);
        log.info("[DocService] 마크다운 생성 완료 (length={})", markdown.length());
        return markdown;
    }

    @Override
    public ProjectMdResult assembleJson(DevRequestRequest request) {
        log.info("[DocService] JSON 결과 생성 시작 (author={}, serviceName={})",
                request.author(), request.serviceName());
        String prompt = promptBuilder.build(request, ReferenceLinks.ALL);
        return requestProjectMd(prompt);
    }

    private ProjectMdResult requestProjectMd(String prompt) {
        GenerateContentConfig config = buildConfig();
        String raw = llmClient.generate(prompt, config, "메타데이터추출");
        String json = LlmResponseParser.stripCodeFence(raw);
        log.debug("[DocService] LLM 원본 JSON:\n{}", json);
        return parse(json);
    }

    private GenerateContentConfig buildConfig() {
        return GenerateContentConfig.builder()
                .systemInstruction(SYSTEM_INSTRUCTION)
                .temperature(geminiProperties.temperature())
                .maxOutputTokens(geminiProperties.maxOutputTokens())
                .tools(Tool.builder().googleSearch(GoogleSearch.builder()).build())
                .build();
    }

    /** 요청서에 이미 들어있는 확정 값은 LLM 추론보다 우선해 덮어쓴다. */
    private static void applyDeterministicFields(ProjectMdResult result, DevRequestRequest req) {
        result.setAuthor(req.author());
        result.setDepartment(req.department());

        if (!StringUtils.hasText(result.getProductName())) {
            result.setProductName(req.serviceName());
        }
        if (!StringUtils.hasText(result.getCreatedDate())) {
            result.setCreatedDate(LocalDate.now().toString());
        }
        if (StringUtils.hasText(req.funcType())) {
            result.setNewServiceOrSelfImprovement(req.funcType().contains("신규"));
        }
    }

    private static ProjectMdResult parse(String json) {
        try {
            ProjectMdResult result = MAPPER.readValue(json, ProjectMdResult.class);
            return result != null ? result : emptyResult();
        } catch (Exception e) {
            log.error("[DocService] ProjectMdResult JSON 파싱 실패. raw=\n{}", json, e);
            return emptyResult();
        }
    }

    private static ProjectMdResult emptyResult() {
        try {
            return MAPPER.readValue("{}", ProjectMdResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("빈 ProjectMdResult 생성 실패", e);
        }
    }
}
