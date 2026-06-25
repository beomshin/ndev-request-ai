package com.nice.qa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA(Single Page Application) 클라이언트 라우팅 Fallback 컨트롤러.
 *
 * <p>Vite 빌드 결과물은 {@code src/main/resources/static/}에 배치되며,
 * Spring Boot가 정적 자원으로 직접 서빙한다.
 * 그러나 {@code /new}, {@code /result}, {@code /knowledge} 같은 클라이언트 측 라우트는
 * 서버에 해당 경로의 실제 파일이 없으므로, 이 컨트롤러가 모든 요청을 {@code index.html}로
 * 포워딩하여 React Router(또는 TanStack Router)가 라우팅을 처리하게 한다.
 *
 * <p>제외 경로 (정규식으로 매핑 대상에서 제외):
 * <ul>
 *   <li>{@code /api/**} — REST API 엔드포인트</li>
 *   <li>{@code /swagger-ui/**}, {@code /v3/**} — API 문서</li>
 *   <li>{@code /assets/**} — 정적 번들 자원 (JS, CSS, 이미지)</li>
 *   <li>{@code /h2-console/**} — H2 인메모리 DB 콘솔 (개발 환경)</li>
 *   <li>{@code /favicon.ico} — 파비콘</li>
 *   <li>확장자가 있는 경로 (예: {@code .js}, {@code .css}) — 정적 파일로 간주</li>
 * </ul>
 */
@Controller
public class SpaFallbackController {

    /**
     * 클라이언트 라우트 요청을 {@code index.html}로 포워딩한다.
     *
     * <p>매핑 패턴 설명:
     * <ul>
     *   <li>{@code "/"} — 루트 경로, {@code index.html}을 직접 서빙한다.</li>
     *   <li>{@code "/{path:^(?!api|swagger-ui|v3|assets|h2-console|favicon\\.ico)[^.]+}"}
     *       — 한 단계 경로({@code /new}, {@code /result} 등).
     *       제외 키워드로 시작하거나 점(.)이 포함된 경로(파일)는 제외한다.</li>
     *   <li>{@code "/{path:^(?!api|swagger-ui|v3|assets|h2-console)[^.]+}/{sub:[^.]+}"}
     *       — 두 단계 경로({@code /knowledge/detail} 등). 마찬가지로 점 포함 경로는 제외한다.</li>
     * </ul>
     *
     * @return {@code "forward:/index.html"} — Spring MVC 내부 포워딩, 새 HTTP 요청 없이 처리됨
     */
    // 루트(/)는 index.html을 직접 서빙하고,
    // 한 단계 경로(/new, /result 등)와 두 단계까지 모두 fallback.
    // /api/** 는 매핑되지 않으므로 영향 없음.
    @GetMapping(value = {
            "/",
            "/{path:^(?!api|swagger-ui|v3|assets|h2-console|favicon\\.ico)[^.]+}",
            "/{path:^(?!api|swagger-ui|v3|assets|h2-console)[^.]+}/{sub:[^.]+}"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
