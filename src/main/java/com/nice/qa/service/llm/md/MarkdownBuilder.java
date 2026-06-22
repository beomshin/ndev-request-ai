package com.nice.qa.service.llm.md;

/**
 * 마크다운 문자열을 fluent 하게 조립하는 헬퍼.
 * 내부에 가변 버퍼를 가지므로 렌더링 호출마다 새 인스턴스를 만들어 쓴다.
 */
public class MarkdownBuilder {

    private final StringBuilder sb = new StringBuilder();

    public MarkdownBuilder raw(String text) {
        sb.append(text);
        return this;
    }

    public MarkdownBuilder line(String text) {
        sb.append(text).append('\n');
        return this;
    }

    public MarkdownBuilder blank() {
        sb.append('\n');
        return this;
    }

    public MarkdownBuilder h1(String text) {
        return line("# " + text);
    }

    public MarkdownBuilder h2(String text) {
        return line("## " + text);
    }

    public MarkdownBuilder h3(String text) {
        return line("### " + text);
    }

    public MarkdownBuilder h4(String text) {
        return line("#### " + text);
    }

    /** "- **라벨:** 값" 한 줄. 값이 비면 빈칸, 여러 줄이면 라벨 아래로 들여쓴다. */
    public MarkdownBuilder field(String label, String value) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            return line("- **" + label + ":** ");
        }
        if (v.contains("\n")) {
            return line("- **" + label + ":**\n  " + v.replace("\n", "\n  "));
        }
        return line("- **" + label + ":** " + v);
    }

    /** "- [x] 라벨" 또는 "- [ ] 라벨". null/false 는 미체크. */
    public MarkdownBuilder checkbox(String label, Boolean checked) {
        return line((Boolean.TRUE.equals(checked) ? "- [x] " : "- [ ] ") + label);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
