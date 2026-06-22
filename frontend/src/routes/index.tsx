import { createFileRoute, Link } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "대시보드 · Req-Genie" },
      { name: "description", content: "결제 도메인 개발요청서 현황과 AI 검증 지표를 한눈에 확인합니다." },
    ],
  }),
  component: Dashboard,
});

const stats = [
  { label: "이번 달 생성 요청서", value: "47", delta: "+12", tone: "text-foreground" },
  { label: "평균 소통 리드타임", value: "0.8일", delta: "-3.2일", tone: "text-[color:var(--success)]" },
  { label: "반려율", value: "8%", delta: "-62%", tone: "text-[color:var(--success)]" },
  { label: "추가 확인 항목 해소율", value: "92%", delta: "+18%", tone: "text-[color:var(--success)]" },
];

const recent = [
  { id: "RQ-2026-0612-031", title: "네이버페이 신규 지불수단 연동", category: "결제창 · 신규", author: "김지윤 (PG사업팀)", status: "AI 검증 완료", date: "06-17", missing: 2 },
  { id: "RQ-2026-0612-030", title: "해외카드 3DS 2.2 인증 플로우 개편", category: "해외결제 · 수정", author: "박서연 (글로벌결제팀)", status: "추가 확인 대기", date: "06-17", missing: 5 },
  { id: "RQ-2026-0612-029", title: "SMS 링크결제 만료시간 정책 변경", category: "SMS링크결제 · 수정", author: "이도현 (커머스기획)", status: "개발 착수", date: "06-16", missing: 0 },
  { id: "RQ-2026-0612-028", title: "WEB API 가맹점 한도 조회 신규", category: "WEB API · 신규", author: "정민호 (가맹점지원)", status: "AI 검증 완료", date: "06-16", missing: 1 },
];

function Dashboard() {
  return (
    <div className="px-10 py-8 max-w-[1280px] mx-auto">
      <div className="flex items-start justify-between mb-8">
        <div>
          <div className="text-xs font-medium tracking-wider text-muted-foreground uppercase mb-2">Workspace · PG결제개발실</div>
          <h1 className="text-3xl font-semibold text-foreground tracking-tight">개발요청서 대시보드</h1>
          <p className="text-sm text-muted-foreground mt-2">현업 요청서 작성부터 개발 착수까지의 흐름을 AI가 함께 검증합니다.</p>
        </div>
        <Link
          to="/new"
          className="inline-flex items-center gap-2 rounded-md bg-primary text-primary-foreground px-4 py-2.5 text-sm font-medium shadow-[var(--shadow-elegant)] hover:opacity-90"
        >
          <span>＋</span> 새 요청서 작성
        </Link>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-10">
        {stats.map((s) => (
          <div key={s.label} className="rounded-xl border border-border bg-card p-5 shadow-[var(--shadow-card)]">
            <div className="text-xs text-muted-foreground">{s.label}</div>
            <div className="mt-3 flex items-baseline gap-2">
              <div className={`text-2xl font-semibold tracking-tight ${s.tone}`}>{s.value}</div>
              <div className="text-xs text-muted-foreground">{s.delta}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-3 gap-6">
        <div className="col-span-2 rounded-xl border border-border bg-card">
          <div className="flex items-center justify-between px-6 py-4 border-b border-border">
            <div>
              <h2 className="text-sm font-semibold text-foreground">최근 요청서</h2>
              <p className="text-xs text-muted-foreground mt-0.5">AI 검증 상태 및 추가 확인 항목 수</p>
            </div>
            <button className="text-xs text-muted-foreground hover:text-foreground">전체 보기 →</button>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-muted-foreground border-b border-border">
                <th className="text-left font-medium px-6 py-3">요청 ID</th>
                <th className="text-left font-medium py-3">제목 / 분류</th>
                <th className="text-left font-medium py-3">상태</th>
                <th className="text-right font-medium px-6 py-3">미확인</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((r) => (
                <tr key={r.id} className="border-b border-border last:border-0 hover:bg-secondary/40">
                  <td className="px-6 py-4 font-mono text-[11px] text-muted-foreground">{r.id}</td>
                  <td className="py-4">
                    <Link to="/result" className="font-medium text-foreground hover:text-accent">{r.title}</Link>
                    <div className="text-xs text-muted-foreground mt-0.5">{r.category} · {r.author}</div>
                  </td>
                  <td className="py-4">
                    <StatusBadge status={r.status} />
                  </td>
                  <td className="px-6 py-4 text-right">
                    {r.missing === 0 ? (
                      <span className="text-xs text-[color:var(--success)] font-medium">✓ 완료</span>
                    ) : (
                      <span className="inline-flex items-center justify-center min-w-6 h-6 px-2 rounded-full bg-[color:var(--warning)]/15 text-[10px] font-semibold text-[color:var(--warning)]">{r.missing}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>


        {/*<div className="space-y-4">
          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-sm font-semibold text-foreground mb-3">AI 도메인 학습 현황</h3>
            <div className="space-y-3">
              {[
                { name: "결제창 기능 사양", pct: 98 },
                { name: "WEB API 규격서", pct: 92 },
                { name: "해외결제 가이드", pct: 84 },
                { name: "SMS 링크결제", pct: 100 },
              ].map((k) => (
                <div key={k.name}>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-foreground">{k.name}</span>
                    <span className="text-muted-foreground font-mono">{k.pct}%</span>
                  </div>
                  <div className="h-1.5 rounded-full bg-secondary overflow-hidden">
                    <div className="h-full bg-primary" style={{ width: `${k.pct}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-xl border border-border p-5" style={{ background: "var(--gradient-primary)" }}>
            <div className="text-[11px] uppercase tracking-wider text-primary-foreground/70 font-medium">이번 분기 만족도</div>
            <div className="mt-2 text-3xl font-semibold text-primary-foreground">4.6<span className="text-base text-primary-foreground/60">/5.0</span></div>
            <div className="mt-1 text-xs text-primary-foreground/70">현업 32명 · 개발자 18명 응답</div>
          </div>

        </div>*/}
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const map: Record<string, string> = {
    "AI 검증 완료": "bg-[color:var(--success)]/12 text-[color:var(--success)]",
    "추가 확인 대기": "bg-[color:var(--warning)]/15 text-[color:var(--warning)]",
    "개발 착수": "bg-accent/15 text-accent",
  };
  return (
    <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-medium ${map[status] ?? "bg-secondary text-muted-foreground"}`}>
      {status}
    </span>
  );
}
