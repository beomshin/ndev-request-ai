package com.nice.qa.controller;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.DocService;
import com.nice.qa.service.FlowService;
import com.nice.qa.util.ZipBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 개발요청서 + 설계 흐름 PNG를 ZIP으로 묶어 다운로드 (POST)
 * + FE 위저드 자동 저장 흐름용 JSON 응답 (GET)
 */
@Slf4j
@RestController
@RequestMapping("/api/dev-requests")
@RequiredArgsConstructor
public class DevRequestController {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final DocService docService;
    private final FlowService flowService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateDevRequest(
            @Valid @RequestBody DevRequestRequest request
    ) {
        log.info("[DevRequest] ZIP 생성 요청 (author={}, serviceName={})",
                request.author(), request.serviceName());
        long start = System.currentTimeMillis();

        String markdown = docService.assembleMarkdown(request);
        byte[] flowPng = flowService.renderPng(markdown);
        byte[] zip = ZipBuilder.buildDevRequestZip(markdown, flowPng);

        String filename = "devrequest_" + LocalDateTime.now().format(TS_FMT) + ".zip";
        log.info("[DevRequest] ZIP 생성 완료 ({}ms, {}bytes, filename={})",
                System.currentTimeMillis() - start, zip.length, filename);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentLength(zip.length);

        return ResponseEntity.ok().headers(headers).body(zip);
    }

    /**
     * FE 위저드 자동 저장 흐름이 호출 — ProjectMdResult와 표준 양식 MD를 한 번에 받는다.
     * Gemini 호출은 여전히 1회 (assembleBoth 내부에서 결과 → markdownRenderer만 추가).
     * markdown은 FE가 DB 저장 시 combinedMarkdown으로 그대로 넘긴다.
     */
    @GetMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateDevRequestJson(
            @Valid @ModelAttribute DevRequestRequest request
    ) {
        log.info("[DevRequest] JSON 생성 요청 (author={}, serviceName={})",
                request.author(), request.serviceName());
        DocService.AssembledDoc doc = docService.assembleBoth(request);
        return ResponseEntity.ok(Map.of(
                "resultCode", "0000",
                "resultMsg", "SUCCESS",
                "devRequest", doc.result(),
                "markdown", doc.markdown()
        ));
    }
}
