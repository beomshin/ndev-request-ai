package com.nice.qa.service.llm.dto;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.knowledge.dto.KnowledgeChunk;

import java.util.List;

// 사전검토 입력.
public record PreCheckCommand(
        DevRequestRequest request,
        List<KnowledgeChunk> kbChunks
) {
}
