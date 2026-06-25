package com.nice.qa.controller;

import com.nice.qa.service.knowledge.kb.KnowledgeBaseService;
import com.nice.qa.service.knowledge.kb.KnowledgeDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 지식 저장소(Knowledge Base) 화면 백엔드 컨트롤러.
 *
 * <p>지식 저장소는 {@code docs/knowledge_base/} 디렉터리 하위의 마크다운 파일들로 구성되며,
 * provider·ui·webapi 등 카테고리별 폴더 구조를 가진다.
 * 이 컨트롤러는 해당 문서들의 메타데이터(목록)와 본문(단건 상세)을 FE에 제공한다.
 *
 * <p>제공 엔드포인트:
 * <ul>
 *   <li>{@code GET /api/knowledge} — 전체 문서 요약 목록 (본문 제외)</li>
 *   <li>{@code GET /api/knowledge/{id}} — 단건 상세 (frontmatter + 본문 마크다운 포함)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    /**
     * 지식 저장소 서비스 — 로컬 MD 파일을 스캔하여 파싱·캐싱하고
     * 목록/단건 조회 기능을 제공한다.
     */
    private final KnowledgeBaseService service;

    /**
     * 지식 저장소 문서 목록 조회 (GET /api/knowledge).
     *
     * <p>FE 지식 저장소 목록 화면에서 호출된다.
     * 렌더링 비용이 큰 마크다운 본문은 제외하고 표시용 메타데이터만 반환하여 응답 크기를 줄인다.
     *
     * @return 각 문서의 요약 메타데이터({@link KnowledgeSummary}) 리스트
     */
    @GetMapping
    public List<KnowledgeSummary> list() {
        // 전체 KnowledgeDoc에서 본문을 제외한 요약 레코드로 변환하여 반환한다
        return service.list().stream().map(KnowledgeSummary::from).toList();
    }

    /**
     * 지식 저장소 단건 문서 조회 (GET /api/knowledge/{id}).
     *
     * <p>FE 문서 상세 화면에서 호출된다. frontmatter(메타데이터) 전체와 마크다운 본문을 함께 반환한다.
     *
     * <p>경로 변수 {@code id}에 점(.)이 포함되는 경우(예: {@code payment.v2.md})
     * Spring MVC의 기본 동작이 마지막 점 이후를 확장자로 인식해 잘라낼 수 있다.
     * 이를 방지하기 위해 정규식 {@code :.+}으로 점을 포함한 전체 문자열을 허용한다.
     *
     * @param id 조회할 문서 ID (파일명 기반, 점 포함 가능)
     * @return 200 OK + {@link KnowledgeDoc}, 존재하지 않으면 404 Not Found
     */
    @GetMapping("/{id:.+}")
    public ResponseEntity<KnowledgeDoc> get(@PathVariable String id) {
        // Optional로 감싸진 결과를 HTTP 응답으로 변환한다 — 없으면 404
        return service.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 지식 문서 요약 레코드 — FE 목록 화면 표시용.
     *
     * <p>{@link KnowledgeDoc}에서 본문(markdown content)을 제외한
     * 메타데이터 필드만을 담는다. 네트워크 전송량을 줄이고
     * FE 목록 렌더링 성능을 최적화하는 것이 목적이다.
     *
     * @param id          문서 고유 식별자 (파일명 기반)
     * @param title       문서 제목 (frontmatter의 title 필드)
     * @param category    문서 카테고리 (provider / ui / webapi 등)
     * @param version     문서 버전 (frontmatter의 version 필드)
     * @param lastUpdated 마지막 수정일 (frontmatter의 last_updated 필드)
     * @param status      문서 상태 (예: active, deprecated)
     * @param fileSize    파일 크기 문자열 (예: "12KB")
     * @param chunkCount  RAG 청크 분할 수 — AI 검색 시 참조 단위 수
     * @param filename    원본 파일명 (예: payment-method.md)
     */
    public record KnowledgeSummary(
            String id,
            String title,
            String category,
            String version,
            String lastUpdated,
            String status,
            String fileSize,
            Integer chunkCount,
            String filename
    ) {
        /**
         * {@link KnowledgeDoc}에서 요약 레코드를 생성하는 정적 팩토리 메서드.
         *
         * @param d 원본 지식 문서 (본문 포함)
         * @return 본문을 제외한 요약 레코드
         */
        static KnowledgeSummary from(KnowledgeDoc d) {
            return new KnowledgeSummary(
                    d.id(), d.title(), d.category(), d.version(), d.lastUpdated(),
                    d.status(), d.fileSize(), d.chunkCount(), d.filename()
            );
        }
    }
}
