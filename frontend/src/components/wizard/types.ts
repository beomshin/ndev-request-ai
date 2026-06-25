/**
 * @file types.ts
 * @description 위저드 전반에서 쓰이는 타입 정의 모음.
 * 슬라이드를 추가하거나 백엔드 DTO가 변경될 때 이 파일에서 시작한다.
 */

// ─────────────────────────────────────────────────────────────────────
// 백엔드 계약
// ─────────────────────────────────────────────────────────────────────

/**
 * 백엔드 GET /api/catalog/ 응답 최상위 구조.
 * 백엔드는 `docs/catalog/catalog_category_tree_v1.yaml` 을 ground truth로 사용한다.
 */
export type CategoryTree = {
  /** 기능 유형 목록 (신규/수정 등) */
  funcTypes: FuncType[];
  /** 대분류 카테고리 목록 */
  categories: CategoryNode[];
};

/**
 * 카탈로그에서 정의하는 기능 유형(funcType) 한 건.
 * yaml의 `func_types[].id` 에 대응한다.
 */
export type FuncType = {
  /** yaml의 func_types[].id (예: "NEW", "MODIFY") */
  code: string;
  /** 화면 표시용 라벨 */
  label: string;
  /** 사용자에게 보여줄 부가 설명 (선택) */
  description?: string;
};

/**
 * 카탈로그의 대분류 카테고리 한 건.
 * 하위에 `SubType[]` 을 포함한다.
 */
export type CategoryNode = {
  /** 카테고리 식별 코드 */
  code: string;
  /** 화면 표시용 라벨 */
  label: string;
  /**
   * 하위 입력 방식 힌트.
   * - `"SELECT"` : 드롭다운/칩 선택
   * - `"FREE_TEXT"` : 자유 입력
   */
  inputMode?: string;
  /** 이 카테고리에 속하는 소분류 목록 */
  subTypes: SubType[];
};

/**
 * 카테고리 하위의 소분류(subType) 한 건.
 * 위저드 S2 분기 로직과 S7(신규 지불수단) 활성화 판단에 사용된다.
 */
export type SubType = {
  /** 소분류 식별 코드 */
  code: string;
  /** 화면 표시용 라벨 */
  label: string;
  /** 이 소분류에서 지원하는 지불수단 목록 (예: ["카드", "계좌이체"]) */
  paymentMethods?: string[];
  /** `true`이면 자유 입력 텍스트 보조 필드를 노출 */
  freeText?: boolean;
  /** KB 검색 시 스펙 매칭에 활용할 힌트 키워드 목록 */
  specMatchHints?: string[];
  /**
   * 이 소분류를 노출할 funcType 코드 목록.
   * `null` 또는 빈 배열이면 모든 funcType에서 노출.
   * 명시 시 해당 funcType에서만 노출 — 위저드 S2 분기에 사용.
   */
  availableFuncTypes?: string[];
};

/**
 * 백엔드 POST /api/dev-requests/generate 요청 DTO (`DevRequestRequest`).
 * `design.md §6` / 백엔드 `DevRequestRequest` 와 1:1 매핑.
 */
export type BackendDevRequest = {
  /** 기능 유형 라벨 (예: "기존 서비스 수정·개선") — 백엔드가 그대로 LLM에 전달 */
  funcType: string;
  /** 대분류 카테고리 라벨 */
  category: string;
  /** 소분류 라벨 */
  subType: string;
  /** 요청자 이름 */
  author: string;
  /** 요청 부서명 */
  department: string;
  /** 서비스(프로젝트) 명칭 */
  serviceName: string;
  /** 추진 배경 설명 */
  background: string;
  /**
   * 목표 일정.
   * 백엔드는 문자열로 받으며 `YYYYMMDD` 또는 `YYYY-MM-DD` 형식 모두 허용.
   */
  targetSchedule: string;
  /** 문제점 및 개선점 본문. 백엔드 `@NotBlank` 조건이므로 빈값 불가. */
  problemAndImprovement: string;
};

/**
 * 위저드 내부 분기에 사용하는 funcType 식별 코드.
 * - `"NEW"` : 신규 서비스 개발
 * - `"MODIFY"` : 기존 서비스 수정·개선
 */
export type FuncTypeCode = "NEW" | "MODIFY";

/**
 * 백엔드 응답 초기안(`design.md §6 generate`).
 * 현재 백엔드는 zip 또는 아래 `ProjectMdResult` JSON 둘 중 하나를 반환한다.
 */
export type GenerateResponse = {
  /** 요청서 식별 ID */
  requestId: string;
  /** 요건 정의서 마크다운 본문 */
  requirementsMarkdown: string;
  /** ProjectMdResult — /result 화면이 사용 */
  projectMeta: unknown;
  /** 추가 확인 항목 목록 */
  additionalChecks: unknown[];
  /** 플로우 이미지 URL (선택) */
  flowImageUrl?: string;
  /** 파일 다운로드 URL (선택) */
  downloadUrl?: string;
};

/**
 * 백엔드 `ProjectMdResult`.
 * `GET /api/dev-requests/generate` 응답의 `devRequest` 필드와 1:1 대응.
 *
 * @remarks
 * 28개 필드 모두 선택형이다. LLM이 채우지 못한 항목은 `null` 또는 빈 문자열로 내려오므로
 * 표시 측에서 빈값 처리가 필요하다.
 */
export type ProjectMdResult = {
  // ── 기본 정보 ──────────────────────────────────────────────────────
  /** 요청자 이름 */
  author?: string;
  /** 요청서 생성 일자 (YYYY-MM-DD) */
  createdDate?: string;
  /** 요청 부서명 */
  department?: string;
  /** 프로젝트 식별 ID */
  projectId?: string;

  // ── 가맹점 정보 ────────────────────────────────────────────────────
  /** 가맹점 MID */
  mid?: string;
  /** 가맹점 이름 */
  merchantName?: string;
  /** 가맹점 사업자번호 */
  merchantBusinessNumber?: string;

  // ── 원천사 정보 ────────────────────────────────────────────────────
  /** 원천사(카드사·PG 등) 이름 */
  providerName?: string;
  /** 원천사 협업 배경 설명 */
  providerCollaborationBackground?: string;

  // ── 추진 배경 / 목표 ──────────────────────────────────────────────
  /** 프로젝트 추진 배경 */
  promotionBackground?: string;
  /** 추가 정보 */
  additionalInfo?: string;
  /** 목표 일정 (텍스트 형식) */
  targetDate?: string;
  /** 상품·서비스 명칭 */
  productName?: string;

  // ── 문제점 / 서비스 정보 ──────────────────────────────────────────
  /** 문제점 및 개선점 */
  issueAndImprovement?: string;
  /** 이슈 확인 방법 */
  issueVerificationMethod?: string;
  /** 서비스 채널 및 지불수단 */
  serviceChannelAndPaymentMethod?: string;
  /** 인증·승인·매입 주체 */
  authApprovalAcquirerSubject?: string;

  // ── 결제 정책 ─────────────────────────────────────────────────────
  /** 최소 결제 금액 */
  minimumPaymentAmount?: string;
  /** 부분 취소 및 환불 정책 */
  partialCancelAndRefundPolicy?: string;
  /** 현금영수증 발급 주체 */
  cashReceiptIssuer?: string;

  // ── 기대 효과 ─────────────────────────────────────────────────────
  /** 예상 수익 */
  expectedRevenue?: string;
  /** 예상 손해(비용) */
  expectedLoss?: string;
  /** 전반적인 기대 효과 설명 */
  expectedEffect?: string;

  // ── 이관 / 개발 정보 ──────────────────────────────────────────────
  /** 이관 가이드 */
  transferGuide?: string;
  /** 개발 진행 중 여부 */
  developmentInProgress?: boolean | null;
  /** 가맹점 관련 개발 포함 여부 */
  merchantRelatedDevelopment?: boolean | null;
  /** 원천사 관련 개발 포함 여부 */
  providerRelatedDevelopment?: boolean | null;
  /** 신규 서비스 또는 자체 개선 여부 */
  newServiceOrSelfImprovement?: boolean | null;
  /** 보안·심사 관련 개발 포함 여부 */
  securityAndAuditDevelopment?: boolean | null;

  /**
   * 핑퐁 방지용 질문 목록.
   * AI가 추론하지 못한 항목을 현업에게 한 번에 묻기 위해 사용한다.
   */
  pendingQuestions?: string[];
  /**
   * AI 추론 근거(가정) 목록.
   * 현업이 명시하지 않은 항목을 AI가 어떻게 채웠는지 투명하게 공개한다.
   */
  assumptionList?: string[];
  /** 개발 착수 전 완료 필요한 선행 조건 */
  prerequisiteActions?: string;
  /** IT 관점의 구체적 개발 범위 (신규/수정 대상 모듈·API·화면, 영향 범위) */
  developmentScope?: string;
  /**
   * 예상 복잡도.
   * - `"LOW"` : 낮음
   * - `"MID"` : 중간
   * - `"HIGH"` : 높음
   */
  estimatedComplexity?: "LOW" | "MID" | "HIGH" | string;
  /** 일정·원천사·정책·기술 관련 리스크 및 제약 사항 */
  riskAndConstraints?: string;
};

/**
 * GET /api/dev-requests/generate JSON 응답 래퍼.
 * `markdown`은 백엔드가 표준 양식 v1.6으로 렌더해 함께 내려주는 본문이며,
 * Gemini 호출은 백엔드에서 1회만 수행된다.
 */
export type GenerateJsonResponse = {
  /** 처리 결과 코드 */
  resultCode: string;
  /** 처리 결과 메시지 */
  resultMsg: string;
  /** LLM이 생성한 프로젝트 메타 결과 */
  devRequest: ProjectMdResult;
  /** 표준 양식으로 렌더된 마크다운 본문 (선택) */
  markdown?: string;
};

/**
 * 위저드 제출 후 백엔드에서 받은 두 산출물의 묶음.
 * `fetchDevRequestJson` 의 반환 타입으로 사용된다.
 */
export type GenerateResult = {
  /** LLM이 채운 프로젝트 메타 결과 */
  result: ProjectMdResult;
  /** 표준 양식 마크다운 본문 */
  markdown: string;
};

// (참고) 이전엔 /result 임시 화면을 위해 React Query cache로 결과를 들고 있었지만,
// 위저드 제출 후 곧장 /result/{id} DB 상세로 이동하는 흐름으로 단순화되며 제거됨.
// 화면이 다시 cache 기반이 되면 LatestResultEntry/LATEST_RESULT_QUERY_KEY를 부활.

// ─────────────────────────────────────────────────────────────────────
// 위저드 추가 수집 데이터 (백엔드 DTO에 직접 들어가지 않는 것들)
// 현 백엔드 DTO 확장 전까지는 클라이언트에서만 유지되며,
// submit 시 problemAndImprovement 등에 자연어로 합쳐 전달한다.
// ─────────────────────────────────────────────────────────────────────

/**
 * 위저드가 추가로 수집하는 데이터 — 백엔드 DTO에 직접 매핑되지 않는 필드들.
 * 제출 시 `composeProblemAndImprovement` 에서 자연어 텍스트로 합쳐 전달된다.
 */
export type WizardExtraData = {
  /** 요청서 생성 일자 (자동 설정, YYYY-MM-DD) */
  createdAt: string;

  /**
   * S1에서 funcType 라벨과 함께 저장되는 내부 코드.
   * 슬라이드 분기(S5 문제점 필드 숨김, S7 활성화 등)에 사용된다.
   */
  funcTypeCode?: FuncTypeCode;

  // ── S3 추가 정보 ──────────────────────────────────────────────────
  /** 가맹점 정보 (이름/MID 등 자유 텍스트) */
  merchantInfo?: string;
  /** 원천사 정보 (카드사·PG 등 자유 텍스트) */
  providerInfo?: string;

  // ── S4 추가 ───────────────────────────────────────────────────────
  /** 목표 일정 근거 (예: "원천사 계약 만료일 기준") */
  scheduleRationale?: string;

  // ── S5 추가 ───────────────────────────────────────────────────────
  /** 문제점 확인 방법 (예: "모니터링 알람 기준") */
  problemDetectionMethod?: string;
  /** 문제 발생 빈도 정량 기술 (예: "월 12건") */
  occurrenceFrequency?: string;
  /** 경쟁사 현황 정보 */
  competitorInfo?: string;
  /** 예상 수익 금액 또는 설명 */
  expectedRevenue?: string;
  /** 예상 손해(비용) 금액 또는 설명 */
  expectedLoss?: string;

  // ── S6 동적 분기 답변 ─────────────────────────────────────────────
  /** Slide 6의 동적 분기 답변 묶음 */
  s6: S6Answers;

  /**
   * S6 또는 다른 슬라이드에서 [잘 모름/추가 확인 필요]로 체크된 항목 목록.
   * 제출 시 `composeChecksSection` 을 통해 본문에 포함된다.
   */
  additionalCheckItems: AdditionalCheckItem[];

  /**
   * 파일 첨부 플레이스홀더 목록.
   * Phase 0에서는 실제 업로드 없이 파일 메타데이터만 보관한다.
   */
  attachments: AttachmentPlaceholder[];

  /**
   * S7 신규 지불수단 등록 폼 답변 (NEW + 표준결제창 + 카드일 때만 활성화).
   * 키: `policyId` (예: "P-101", "P-201") / 값: 문자열·숫자·boolean·배열·중첩객체
   */
  paymentMethodIntake?: Record<string, unknown>;
};

/**
 * Slide 6 심층 질의 응답 묶음.
 * 선택된 카테고리 및 funcType에 따라 노출되는 필드가 달라진다.
 */
export type S6Answers = {
  // ── 결제창 카테고리일 때 노출 ─────────────────────────────────────
  /** 지불수단 (예: "카드", "계좌이체") */
  paymentMethod?: string;
  /** 카드 세부 옵션 (지불수단이 "카드"일 때) */
  cardOptions?: {
    /** 선택한 할부 옵션 목록 (예: ["일시불", "3개월", "6개월"]) */
    installments?: string[];
    /** 포인트·머니 사용 여부 */
    usePoint?: boolean;
    /** 프로모션 쿠폰 사용 여부 */
    usePromotion?: boolean;
  };

  // ── funcType = "기존 서비스 수정·개선"일 때 노출 ─────────────────
  /** AS-IS / TO-BE 변경 내용 */
  asisTobe?: AsIsToBe;

  /** KB 데이터가 없을 때 사용자가 직접 입력하는 자유 메모 */
  freeNotes?: string;
};

/**
 * AS-IS / TO-BE 변경 내용 구조체.
 * `funcType = "MODIFY"` 인 경우 S6에서 수집한다.
 */
export type AsIsToBe = {
  /** 변경 대상 종류 — UI 화면 변경 또는 API 변경 */
  kind: "UI" | "API";
  /** 변경 전 현재 상태 설명 */
  asis: string;
  /** 변경 후 목표 상태 설명 */
  tobe: string;
};

/**
 * [잘 모름/추가 확인 필요] 체크 항목 한 건.
 * 어느 슬라이드의 어떤 필드에서 체크됐는지와 선택적 사유를 보관한다.
 */
export type AdditionalCheckItem = {
  /** 체크가 발생한 슬라이드 번호 (1-based) */
  slide: number;
  /** 체크된 항목의 라벨 (표시용) */
  field: string;
  /** 불확실한 이유에 대한 짧은 메모 (선택) */
  reason?: string;
};

/**
 * 파일 첨부 플레이스홀더.
 * Phase 0에서는 실제 업로드 없이 파일 이름과 크기만 저장한다.
 */
export type AttachmentPlaceholder = {
  /** 파일 이름 (중복 제거 키로도 사용) */
  name: string;
  /** 파일 크기 (바이트 단위) */
  size: number;
};

// ─────────────────────────────────────────────────────────────────────
// 위저드 전역 상태 + 액션
// ─────────────────────────────────────────────────────────────────────

/**
 * 위저드 전역 데이터 타입.
 * 백엔드 DTO(`BackendDevRequest`)와 위저드 전용 데이터(`WizardExtraData`) 모두를
 * Partial로 허용하여 단계별로 점진적으로 채울 수 있다.
 */
export type WizardData = Partial<BackendDevRequest> & Partial<WizardExtraData>;

/**
 * 위저드의 전역 상태 구조체.
 * `useReducer` 의 state 타입으로 사용된다.
 */
export type WizardState = {
  /** 현재 활성 슬라이드 번호 (1..TOTAL_SLIDES) */
  currentSlide: number;
  /**
   * 슬라이드 전환 방향 (CSS 애니메이션 용도).
   * - `1` : 다음(오른쪽 → 왼쪽)
   * - `-1` : 이전(왼쪽 → 오른쪽)
   */
  direction: 1 | -1;
  /** 현재까지 수집된 위저드 데이터 */
  data: WizardData;
};

/**
 * 위저드 상태 머신의 액션 유니언 타입.
 * `WizardContext.tsx` 의 reducer 함수에서 처리된다.
 */
export type WizardAction =
  /** 특정 슬라이드로 직접 이동 */
  | { type: "GOTO"; slide: number }
  /** 다음 슬라이드로 이동 */
  | { type: "NEXT" }
  /** 이전 슬라이드로 이동 */
  | { type: "PREV" }
  /** 위저드 데이터를 부분 병합(patch) */
  | { type: "PATCH"; patch: WizardData }
  /** 추가 확인 항목 추가 */
  | { type: "ADD_CHECK"; item: AdditionalCheckItem }
  /** 특정 필드의 추가 확인 항목 제거 */
  | { type: "REMOVE_CHECK"; field: string }
  /** 파일 첨부 플레이스홀더 추가 */
  | { type: "ADD_ATTACHMENT"; file: AttachmentPlaceholder }
  /** 특정 이름의 첨부 파일 제거 */
  | { type: "REMOVE_ATTACHMENT"; name: string };

/**
 * 위저드의 최대 슬라이드 수.
 * 실제 노출 슬라이드 수는 `WizardShell` 에서 데이터 조건에 따라 동적으로 계산된다.
 */
export const TOTAL_SLIDES = 7;

/**
 * S7(신규 지불수단 등록 폼)이 활성화되는 조건을 판별한다.
 *
 * @param data - 현재 위저드 데이터
 * @returns S7을 노출해야 하면 `true`
 *
 * @remarks
 * 세 조건이 모두 충족되어야 한다:
 * 1. funcTypeCode === "NEW" (신규 개발)
 * 2. category에 "pg표준결제창" 포함
 * 3. subType에 "카드" 포함
 */
export function isIntakeSlideActive(data: WizardData): boolean {
  return (
    data.funcTypeCode === "NEW" &&
    (data.category ?? "").includes("pg표준결제창") &&
    (data.subType ?? "").includes("카드")
  );
}
