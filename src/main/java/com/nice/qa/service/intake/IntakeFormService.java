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
 *
 * <p>애플리케이션 구동 시 {@link #load()} 메서드가 {@code @PostConstruct}에 의해 자동 실행되어
 * 정책 문서의 YAML frontmatter를 파싱하고 {@link IntakeForm} 객체를 메모리에 캐시한다.
 * 이후 {@link #get()}으로 캐시된 폼 스키마를 반복 조회할 수 있다.
 *
 * <h3>파싱 대상 문서 경로</h3>
 * {@code classpath:docs/policy/payment_method_intake_form_v1.md}
 *
 * <h3>frontmatter 형식</h3>
 * Markdown 파일의 맨 앞 {@code ---} 구분자 사이에 위치한 YAML 블록을 파싱한다.
 * 예시:
 * <pre>
 * ---
 * doc_id: payment_method_intake_form_v1
 * title: 신규 지불수단 등록 요청서
 * version: "1.0"
 * sections:
 *   - code: BASIC
 *     name: 기본 정보
 *     order: 1
 * fields:
 *   - policyId: P001
 *     ...
 * ---
 * </pre>
 *
 * @see IntakeForm
 */
@Slf4j
@Service
public class IntakeFormService {

    /** YAML frontmatter를 파싱할 정책 문서의 클래스패스 경로 */
    private static final String DOC_PATH = "docs/policy/payment_method_intake_form_v1.md";

    /** 파싱 결과를 보관하는 인메모리 캐시 (서버 기동 시 1회 초기화) */
    private IntakeForm cached;

    /**
     * 애플리케이션 구동 시 정책 문서를 읽어 {@link IntakeForm}을 초기화한다.
     *
     * <p>스프링 컨텍스트 초기화 완료 후 {@code @PostConstruct}에 의해 자동 호출된다.
     * 처리 단계:
     * <ol>
     *   <li>클래스패스에서 마크다운 파일을 {@link InputStream}으로 읽음</li>
     *   <li>{@link #extractFrontMatter}로 {@code ---} 구분자 사이의 YAML 추출</li>
     *   <li>{@link YAMLMapper}로 YAML을 {@link JsonNode} 트리로 파싱</li>
     *   <li>{@link #parse}로 {@link IntakeForm} 객체 조립 후 {@link #cached}에 저장</li>
     * </ol>
     *
     * @throws IllegalStateException 파일을 읽을 수 없거나 YAML 파싱이 실패한 경우
     */
    @PostConstruct
    void load() {
        try (InputStream is = new ClassPathResource(DOC_PATH).getInputStream()) {
            // 파일 전체를 UTF-8 문자열로 읽음
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // --- 구분자 사이의 YAML frontmatter 구간만 추출
            String frontMatter = extractFrontMatter(content);
            // YAML 문자열을 Jackson JsonNode 트리로 파싱
            JsonNode root = new YAMLMapper().readTree(frontMatter);
            this.cached = parse(root);
            log.info("[IntakeForm] 로드 완료 — fields={}, sections={}",
                    cached.fields().size(), cached.sections().size());
        } catch (IOException e) {
            throw new IllegalStateException("IntakeForm yaml 로드 실패: " + DOC_PATH, e);
        }
    }

    /**
     * 캐시된 {@link IntakeForm} 스키마를 반환한다.
     *
     * <p>서버 구동 시 1회 파싱된 결과를 그대로 반환하므로 매 호출마다 파일 I/O가 없다.
     *
     * @return 파싱 완료된 {@link IntakeForm} 인스턴스
     */
    public IntakeForm get() {
        return cached;
    }

    /**
     * md 파일의 {@code ---} … {@code ---} 구간만 추출한다.
     *
     * <p>첫 번째 {@code ---} 이후부터 두 번째 {@code \n---} 이전까지의 문자열을
     * YAML frontmatter로 간주하고 반환한다.
     *
     * @param content 마크다운 파일 전체 문자열
     * @return YAML frontmatter 문자열 (구분자 {@code ---} 제외)
     * @throws IllegalStateException frontmatter 시작 또는 종료 마커를 찾지 못한 경우
     */
    private static String extractFrontMatter(String content) {
        // 첫 번째 --- 위치 탐색
        int first = content.indexOf("---");
        if (first < 0) throw new IllegalStateException("frontmatter 시작 마커(---) 없음");
        // 첫 번째 --- 이후에 나오는 \n--- 위치 탐색 (종료 마커)
        int second = content.indexOf("\n---", first + 3);
        if (second < 0) throw new IllegalStateException("frontmatter 종료 마커(---) 없음");
        // 시작 마커 길이(3)만큼 건너뛰고, 종료 마커 직전까지 반환
        return content.substring(first + 3, second);
    }

    /**
     * {@link JsonNode} 트리를 {@link IntakeForm} 객체로 조립한다.
     *
     * <p>루트 노드에서 {@code sections} 배열과 {@code fields} 배열을 각각 순회하며
     * {@link IntakeForm.Section}과 {@link IntakeForm.Field} 목록을 생성한다.
     *
     * @param root YAML frontmatter 전체를 파싱한 루트 {@link JsonNode}
     * @return 조립된 {@link IntakeForm} 객체
     */
    private static IntakeForm parse(JsonNode root) {
        // sections 배열 파싱
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

        // fields 배열 파싱 (각 필드는 재귀적으로 parseField 호출)
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

    /**
     * 단일 필드 {@link JsonNode}를 {@link IntakeForm.Field} 레코드로 변환한다.
     *
     * <p>중첩 구조 처리:
     * <ul>
     *   <li>{@code options} — 배열이면 문자열 목록으로 변환 (select/multiselect 전용)</li>
     *   <li>{@code fields} — 배열이면 재귀 호출로 중첩 필드 목록 생성 (group 전용)</li>
     *   <li>{@code defaultValue} — Boolean, Number, String 타입을 동적으로 구분하여 Object 반환</li>
     * </ul>
     *
     * <p>YAML 노드가 존재하지 않거나 null인 경우 Java null로 매핑된다.
     * {@code required}, {@code maxLength} 같이 기본값이 의미 있는 Boolean/Integer 필드는
     * 노드 존재 여부({@code has})를 먼저 확인하여 null과 false/0을 구분한다.
     *
     * @param n 파싱할 단일 필드 {@link JsonNode}
     * @return 변환된 {@link IntakeForm.Field} 레코드
     */
    private static IntakeForm.Field parseField(JsonNode n) {
        // select / multiselect 전용 선택지 목록 파싱
        List<String> options = null;
        JsonNode opt = n.path("options");
        if (opt.isArray()) {
            options = new ArrayList<>();
            for (JsonNode o : opt) options.add(o.asText());
        }

        // group 타입의 중첩 필드 재귀 파싱
        List<IntakeForm.Field> nested = null;
        JsonNode nestedArr = n.path("fields");
        if (nestedArr.isArray()) {
            nested = new ArrayList<>();
            for (JsonNode f : nestedArr) nested.add(parseField(f));
        }

        // defaultValue는 Boolean / Number / String 세 가지 타입이 가능하므로
        // JsonNode 타입을 확인한 후 적절한 Java 타입으로 변환
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
                // required는 YAML에 명시된 경우에만 Boolean 값으로 설정 (미명시 시 null)
                n.has("required") ? n.path("required").asBoolean() : null,
                options,
                defVal,
                n.path("placeholder").asText(null),
                n.path("helpText").asText(null),
                n.path("pattern").asText(null),
                n.path("format").asText(null),
                n.path("unit").asText(null),
                // maxLength도 YAML에 명시된 경우에만 Integer 값으로 설정 (미명시 시 null)
                n.has("maxLength") ? n.path("maxLength").asInt() : null,
                n.path("key").asText(null),
                nested,
                n.path("sourceDocId").asText(null)
        );
    }

}
