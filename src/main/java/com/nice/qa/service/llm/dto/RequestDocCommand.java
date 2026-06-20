package com.nice.qa.service.llm.dto;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.knowledge.dto.KnowledgeChunk;
import com.nice.qa.service.knowledge.dto.SpecDocRef;

import java.util.List;

// 요청서 본문 생성용 입력. 원본 요청 + KB에서 끌어온 컨텍스트를 함께 전달.
public record RequestDocCommand(
        DevRequestRequest request,
        List<KnowledgeChunk> kbChunks,
        List<SpecDocRef> specDocs
) {
}
