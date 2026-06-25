package com.nice.qa.service.knowledge;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * classpath:{@code docs/**\/*.md} 파일을 애플리케이션 기동 시 1회 읽어 메모리에 보관하는 문서 저장소.
 *
 * <p>각 마크다운 파일의 YAML 프론트매터({@code --- ... ---})와 본문을 분리하여
 * {@link KnowledgeDoc} 형태로 저장한다.</p>
 *
 * <h2>폴더 구조 (Folder Structure)</h2>
 * <pre>
 * classpath:docs/
 *   ├── catalog/      — 분기 트리 YAML (직접 읽지 않음, StubKnowledgeClient가 담당)
 *   ├── policy/       — 정책 문서
 *   ├── provider/     — 원천사/제공사 문서
 *   ├── spec/         — 규격서
 *   └── templates/    — 요청서 양식 템플릿
 * </pre>
 *
 * <h2>검색 점수 가중치 (Search Score Weights)</h2>
 * <ul>
 *   <li>태그 일치: +2.0</li>
 *   <li>제목 포함: +1.5</li>
 *   <li>본문 포함: +1.0</li>
 * </ul>
 *
 * @see StubKnowledgeClient
 */
@Component
public class DocRepository {

    /**
     * 메모리에 보관되는 마크다운 문서 1건을 나타내는 불변 레코드.
     *
     * <p>YAML 프론트매터에서 파싱된 메타데이터와 본문이 분리되어 저장된다.</p>
     */
    public record KnowledgeDoc(
            String id,                  // 파일명에서 확장자 뺀 값 (예: provider_kakaopay)
            String topFolder,           // policy / provider / spec / templates
            String path,                // classpath 상대 경로
            Map<String, String> meta,   // YAML frontmatter (단일 값)
            List<String> tags,          // meta의 tags 배열
            String title,               // 본문 첫 # 제목 (없으면 id)
            String body                 // 프론트매터 제외한 본문 전체
    ) {
    }

    /**
     * YAML 프론트매터 블록({@code --- ... ---})을 추출하기 위한 정규식.
     * DOTALL 플래그로 개행 문자를 포함하여 다중 줄을 매칭한다.
     */
    private static final Pattern FRONT_MATTER =
            Pattern.compile("^---\\s*\\r?\\n(.*?)\\r?\\n---\\s*\\r?\\n", Pattern.DOTALL);

    /**
     * YAML 프론트매터의 단일 키-값 라인 파싱용 정규식.
     * 예: {@code last_updated: 2024-01-01}
     */
    private static final Pattern META_LINE =
            Pattern.compile("^([\\w-]+)\\s*:\\s*(.+)$");

    /**
     * 마크다운 본문의 첫 번째 H1 제목({@code # 제목}) 추출용 정규식.
     * MULTILINE 플래그로 각 줄의 시작을 기준으로 매칭한다.
     */
    private static final Pattern TITLE_LINE =
            Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);

    /** 기동 후 메모리에 보관되는 불변 문서 목록 */
    private List<KnowledgeDoc> docs = List.of();

    /**
     * 애플리케이션 기동 시 {@code classpath:docs/**\/*.md} 파일을 모두 스캔하여
     * {@link #docs} 목록을 초기화한다.
     *
     * <p>각 파일에 대해 다음 처리를 수행한다:</p>
     * <ol>
     *   <li>파일명에서 확장자를 제거하여 {@code id} 생성</li>
     *   <li>YAML 프론트매터 파싱 → {@code meta} 및 {@code tags} 분리</li>
     *   <li>본문 첫 H1 제목 추출 → {@code title} 설정 (없으면 {@code id} 사용)</li>
     *   <li>최상위 폴더명 추출 → {@code topFolder} 설정</li>
     * </ol>
     *
     * @throws IOException 리소스 스캔 자체가 실패한 경우 (개별 파일 실패는 무시됨)
     */
    @PostConstruct
    void load() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:docs/**/*.md");

        List<KnowledgeDoc> loaded = new ArrayList<>();
        for (Resource res : resources) {
            String filename = res.getFilename();
            if (filename == null) continue; // 파일명을 알 수 없는 리소스는 건너뜀
            // 파일명에서 .md 확장자를 제거해 문서 ID로 사용
            String id = filename.replaceFirst("\\.md$", "");

            String raw;
            try (var in = res.getInputStream()) {
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            Map<String, String> meta = new LinkedHashMap<>();
            List<String> tags = new ArrayList<>();
            String body = raw; // 프론트매터가 없는 경우 파일 전체가 본문

            Matcher fm = FRONT_MATTER.matcher(raw);
            if (fm.find()) {
                String header = fm.group(1); // 프론트매터 내용 추출
                body = raw.substring(fm.end()); // 프론트매터 이후가 실제 본문
                for (String line : header.split("\\r?\\n")) {
                    Matcher ml = META_LINE.matcher(line.trim());
                    if (!ml.matches()) continue;
                    String key = ml.group(1);
                    String value = ml.group(2).trim();

                    if (value.startsWith("[") && value.endsWith("]")) {
                        // 배열 형태의 값은 tags 필드에 한해서만 파싱 처리
                        if ("tags".equals(key)) {
                            for (String t : value.substring(1, value.length() - 1).split(",")) {
                                // 각 태그 항목의 앞뒤 공백 및 따옴표 제거
                                String cleaned = t.trim().replaceAll("^\"|\"$", "");
                                if (!cleaned.isEmpty()) tags.add(cleaned);
                            }
                        }
                        continue; // 배열 값은 meta 맵에 넣지 않음
                    }
                    // 단일 값: 앞뒤 따옴표 제거 후 meta 맵에 저장
                    meta.put(key, value.replaceAll("^\"|\"$", ""));
                }
            }

            // 본문에서 첫 번째 H1 제목을 찾아 title로 사용; 없으면 id로 대체
            String title = id;
            Matcher tm = TITLE_LINE.matcher(body);
            if (tm.find()) title = tm.group(1).trim();

            loaded.add(new KnowledgeDoc(
                    id, extractTopFolder(res),
                    "classpath:docs/" + extractTopFolder(res) + "/" + filename,
                    meta, Collections.unmodifiableList(tags), title, body));
        }
        // 로드 완료 후 불변 리스트로 교체 (이후 외부 수정 불가)
        this.docs = Collections.unmodifiableList(loaded);
    }

    /**
     * 메모리에 로드된 전체 문서 수를 반환한다.
     *
     * @return 로드된 문서 수
     */
    public int size() {
        return docs.size();
    }

    /**
     * 전체 문서 중 {@code last_updated} 메타값이 가장 최신인 날짜 문자열을 반환한다.
     *
     * <p>문자열 자연 정렬(ISO-8601 형식 가정)을 사용하므로
     * 날짜 형식이 일관되어야 정확한 결과를 보장한다.</p>
     *
     * @return 가장 최신 {@code last_updated} 값, 해당 메타가 없으면 {@code null}
     */
    // 메타의 last_updated 중 가장 최신값
    public String latestUpdate() {
        return docs.stream()
                .map(d -> d.meta().get("last_updated"))
                .filter(s -> s != null && !s.isBlank())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * 키워드 매칭 점수를 기반으로 문서를 검색하여 상위 N건을 반환한다.
     *
     * <p>점수 계산 가중치: 태그 일치 +2.0 / 제목 포함 +1.5 / 본문 포함 +1.0</p>
     * <p>점수가 0인 문서(키워드 미매칭)는 결과에서 제외된다.</p>
     *
     * @param keywords       검색 키워드 목록
     * @param topFolderFilter 검색 대상 폴더명 ({@code null} 또는 빈 문자열이면 전체 폴더 검색)
     * @param limit          반환할 최대 문서 수
     * @return 점수 내림차순으로 정렬된 문서 목록 (최대 {@code limit}건)
     */
    // 키워드 매칭 점수 기반 단순 검색. tag 가중치 2.0 / 제목 1.5 / 본문 1.0.
    public List<KnowledgeDoc> search(List<String> keywords, String topFolderFilter, int limit) {
        return docs.stream()
                // topFolderFilter가 null이거나 빈 문자열이면 폴더 제한 없이 전체 검색
                .filter(d -> topFolderFilter == null || topFolderFilter.isEmpty()
                        || topFolderFilter.equals(d.topFolder()))
                .map(d -> Map.entry(d, score(d, keywords)))
                .filter(e -> e.getValue() > 0) // 점수가 0인 문서(키워드 미매칭)는 제외
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // 점수 내림차순 정렬
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 특정 최상위 폴더({@code topFolder})에 속하는 모든 문서를 반환한다.
     *
     * @param topFolder 폴더명 (예: "spec", "provider", "templates")
     * @return 해당 폴더의 문서 목록 (없으면 빈 리스트)
     */
    public List<KnowledgeDoc> byFolder(String topFolder) {
        return docs.stream().filter(d -> topFolder.equals(d.topFolder())).toList();
    }

    /**
     * 문서 하나에 대한 키워드 매칭 점수를 계산한다.
     *
     * <p>검색은 대소문자를 구분하지 않으며, 다음 가중치를 합산한다:</p>
     * <ul>
     *   <li>태그에 키워드가 포함: +2.0</li>
     *   <li>제목에 키워드가 포함: +1.5</li>
     *   <li>전체 텍스트(제목+본문)에 키워드가 포함: +1.0</li>
     * </ul>
     *
     * @param d        점수를 계산할 문서
     * @param keywords 검색 키워드 목록
     * @return 합산된 점수 (0.0 이상; 키워드가 비었거나 null이면 0.0)
     */
    private double score(KnowledgeDoc d, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0;
        double s = 0;
        // 제목과 본문을 합쳐 소문자화한 검색 대상 문자열 생성
        String haystack = (d.title() + " " + d.body()).toLowerCase();
        for (String kw : keywords) {
            if (kw == null || kw.isBlank()) continue;
            String k = kw.toLowerCase();
            // 태그에 키워드가 포함되면 가장 높은 가중치 부여 (2.0)
            if (d.tags().stream().anyMatch(t -> t.toLowerCase().contains(k))) s += 2.0;
            // 제목에 포함되면 중간 가중치 (1.5)
            if (d.title().toLowerCase().contains(k)) s += 1.5;
            // 전체 텍스트(haystack)에 포함되면 기본 가중치 (1.0)
            if (haystack.contains(k)) s += 1.0;
        }
        return s;
    }

    /**
     * 하나 이상의 문자열을 공백·쉼표·슬래시 기준으로 토큰화하여 키워드 목록을 반환한다.
     *
     * <p>검색 키워드 생성에 사용되며, 각 인자를 개별 토큰으로 분리한다.
     * 예: {@code tokenize("카카오페이 결제", "payment/kakaopay")}
     * → {@code ["카카오페이", "결제", "payment", "kakaopay"]}</p>
     *
     * @param parts 토큰화할 문자열 배열 (null 항목은 무시됨)
     * @return 공백이 아닌 토큰으로만 구성된 리스트
     */
    // 공백/쉼표/슬래시 단위로 토큰 분리
    public static List<String> tokenize(String... parts) {
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p == null) continue; // null 인자는 건너뜀
            for (String t : p.split("[\\s,/]+")) {
                if (!t.isBlank()) tokens.add(t); // 빈 토큰은 추가하지 않음
            }
        }
        return tokens;
    }

    /**
     * 본문을 지정된 최대 글자 수로 자른 미리보기 문자열을 반환한다.
     *
     * <p>연속된 공백 및 개행을 단일 공백으로 압축(compact)한 뒤 자른다.
     * 자른 경우 말줄임표({@code …})를 끝에 붙인다.</p>
     *
     * @param body     원본 본문 (null 허용)
     * @param maxChars 최대 글자 수
     * @return 압축·잘린 미리보기 문자열 (null 입력 시 빈 문자열)
     */
    public static String excerpt(String body, int maxChars) {
        if (body == null) return "";
        // 연속 공백·개행을 단일 공백으로 압축하여 미리보기 가독성 향상
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= maxChars ? compact : compact.substring(0, maxChars) + "…";
    }

    /**
     * 리소스의 URL에서 {@code /docs/} 바로 아래의 최상위 폴더명을 추출한다.
     *
     * <p>예: {@code ...classpath:/docs/provider/kakaopay.md} → {@code "provider"}</p>
     *
     * @param res 폴더명을 추출할 리소스
     * @return 최상위 폴더명 (추출 불가 시 빈 문자열)
     */
    private String extractTopFolder(Resource res) {
        try {
            String url = res.getURL().toString();
            int idx = url.indexOf("/docs/");
            if (idx < 0) return ""; // /docs/ 경로가 없으면 분류 불가
            String tail = url.substring(idx + "/docs/".length());
            int slash = tail.indexOf('/');
            // 슬래시가 없으면 최상위 폴더 하위 구조가 아님
            return slash < 0 ? "" : tail.substring(0, slash);
        } catch (IOException e) {
            return "";
        }
    }
}
