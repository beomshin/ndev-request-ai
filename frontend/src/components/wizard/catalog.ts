/**
 * @file catalog.ts
 * @description 백엔드 카탈로그 API를 조회하는 React Query 훅 모음.
 *
 * 카탈로그는 기능 유형(funcType)과 대분류·소분류 트리를 제공하며,
 * 위저드 S1·S2 슬라이드에서 선택지를 구성하는 데 사용된다.
 */

import { useQuery } from "@tanstack/react-query";
import type { CategoryTree } from "./types";

/**
 * 카탈로그 API 엔드포인트 URL.
 *
 * @remarks
 * 백엔드는 `@GetMapping("/")` 로 매핑되어 있어 **끝 슬래시가 필수**다 (백엔드 README 참고).
 * 슬래시를 생략하면 404가 반환된다.
 */
const CATALOG_URL = "/api/catalog/";

/**
 * 백엔드에서 카탈로그 트리를 가져온다.
 *
 * @returns `CategoryTree` — 기능 유형 목록과 카테고리 트리 포함
 * @throws 응답이 `ok`가 아닐 때 에러를 던진다.
 */
async function fetchCatalog(): Promise<CategoryTree> {
  const res = await fetch(CATALOG_URL, { headers: { Accept: "application/json" } });
  if (!res.ok) throw new Error(`카탈로그 조회 실패: ${res.status}`);
  return (await res.json()) as CategoryTree;
}

/**
 * 카탈로그 트리를 조회하는 커스텀 React Query 훅.
 *
 * @returns `UseQueryResult<CategoryTree>` — 데이터·로딩·에러 상태 포함
 *
 * @remarks
 * 분기 트리는 운영 중 수시로 수정될 수 있으므로 `staleTime`을 1분으로 짧게 유지한다
 * (design.md §4 참고). 페이지 전체에서 동일 queryKey를 공유하므로 중복 요청은 발생하지 않는다.
 *
 * @example
 * const { data: catalog, isLoading } = useCatalog();
 */
export function useCatalog() {
  return useQuery({
    queryKey: ["catalog"],
    queryFn: fetchCatalog,
    // 분기 트리는 운영 중 상시 수정 가능 → 길게 캐시하지 않는다 (design.md §4 참고)
    staleTime: 60 * 1000,
  });
}
