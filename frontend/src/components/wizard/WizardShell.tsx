/**
 * @file WizardShell.tsx
 * @description 위저드의 최상위 진입점 컴포넌트.
 *
 * `WizardProvider` 로 전역 상태를 초기화하고, 실제 UI를 담당하는 `WizardInner` 를 감싼다.
 * 라우트 파일(`new.tsx`)에서 이 컴포넌트를 단독으로 임포트해 사용한다.
 *
 * @remarks
 * 위저드 제출 흐름:
 * 1. Gemini 호출 (백엔드 GET /api/dev-requests/generate) → `ProjectMdResult` + markdown
 * 2. DB 저장 (POST /api/requests) → 저장된 id 반환
 * 3. `/result/{id}` 상세 페이지로 이동 (DB 기반 영구 URL)
 */

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

/**
 * 위저드의 외부 진입점 컴포넌트.
 * `WizardProvider` 로 상태를 초기화한 뒤 `WizardInner` 를 렌더한다.
 * 라우트(`new.tsx`)에서 이 컴포넌트만 임포트하면 된다.
 */
export function WizardShell() {
  return (
    <WizardProvider>
      <WizardInner />
    </WizardProvider>
  );
}

/**
 * 위저드 단계(step) 하나의 메타 정보.
 */
type StepMeta = {
  /** 슬라이드 번호 (1-based) */
  n: number;
  /** 스텝 탭/배지에 표시할 짧은 라벨 */
  label: string;
  /**
   * 현재 데이터가 이 단계를 통과할 수 있는지 판별한다.
   * `false`이면 [다음] 버튼이 비활성화된다.
   *
   * @param d - 현재 위저드 데이터
   */
  canProceed: (d: WizardData) => boolean;
};

/**
 * 항상 노출되는 기본 단계 목록 (S1~S6).
 * 각 단계의 `canProceed` 조건은 UX 요구사항에 따라 정의한다.
 */
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
    // NEW 유형은 문제점/개선점 필드를 숨기므로 자동 통과 (submit 시 placeholder 자동 채움)
    canProceed: (d) =>
      d.funcTypeCode === "NEW" || !!d.problemAndImprovement?.trim(),
  },
  // S6는 자유 메모/체크 위주라 진행 차단 없음 — 비워도 제출 가능
  { n: 6, label: "상세 3/3", canProceed: () => true },
];

/**
 * S7 단계 메타 — 신규 표준결제창-카드 조건일 때만 `buildSteps`에서 끼워 넣는다.
 * 필수값을 강제하지 않으므로 빈 폼이어도 제출이 허용된다(정책).
 */
const INTAKE_STEP: StepMeta = {
  n: 7,
  label: "신규 지불수단 폼",
  canProceed: () => true,
};

/**
 * 현재 데이터 상태에 따라 위저드가 노출할 step 메타 목록을 동적으로 계산한다.
 *
 * @param d - 현재 위저드 데이터
 * @returns 노출할 단계 목록 (S7 포함 여부는 `isIntakeSlideActive`로 판별)
 */
function buildSteps(d: WizardData): StepMeta[] {
  return isIntakeSlideActive(d) ? [...BASE_STEPS, INTAKE_STEP] : BASE_STEPS;
}

/**
 * 위저드 실제 UI 컴포넌트.
 * `WizardProvider` 내부에서 렌더되며 `useWizard()` 훅으로 상태에 접근한다.
 *
 * @remarks
 * - `useMutation` 으로 Gemini 호출 → DB 저장을 순차 처리한다.
 * - 단계 목록(`steps`)은 데이터가 바뀔 때마다 `useMemo` 로 재계산한다.
 * - S7이 사라진 채로 해당 슬라이드에 있으면 마지막 단계로 안전하게 fallback한다.
 */
function WizardInner() {
  const { state, next, prev, goto } = useWizard();
  const navigate = useNavigate();

  /** 제출 중 발생한 오류 메시지. `null`이면 오류 없음 */
  const [submitError, setSubmitError] = useState<string | null>(null);
  const saveRequest = useSaveRequest();

  /**
   * 위저드 최종 제출 뮤테이션.
   * ① Gemini 호출 → ② DB 저장 → ③ /result/{id} 상세 페이지로 이동
   */
  const submit = useMutation({
    mutationFn: async () => {
      // ① 백엔드 GET generate 호출 — ProjectMdResult + markdown 수신
      const generate = await fetchDevRequestJson(state.data);
      // ② 위저드 데이터와 LLM 결과를 합쳐 저장 payload 구성
      const payload = toSavePayload({ data: state.data, generate });
      // ③ DB 저장 — 저장된 요청서 객체 반환
      const saved = await saveRequest.mutateAsync(payload);
      return saved;
    },
    onSuccess: (saved) => {
      // 저장 성공 시 /result/{id} 페이지로 이동 (fullPath 기준 경로)
      void navigate({ to: "/result/$id", params: { id: String(saved.id) } });
    },
    onError: (e: unknown) => {
      // 오류 메시지를 상태에 저장해 에러 배너로 표시
      setSubmitError(e instanceof Error ? e.message : String(e));
    },
  });

  const current = state.currentSlide;

  // 데이터에 따라 노출 단계 목록을 동적 계산 — S7(신규 지불수단 폼) 포함 여부
  const steps = useMemo(() => buildSteps(state.data), [state.data]);
  const totalSteps = steps.length;

  // 사용자가 S7에 있다가 카드 선택을 해제해 S7이 사라진 경우 step 메타가 없을 수 있음
  // → 마지막 단계로 안전하게 fallback
  const step = steps.find((s) => s.n === current) ?? steps[steps.length - 1];

  // 현재 단계의 진행 가능 여부 — [다음] 버튼 활성화 판별
  const canProceed = useMemo(() => step.canProceed(state.data), [step, state.data]);

  // 현재 단계가 마지막 단계이면 [다음] 대신 [요청서 생성하기] 버튼을 노출
  const isLast = step.n === steps[steps.length - 1].n;

  return (
    <div className="px-6 sm:px-10 py-8 max-w-[920px] mx-auto">
      {/* 페이지 제목 영역 */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground mb-3">
        <span className="text-foreground">새 요청서 작성</span>
      </div>
      <h1 className="text-2xl font-semibold text-foreground tracking-tight">
        새 개발요청서 작성
      </h1>
      <p className="text-sm text-muted-foreground mt-1.5">
        한 단계씩 짚어가며 작성합니다. 입력은 자동 저장되며 [이전]을 눌러도 보존됩니다.
      </p>

      {/* 진행률 표시 영역 — 프로그레스 바 + 단계 버튼 탭 */}
      <div className="mt-6">
        <div className="flex items-center justify-between text-xs text-muted-foreground mb-2">
          <span>
            STEP {current} <span className="text-foreground/70">· {step.label}</span>
          </span>
          {/* 퍼센트는 현재 슬라이드 / 전체 슬라이드로 계산 */}
          <span>{Math.round((current / totalSteps) * 100)}%</span>
        </div>
        <Progress value={(current / totalSteps) * 100} className="h-2" />

        {/* 단계 탭 — 이미 방문한 단계는 클릭 가능, 미래 단계는 비활성화 */}
        <ol className="mt-3 flex items-center gap-1.5 overflow-x-auto">
          {steps.map((s) => {
            const done = s.n < current;    // 이미 완료된 단계
            const active = s.n === current; // 현재 활성 단계
            return (
              <li key={s.n}>
                <button
                  type="button"
                  onClick={() => goto(s.n)}
                  disabled={s.n > current} // 아직 방문하지 않은 단계는 비활성화
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

      {/* 슬라이드 스테이지 — 현재 슬라이드 컴포넌트를 CSS 애니메이션으로 전환 */}
      <div className="mt-6 overflow-hidden">
        <div
          key={current} // key 변경으로 React가 새 엘리먼트로 인식 → 애니메이션 재시작
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

      {/* 제출 오류 배너 — 오류 발생 시에만 렌더 */}
      {submitError && (
        <div className="mt-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
          제출 중 오류가 발생했습니다: {submitError}
        </div>
      )}

      {/* 하단 네비게이션 — [이전] / [다음 or 요청서 생성하기] */}
      <div className="mt-6 flex items-center justify-between gap-3">
        {/* [이전] — 첫 슬라이드이거나 제출 중이면 비활성화 */}
        <Button variant="outline" onClick={prev} disabled={current === 1 || submit.isPending}>
          ← 이전
        </Button>

        {/* 마지막 단계가 아니면 [다음], 마지막이면 [요청서 생성하기] 표시 */}
        {!isLast ? (
          <Button onClick={next} disabled={!canProceed}>
            다음 →
          </Button>
        ) : (
          <Button
            onClick={() => {
              setSubmitError(null); // 이전 오류 초기화 후 재시도
              submit.mutate();
            }}
            disabled={submit.isPending}
          >
            {submit.isPending ? "생성 중…" : "요청서 생성하기"}
          </Button>
        )}
      </div>

      {/* 추가 확인 항목 요약 배너 — 항목이 1개 이상일 때 노출 */}
      {(state.data.additionalCheckItems ?? []).length > 0 && (
        <div className="mt-6 rounded-md border border-[color:var(--warning)]/30 bg-[color:var(--warning)]/5 p-4">
          <div className="text-xs font-semibold text-[color:var(--warning)] mb-2">
            ⚠ 추가 확인 필요 항목 ({(state.data.additionalCheckItems ?? []).length})
          </div>
          <ul className="text-xs text-foreground space-y-1">
            {(state.data.additionalCheckItems ?? []).map((it) => (
              <li key={it.field}>
                · (S{it.slide}) {it.field}
                {/* 사유가 있으면 대시로 이어서 표시 */}
                {it.reason ? <span className="text-muted-foreground"> — {it.reason}</span> : null}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
