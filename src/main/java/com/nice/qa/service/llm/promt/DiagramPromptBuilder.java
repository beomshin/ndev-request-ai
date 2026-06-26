package com.nice.qa.service.llm.promt;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 개발 요청서 마크다운을 기반으로 draw.io 호환 다이어그램(mxGraphModel XML)을
 * LLM이 생성하도록 유도하는 프롬프트 빌더.
 *
 * <p>지원하는 다이어그램 유형:
 * <ul>
 *   <li>{@link DiagramType#SEQUENCE}: 기관 간 결제 승인/취소 메시지 흐름을 시간 순서로 표현하는 시퀀스 다이어그램</li>
 *   <li>{@link DiagramType#FLOWCHART}: 요청 접수부터 완료까지 처리 흐름(분기/검증 포함)을 표현하는 플로우차트</li>
 * </ul>
 *
 * <p>draw.io 전용 시퀀스 스텐실은 서버 사이드 JGraphX 렌더링이 불가능하므로,
 * 기본 도형(헤더 사각형 + 세로 점선 라이프라인 + 좌표 기반 가로 floating 화살표)으로
 * 시퀀스를 직접 구성하도록 LLM에 지시한다.
 *
 * <p>프롬프트 조립 구조 ({@link #build(String, List, DiagramType)} 기준):
 * <ol>
 *   <li>작업 헤더 — 다이어그램 유형·목적 설명</li>
 *   <li>참고 링크 목록 — LLM이 직접 열람해야 할 URL 목록</li>
 *   <li>개발요청서 내용 — 마크다운 형식의 구조화된 요청서</li>
 *   <li>규칙 — 유형별 좌표·도형·레이블 규칙 (SEQUENCE 또는 FLOWCHART)</li>
 *   <li>출력 골격 — LLM이 형식을 이탈하지 않도록 제공하는 mxGraphModel XML 예시</li>
 * </ol>
 *
 * <p>LLM의 응답은 {@code <mxGraphModel>}으로 시작하고 {@code </mxGraphModel>}으로 끝나는
 * 순수 XML이어야 한다. 코드펜스·설명·머리말이 포함된 경우
 * {@link com.nice.qa.service.llm.LlmResponseParser#stripCodeFence(String)}로 제거한다.
 *
 * @see com.nice.qa.service.llm.MxGraphRenderer  생성된 XML을 PNG로 렌더링하는 컴포넌트
 * @see com.nice.qa.service.llm.ReferenceLinks    프롬프트에 주입되는 참조 문서 링크 목록
 */
@Component
public class DiagramPromptBuilder {

    /**
     * LLM에게 생성을 요청할 다이어그램 유형.
     *
     * <p>각 유형은 프롬프트에 삽입될 키워드(keyword)와 LLM이 다이어그램의
     * 목적을 이해하는 데 사용할 설명(hint)을 가진다.
     */
    public enum DiagramType {
        /**
         * 거래 흐름 시퀀스 다이어그램.
         *
         * <p>가맹점·자사(PG)·인증기관·카드사 등 기관 간 결제 승인/취소 메시지를
         * 시간 순서(위→아래)로 표현한다. 각 기관은 헤더 사각형과 세로 라이프라인으로,
         * 메시지는 기관 사이를 잇는 가로 화살표로 표현된다.
         */
        SEQUENCE("거래 흐름 시퀀스", "가맹점·자사(PG)·인증기관·카드사 등 기관 간 결제 승인/취소 메시지를 시간 순서로 잇는 시퀀스"),

        /**
         * 처리 흐름 플로우차트 다이어그램.
         *
         * <p>요청 접수부터 완료까지의 단일 처리 흐름을 분기·검증 포함하여 표현한다.
         * 시작/종료는 타원, 처리 단계는 둥근 사각형, 분기는 마름모로 표현된다.
         */
        FLOWCHART("처리 흐름 플로우차트", "요청 접수부터 완료까지의 단일 처리 흐름(분기/검증 포함)");

        /** 프롬프트 헤더에 삽입될 다이어그램 유형 키워드 */
        private final String keyword;
        /** LLM이 다이어그램 목적을 이해하는 데 사용할 설명 문구 */
        private final String hint;

        DiagramType(String keyword, String hint) {
            this.keyword = keyword;
            this.hint = hint;
        }
    }

    /**
     * SEQUENCE 다이어그램 생성 시 LLM에 전달되는 좌표·도형·레이블 규칙.
     *
     * <p>JGraphX 서버 렌더링과의 호환성을 위해 floating edge(좌표 직접 지정) 방식을 사용한다.
     * LLM이 규칙을 이탈하지 않도록 구체적인 좌표 계산 방법까지 명시한다.
     * 추론 불확실 항목에는 "(가정)" 표기를 의무화하여 검토자가 식별할 수 있도록 한다.
     */
    private static final String SEQUENCE_RULES = """
            # 규칙
            1. 위 '개발요청서 내용'과 '참고 링크'에서 이 거래에 실제 참여하는 기관과 주고받는 메시지(요청/응답)를 추론한다.
            2. '거래 흐름 시퀀스'로 그린다. 일반적 참여 기관: 가맹점, 자사(PG), 인증기관, 카드사(승인/매입). 요청 성격에 맞게 가감한다.
            3. 승인 흐름(결제 요청 → 인증 → 승인 → 완료)과 취소/환불 흐름(취소 요청 → 승인취소 → 응답)을 모두 포함한다. 요청에 없는 흐름은 생략한다.
            4. 좌표 규칙 (반드시 준수):
               - 각 기관 헤더: 상단 사각형. y=20, height=40, width=140, x 는 40 부터 좌→우로 200씩 증가(40, 240, 440, 640 ...).
               - 라이프라인: 각 헤더 중앙 x 에서 세로 점선. floating edge(geometry 안 sourcePoint/targetPoint)로 y=60 부터 하단까지 내린다.
               - 메시지: 두 라이프라인 사이를 잇는 가로 floating edge. y 를 위→아래로 50씩 증가시켜 시간 순서대로 배치한다. 요청은 실선(endArrow=block), 응답은 점선(dashed=1;endArrow=open).
               - 모든 메시지 라벨 앞에 ①②③ 순번을 붙인다.
            5. 문서에 없어 추론한 메시지 라벨 끝에는 '(가정)'을 붙인다. 라벨은 한국어로 작성한다.
            6. root 에 id="0" 과 자식 id="1" 을 두고 모든 셀의 parent 는 "1". 헤더는 vertex="1", 라이프라인·메시지는 edge="1" 로 한다.
            7. 출력은 draw.io 호환 mxGraphModel XML 하나만 낸다. <mxGraphModel> 로 시작해 </mxGraphModel> 로 끝내고, <mxfile> 래퍼·압축·설명·머리말·코드펜스(백틱 3개)를 절대 붙이지 않는다.
            """;

    /**
     * FLOWCHART 다이어그램 생성 시 LLM에 전달되는 도형·방향·레이블 규칙.
     *
     * <p>표준 플로우차트 도형(타원·둥근 사각형·마름모)을 사용하며 위→아래 방향 레이아웃을 지시한다.
     * 추론 불확실 단계에는 "(가정)" 표기를 의무화한다.
     */
    private static final String FLOWCHART_RULES = """
            # 규칙
            1. '개발요청서 내용'과 '참고 링크'에서 실제 처리 흐름·검증·분기를 추론한다.
            2. 표준 플로우차트 도형을 사용한다: 시작/종료(ellipse), 처리(rounded=1), 분기(rhombus). 위→아래로 흐르게 좌표를 둔다.
            3. 문서에 없어 추론한 단계 라벨 끝에는 '(가정)'을 붙인다. 라벨은 한국어.
            4. root 에 id="0", 자식 id="1" 을 두고 모든 셀의 parent 는 "1".
            5. 출력은 draw.io 호환 mxGraphModel XML 하나만 낸다. <mxGraphModel> ~ </mxGraphModel> 외 텍스트·코드펜스(백틱 3개)를 붙이지 않는다.
            """;

    /**
     * 다이어그램 생성용 완성 프롬프트를 조립하여 반환한다.
     *
     * <p>조립 순서:
     * <ol>
     *   <li>{@link #header(DiagramType)} — 작업 목적 및 다이어그램 유형 명시</li>
     *   <li>{@link #referenceLinks(List)} — LLM이 열람해야 할 참조 문서 URL 목록</li>
     *   <li>{@link #requestContent(String)} — 분석 대상 개발요청서 마크다운</li>
     *   <li>유형별 규칙 — {@link #SEQUENCE_RULES} 또는 {@link #FLOWCHART_RULES}</li>
     *   <li>{@link #skeleton(DiagramType)} — 출력 형식 골격 XML 예시</li>
     * </ol>
     *
     * <p>각 섹션은 {@code \n\n}으로 구분되어 LLM이 섹션 경계를 명확히 인식하도록 한다.
     *
     * @param requestMarkdown {@link com.nice.qa.service.llm.md.StandardMarkdownRenderer}가
     *                        렌더링한 개발요청서 마크다운 문자열
     * @param links           {@link com.nice.qa.service.llm.ReferenceLinks#ALL} 등 참조 문서 URL 목록.
     *                        {@code null} 또는 빈 리스트 허용.
     * @param type            생성할 다이어그램 유형 ({@link DiagramType#SEQUENCE} 또는 {@link DiagramType#FLOWCHART})
     * @return LLM에 전달할 완성된 프롬프트 문자열
     */
    public String build(String requestMarkdown, List<String> links, DiagramType type) {
        return String.join("\n\n",
                header(type),
                referenceLinks(links),
                requestContent(requestMarkdown),
                // 다이어그램 유형에 따라 서로 다른 규칙 섹션을 선택하여 삽입
                (type == DiagramType.SEQUENCE ? SEQUENCE_RULES : FLOWCHART_RULES).strip(),
                skeleton(type));
    }

    /**
     * 프롬프트 헤더 섹션을 생성한다.
     *
     * <p>LLM에게 작업 목적, 다이어그램 유형·키워드, 목적 설명을 전달한다.
     * 이 섹션이 LLM의 전체 응답 방향을 결정하는 중요한 역할을 한다.
     *
     * @param type 생성할 다이어그램 유형
     * @return 작업 헤더 문자열
     */
    private String header(DiagramType type) {
        return """
                # 작업
                아래 '개발요청서 내용'을 분석하고 '참고 링크'를 열람하여, 이 개발 건의 거래/처리 흐름을 draw.io 호환 다이어그램(mxGraphModel XML)으로 그린다.
                다이어그램 형태: %s
                목적: %s""".formatted(type.keyword, type.hint);
    }

    /**
     * 참고 링크 섹션을 생성한다.
     *
     * <p>LLM에게 "반드시 직접 열람하라"고 지시하는 참조 문서 URL 목록을 불릿 형식으로 출력한다.
     * LLM은 이 링크들을 직접 열람하여 거래 흐름 추론의 정확도를 높인다.
     *
     * @param links 참조 문서 URL 목록. {@code null} 또는 빈 리스트인 경우 "(참고 링크 없음)" 출력.
     * @return 참고 링크 섹션 문자열
     */
    private String referenceLinks(List<String> links) {
        // 링크가 없는 경우 LLM이 링크 없이 추론하도록 안내
        String body = (links == null || links.isEmpty())
                ? "- (참고 링크 없음)"
                : links.stream().map(link -> "- " + link).collect(Collectors.joining("\n"));
        return "# 참고 링크 (반드시 직접 열람하여 흐름을 추론)\n" + body;
    }

    /**
     * 개발요청서 내용 섹션을 생성한다.
     *
     * <p>LLM이 분석할 대상 개발요청서 마크다운을 프롬프트에 삽입한다.
     * null인 경우 빈 내용으로 처리하여 프롬프트 구조는 유지된다.
     *
     * @param requestMarkdown 구조화된 개발요청서 마크다운 문자열. {@code null} 허용.
     * @return 개발요청서 내용 섹션 문자열
     */
    private String requestContent(String requestMarkdown) {
        return "# 개발요청서 내용\n" + (requestMarkdown == null ? "" : requestMarkdown.strip());
    }

    /**
     * LLM이 출력 형식을 이탈하지 않도록 mxGraphModel XML 골격 예시를 제공한다.
     *
     * <p>LLM은 Few-shot 예시에 민감하게 반응하므로, 실제 사용 가능한 완전한 XML 골격을
     * 제공하여 모델이 동일한 구조·좌표 체계·스타일을 따르도록 유도한다.
     * SEQUENCE와 FLOWCHART는 구조가 완전히 다르므로 별도의 골격을 제공한다.
     *
     * @param type 생성할 다이어그램 유형
     * @return 다이어그램 유형에 맞는 mxGraphModel XML 골격 예시 문자열
     */
    private String skeleton(DiagramType type) {
        if (type == DiagramType.FLOWCHART) {
            return """
                    # 출력 골격 (이 구조와 스타일을 따른다)
                    <mxGraphModel>
                      <root>
                        <mxCell id="0"/>
                        <mxCell id="1" parent="0"/>
                        <mxCell id="start" value="요청 접수" style="ellipse;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;" vertex="1" parent="1">
                          <mxGeometry x="240" y="40" width="140" height="50" as="geometry"/>
                        </mxCell>
                        <mxCell id="check" value="검증 통과?" style="rhombus;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;" vertex="1" parent="1">
                          <mxGeometry x="250" y="140" width="120" height="80" as="geometry"/>
                        </mxCell>
                        <mxCell id="end" value="완료" style="ellipse;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;" vertex="1" parent="1">
                          <mxGeometry x="240" y="270" width="140" height="50" as="geometry"/>
                        </mxCell>
                        <mxCell id="e1" style="edgeStyle=orthogonalEdgeStyle;html=1;" edge="1" parent="1" source="start" target="check">
                          <mxGeometry relative="1" as="geometry"/>
                        </mxCell>
                        <mxCell id="e2" value="예" style="edgeStyle=orthogonalEdgeStyle;html=1;" edge="1" parent="1" source="check" target="end">
                          <mxGeometry relative="1" as="geometry"/>
                        </mxCell>
                      </root>
                    </mxGraphModel>""";
        }
        // SEQUENCE: 기관 헤더 + 세로 라이프라인 + 가로 메시지(floating edge)
        return """
                # 출력 골격 (이 구조·좌표·스타일을 그대로 따른다. 기관/메시지는 요청 내용에 맞게 구성)
                <mxGraphModel>
                  <root>
                    <mxCell id="0"/>
                    <mxCell id="1" parent="0"/>

                    <mxCell id="p1" value="가맹점" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;" vertex="1" parent="1">
                      <mxGeometry x="40" y="20" width="140" height="40" as="geometry"/>
                    </mxCell>
                    <mxCell id="p2" value="자사(PG)" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;" vertex="1" parent="1">
                      <mxGeometry x="240" y="20" width="140" height="40" as="geometry"/>
                    </mxCell>
                    <mxCell id="p3" value="인증기관" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;" vertex="1" parent="1">
                      <mxGeometry x="440" y="20" width="140" height="40" as="geometry"/>
                    </mxCell>
                    <mxCell id="p4" value="카드사(승인/매입)" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;" vertex="1" parent="1">
                      <mxGeometry x="640" y="20" width="140" height="40" as="geometry"/>
                    </mxCell>

                    <mxCell id="l1" style="endArrow=none;dashed=1;html=1;strokeColor=#999999;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="110" y="60" as="sourcePoint"/><mxPoint x="110" y="540" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="l2" style="endArrow=none;dashed=1;html=1;strokeColor=#999999;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="310" y="60" as="sourcePoint"/><mxPoint x="310" y="540" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="l3" style="endArrow=none;dashed=1;html=1;strokeColor=#999999;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="510" y="60" as="sourcePoint"/><mxPoint x="510" y="540" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="l4" style="endArrow=none;dashed=1;html=1;strokeColor=#999999;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="710" y="60" as="sourcePoint"/><mxPoint x="710" y="540" as="targetPoint"/></mxGeometry>
                    </mxCell>

                    <mxCell id="m1" value="① 결제 요청" style="html=1;endArrow=block;rounded=0;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="110" y="100" as="sourcePoint"/><mxPoint x="310" y="100" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="m2" value="② 인증 요청" style="html=1;endArrow=block;rounded=0;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="310" y="150" as="sourcePoint"/><mxPoint x="510" y="150" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="m3" value="③ 인증 결과" style="html=1;endArrow=open;dashed=1;rounded=0;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="510" y="200" as="sourcePoint"/><mxPoint x="310" y="200" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="m4" value="④ 승인 요청" style="html=1;endArrow=block;rounded=0;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="310" y="250" as="sourcePoint"/><mxPoint x="710" y="250" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="m5" value="⑤ 승인 응답" style="html=1;endArrow=open;dashed=1;rounded=0;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="710" y="300" as="sourcePoint"/><mxPoint x="310" y="300" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="m6" value="⑥ 결제 완료" style="html=1;endArrow=open;dashed=1;rounded=0;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="310" y="350" as="sourcePoint"/><mxPoint x="110" y="350" as="targetPoint"/></mxGeometry>
                    </mxCell>

                    <mxCell id="m7" value="⑦ 취소 요청" style="html=1;endArrow=block;rounded=0;strokeColor=#b85450;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="110" y="420" as="sourcePoint"/><mxPoint x="310" y="420" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="m8" value="⑧ 승인취소 요청" style="html=1;endArrow=block;rounded=0;strokeColor=#b85450;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="310" y="470" as="sourcePoint"/><mxPoint x="710" y="470" as="targetPoint"/></mxGeometry>
                    </mxCell>
                    <mxCell id="m9" value="⑨ 취소 응답" style="html=1;endArrow=open;dashed=1;rounded=0;strokeColor=#b85450;" edge="1" parent="1">
                      <mxGeometry relative="1" as="geometry"><mxPoint x="710" y="520" as="sourcePoint"/><mxPoint x="310" y="520" as="targetPoint"/></mxGeometry>
                    </mxCell>
                  </root>
                </mxGraphModel>""";
    }
}