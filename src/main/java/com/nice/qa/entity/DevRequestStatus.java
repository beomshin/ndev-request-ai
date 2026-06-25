package com.nice.qa.entity;

/**
 * 개발요청서의 작성 상태를 나타내는 열거형 (Enum representing the authoring state of a DevRequest).
 *
 * <p>이 시스템은 Confluence/SHARE 붙여넣기 전용 문서 생성 도구이므로
 * 실제 개발 진행 단계(접수 → 검토중 → 승인 → 완료 등)는 포함하지 않는다.
 * 현재는 문서 작성 단계만 두 가지 상태로 관리하며, 필요 시 enum 값을 추가·확장할 수 있다.
 *
 * <p>DB에는 {@code EnumType.STRING} 으로 저장되어 열거형 이름 그대로 보관된다.
 */
public enum DevRequestStatus {

    /**
     * 작성 중인 초안 상태 (Draft — request created but not yet AI-analyzed).
     *
     * <p>사용자가 위저드에서 [저장]만 누른 상태이다.
     * AI(Gemini) 분석이 아직 실행되지 않았으므로 {@code combinedMarkdown} 등의
     * AI 생성 필드가 비어 있을 수 있다.
     */
    DRAFT("작성중"),

    /**
     * AI 분석 완료 상태 (AI-analyzed — Gemini has generated the full document).
     *
     * <p>Gemini API 호출이 성공적으로 완료된 후 설정되는 상태이다.
     * {@code combinedMarkdown}, {@code flowDiagram}, {@code unconfirmedSection} 등
     * AI 생성 필드가 모두 채워진 상태이다.
     */
    AI_ANALYZED("AI분석완료");

    /**
     * UI에 표시되는 한글 레이블 (Korean label displayed in the UI).
     * DB에 저장되는 값이 아닌 순수 표시용 문자열이다.
     */
    private final String label;

    /**
     * 열거형 생성자 — 각 상수에 한글 레이블을 연결한다
     * (Constructor associating a Korean display label with each constant).
     *
     * @param label UI 표시용 한글 레이블 (Korean display label)
     */
    DevRequestStatus(String label) {
        this.label = label;
    }

    /**
     * UI 표시용 한글 레이블을 반환한다 (Returns the Korean display label for UI rendering).
     *
     * @return 한글 레이블 문자열 (e.g. {@code "작성중"}, {@code "AI분석완료"})
     */
    public String getLabel() {
        return label;
    }
}
