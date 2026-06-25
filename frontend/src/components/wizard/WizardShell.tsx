import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { useWizard, WizardProvider } from "./WizardContext";
import { isIntakeSlideActive } from "./types";
import { fetchDevRequestJson, toSavePayload } from "./submit";
import { useSaveRequest } from "@/lib/requests";
import { Slide1FuncType } from "./slides/Slide1FuncType";
import { Slide2Category } from "./slides/Slide2Category";
import { Slide3Basics } from "./slides/Slide3Basics";
import { Slide4Details } from "./slides/Slide4Details";
import { Slide5Impact } from "./slides/Slide5Impact";
import { Slide6AiDeepDive } from "./slides/Slide6AiDeepDive";
import { Slide7PaymentMethodIntake } from "./slides/Slide7PaymentMethodIntake";
import type { WizardData } from "./types";

// 외부 진입점 — 라우트(new.tsx)에서 이걸 쓴다.
export function WizardShell() {
  return (
    <WizardProvider>
      <WizardInner />
    </WizardProvider>
  );
}

type StepMeta = {
  n: number;
  label: string;
  canProceed: (d: WizardData) => boolean;
};

const BASE_STEPS: StepMeta[] = [
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
    label: "상세 1/3",
    canProceed: (d) => !!d.serviceName?.trim() && !!d.targetSchedule?.trim(),
  },
  {
    n: 5,
    label: "상세 2/3 · 임팩트",
    // NEW는 문제점/개선점 필드를 숨기므로 자동 통과 (submit 시 placeholder 자동 채움).
    canProceed: (d) =>
      d.funcTypeCode === "NEW" || !!d.problemAndImprovement?.trim(),
  },
  // S6은 자유 메모/체크 위주라 진행 차단 없음 — 비워도 제출 가능
  { n: 6, label: "상세 3/3", canProceed: () => true },
];

// S7은 신규 표준결제창-카드일 때만 끼움. 필수값 강제하지 않음(빈 폼이어도 제출 허용 — 정책)
const INTAKE_STEP: StepMeta = {
  n: 7,
  label: "신규 지불수단 폼",
  canProceed: () => true,
};

/** 현재 데이터 상태에 따라 위저드가 노출할 step 메타 목록을 반환. */
function buildSteps(d: WizardData): StepMeta[] {
  return isIntakeSlideActive(d) ? [...BASE_STEPS, INTAKE_STEP] : BASE_STEPS;
}

function WizardInner() {
  const { state, next, prev, goto } = useWizard();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const saveRequest = useSaveRequest();

  // 위저드 [요청서 생성하기] 한 번에 두 단계가 묶여 돈다:
  //   ① Gemini 호출 (백엔드 GET generate) → ProjectMdResult + markdown
  //   ② DB 저장 (POST /api/requests) → 저장된 id 받음
  //   ③ /result/{id} 상세로 이동 (DB 기반 영구 페이지)
  const submit = useMutation({
    mutationFn: async () => {
      const generate = await fetchDevRequestJson(state.data);
      const payload = toSavePayload({ data: state.data, generate });
      const saved = await saveRequest.mutateAsync(payload);
      return saved;
    },
    onSuccess: (saved) => {
      // fullPath 기준 (식별자는 /result_/$id 지만, to는 사용자 URL 기준 /result/$id)
      void navigate({ to: "/result/$id", params: { id: String(saved.id) } });
    },
    onError: (e: unknown) => {
      setSubmitError(e instanceof Error ? e.message : String(e));
    },
  });

  const current = state.currentSlide;
  // 데이터에 따라 노출 step 동적 계산 — S7(신규 지불수단 폼) 분기
  const steps = useMemo(() => buildSteps(state.data), [state.data]);
  const totalSteps = steps.length;
  // 사용자가 S7까지 갔다가 S2에서 카드를 풀어 S7이 사라진 경우 step 메타가 없을 수 있음 — 안전한 fallback
  const step = steps.find((s) => s.n === current) ?? steps[steps.length - 1];
  const canProceed = useMemo(() => step.canProceed(state.data), [step, state.data]);
  const isLast = step.n === steps[steps.length - 1].n;

  return (
    <div className="px-6 sm:px-10 py-8 max-w-[920px] mx-auto">
      <div className="flex items-center gap-2 text-xs text-muted-foreground mb-3">
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
          <span>{Math.round((current / totalSteps) * 100)}%</span>
        </div>
        <Progress value={(current / totalSteps) * 100} className="h-2" />
        <ol className="mt-3 flex items-center gap-1.5 overflow-x-auto">
          {steps.map((s) => {
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
          {current === 7 && <Slide7PaymentMethodIntake />}
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
