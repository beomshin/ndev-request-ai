package com.nice.qa.controller;

import com.nice.qa.dto.TestCaseRequest;
import com.nice.qa.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 테스트케이스 xlsx 다운로드 엔드포인트.
@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody TestCaseRequest request) {
        byte[] xlsx = testCaseService.generateXlsx(request);
        String filename = "testcases_" + LocalDateTime.now().format(TS_FMT) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX_MEDIA_TYPE);
        // 파일명 큰따옴표로 감싸 다운로드되도록 지정
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentLength(xlsx.length);

        return ResponseEntity.ok().headers(headers).body(xlsx);
    }
}
