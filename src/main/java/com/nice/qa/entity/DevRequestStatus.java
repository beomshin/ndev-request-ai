package com.nice.qa.entity;

/**
 * 개발요청서 상태.
 * 이 화면은 SHARE 붙여넣기용이라 개발 진행 단계(접수/검토중/승인 등)는 빼고,
 * 작성 단계만 두 가지로 시작한다 — 추후 필요 시 enum에 추가.
 */
public enum DevRequestStatus {
    /** 작성 중인 초안. 사용자가 [저장]만 누른 상태(아직 AI 분석 결과 없음). */
    DRAFT("작성중"),

    /** AI(Gemini) 분석 후 본문/추가확인 등이 채워진 상태. */
    AI_ANALYZED("AI분석완료");

    private final String label;

    DevRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
