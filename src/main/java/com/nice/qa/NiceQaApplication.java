package com.nice.qa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NICE QA 개발요청서 AI 자동화 애플리케이션의 진입점
 * (Entry point for the NICE QA Development Request AI Automation application).
 *
 * <p>이 애플리케이션은 개발 연동 요건 정의서 작성을 자동화하는 도구이다.
 * 사용자가 위저드 UI에서 입력한 정보를 바탕으로 Google Gemini AI가
 * 표준 양식에 맞는 마크다운 문서와 플로우 다이어그램을 생성한다.
 *
 * <p>주요 기술 스택 (Tech stack):
 * <ul>
 *   <li>Spring Boot — 웹 서버 및 DI 컨테이너</li>
 *   <li>Spring Data JPA + H2 — 파일 기반 로컬 DB로 요청서 영속화</li>
 *   <li>Google Gemini API — AI 문서 생성</li>
 *   <li>React + TanStack Router — 프론트엔드 SPA (빌드 산출물을 static 리소스로 서빙)</li>
 * </ul>
 *
 * <p>{@code @SpringBootApplication}은 다음 세 어노테이션을 합친 메타 어노테이션이다:
 * <ul>
 *   <li>{@code @Configuration} — 이 클래스가 빈 정의 소스임을 선언</li>
 *   <li>{@code @EnableAutoConfiguration} — Spring Boot 자동 설정 활성화</li>
 *   <li>{@code @ComponentScan} — 현재 패키지({@code com.nice.qa}) 하위를 컴포넌트 스캔</li>
 * </ul>
 */
@SpringBootApplication
public class NiceQaApplication {

    /**
     * 애플리케이션 기동 메서드 (Application startup method).
     *
     * <p>{@link SpringApplication#run(Class, String[])}을 호출하여
     * Spring 컨텍스트를 초기화하고 내장 Tomcat 서버를 시작한다.
     *
     * @param args JVM에 전달된 커맨드라인 인수 (command-line arguments passed to the JVM)
     */
    public static void main(String[] args) {
        // Spring Boot 컨텍스트 초기화 및 내장 서버 기동
        // (bootstrap the Spring application context and start the embedded server)
        SpringApplication.run(NiceQaApplication.class, args);
    }

}
