package com.nice.qa.service;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.knowledge.dto.KbFilter;
import com.nice.qa.service.knowledge.dto.KnowledgeChunk;
import com.nice.qa.service.knowledge.KnowledgeClient;
import com.nice.qa.service.knowledge.dto.SpecDocRef;
import com.nice.qa.service.llm.LlmClient;
import com.nice.qa.service.llm.dto.PreCheckCommand;
import com.nice.qa.service.llm.dto.PreCheckResult;
import com.nice.qa.service.llm.dto.RequestDocCommand;
import com.nice.qa.service.llm.dto.RequestDocResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestDocService {

    private final LlmClient llmClient;
    private final KnowledgeClient knowledgeClient;

    /**
     * 공통 포맷 요청서 + 추가확인 + 사전검토 + 규격서 매칭을 묶어 종합 MD를 만든다.
     * 설계 flow는 마지막에 이미지 링크(flow.png)만 끼워두고 실제 PNG는 DesignFlowService가 채운다.
     * @param req
     * @return
     */
    public String assembleMarkdown(DevRequestRequest req) {
        // 1. KB 컨텍스트 수집
        List<SpecDocRef> specDocs = knowledgeClient.matchSpecDocs(req.category(), req.subType());
        List<KnowledgeChunk> chunks = knowledgeClient.search(
                req.problemAndImprovement(), KbFilter.of(req.category(), req.subType()));

        // 2. 사전검토
        PreCheckResult precheck = llmClient.precheck(new PreCheckCommand(req, chunks));

        // 3. 요청서 본문 생성
        RequestDocResult doc = llmClient.generateRequestDoc(
                new RequestDocCommand(req, chunks, specDocs));

        // 4. 종합 MD 조립
        StringBuilder md = new StringBuilder();
        md.append("# 개발요청서 — ").append(req.serviceName()).append("\n\n");

        md.append("## 메타\n");
        md.append("- 작성자: ").append(req.author()).append("\n");
        md.append("- 부서: ").append(req.department()).append("\n");
        md.append("- 기능구분: ").append(req.funcType()).append("\n");
        md.append("- 카테고리: ").append(req.category()).append(" / ").append(req.subType()).append("\n");
        md.append("- 목표일정: ").append(req.targetSchedule()).append("\n\n");

        md.append("## 추진배경\n").append(req.background()).append("\n\n");
        md.append("## 문제점 / 개선점\n").append(req.problemAndImprovement()).append("\n\n");

        md.append("## 본문 (LLM 생성)\n").append(doc.bodyMarkdown()).append("\n");

        md.append("## 추가 확인 필요 항목 (F7)\n");
        for (String item : doc.additionalChecks()) {
            md.append("- ").append(item).append("\n");
        }
        md.append("\n");

        md.append("## 사전검토 경고 (F8)\n");
        if (precheck.warnings().isEmpty()) {
            md.append("- (경고 없음)\n");
        } else {
            for (String w : precheck.warnings()) {
                md.append("- ").append(w).append("\n");
            }
        }
        md.append("\n");

        md.append("## 관련 규격서 (F10)\n");
        if (specDocs.isEmpty()) {
            md.append("- (매칭된 규격서 없음)\n");
        } else {
            md.append(specDocs.stream()
                    .map(s -> "- " + s.title() + " (" + s.version() + ") — " + s.url())
                    .collect(Collectors.joining("\n"))).append("\n");
        }
        md.append("\n");

        md.append("## 참고 KB 청크\n");
        if (chunks.isEmpty()) {
            md.append("- (검색 결과 없음)\n");
        } else {
            md.append(chunks.stream()
                    .map(c -> "- [" + c.title() + "] " + c.content() + " (score=" + c.score() + ")")
                    .collect(Collectors.joining("\n"))).append("\n");
        }
        md.append("\n");

        md.append("## 설계 흐름\n");
        md.append("![flow](flow.png)\n");

        return md.toString();
    }


    public String assembleMarkdown2(DevRequestRequest req) {
        return null;
    }
}
