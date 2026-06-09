package com.nice.qa.service.llm;

// LLM 호출부 추상화. 실제 사내 LLM 연동 시 이 인터페이스의 새 구현체로 교체.
public interface LlmClient {

    // 프롬프트를 받아 LLM 응답(원문 문자열, 보통 JSON)을 반환.
    String generate(String prompt);
}
