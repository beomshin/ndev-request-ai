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
 * 개발 요청서 생성 API 컨트롤러.
 *
 * <p>두 가지 생성 방식을 제공한다:
 * <ul>
 *   <li><b>POST /api/dev-requests/generate</b> — 마크다운 문서 + 설계 흐름 PNG를 ZIP 파일로 묶어 즉시 다운로드.
 *       파일 저장 없이 일회성 생성물을 제공하므로, DB에 저장하지 않는다.</li>
 *   <li><b>GET /api/dev-requests/generate</b> — 프론트엔드(FE) 위저드 자동 저장 흐름에서 호출.
 *       Gemini AI 호출 결과와 조합된 마크다운을 JSON으로 반환하여, FE가 DB에 저장할 수 있게 한다.</li>
 * </ul>
 *
 * <p>설계 의도: 저장이 필요한 흐름은 {@link RequestController}에 위임하고,
 * 이 컨트롤러는 순수 "문서 생성(무저장)" 역할만 담당한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/dev-requests")
@RequiredArgsConstructor
public class DevRequestController {

    /**
     * 파일명 타임스탬프 포맷 — "yyyyMMdd_HHmmss" 형식으로 중복 없는 ZIP 파일명을 생성한다.
     * 예: devrequest_20240615_143022.zip
     */
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** 마크다운 문서 조립 서비스 — AI(Gemini) 호출을 통해 표준 양식 문서를 생성한다. */
    private final DocService docService;

    /** 설계 흐름 다이어그램 렌더링 서비스 — mxGraph XML을 PNG 이미지로 변환한다. */
    private final FlowService flowService;

    /**
     * 개발 요청서 ZIP 다운로드 엔드포인트 (POST).
     *
     * <p>처리 순서:
     * <ol>
     *   <li>요청 DTO를 검증(Bean Validation)한다.</li>
     *   <li>{@link DocService#assembleMarkdown}으로 AI 기반 마크다운 문서를 생성한다.</li>
     *   <li>{@link FlowService#renderPng}로 설계 흐름 PNG 이미지를 렌더링한다.</li>
     *   <li>{@link ZipBuilder#buildDevRequestZip}으로 두 파일을 ZIP으로 묶는다.</li>
     *   <li>타임스탬프가 포함된 파일명으로 attachment 응답을 반환한다.</li>
     * </ol>
     *
     * @param request 위저드에서 수집한 개발 요청 정보 (작성자, 서비스명 등 포함), Bean Validation 적용
     * @return ZIP 바이너리를 담은 {@code application/zip} 응답, Content-Disposition 헤더로 다운로드 유도
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateDevRequest(
            @Valid @RequestBody DevRequestRequest request
    ) {
        log.info("[DevRequest] ZIP 생성 요청 (author={}, serviceName={})",
                request.author(), request.serviceName());
        // 성능 측정을 위해 시작 시각을 기록한다
        long start = System.currentTimeMillis();

        // 1단계: AI(Gemini)를 호출해 마크다운 문서를 조립한다
        String markdown = docService.assembleMarkdown(request);

        // 2단계: 마크다운을 기반으로 설계 흐름 PNG를 렌더링한다
        byte[] flowPng = flowService.renderPng(markdown);

        // 3단계: 마크다운과 PNG를 하나의 ZIP 바이트 배열로 묶는다
        byte[] zip = ZipBuilder.buildDevRequestZip(markdown, flowPng);

        // 파일명은 타임스탬프 기반으로 생성하여 중복을 방지한다
        String filename = "devrequest_" + LocalDateTime.now().format(TS_FMT) + ".zip";
        log.info("[DevRequest] ZIP 생성 완료 ({}ms, {}bytes, filename={})",
                System.currentTimeMillis() - start, zip.length, filename);

        // Content-Disposition: attachment 헤더를 설정하여 브라우저 다운로드를 유도한다
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentLength(zip.length);

        return ResponseEntity.ok().headers(headers).body(zip);
    }

    /**
     * FE 위저드 자동 저장 흐름용 JSON 생성 엔드포인트 (GET).
     *
     * <p>FE 위저드에서 "결과 미리보기" 단계에 진입할 때 호출된다.
     * POST와 달리 ZIP이 아닌 JSON을 반환하여, FE가 화면에 렌더링하고
     * 이후 사용자가 [저장] 버튼을 누를 때 {@link RequestController}로 DB 저장을 요청할 수 있다.
     *
     * <p>Gemini 호출은 {@link DocService#assembleBoth} 내부에서 1회만 수행된다.
     * 반환되는 {@code markdown}(combinedMarkdown)은 FE가 DB 저장 시 그대로 전달해야 한다.
     *
     * @param request 위저드에서 수집한 개발 요청 정보, {@code @ModelAttribute}로 쿼리 파라미터를 바인딩
     * @return AI 처리 결과({@code devRequest})와 마크다운 원문({@code markdown})을 포함하는 JSON 맵
     */
    @GetMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateDevRequestJson(
            @Valid @ModelAttribute DevRequestRequest request
    ) {
        log.info("[DevRequest] JSON 생성 요청 (author={}, serviceName={})",
                request.author(), request.serviceName());

        // assembleBoth는 AI 호출 1회로 result(구조화 객체)와 markdown(원문) 두 가지를 동시에 반환한다
        DocService.AssembledDoc doc = docService.assembleBoth(request);

        // 표준 응답 포맷: resultCode/resultMsg는 FE 공통 처리 규약
        return ResponseEntity.ok(Map.of(
                "resultCode", "0000",
                "resultMsg", "SUCCESS",
                "devRequest", doc.result(),     // 구조화된 AI 응답 결과 객체
                "markdown", doc.markdown()      // FE → DB 저장 시 그대로 전달할 마크다운 원문
        ));
    }
}
