package com.nice.qa.service.intake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 신규 지불수단 등록 폼 스키마 로더.
 * docs/policy/payment_method_intake_form_v1.md 의 YAML frontmatter를 1회 파싱해 메모리 보관.
 */
@Slf4j
@Service
public class IntakeFormService {

    private static final String DOC_PATH = "docs/policy/payment_method_intake_form_v1.md";

    private IntakeForm cached;

    @PostConstruct
    void load() {
        try (InputStream is = new ClassPathResource(DOC_PATH).getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String frontMatter = extractFrontMatter(content);
            JsonNode root = new YAMLMapper().readTree(frontMatter);
            this.cached = parse(root);
            log.info("[IntakeForm] 로드 완료 — fields={}, sections={}",
                    cached.fields().size(), cached.sections().size());
        } catch (IOException e) {
            throw new IllegalStateException("IntakeForm yaml 로드 실패: " + DOC_PATH, e);
        }
    }

    public IntakeForm get() {
        return cached;
    }

    /** md 파일의 `---` … `---` 구간만 추출. */
    private static String extractFrontMatter(String content) {
        int first = content.indexOf("---");
        if (first < 0) throw new IllegalStateException("frontmatter 시작 마커(---) 없음");
        int second = content.indexOf("\n---", first + 3);
        if (second < 0) throw new IllegalStateException("frontmatter 종료 마커(---) 없음");
        return content.substring(first + 3, second);
    }

    private static IntakeForm parse(JsonNode root) {
        List<IntakeForm.Section> sections = new ArrayList<>();
        JsonNode sectionArr = root.path("sections");
        if (sectionArr.isArray()) {
            for (JsonNode n : sectionArr) {
                sections.add(new IntakeForm.Section(
                        n.path("code").asText(),
                        n.path("name").asText(),
                        n.path("order").asInt(0)
                ));
            }
        }

        List<IntakeForm.Field> fields = new ArrayList<>();
        JsonNode fieldArr = root.path("fields");
        if (fieldArr.isArray()) {
            for (JsonNode n : fieldArr) {
                fields.add(parseField(n));
            }
        }

        return new IntakeForm(
                root.path("doc_id").asText(null),
                root.path("title").asText(null),
                root.path("version").asText(null),
                sections,
                fields
        );
    }

    private static IntakeForm.Field parseField(JsonNode n) {
        List<String> options = null;
        JsonNode opt = n.path("options");
        if (opt.isArray()) {
            options = new ArrayList<>();
            for (JsonNode o : opt) options.add(o.asText());
        }

        List<IntakeForm.Field> nested = null;
        JsonNode nestedArr = n.path("fields");
        if (nestedArr.isArray()) {
            nested = new ArrayList<>();
            for (JsonNode f : nestedArr) nested.add(parseField(f));
        }

        Object defVal = null;
        JsonNode dv = n.path("defaultValue");
        if (!dv.isMissingNode() && !dv.isNull()) {
            if (dv.isBoolean()) defVal = dv.asBoolean();
            else if (dv.isNumber()) defVal = dv.numberValue();
            else defVal = dv.asText();
        }

        return new IntakeForm.Field(
                n.path("policyId").asText(null),
                n.path("section").asText(null),
                n.path("label").asText(null),
                n.path("inputType").asText(null),
                n.has("required") ? n.path("required").asBoolean() : null,
                options,
                defVal,
                n.path("placeholder").asText(null),
                n.path("helpText").asText(null),
                n.path("pattern").asText(null),
                n.path("format").asText(null),
                n.path("unit").asText(null),
                n.has("maxLength") ? n.path("maxLength").asInt() : null,
                n.path("key").asText(null),
                nested,
                n.path("sourceDocId").asText(null)
        );
    }

}
