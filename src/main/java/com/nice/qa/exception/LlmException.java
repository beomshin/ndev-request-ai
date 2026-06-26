package com.nice.qa.exception;

/**
 * LLM(대형 언어 모델) API 호출 실패 예외.
 *
 * <p>Gemini 등 외부 AI 서비스를 호출할 때 네트워크 오류, 타임아웃,
 * API 키 오류, 할당량 초과 등으로 인해 복구 불가능한 오류가 발생했을 때 던져진다.
 *
 * <p>이 예외는 {@link RuntimeException}을 상속하는 비검사 예외(unchecked exception)이므로
 * 호출 측에서 {@code throws} 선언이나 {@code try-catch} 없이도 전파된다.
 * 전역 예외 핸들러({@code ApiExceptionHandler})가 이를 잡아 502 Bad Gateway로 변환한다.
 * 502를 사용하는 이유는 서버 자체의 오류가 아닌 업스트림(외부 AI 서비스)의 문제임을 나타내기 위함이다.
 *
 * @see com.nice.qa.controller.ApiExceptionHandler
 */
public class LlmException extends RuntimeException {

    /**
     * 오류 메시지와 원인 예외를 지정하여 {@code LlmException}을 생성한다.
     *
     * @param message LLM 호출 실패 상황을 설명하는 메시지 (로그 및 오류 응답에 사용됨)
     * @param cause   호출 실패의 근본 원인이 된 원래 예외 (스택 트레이스 추적용)
     */
    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
