import { useIntakeForm } from "@/lib/intakeForm";
import { IntakeFormView } from "../intake/IntakeFormView";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

// S7 — 신규 지불수단 등록 공통 포맷.
// 활성 조건: funcTypeCode === NEW && category === pg표준결제창 && subType === 카드 (WizardShell에서 분기)
// 폼 스키마: 백엔드 GET /api/forms/payment-method-intake (docs/policy/payment_method_intake_form_v1.md 기반)
export function Slide7PaymentMethodIntake() {
  const { state, patch } = useWizard();
  const { data: schema, isLoading, isError, error } = useIntakeForm();

  const answers = state.data.paymentMethodIntake ?? {};

  return (
    <SlideShell
      step={7}
      title="신규 지불수단 등록 공통 포맷"
      description="신규 표준결제창·카드 연동에 필요한 정책 33가지를 섹션별로 채워 주세요. 필수(*) 항목 외에는 비워둬도 됩니다."
    >
      {isLoading && (
        <p className="text-xs text-muted-foreground">폼 스키마 불러오는 중…</p>
      )}
      {isError && (
        <p className="text-xs text-destructive">
          폼 스키마 조회 실패: {error instanceof Error ? error.message : String(error)}
        </p>
      )}
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
