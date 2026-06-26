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
 * 개발 요청서 영속성 CRUD 컨트롤러.
 *
 * <p>설계 의도 — "생성(무저장)"과 "저장(DB 반영)"을 의도적으로 분리한다:
 * <ul>
 *   <li>{@code POST /api/dev-requests/generate} ({@link DevRequestController}) :
 *       Gemini AI를 호출해 초안을 만들되, DB에는 저장하지 않는다.</li>
 *   <li>{@code POST /api/requests} (이 컨트롤러) :
 *       FE 위저드의 [저장] 버튼 클릭 시 호출. 위저드 입력값과 AI 생성 결과를 함께 DB에 저장한다.</li>
 * </ul>
 *
 * <p>제공 기능:
 * <ul>
 *   <li>신규 저장 (POST)</li>
 *   <li>수정 / 상태 변경 (PUT)</li>
 *   <li>목록 검색·필터·페이징 (GET)</li>
 *   <li>상세 조회 (GET /{id})</li>
 *   <li>소프트 삭제 (DELETE /{id})</li>
 *   <li>설계 흐름 PNG 조회 / 지연 생성 (GET /{id}/flow.png)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    /** 개발 요청서 DB 저장·조회·삭제 서비스 */
    private final DevRequestStorageService service;

    /** mxGraph XML 생성 및 PNG 렌더링 서비스 */
    private final FlowService flowService;

    /** mxGraph XML → PNG 바이트 배열 변환기 */
    private final MxGraphRenderer mxGraphRenderer;

    /**
     * 개발 요청서 신규 저장 (POST /api/requests).
     *
     * <p>FE 위저드 "/result" 화면에서 사용자가 [저장] 버튼을 누를 때 호출된다.
     * 저장 성공 시 HTTP 201 Created와 함께 저장된 상세 정보를 반환하여,
     * FE가 별도 조회 없이 바로 화면을 갱신할 수 있다.
     *
     * @param req 위저드 입력값 + AI 생성 마크다운을 포함하는 저장 요청 DTO
     * @return 201 Created, Location 헤더(저장된 리소스 URI), 저장 결과 상세 DTO
     */
    @PostMapping
    public ResponseEntity<DevRequestDetail> create(@Valid @RequestBody DevRequestSaveRequest req) {
        DevRequest saved = service.create(req);
        // 저장된 리소스의 URI를 Location 헤더에 포함시켜 REST 규약을 준수한다
        return ResponseEntity
                .created(URI.create("/api/requests/" + saved.getId()))
                .body(DevRequestDetail.from(saved));
    }

    /**
     * 개발 요청서 수정 (PUT /api/requests/{id}).
     *
     * <p>이어쓰기(본문 수정), 상태 변경(예: DRAFT → SUBMITTED) 등 모든 변경에 사용된다.
     * null 필드는 기존 값을 보존하는 부분 갱신(Partial Update) 방식으로 동작한다.
     *
     * @param id  수정할 요청서 PK
     * @param req 변경할 필드를 담은 저장 요청 DTO (null 필드는 보존됨)
     * @return 갱신된 상세 DTO
     */
    @PutMapping("/{id}")
    public DevRequestDetail update(@PathVariable Long id, @RequestBody DevRequestSaveRequest req) {
        DevRequest updated = service.update(id, req);
        return DevRequestDetail.from(updated);
    }

    /**
     * 개발 요청서 목록 조회 (GET /api/requests).
     *
     * <p>키워드 검색, 상태·카테고리·작성자 필터, 정렬, 페이징을 동시에 지원한다.
     * 기본 정렬은 최신 등록순(createdAt DESC)이며,
     * 클라이언트는 {@code ?sort=updatedAt,asc} 형태로 정렬 기준을 덮어쓸 수 있다.
     *
     * @param keyword  제목/본문 검색 키워드 (선택)
     * @param status   요청서 상태 필터 {@link DevRequestStatus} (선택)
     * @param category 카테고리 필터 (선택)
     * @param author   작성자 필터 (선택)
     * @param pageable 페이징·정렬 파라미터 (기본: size=20, sort=createdAt DESC)
     * @return 요약 정보({@link DevRequestSummary})의 페이지
     */
    @GetMapping
    public Page<DevRequestSummary> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DevRequestStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String author,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 검색 결과를 목록 표시용 요약 DTO(DevRequestSummary)로 변환하여 반환한다
        return service.search(keyword, status, category, author, pageable)
                .map(DevRequestSummary::from);
    }

    /**
     * 개발 요청서 상세 조회 (GET /api/requests/{id}).
     *
     * <p>목록과 달리 본문(마크다운, 다이어그램 XML, 위저드 입력 세부 정보)을 모두 포함한다.
     *
     * @param id 조회할 요청서 PK
     * @return 마크다운·다이어그램 XML 등 전체 정보를 담은 상세 DTO
     */
    @GetMapping("/{id}")
    public DevRequestDetail get(@PathVariable Long id) {
        return DevRequestDetail.from(service.get(id));
    }

    /**
     * 개발 요청서 소프트 삭제 (DELETE /api/requests/{id}).
     *
     * <p>실제 DB row는 보존하고 {@code deleted=true} 플래그만 변경하는 소프트 삭제 방식.
     * 모든 조회 쿼리에서 {@code deleted=true}인 행은 자동으로 제외된다.
     * 데이터 복구 가능성 및 감사 이력 보존이 목적이다.
     *
     * @param id 삭제할 요청서 PK
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 설계 흐름 다이어그램 PNG 조회 (GET /api/requests/{id}/flow.png).
     *
     * <p>지연 생성(Lazy Generation) 전략을 사용한다:
     * <ol>
     *   <li>DB에 {@code flowDiagram}(mxGraph XML)이 이미 저장되어 있으면 → XML을 PNG로 변환하여 즉시 반환 (Gemini 호출 없음).</li>
     *   <li>XML이 없으면 → {@code combinedMarkdown}으로 Gemini를 호출해 XML을 생성하고
     *       DB에 저장한 뒤 PNG로 변환하여 반환.</li>
     *   <li>{@code combinedMarkdown}도 없으면 → 404 반환.</li>
     * </ol>
     *
     * <p>이 전략 덕분에 같은 요청서를 다시 열 때는 Gemini를 재호출하지 않아 응답이 빠르고 비용이 절약된다.
     * 캐시 헤더(max-age=5분)를 설정하여 브라우저 측에서도 중복 요청을 방지한다.
     *
     * @param id 조회할 요청서 PK
     * @return PNG 이미지 바이트, Content-Type: image/png, Cache-Control: max-age=300
     */
    @GetMapping(value = "/{id}/flow.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> flowPng(@PathVariable Long id) {
        DevRequest entity = service.get(id);
        String xml = entity.getFlowDiagram();

        if (!StringUtils.hasText(xml)) {
            // DB에 저장된 XML이 없는 경우 — Gemini를 호출해 새로 생성한다
            String md = entity.getCombinedMarkdown();
            if (!StringUtils.hasText(md)) {
                // 마크다운 원문도 없으면 다이어그램을 생성할 수 없으므로 404 반환
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            log.info("[Flow PNG] id={} XML 없음 → Gemini 호출로 생성", id);
            // Gemini 호출로 mxGraph XML 생성 후 DB에 저장하여 다음 요청에서 재사용한다
            xml = flowService.generateXml(md);
            service.updateFlowDiagram(id, xml);
        } else {
            // DB에 캐시된 XML이 있으면 Gemini 호출 없이 즉시 PNG로 변환한다
            log.debug("[Flow PNG] id={} DB 캐시 사용", id);
        }

        // mxGraph XML을 PNG 바이트 배열로 렌더링한다.
        // 렌더에 실패하면 같은 XML로 다시 호출해도 같은 실패가 반복되므로(렌더 직전에 DB 저장이 끝난 상태),
        // 캐시(flow_diagram)를 비워 다음 호출 시 Gemini를 다시 부르게 한다.
        byte[] png;
        try {
            png = mxGraphRenderer.toPng(xml);
        } catch (RuntimeException e) {
            log.warn("[Flow PNG] id={} PNG 렌더 실패 — flow_diagram 캐시 무효화", id, e);
            service.updateFlowDiagram(id, null);
            throw e;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                // 5분 동안 브라우저 캐시를 허용하여 동일한 이미지를 반복 요청하지 않게 한다
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(5)))
                .body(png);
    }

    /**
     * 이 컨트롤러 범위 내 EntityNotFoundException 처리기.
     *
     * <p>존재하지 않는 ID로 요청서를 조회·수정·삭제할 때 서비스 레이어에서 발생한다.
     * 전역 핸들러({@link ApiExceptionHandler})로 올리지 않고 여기서 직접 처리하여
     * 404 응답을 명확히 한다. 그 외 예외는 전역 핸들러로 위임된다.
     *
     * @param e 발생한 EntityNotFoundException
     * @return 404 Not Found, 오류 메시지 포함 JSON 바디
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404, "error", "Not Found", "message", e.getMessage()));
    }
}
