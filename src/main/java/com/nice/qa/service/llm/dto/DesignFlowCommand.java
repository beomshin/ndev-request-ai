package com.nice.qa.service.llm.dto;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.knowledge.dto.KnowledgeChunk;

import java.util.List;

// 설계 플로우 생성 입력.
public record DesignFlowCommand(
        DevRequestRequest request,
        List<KnowledgeChunk> kbChunks
) {
}
