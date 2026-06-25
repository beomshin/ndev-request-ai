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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevRequestStorageService {

    private final DevRequestRepository repository;

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

    @Transactional(readOnly = true)
    public DevRequest get(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("DevRequest not found: id=" + id));
    }

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

    /** 다이어그램 XML만 갱신 (flow.png 엔드포인트가 캐시 채울 때 사용). */
    @Transactional
    public void updateFlowDiagram(Long id, String xml) {
        DevRequest entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("DevRequest not found: id=" + id));
        entity.setFlowDiagram(xml);
        // 더티 체킹으로 flush
    }

    @Transactional
    public void softDelete(Long id) {
        DevRequest entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("DevRequest not found: id=" + id));
        // @SQLDelete 어노테이션이 delete()를 UPDATE deleted=true 로 갈아끼움
        repository.delete(entity);
        log.info("DevRequest 소프트 삭제: id={}", id);
    }
}
