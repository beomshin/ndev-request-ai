package com.nice.qa.repository;

import com.nice.qa.entity.DevRequest;
import com.nice.qa.entity.DevRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 검색은 동적 조건이 있어 Specification으로 합성.
 * 소프트 삭제: 모든 조회는 {@link Specs#notDeleted()} 를 기본 조건으로 끼운다.
 */
public interface DevRequestRepository
        extends JpaRepository<DevRequest, Long>, JpaSpecificationExecutor<DevRequest> {

    /** id로 조회하되 삭제된 건 제외 */
    Optional<DevRequest> findByIdAndDeletedFalse(Long id);

    /** 페이지 조회 헬퍼 — Specs를 합성해 호출 측 코드를 짧게 */
    default Page<DevRequest> search(
            String keyword,
            DevRequestStatus status,
            String category,
            String author,
            Pageable pageable
    ) {
        Specification<DevRequest> spec = Specs.notDeleted()
                .and(Specs.titleLike(keyword))
                .and(Specs.statusEq(status))
                .and(Specs.categoryStartsWith(category))
                .and(Specs.authorEq(author));
        return findAll(spec, pageable);
    }

    /** Specification 모음 — Repository와 같은 파일에 둬서 import 한 곳만으로 끝. */
    final class Specs {
        private Specs() {}

        public static Specification<DevRequest> notDeleted() {
            return (root, q, cb) -> cb.isFalse(root.get("deleted"));
        }

        public static Specification<DevRequest> titleLike(String keyword) {
            if (!StringUtils.hasText(keyword)) return null;
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return (root, q, cb) -> cb.like(cb.lower(root.get("title")), like);
        }

        public static Specification<DevRequest> statusEq(DevRequestStatus status) {
            if (status == null) return null;
            return (root, q, cb) -> cb.equal(root.get("status"), status);
        }

        public static Specification<DevRequest> categoryStartsWith(String category) {
            if (!StringUtils.hasText(category)) return null;
            // categoryPath가 "결제창 / 카드" 형태라 시작·포함 둘 다 매칭되게 contains
            String like = "%" + category.trim().toLowerCase() + "%";
            return (root, q, cb) -> cb.like(cb.lower(root.get("categoryPath")), like);
        }

        public static Specification<DevRequest> authorEq(String author) {
            if (!StringUtils.hasText(author)) return null;
            return (root, q, cb) -> cb.equal(root.get("author"), author.trim());
        }
    }
}
