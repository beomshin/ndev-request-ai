package com.nice.qa.service.llm;

import com.nice.qa.dto.DevRequestRequest;
import com.nice.qa.service.knowledge.KnowledgeChunk;

import java.util.List;

// 사전검토 입력.
public record PreCheckCommand(
        DevRequestRequest request,
        List<KnowledgeChunk> kbChunks
) {
}
