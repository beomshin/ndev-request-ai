package com.nice.qa.controller;

import com.nice.qa.exception.DiagramRenderException;
import com.nice.qa.exception.LlmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 전역 REST API 예외 처리 핸들러.
 *
 * <p>{@link RestControllerAdvice}를 사용하여 모든 {@link RestController}에서 발생하는
 * 예외를 일관된 JSON 오류 응답 형식으로 변환한다.
 *
 * <p>처리 예외 목록:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request (Bean Validation 실패)</li>
 *   <li>{@link LlmException} → 502 Bad Gateway (Gemini 등 외부 AI API 호출 실패)</li>
 *   <li>{@link DiagramRenderException} → 500 Internal Server Error (다이어그램 렌더링 실패)</li>
 *   <li>{@link NoResourceFoundException} → 404 Not Found (정적 자원 미발견)</li>
 *   <li>{@link Exception} → 500 Internal Server Error (기타 예기치 않은 오류)</li>
 * </ul>
 *
 * <p>모든 오류 응답 바디는 {@link #errorBody} 헬퍼로 생성하며,
 * {@code timestamp}, {@code status}, {@code error}, 선택적 {@code message},
 * 선택적 {@code fieldErrors} 필드를 포함한다.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Bean Validation 실패 처리 — 400 Bad Request.
     *
     * <p>{@code @Valid} 어노테이션이 붙은 요청 DTO의 필드 검증에 실패했을 때 발생한다.
     * 어떤 필드에서 어떤 이유로 실패했는지를 {@code fieldErrors} 맵에 담아 반환한다.
     * FE는 이 정보로 어떤 입력 필드에 오류 메시지를 표시할지 파악한다.
     *
     * @param e Spring MVC가 발생시키는 검증 실패 예외
     * @return 400 Bad Request, 필드별 오류 메시지 맵({@code fieldErrors}) 포함
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        // 검증 실패한 필드명과 기본 오류 메시지를 맵으로 수집한다
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                errorBody(400, "BAD_REQUEST", null, fieldErrors));
    }

    /**
     * LLM(AI) API 호출 실패 처리 — 502 Bad Gateway.
     *
     * <p>Gemini 등 외부 AI 서비스 호출 시 네트워크 오류, 타임아웃,
     * API 한도 초과 등으로 {@link LlmException}이 발생할 때 처리된다.
     * 502를 반환하는 이유: 서버 자체의 오류가 아닌 업스트림(외부 AI 서비스)의 문제이므로.
     * 내부 오류 상세는 서버 로그에만 기록하고, 클라이언트에는 안전한 메시지만 노출한다.
     *
     * @param e AI 서비스 호출 실패 예외
     * @return 502 Bad Gateway, 사용자 친화적 오류 메시지 포함
     */
    @ExceptionHandler(LlmException.class)
    public ResponseEntity<Map<String, Object>> handleLlm(LlmException e) {
        log.error("[ApiExceptionHandler] LLM 호출 실패: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                errorBody(502, "LLM_ERROR", "LLM 서비스 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.", null));
    }

    /**
     * 다이어그램 렌더링 실패 처리 — 500 Internal Server Error.
     *
     * <p>mxGraph XML → PNG 변환 또는 PlantUML 렌더링 과정에서 오류가 발생할 때 처리된다.
     * 서버 내부 오류이므로 500을 반환하며, 내부 스택 트레이스는 로그에만 기록한다.
     *
     * @param e 다이어그램 렌더링 실패 예외
     * @return 500 Internal Server Error, 사용자 친화적 오류 메시지 포함
     */
    @ExceptionHandler(DiagramRenderException.class)
    public ResponseEntity<Map<String, Object>> handleDiagramRender(DiagramRenderException e) {
        log.error("[ApiExceptionHandler] 다이어그램 렌더링 실패: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                errorBody(500, "DIAGRAM_RENDER_ERROR", "다이어그램 생성에 실패했습니다.", null));
    }

    /**
     * 정적 자원 미발견 처리 — 404 Not Found.
     *
     * <p>{@code favicon.ico}, 존재하지 않는 정적 파일 등을 요청할 때 발생한다.
     * Catch-all {@link #handleGeneral}이 이를 500 오류로 잘못 처리하지 않도록 별도로 처리한다.
     * 이 예외는 정상적인 클라이언트 요청에서 빈번하게 발생하므로 ERROR 레벨로 로깅하지 않는다.
     *
     * @param e 미발견 자원 예외 (요청된 자원 경로 포함)
     * @return 404 Not Found, 요청된 경로 포함
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                errorBody(404, "NOT_FOUND", e.getResourcePath(), null));
    }

    /**
     * 기타 모든 예기치 않은 예외 처리 — 500 Internal Server Error.
     *
     * <p>위의 특정 핸들러들이 처리하지 못한 모든 예외의 최종 처리기(catch-all)다.
     * 예외 상세는 서버 로그에 ERROR 레벨로 기록하고,
     * 클라이언트에는 내부 구현 세부사항이 노출되지 않는 안전한 메시지만 반환한다.
     *
     * @param e 처리되지 않은 예외
     * @return 500 Internal Server Error, 일반 오류 메시지 포함
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("[ApiExceptionHandler] 예기치 않은 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                errorBody(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", null));
    }

    /**
     * 표준 오류 응답 바디 생성 헬퍼.
     *
     * <p>모든 오류 응답이 일관된 JSON 구조를 갖도록 공통 포맷을 제공한다.
     * 선택적 필드({@code message}, {@code fieldErrors})는 null이면 응답에서 제외된다.
     *
     * <p>응답 바디 예시:
     * <pre>{@code
     * {
     *   "timestamp": "2024-06-15T14:30:22.123Z",
     *   "status": 400,
     *   "error": "BAD_REQUEST",
     *   "fieldErrors": { "author": "must not be blank" }
     * }
     * }</pre>
     *
     * @param status      HTTP 상태 코드 숫자
     * @param error       오류 코드 문자열 (예: "BAD_REQUEST", "LLM_ERROR")
     * @param message     사용자 표시용 오류 메시지 (null이면 응답에서 제외)
     * @param fieldErrors 필드별 오류 메시지 맵 (Bean Validation 실패 시 사용, null이면 제외)
     * @return 표준 오류 응답 맵 (삽입 순서 보장을 위해 {@link LinkedHashMap} 사용)
     */
    private static Map<String, Object> errorBody(
            int status, String error, String message, Map<String, String> fieldErrors) {
        // LinkedHashMap을 사용해 JSON 직렬화 시 필드 순서를 일정하게 유지한다
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());  // ISO-8601 UTC 타임스탬프
        body.put("status", status);
        body.put("error", error);
        // null인 경우 응답 바디에서 필드 자체를 제외하여 불필요한 null 값 노출을 방지한다
        if (message != null) {
            body.put("message", message);
        }
        if (fieldErrors != null) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }
}
