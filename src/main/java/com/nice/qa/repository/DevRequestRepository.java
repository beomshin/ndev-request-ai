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
 * {@link DevRequest} 엔티티에 대한 JPA Repository (JPA Repository for DevRequest entities).
 *
 * <p>목록 조회 시 검색 조건이 동적으로 변하므로 JPA Criteria API 기반의
 * {@link Specification}을 조합하여 쿼리를 생성한다 ({@link JpaSpecificationExecutor} 확장).
 *
 * <p>소프트 삭제 정책: 모든 조회 쿼리에는 {@link Specs#notDeleted()} 조건이 반드시 포함되어야 한다.
 * {@code @SQLDelete}가 DELETE SQL을 UPDATE로 대체하므로, 조건을 생략하면 삭제된 데이터가 노출된다.
 *
 * <p>기본 CRUD 메서드는 {@link JpaRepository}가 제공하며, 이 인터페이스에서는
 * 소프트 삭제 필터링이 필요한 조회와 동적 검색에 대한 메서드만 추가로 정의한다.
 */
public interface DevRequestRepository
        extends JpaRepository<DevRequest, Long>, JpaSpecificationExecutor<DevRequest> {

    /**
     * ID로 단건을 조회하되 소프트 삭제된 레코드는 제외한다
     * (Find a single record by ID, excluding soft-deleted entries).
     *
     * <p>Spring Data JPA 메서드 이름 규칙({@code findBy...And...})으로 자동 구현된다.
     * {@code deletedFalse} 조건이 포함되어 {@code WHERE id = ? AND deleted = false} 쿼리가 생성된다.
     *
     * @param id 조회할 요청서의 기본키 (primary key of the request to retrieve)
     * @return 삭제되지 않은 요청서 Optional (Optional containing the non-deleted request, or empty)
     */
    Optional<DevRequest> findByIdAndDeletedFalse(Long id);

    /**
     * 동적 조건을 조합하여 요청서 목록을 페이지 단위로 조회한다
     * (Search requests with dynamic filter conditions, returning a paginated result).
     *
     * <p>모든 파라미터는 optional이다 — {@code null} 또는 빈 문자열이면 해당 조건이 쿼리에서 제외된다.
     * 조건들은 AND로 연결된다. {@link Specs#notDeleted()} 조건은 항상 기본으로 포함된다.
     *
     * @param keyword  제목 대소문자 무시 부분 일치 검색어 (case-insensitive title keyword; null/blank to skip)
     * @param status   상태 정확 일치 필터 (exact status filter; null to skip)
     * @param category categoryPath 포함 검색어 (substring match on categoryPath; null/blank to skip)
     * @param author   작성자 정확 일치 필터 (exact author match; null/blank to skip)
     * @param pageable 페이지·정렬 정보 (pagination and sort configuration)
     * @return 조건에 맞는 요청서 페이지 (page of matching DevRequest entities)
     */
    default Page<DevRequest> search(
            String keyword,
            DevRequestStatus status,
            String category,
            String author,
            Pageable pageable
    ) {
        // 소프트 삭제 조건을 기본으로 두고 나머지 조건을 AND로 추가한다
        // (start with the mandatory soft-delete filter, then chain optional filters)
        Specification<DevRequest> spec = Specs.notDeleted()
                .and(Specs.titleLike(keyword))
                .and(Specs.statusEq(status))
                .and(Specs.categoryStartsWith(category))
                .and(Specs.authorEq(author));
        return findAll(spec, pageable);
    }

    /**
     * {@link DevRequest} 조회를 위한 JPA Specification 모음 (Collection of JPA Specifications for DevRequest queries).
     *
     * <p>Repository 인터페이스와 같은 파일 안에 중첩 클래스로 배치하여
     * 임포트 경로를 하나로 줄이고 응집도를 높인다.
     *
     * <p>각 메서드는 조건이 없을 경우 {@code null}을 반환하며,
     * JPA Specification의 {@code and(null)} 동작에 의해 해당 조건은 무시된다.
     */
    final class Specs {

        /** 유틸리티 클래스 — 인스턴스화 방지 (utility class; prevent instantiation) */
        private Specs() {}

        /**
         * 소프트 삭제되지 않은 레코드만 조회하는 조건을 반환한다
         * (Returns a Specification that filters out soft-deleted records).
         *
         * <p>모든 사용자 대상 쿼리에 반드시 포함해야 한다.
         * {@code WHERE deleted = false} 절을 생성한다.
         *
         * @return {@code deleted = false} 조건 Specification
         */
        public static Specification<DevRequest> notDeleted() {
            return (root, q, cb) -> cb.isFalse(root.get("deleted"));
        }

        /**
         * 제목 대소문자 무시 부분 일치 조건을 반환한다
         * (Returns a case-insensitive LIKE condition on the title field).
         *
         * <p>키워드 양쪽에 {@code %} 와일드카드를 붙여 {@code WHERE LOWER(title) LIKE %keyword%}를 생성한다.
         * {@code keyword}가 null이거나 빈 문자열이면 {@code null}을 반환하여 조건에서 제외된다.
         *
         * @param keyword 검색 키워드 (search keyword; null/blank means no filter)
         * @return LIKE 조건 Specification, 또는 키워드 없으면 {@code null}
         */
        public static Specification<DevRequest> titleLike(String keyword) {
            // 키워드가 없으면 null을 반환해 Specification 체인에서 자동으로 무시되게 한다
            // (return null so the caller's .and(null) becomes a no-op)
            if (!StringUtils.hasText(keyword)) return null;
            // 양쪽 % 와일드카드 + 소문자 변환으로 대소문자 무관 부분 일치 수행
            // (surround with % wildcards and lowercase for case-insensitive partial match)
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return (root, q, cb) -> cb.like(cb.lower(root.get("title")), like);
        }

        /**
         * 상태 정확 일치 조건을 반환한다 (Returns an exact-match condition on the status field).
         *
         * <p>{@code status}가 {@code null}이면 {@code null}을 반환하여 조건에서 제외된다.
         *
         * @param status 필터링할 상태 값 (target status; null means no filter)
         * @return 상태 일치 조건 Specification, 또는 상태 없으면 {@code null}
         */
        public static Specification<DevRequest> statusEq(DevRequestStatus status) {
            if (status == null) return null;
            return (root, q, cb) -> cb.equal(root.get("status"), status);
        }

        /**
         * categoryPath 포함 검색 조건을 반환한다
         * (Returns a case-insensitive substring match condition on the categoryPath field).
         *
         * <p>categoryPath 값은 {@code "결제창 / 카드"} 형태의 슬래시 연결 문자열이므로
         * 시작 일치(startsWith)가 아닌 포함 일치(contains)를 사용하여
         * 중간 경로 단어로도 검색이 가능하도록 한다.
         *
         * @param category 카테고리 검색어 (category keyword; null/blank means no filter)
         * @return categoryPath LIKE 조건 Specification, 또는 검색어 없으면 {@code null}
         */
        public static Specification<DevRequest> categoryStartsWith(String category) {
            if (!StringUtils.hasText(category)) return null;
            // categoryPath가 "결제창 / 카드" 형태라 시작·포함 둘 다 매칭되게 contains 방식 사용
            // (path is slash-separated so use contains rather than startsWith for mid-path matching)
            String like = "%" + category.trim().toLowerCase() + "%";
            return (root, q, cb) -> cb.like(cb.lower(root.get("categoryPath")), like);
        }

        /**
         * 작성자 이름 정확 일치 조건을 반환한다 (Returns an exact-match condition on the author field).
         *
         * <p>앞뒤 공백을 trim하여 비교한다.
         * {@code author}가 null이거나 빈 문자열이면 {@code null}을 반환하여 조건에서 제외된다.
         *
         * @param author 필터링할 작성자 이름 (target author name; null/blank means no filter)
         * @return 작성자 일치 조건 Specification, 또는 작성자 없으면 {@code null}
         */
        public static Specification<DevRequest> authorEq(String author) {
            if (!StringUtils.hasText(author)) return null;
            // trim()으로 앞뒤 공백 제거 후 정확 일치 비교 (trim to avoid accidental whitespace mismatch)
            return (root, q, cb) -> cb.equal(root.get("author"), author.trim());
        }
    }
}
