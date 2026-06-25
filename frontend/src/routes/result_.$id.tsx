import { createFileRoute, Link, useNavigate, useParams } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  useDeleteRequest,
  useRequestDetail,
  type DevRequestDetail,
  type DevRequestStatus,
} from "@/lib/requests";
import type {
  AdditionalCheckItem,
  AsIsToBe,
  ProjectMdResult,
  S6Answers,
  WizardData,
} from "@/components/wizard/types";

// /result/{id} — 저장된 요청서 상세 (flat route, /result_/$id 식별자).
// 디자인은 lovable 원본의 §1~§4 + 추가확인 + 사이드 그대로.
// 표준 양식 본문 섹션은 두지 않고, 본문은 [⎘ MD 복사] 버튼으로만 활용한다.
export const Route = createFileRoute("/result_/$id")({
  head: () => ({
    meta: [{ title: "요청서 상세 · Req-Genie" }],
  }),
  component: ResultDetail,
});

function ResultDetail() {
  const { id } = useParams({ from: "/result_/$id" });
  const numericId = Number(id);
  const { data, isLoading, isError, error } = useRequestDetail(
    Number.isFinite(numericId) ? numericId : undefined,
  );

  if (isLoading) {
    return (
      <div className="px-10 py-16 text-center text-sm text-muted-foreground">
        불러오는 중…
      </div>
    );
  }
  if (isError || !data) {
    return (
      <div className="px-10 py-16 text-center">
        <p className="text-sm text-destructive">
          상세 조회 실패: {error instanceof Error ? error.message : "데이터 없음"}
        </p>
        <Link to="/list" className="mt-3 inline-block text-sm text-primary hover:underline">
          ← 목록으로
        </Link>
      </div>
    );
  }
  return <Loaded detail={data} />;
}

// details JSON 형태 — toSavePayload가 저장한 구조
type ParsedDetails = {
  wizard?: WizardData;
  projectMd?: ProjectMdResult;
};

function Loaded({ detail }: { detail: DevRequestDetail }) {
  const navigate = useNavigate();
  const deleteReq = useDeleteRequest();
  const [copied, setCopied] = useState(false);

  // 저장 시점에 details에 직렬화한 위저드 입력 + ProjectMdResult를 다시 복원
  const parsed: ParsedDetails = useMemo(() => {
    if (!detail.details) return {};
    try {
      return JSON.parse(detail.details) as ParsedDetails;
    } catch {
      return {};
    }
  }, [detail.details]);

  const r: ProjectMdResult = parsed.projectMd ?? {};
  const wizard = parsed.wizard;
  const s6: S6Answers | undefined = wizard?.s6;
  const asisTobe: AsIsToBe | undefined = s6?.asisTobe;
  const cardOpts = s6?.cardOptions;

  const checks: AdditionalCheckItem[] = wizard?.additionalCheckItems ?? [];
  const completeness = computeCompleteness(r);

  const copyMarkdown = async () => {
    if (!detail.combinedMarkdown) return;
    try {
      await navigator.clipboard.writeText(detail.combinedMarkdown);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      window.alert("클립보드 접근이 거부되었습니다. 본문을 직접 선택해 복사해 주세요.");
    }
  };

  const onDelete = async () => {
    if (!window.confirm("이 요청서를 삭제하시겠습니까? (소프트 삭제 — 복원은 DB 수동)")) return;
    await deleteReq.mutateAsync(detail.id);
    void navigate({ to: "/list" });
  };

  return (
    <div className="px-10 py-8 max-w-[1180px] mx-auto">
      {/* 브레드크럼 */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground mb-3">
        <Link to="/list" className="hover:text-foreground">생성 요청서 목록</Link>
        <span>/</span>
        <span className="text-foreground font-mono">#{detail.id}</span>
      </div>

      {/* 헤더 */}
      <div className="flex items-start justify-between mb-6 gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-2 mb-2">
            <span className="text-[11px] font-mono text-muted-foreground">
              RQ-{String(detail.id).padStart(6, "0")}
            </span>
            <StatusBadge status={detail.status} />
            {detail.categoryPath && (
              <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium bg-accent/15 text-accent">
                {detail.categoryPath}
              </span>
            )}
          </div>
          <h1 className="text-2xl font-semibold text-foreground tracking-tight">
            {detail.title || r.productName || "개발요청서"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1.5">
            {formatDate(detail.createdAt)} 생성 · {detail.author || "—"}
            {detail.dept ? ` (${detail.dept})` : ""}
            {detail.updatedAt && detail.updatedAt !== detail.createdAt
              ? ` · 최근수정 ${formatDate(detail.updatedAt)}`
              : ""}
            {checks.length > 0 ? ` · 추가 확인 ${checks.length}건` : ""}
          </p>
        </div>
        <div className="flex flex-col items-end gap-1">
          <div className="flex gap-2">
            <button
              onClick={copyMarkdown}
              disabled={!detail.combinedMarkdown}
              className="rounded-md border border-border bg-card px-3 py-2 text-xs text-foreground hover:bg-secondary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {copied ? "✓ 복사됨" : "⎘ MD 복사"}
            </button>
            <button
              onClick={onDelete}
              disabled={deleteReq.isPending}
              className="rounded-md border border-destructive/40 text-destructive px-3 py-2 text-xs font-medium hover:bg-destructive/10 disabled:opacity-50"
            >
              🗑 삭제
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-6">
        <article className="col-span-2 rounded-xl border border-border bg-card overflow-hidden">
          {/* 추가 확인 필요 항목 */}
          {checks.length > 0 && <UnconfirmedBanner checks={checks} />}

          <div className="p-8 prose-sm max-w-none">
            <Section n="1" title="요청 개요">
              <Row k="요청 일자" v={r.createdDate ?? formatDateShort(detail.createdAt)} />
              <Row k="목표 일정" v={r.targetDate} />
              <Row k="가맹점 / 원천사" v={joinMerchantProvider(r)} />
              <Row k="서비스명" v={r.productName ?? detail.title} />
            </Section>

            <Section n="2" title="추진 배경">
              {r.promotionBackground?.trim() ? (
                <p className="text-sm text-foreground leading-relaxed whitespace-pre-line">
                  {r.promotionBackground}
                </p>
              ) : (
                <p className="text-sm text-muted-foreground">(추진 배경 정보 없음)</p>
              )}
              {r.issueAndImprovement?.trim() && (
                <details className="mt-3 rounded-md border border-border bg-secondary/30 p-3">
                  <summary className="cursor-pointer text-xs font-medium text-foreground">
                    문제점 · 개선점 (AI 재해석)
                  </summary>
                  <div className="mt-2 text-sm text-foreground leading-relaxed whitespace-pre-line">
                    {r.issueAndImprovement}
                  </div>
                </details>
              )}
            </Section>

            <Section n="3" title="AS-IS / TO-BE">
              {asisTobe?.kind ? (
                <div className="grid grid-cols-2 gap-3 text-xs">
                  <div className="rounded-md border border-border p-3">
                    <div className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-1">
                      AS-IS · {asisTobe.kind}
                    </div>
                    <div className="text-sm text-foreground whitespace-pre-line">
                      {asisTobe.asis?.trim() || "—"}
                    </div>
                  </div>
                  <div className="rounded-md border border-primary/30 bg-primary/5 p-3">
                    <div className="text-[10px] font-semibold text-primary uppercase tracking-wider mb-1">
                      TO-BE · {asisTobe.kind}
                    </div>
                    <div className="text-sm text-foreground whitespace-pre-line">
                      {asisTobe.tobe?.trim() || "—"}
                    </div>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">
                  (신규 개발이라 AS-IS / TO-BE 입력이 없음)
                </p>
              )}
            </Section>

            <Section n="4" title="정책 상세">
              <table className="w-full text-sm">
                <tbody>
                  {([
                    ["할부 옵션", cardOpts?.installments?.join(", ")],
                    ["포인트 / 머니 동시 사용", boolLabel(cardOpts?.usePoint)],
                    ["프로모션 / 즉시할인", boolLabel(cardOpts?.usePromotion)],
                    ["서비스 채널 / 결제 방식", r.serviceChannelAndPaymentMethod],
                    ["인증 / 승인 / 매입 주체", r.authApprovalAcquirerSubject],
                    ["최소 결제 금액", formatAmount(r.minimumPaymentAmount)],
                    ["부분 취소 / 환불 정책", r.partialCancelAndRefundPolicy],
                    ["현금영수증 발행 주체", r.cashReceiptIssuer],
                  ] as [string, string | undefined][])
                    .filter(([, v]) => v && v.trim())
                    .map(([k, v]) => (
                      <tr key={k} className="border-b border-border last:border-0">
                        <td className="py-2.5 pr-4 text-xs text-muted-foreground w-44">{k}</td>
                        <td className="py-2.5 text-foreground whitespace-pre-line">{v}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </Section>

            <Section n="5" title="개발 범위 · 복잡도 (AI 분석)">
              {(r.developmentScope || r.estimatedComplexity || r.prerequisiteActions || r.riskAndConstraints) ? (
                <div className="space-y-3 text-sm">
                  {r.estimatedComplexity && (
                    <div className="flex items-center gap-3">
                      <span className="text-xs text-muted-foreground w-28">예상 복잡도</span>
                      <ComplexityBadge value={r.estimatedComplexity} />
                    </div>
                  )}
                  {r.developmentScope?.trim() && (
                    <div>
                      <div className="text-xs text-muted-foreground mb-1">개발 범위</div>
                      <p className="text-sm text-foreground leading-relaxed whitespace-pre-line">
                        {r.developmentScope}
                      </p>
                    </div>
                  )}
                  {r.prerequisiteActions?.trim() && (
                    <div>
                      <div className="text-xs text-muted-foreground mb-1">선행 조건</div>
                      <p className="text-sm text-foreground leading-relaxed whitespace-pre-line">
                        {r.prerequisiteActions}
                      </p>
                    </div>
                  )}
                  {r.riskAndConstraints?.trim() && (
                    <div className="rounded-md border border-[color:var(--warning)]/30 bg-[color:var(--warning)]/5 p-3">
                      <div className="text-xs font-semibold text-[color:var(--warning)] mb-1">
                        ⚠ 리스크 / 제약
                      </div>
                      <p className="text-sm text-foreground leading-relaxed whitespace-pre-line">
                        {r.riskAndConstraints}
                      </p>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">(AI 분석 데이터 없음)</p>
              )}
            </Section>

            <Section n="6" title="기대 효과">
              <div className="grid grid-cols-2 gap-3">
                <Stat
                  label="예상 결제액 / 수익"
                  value={r.expectedRevenue?.trim() || "—"}
                  tone={r.expectedRevenue?.trim() ? "success" : "muted"}
                />
                <Stat
                  label="예상 손해 / 리스크"
                  value={r.expectedLoss?.trim() || "확인 필요"}
                  tone={r.expectedLoss?.trim() ? "muted" : "warn"}
                />
              </div>
              {r.expectedEffect?.trim() && (
                <p className="mt-3 text-sm text-foreground leading-relaxed whitespace-pre-line">
                  <b>기대 효과 · </b>{r.expectedEffect}
                </p>
              )}
            </Section>

            {(r.pendingQuestions?.length ?? 0) > 0 && (
              <Section n="7" title="현업 재확인 필요 항목 (핑퐁 방지)">
                <p className="text-xs text-muted-foreground mb-3">
                  아래 항목을 현업에게 한 번에 확인하면 추가 핑퐁 없이 개발을 착수할 수 있습니다.
                </p>
                <ol className="space-y-2 text-sm text-foreground list-decimal pl-5">
                  {r.pendingQuestions!.map((q, i) => (
                    <li key={i} className="leading-relaxed">{q}</li>
                  ))}
                </ol>
              </Section>
            )}

            {(r.assumptionList?.length ?? 0) > 0 && (
              <Section n="8" title="AI 추론 근거 (가정 목록)">
                <p className="text-xs text-muted-foreground mb-3">
                  아래 항목은 입력에 명시되지 않아 AI가 참고 문서·PG 도메인 지식으로 추론했습니다. 확인 후 수정하세요.
                </p>
                <ul className="space-y-1.5 text-sm text-foreground">
                  {r.assumptionList!.map((a, i) => (
                    <li key={i} className="flex gap-2">
                      <span className="text-muted-foreground mt-0.5">·</span>
                      <span className="leading-relaxed">{a}</span>
                    </li>
                  ))}
                </ul>
              </Section>
            )}

            <Section n="9" title="설계 흐름 다이어그램">
              <FlowDiagram requestId={detail.id} hasCached={!!detail.flowDiagram?.trim()} />
            </Section>
          </div>
        </article>

        <aside className="space-y-4">
          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
              목차
            </h3>
            <ol className="space-y-2 text-sm">
              {[
                "요청 개요",
                "추진 배경",
                "AS-IS / TO-BE",
                "정책 상세",
                "개발 범위 · 복잡도",
                "기대 효과",
                ...((r.pendingQuestions?.length ?? 0) > 0 ? ["현업 재확인 필요"] : []),
                ...((r.assumptionList?.length ?? 0) > 0 ? ["AI 추론 근거"] : []),
                "설계 흐름 다이어그램",
              ].map((t, i) => (
                <li key={t} className="flex gap-2 text-foreground">
                  <span className="text-xs text-muted-foreground font-mono w-5">{i + 1}.</span>
                  {t}
                </li>
              ))}
            </ol>
          </div>

          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
              AI 검증 요약
            </h3>
            <div className="space-y-3 text-xs">
              <Metric label="요청서 완성도" v={`${completeness}%`} tone={completeness >= 80 ? "success" : completeness >= 50 ? undefined : "warn"} />
              <Metric label="추가 확인 항목" v={`${checks.length}건`} tone={checks.length > 0 ? "warn" : "success"} />
              <Metric label="DB id" v={`#${detail.id}`} />
            </div>
          </div>

          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
              빠른 이동
            </h3>
            <div className="space-y-2">
              <Link to="/list">
                <Button variant="outline" className="w-full justify-start">
                  ← 목록으로
                </Button>
              </Link>
              <Link to="/new">
                <Button variant="outline" className="w-full justify-start">
                  ＋ 새 요청서 작성
                </Button>
              </Link>
            </div>
          </div>
        </aside>
      </div>

      {deleteReq.isError && (
        <div className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
          삭제 실패:{" "}
          {deleteReq.error instanceof Error ? deleteReq.error.message : String(deleteReq.error)}
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────
// 보조 컴포넌트 (/result.tsx와 동일 — 코드 중복 인정. 분리는 추후 필요 시)
// ─────────────────────────────────────────────────────────────────────

function UnconfirmedBanner({ checks }: { checks: AdditionalCheckItem[] }) {
  return (
    <div className="border-b-2 border-[color:var(--warning)]/40 bg-[color:var(--warning)]/5 p-6">
      <div className="flex items-center gap-2 text-[color:var(--warning)] text-xs font-semibold uppercase tracking-wider mb-3">
        ⚠ 현업 추가 확인 필요 항목 ({checks.length}건)
      </div>
      <ul className="space-y-2.5 text-sm text-foreground">
        {checks.map((c, i) => (
          <li key={`${c.slide}-${c.field}-${i}`} className="flex gap-3">
            <span className="font-mono text-xs text-[color:var(--warning)] mt-0.5">
              {String(i + 1).padStart(2, "0")}
            </span>
            <div>
              <div className="font-medium">{c.field}</div>
              {c.reason && <div className="text-xs text-muted-foreground mt-0.5">{c.reason}</div>}
              <div className="text-[10px] text-muted-foreground/70 mt-0.5">위저드 S{c.slide}</div>
            </div>
          </li>
        ))}
      </ul>
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

function Row({ k, v }: { k: string; v?: string | null }) {
  const empty = !v || !v.trim();
  return (
    <div className="flex border-b border-border py-2 text-sm">
      <div className="w-32 text-xs text-muted-foreground">{k}</div>
      <div className={`text-foreground ${empty ? "text-muted-foreground/50" : ""}`}>
        {empty ? "—" : v}
      </div>
    </div>
  );
}

function Stat({ label, value, tone }: { label: string; value: string; tone: "success" | "warn" | "muted" }) {
  const borderBg =
    tone === "success"
      ? "border-[color:var(--success)]/30 bg-[color:var(--success)]/5"
      : tone === "warn"
        ? "border-[color:var(--warning)]/30 bg-[color:var(--warning)]/5"
        : "border-border bg-card";
  const valueColor =
    tone === "success"
      ? "text-[color:var(--success)]"
      : tone === "warn"
        ? "text-[color:var(--warning)]"
        : "text-foreground";
  return (
    <div className={`rounded-md border p-3 ${borderBg}`}>
      <div className="text-[11px] text-muted-foreground">{label}</div>
      <div className={`text-lg font-semibold mt-0.5 ${valueColor}`}>{value}</div>
    </div>
  );
}

function Metric({ label, v, tone }: { label: string; v: string; tone?: "success" | "warn" }) {
  const c =
    tone === "success"
      ? "text-[color:var(--success)]"
      : tone === "warn"
        ? "text-[color:var(--warning)]"
        : "text-foreground";
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className={`font-semibold ${c}`}>{v}</span>
    </div>
  );
}

function StatusBadge({ status }: { status: DevRequestStatus }) {
  const cls =
    status === "AI_ANALYZED"
      ? "bg-[color:var(--success)]/15 text-[color:var(--success)]"
      : "bg-secondary text-muted-foreground";
  const label = status === "AI_ANALYZED" ? "AI분석완료" : "작성중";
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium ${cls}`}>
      {label}
    </span>
  );
}

/**
 * 설계 흐름 다이어그램 — 백엔드 GET /api/requests/{id}/flow.png를 img src로 직접 호출.
 * - DB에 XML 캐시가 있으면 (hasCached) 자동 로드 (빠름, Gemini 호출 X)
 * - 없으면 [⟳ 다이어그램 생성] 버튼으로 명시적 트리거 (한 번 누르면 DB에 캐시되어 다음부터 자동)
 */
function FlowDiagram({ requestId, hasCached }: { requestId: number; hasCached: boolean }) {
  const [trigger, setTrigger] = useState(hasCached);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [cacheBust, setCacheBust] = useState(Date.now());

  const src = `/api/requests/${requestId}/flow.png?t=${cacheBust}`;

  const handleGenerate = () => {
    setBusy(true);
    setErr(null);
    setTrigger(true);
    setCacheBust(Date.now());
  };

  if (!trigger) {
    return (
      <div className="rounded-md border border-dashed border-border bg-secondary/30 p-6 text-center">
        <p className="text-sm text-muted-foreground mb-3">
          이 요청서의 거래 흐름 시퀀스 다이어그램이 아직 생성되지 않았습니다.
          <br />
          <span className="text-[11px]">생성 시 Gemini를 1회 호출합니다 (약 30~60초 소요).</span>
        </p>
        <Button onClick={handleGenerate}>⟳ 다이어그램 생성하기</Button>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {busy && (
        <p className="text-xs text-muted-foreground">다이어그램 불러오는 중…</p>
      )}
      {err && (
        <div className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
          이미지 로드 실패: {err}
          <button
            onClick={handleGenerate}
            className="ml-2 text-foreground underline"
          >
            재시도
          </button>
        </div>
      )}
      <div className="rounded-md border border-border bg-white p-3 overflow-auto">
        <img
          src={src}
          alt="설계 흐름 시퀀스 다이어그램"
          className="w-full h-auto"
          onLoad={() => {
            setBusy(false);
            setErr(null);
          }}
          onError={() => {
            setBusy(false);
            setErr("백엔드 응답 실패");
          }}
        />
      </div>
      <p className="text-[11px] text-muted-foreground">
        ※ 다이어그램은 DB에 캐시되어 다음 방문 시 즉시 표시됩니다.
        {" "}
        <button onClick={handleGenerate} className="text-primary hover:underline">
          ⟳ 다시 생성 (Gemini 추가 호출)
        </button>
      </p>
    </div>
  );
}

function ComplexityBadge({ value }: { value: string }) {
  const v = value.trim().toUpperCase();
  const map: Record<string, { cls: string; label: string }> = {
    LOW: { cls: "bg-[color:var(--success)]/15 text-[color:var(--success)]", label: "🟢 LOW — 단순 설정/파라미터" },
    MID: { cls: "bg-[color:var(--warning)]/15 text-[color:var(--warning)]", label: "🟡 MID — 신규 API 연동 또는 기존 로직 수정" },
    HIGH: { cls: "bg-destructive/15 text-destructive", label: "🔴 HIGH — 원천사 신규 협의/계약, 복수 시스템 영향" },
  };
  const meta = map[v] ?? { cls: "bg-secondary text-foreground", label: value };
  return (
    <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-medium ${meta.cls}`}>
      {meta.label}
    </span>
  );
}

// ─────────────────────────────────────────────────────────────────────
// 헬퍼
// ─────────────────────────────────────────────────────────────────────

function joinMerchantProvider(r: ProjectMdResult): string | undefined {
  const merchant = [r.merchantName, r.mid].filter((x) => x && x.trim()).join(" · ");
  const provider = r.providerName?.trim();
  if (merchant && provider) return `${provider} (${merchant})`;
  return provider || merchant || undefined;
}

function boolLabel(v: boolean | undefined): string | undefined {
  if (v === true) return "사용 가능";
  if (v === false) return "사용 불가";
  return undefined;
}

function formatAmount(raw?: string): string | undefined {
  if (!raw) return undefined;
  const digits = raw.replace(/[^0-9]/g, "");
  if (!digits) return raw;
  return `${Number(digits).toLocaleString("ko-KR")}원`;
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

function formatDateShort(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" });
  } catch {
    return iso;
  }
}

function computeCompleteness(r: ProjectMdResult): number {
  const fields: (string | boolean | null | undefined)[] = [
    r.author, r.createdDate, r.department, r.projectId,
    r.mid, r.merchantName, r.merchantBusinessNumber,
    r.providerName, r.providerCollaborationBackground,
    r.promotionBackground, r.additionalInfo, r.targetDate, r.productName,
    r.issueAndImprovement, r.issueVerificationMethod,
    r.serviceChannelAndPaymentMethod, r.authApprovalAcquirerSubject,
    r.minimumPaymentAmount, r.partialCancelAndRefundPolicy, r.cashReceiptIssuer,
    r.expectedRevenue, r.expectedLoss, r.expectedEffect,
    r.transferGuide,
    r.developmentInProgress,
    r.merchantRelatedDevelopment, r.providerRelatedDevelopment,
    r.newServiceOrSelfImprovement, r.securityAndAuditDevelopment,
  ];
  const filled = fields.filter((v) => {
    if (typeof v === "boolean") return true;
    if (v == null) return false;
    return String(v).trim().length > 0;
  }).length;
  return Math.round((filled / fields.length) * 100);
}
