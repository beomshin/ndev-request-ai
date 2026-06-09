package com.nice.qa.service.llm;

import com.nice.qa.dto.TestCaseRequest;
import org.springframework.stereotype.Component;

// 프롬프트 조립부 자리만 잡아둠. 실제 프롬프트 설계는 타 파트가 채울 예정.
@Component
public class PromptBuilder {

    // 요청 정보를 받아 LLM에 보낼 프롬프트 문자열을 만든다.
    public String build(TestCaseRequest request) {
        // TODO: 실제 프롬프트 템플릿은 타 파트에서 작성
        return """
                결제수단: %s
                PG/Provider: %s
                요구사항: %s
                위 정보를 기반으로 QA 테스트케이스 JSON 배열을 생성하세요.
                """.formatted(request.paymentMethod(), request.provider(), request.requirements());
    }
}
