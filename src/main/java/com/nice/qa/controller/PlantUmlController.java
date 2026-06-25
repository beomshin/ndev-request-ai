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
 * PlantUML 텍스트를 SVG/PNG로 렌더링.
 * knowledge_base md 본문에 들어있는 ```plantuml ... ``` 코드 블록을
 * FE 마크다운 렌더러가 가로채 본 엔드포인트로 POST → 이미지로 표시.
 */
@Slf4j
@RestController
@RequestMapping("/api/plantuml")
public class PlantUmlController {

    @PostMapping(value = "/render", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> render(
            @RequestBody String source,
            @RequestParam(name = "format", defaultValue = "svg") String format
    ) {
        if (source == null || source.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        boolean png = "png".equalsIgnoreCase(format);
        FileFormat ff = png ? FileFormat.PNG : FileFormat.SVG;
        MediaType ct = png ? MediaType.IMAGE_PNG : MediaType.valueOf("image/svg+xml");

        // @startuml ~ @enduml 누락 시 자동 보정 — md 본문에서 코드 펜스만 떼면 보통 누락된 채 오기 때문
        String normalized = ensureStartEndTags(source);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            SourceStringReader reader = new SourceStringReader(normalized);
            reader.outputImage(out, new FileFormatOption(ff));
            byte[] bytes = out.toByteArray();
            if (bytes.length == 0) {
                log.warn("[PlantUML] 빈 응답 — source 앞 80자: {}", normalized.substring(0, Math.min(80, normalized.length())));
                return ResponseEntity.unprocessableEntity().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(ct);
            headers.setCacheControl("public, max-age=300");
            return new ResponseEntity<>(bytes, headers, 200);
        } catch (IOException e) {
            log.error("[PlantUML] 렌더 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String ensureStartEndTags(String s) {
        String t = s.trim();
        if (!t.contains("@startuml")) t = "@startuml\n" + t;
        if (!t.contains("@enduml")) t = t + "\n@enduml";
        return t;
    }
}
