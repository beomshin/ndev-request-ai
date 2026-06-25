package com.nice.qa.service.knowledge.kb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * src/main/resources/docs/knowledge_base/{provider,ui,webapi}/*.md 스캔.
 * 각 md 파일의 yaml frontmatter를 1회 파싱해 메모리 보관. FE 지식저장소 화면이 사용.
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private static final String SCAN_PATTERN = "classpath*:docs/knowledge_base/**/*.md";

    private final Map<String, KnowledgeDoc> byId = new LinkedHashMap<>();
    private final YAMLMapper yaml = new YAMLMapper();

    @PostConstruct
    void scan() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(SCAN_PATTERN);
            for (Resource res : resources) {
                try (InputStream is = res.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    KnowledgeDoc doc = parse(res.getFilename(), content);
                    if (doc != null) byId.put(doc.id(), doc);
                } catch (Exception e) {
                    log.warn("[KB] 파싱 실패: {} - {}", res.getFilename(), e.getMessage());
                }
            }
            log.info("[KB] knowledge_base 스캔 완료 — 문서 {}건", byId.size());
        } catch (IOException e) {
            throw new IllegalStateException("knowledge_base 스캔 실패", e);
        }
    }

    public List<KnowledgeDoc> list() {
        return new ArrayList<>(byId.values());
    }

    public Optional<KnowledgeDoc> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** `---` ... `---` frontmatter + 본문 분리 후 {@link KnowledgeDoc}로 매핑. */
    private KnowledgeDoc parse(String filename, String content) throws IOException {
        int first = content.indexOf("---");
        if (first < 0) return null;
        int second = content.indexOf("\n---", first + 3);
        if (second < 0) return null;

        String fm = content.substring(first + 3, second);
        // 본문은 두 번째 `---` 다음 줄부터
        int bodyStart = second + 4;
        // CRLF/LF 모두 첫 줄바꿈을 건너뛴다
        if (bodyStart < content.length() && content.charAt(bodyStart) == '\r') bodyStart++;
        if (bodyStart < content.length() && content.charAt(bodyStart) == '\n') bodyStart++;
        String body = bodyStart < content.length() ? content.substring(bodyStart) : "";

        JsonNode meta = yaml.readTree(fm);

        String docId = meta.path("doc_id").asText(null);
        if (docId == null || docId.isBlank()) {
            // doc_id 없으면 파일명을 fallback id로
            docId = filename != null ? filename.replaceAll("\\.md$", "") : UUID.randomUUID().toString();
        }

        return new KnowledgeDoc(
                docId,
                meta.path("title").asText(filename),
                meta.path("category").asText(null),
                meta.path("version").asText(null),
                meta.path("last_updated").asText(null),
                meta.path("status").asText(null),
                meta.path("file_size").asText(null),
                meta.has("chunk_count") ? meta.path("chunk_count").asInt() : null,
                filename,
                meta,
                body
        );
    }
}
