import { useQuery } from "@tanstack/react-query";

/**
 * 지식저장소 문서의 목록 항목을 나타내는 DTO 타입.
 *
 * 백엔드 `KnowledgeSummary` DTO와 1:1로 대응하며,
 * 목록 테이블에 표시할 메타데이터 정보만 포함한다.
 */
export type KnowledgeSummary = {
  /** 문서 고유 식별자 (파일 경로 기반 ID 또는 UUID). */
  id: string;
  /** 문서 제목 (frontmatter의 `title` 값). */
  title: string;
  /** 문서가 속한 카테고리 (frontmatter의 `category` 값). */
  category?: string;
  /** 문서 버전 문자열 (frontmatter의 `version` 값). */
  version?: string;
  /** 문서 최종 수정 일시 문자열. */
  lastUpdated?: string;
  /** 문서 상태 (예: "활성", "폐기"). */
  status?: string;
  /** 원본 파일 크기 표시 문자열 (예: "12 KB"). */
  fileSize?: string;
  /** 벡터 DB에 분할 저장된 청크 수. */
  chunkCount?: number;
  /** 원본 파일명. */
  filename?: string;
};

/**
 * 지식저장소 문서 상세를 나타내는 DTO 타입.
 *
 * {@link KnowledgeSummary}를 확장하며 frontmatter 전체와 본문 마크다운을 추가로 포함한다.
 * 백엔드 `KnowledgeDoc` DTO와 1:1로 대응한다.
 */
export type KnowledgeDoc = KnowledgeSummary & {
  /**
   * 문서 frontmatter 전체를 파싱한 키-값 맵.
   * 구조가 문서마다 다를 수 있으므로 `Record<string, unknown>` 타입을 사용한다.
   */
  meta?: Record<string, unknown>;
  /**
   * 문서 본문 마크다운 문자열.
   * 상세 화면에서 `react-markdown`으로 렌더링된다.
   */
  markdown?: string;
};

/**
 * 지식저장소 카테고리 목록 상수.
 *
 * 백엔드에서 문서 frontmatter의 `category` 필드와 정확히 일치하는 값을 사용한다.
 * 폴더-카테고리 매핑:
 * - `provider` 폴더 → 원천사 규격
 * - `ui` 폴더       → 결제창
 * - `webapi` 폴더   → WEB API
 *
 * `ALL` 코드는 필터 없음(전체 조회)을 의미하는 프론트엔드 전용 값이다.
 */
export const KNOWLEDGE_CATEGORIES = [
  { code: "ALL", label: "전체" },
  { code: "원천사 규격", label: "원천사 규격" },
  { code: "결제창", label: "결제창" },
  { code: "WEB API", label: "WEB API" },
] as const;

/**
 * 지식저장소 문서 목록 전체를 조회하는 React Query 훅.
 *
 * `staleTime`을 60초로 설정하여 빈번한 재조회를 방지한다.
 * 반환된 목록은 프론트엔드에서 카테고리·키워드로 클라이언트 사이드 필터링하여 사용한다.
 *
 * @returns `useQuery` 결과 객체. `data`는 {@link KnowledgeSummary} 배열 타입.
 *
 * @example
 * ```tsx
 * const { data: docs, isLoading } = useKnowledgeList();
 * ```
 */
export function useKnowledgeList() {
  return useQuery<KnowledgeSummary[]>({
    queryKey: ["knowledge", "list"],
    queryFn: async () => {
      const res = await fetch("/api/knowledge", { headers: { Accept: "application/json" } });
      if (!res.ok) throw new Error(`지식저장소 목록 조회 실패 ${res.status}`);
      return (await res.json()) as KnowledgeSummary[];
    },
    // 1분 동안 동일한 데이터로 유지 (불필요한 재조회 방지)
    staleTime: 60 * 1000,
  });
}

/**
 * 특정 지식저장소 문서의 상세 내용을 조회하는 React Query 훅.
 *
 * `id`가 `undefined`이거나 빈 문자열이면 쿼리가 비활성화되어 API를 호출하지 않는다.
 * `staleTime`을 5분으로 설정하여 문서 본문의 반복 재조회를 줄인다.
 *
 * @param id - 조회할 문서의 고유 식별자. `undefined`이면 쿼리 비활성화.
 * @returns `useQuery` 결과 객체. `data`는 {@link KnowledgeDoc} 타입.
 *
 * @example
 * ```tsx
 * const { data: doc } = useKnowledgeDoc(selectedDocId);
 * ```
 */
export function useKnowledgeDoc(id: string | undefined) {
  return useQuery<KnowledgeDoc>({
    queryKey: ["knowledge", "doc", id],
    // id가 falsy이면 쿼리 실행 안 함
    enabled: !!id,
    queryFn: async () => {
      // id에 슬래시 등 특수문자가 포함될 수 있으므로 encodeURIComponent 적용
      const res = await fetch(`/api/knowledge/${encodeURIComponent(id!)}`, {
        headers: { Accept: "application/json" },
      });
      if (!res.ok) throw new Error(`지식문서 조회 실패 ${res.status}`);
      return (await res.json()) as KnowledgeDoc;
    },
    // 5분 동안 캐시 유지 (문서 본문은 자주 바뀌지 않음)
    staleTime: 5 * 60 * 1000,
  });
}
