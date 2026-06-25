import { useQuery } from "@tanstack/react-query";

// 백엔드 IntakeForm DTO와 1:1.
// docs/policy/payment_method_intake_form_v1.md 의 frontmatter가 ground truth.

/**
 * 인테이크 폼 필드의 입력 방식을 나타내는 유니언 타입.
 *
 * - `text`        : 단일 행 문자열 입력.
 * - `number`      : 숫자 입력.
 * - `boolean`     : 체크박스(참/거짓) 입력.
 * - `select`      : 단일 선택 드롭다운.
 * - `multiselect` : 복수 선택 드롭다운 또는 체크박스 그룹.
 * - `group`       : 중첩 필드 그룹. `fields` 배열로 하위 필드를 포함.
 */
export type IntakeInputType =
  | "text"
  | "number"
  | "boolean"
  | "select"
  | "multiselect"
  | "group";

/**
 * 인테이크 폼의 개별 필드를 나타내는 타입.
 *
 * `inputType`이 `"group"`이면 `fields` 배열에 하위 필드가 존재한다.
 * 정책 파일(`payment_method_intake_form_v1.md`)의 frontmatter 구조를 따른다.
 */
export type IntakeField = {
  /**
   * 필드 고유 정책 ID (예: `"PMT_WINDOW_TITLE_KO"`).
   * 사용자 입력값을 저장하는 {@link IntakeAnswers}의 키로 사용된다.
   */
  policyId?: string;
  /** 필드가 속한 섹션 코드. {@link IntakeSection.code}와 대응. */
  section?: string;
  /** 폼 UI에 표시되는 레이블 텍스트. */
  label?: string;
  /** 입력 방식 타입. */
  inputType?: IntakeInputType;
  /** 필수 입력 여부. `true`이면 빈 값 제출 불가. */
  required?: boolean;
  /** `select` / `multiselect` 타입에서 선택 가능한 옵션 목록. */
  options?: string[];
  /** 필드의 기본값. 타입에 따라 문자열·숫자·불리언 가능. */
  defaultValue?: string | number | boolean;
  /** 입력 필드 플레이스홀더 텍스트. */
  placeholder?: string;
  /** 필드 하단에 표시되는 도움말 텍스트. */
  helpText?: string;
  /** 입력값 검증용 정규식 패턴 문자열. */
  pattern?: string;
  /** 입력값 포맷 설명 (예: `"YYYY-MM-DD"`). */
  format?: string;
  /** 숫자 입력 시 단위 표시 문자열 (예: `"원"`, `"개"`). */
  unit?: string;
  /** 문자열 입력 최대 길이. */
  maxLength?: number;
  /**
   * `group` 타입일 때 그룹 내 식별자.
   * 예: `"PAYMENT_WINDOW"`, `"ko"`, `"en"`, `"zh"`.
   */
  key?: string;
  /** `group` 타입일 때 하위 필드 배열. */
  fields?: IntakeField[];
  /** 이 필드의 원천 지식저장소 문서 ID (참조 문서 연결용). */
  sourceDocId?: string;
};

/**
 * 인테이크 폼의 섹션(단계) 정보를 나타내는 타입.
 *
 * 위저드 UI에서 각 단계의 탭 또는 스텝 표시에 사용된다.
 */
export type IntakeSection = {
  /** 섹션 고유 코드 (예: `"BASIC"`, `"PAYMENT_WINDOW"`). */
  code: string;
  /** 섹션 표시 이름. */
  name: string;
  /** 위저드 단계 순서 (1부터 시작). */
  order: number;
};

/**
 * 인테이크 폼 전체 구조를 나타내는 타입.
 *
 * 백엔드 `IntakeForm` DTO와 1:1로 대응한다.
 * `sections`로 위저드 단계를 구성하고, `fields`로 각 단계의 입력 필드를 구성한다.
 */
export type IntakeForm = {
  /** 폼의 원본 정책 문서 ID. */
  docId?: string;
  /** 폼 제목 (예: "결제수단 신규 연동 요건 정의서"). */
  title?: string;
  /** 폼 버전 문자열 (예: `"v1.0"`). */
  version?: string;
  /** 위저드 단계 목록. `order` 기준으로 정렬된다. */
  sections: IntakeSection[];
  /** 모든 섹션에 걸친 입력 필드 목록. `section` 코드로 섹션에 매핑된다. */
  fields: IntakeField[];
};

/**
 * 사용자가 인테이크 폼에 입력한 값들의 집합 타입.
 *
 * 키는 `IntakeField.policyId`, 값은 입력 타입에 따라 달라진다.
 * - 단일값: `string | number | boolean`
 * - `multiselect`: `string[]`
 * - `group`: 중첩된 `Record<string, unknown>` 객체
 */
export type IntakeAnswers = Record<string, unknown>;

/**
 * 결제수단 신규 연동 인테이크 폼 데이터를 조회하는 React Query 훅.
 *
 * 폼 정의는 백엔드에서 정책 파일(`payment_method_intake_form_v1.md`)을 파싱하여 반환한다.
 * `staleTime`을 5분으로 설정하여 폼 구조가 자주 변경되지 않음을 반영한다.
 *
 * @returns `useQuery` 결과 객체. `data`는 {@link IntakeForm} 타입.
 *
 * @example
 * ```tsx
 * const { data: form, isLoading } = useIntakeForm();
 * const sections = form?.sections ?? [];
 * ```
 */
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
    // 폼 구조는 자주 바뀌지 않으므로 5분 캐시 유지
    staleTime: 5 * 60 * 1000,
  });
}
