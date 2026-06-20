package com.nice.qa.service.llm;

import com.nice.qa.service.llm.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 실제 LLM 호출 없이 고정 mock 반환. 흐름·산출물 검증 + 프론트 개발용.
 * 실제 사내 LLM 어댑터가 들어오기 전까지의 자리채움.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "stub", matchIfMissing = true)
public class StubLlmClient implements LlmClient {

    @Override
    public RequestDocResult generateRequestDoc(RequestDocCommand cmd) {
        // 본문은 placeholder. 실제 작성은 LLM/프롬프트 담당이 채움.
        String body = """
                ## 1. 개요
                (LLM이 생성할 요청서 본문 placeholder)

                ## 2. 요구사항
                - 입력으로 받은 요청자/카테고리/세부유형을 기반으로 본문이 채워질 자리

                ## 3. 비고
                Phase 0 stub 응답입니다. 실제 LLM 연결 시 이 영역은 모델이 생성한 본문으로 대체됩니다.
                """;

        // 추가 확인이 필요한 항목 3건(샘플) — F7
        List<String> additionalChecks = List.of(
                "결제 한도 정책 확인 필요",
                "예외 케이스(취소/환불) 흐름 확인 필요",
                "외부 시스템 연동 SLA 협의 필요"
        );
        return new RequestDocResult(body, additionalChecks);
    }

    @Override
    public PreCheckResult precheck(PreCheckCommand cmd) {
        // design.md §4.1에 명시된 샘플 경고
        return new PreCheckResult(List.of(
                "샘플: 할부 12개월 요청 — 카드사 최대 10개월, 확인 필요"
        ));
    }

    @Override
    public DesignFlowResult generateDesignFlow(DesignFlowCommand cmd) {
        // 프롬프트에 명시된 샘플 PlantUML 소스
        String puml = """
                @startuml
                start
                :요청 접수;
                :검증;
                :결제 연동;
                stop
                @enduml
                """;
        return new DesignFlowResult(puml);
    }
}
