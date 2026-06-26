package com.nice.qa.exception;

/**
 * 다이어그램 렌더링 실패 예외.
 *
 * <p>mxGraph XML → PNG 변환 또는 PlantUML 소스 텍스트 렌더링 과정에서
 * 복구 불가능한 오류가 발생했을 때 던져진다.
 *
 * <p>이 예외는 {@link RuntimeException}을 상속하는 비검사 예외(unchecked exception)이므로
 * 호출 측에서 {@code throws} 선언이나 {@code try-catch} 없이도 전파된다.
 * 전역 예외 핸들러({@code ApiExceptionHandler})가 이를 잡아 500 Internal Server Error로 변환한다.
 *
 * @see com.nice.qa.controller.ApiExceptionHandler
 */
public class DiagramRenderException extends RuntimeException {

    /**
     * 오류 메시지와 원인 예외를 지정하여 {@code DiagramRenderException}을 생성한다.
     *
     * @param message 렌더링 실패 상황을 설명하는 메시지 (로그 및 오류 응답에 사용됨)
     * @param cause   렌더링 실패의 근본 원인이 된 원래 예외 (스택 트레이스 추적용)
     */
    public DiagramRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
