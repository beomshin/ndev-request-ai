import { useQuery } from "@tanstack/react-query";
import type { CategoryTree } from "./types";

// 백엔드는 @GetMapping("/")로 매핑되어 끝 슬래시가 필수다 (백엔드 README 참고).
// 잘못 호출 시 404가 떨어진다.
const CATALOG_URL = "/api/catalog/";

async function fetchCatalog(): Promise<CategoryTree> {
  const res = await fetch(CATALOG_URL, { headers: { Accept: "application/json" } });
  if (!res.ok) throw new Error(`카탈로그 조회 실패: ${res.status}`);
  return (await res.json()) as CategoryTree;
}

export function useCatalog() {
  return useQuery({
    queryKey: ["catalog"],
    queryFn: fetchCatalog,
    // 분기 트리는 운영 중 상시 수정 가능 → 길게 캐시하지 않는다 (design.md §4 참고)
    staleTime: 60 * 1000,
  });
}
