package com.nice.qa.service;

/**
 * 개발요청서 마크다운으로부터 시스템 흐름 다이어그램을 생성하는 서비스 인터페이스.
 *
 * <p>이 인터페이스는 두 가지 생성 모드를 제공한다.
 * <ul>
 *   <li>{@link #renderPng} — 마크다운을 받아 PNG 이미지 바이트를 즉시 반환 (1회성 렌더링)</li>
 *   <li>{@link #generateXml} — 마크다운을 받아 draw.io 호환 mxGraph XML만 반환 (DB 캐시 흐름)</li>
 * </ul>
 *
 * <p>내부적으로 Gemini LLM에 시퀀스 다이어그램 생성을 요청하고,
 * 반환된 mxGraph XML을 {@code MxGraphRenderer}로 PNG로 변환한다.
 *
 * <h3>DB 캐시 설계 의도</h3>
 * XML 생성 비용(LLM 호출)은 한 번만 발생하고 DB에 저장한다.
 * 이후 PNG가 필요할 때마다 저장된 XML로만 렌더링하여 LLM 재호출을 방지한다.
 *
 * @see com.nice.qa.service.impl.FlowServiceImpl
 */
public interface FlowService {

    /**
     * 마크다운 → Gemini → mxGraph XML → PNG bytes (한 번에).
     *
     * <p>단일 호출로 LLM 프롬프트 생성, Gemini API 호출, XML 파싱,
     * PNG 렌더링까지 모두 수행하여 바이트 배열을 반환한다.
     * 결과를 캐시하거나 DB에 저장하지 않으므로 매번 LLM이 호출된다.
     *
     * @param markdown 시퀀스 다이어그램의 기반이 될 요청서 마크다운 문자열
     * @return 렌더링된 다이어그램 PNG 이미지 바이트 배열
     */
    byte[] renderPng(String markdown);

    /**
     * 마크다운 → Gemini → mxGraph XML (텍스트). PNG 렌더는 호출 측이 수행.
     * DB 캐시 흐름: XML을 DB에 저장해두고 매번 PNG 렌더만 하기 위해 분리.
     *
     * <p>이 메서드는 LLM 호출 결과인 draw.io 호환 XML 문자열만 반환한다.
     * 호출자가 XML을 DB에 저장하고, 이후 PNG가 필요할 때
     * 저장된 XML을 MxGraphRenderer에 직접 전달하는 패턴에서 사용된다.
     *
     * @param markdown 시퀀스 다이어그램의 기반이 될 요청서 마크다운 문자열
     * @return draw.io 호환 mxGraph XML 문자열 (파싱 실패 시 대체 에러 XML 반환)
     */
    String generateXml(String markdown);
}
