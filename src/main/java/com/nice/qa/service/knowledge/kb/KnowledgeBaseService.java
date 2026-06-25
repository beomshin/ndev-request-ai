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
 * 지식저장소(Knowledge Base) 문서 목록 서비스.
 *
 * <p>애플리케이션 기동 시 {@code src/main/resources/docs/knowledge_base/} 하위의
 * {@code provider}, {@code ui}, {@code webapi} 폴더에 있는 마크다운({@code .md}) 파일을 스캔하여
 * 각 파일의 YAML 프론트매터를 파싱하고 메모리에 보관한다.</p>
 *
 * <h2>스캔 대상 경로 (Scan Target)</h2>
 * <pre>{@code classpath*:docs/knowledge_base/**\/*.md}</pre>
 *
 * <h2>주요 사용처 (Primary Usage)</h2>
 * <ul>
 *   <li>프론트엔드 지식저장소 화면 — 문서 목록 조회({@link #list()}) 및 단건 조회({@link #get(String)})</li>
 * </ul>
 *
 * <h2>파싱 규칙 (Parsing Rules)</h2>
 * <ul>
 *   <li>프론트매터 없는 파일은 파싱을 건너뜀</li>
 *   <li>{@code doc_id}가 없으면 파일명(확장자 제외)을 fallback ID로 사용</li>
 *   <li>파싱 실패 시 해당 파일만 경고 로그 후 무시 (서비스 기동 중단 없음)</li>
 * </ul>
 *
 * @see KnowledgeDoc
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    /** knowledge_base 마크다운 파일 스캔 패턴 (classpath* 로 JAR 내부까지 탐색) */
    private static final String SCAN_PATTERN = "classpath*:docs/knowledge_base/**/*.md";

    /**
     * 문서 ID를 키로, {@link KnowledgeDoc}을 값으로 하는 순서 보존 맵.
     * 삽입 순서(스캔 순서)가 유지된다.
     */
    private final Map<String, KnowledgeDoc> byId = new LinkedHashMap<>();

    /** YAML 프론트매터 파싱에 사용하는 잭슨 YAML 매퍼 */
    private final YAMLMapper yaml = new YAMLMapper();

    /**
     * 애플리케이션 기동 시 {@value #SCAN_PATTERN} 경로의 마크다운 파일을 모두 스캔하여
     * {@link #byId} 맵을 초기화한다.
     *
     * <p>개별 파일 파싱 실패는 경고 로그만 남기고 계속 진행하므로,
     * 일부 파일에 문제가 있어도 나머지 파일은 정상 로드된다.</p>
     *
     * @throws IllegalStateException 리소스 스캔 자체({@code getResources}) 가 실패한 경우
     */
    @PostConstruct
    void scan() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(SCAN_PATTERN);
            for (Resource res : resources) {
                try (InputStream is = res.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    KnowledgeDoc doc = parse(res.getFilename(), content);
                    // doc_id(또는 파일명)를 키로 맵에 등록; null 반환(프론트매터 없음)은 무시
                    if (doc != null) byId.put(doc.id(), doc);
                } catch (Exception e) {
                    // 개별 파일 파싱 실패는 경고 로그만 남기고 계속 진행
                    log.warn("[KB] 파싱 실패: {} - {}", res.getFilename(), e.getMessage());
                }
            }
            log.info("[KB] knowledge_base 스캔 완료 — 문서 {}건", byId.size());
        } catch (IOException e) {
            throw new IllegalStateException("knowledge_base 스캔 실패", e);
        }
    }

    /**
     * 메모리에 로드된 모든 지식저장소 문서 목록을 반환한다.
     *
     * <p>반환된 리스트는 스캔 순서(삽입 순서)를 유지하는 새로운 가변 리스트이다.</p>
     *
     * @return {@link KnowledgeDoc} 목록 (로드된 문서가 없으면 빈 리스트)
     */
    public List<KnowledgeDoc> list() {
        return new ArrayList<>(byId.values());
    }

    /**
     * 주어진 ID에 해당하는 지식저장소 문서를 반환한다.
     *
     * @param id 조회할 문서 ID ({@code doc_id} 메타값 또는 파일명 기반 fallback ID)
     * @return 해당 문서의 {@link Optional}, 없으면 {@link Optional#empty()}
     */
    public Optional<KnowledgeDoc> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * 마크다운 파일 내용을 파싱하여 {@link KnowledgeDoc} 인스턴스를 생성한다.
     *
     * <p>파싱 절차:</p>
     * <ol>
     *   <li>첫 번째 {@code ---}를 찾아 프론트매터 시작 지점 확인</li>
     *   <li>두 번째 {@code ---}를 찾아 프론트매터 종료 지점 확인</li>
     *   <li>프론트매터 문자열을 YAML로 파싱하여 {@link JsonNode} 추출</li>
     *   <li>CRLF/LF 차이를 고려해 본문 시작 위치 계산</li>
     *   <li>{@code doc_id} 없으면 파일명(확장자 제외)을 fallback ID로 사용</li>
     * </ol>
     *
     * @param filename 파일명 (확장자 포함, null 허용)
     * @param content  파일 전체 내용
     * @return 파싱된 {@link KnowledgeDoc}, 프론트매터가 없는 경우 {@code null}
     * @throws IOException YAML 파싱 중 오류 발생 시
     */
    /** `---` ... `---` frontmatter + 본문 분리 후 {@link KnowledgeDoc}로 매핑. */
    private KnowledgeDoc parse(String filename, String content) throws IOException {
        int first = content.indexOf("---");
        if (first < 0) return null; // 프론트매터 없는 파일은 건너뜀
        int second = content.indexOf("\n---", first + 3);
        if (second < 0) return null; // 닫는 --- 없으면 형식 불완전 — 건너뜀

        // first+3 ~ second 사이가 YAML 프론트매터 내용
        String fm = content.substring(first + 3, second);
        // 본문은 두 번째 `---` 다음 줄부터
        int bodyStart = second + 4;
        // CRLF/LF 모두 첫 줄바꿈을 건너뛴다 (Windows 환경 호환)
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
                meta.path("title").asText(filename),       // title 없으면 파일명으로 대체
                meta.path("category").asText(null),         // 원천사 규격 / 결제창 / WEB API 등
                meta.path("version").asText(null),
                meta.path("last_updated").asText(null),
                meta.path("status").asText(null),
                meta.path("file_size").asText(null),
                meta.has("chunk_count") ? meta.path("chunk_count").asInt() : null, // 청크 수 없으면 null
                filename,
                meta,
                body
        );
    }
}
