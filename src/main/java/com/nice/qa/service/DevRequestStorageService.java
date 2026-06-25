package com.nice.qa.service;

import com.nice.qa.entity.DevRequest;
import com.nice.qa.entity.DevRequestStatus;
import com.nice.qa.model.api.dto.DevRequestSaveRequest;
import com.nice.qa.repository.DevRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발요청서 저장/조회/수정/소프트삭제.
 * 생성(generate)은 무저장이고, 사용자가 명시적으로 [저장]을 누를 때 이 서비스가 호출된다.
 *
 * <p>이 서비스는 {@link DevRequest} 엔티티의 CRUD 영속 계층 역할을 담당한다.
 * LLM 기반 문서 생성({@link DocService})과는 완전히 분리되어 있으며,
 * 사용자가 생성된 문서를 확인하고 명시적으로 저장 요청을 할 때만 동작한다.
 *
 * <h3>소프트 삭제(Soft Delete) 정책</h3>
 * 물리 삭제 대신 {@code deleted} 플래그를 {@code true}로 설정하는 방식을 사용한다.
 * {@code @SQLDelete} 어노테이션이 JPA의 {@code delete()} 호출을
 * {@code UPDATE deleted=true} 쿼리로 자동 치환하며,
 * 조회 시에는 {@code findByIdAndDeletedFalse} 같은 조건부 쿼리로 삭제된 항목을 제외한다.
 *
 * @see DevRequest
 * @see DevRequestRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevRequestStorageService {

    /** 개발요청서 JPA 리포지토리 */
    private final DevRequestRepository repository;

    /**
     * 새 개발요청서를 데이터베이스에 저장한다.
     *
     * <p>요청 DTO의 각 필드를 {@link DevRequest#of} 팩토리 메서드에 전달하여
     * 엔티티를 생성하고 즉시 저장한다.
     * status가 null이면 엔티티의 기본값({@code DRAFT})이 적용된다.
     *
     * @param req 저장할 개발요청서 정보를 담은 DTO
     * @return 저장된 {@link DevRequest} 엔티티 (자동 생성된 ID 포함)
     */
    @Transactional
    public DevRequest create(DevRequestSaveRequest req) {
        DevRequest entity = DevRequest.of(
                req.title(),
                req.categoryPath(),
                req.status(),                // null이면 엔티티에서 DRAFT로 기본
                req.author(),
                req.dept(),
                req.details(),
                req.combinedMarkdown(),
                req.flowDiagram(),
                req.unconfirmedSection()
        );
        DevRequest saved = repository.save(entity);
        log.info("DevRequest 저장: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * 기존 개발요청서를 부분 갱신한다.
     *
     * <p>요청 DTO에서 null이 아닌 필드만 선택적으로 덮어쓴다.
     * 이는 위저드의 [이어쓰기] 같은 흐름에서 일부 필드만 보내도
     * 기존 데이터가 유실되지 않도록 보장하기 위한 설계이다.
     *
     * <p>엔티티를 직접 반환하면 JPA 더티 체킹(dirty checking)에 의해
     * 트랜잭션 커밋 시 변경된 필드만 자동으로 UPDATE 쿼리가 실행된다.
     *
     * @param id  수정할 개발요청서의 식별자
     * @param req 수정할 필드를 담은 DTO (null 필드는 변경하지 않음)
     * @return 변경이 반영된 {@link DevRequest} 엔티티
     * @throws EntityNotFoundException 해당 ID의 요청서가 없거나 이미 삭제된 경우
     */
    @Transactional
    public DevRequest update(Long id, DevRequestSaveRequest req) {
        DevRequest entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("DevRequest not found: id=" + id));

        // 부분 갱신: null이 아닌 필드만 덮어쓴다. 위저드의 [이어쓰기] 같은 흐름에서
        // 일부만 보내도 다른 필드가 날아가지 않게.
        if (req.title() != null) entity.setTitle(req.title());
        if (req.categoryPath() != null) entity.setCategoryPath(req.categoryPath());
        if (req.status() != null) entity.setStatus(req.status());
        if (req.author() != null) entity.setAuthor(req.author());
        if (req.dept() != null) entity.setDept(req.dept());
        if (req.details() != null) entity.setDetails(req.details());
        if (req.combinedMarkdown() != null) entity.setCombinedMarkdown(req.combinedMarkdown());
        if (req.flowDiagram() != null) entity.setFlowDiagram(req.flowDiagram());
        if (req.unconfirmedSection() != null) entity.setUnconfirmedSection(req.unconfirmedSection());

        return entity; // 더티 체킹으로 flush
    }

    /**
     * ID로 개발요청서를 단건 조회한다.
     *
     * <p>소프트 삭제된({@code deleted=true}) 요청서는 조회되지 않는다.
     *
     * @param id 조회할 개발요청서의 식별자
     * @return 조회된 {@link DevRequest} 엔티티
     * @throws EntityNotFoundException 해당 ID의 요청서가 없거나 이미 삭제된 경우
     */
    @Transactional(readOnly = true)
    public DevRequest get(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("DevRequest not found: id=" + id));
    }

    /**
     * 검색 조건에 따라 개발요청서 목록을 페이지 단위로 조회한다.
     *
     * <p>모든 파라미터는 선택적이며, null 전달 시 해당 조건은 필터링에서 제외된다.
     * 소프트 삭제된 항목은 리포지토리 쿼리에서 자동 제외된다.
     *
     * @param keyword  제목 또는 내용 키워드 검색어 (null 허용)
     * @param status   요청서 상태 필터 (null 허용)
     * @param category 카테고리 경로 필터 (null 허용)
     * @param author   작성자 필터 (null 허용)
     * @param pageable 페이지 번호, 크기, 정렬 방향 정보
     * @return 조건에 맞는 {@link DevRequest} 페이지 결과
     */
    @Transactional(readOnly = true)
    public Page<DevRequest> search(
            String keyword,
            DevRequestStatus status,
            String category,
            String author,
            Pageable pageable
    ) {
        return repository.search(keyword, status, category, author, pageable);
    }

    /**
     * 다이어그램 XML만 갱신 (flow.png 엔드포인트가 캐시 채울 때 사용).
     *
     * <p>LLM으로 새로 생성된 mxGraph XML을 DB에 저장하는 경량 업데이트 메서드이다.
     * 전체 필드를 수정하는 {@link #update}를 사용하지 않고 다이어그램 필드만
     * 갱신하여 불필요한 필드 검증을 줄인다.
     * JPA 더티 체킹으로 트랜잭션 커밋 시 자동으로 UPDATE 쿼리가 실행된다.
     *
     * @param id  수정할 개발요청서의 식별자
     * @param xml 저장할 mxGraph XML 문자열
     * @throws EntityNotFoundException 해당 ID의 요청서가 없거나 이미 삭제된 경우
     */
    @Transactional
    public void updateFlowDiagram(Long id, String xml) {
        DevRequest entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("DevRequest not found: id=" + id));
        entity.setFlowDiagram(xml);
        // 더티 체킹으로 flush
    }

    /**
     * 개발요청서를 소프트 삭제한다.
     *
     * <p>물리 삭제 대신 {@code deleted} 플래그를 {@code true}로 설정한다.
     * {@code DevRequest} 엔티티에 선언된 {@code @SQLDelete} 어노테이션이
     * JPA {@code repository.delete()} 호출을 자동으로
     * {@code UPDATE dev_request SET deleted = true WHERE id = ?} 쿼리로 치환한다.
     *
     * @param id 삭제할 개발요청서의 식별자
     * @throws EntityNotFoundException 해당 ID의 요청서가 없거나 이미 삭제된 경우
     */
    @Transactional
    public void softDelete(Long id) {
        DevRequest entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("DevRequest not found: id=" + id));
        // @SQLDelete 어노테이션이 delete()를 UPDATE deleted=true 로 갈아끼움
        repository.delete(entity);
        log.info("DevRequest 소프트 삭제: id={}", id);
    }
}
