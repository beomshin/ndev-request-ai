package com.nice.qa.controller;

import com.nice.qa.dto.DevRequestRequest;
import com.nice.qa.service.DesignFlowService;
import com.nice.qa.service.RequestDocService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// 개발요청서 + 설계 flow PNG를 zip으로 묶어 다운로드.
@RestController
@RequestMapping("/api/dev-requests")
public class DevRequestController {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final RequestDocService requestDocService;
    private final DesignFlowService designFlowService;

    public DevRequestController(RequestDocService requestDocService, DesignFlowService designFlowService) {
        this.requestDocService = requestDocService;
        this.designFlowService = designFlowService;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody DevRequestRequest req) {
        String markdown = requestDocService.assembleMarkdown(req);
        byte[] flowPng = designFlowService.renderPng(req);

        byte[] zip = buildZip(markdown, flowPng);
        String filename = "devrequest_" + LocalDateTime.now().format(TS_FMT) + ".zip";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentLength(zip.length);

        return ResponseEntity.ok().headers(headers).body(zip);
    }

    // requirements.md + flow.png 두 엔트리로 zip 생성
    private byte[] buildZip(String markdown, byte[] flowPng) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(out)) {

            zos.putNextEntry(new ZipEntry("requirements.md"));
            zos.write(markdown.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("flow.png"));
            zos.write(flowPng);
            zos.closeEntry();

            zos.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("zip 생성 실패", e);
        }
    }
}
