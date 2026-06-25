package com.nice.qa.service.llm;

import java.util.List;

/**
 * LLM 프롬프트에서 공통으로 사용하는 참조 문서 링크 목록을 관리하는 상수 클래스.
 *
 * <p>이 클래스는 Gemini LLM이 개발 요청서를 분석하거나 다이어그램을 생성할 때
 * "반드시 직접 열람하라"고 지시하는 참고 문서들의 GitHub Raw URL 목록을 정의한다.
 *
 * <p>LLM은 프롬프트에 포함된 링크를 직접 접근(URL Fetch)하여 문서 내용을 파악하고,
 * 그 지식을 바탕으로 더 정확한 요청서 구조화·다이어그램 추론을 수행한다.
 * 즉, 이 링크들이 LLM의 도메인 지식 소스(knowledge source) 역할을 한다.
 *
 * <p>문서 종류:
 * <ul>
 *   <li><b>README</b>: 지식베이스 전체 구조 안내</li>
 *   <li><b>catalog</b>: 위저드 분기 트리 — 기능 카테고리/서브타입 분류 체계</li>
 *   <li><b>policy</b>: PG 도메인 정책 — 결제 수단, 복합결제, 할부, 최소금액, 부분취소 등</li>
 *   <li><b>provider</b>: 원천사(카카오페이·네이버페이·페이코) 연동 규격 문서</li>
 *   <li><b>requests</b>: 개발 요청서 양식·작성 사례·이력</li>
 *   <li><b>spec</b>: 인증/승인/취소/전자서명 등 기술 명세서</li>
 *   <li><b>knowledge_base</b>: 원천사 상세 규격 및 결제창 UI 흐름, Web API 명세</li>
 * </ul>
 *
 * <p>{@code DocService}와 {@code FlowService} 양쪽이 동일한 링크 목록을 참조하므로
 * 이 클래스에서 일괄 관리한다. 문서가 추가/갱신되면 이 클래스만 수정하면 된다.
 *
 * <p>인스턴스화가 불필요한 상수 클래스이므로 생성자를 {@code private}으로 막는다.
 */
public final class ReferenceLinks {

    /** 인스턴스화 방지 — 정적 상수 클래스 */
    private ReferenceLinks() {}

    /**
     * 참조 문서의 GitHub 저장소 Base URL.
     * 모든 링크는 이 Base에 상대 경로를 붙여 구성된다.
     */
    private static final String BASE = "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/";

    /**
     * LLM 프롬프트에 주입되는 전체 참조 문서 링크 목록 (불변).
     *
     * <p>이 목록은 {@link com.nice.qa.service.llm.promt.ProjectPromptBuilder}와
     * {@link com.nice.qa.service.llm.promt.DiagramPromptBuilder}가 프롬프트를 조립할 때
     * "참고 링크" 섹션에 삽입되어, LLM이 각 URL을 직접 열람하도록 유도한다.
     *
     * <p>LLM이 링크를 열람하면:
     * <ul>
     *   <li>특정 원천사(카카오페이 등)의 연동 규격을 정확히 파악한다.</li>
     *   <li>결제 정책(최소금액, 부분취소 등)을 참고해 요청서 필드를 추론한다.</li>
     *   <li>기술 명세서(spec)를 기반으로 개발 범위·선행 조건을 구체화한다.</li>
     * </ul>
     */
    public static final List<String> ALL = List.of(
            // ── README ──────────────────────────────────────────────────────
            // 지식베이스 전체 구조와 문서 색인 안내 — LLM이 가장 먼저 참조해야 할 문서
            BASE + "README_KB.md",

            // ── 카탈로그 (위저드 분기 트리) ──────────────────────────────
            // 기능구분(funcType) / 카테고리(category) / 세부유형(subType) 분류 체계
            // 요청 입력의 분류를 정규화하는 데 사용
            BASE + "catalog/catalog_category_tree_v1.yaml",

            // ── 정책 문서 ────────────────────────────────────────────────
            // 결제 수단별 접수 양식 작성 가이드
            BASE + "policy/payment_method_intake_form_v1.md",
            // 복합결제(포인트+카드 혼합 등) 정책
            BASE + "policy/policy_composite_payment_v1.md",
            // CPID(채널 파트너 ID) 발급/관리 정책
            BASE + "policy/policy_cpid_v1.md",
            // 오류 코드 → 사용자 메시지 매핑 정책
            BASE + "policy/policy_error_message_mapping_v1.md",
            // 전체 정책 문서 색인
            BASE + "policy/policy_index.yaml",
            // 할부 결제 정책 (무이자 조건, 개월 수 제한 등)
            BASE + "policy/policy_installment_v1.md",
            // 연동 결제 수단 등록/관리 정책
            BASE + "policy/policy_linked_payment_method_v1.md",
            // 결제 최소 금액 정책
            BASE + "policy/policy_min_amount_v1.md",
            // 망취소(Net Cancel) 지원 정책
            BASE + "policy/policy_net_cancel_support_v1.md",
            // 신규 결제 수단 연동 시 체크리스트
            BASE + "policy/policy_new_payment_checklist_v1.md",
            // 부분취소 정책 (취소 우선순위, 복합결제 환불 방식 등)
            BASE + "policy/policy_partial_cancel_v1.md",
            // 서비스 제공 채널(PC/Mobile/App) 정책
            BASE + "policy/policy_target_channel_v1.md",
            // 정책 문서 작성 템플릿
            BASE + "policy/policy_template.md",
            // 결제/승인 타임아웃 정책
            BASE + "policy/policy_timeout_v1.md",

            // ── 원천사 문서 (provider) ───────────────────────────────────
            // 카카오페이 연동 규격 v2.0
            BASE + "provider/provider_kakaopay_v2.0.md",
            // 네이버페이 연동 규격 v1.5
            BASE + "provider/provider_naverpay_v1.5.md",
            // 페이코 연동 규격 v1.2
            BASE + "provider/provider_payco_v1.2.md",

            // ── 요청서 양식 및 사례 ──────────────────────────────────────
            // 실제 접수된 개발 요청서 사례 모음 — LLM 추론의 패턴 레퍼런스
            BASE + "requests/request_examples_v1.md",
            // 표준 개발 요청서 양식 템플릿
            BASE + "requests/request_form_template_v1.md",
            // 요청 이력(과거 접수 건 메타데이터) — 유사 사례 참조용
            BASE + "requests/request_history_v1.yaml",
            // 요청 유형 → 적용 정책 매핑 맵
            BASE + "requests/request_policy_trigger_map_v1.yaml",

            // ── 기술 명세서 (spec) ───────────────────────────────────────
            // 승인 API 명세 v2 (파라미터, 응답 코드, 에러 처리)
            BASE + "spec/spec_approval_v2.md",
            // 인증 API 명세 v2 (결제창 인증 흐름)
            BASE + "spec/spec_auth_v2.md",
            // 망취소 API 명세 v1
            BASE + "spec/spec_netcancel_v1.md",
            // 전자서명 데이터 명세 v2
            BASE + "spec/spec_signdata_v2.md",
            // 기술 명세서 작성 템플릿
            BASE + "spec/spec_template.md",

            // ── 지식베이스 (knowledge_base) — 원천사 상세 규격 ──────────
            // 카카오페이 상세 연동 지식베이스 v2.0
            BASE + "knowledge_base/provider/kb_provider_kakaopay_v2.0.md",
            // 네이버페이 상세 연동 지식베이스 v1.5
            BASE + "knowledge_base/provider/kb_provider_naverpay_v1.5.md",
            // 페이코 상세 연동 지식베이스 v1.2
            BASE + "knowledge_base/provider/kb_provider_payco_v1.2.md",

            // ── 지식베이스 — UI/결제창 흐름 ─────────────────────────────
            // 결제창 인증 흐름 상세 v3.2 — 시퀀스 다이어그램 추론의 핵심 참조 문서
            BASE + "knowledge_base/ui/kb_payment_window_auth_flow_v3.2.md",

            // ── 지식베이스 — Web API 명세 ────────────────────────────────
            // 빌링키 발급 Web API 명세 v2.8
            BASE + "knowledge_base/webapi/kb_webapi_billing_key_v2.8.md",
            // 취소(환불) Web API 명세 v2.8
            BASE + "knowledge_base/webapi/kb_webapi_cancel_v2.8.md",
            // 카드 키인(수기결제) Web API 명세 v2.8
            BASE + "knowledge_base/webapi/kb_webapi_card_keyin_v2.8.md",
            // 가상계좌 Web API 명세 v2.8
            BASE + "knowledge_base/webapi/kb_webapi_vbank_v2.8.md"
    );
}
