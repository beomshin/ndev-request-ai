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

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                errorBody(400, "BAD_REQUEST", null, fieldErrors));
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<Map<String, Object>> handleLlm(LlmException e) {
        log.error("[ApiExceptionHandler] LLM 호출 실패: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                errorBody(502, "LLM_ERROR", "LLM 서비스 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.", null));
    }

    @ExceptionHandler(DiagramRenderException.class)
    public ResponseEntity<Map<String, Object>> handleDiagramRender(DiagramRenderException e) {
        log.error("[ApiExceptionHandler] 다이어그램 렌더링 실패: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                errorBody(500, "DIAGRAM_RENDER_ERROR", "다이어그램 생성에 실패했습니다.", null));
    }

    /** 정적 자원 미발견(favicon.ico 같은 것)은 그냥 404로 — catch-all이 ERROR로 잡지 않게 별도 처리. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                errorBody(404, "NOT_FOUND", e.getResourcePath(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("[ApiExceptionHandler] 예기치 않은 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                errorBody(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", null));
    }

    private static Map<String, Object> errorBody(
            int status, String error, String message, Map<String, String> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        if (message != null) {
            body.put("message", message);
        }
        if (fieldErrors != null) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }
}
