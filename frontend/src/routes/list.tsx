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

const PAGE_SIZE = 20;

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

      {/* 페이지네이션 */}
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

// 라우터의 navigate 호출을 한곳에서 — 어떤 trigger(클릭/Link/키보드)든 같은 경로로 떨어지게.
function goDetail(navigate: ReturnType<typeof useNavigate>, id: number) {
  void navigate({ to: "/result/$id", params: { id: String(id) } });
}

// 한 행 — <tr>이 onClick을 못 잡는 환경(브라우저 캐시·구조 이슈 등)에 대비해
// 제목 셀에 명시적 <Link>도 함께 박아둔다. 어디를 클릭해도 이동.
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
