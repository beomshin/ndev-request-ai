import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/knowledge")({
  head: () => ({
    meta: [
      { title: "지식 저장소 · Req-Genie" },
      { name: "description", content: "AI가 학습한 결제 도메인 지식(결제창/WEB API/해외결제/SMS링크)을 관리합니다." },
    ],
  }),
  component: Knowledge,
});

const docs = [
  { name: "결제창 기능 사양서", ver: "v3.2", date: "2026-06-14", size: "8.2 MB", chunks: 1240, status: "동기화됨" },
  { name: "WEB API 규격서", ver: "v2.8", date: "2026-06-10", size: "4.7 MB", chunks: 982, status: "동기화됨" },
  { name: "해외결제 가이드", ver: "v1.9", date: "2026-05-28", size: "12.4 MB", chunks: 1568, status: "재학습 필요" },
  { name: "SMS 링크결제 규격", ver: "v2.0", date: "2026-06-17", size: "2.1 MB", chunks: 421, status: "동기화됨" },
  { name: "지불수단별 할부 매트릭스", ver: "2026 Q2", date: "2026-04-02", size: "850 KB", chunks: 184, status: "동기화됨" },
  { name: "원천사 연동 규격 (네이버페이)", ver: "v2.3", date: "2026-06-09", size: "5.6 MB", chunks: 723, status: "동기화됨" },
];

function Knowledge() {
  return (
    <div className="px-10 py-8 max-w-[1280px] mx-auto">
      <div className="flex items-start justify-between mb-8">
        <div>
          <div className="text-xs font-medium tracking-wider text-muted-foreground uppercase mb-2">Knowledge Base · 결제 도메인</div>
          <h1 className="text-3xl font-semibold text-foreground tracking-tight">지식 저장소</h1>
          <p className="text-sm text-muted-foreground mt-2">AI가 요청서 검증·생성에 참조하는 도메인 문서를 관리합니다. <b className="text-foreground">최종 업데이트: 2026-06-17 14:02</b></p>
        </div>
        <div className="flex gap-2">
          <button className="rounded-md border border-border bg-card px-3 py-2 text-xs text-foreground hover:bg-secondary">↻ 전체 재학습</button>
          <button className="rounded-md bg-primary text-primary-foreground px-4 py-2.5 text-sm font-medium shadow-[var(--shadow-elegant)]">＋ 문서 업로드</button>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-8">
        {[
          { l: "등록 문서", v: "24" },
          { l: "총 청크", v: "11,438" },
          { l: "임베딩 크기", v: "84.2 MB" },
          { l: "재학습 필요", v: "1", tone: "text-[color:var(--warning)]" },
        ].map((s) => (
          <div key={s.l} className="rounded-xl border border-border bg-card p-5">
            <div className="text-xs text-muted-foreground">{s.l}</div>
            <div className={`mt-2 text-2xl font-semibold ${s.tone ?? "text-foreground"}`}>{s.v}</div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-4 gap-6">
        <aside className="space-y-1">
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 px-2">카테고리</h3>
          {[
            ["전체", 24, true],
            ["결제창", 8, false],
            ["WEB API", 6, false],
            ["해외결제", 4, false],
            ["SMS 링크결제", 2, false],
            ["원천사 규격", 3, false],
            ["기타", 1, false],
          ].map(([n, c, active]) => (
            <button key={n as string} className={`w-full flex items-center justify-between px-3 py-2 rounded-md text-sm ${active ? "bg-secondary text-foreground font-medium" : "text-muted-foreground hover:bg-secondary/60 hover:text-foreground"}`}>
              <span>{n as string}</span>
              <span className="text-[11px] font-mono text-muted-foreground">{c as number}</span>
            </button>
          ))}
        </aside>

        <div className="col-span-3 rounded-xl border border-border bg-card overflow-hidden">
          <div className="flex items-center gap-3 px-5 py-3 border-b border-border">
            <div className="flex-1 flex items-center gap-2 rounded-md border border-border bg-secondary/40 px-3 py-1.5">
              <span className="text-muted-foreground text-xs">⌕</span>
              <input className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground" placeholder="문서명 / 키워드 검색" />
            </div>
            <button className="text-xs text-muted-foreground hover:text-foreground">필터 ⌄</button>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-[11px] text-muted-foreground border-b border-border uppercase tracking-wider">
                <th className="text-left font-medium px-5 py-3">문서명</th>
                <th className="text-left font-medium py-3">버전</th>
                <th className="text-left font-medium py-3">최종 업데이트</th>
                <th className="text-left font-medium py-3">청크</th>
                <th className="text-left font-medium py-3">상태</th>
                <th className="w-10 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {docs.map((d) => (
                <tr key={d.name} className="border-b border-border last:border-0 hover:bg-secondary/30">
                  <td className="px-5 py-3.5">
                    <div className="flex items-center gap-3">
                      <div className="h-8 w-8 rounded bg-primary/10 text-primary flex items-center justify-center text-[10px] font-bold">PDF</div>
                      <div>
                        <div className="font-medium text-foreground">{d.name}</div>
                        <div className="text-[11px] text-muted-foreground">{d.size}</div>
                      </div>
                    </div>
                  </td>
                  <td className="py-3.5 text-xs font-mono text-muted-foreground">{d.ver}</td>
                  <td className="py-3.5 text-xs text-foreground">{d.date}</td>
                  <td className="py-3.5 text-xs font-mono text-muted-foreground">{d.chunks.toLocaleString()}</td>
                  <td className="py-3.5">
                    <span className={`inline-flex items-center gap-1.5 text-[11px] font-medium ${d.status === "동기화됨" ? "text-[color:var(--success)]" : "text-[color:var(--warning)]"}`}>
                      <span className={`h-1.5 w-1.5 rounded-full ${d.status === "동기화됨" ? "bg-[color:var(--success)]" : "bg-[color:var(--warning)]"}`} />
                      {d.status}
                    </span>
                  </td>
                  <td className="py-3.5 pr-5 text-right text-muted-foreground">⋯</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
