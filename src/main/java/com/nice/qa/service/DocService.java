package com.nice.qa.service;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.llm.dto.ProjectMdResult;

public interface DocService {

    String assembleMarkdown(DevRequestRequest request);

    ProjectMdResult assembleJson(DevRequestRequest request);

    /**
     * Gemini 1회 호출로 JSON과 표준 양식 MD를 한 번에 만든다.
     * FE 위저드 → /api/requests 자동 저장 흐름에서 markdown 필드까지 같이 응답하기 위해 사용.
     */
    AssembledDoc assembleBoth(DevRequestRequest request);

    /** JSON 결과 + 표준 양식 MD 한 묶음. */
    record AssembledDoc(ProjectMdResult result, String markdown) {}
}
