package com.nice.qa.service.llm.md;

/**
 * 마크다운 문자열을 fluent 스타일로 조립하는 헬퍼 클래스.
 *
 * <p>내부에 가변 {@link StringBuilder} 버퍼를 가지므로, 렌더링 호출마다 새 인스턴스를
 * 생성해서 사용해야 한다. 스레드 안전성을 보장하지 않으므로 단일 스레드에서 사용한다.
 *
 * <p>각 메서드는 {@code this}를 반환하여 메서드 체이닝(method chaining)이 가능하다.
 *
 * <p>사용 예:
 * <pre>
 * String md = new MarkdownBuilder()
 *     .h1("개발 요청서")
 *     .blank()
 *     .h2("1. 요청 정보")
 *     .field("작성자", "홍길동")
 *     .field("부서", "PG개발실")
 *     .checkbox("가맹점 관련", true)
 *     .toString();
 * </pre>
 *
 * @see StandardMarkdownRenderer 이 클래스를 사용하는 마크다운 렌더러
 */
public class MarkdownBuilder {

    /** 마크다운 문자열을 누적하는 내부 버퍼 */
    private final StringBuilder sb = new StringBuilder();

    /**
     * 개행 없이 텍스트를 그대로 버퍼에 추가한다.
     *
     * <p>줄바꿈을 직접 제어하거나, 복잡한 형식의 블록을 직접 작성할 때 사용한다.
     *
     * @param text 추가할 원문 텍스트
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder raw(String text) {
        sb.append(text);
        return this;
    }

    /**
     * 텍스트 뒤에 개행 문자({@code \n})를 붙여 한 줄로 추가한다.
     *
     * @param text 추가할 텍스트 (마지막에 {@code \n} 자동 추가)
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder line(String text) {
        sb.append(text).append('\n');
        return this;
    }

    /**
     * 빈 줄({@code \n})을 추가한다.
     *
     * <p>마크다운에서 섹션 사이의 빈 줄은 단락을 구분하는 역할을 한다.
     *
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder blank() {
        sb.append('\n');
        return this;
    }

    /**
     * H1 레벨 헤딩({@code # 텍스트})을 한 줄로 추가한다.
     *
     * @param text 헤딩 텍스트
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder h1(String text) {
        return line("# " + text);
    }

    /**
     * H2 레벨 헤딩({@code ## 텍스트})을 한 줄로 추가한다.
     *
     * @param text 헤딩 텍스트
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder h2(String text) {
        return line("## " + text);
    }

    /**
     * H3 레벨 헤딩({@code ### 텍스트})을 한 줄로 추가한다.
     *
     * @param text 헤딩 텍스트
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder h3(String text) {
        return line("### " + text);
    }

    /**
     * H4 레벨 헤딩({@code #### 텍스트})을 한 줄로 추가한다.
     *
     * @param text 헤딩 텍스트
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder h4(String text) {
        return line("#### " + text);
    }

    /**
     * {@code - **라벨:** 값} 형식의 필드 한 줄을 추가한다.
     *
     * <p>처리 규칙:
     * <ul>
     *   <li>값이 {@code null}이거나 공백 전용이면 빈 값으로 출력: {@code - **라벨:** }</li>
     *   <li>값에 개행({@code \n})이 포함된 경우 라벨 다음 줄에 2칸 들여쓰기로 이어쓴다:
     *       <pre>
     *       - **라벨:**
     *         첫 번째 줄
     *         두 번째 줄
     *       </pre>
     *   </li>
     *   <li>단일 줄 값은 인라인으로 출력: {@code - **라벨:** 값}</li>
     * </ul>
     *
     * @param label 필드 라벨 (볼드체로 표시)
     * @param value 필드 값 ({@code null} 허용)
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder field(String label, String value) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            // 값이 없는 경우 빈 필드로 출력 (마크다운에서 공란임을 명시)
            return line("- **" + label + ":** ");
        }
        if (v.contains("\n")) {
            // 여러 줄 값: 라벨 아래에 2칸 들여쓰기로 이어쓰기
            // replace("\n", "\n  ") → 각 줄에 2칸 들여쓰기 적용
            return line("- **" + label + ":**\n  " + v.replace("\n", "\n  "));
        }
        // 단일 줄 값: 인라인 출력
        return line("- **" + label + ":** " + v);
    }

    /**
     * 체크박스 항목({@code - [x] 라벨} 또는 {@code - [ ] 라벨})을 한 줄로 추가한다.
     *
     * <p>마크다운 GFM(GitHub Flavored Markdown) 태스크 리스트 형식을 사용한다.
     * {@link com.nice.qa.service.llm.dto.ProjectMdResult}의 Boolean 필드(개발 유형 체크)를
     * 체크박스로 렌더링할 때 사용된다.
     *
     * @param label   체크박스 레이블 텍스트
     * @param checked {@code true}이면 체크됨({@code [x]}), {@code false} 또는 {@code null}이면 미체크({@code [ ]})
     * @return 메서드 체이닝을 위한 {@code this}
     */
    public MarkdownBuilder checkbox(String label, Boolean checked) {
        // Boolean.TRUE.equals()로 null-safe 비교 — null은 미체크로 처리
        return line((Boolean.TRUE.equals(checked) ? "- [x] " : "- [ ] ") + label);
    }

    /**
     * 누적된 마크다운 문자열 전체를 반환한다.
     *
     * @return 빌드된 마크다운 문자열
     */
    @Override
    public String toString() {
        return sb.toString();
    }
}
