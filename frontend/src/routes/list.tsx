import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  STATUS_LABEL,
  useRequestsList,
  type DevRequestStatus,
  type DevRequestSummary,
} from "@/lib/requests";

// /list — 저장된 요청서 목록 (DB 기반). 검색·status 필터·페이징.
// 행 클릭 → /result/$id 상세 (영구 저장본).
// 사이드바엔 노출 안 되지만 URL 직접 입력으로 진입 가능.

/**
 * `/list` 라우트 정의.
 * 저장된 개발요청서 목록 페이지의 메타 정보를 선언하고 `ResultList` 컴포넌트를 등록한다.
 */
export const Route = createFileRoute("/list")({
  head: () => ({
    meta: [
      { title: "저장된 요청서 목록 · Req-Genie" },
      {
        name: "description",
        content: "저장된 개발요청서 목록을 검색·정렬·확인합니다.",
      },
    ],
  }),
  component: ResultList,
});

/** 한 페이지에 표시할 요청서 항목 수. */
const PAGE_SIZE = 20;

/**
 * 요청서 목록 페이지 컴포넌트.
 * 제목 키워드 검색, 상태 필터, 페이지네이션을 지원한다.
 * 키워드는 Enter 입력 또는 검색 버튼 클릭 시에만 서버로 전송한다(불필요한 fetch 방지).
 * 각 행을 클릭하면 `/result/$id` 상세 페이지로 이동한다.
 */
function ResultList() {
  const navigate = useNavigate();

  // 입력 중인 값과 적용된(=서버로 보낸) 값 분리 — Enter/버튼 클릭 시에만 fetch
  const [keywordInput, setKeywordInput] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [status, setStatus] = useState<DevRequestStatus | "">("");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, isFetching } = useRequestsList({
    keyword: appliedKeyword || undefined,
    status: status || undefined,
    page,
    size: PAGE_SIZE,
    sort: "createdAt,desc",
  });

  /**
   * 검색 실행 핸들러.
   * 현재 입력값을 trim하여 적용 키워드로 반영하고, 페이지를 0으로 리셋한다.
   */
  const onSearch = () => {
    setAppliedKeyword(keywordInput.trim());
    setPage(0);
  };

  return (
    <div className="px-6 sm:px-10 py-8 max-w-[1180px] mx-auto">
      {/* 헤더 */}
      <div className="flex items-start justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-foreground tracking-tight">
            최근 생성 문서
          </h1>
          <p className="text-sm text-muted-foreground mt-1.5">
            저장된 개발요청서를 검색·확인합니다. 항목을 클릭하면 상세 화면으로 이동합니다.
          </p>
        </div>
        <Link to="/new">
          <Button>＋ 새 요청서 작성</Button>
        </Link>
      </div>

      {/* 검색바 */}
      <div className="rounded-xl border border-border bg-card p-4 mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-[1fr_200px_auto] gap-3 items-end">
          <div>
            <Label htmlFor="keyword" className="text-xs">
              제목 키워드
            </Label>
            <Input
              id="keyword"
              value={keywordInput}
              onChange={(e) => setKeywordInput(e.target.value)}
              onKeyDown={(e) => {
                // Enter 키 입력 시 즉시 검색 실행
                if (e.key === "Enter") onSearch();
              }}
              placeholder="예: 카카오페이"
            />
          </div>
          <div>
            <Label className="text-xs">상태</Label>
            <select
              value={status}
              onChange={(e) => {
                // 상태 변경 즉시 검색 (별도 검색 버튼 불필요)
                setStatus(e.target.value as DevRequestStatus | "");
                setPage(0);
              }}
              className="mt-1 w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
            >
              <option value="">전체</option>
              <option value="DRAFT">작성중</option>
              <option value="AI_ANALYZED">AI분석완료</option>
            </select>
          </div>
          <Button onClick={onSearch} disabled={isFetching}>
            검색
          </Button>
        </div>
      </div>

      {/* 결과 */}
      <div className="rounded-xl border border-border bg-card overflow-hidden">
        {isLoading && (
          <div className="p-10 text-center text-sm text-muted-foreground">
            불러오는 중…
          </div>
        )}
        {isError && (
          <div className="p-6 text-sm text-destructive">
            목록 조회 실패: {error instanceof Error ? error.message : String(error)}
          </div>
        )}
        {!isLoading && !isError && (data?.content.length ?? 0) === 0 && (
          <div className="p-10 text-center text-sm text-muted-foreground">
            결과가 없습니다.{" "}
            <Link to="/new" className="text-primary hover:underline">
              새 요청서 작성하기
            </Link>
          </div>
        )}
        {data && data.content.length > 0 && (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-muted-foreground border-b border-border">
                <th className="text-left font-medium px-4 py-3 w-16">#</th>
                <th className="text-left font-medium py-3">제목 / 분류</th>
                <th className="text-left font-medium py-3 w-32">상태</th>
                <th className="text-left font-medium py-3 w-48">작성자</th>
                <th className="text-right font-medium px-4 py-3 w-36">생성일</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((r) => (
                <RowItem key={r.id} item={r} onPick={(id) => goDetail(navigate, id)} />
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* 페이지네이션 — 총 페이지가 2 이상일 때만 표시 */}
      {data && data.totalPages > 1 && (
        <Pagination
          page={data.number}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          isFetching={isFetching}
          onChange={setPage}
        />
      )}
    </div>
  );
}

/**
 * 요청서 상세 페이지로 이동하는 헬퍼 함수.
 * 어떤 트리거(행 클릭, 링크, 키보드)든 동일한 경로로 네비게이션하도록 단일 지점에서 처리한다.
 *
 * @param navigate - TanStack Router의 `useNavigate` 훅이 반환하는 navigate 함수
 * @param id - 이동할 요청서의 DB ID
 */
function goDetail(navigate: ReturnType<typeof useNavigate>, id: number) {
  void navigate({ to: "/result/$id", params: { id: String(id) } });
}

/**
 * 요청서 목록의 개별 행 컴포넌트.
 * `<tr>` 전체에 `onClick`을 걸고, 제목 셀에도 `<Link>`를 추가하여
 * 어느 영역을 클릭해도 상세 페이지로 이동한다.
 * 키보드 사용자를 위해 `tabIndex`와 `onKeyDown` 이벤트도 처리한다.
 *
 * @param item - 목록에 표시할 요청서 요약 데이터
 * @param onPick - 행 선택 시 호출되는 콜백 (id를 인자로 받음)
 */
function RowItem({
  item,
  onPick,
}: {
  item: DevRequestSummary;
  onPick: (id: number) => void;
}) {
  return (
    <tr
      role="button"
      tabIndex={0}
      onClick={() => onPick(item.id)}
      onKeyDown={(e) => {
        // Enter / Space 키로도 행 선택이 가능하도록 처리 (접근성)
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onPick(item.id);
        }
      }}
      className="border-b border-border last:border-0 hover:bg-secondary/40 cursor-pointer focus:bg-secondary/40 focus:outline-none"
    >
      <td className="px-4 py-3 font-mono text-[11px] text-muted-foreground">{item.id}</td>
      <td className="py-3">
        <Link
          to="/result/$id"
          params={{ id: String(item.id) }}
          className="font-medium text-foreground hover:text-primary"
          // 부모 <tr> onClick과 중복 트리거 방지
          onClick={(e) => e.stopPropagation()}
        >
          {item.title}
        </Link>
        <div className="text-xs text-muted-foreground mt-0.5">{item.categoryPath || "—"}</div>
      </td>
      <td className="py-3">
        <StatusBadge status={item.status} />
      </td>
      <td className="py-3 text-foreground">
        <div>{item.author || "—"}</div>
        <div className="text-xs text-muted-foreground mt-0.5">{item.dept || ""}</div>
      </td>
      <td className="px-4 py-3 text-right text-muted-foreground text-xs">
        {formatDate(item.createdAt)}
      </td>
    </tr>
  );
}

/**
 * 요청서 상태를 색상이 있는 뱃지로 표시하는 컴포넌트.
 * `AI_ANALYZED` 상태는 성공(초록) 색상으로, 그 외 상태는 중립 색상으로 표시한다.
 *
 * @param status - 표시할 요청서 상태 코드
 */
function StatusBadge({ status }: { status: DevRequestStatus }) {
  const cls =
    status === "AI_ANALYZED"
      ? "bg-[color:var(--success)]/15 text-[color:var(--success)]"
      : "bg-secondary text-muted-foreground";
  return (
    <span
      className={`inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-medium ${cls}`}
    >
      {STATUS_LABEL[status]}
    </span>
  );
}

/**
 * 목록 하단 페이지네이션 컴포넌트.
 * 현재 페이지를 중심으로 최대 5개의 페이지 버튼을 표시하며, 앞/뒤 화살표 버튼을 제공한다.
 * 데이터 fetching 중에는 모든 버튼이 비활성화된다.
 *
 * @param page - 현재 페이지 인덱스 (0-based)
 * @param totalPages - 전체 페이지 수
 * @param totalElements - 전체 항목 수 (표시용)
 * @param isFetching - 데이터 로딩 중 여부
 * @param onChange - 페이지 변경 시 호출되는 콜백
 */
function Pagination({
  page,
  totalPages,
  totalElements,
  isFetching,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  isFetching: boolean;
  onChange: (page: number) => void;
}) {
  // 현재 페이지 중심으로 앞뒤 2페이지씩, 최대 5개 버튼을 계산
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages, start + 5);
  const pages = Array.from({ length: end - start }, (_, i) => start + i);

  return (
    <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
      <div>총 {totalElements}건</div>
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="sm"
          disabled={page === 0 || isFetching}
          onClick={() => onChange(Math.max(0, page - 1))}
        >
          ←
        </Button>
        {pages.map((p) => (
          <Button
            key={p}
            size="sm"
            variant={p === page ? "default" : "outline"}
            disabled={isFetching}
            onClick={() => onChange(p)}
          >
            {p + 1}
          </Button>
        ))}
        <Button
          variant="outline"
          size="sm"
          disabled={page >= totalPages - 1 || isFetching}
          onClick={() => onChange(Math.min(totalPages - 1, page + 1))}
        >
          →
        </Button>
      </div>
    </div>
  );
}

/**
 * ISO 8601 형식의 날짜 문자열을 한국어 로케일의 날짜+시간 문자열로 변환한다.
 * 파싱에 실패하면 원본 문자열을 그대로 반환한다.
 *
 * @param iso - ISO 8601 형식의 날짜 문자열 (예: "2024-01-15T09:30:00")
 * @returns 포맷된 날짜+시간 문자열 (예: "2024. 01. 15. 오전 09:30")
 */
function formatDate(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}
