package com.nice.qa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nice.qa.dto.TestCaseDto;
import com.nice.qa.dto.TestCaseRequest;
import com.nice.qa.service.excel.ExcelExporter;
import com.nice.qa.service.llm.LlmClient;
import com.nice.qa.service.llm.PromptBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

// 요청 → 프롬프트 조립 → LLM 호출 → JSON 파싱 → Excel 변환 흐름을 묶는 서비스.
@Service
public class TestCaseService {

    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ExcelExporter excelExporter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestCaseService(PromptBuilder promptBuilder, LlmClient llmClient, ExcelExporter excelExporter) {
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.excelExporter = excelExporter;
    }

    // 케이스 생성 → xlsx 바이트 반환
    public byte[] generateXlsx(TestCaseRequest request) {
        String prompt = promptBuilder.build(request);
        String json = llmClient.generate(prompt);
        List<TestCaseDto> cases = parse(json);
        return excelExporter.export(cases);
    }

    // LLM이 돌려준 JSON 배열을 DTO 리스트로 파싱
    private List<TestCaseDto> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<TestCaseDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("LLM 응답 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }
}
