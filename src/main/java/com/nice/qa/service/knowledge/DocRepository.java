package com.nice.qa.service.knowledge;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * classpath:docs/~/*.md 를 시동 시 한 번 읽어 메모리에 보관.
 * YAML 프론트매터(--- ... ---) + 본문을 분리해 KnowledgeDoc 형태로 저장.
 */
@Component
public class DocRepository {

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

    private static final Pattern FRONT_MATTER =
            Pattern.compile("^---\\s*\\r?\\n(.*?)\\r?\\n---\\s*\\r?\\n", Pattern.DOTALL);
    private static final Pattern META_LINE =
            Pattern.compile("^([\\w-]+)\\s*:\\s*(.+)$");
    private static final Pattern TITLE_LINE =
            Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);

    private List<KnowledgeDoc> docs = List.of();

    @PostConstruct
    void load() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:docs/**/*.md");

        List<KnowledgeDoc> loaded = new ArrayList<>();
        for (Resource res : resources) {
            String filename = res.getFilename();
            if (filename == null) continue;
            String id = filename.replaceFirst("\\.md$", "");

            String raw;
            try (var in = res.getInputStream()) {
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            Map<String, String> meta = new LinkedHashMap<>();
            List<String> tags = new ArrayList<>();
            String body = raw;

            Matcher fm = FRONT_MATTER.matcher(raw);
            if (fm.find()) {
                String header = fm.group(1);
                body = raw.substring(fm.end());
                for (String line : header.split("\\r?\\n")) {
                    Matcher ml = META_LINE.matcher(line.trim());
                    if (!ml.matches()) continue;
                    String key = ml.group(1);
                    String value = ml.group(2).trim();

                    if (value.startsWith("[") && value.endsWith("]")) {
                        // 배열은 tags 한정으로 처리
                        if ("tags".equals(key)) {
                            for (String t : value.substring(1, value.length() - 1).split(",")) {
                                String cleaned = t.trim().replaceAll("^\"|\"$", "");
                                if (!cleaned.isEmpty()) tags.add(cleaned);
                            }
                        }
                        continue;
                    }
                    meta.put(key, value.replaceAll("^\"|\"$", ""));
                }
            }

            String title = id;
            Matcher tm = TITLE_LINE.matcher(body);
            if (tm.find()) title = tm.group(1).trim();

            loaded.add(new KnowledgeDoc(
                    id, extractTopFolder(res),
                    "classpath:docs/" + extractTopFolder(res) + "/" + filename,
                    meta, Collections.unmodifiableList(tags), title, body));
        }
        this.docs = Collections.unmodifiableList(loaded);
    }

    public int size() {
        return docs.size();
    }

    // 메타의 last_updated 중 가장 최신값
    public String latestUpdate() {
        return docs.stream()
                .map(d -> d.meta().get("last_updated"))
                .filter(s -> s != null && !s.isBlank())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    // 키워드 매칭 점수 기반 단순 검색. tag 가중치 2.0 / 제목 1.5 / 본문 1.0.
    public List<KnowledgeDoc> search(List<String> keywords, String topFolderFilter, int limit) {
        return docs.stream()
                .filter(d -> topFolderFilter == null || topFolderFilter.isEmpty()
                        || topFolderFilter.equals(d.topFolder()))
                .map(d -> Map.entry(d, score(d, keywords)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<KnowledgeDoc> byFolder(String topFolder) {
        return docs.stream().filter(d -> topFolder.equals(d.topFolder())).toList();
    }

    private double score(KnowledgeDoc d, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0;
        double s = 0;
        String haystack = (d.title() + " " + d.body()).toLowerCase();
        for (String kw : keywords) {
            if (kw == null || kw.isBlank()) continue;
            String k = kw.toLowerCase();
            if (d.tags().stream().anyMatch(t -> t.toLowerCase().contains(k))) s += 2.0;
            if (d.title().toLowerCase().contains(k)) s += 1.5;
            if (haystack.contains(k)) s += 1.0;
        }
        return s;
    }

    // 공백/쉼표/슬래시 단위로 토큰 분리
    public static List<String> tokenize(String... parts) {
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p == null) continue;
            for (String t : p.split("[\\s,/]+")) {
                if (!t.isBlank()) tokens.add(t);
            }
        }
        return tokens;
    }

    public static String excerpt(String body, int maxChars) {
        if (body == null) return "";
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= maxChars ? compact : compact.substring(0, maxChars) + "…";
    }

    private String extractTopFolder(Resource res) {
        try {
            String url = res.getURL().toString();
            int idx = url.indexOf("/docs/");
            if (idx < 0) return "";
            String tail = url.substring(idx + "/docs/".length());
            int slash = tail.indexOf('/');
            return slash < 0 ? "" : tail.substring(0, slash);
        } catch (IOException e) {
            return "";
        }
    }
}
