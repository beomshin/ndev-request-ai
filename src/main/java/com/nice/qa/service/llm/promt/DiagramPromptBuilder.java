package com.nice.qa.service.llm.promt;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 종합 MD + 참고 링크를 근거로 PlantUML 다이어그램을 추론하게 하는 프롬프트 빌더.
 */
@Component
public class DiagramPromptBuilder {

    public enum DiagramType {
        ACTIVITY("activity", "요청 접수부터 완료까지의 처리 흐름(분기/검증 포함)"),
        SEQUENCE("sequence", "현업/자사/PG사/카드사 등 주체 간 호출·응답 순서");

        private final String keyword;
        private final String hint;

        DiagramType(String keyword, String hint) {
            this.keyword = keyword;
            this.hint = hint;
        }
    }

    private static final String RULES = """
            # 규칙
            1. 위 '개발요청서 내용'과 '참고 링크'에서 실제 처리 흐름·주체·검증 단계를 추론한다.
            2. 결제 도메인 흐름(요청 접수 → 검증 → 인증/승인 → 매입/정산 → 취소·환불, 웹훅 노티 등)을 반영한다.
            3. 문서에 명시되지 않아 추론한 단계에는 노드/메시지 라벨 끝에 '(가정)'을 붙인다.
            4. 한국어 라벨을 사용한다.
            5. 출력은 PlantUML 코드 하나만 낸다. @startuml 로 시작해 @enduml 로 끝내고, 그 밖의 설명·머리말·코드펜스(백틱 3개)를 절대 붙이지 않는다.
            """;

    public String build(String requestMarkdown, List<String> links, DiagramType type) {
        return String.join("\n\n",
                header(type),
                referenceLinks(links),
                requestContent(requestMarkdown),
                RULES.strip(),
                skeleton(type));
    }

    private String header(DiagramType type) {
        return """
                # 작업
                아래 '개발요청서 내용'을 분석하고 '참고 링크'를 열람하여, 이 개발 건의 처리 흐름을 PlantUML %s 다이어그램으로 그린다.
                다이어그램 목적: %s""".formatted(type.keyword, type.hint);
    }

    private String referenceLinks(List<String> links) {
        String body = (links == null || links.isEmpty())
                ? "- (참고 링크 없음)"
                : links.stream().map(link -> "- " + link).collect(Collectors.joining("\n"));
        return "# 참고 링크 (반드시 직접 열람하여 흐름을 추론)\n" + body;
    }

    private String requestContent(String requestMarkdown) {
        return "# 개발요청서 내용\n" + (requestMarkdown == null ? "" : requestMarkdown.strip());
    }

    /** 모델이 형식을 벗어나지 않도록 최소 골격을 예시로 제공. */
    private String skeleton(DiagramType type) {
        if (type == DiagramType.SEQUENCE) {
            return """
                    # 출력 골격 (이 형태를 따른다)
                    @startuml
                    actor 현업
                    participant 자사 as Merchant
                    participant PG
                    participant 카드사 as Acquirer
                    현업 -> Merchant : 결제 요청
                    Merchant -> PG : 결제창 호출
                    PG -> Acquirer : 인증/승인 요청
                    Acquirer --> PG : 승인 결과
                    PG --> Merchant : 결과 노티(웹훅)
                    @enduml""";
        }
        return """
                # 출력 골격 (이 형태를 따른다)
                @startuml
                start
                :요청 접수;
                if (검증 통과?) then (yes)
                  :결제 연동;
                else (no)
                  :반려;
                  stop
                endif
                :완료;
                stop
                @enduml""";
    }

}
