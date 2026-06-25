package com.nice.qa.controller;

import com.nice.qa.entity.DevRequest;
import com.nice.qa.entity.DevRequestStatus;
import com.nice.qa.model.api.dto.DevRequestDetail;
import com.nice.qa.model.api.dto.DevRequestSaveRequest;
import com.nice.qa.model.api.dto.DevRequestSummary;
import com.nice.qa.service.DevRequestStorageService;
import com.nice.qa.service.FlowService;
import com.nice.qa.service.llm.MxGraphRenderer;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * 개발요청서 저장/조회/수정/삭제.
 *
 * <p>설계 의도:
 * <ul>
 *   <li>{@code POST /api/dev-requests/generate} 는 그대로 두고(무저장 초안 생성)</li>
 *   <li>여기서 별도 {@code /api/requests} 를 두어 "명시적 저장" 흐름을 분리한다.</li>
 *   <li>위저드/`/result` 에서 [저장] 버튼을 눌러 generate 결과 + 위저드 입력을 함께 POST.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final DevRequestStorageService service;
    private final FlowService flowService;
    private final MxGraphRenderer mxGraphRenderer;

    /** 신규 저장. 저장된 id와 상세를 함께 반환해 호출 측이 곧장 화면에 표시 가능. */
    @PostMapping
    public ResponseEntity<DevRequestDetail> create(@Valid @RequestBody DevRequestSaveRequest req) {
        DevRequest saved = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/requests/" + saved.getId()))
                .body(DevRequestDetail.from(saved));
    }

    /** 수정 (이어쓰기 / 상태 변경 포함). null 필드는 보존(부분 갱신). */
    @PutMapping("/{id}")
    public DevRequestDetail update(@PathVariable Long id, @RequestBody DevRequestSaveRequest req) {
        DevRequest updated = service.update(id, req);
        return DevRequestDetail.from(updated);
    }

    /**
     * 목록 (검색·필터·정렬·페이징).
     * 정렬 기본: createdAt desc. 클라이언트는 ?sort=updatedAt,asc 같이 덮어쓸 수 있음.
     */
    @GetMapping
    public Page<DevRequestSummary> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DevRequestStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String author,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.search(keyword, status, category, author, pageable)
                .map(DevRequestSummary::from);
    }

    /** 상세 조회. 본문(MD/Diagram/details)까지 모두 포함. */
    @GetMapping("/{id}")
    public DevRequestDetail get(@PathVariable Long id) {
        return DevRequestDetail.from(service.get(id));
    }

    /** 소프트 삭제 — 실제 row는 남고 deleted=true로 표시. 모든 조회에서 자동 제외됨. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 설계 흐름 다이어그램 PNG.
     * - DB의 flowDiagram(mxGraph XML)이 있으면 그것만 PNG로 렌더링 (Gemini 호출 X).
     * - 없으면 combinedMarkdown으로 Gemini를 한 번 호출해 XML 생성 + DB 저장 + 렌더링.
     *   → 같은 요청서를 다시 열 때는 캐시된 XML로 즉시 렌더.
     */
    @GetMapping(value = "/{id}/flow.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> flowPng(@PathVariable Long id) {
        DevRequest entity = service.get(id);
        String xml = entity.getFlowDiagram();
        if (!StringUtils.hasText(xml)) {
            String md = entity.getCombinedMarkdown();
            if (!StringUtils.hasText(md)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            log.info("[Flow PNG] id={} XML 없음 → Gemini 호출로 생성", id);
            xml = flowService.generateXml(md);
            service.updateFlowDiagram(id, xml);
        } else {
            log.debug("[Flow PNG] id={} DB 캐시 사용", id);
        }
        byte[] png = mxGraphRenderer.toPng(xml);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5)))
                .body(png);
    }

    /** 404 매핑 — EntityNotFoundException 만 여기서 잡고, 그 외는 전역 핸들러로. */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404, "error", "Not Found", "message", e.getMessage()));
    }
}
