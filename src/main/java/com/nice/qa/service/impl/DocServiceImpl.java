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

/**
 * {@link DocService}의 Gemini 기반 구현체.
 *
 * <p>PG(전자결제대행) 도메인 전문 시스템 프롬프트를 사전 설정하고,
 * 개발요청서({@link DevRequestRequest})를 받아 Gemini LLM에 프롬프트를 전송한 뒤
 * 반환된 JSON을 {@link ProjectMdResult}로 파싱하고 표준 마크다운으로 렌더링한다.
 *
 * <h3>주요 처리 흐름</h3>
 * <ol>
 *   <li>요청 정보를 기반으로 {@link ProjectPromptBuilder}가 LLM 프롬프트를 구성</li>
 *   <li>{@link GeminiLlmClient}를 통해 Gemini API를 호출</li>
 *   <li>코드 펜스(```) 등을 제거한 후 JSON을 {@link ProjectMdResult}로 파싱</li>
 *   <li>요청서에 이미 확정된 필드(작성자, 부서 등)를 LLM 추론 결과 위에 덮어씀</li>
 *   <li>필요한 경우 {@link StandardMarkdownRenderer}로 최종 마크다운 생성</li>
 * </ol>
 *
 * @see DocService
 * @see GeminiLlmClient
 * @see ProjectMdResult
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {

    /**
     * JSON 파싱에 사용하는 공유 ObjectMapper 인스턴스.
     * LLM 응답에 알 수 없는 필드가 포함될 수 있으므로
     * {@code FAIL_ON_UNKNOWN_PROPERTIES}를 비활성화한다.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Gemini에 전달하는 시스템 지시문(system instruction).
     * 모델이 PG 연동 전문가 역할을 수행하도록 페르소나와 분석 목표를 정의한다.
     * — 결제창 연동, 인증/승인/취소/환불, 정산, 웹훅(노티) 처리, PCI-DSS 등 전 영역 포함.
     */
    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(Part.fromText(
            "당신은 국내외 PG(전자결제대행) 연동 전문가입니다. "
                    + "결제창 연동, 인증/승인/취소/환불, 정산, 웹훅(노티) 처리, 시큐어 코딩(PCI-DSS) 등 "
                    + "PG 도메인 전반을 깊이 이해하고 있습니다. "
                    + "역할은 개발요청서를 분석해 프로젝트 메타데이터를 정확하고 구현 가능한 형태의 JSON으로 추출/추론하는 것입니다."));

    /** Gemini API 호출 클라이언트 */
    private final GeminiLlmClient llmClient;

    /** Gemini 호출 파라미터(온도, 최대 토큰 수 등) 설정 */
    private final GeminiProperties geminiProperties;

    /** 개발요청서를 LLM 프롬프트 문자열로 조립하는 빌더 */
    private final ProjectPromptBuilder promptBuilder;

    /** {@link ProjectMdResult}를 표준 양식 마크다운으로 변환하는 렌더러 */
    private final StandardMarkdownRenderer markdownRenderer;

    /**
     * 개발요청서로부터 표준 양식 마크다운 문서를 생성한다.
     *
     * <p>처리 순서:
     * <ol>
     *   <li>프롬프트 생성 → Gemini 호출 → JSON 파싱</li>
     *   <li>확정 필드 덮어쓰기({@link #applyDeterministicFields})</li>
     *   <li>마크다운 렌더링</li>
     * </ol>
     *
     * @param request 프론트엔드 위저드에서 수집한 개발요청 정보
     * @return 표준 양식으로 렌더링된 마크다운 문자열
     */
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

    /**
     * 개발요청서로부터 프로젝트 메타데이터 JSON 구조체를 생성한다.
     *
     * <p>마크다운 렌더링 단계를 거치지 않고 LLM 응답 파싱 결과인
     * {@link ProjectMdResult}를 그대로 반환한다.
     *
     * @param request 프론트엔드 위저드에서 수집한 개발요청 정보
     * @return LLM이 추론한 프로젝트 메타데이터 구조체
     */
    @Override
    public ProjectMdResult assembleJson(DevRequestRequest request) {
        log.info("[DocService] JSON 결과 생성 시작 (author={}, serviceName={})",
                request.author(), request.serviceName());
        String prompt = promptBuilder.build(request, ReferenceLinks.ALL);
        return requestProjectMd(prompt);
    }

    /**
     * FE 위저드 자동 저장 흐름용 — Gemini 호출 1회로 JSON + MD 둘 다 생성.
     * (assembleJson + markdownRenderer 통과. LLM 추가 호출 X)
     *
     * <p>LLM을 한 번만 호출해 파싱된 {@link ProjectMdResult}와
     * 렌더링된 마크다운을 {@link AssembledDoc}으로 묶어 반환하므로
     * 두 번 호출하는 것보다 비용과 지연 모두 절감된다.
     *
     * @param request 프론트엔드 위저드에서 수집한 개발요청 정보
     * @return JSON 결과와 마크다운 문자열을 함께 담은 레코드
     */
    @Override
    public AssembledDoc assembleBoth(DevRequestRequest request) {
        log.info("[DocService] JSON+MD 동시 생성 시작 (author={}, serviceName={})",
                request.author(), request.serviceName());
        String prompt = promptBuilder.build(request, ReferenceLinks.ALL);
        ProjectMdResult result = requestProjectMd(prompt);
        applyDeterministicFields(result, request);
        String markdown = markdownRenderer.render(result);
        log.info("[DocService] JSON+MD 생성 완료 (length={})", markdown.length());
        return new AssembledDoc(result, markdown);
    }

    /**
     * 프롬프트를 LLM에 전송하고 응답을 {@link ProjectMdResult}로 파싱하는 내부 메서드.
     *
     * <p>처리 단계:
     * <ol>
     *   <li>Gemini 설정({@link #buildConfig}) 생성</li>
     *   <li>{@link GeminiLlmClient#generate}로 LLM 호출</li>
     *   <li>응답에서 코드 펜스 제거 후 JSON 파싱</li>
     * </ol>
     *
     * @param prompt LLM에 전달할 완성된 프롬프트 문자열
     * @return 파싱된 {@link ProjectMdResult} 객체 (파싱 실패 시 빈 객체 반환)
     */
    private ProjectMdResult requestProjectMd(String prompt) {
        GenerateContentConfig config = buildConfig();
        // LLM 호출: "메타데이터추출" 레이블은 로그 추적용
        String raw = llmClient.generate(prompt, config, "메타데이터추출");
        // 응답에 포함된 ```json ... ``` 등의 코드 펜스를 제거해 순수 JSON 추출
        String json = LlmResponseParser.stripCodeFence(raw);
        log.debug("[DocService] LLM 원본 JSON:\n{}", json);
        return parse(json);
    }

    /**
     * Gemini 생성 요청에 사용할 {@link GenerateContentConfig}를 구성한다.
     *
     * <p>시스템 지시문, 온도, 최대 출력 토큰을 설정하고
     * Google Search 도구를 활성화하여 모델이 외부 참조를 활용할 수 있게 한다.
     *
     * @return 완성된 Gemini 콘텐츠 생성 설정 객체
     */
    private GenerateContentConfig buildConfig() {
        return GenerateContentConfig.builder()
                .systemInstruction(SYSTEM_INSTRUCTION)
                .temperature(geminiProperties.temperature())
                .maxOutputTokens(geminiProperties.maxOutputTokens())
                // Google Search 도구 활성화: LLM이 최신 PG 스펙 문서를 검색할 수 있음
                .tools(Tool.builder().googleSearch(GoogleSearch.builder()).build())
                .build();
    }

    /**
     * 요청서에 이미 들어있는 확정 값은 LLM 추론보다 우선해 덮어쓴다.
     *
     * <p>LLM은 작성자·부서 같은 메타 필드를 잘못 추론할 수 있으므로
     * 요청 객체에 명시된 값을 항상 최종 결과에 반영한다.
     * <ul>
     *   <li>{@code author}, {@code department} — 항상 덮어씀</li>
     *   <li>{@code productName} — LLM 결과가 비어 있을 때만 요청값으로 보완</li>
     *   <li>{@code createdDate} — LLM 결과가 비어 있을 때 오늘 날짜로 보완</li>
     *   <li>{@code newServiceOrSelfImprovement} — funcType에 "신규" 포함 여부로 판단</li>
     * </ul>
     *
     * @param result 덮어쓸 LLM 파싱 결과 (in-place 수정)
     * @param req    원본 개발요청 DTO
     */
    private static void applyDeterministicFields(ProjectMdResult result, DevRequestRequest req) {
        // 작성자·부서는 입력값이 항상 정확하므로 무조건 덮어씀
        result.setAuthor(req.author());
        result.setDepartment(req.department());

        // LLM이 제품명을 비워 둔 경우에만 요청서의 서비스명으로 채움
        if (!StringUtils.hasText(result.getProductName())) {
            result.setProductName(req.serviceName());
        }
        // LLM이 날짜를 비워 둔 경우에만 오늘 날짜를 기본값으로 설정
        if (!StringUtils.hasText(result.getCreatedDate())) {
            result.setCreatedDate(LocalDate.now().toString());
        }
        // funcType에 "신규"가 포함되어 있으면 신규 서비스로, 그 외는 자체 개선으로 판단
        if (StringUtils.hasText(req.funcType())) {
            result.setNewServiceOrSelfImprovement(req.funcType().contains("신규"));
        }
    }

    /**
     * JSON 문자열을 {@link ProjectMdResult}로 역직렬화한다.
     *
     * <p>파싱 실패 또는 null 결과인 경우 로그를 남기고
     * 빈 객체({@link #emptyResult()})를 반환하여 NullPointerException을 방지한다.
     *
     * @param json 파싱 대상 JSON 문자열
     * @return 파싱 성공 시 {@link ProjectMdResult}, 실패 시 빈 인스턴스
     */
    private static ProjectMdResult parse(String json) {
        try {
            ProjectMdResult result = MAPPER.readValue(json, ProjectMdResult.class);
            return result != null ? result : emptyResult();
        } catch (Exception e) {
            log.error("[DocService] ProjectMdResult JSON 파싱 실패. raw=\n{}", json, e);
            return emptyResult();
        }
    }

    /**
     * 모든 필드가 기본값인 빈 {@link ProjectMdResult}를 생성한다.
     *
     * <p>파싱 실패 시 fallback으로 사용하며,
     * 빈 JSON "{}"을 역직렬화해 객체를 생성한다.
     *
     * @return 필드가 모두 null/기본값인 {@link ProjectMdResult} 인스턴스
     * @throws IllegalStateException 빈 JSON 파싱조차 실패한 경우 (환경 오류)
     */
    private static ProjectMdResult emptyResult() {
        try {
            return MAPPER.readValue("{}", ProjectMdResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("빈 ProjectMdResult 생성 실패", e);
        }
    }
}
