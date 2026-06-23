package com.nice.qa.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.DocService;
import com.nice.qa.service.llm.dto.ProjectMdResult;
import com.nice.qa.service.llm.promt.ProjectPromptBuilder;
import com.nice.qa.service.llm.md.StandardMarkdownRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {


    private static final String MODEL = "gemini-2.5-flash";

    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(Part.fromText(
            "당신은 국내외 PG(전자결제대행) 연동 전문가입니다. "
                    + "결제창 연동, 인증/승인/취소/환불, 정산, 웹훅(노티) 처리, 시큐어 코딩(PCI-DSS) 등 "
                    + "PG 도메인 전반을 깊이 이해하고 있습니다. "
                    + "역할은 개발요청서를 분석해 프로젝트 메타데이터를 정확하고 구현 가능한 형태의 JSON으로 추출/추론하는 것입니다."));

    /** 분석에 사용할 참고 문서 링크. 운영에서는 application.yml 등으로 분리 권장. */
    private static final List<String> REFERENCE_LINKS = List.of(
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/catalog/catalog_category_tree_v1.yaml",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_composite_payment_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_cpid_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_error_message_mapping_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_index.yaml",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_installment_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_linked_payment_method_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_min_amount_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_net_cancel_support_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_new_payment_checklist_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_partial_cancel_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_target_channel_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_template.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/policy_timeout_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/provider/provider_kakaopay_v2.0.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/provider/provider_naverpay_v1.5.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/provider/provider_payco_v1.2.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/requests/request_examples_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/requests/request_form_template_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/requests/request_history_v1.yaml",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/requests/request_policy_trigger_map_v1.yaml",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/spec/spec_approval_v2.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/spec/spec_auth_v2.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/spec/spec_netcancel_v1.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/spec/spec_signdata_v2.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/spec/spec_template.md",
            "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/README_KB.md");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private final ProjectPromptBuilder promptBuilder;
    private final StandardMarkdownRenderer markdownRenderer;

    // 매 호출마다 새로 만들 필요가 없어 1회 초기화해 재사용한다.
    private Client client;
    private GenerateContentConfig config;

    @PostConstruct
    void initGemini() {
        // 키 누락 시 부팅을 즉시 막아 호출 시점의 의미 모를 403을 차단한다.
        Assert.hasText(geminiApiKey, "GEMINI_API_KEY 환경변수가 설정 필요 (application.yml의 gemini.api-key)");
        this.client = Client.builder().apiKey(geminiApiKey).build();
        this.config = GenerateContentConfig.builder()
                .systemInstruction(SYSTEM_INSTRUCTION)
                .temperature(0.2f)                 // 메타데이터 추출 → 일관/정확이 중요하므로 낮게
                .maxOutputTokens(8192)
                .tools(Tool.builder().googleSearch(GoogleSearch.builder()).build())
                .build();
    }

    @Override
    public String assembleMarkdown(DevRequestRequest request) {
        String prompt = promptBuilder.build(request, REFERENCE_LINKS);
        ProjectMdResult result = requestProjectMd(prompt);
        applyDeterministicFields(result, request);
        return markdownRenderer.render(result);
    }

    @Override
    public ProjectMdResult assembleJson(DevRequestRequest request) {
        String prompt = promptBuilder.build(request, REFERENCE_LINKS);
        return requestProjectMd(prompt);
    }

    /** Gemini 호출 → JSON 파싱. */
    private ProjectMdResult requestProjectMd(String prompt) {
        GenerateContentResponse response = client.models.generateContent(MODEL, prompt, config);
        String json = stripCodeFence(response.text());
        log.info("ProjectMdResult 원본 JSON:\n{}", json);
        return parse(json);
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
            log.error("ProjectMdResult JSON 파싱 실패. raw:\n{}", json, e);
            return emptyResult();
        }
    }

    /** ProjectMdResult 의 기본 생성자가 private 이므로 Jackson 으로 빈 인스턴스를 만든다. */
    private static ProjectMdResult emptyResult() {
        try {
            return MAPPER.readValue("{}", ProjectMdResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("빈 ProjectMdResult 생성 실패", e);
        }
    }

    /** 모델이 코드펜스(백틱 3개 + json)를 붙였을 경우 제거. */
    private static String stripCodeFence(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return trimmed.strip();
    }

}
