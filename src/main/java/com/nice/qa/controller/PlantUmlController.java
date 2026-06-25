package com.nice.qa.controller;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * PlantUML 소스 텍스트를 SVG 또는 PNG 이미지로 렌더링하는 컨트롤러.
 *
 * <p>지식 저장소의 마크다운 파일에는 {@code ```plantuml ... ```} 형식의 코드 블록이 포함될 수 있다.
 * FE의 마크다운 렌더러가 해당 블록을 감지하면 PlantUML 소스 텍스트를 이 엔드포인트로 POST하여
 * 렌더링된 이미지를 받아 화면에 삽입한다.
 *
 * <p>렌더링 라이브러리: {@code net.sourceforge.plantuml} (PlantUML 오픈소스 라이브러리)
 */
@Slf4j
@RestController
@RequestMapping("/api/plantuml")
public class PlantUmlController {

    /**
     * PlantUML 소스 텍스트를 이미지로 렌더링 (POST /api/plantuml/render).
     *
     * <p>요청 바디에 PlantUML 소스 텍스트(plain text)를 받아 SVG 또는 PNG로 변환한다.
     *
     * <p>처리 흐름:
     * <ol>
     *   <li>빈 입력 검증 — 소스가 없으면 400 Bad Request 반환.</li>
     *   <li>포맷 선택 — {@code format=png}이면 PNG, 그 외는 SVG(기본값).</li>
     *   <li>태그 정규화 — {@code @startuml}/{@code @enduml} 누락 시 자동으로 추가.</li>
     *   <li>PlantUML 렌더링 — {@link SourceStringReader}로 이미지를 생성.</li>
     *   <li>빈 결과 처리 — 렌더링 결과가 비어 있으면 422 Unprocessable Entity 반환.</li>
     * </ol>
     *
     * @param source PlantUML 소스 텍스트 (Content-Type: text/plain)
     * @param format 출력 포맷 — {@code "svg"}(기본값) 또는 {@code "png"}
     * @return 렌더링된 이미지 바이트, Cache-Control: public, max-age=300
     */
    @PostMapping(value = "/render", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> render(
            @RequestBody String source,
            @RequestParam(name = "format", defaultValue = "svg") String format
    ) {
        // 빈 입력 검증 — FE에서 코드 펜스만 있고 내용이 없는 경우를 방어한다
        if (source == null || source.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 요청된 포맷에 따라 PlantUML FileFormat과 HTTP Content-Type을 결정한다
        boolean png = "png".equalsIgnoreCase(format);
        FileFormat ff = png ? FileFormat.PNG : FileFormat.SVG;
        MediaType ct = png ? MediaType.IMAGE_PNG : MediaType.valueOf("image/svg+xml");

        // @startuml ~ @enduml 누락 시 자동 보정 — MD 본문에서 코드 펜스(```)만 제거하면
        // 보통 이 태그가 없는 채로 오기 때문에 방어적으로 추가한다
        String normalized = ensureStartEndTags(source);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // PlantUML 라이브러리로 소스 텍스트를 이미지 바이트로 변환한다
            SourceStringReader reader = new SourceStringReader(normalized);
            reader.outputImage(out, new FileFormatOption(ff));
            byte[] bytes = out.toByteArray();

            // 렌더링 결과가 비어 있으면 소스 문법 오류일 가능성이 높다 — 422 반환
            if (bytes.length == 0) {
                log.warn("[PlantUML] 빈 응답 — source 앞 80자: {}", normalized.substring(0, Math.min(80, normalized.length())));
                return ResponseEntity.unprocessableEntity().build();
            }

            // 동일한 다이어그램을 반복 요청하는 것을 방지하기 위해 5분 캐시를 허용한다
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(ct);
            headers.setCacheControl("public, max-age=300");
            return new ResponseEntity<>(bytes, headers, 200);
        } catch (IOException e) {
            log.error("[PlantUML] 렌더 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PlantUML 소스에 {@code @startuml}/{@code @enduml} 태그가 없으면 자동으로 추가한다.
     *
     * <p>FE 마크다운 파서가 코드 펜스({@code ```plantuml ... ```})에서
     * PlantUML 소스를 추출할 때 보통 태그 없이 내용만 전달된다.
     * PlantUML 라이브러리는 이 태그가 없으면 올바르게 파싱하지 못하므로
     * 서버 측에서 방어적으로 보완한다.
     *
     * @param s 원본 PlantUML 소스 텍스트
     * @return {@code @startuml}과 {@code @enduml}이 보장된 정규화된 소스 텍스트
     */
    private static String ensureStartEndTags(String s) {
        String t = s.trim();
        // @startuml이 없으면 맨 앞에 추가한다
        if (!t.contains("@startuml")) t = "@startuml\n" + t;
        // @enduml이 없으면 맨 뒤에 추가한다
        if (!t.contains("@enduml")) t = t + "\n@enduml";
        return t;
    }
}
