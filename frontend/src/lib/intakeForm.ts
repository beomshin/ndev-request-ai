import { useQuery } from "@tanstack/react-query";

// 백엔드 IntakeForm DTO와 1:1.
// docs/policy/payment_method_intake_form_v1.md 의 frontmatter가 ground truth.

export type IntakeInputType =
  | "text"
  | "number"
  | "boolean"
  | "select"
  | "multiselect"
  | "group";

export type IntakeField = {
  policyId?: string;
  section?: string;
  label?: string;
  inputType?: IntakeInputType;
  required?: boolean;
  options?: string[];
  defaultValue?: string | number | boolean;
  placeholder?: string;
  helpText?: string;
  pattern?: string;
  format?: string;
  unit?: string;
  maxLength?: number;
  // group 일 때만
  key?: string;          // 그룹 내 식별자 (예: PAYMENT_WINDOW, ko/en/zh)
  fields?: IntakeField[];
  sourceDocId?: string;
};

export type IntakeSection = {
  code: string;
  name: string;
  order: number;
};

export type IntakeForm = {
  docId?: string;
  title?: string;
  version?: string;
  sections: IntakeSection[];
  fields: IntakeField[];
};

// 사용자가 폼에 입력한 값. policyId → 값(단일/배열/group은 중첩 객체)
export type IntakeAnswers = Record<string, unknown>;

export function useIntakeForm() {
  return useQuery<IntakeForm>({
    queryKey: ["intakeForm", "paymentMethod"],
    queryFn: async () => {
      const res = await fetch("/api/forms/payment-method-intake", {
        headers: { Accept: "application/json" },
      });
      if (!res.ok) throw new Error(`IntakeForm 조회 실패 ${res.status}`);
      return (await res.json()) as IntakeForm;
    },
    staleTime: 5 * 60 * 1000,
  });
}
