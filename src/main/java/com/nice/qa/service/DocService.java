package com.nice.qa.service;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.llm.dto.ProjectMdResult;

/**
 * 개발요청서를 기반으로 AI 문서를 생성하는 서비스 인터페이스.
 *
 * <p>이 인터페이스는 세 가지 생성 모드를 제공한다.
 * <ul>
 *   <li>{@link #assembleMarkdown} — 표준 양식 마크다운 문서만 반환</li>
 *   <li>{@link #assembleJson} — 프로젝트 메타데이터 JSON 구조체만 반환</li>
 *   <li>{@link #assembleBoth} — LLM 1회 호출로 JSON + 마크다운을 한 번에 반환 (권장)</li>
 * </ul>
 *
 * <p>구현체는 Gemini LLM을 통해 PG 연동 전문가 관점에서 요청서를 분석하며,
 * 결과는 {@link ProjectMdResult} 및 표준 마크다운 문자열로 제공된다.
 *
 * @see com.nice.qa.service.impl.DocServiceImpl
 */
public interface DocService {

    /**
     * 개발요청서로부터 표준 양식 마크다운 문서를 생성한다.
     *
     * <p>내부적으로 Gemini LLM에 프롬프트를 전송하고,
     * 반환된 JSON 결과를 {@code StandardMarkdownRenderer}를 통해
     * 마크다운 문자열로 변환한다.
     *
     * @param request 프론트엔드 위저드에서 수집한 개발요청 정보
     * @return 표준 양식으로 렌더링된 마크다운 문자열
     */
    String assembleMarkdown(DevRequestRequest request);

    /**
     * 개발요청서로부터 프로젝트 메타데이터 JSON 구조체를 생성한다.
     *
     * <p>마크다운 렌더링 없이 {@link ProjectMdResult} 객체만 반환한다.
     * 마크다운 문서까지 필요한 경우 {@link #assembleBoth}를 사용하는 것이 효율적이다.
     *
     * @param request 프론트엔드 위저드에서 수집한 개발요청 정보
     * @return LLM이 추론한 프로젝트 메타데이터 구조체
     */
    ProjectMdResult assembleJson(DevRequestRequest request);

    /**
     * Gemini 1회 호출로 JSON과 표준 양식 MD를 한 번에 만든다.
     * FE 위저드 → /api/requests 자동 저장 흐름에서 markdown 필드까지 같이 응답하기 위해 사용.
     *
     * <p>LLM을 두 번 호출하지 않고 한 번의 추론 결과로 JSON 파싱과
     * 마크다운 렌더링을 모두 수행하므로 응답 비용과 지연을 최소화한다.
     *
     * @param request 프론트엔드 위저드에서 수집한 개발요청 정보
     * @return JSON 결과와 마크다운 문자열을 함께 담은 {@link AssembledDoc} 레코드
     */
    AssembledDoc assembleBoth(DevRequestRequest request);

    /**
     * JSON 결과 + 표준 양식 MD 한 묶음.
     *
     * <p>{@link #assembleBoth}의 반환 타입으로, 단일 LLM 호출 결과를
     * 구조체(result)와 렌더링된 마크다운(markdown)으로 함께 전달한다.
     *
     * @param result   LLM이 추론한 프로젝트 메타데이터 구조체
     * @param markdown 표준 양식으로 렌더링된 마크다운 문자열
     */
    record AssembledDoc(ProjectMdResult result, String markdown) {}
}
