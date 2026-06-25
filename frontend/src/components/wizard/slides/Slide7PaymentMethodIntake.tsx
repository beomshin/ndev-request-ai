import { useIntakeForm } from "@/lib/intakeForm";
import { IntakeFormView } from "../intake/IntakeFormView";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

/**
 * @file Slide7PaymentMethodIntake.tsx
 * @description 위저드 7단계 — 신규 지불수단 등록 공통 포맷 슬라이드.
 *
 * 신규 표준결제창·카드 연동에 필요한 정책 항목(33가지)을 섹션별로 수집합니다.
 *
 * @remarks
 * ### 활성 조건
 * 이 슬라이드는 다음 세 조건이 모두 충족될 때만 `WizardShell`에서 분기되어 표시됩니다.
 * - `funcTypeCode === "NEW"` (신규 개발)
 * - `category === "pg표준결제창"` (PG 표준결제창 카테고리)
 * - `subType === "카드"` (카드 세부유형)
 *
 * ### 폼 스키마 출처
 * - 백엔드 `GET /api/forms/payment-method-intake` 엔드포인트에서 JSON 스키마를 가져옵니다.
 * - 스키마 정의 기준 문서: `docs/policy/payment_method_intake_form_v1.md`
 * - `useIntakeForm` 훅이 React Query를 통해 스키마를 캐시·관리합니다.
 *
 * ### 데이터 저장
 * 사용자가 입력한 각 필드 답변은 `state.data.paymentMethodIntake` 객체에 저장됩니다.
 * 키는 스키마 필드 ID, 값은 사용자 입력 문자열입니다.
 *
 * ### 필수/선택
 * 스키마의 `required: true` 항목만 필수(`*`) 표시됩니다.
 * 나머지 항목은 비워둔 채 제출해도 됩니다.
 */

/**
 * 위저드 7단계 컴포넌트 — 신규 지불수단 등록을 위한 공통 정책 폼을 표시합니다.
 *
 * @returns 백엔드 스키마 기반의 신규 지불수단 등록 정책 입력 폼을 포함한 슬라이드 UI.
 *
 * @example
 * ```tsx
 * <Slide7PaymentMethodIntake />
 * ```
 */
// S7 — 신규 지불수단 등록 공통 포맷.
// 활성 조건: funcTypeCode === NEW && category === pg표준결제창 && subType === 카드 (WizardShell에서 분기)
// 폼 스키마: 백엔드 GET /api/forms/payment-method-intake (docs/policy/payment_method_intake_form_v1.md 기반)
export function Slide7PaymentMethodIntake() {
  /** 위저드 전역 상태 및 패치 함수 */
  const { state, patch } = useWizard();
  /**
   * 백엔드에서 가져온 신규 지불수단 등록 폼 스키마.
   * `isLoading`: 스키마 로딩 중 여부.
   * `isError`: 스키마 로딩 실패 여부.
   * `error`: 실패 시 Error 객체 (오류 메시지 표시에 사용).
   */
  const { data: schema, isLoading, isError, error } = useIntakeForm();

  /**
   * 현재까지 입력된 폼 답변 객체.
   * `paymentMethodIntake`가 undefined이면 빈 객체(`{}`)로 초기화합니다.
   * `IntakeFormView`에 전달되어 각 필드의 현재 값을 표시합니다.
   */
  const answers = state.data.paymentMethodIntake ?? {};

  return (
    <SlideShell
      step={7}
      title="신규 지불수단 등록 공통 포맷"
      description="신규 표준결제창·카드 연동에 필요한 정책 33가지를 섹션별로 채워 주세요. 필수(*) 항목 외에는 비워둬도 됩니다."
    >
      {/* 폼 스키마 로딩 중 안내 메시지 */}
      {isLoading && (
        <p className="text-xs text-muted-foreground">폼 스키마 불러오는 중…</p>
      )}
      {/* 폼 스키마 로딩 실패 시 오류 메시지 — Error 인스턴스이면 message를, 아니면 문자열로 변환하여 표시 */}
      {isError && (
        <p className="text-xs text-destructive">
          폼 스키마 조회 실패: {error instanceof Error ? error.message : String(error)}
        </p>
      )}
      {/* 스키마가 정상적으로 로드되었을 때 동적 폼을 렌더링합니다.
          onChange 콜백은 답변이 변경될 때마다 위저드 상태를 업데이트합니다. */}
      {schema && (
        <IntakeFormView
          schema={schema}
          answers={answers}
          onChange={(next) => patch({ paymentMethodIntake: next })}
        />
      )}
    </SlideShell>
  );
}
