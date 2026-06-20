package com.nice.qa.service;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.flow.FlowImageRenderer;
import com.nice.qa.service.knowledge.dto.KbFilter;
import com.nice.qa.service.knowledge.dto.KnowledgeChunk;
import com.nice.qa.service.knowledge.KnowledgeClient;
import com.nice.qa.service.llm.dto.DesignFlowCommand;
import com.nice.qa.service.llm.dto.DesignFlowResult;
import com.nice.qa.service.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// LlmClient가 만들어 준 PlantUML 소스를 FlowImageRenderer로 PNG 렌더.
@Slf4j
@Service
@RequiredArgsConstructor
public class DesignFlowService {

    private final LlmClient llmClient;
    private final KnowledgeClient knowledgeClient;
    private final FlowImageRenderer renderer;

    public byte[] renderPng(DevRequestRequest req) {
        // KB 컨텍스트(설계 근거용) — Phase 0에선 stub가 빈/샘플 결과 돌려줌
        List<KnowledgeChunk> chunks = knowledgeClient.search(
                req.problemAndImprovement(), KbFilter.of(req.category(), req.subType()));

        DesignFlowResult flow = llmClient.generateDesignFlow(new DesignFlowCommand(req, chunks));
        return renderer.render(flow.plantUmlSource());
    }
}
