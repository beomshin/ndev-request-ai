package com.nice.qa.service.llm;

import com.nice.qa.dto.DevRequestRequest;
import com.nice.qa.service.knowledge.KnowledgeChunk;

import java.util.List;

// 설계 플로우 생성 입력.
public record DesignFlowCommand(
        DevRequestRequest request,
        List<KnowledgeChunk> kbChunks
) {
}
