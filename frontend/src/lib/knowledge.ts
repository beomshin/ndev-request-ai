import { useQuery } from "@tanstack/react-query";

// 백엔드 KnowledgeSummary DTO와 1:1
export type KnowledgeSummary = {
  id: string;
  title: string;
  category?: string;
  version?: string;
  lastUpdated?: string;
  status?: string;
  fileSize?: string;
  chunkCount?: number;
  filename?: string;
};

// 백엔드 KnowledgeDoc DTO와 1:1
export type KnowledgeDoc = KnowledgeSummary & {
  // frontmatter 전체(JSON)
  meta?: Record<string, unknown>;
  // 본문 마크다운
  markdown?: string;
};

// 폴더-카테고리 매핑 (문서 frontmatter의 category 값과 정확히 일치)
//   provider 폴더 → 원천사 규격
//   ui 폴더       → 결제창
//   webapi 폴더   → WEB API
export const KNOWLEDGE_CATEGORIES = [
  { code: "ALL", label: "전체" },
  { code: "원천사 규격", label: "원천사 규격" },
  { code: "결제창", label: "결제창" },
  { code: "WEB API", label: "WEB API" },
] as const;

export function useKnowledgeList() {
  return useQuery<KnowledgeSummary[]>({
    queryKey: ["knowledge", "list"],
    queryFn: async () => {
      const res = await fetch("/api/knowledge", { headers: { Accept: "application/json" } });
      if (!res.ok) throw new Error(`지식저장소 목록 조회 실패 ${res.status}`);
      return (await res.json()) as KnowledgeSummary[];
    },
    staleTime: 60 * 1000,
  });
}

export function useKnowledgeDoc(id: string | undefined) {
  return useQuery<KnowledgeDoc>({
    queryKey: ["knowledge", "doc", id],
    enabled: !!id,
    queryFn: async () => {
      const res = await fetch(`/api/knowledge/${encodeURIComponent(id!)}`, {
        headers: { Accept: "application/json" },
      });
      if (!res.ok) throw new Error(`지식문서 조회 실패 ${res.status}`);
      return (await res.json()) as KnowledgeDoc;
    },
    staleTime: 5 * 60 * 1000,
  });
}
