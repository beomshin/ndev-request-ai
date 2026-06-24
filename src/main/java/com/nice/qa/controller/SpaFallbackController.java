package com.nice.qa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA fallback — /api/** 및 정적 자원 외의 모든 경로를 index.html 로 forward.
 * Vite 빌드 결과물이 src/main/resources/static/ 에 떨어지면 Spring Boot가 그대로 서빙하고,
 * /new · /result · /knowledge 같은 클라이언트 라우트는 이 컨트롤러를 통해 index.html 을 받는다.
 */
@Controller
public class SpaFallbackController {

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
