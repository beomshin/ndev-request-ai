import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import {
  KNOWLEDGE_CATEGORIES,
  useKnowledgeList,
  type KnowledgeSummary,
} from "@/lib/knowledge";

export const Route = createFileRoute("/knowledge")({
  head: () => ({
    meta: [
      { title: "지식 저장소 · Req-Genie" },
      { name: "description", content: "AI가 요청서 검증·생성에 참조하는 결제 도메인 문서(원천사 규격·결제창·WEB API)" },
    ],
  }),
  component: Knowledge,
});

function Knowledge() {
  const { data, isLoading, isError, error } = useKnowledgeList();
  const [activeCategory, setActiveCategory] = useState<string>("ALL");
  const [keyword, setKeyword] = useState("");

  const docs = data ?? [];

  // 가장 최근에 업데이트된 문서의 last_updated (frontmatter엔 uploaded_at이 없어 last_updated로 대체)
  const latestUpdated = useMemo(() => {
    return docs
      .map((d) => d.lastUpdated)
      .filter((v): v is string => !!v && v.trim().length > 0)
      .sort()
      .at(-1);
  }, [docs]);

  // 카테고리별 카운트 (사이드 메뉴)
  const counts = useMemo(() => {
    const m: Record<string, number> = { ALL: docs.length };
    for (const d of docs) {
      if (!d.category) continue;
      m[d.category] = (m[d.category] ?? 0) + 1;
    }
    return m;
  }, [docs]);

  // 필터링 (카테고리 + 키워드)
  const filtered = useMemo(() => {
    let arr = docs;
    if (activeCategory !== "ALL") {
      arr = arr.filter((d) => d.category === activeCategory);
    }
    const kw = keyword.trim().toLowerCase();
    if (kw) {
      arr = arr.filter(
        (d) =>
          d.title?.toLowerCase().includes(kw) ||
          d.id?.toLowerCase().includes(kw) ||
          d.filename?.toLowerCase().includes(kw),
      );
    }
    return arr;
  }, [docs, activeCategory, keyword]);

  return (
    <div className="px-10 py-8 max-w-[1280px] mx-auto">
      <div className="flex items-start justify-between mb-8">
        <div>
          <div className="text-xs font-medium tracking-wider text-muted-foreground uppercase mb-2">
            Knowledge Base · 결제 도메인
          </div>
          <h1 className="text-3xl font-semibold text-foreground tracking-tight">지식 저장소</h1>
          <p className="text-sm text-muted-foreground mt-2">
            AI가 요청서 검증·생성에 참조하는 도메인 문서를 모았습니다. 카테고리는{" "}
            <b className="text-foreground">원천사 규격 / 결제창 / WEB API</b> 세 가지로 나뉩니다.
            {latestUpdated && (
              <> · 최종 업데이트: <b className="text-foreground">{latestUpdated}</b></>
            )}
          </p>
        </div>
      </div>

      {/* 통계 4종 (실 데이터 기반) */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        <Stat l="등록 문서" v={String(docs.length)} />
        <Stat l="총 청크" v={(docs.reduce((s, d) => s + (d.chunkCount ?? 0), 0)).toLocaleString()} />
        <Stat l="원천사 규격" v={String(counts["원천사 규격"] ?? 0)} />
        <Stat l="WEB API" v={String(counts["WEB API"] ?? 0)} />
      </div>

      <div className="grid grid-cols-4 gap-6">
        <aside className="space-y-1">
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 px-2">
            카테고리
          </h3>
          {KNOWLEDGE_CATEGORIES.map((c) => {
            const active = activeCategory === c.code;
            const cnt = counts[c.code] ?? 0;
            return (
              <button
                key={c.code}
                onClick={() => setActiveCategory(c.code)}
                className={`w-full flex items-center justify-between px-3 py-2 rounded-md text-sm transition ${
                  active
                    ? "bg-secondary text-foreground font-medium"
                    : "text-muted-foreground hover:bg-secondary/60 hover:text-foreground"
                }`}
              >
                <span>{c.label}</span>
                <span className="text-[11px] font-mono text-muted-foreground">{cnt}</span>
              </button>
            );
          })}
        </aside>

        <div className="col-span-3 rounded-xl border border-border bg-card overflow-hidden">
          <div className="flex items-center gap-3 px-5 py-3 border-b border-border">
            <div className="flex-1 flex items-center gap-2 rounded-md border border-border bg-secondary/40 px-3 py-1.5">
              <span className="text-muted-foreground text-xs">⌕</span>
              <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
                placeholder="문서명 / 키워드 검색"
              />
            </div>
            <span className="text-xs text-muted-foreground">{filtered.length}건</span>
          </div>

          {isLoading && (
            <div className="px-5 py-10 text-center text-sm text-muted-foreground">불러오는 중…</div>
          )}
          {isError && (
            <div className="px-5 py-10 text-center text-sm text-destructive">
              조회 실패: {error instanceof Error ? error.message : String(error)}
            </div>
          )}
          {!isLoading && !isError && filtered.length === 0 && (
            <div className="px-5 py-10 text-center text-sm text-muted-foreground">
              조건에 맞는 문서가 없습니다.
            </div>
          )}

          {filtered.length > 0 && (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-[11px] text-muted-foreground border-b border-border uppercase tracking-wider">
                  <th className="text-left font-medium px-5 py-3">문서명</th>
                  <th className="text-left font-medium py-3">카테고리</th>
                  <th className="text-left font-medium py-3">버전</th>
                  <th className="text-left font-medium py-3">최종 업데이트</th>
                  <th className="text-left font-medium py-3">청크</th>
                  <th className="text-left font-medium py-3">상태</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((d) => (
                  <DocRow key={d.id} d={d} />
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function DocRow({ d }: { d: KnowledgeSummary }) {
  return (
    <tr className="border-b border-border last:border-0 hover:bg-secondary/30">
      <td className="px-5 py-3.5">
        <Link
          to="/knowledge/$id"
          params={{ id: d.id }}
          className="flex items-center gap-3 group"
        >
          <div className="h-8 w-8 rounded bg-primary/10 text-primary flex items-center justify-center text-[10px] font-bold">
            MD
          </div>
          <div>
            <div className="font-medium text-foreground group-hover:underline">{d.title}</div>
            <div className="text-[11px] text-muted-foreground">
              {d.fileSize ?? "—"} · {d.filename}
            </div>
          </div>
        </Link>
      </td>
      <td className="py-3.5 text-xs text-foreground">{d.category ?? "—"}</td>
      <td className="py-3.5 text-xs font-mono text-muted-foreground">{d.version ?? "—"}</td>
      <td className="py-3.5 text-xs text-foreground">{d.lastUpdated ?? "—"}</td>
      <td className="py-3.5 text-xs font-mono text-muted-foreground">
        {d.chunkCount?.toLocaleString() ?? "—"}
      </td>
      <td className="py-3.5">
        <StatusBadge status={d.status} />
      </td>
    </tr>
  );
}

function StatusBadge({ status }: { status?: string }) {
  if (!status) return <span className="text-xs text-muted-foreground">—</span>;
  const good = status === "PUBLISHED" || status === "동기화됨";
  const cls = good
    ? "text-[color:var(--success)] bg-[color:var(--success)]/10"
    : "text-[color:var(--warning)] bg-[color:var(--warning)]/10";
  return (
    <span className={`inline-flex items-center gap-1.5 text-[11px] font-medium px-2 py-0.5 rounded ${cls}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${good ? "bg-[color:var(--success)]" : "bg-[color:var(--warning)]"}`} />
      {status}
    </span>
  );
}

function Stat({ l, v }: { l: string; v: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="text-xs text-muted-foreground">{l}</div>
      <div className="mt-2 text-2xl font-semibold text-foreground">{v}</div>
    </div>
  );
}
