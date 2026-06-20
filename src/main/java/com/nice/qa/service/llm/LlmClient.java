package com.nice.qa.service.llm;

import com.nice.qa.service.llm.dto.*;

/**
 * 사내 LLM 호출 격리. 프롬프트는 인터페이스 너머(LLM 담당)가 소유 — 우리는 도메인 메서드만 호출.
 * 실제 구현체는 타 파트가 채움. Phase 0은 StubLlmClient.
 */
public interface LlmClient {

    // 공통 포맷 개발요청서 본문 생성 (입력 + KB 컨텍스트)
    RequestDocResult generateRequestDoc(RequestDocCommand cmd);

    // 모순/누락 사전검토 (F8)
    PreCheckResult precheck(PreCheckCommand cmd);

    // 개발 설계 흐름 — PlantUML 소스 반환. PNG 렌더는 서버(FlowImageRenderer) 책임.
    DesignFlowResult generateDesignFlow(DesignFlowCommand cmd);
}
