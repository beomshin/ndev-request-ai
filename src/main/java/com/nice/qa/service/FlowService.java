package com.nice.qa.service;

public interface FlowService {

    /** 마크다운 → Gemini → mxGraph XML → PNG bytes (한 번에). */
    byte[] renderPng(String markdown);

    /**
     * 마크다운 → Gemini → mxGraph XML (텍스트). PNG 렌더는 호출 측이 수행.
     * DB 캐시 흐름: XML을 DB에 저장해두고 매번 PNG 렌더만 하기 위해 분리.
     */
    String generateXml(String markdown);
}
