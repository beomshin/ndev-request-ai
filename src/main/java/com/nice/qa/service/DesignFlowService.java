package com.nice.qa.service;

import com.nice.qa.dto.DevRequestRequest;
import com.nice.qa.service.flow.FlowImageRenderer;
import com.nice.qa.service.knowledge.KbFilter;
import com.nice.qa.service.knowledge.KnowledgeChunk;
import com.nice.qa.service.knowledge.KnowledgeClient;
import com.nice.qa.service.llm.DesignFlowCommand;
import com.nice.qa.service.llm.DesignFlowResult;
import com.nice.qa.service.llm.LlmClient;
import org.springframework.stereotype.Service;

import java.util.List;

// LlmClient가 만들어 준 PlantUML 소스를 FlowImageRenderer로 PNG 렌더.
@Service
public class DesignFlowService {

    private final LlmClient llmClient;
    private final KnowledgeClient knowledgeClient;
    private final FlowImageRenderer renderer;

    public DesignFlowService(LlmClient llmClient, KnowledgeClient knowledgeClient, FlowImageRenderer renderer) {
        this.llmClient = llmClient;
        this.knowledgeClient = knowledgeClient;
        this.renderer = renderer;
    }

    public byte[] renderPng(DevRequestRequest req) {
        // KB 컨텍스트(설계 근거용) — Phase 0에선 stub가 빈/샘플 결과 돌려줌
        List<KnowledgeChunk> chunks = knowledgeClient.search(
                req.problemAndImprovement(), KbFilter.of(req.category(), req.subType()));

        DesignFlowResult flow = llmClient.generateDesignFlow(new DesignFlowCommand(req, chunks));
        return renderer.render(flow.plantUmlSource());
    }
}
