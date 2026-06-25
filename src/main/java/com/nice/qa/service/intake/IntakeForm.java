package com.nice.qa.service.intake;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 신규 지불수단 등록 입력 폼 스키마.
 * docs/policy/payment_method_intake_form_v1.md 의 frontmatter를 그대로 노출.
 * FE가 inputType별 위젯으로 동적 렌더링한다.
 *
 * <p>이 레코드는 YAML frontmatter에서 파싱된 폼 스키마를 API 응답으로 직렬화하기 위한
 * 불변 데이터 컨테이너이다. {@code @JsonInclude(NON_NULL)} 설정으로
 * null 필드는 JSON 직렬화 결과에서 제외되어 응답 크기를 줄인다.
 *
 * <h3>FE 렌더링 규칙</h3>
 * 프론트엔드는 {@link Field#inputType} 값에 따라 아래와 같이 위젯을 선택한다.
 * <ul>
 *   <li>{@code text} — 단일 행 텍스트 입력</li>
 *   <li>{@code number} — 숫자 입력</li>
 *   <li>{@code boolean} — 체크박스 또는 토글</li>
 *   <li>{@code select} — 단일 선택 드롭다운 ({@link Field#options} 목록 사용)</li>
 *   <li>{@code multiselect} — 복수 선택 드롭다운 ({@link Field#options} 목록 사용)</li>
 *   <li>{@code group} — 중첩 필드 그룹 ({@link Field#fields} 재귀 렌더링)</li>
 * </ul>
 *
 * @param docId    정책 문서 식별자 (예: {@code payment_method_intake_form_v1})
 * @param title    폼 제목
 * @param version  폼 스키마 버전
 * @param sections 폼을 구성하는 섹션 목록 (순서 있음)
 * @param fields   폼 입력 필드 전체 목록
 *
 * @see IntakeFormService
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IntakeForm(
        String docId,
        String title,
        String version,
        List<Section> sections,
        List<Field> fields
) {

    /**
     * 폼의 논리적 구분 단위를 나타내는 섹션 레코드.
     *
     * <p>여러 {@link Field}를 하나의 섹션으로 묶어 FE에서 그룹 UI를 렌더링할 수 있도록 한다.
     * {@code order} 필드를 기준으로 섹션 표시 순서가 결정된다.
     *
     * @param code  섹션 고유 코드 (Field의 {@code section} 참조 키와 일치)
     * @param name  섹션 표시명 (한국어)
     * @param order 섹션 표시 순서 (오름차순 정렬)
     */
    public record Section(String code, String name, int order) {}

    /**
     * 폼의 단일 입력 필드를 나타내는 레코드.
     *
     * <p>null 필드는 JSON 직렬화 시 제외된다({@code @JsonInclude(NON_NULL)}).
     * {@code inputType}이 {@code group}인 경우 {@link #fields} 리스트에
     * 중첩된 하위 필드들이 포함되며, FE는 이를 재귀적으로 렌더링한다.
     *
     * @param policyId      정책 문서 내 필드 고유 식별자
     * @param section       이 필드가 속한 섹션 코드 ({@link Section#code}와 매핑)
     * @param label         필드 레이블 (한국어 표시명)
     * @param inputType     입력 위젯 타입: text | number | boolean | select | multiselect | group
     * @param required      필수 입력 여부 (null이면 선택 사항으로 간주)
     * @param options        select / multiselect 전용 선택지 목록
     * @param defaultValue  기본값 — 타입이 다양하므로(Boolean, Number, String) Object로 선언
     * @param placeholder   입력 힌트 텍스트 (text/number 전용)
     * @param helpText      필드 하단 도움말 텍스트
     * @param pattern       입력값 검증용 정규식 패턴
     * @param format        입력 형식 힌트 (예: {@code YYYY-MM-DD})
     * @param unit          입력 단위 표시 (예: {@code 원}, {@code ms})
     * @param maxLength     최대 입력 길이 (text 전용)
     * @param key            group 안 nested field 식별 (예: PAYMENT_WINDOW, ko/en/zh)
     * @param fields         group 일 때 nested 하위 필드 목록 (재귀 구조)
     * @param sourceDocId   이 필드 정의가 참조하는 원본 정책 문서 ID
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Field(
            String policyId,
            String section,
            String label,
            String inputType,        // text | number | boolean | select | multiselect | group
            Boolean required,
            List<String> options,    // select / multiselect 전용
            Object defaultValue,     // 타입 다양 — 그대로 노출
            String placeholder,
            String helpText,
            String pattern,
            String format,
            String unit,
            Integer maxLength,
            String key,              // group 안 nested field 식별 (예: PAYMENT_WINDOW, ko/en/zh)
            List<Field> fields,      // group 일 때 nested
            String sourceDocId
    ) {}
}
