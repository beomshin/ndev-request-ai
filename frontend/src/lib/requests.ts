import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

// 백엔드 /api/requests CRUD + React Query 훅.
// status enum은 백엔드 DevRequestStatus와 1:1.

export type DevRequestStatus = "DRAFT" | "AI_ANALYZED";

export const STATUS_LABEL: Record<DevRequestStatus, string> = {
  DRAFT: "작성중",
  AI_ANALYZED: "AI분석완료",
};

export type DevRequestSummary = {
  id: number;
  title: string;
  categoryPath?: string;
  status: DevRequestStatus;
  statusLabel: string;
  author?: string;
  dept?: string;
  createdAt: string;
  updatedAt: string;
};

export type DevRequestDetail = DevRequestSummary & {
  details?: string;             // JSON 문자열 (위저드 입력 + ProjectMdResult)
  combinedMarkdown?: string;    // 표준 양식 v1.6 MD
  flowDiagram?: string;         // mxGraph XML 또는 PlantUML 소스
  unconfirmedSection?: string;  // 추가확인 항목 (JSON 또는 MD)
};

export type DevRequestSaveRequest = {
  title: string;
  categoryPath?: string;
  status?: DevRequestStatus;
  author?: string;
  dept?: string;
  details?: string;
  combinedMarkdown?: string;
  flowDiagram?: string;
  unconfirmedSection?: string;
};

// Spring Page<T> 응답 형식 (필요 필드만)
export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;       // 현재 페이지 (0-based)
  size: number;
  first: boolean;
  last: boolean;
};

export type ListParams = {
  keyword?: string;
  status?: DevRequestStatus;
  category?: string;
  author?: string;
  page?: number;        // 0-based
  size?: number;
  sort?: string;        // "createdAt,desc"
};

const BASE = "/api/requests";

function buildQs(p: ListParams): string {
  const qs = new URLSearchParams();
  if (p.keyword) qs.set("keyword", p.keyword);
  if (p.status) qs.set("status", p.status);
  if (p.category) qs.set("category", p.category);
  if (p.author) qs.set("author", p.author);
  if (p.page != null) qs.set("page", String(p.page));
  if (p.size != null) qs.set("size", String(p.size));
  if (p.sort) qs.set("sort", p.sort);
  return qs.toString();
}

async function jsonOrThrow<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let detail = "";
    try {
      detail = JSON.stringify(await res.json());
    } catch {
      detail = await res.text();
    }
    throw new Error(`${res.status} ${detail}`);
  }
  return (await res.json()) as T;
}

// ─────────────────────────────────────────────────────────────────────
// React Query 훅
// ─────────────────────────────────────────────────────────────────────

export function useRequestsList(params: ListParams) {
  return useQuery<PageResponse<DevRequestSummary>>({
    queryKey: ["requests", "list", params],
    queryFn: async () => {
      const qs = buildQs(params);
      const res = await fetch(`${BASE}${qs ? `?${qs}` : ""}`, {
        headers: { Accept: "application/json" },
      });
      return jsonOrThrow<PageResponse<DevRequestSummary>>(res);
    },
    placeholderData: (prev) => prev,    // 페이지 이동 시 깜빡임 방지
  });
}

export function useRequestDetail(id: number | undefined) {
  return useQuery<DevRequestDetail>({
    queryKey: ["requests", "detail", id],
    queryFn: async () => {
      const res = await fetch(`${BASE}/${id}`, {
        headers: { Accept: "application/json" },
      });
      return jsonOrThrow<DevRequestDetail>(res);
    },
    enabled: id != null,
  });
}

export function useSaveRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (req: DevRequestSaveRequest): Promise<DevRequestDetail> => {
      const res = await fetch(BASE, {
        method: "POST",
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          Accept: "application/json",
        },
        body: JSON.stringify(req),
      });
      return jsonOrThrow<DevRequestDetail>(res);
    },
    onSuccess: () => {
      // 저장 후 목록 리프레시
      void qc.invalidateQueries({ queryKey: ["requests", "list"] });
    },
  });
}

export function useDeleteRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      const res = await fetch(`${BASE}/${id}`, { method: "DELETE" });
      if (!res.ok && res.status !== 204) {
        throw new Error(`삭제 실패 ${res.status}`);
      }
    },
    onSuccess: (_, id) => {
      void qc.invalidateQueries({ queryKey: ["requests", "list"] });
      qc.removeQueries({ queryKey: ["requests", "detail", id] });
    },
  });
}
