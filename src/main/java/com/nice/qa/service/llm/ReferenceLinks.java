package com.nice.qa.service.llm;

import java.util.List;

/**
 * LLM 프롬프트에서 공통으로 사용하는 참조 문서 링크 목록.
 * DocService / FlowService 양쪽이 동일한 링크를 참조하므로 여기서 일괄 관리한다.
 */
public final class ReferenceLinks {

    private ReferenceLinks() {}

    private static final String BASE = "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/";

    public static final List<String> ALL = List.of(
            // ── README ──────────────────────────────────────────────────────
            BASE + "README_KB.md",

            // ── 카탈로그 (위저드 분기 트리) ──────────────────────────────
            BASE + "catalog/catalog_category_tree_v1.yaml",

            // ── 정책 문서 ────────────────────────────────────────────────
            BASE + "policy/payment_method_intake_form_v1.md",
            BASE + "policy/policy_composite_payment_v1.md",
            BASE + "policy/policy_cpid_v1.md",
            BASE + "policy/policy_error_message_mapping_v1.md",
            BASE + "policy/policy_index.yaml",
            BASE + "policy/policy_installment_v1.md",
            BASE + "policy/policy_linked_payment_method_v1.md",
            BASE + "policy/policy_min_amount_v1.md",
            BASE + "policy/policy_net_cancel_support_v1.md",
            BASE + "policy/policy_new_payment_checklist_v1.md",
            BASE + "policy/policy_partial_cancel_v1.md",
            BASE + "policy/policy_target_channel_v1.md",
            BASE + "policy/policy_template.md",
            BASE + "policy/policy_timeout_v1.md",

            // ── 원천사 문서 (provider) ───────────────────────────────────
            BASE + "provider/provider_kakaopay_v2.0.md",
            BASE + "provider/provider_naverpay_v1.5.md",
            BASE + "provider/provider_payco_v1.2.md",

            // ── 요청서 양식 및 사례 ──────────────────────────────────────
            BASE + "requests/request_examples_v1.md",
            BASE + "requests/request_form_template_v1.md",
            BASE + "requests/request_history_v1.yaml",
            BASE + "requests/request_policy_trigger_map_v1.yaml",

            // ── 기술 명세서 (spec) ───────────────────────────────────────
            BASE + "spec/spec_approval_v2.md",
            BASE + "spec/spec_auth_v2.md",
            BASE + "spec/spec_netcancel_v1.md",
            BASE + "spec/spec_signdata_v2.md",
            BASE + "spec/spec_template.md",

            // ── 지식베이스 (knowledge_base) — 원천사 상세 규격 ──────────
            BASE + "knowledge_base/provider/kb_provider_kakaopay_v2.0.md",
            BASE + "knowledge_base/provider/kb_provider_naverpay_v1.5.md",
            BASE + "knowledge_base/provider/kb_provider_payco_v1.2.md",

            // ── 지식베이스 — UI/결제창 흐름 ─────────────────────────────
            BASE + "knowledge_base/ui/kb_payment_window_auth_flow_v3.2.md",

            // ── 지식베이스 — Web API 명세 ────────────────────────────────
            BASE + "knowledge_base/webapi/kb_webapi_billing_key_v2.8.md",
            BASE + "knowledge_base/webapi/kb_webapi_cancel_v2.8.md",
            BASE + "knowledge_base/webapi/kb_webapi_card_keyin_v2.8.md",
            BASE + "knowledge_base/webapi/kb_webapi_vbank_v2.8.md"
    );
}
