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
            BASE + "catalog/catalog_category_tree_v1.yaml",
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
            BASE + "provider/provider_kakaopay_v2.0.md",
            BASE + "provider/provider_naverpay_v1.5.md",
            BASE + "provider/provider_payco_v1.2.md",
            BASE + "requests/request_examples_v1.md",
            BASE + "requests/request_form_template_v1.md",
            BASE + "requests/request_history_v1.yaml",
            BASE + "requests/request_policy_trigger_map_v1.yaml",
            BASE + "spec/spec_approval_v2.md",
            BASE + "spec/spec_auth_v2.md",
            BASE + "spec/spec_netcancel_v1.md",
            BASE + "spec/spec_signdata_v2.md",
            BASE + "spec/spec_template.md",
            BASE + "README_KB.md"
    );
}
