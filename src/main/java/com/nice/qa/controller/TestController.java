package com.nice.qa.controller;

import com.google.genai.Client;
import com.google.genai.types.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    // 출력에 포함할 최상위 항목 (4번: 여기 적은 것만 나갑니다)
    private static final List<String> REQUESTED_FIELDS = List.of(
            "작성일", "작성자", "제목", "개요", "참고링크", "요구사항"
    );

    @GetMapping("/test")
    public String test() {

        Client client = Client.builder()
                .apiKey("AIzaSyCW-Hw6MjHY5j3H1UXWn4XNvOxXZeICNtc")
                .build();

        // 1) 역할: PG 전문가
        Content systemInstruction = Content.fromParts(Part.fromText(
                "당신은 국내외 PG(전자결제대행) 연동 전문가입니다. "
                        + "결제창 연동, 인증/승인/취소/환불, 정산, 웹훅(노티) 처리, 시큐어 코딩(PCI-DSS) 등 "
                        + "PG 도메인 전반을 깊이 이해하고 있습니다. "
                        + "역할은 개발팀에게 전달할 '개발 요청사항' 문서를 정확하고 구현 가능한 형태로 작성하는 것입니다."
        ));

        // 2,5) 링크 직접 읽기(UrlContext) + 부족분 검색 보완(GoogleSearch)
        Tool urlContextTool = Tool.builder().urlContext(UrlContext.builder()).build();
        Tool googleSearchTool = Tool.builder().googleSearch(GoogleSearch.builder()).build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstruction)
                .temperature(0.3f)          // 요구사항 문서는 일관/정확이 중요 → 낮게
                .maxOutputTokens(8192)
                .tools(urlContextTool, googleSearchTool)
                .build();

        // 분석할 참고 링크 (2번 입력)
        List<String> links = List.of(
                "https://example-pg.com/docs/payment-window",
                "https://example-pg.com/docs/webhook"
        );

        String prompt = buildRequirementPrompt(
                "○○ 결제 연동",                  // 제목 힌트
                "신규 가맹점 PG 결제창 연동",       // 작업 맥락
                links,
                REQUESTED_FIELDS
        );

        GenerateContentResponse response = client.models.generateContent(
                "gemini-2.5-flash",
                prompt,
                config
        );

        String json = stripCodeFence(response.text());
        System.out.println("=== 생성된 JSON (MD 변환용) ===");
        System.out.println(json);

        return json;
    }

    /**
     * 개발 요청사항 생성용 프롬프트 조립.
     * 역할/입력/작업/규칙/출력형식을 명확히 구분해 의도 오해를 줄입니다.
     */
    private static String buildRequirementPrompt(String titleHint,
                                                 String context,
                                                 List<String> links,
                                                 List<String> requestedFields) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 작업\n")
                .append("아래 참고 링크의 내용을 읽고 추론하여, PG 연동을 위한 '개발 요청사항' 문서를 작성한다.\n")
                .append("제목 힌트: ").append(titleHint).append("\n")
                .append("맥락: ").append(context).append("\n\n");

        sb.append("# 참고 링크 (반드시 직접 열람하여 내용을 추론)\n");
        for (String link : links) {
            sb.append("- ").append(link).append("\n");
        }
        sb.append("\n");

        sb.append("# 규칙\n")
                .append("1. 링크에서 추론 가능한 내용을 최우선으로 반영한다.\n")
                .append("2. 링크에 없거나 불충분한 내용은 PG 일반 지식과 웹 검색으로 보완한다.\n")
                .append("3. 각 요구사항 항목에는 출처를 표기한다: \"링크추론\" | \"보완\" | \"혼합\".\n")
                .append("4. 추측이 불가피한 부분은 상세 내용에 '(가정)'을 명시한다.\n")
                .append("5. 아래 '출력 항목'에 명시된 키만 출력하고, 그 외 어떤 키도 추가하지 않는다.\n")
                .append("6. 응답은 오직 JSON 하나만 출력한다. 코드펜스(```), 설명 문장, 머리말을 절대 붙이지 않는다.\n\n");

        sb.append("# 출력 항목 (이 키들만 포함)\n");
        for (String f : requestedFields) {
            sb.append("- ").append(f).append("\n");
        }
        sb.append("\n");

        sb.append("# 출력 형식 (JSON 스키마 — 이 형태를 정확히 따른다)\n")
                .append("""
                {
                  "작성일": "YYYY-MM-DD",
                  "작성자": "문자열",
                  "제목": "문자열",
                  "개요": "문자열",
                  "참고링크": ["문자열"],
                  "요구사항": [
                    {
                      "id": "REQ-001",
                      "구분": "기능 | 비기능",
                      "제목": "문자열",
                      "상세": "문자열",
                      "출처": "링크추론 | 보완 | 혼합",
                      "우선순위": "상 | 중 | 하"
                    }
                  ]
                }
                """);

        return sb.toString();
    }

    /** 모델이 ```json ... ``` 코드펜스를 붙였을 경우 제거 */
    private static String stripCodeFence(String text) {
        String t = text.strip();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t.strip();
    }
}
