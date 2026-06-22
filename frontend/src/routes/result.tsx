import { createFileRoute, Link } from "@tanstack/react-router";

export const Route = createFileRoute("/result")({
  head: () => ({
    meta: [
      { title: "생성된 개발요청서 · Req-Genie" },
      { name: "description", content: "AI가 생성한 표준 개발요청서와 추가 확인 필요 항목 리스트를 확인합니다." },
    ],
  }),
  component: Result,
});

function Result() {
  return (
    <div className="px-10 py-8 max-w-[1180px] mx-auto">
      <div className="flex items-center gap-2 text-xs text-muted-foreground mb-3">
        <Link to="/" className="hover:text-foreground">대시보드</Link>
        <span>/</span>
        <Link to="/new" className="hover:text-foreground">새 요청서</Link>
        <span>/</span>
        <span className="text-foreground">생성된 요청서</span>
      </div>

      <div className="flex items-start justify-between mb-6">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="text-[11px] font-mono text-muted-foreground">RQ-2026-0612-031</span>
            <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium bg-[color:var(--success)]/15 text-[color:var(--success)]">AI 검증 완료</span>
            <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium bg-accent/15 text-accent">결제창 · 신규</span>
          </div>
          <h1 className="text-2xl font-semibold text-foreground tracking-tight">네이버페이 신규 지불수단 (머니) 연동 개발요청서</h1>
          <p className="text-sm text-muted-foreground mt-1.5">2026.06.17 생성 · 김지윤 (PG사업팀) · AI 보완 23개 항목</p>
        </div>
        <div className="flex gap-2">
          <button className="rounded-md border border-border bg-card px-3 py-2 text-xs text-foreground hover:bg-secondary">↗ 공유</button>
          <button className="rounded-md border border-border bg-card px-3 py-2 text-xs text-foreground hover:bg-secondary">⌄ Word</button>
          <button className="rounded-md bg-primary text-primary-foreground px-3 py-2 text-xs font-medium">개발팀에 전달</button>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-6">
        <article className="col-span-2 rounded-xl border border-border bg-card overflow-hidden">
          {/* Highlighted: missing items */}
          <div className="border-b-2 border-[color:var(--warning)]/40 bg-[color:var(--warning)]/5 p-6">
            <div className="flex items-center gap-2 text-[color:var(--warning)] text-xs font-semibold uppercase tracking-wider mb-3">
              ⚠ 현업 추가 확인 필요 항목 (3건)
            </div>
            <ul className="space-y-2.5 text-sm text-foreground">
              {[
                { t: "프로모션 / 즉시할인 정책", d: "요청자: '모름' 응답. 마케팅팀 협의 후 회신 필요" },
                { t: "결제 실패 시 재시도 UX 정책", d: "AI 분석: 결제창 신규 추가 시 필수. PRD에 누락" },
                { t: "예상 손해 / 운영 리스크 수치", d: "기대 수익(3.2억)만 입력. 우선순위 산정에 필요" },
              ].map((m, i) => (
                <li key={i} className="flex gap-3">
                  <span className="font-mono text-xs text-[color:var(--warning)] mt-0.5">{String(i + 1).padStart(2, "0")}</span>
                  <div>
                    <div className="font-medium">{m.t}</div>
                    <div className="text-xs text-muted-foreground mt-0.5">{m.d}</div>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <div className="p-8 prose-sm max-w-none">
            <Section n="1" title="요청 개요">
              <Row k="요청 일자" v="2026-06-17" />
              <Row k="목표 일정" v="2026-07-31 (근거: 가맹점 신규 프로모션 런칭)" />
              <Row k="가맹점 / 원천사" v="네이버페이 (가맹점 ID 10293841)" />
              <Row k="서비스명" v="네이버페이 머니 결제" />
            </Section>

            <Section n="2" title="추진 배경 및 기대 효과">
              <p className="text-sm text-foreground leading-relaxed">
                현재 결제창 내 네이버페이는 포인트 결제만 지원 중. 가맹점 요청에 따라 머니 결제를 신규로 추가해 결제 전환율을 개선하고, 경쟁사(카카오페이) 대비 결제수단 커버리지 격차를 해소함.
              </p>
              <div className="mt-3 grid grid-cols-2 gap-3">
                <Stat label="예상 결제액 증가" value="+3.2억/월" tone="success" />
                <Stat label="예상 손해 / 리스크" value="확인 필요" tone="warn" />
              </div>
            </Section>

            <Section n="3" title="AS-IS / TO-BE">
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="rounded-md border border-border p-3">
                  <div className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-1">AS-IS</div>
                  <div className="text-sm text-foreground">결제창 내 네이버페이 = 포인트 결제만 노출</div>
                </div>
                <div className="rounded-md border border-primary/30 bg-primary/5 p-3">
                  <div className="text-[10px] font-semibold text-primary uppercase tracking-wider mb-1">TO-BE</div>
                  <div className="text-sm text-foreground">포인트 + 머니 결제 동시 노출 (머니 = 일시불, 포인트 동시 사용 불가)</div>
                </div>
              </div>
            </Section>

            <Section n="4" title="정책 상세">
              <table className="w-full text-sm">
                <tbody>
                  {[
                    ["할부 개월수", "일시불만 (네이버페이 머니 정책)"],
                    ["포인트 동시 사용", "불가"],
                    ["부분 취소", "지원 (원천사 규격 §4.2)"],
                    ["프로모션 / 즉시할인", "⚠ 확인 필요"],
                    ["결제 실패 재시도 UX", "⚠ 확인 필요"],
                  ].map(([k, v]) => (
                    <tr key={k} className="border-b border-border last:border-0">
                      <td className="py-2.5 pr-4 text-xs text-muted-foreground w-44">{k}</td>
                      <td className="py-2.5 text-foreground">{v}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Section>

            <Section n="5" title="API 요청 / 응답값 (AI 자동 추출)">
              <div className="rounded-md bg-foreground text-background p-4 font-mono text-[11px] leading-relaxed overflow-x-auto">
                <span className="text-[color:var(--warning)]">POST</span> /v3/pay/ready{"\n"}
                {"{"}{"\n"}
                {"  "}"merchantId": "10293841",{"\n"}
                {"  "}"payMethod": <span className="text-[color:var(--success)]">"NAVERPAY_MONEY"</span>,{"\n"}
                {"  "}"installment": 0,{"\n"}
                {"  "}"usePoint": false{"\n"}
                {"}"}
              </div>
              <p className="text-[11px] text-muted-foreground mt-2">naverpay_money_v2.3.pdf 38p · §3.1 발췌</p>
            </Section>
          </div>
        </article>

        <aside className="space-y-4">
          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">목차</h3>
            <ol className="space-y-2 text-sm">
              {["요청 개요", "추진 배경 및 기대 효과", "AS-IS / TO-BE", "정책 상세", "API 요청 / 응답값"].map((t, i) => (
                <li key={t} className="flex gap-2 text-foreground hover:text-accent cursor-pointer">
                  <span className="text-xs text-muted-foreground font-mono w-5">{i + 1}.</span>
                  {t}
                </li>
              ))}
            </ol>
          </div>

          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">AI 검증 요약</h3>
            <div className="space-y-3 text-xs">
              <Metric label="요청서 완성도" v="92%" />
              <Metric label="AI 자동 보완" v="23개 항목" />
              <Metric label="추가 확인 항목" v="3건" tone="warn" />
              <Metric label="규격서 정합성" v="High" tone="success" />
            </div>
          </div>

          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">활동 로그</h3>
            <ul className="space-y-3 text-xs">
              {[
                { t: "14:32", m: "개발팀 박개발 — 검토 시작" },
                { t: "14:28", m: "AI — 추가 확인 항목 3건 정리" },
                { t: "14:25", m: "김지윤 — AI 심층 질의 응답 완료" },
                { t: "14:18", m: "AI — 규격서 38p 분석 완료" },
              ].map((l, i) => (
                <li key={i} className="flex gap-2">
                  <span className="text-muted-foreground font-mono">{l.t}</span>
                  <span className="text-foreground">{l.m}</span>
                </li>
              ))}
            </ul>
          </div>
        </aside>
      </div>
    </div>
  );
}

function Section({ n, title, children }: { n: string; title: string; children: React.ReactNode }) {
  return (
    <section className="mb-7">
      <h2 className="flex items-baseline gap-3 text-base font-semibold text-foreground mb-3">
        <span className="text-xs font-mono text-muted-foreground">§{n}</span>
        {title}
      </h2>
      {children}
    </section>
  );
}

function Row({ k, v }: { k: string; v: string }) {
  return (
    <div className="flex border-b border-border py-2 text-sm">
      <div className="w-32 text-xs text-muted-foreground">{k}</div>
      <div className="text-foreground">{v}</div>
    </div>
  );
}

function Stat({ label, value, tone }: { label: string; value: string; tone: "success" | "warn" }) {
  return (
    <div className={`rounded-md border p-3 ${tone === "success" ? "border-[color:var(--success)]/30 bg-[color:var(--success)]/5" : "border-[color:var(--warning)]/30 bg-[color:var(--warning)]/5"}`}>
      <div className="text-[11px] text-muted-foreground">{label}</div>
      <div className={`text-lg font-semibold mt-0.5 ${tone === "success" ? "text-[color:var(--success)]" : "text-[color:var(--warning)]"}`}>{value}</div>
    </div>
  );
}

function Metric({ label, v, tone }: { label: string; v: string; tone?: "success" | "warn" }) {
  const c = tone === "success" ? "text-[color:var(--success)]" : tone === "warn" ? "text-[color:var(--warning)]" : "text-foreground";
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className={`font-semibold ${c}`}>{v}</span>
    </div>
  );
}
