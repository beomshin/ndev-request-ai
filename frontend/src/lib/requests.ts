import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

// 백엔드 /api/requests CRUD + React Query 훅.
// status enum은 백엔드 DevRequestStatus와 1:1.

/**
 * 개발 요청서의 처리 상태를 나타내는 열거형 타입.
 *
 * - `DRAFT`: 사용자가 작성 중인 임시 저장 상태.
 * - `AI_ANALYZED`: AI가 요청서를 분석하여 결과를 생성한 완료 상태.
 */
export type DevRequestStatus = "DRAFT" | "AI_ANALYZED";

/**
 * {@link DevRequestStatus} 값에 대응하는 한국어 화면 표시 레이블 매핑.
 *
 * @example
 * ```ts
 * STATUS_LABEL["DRAFT"]       // "작성중"
 * STATUS_LABEL["AI_ANALYZED"] // "AI분석완료"
 * ```
 */
export const STATUS_LABEL: Record<DevRequestStatus, string> = {
  DRAFT: "작성중",
  AI_ANALYZED: "AI분석완료",
};

/**
 * 개발 요청서 목록 조회 시 반환되는 요약 DTO.
 *
 * 목록 테이블에 표시할 최소 정보만 포함하며, 본문(마크다운·다이어그램)은 포함하지 않는다.
 */
export type DevRequestSummary = {
  /** 요청서 고유 식별자 (PK). */
  id: number;
  /** 요청서 제목. */
  title: string;
  /** 카테고리 경로 (예: "결제창 > 신규 연동"). 없을 수 있음. */
  categoryPath?: string;
  /** 현재 처리 상태 코드. */
  status: DevRequestStatus;
  /** 처리 상태 한국어 레이블 (백엔드에서 변환하여 내려줌). */
  statusLabel: string;
  /** 요청서 작성자 이름. */
  author?: string;
  /** 작성자 부서명. */
  dept?: string;
  /** 생성 일시 (ISO 8601 문자열). */
  createdAt: string;
  /** 최종 수정 일시 (ISO 8601 문자열). */
  updatedAt: string;
};

/**
 * 개발 요청서 상세 조회 시 반환되는 DTO.
 *
 * {@link DevRequestSummary}를 확장하며, 위저드 입력값·마크다운·다이어그램 등
 * 상세 페이지에 필요한 필드를 추가로 포함한다.
 */
export type DevRequestDetail = DevRequestSummary & {
  /**
   * 위저드 단계별 입력값과 AI 분석 결과(`ProjectMdResult`)를 직렬화한 JSON 문자열.
   * 파싱 후 위저드 폼 복원이나 AI 재분석 요청에 사용된다.
   */
  details?: string;
  /**
   * 표준 양식 v1.6 포맷의 마크다운 문자열.
   * 결과 화면에서 `react-markdown`으로 렌더링된다.
   */
  combinedMarkdown?: string;
  /**
   * 업무 흐름 다이어그램 소스.
   * mxGraph XML 또는 PlantUML 소스 문자열로 저장된다.
   */
  flowDiagram?: string;
  /**
   * AI가 추가 확인이 필요하다고 판단한 항목.
   * JSON 배열 또는 마크다운 텍스트로 저장된다.
   */
  unconfirmedSection?: string;
};

/**
 * 개발 요청서 저장(POST) 요청 시 전송하는 페이로드 타입.
 *
 * 신규 생성과 수정 모두 이 타입을 사용한다.
 * `id`는 URL 경로로 전달하므로 포함하지 않는다.
 */
export type DevRequestSaveRequest = {
  /** 요청서 제목. */
  title: string;
  /** 카테고리 경로. */
  categoryPath?: string;
  /** 저장할 상태 코드. 생략 시 백엔드 기본값(DRAFT) 적용. */
  status?: DevRequestStatus;
  /** 작성자 이름. */
  author?: string;
  /** 작성자 부서명. */
  dept?: string;
  /** 위저드 입력값 + AI 결과를 직렬화한 JSON 문자열. */
  details?: string;
  /** 표준 양식 v1.6 마크다운 문자열. */
  combinedMarkdown?: string;
  /** 흐름 다이어그램 소스 문자열. */
  flowDiagram?: string;
  /** 추가 확인 항목 문자열. */
  unconfirmedSection?: string;
};

/**
 * Spring Data 페이지네이션 응답(`Page<T>`)에서 필요한 필드만 추출한 제네릭 타입.
 *
 * @template T - 페이지 내 각 항목의 타입.
 */
export type PageResponse<T> = {
  /** 현재 페이지의 데이터 배열. */
  content: T[];
  /** 전체 항목 수. */
  totalElements: number;
  /** 전체 페이지 수. */
  totalPages: number;
  /** 현재 페이지 번호 (0-based). */
  number: number;
  /** 페이지당 항목 수. */
  size: number;
  /** 첫 번째 페이지 여부. */
  first: boolean;
  /** 마지막 페이지 여부. */
  last: boolean;
};

/**
 * 요청서 목록 조회 시 사용하는 필터·페이지네이션 파라미터 타입.
 */
export type ListParams = {
  /** 제목 또는 내용 키워드 검색어. */
  keyword?: string;
  /** 상태 코드 필터. */
  status?: DevRequestStatus;
  /** 카테고리 경로 필터. */
  category?: string;
  /** 작성자 이름 필터. */
  author?: string;
  /** 요청 페이지 번호 (0-based). */
  page?: number;
  /** 페이지당 항목 수. */
  size?: number;
  /**
   * 정렬 기준 문자열. Spring Pageable 규격을 따른다.
   * @example "createdAt,desc"
   */
  sort?: string;
};

/** 요청서 API 기본 경로. */
const BASE = "/api/requests";

/**
 * {@link ListParams} 객체를 URL 쿼리 문자열로 변환한다.
 *
 * `undefined` 또는 `null`인 파라미터는 쿼리 문자열에 포함하지 않는다.
 *
 * @param p - 변환할 목록 조회 파라미터 객체.
 * @returns `key=value&...` 형식의 URL 쿼리 문자열. 파라미터가 없으면 빈 문자열 반환.
 */
function buildQs(p: ListParams): string {
  const qs = new URLSearchParams();
  if (p.keyword) qs.set("keyword", p.keyword);
  if (p.status) qs.set("status", p.status);
  if (p.category) qs.set("category", p.category);
  if (p.author) qs.set("author", p.author);
  // 숫자 0도 유효한 값이므로 != null 으로 비교
  if (p.page != null) qs.set("page", String(p.page));
  if (p.size != null) qs.set("size", String(p.size));
  if (p.sort) qs.set("sort", p.sort);
  return qs.toString();
}

/**
 * `fetch` 응답이 성공(`res.ok`)이면 JSON을 파싱하여 반환하고,
 * 실패이면 상태 코드와 응답 본문을 포함한 에러를 던진다.
 *
 * JSON 파싱에 실패할 경우 텍스트 그대로를 에러 메시지에 포함한다.
 *
 * @template T - 파싱된 JSON의 예상 타입.
 * @param res - `fetch`로 받은 `Response` 객체.
 * @returns 파싱된 JSON 값 (타입 `T`).
 * @throws 응답 상태가 실패(`!res.ok`)일 때 상태 코드와 상세 메시지를 담은 `Error`.
 */
async function jsonOrThrow<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let detail = "";
    try {
      // JSON 파싱을 먼저 시도하고, 실패 시 텍스트로 fallback
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

/**
 * 개발 요청서 목록을 페이지네이션으로 조회하는 React Query 훅.
 *
 * `params`가 변경될 때마다 자동으로 재조회된다.
 * `placeholderData`를 이전 데이터로 유지하여 페이지 이동 시 화면 깜빡임을 방지한다.
 *
 * @param params - 키워드·상태·카테고리·작성자 필터 및 페이지네이션 옵션.
 * @returns `useQuery` 결과 객체. `data`는 {@link PageResponse}`<`{@link DevRequestSummary}`>` 타입.
 *
 * @example
 * ```tsx
 * const { data, isLoading } = useRequestsList({ page: 0, size: 10 });
 * ```
 */
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

/**
 * 특정 개발 요청서의 상세 정보를 조회하는 React Query 훅.
 *
 * `id`가 `undefined`이면 쿼리가 비활성화(`enabled: false`)되어 API를 호출하지 않는다.
 *
 * @param id - 조회할 요청서의 고유 식별자. 미확정 상태일 경우 `undefined` 전달 가능.
 * @returns `useQuery` 결과 객체. `data`는 {@link DevRequestDetail} 타입.
 *
 * @example
 * ```tsx
 * const { data: detail } = useRequestDetail(requestId);
 * ```
 */
export function useRequestDetail(id: number | undefined) {
  return useQuery<DevRequestDetail>({
    queryKey: ["requests", "detail", id],
    queryFn: async () => {
      const res = await fetch(`${BASE}/${id}`, {
        headers: { Accept: "application/json" },
      });
      return jsonOrThrow<DevRequestDetail>(res);
    },
    // id가 null/undefined이면 쿼리 실행 안 함
    enabled: id != null,
  });
}

/**
 * 개발 요청서를 신규 저장(POST)하는 React Query Mutation 훅.
 *
 * 저장 성공 시 목록 쿼리 캐시를 자동으로 무효화하여 최신 데이터가 반영되도록 한다.
 *
 * @returns `useMutation` 결과 객체.
 *   - `mutateAsync(req)`: {@link DevRequestSaveRequest}를 전송하고 저장된 {@link DevRequestDetail}을 반환.
 *
 * @example
 * ```tsx
 * const { mutateAsync: save, isPending } = useSaveRequest();
 * const saved = await save({ title: "신규 요청", status: "DRAFT" });
 * ```
 */
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

/**
 * 개발 요청서를 삭제(DELETE)하는 React Query Mutation 훅.
 *
 * 삭제 성공 시 목록 쿼리 캐시를 무효화하고, 해당 상세 쿼리 캐시를 제거한다.
 * HTTP 204 No Content 응답은 정상 삭제로 처리한다.
 *
 * @returns `useMutation` 결과 객체.
 *   - `mutateAsync(id)`: 삭제할 요청서의 `id`를 전달.
 *
 * @example
 * ```tsx
 * const { mutateAsync: remove } = useDeleteRequest();
 * await remove(requestId);
 * ```
 */
export function useDeleteRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      const res = await fetch(`${BASE}/${id}`, { method: "DELETE" });
      // 204 No Content는 성공이므로 예외 처리에서 제외
      if (!res.ok && res.status !== 204) {
        throw new Error(`삭제 실패 ${res.status}`);
      }
    },
    onSuccess: (_, id) => {
      // 목록 캐시 무효화 후 해당 상세 캐시도 제거
      void qc.invalidateQueries({ queryKey: ["requests", "list"] });
      qc.removeQueries({ queryKey: ["requests", "detail", id] });
    },
  });
}
