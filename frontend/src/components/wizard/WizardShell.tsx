import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { useWizard, WizardProvider } from "./WizardContext";
import { TOTAL_SLIDES } from "./types";
import { submitDevRequest } from "./submit";
import { Slide1FuncType } from "./slides/Slide1FuncType";
import { Slide2Category } from "./slides/Slide2Category";
import { Slide3Basics } from "./slides/Slide3Basics";
import { Slide4Details } from "./slides/Slide4Details";
import { Slide5Impact } from "./slides/Slide5Impact";
import { Slide6AiDeepDive } from "./slides/Slide6AiDeepDive";
import type { WizardData } from "./types";

// 외부 진입점 — 라우트(new.tsx)에서 이걸 쓴다.
export function WizardShell() {
  return (
    <WizardProvider>
      <WizardInner />
    </WizardProvider>
  );
}

// 단계별 메타 — 진행 표시·필수값 검증 함수 부착
const STEPS: {
  n: number;
  label: string;
  // 각 슬라이드의 [다음] 활성 조건 (필수 입력)
  canProceed: (d: WizardData) => boolean;
}[] = [
  { n: 1, label: "유형", canProceed: (d) => !!d.funcType },
  { n: 2, label: "대분류", canProceed: (d) => !!d.category },
  {
    n: 3,
    label: "기본·배경",
    canProceed: (d) =>
      !!d.author?.trim() && !!d.department?.trim() && !!d.background?.trim(),
  },
  {
    n: 4,
    label: "상세 1/2",
    canProceed: (d) => !!d.serviceName?.trim() && !!d.targetSchedule?.trim(),
  },
  {
    n: 5,
    label: "상세 2/2 · 임팩트",
    canProceed: (d) => !!d.problemAndImprovement?.trim(),
  },
  // S6은 자유 메모/체크 위주라 진행 차단 없음 — 비워도 제출 가능
  { n: 6, label: "AI 심층 질의", canProceed: () => true },
];

function WizardInner() {
  const { state, next, prev, goto } = useWizard();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const submit = useMutation({
    mutationFn: () => submitDevRequest(state.data),
    onSuccess: () => {
      // 성공 시 /result로. (응답 JSON 안에 requestId 등이 들어가도록 백엔드 §12-1 보강 시 그걸 query로 넘김)
      void navigate({ to: "/result" });
    },
    onError: (e: unknown) => {
      setSubmitError(e instanceof Error ? e.message : String(e));
    },
  });

  const current = state.currentSlide;
  const step = STEPS[current - 1];
  const canProceed = useMemo(() => step.canProceed(state.data), [step, state.data]);
  const isLast = current === TOTAL_SLIDES;

  return (
    <div className="px-6 sm:px-10 py-8 max-w-[920px] mx-auto">
      <div className="flex items-center gap-2 text-xs text-muted-foreground mb-3">
        <span>대시보드</span>
        <span>/</span>
        <span className="text-foreground">새 요청서 작성</span>
      </div>
      <h1 className="text-2xl font-semibold text-foreground tracking-tight">
        새 개발요청서 작성
      </h1>
      <p className="text-sm text-muted-foreground mt-1.5">
        한 단계씩 짚어가며 작성합니다. 입력은 자동 저장되며 [이전]을 눌러도 보존됩니다.
      </p>

      {/* Progress */}
      <div className="mt-6">
        <div className="flex items-center justify-between text-xs text-muted-foreground mb-2">
          <span>
            STEP {current} <span className="text-foreground/70">· {step.label}</span>
          </span>
          <span>{Math.round((current / TOTAL_SLIDES) * 100)}%</span>
        </div>
        <Progress value={(current / TOTAL_SLIDES) * 100} className="h-2" />
        <ol className="mt-3 flex items-center gap-1.5 overflow-x-auto">
          {STEPS.map((s) => {
            const done = s.n < current;
            const active = s.n === current;
            return (
              <li key={s.n}>
                <button
                  type="button"
                  onClick={() => goto(s.n)}
                  disabled={s.n > current}
                  className={`text-[11px] rounded-full px-2.5 py-1 border transition ${
                    active
                      ? "bg-primary text-primary-foreground border-primary"
                      : done
                        ? "bg-[color:var(--success)]/10 text-foreground border-[color:var(--success)]/30 hover:bg-[color:var(--success)]/20"
                        : "bg-secondary text-muted-foreground border-border cursor-not-allowed"
                  }`}
                >
                  {s.n}. {s.label}
                </button>
              </li>
            );
          })}
        </ol>
      </div>

      {/* Slide stage — 좌우 슬라이딩 (CSS transition) */}
      <div className="mt-6 overflow-hidden">
        <div
          key={current}
          className="animate-in fade-in slide-in-from-right-8 duration-300"
          // 이전 방향이면 좌측에서 진입 — Tailwind animate 유틸 한정으로 right만 쓰고 prev에 별도 클래스 적용
        >
          {current === 1 && <Slide1FuncType />}
          {current === 2 && <Slide2Category />}
          {current === 3 && <Slide3Basics />}
          {current === 4 && <Slide4Details />}
          {current === 5 && <Slide5Impact />}
          {current === 6 && <Slide6AiDeepDive />}
        </div>
      </div>

      {/* 에러 배너 */}
      {submitError && (
        <div className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
          제출 중 오류가 발생했습니다: {submitError}
        </div>
      )}

      {/* 네비게이션 */}
      <div className="mt-6 flex items-center justify-between gap-3">
        <Button variant="outline" onClick={prev} disabled={current === 1 || submit.isPending}>
          ← 이전
        </Button>

        {!isLast ? (
          <Button onClick={next} disabled={!canProceed}>
            다음 →
          </Button>
        ) : (
          <Button
            onClick={() => {
              setSubmitError(null);
              submit.mutate();
            }}
            disabled={submit.isPending}
          >
            {submit.isPending ? "생성 중…" : "요청서 생성하기"}
          </Button>
        )}
      </div>

      {/* 누적 추가 확인 항목 요약 (디버그 + 사용자에게도 보임) */}
      {(state.data.additionalCheckItems ?? []).length > 0 && (
        <div className="mt-6 rounded-md border border-[color:var(--warning)]/30 bg-[color:var(--warning)]/5 p-4">
          <div className="text-xs font-semibold text-[color:var(--warning)] mb-2">
            ⚠ 추가 확인 필요 항목 ({(state.data.additionalCheckItems ?? []).length})
          </div>
          <ul className="text-xs text-foreground space-y-1">
            {(state.data.additionalCheckItems ?? []).map((it) => (
              <li key={it.field}>
                · (S{it.slide}) {it.field}
                {it.reason ? <span className="text-muted-foreground"> — {it.reason}</span> : null}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
