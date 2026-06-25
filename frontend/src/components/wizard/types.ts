// 위저드 전반에서 쓰이는 타입 정의.
// 한쪽에서만 임포트하도록 모았다 — 슬라이드 늘리거나 백엔드 DTO가 바뀌면 여기서 시작.

// ─────────────────────────────────────────────────────────────────────
// 백엔드 계약
// ─────────────────────────────────────────────────────────────────────

// 백엔드 GET /api/catalog/ 응답.
// 백엔드는 docs/catalog/catalog_category_tree_v1.yaml 을 ground truth로 사용한다.
export type CategoryTree = {
  funcTypes: FuncType[];
  categories: CategoryNode[];
};

export type FuncType = {
  code: string;          // NEW | MODIFY (yaml의 func_types[].id)
  label: string;
  description?: string;
};

export type CategoryNode = {
  code: string;
  label: string;
  inputMode?: string;    // SELECT | FREE_TEXT
  subTypes: SubType[];
};

export type SubType = {
  code: string;
  label: string;
  paymentMethods?: string[];
  freeText?: boolean;
  specMatchHints?: string[];
  // null/[]면 둘 다 가용, 명시되면 그 funcType에서만 노출 — 위저드 S2 분기에 사용
  availableFuncTypes?: string[];
};

// 백엔드 POST /api/dev-requests/generate 요청 DTO (DevRequestRequest)
// design.md §6 / 백엔드 DevRequestRequest와 1:1 매핑.
export type BackendDevRequest = {
  funcType: string;                // 라벨 ("기존 서비스 수정·개선" 등) — 백엔드는 라벨로 LLM에 전달
  category: string;
  subType: string;
  author: string;
  department: string;
  serviceName: string;
  background: string;
  targetSchedule: string;          // YYYYMMDD or YYYY-MM-DD (백엔드는 문자열로 받음)
  problemAndImprovement: string;
};

// 위저드 내부 분기에 사용하는 funcType 코드 (yaml의 func_types[].id)
export type FuncTypeCode = "NEW" | "MODIFY";

// 백엔드 응답 (design.md §6 generate) — 초기안. 현재 백엔드는 zip 또는 아래 ProjectMdResult JSON 둘 중 하나 반환.
export type GenerateResponse = {
  requestId: string;
  requirementsMarkdown: string;
  projectMeta: unknown;             // ProjectMdResult — /result 화면이 사용
  additionalChecks: unknown[];
  flowImageUrl?: string;
  downloadUrl?: string;
};

// 백엔드 ProjectMdResult — `GET /api/dev-requests/generate` 응답의 devRequest 필드와 1:1.
// 28필드 모두 선택형(LLM이 못 채우면 null/빈문자열)이라 표시 측에서 빈값 처리 필요.
export type ProjectMdResult = {
  // 기본 정보
  author?: string;
  createdDate?: string;
  department?: string;
  projectId?: string;
  // 가맹점 정보
  mid?: string;
  merchantName?: string;
  merchantBusinessNumber?: string;
  // 원천사 정보
  providerName?: string;
  providerCollaborationBackground?: string;
  // 추진 배경 / 목표
  promotionBackground?: string;
  additionalInfo?: string;
  targetDate?: string;
  productName?: string;
  // 문제점 / 서비스 정보
  issueAndImprovement?: string;
  issueVerificationMethod?: string;
  serviceChannelAndPaymentMethod?: string;
  authApprovalAcquirerSubject?: string;
  // 결제 정책
  minimumPaymentAmount?: string;
  partialCancelAndRefundPolicy?: string;
  cashReceiptIssuer?: string;
  // 기대 효과
  expectedRevenue?: string;
  expectedLoss?: string;
  expectedEffect?: string;
  // 이관 / 개발 정보
  transferGuide?: string;
  developmentInProgress?: boolean | null;
  merchantRelatedDevelopment?: boolean | null;
  providerRelatedDevelopment?: boolean | null;
  newServiceOrSelfImprovement?: boolean | null;
  securityAndAuditDevelopment?: boolean | null;

  // 핑퐁 방지 — AI가 추론 못 한 항목을 현업에게 한 번에 묻기 위한 질문 목록
  pendingQuestions?: string[];
  // AI 추론 근거 (가정) — 현업이 명시 안 한 항목을 어떻게 채웠는지 투명 공개
  assumptionList?: string[];
  // 개발 착수 전 완료 필요한 선행 조건
  prerequisiteActions?: string;
  // IT 관점의 구체적 개발 범위 (신규/수정 대상 모듈·API·화면, 영향 범위)
  developmentScope?: string;
  // 예상 복잡도: LOW | MID | HIGH
  estimatedComplexity?: "LOW" | "MID" | "HIGH" | string;
  // 일정/원천사/정책/기술 리스크와 제약
  riskAndConstraints?: string;
};

// GET /api/dev-requests/generate JSON 응답 래퍼.
// markdown은 백엔드가 표준 양식 v1.6으로 렌더해 같이 내려주는 본문 (Gemini 호출은 1회).
export type GenerateJsonResponse = {
  resultCode: string;
  resultMsg: string;
  devRequest: ProjectMdResult;
  markdown?: string;
};

// 위저드 submit 후 백엔드에서 받은 두 산출물 묶음.
export type GenerateResult = {
  result: ProjectMdResult;
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

export type WizardExtraData = {
  createdAt: string;                       // 자동 (YYYY-MM-DD)

  // S1에서 funcType 라벨과 함께 저장하는 코드 — 슬라이드 분기에 사용
  funcTypeCode?: FuncTypeCode;

  // S3 추가 정보
  merchantInfo?: string;                   // 가맹점 정보 (이름/MID 등 자유 텍스트)
  providerInfo?: string;                   // 원천사 정보

  // S4 추가
  scheduleRationale?: string;              // 목표일정 근거

  // S5 추가
  problemDetectionMethod?: string;         // 문제점 확인 방법
  occurrenceFrequency?: string;            // 정량 빈도 (예: "월 12건")
  competitorInfo?: string;                 // 경쟁사 정보
  expectedRevenue?: string;                // 예상 수익
  expectedLoss?: string;                   // 예상 손해

  // S6 동적 분기 답변
  s6: S6Answers;

  // S6 또는 다른 슬라이드의 [잘 모름/추가 확인 필요] 체크 모음
  additionalCheckItems: AdditionalCheckItem[];

  // 파일 첨부 (자리만 — 업로드 미구현, 메타만 보관)
  attachments: AttachmentPlaceholder[];

  // S7 (NEW + 표준결제창 + 카드일 때만) — 신규 지불수단 등록 폼 답변
  // 키: policyId (P-101, P-201, ...) / 값: 문자열·숫자·boolean·배열·중첩객체
  paymentMethodIntake?: Record<string, unknown>;
};

export type S6Answers = {
  // 결제창 카테고리 시
  paymentMethod?: string;                  // 카드/계좌이체/...
  cardOptions?: {
    installments?: string[];               // 예: ["일시불", "3개월", "6개월"]
    usePoint?: boolean;                    // 포인트/머니 사용
    usePromotion?: boolean;                // 프로모션 쿠폰 사용
  };
  // funcType = "기존 서비스 수정·개선" 시
  asisTobe?: AsIsToBe;
  // KB 데이터 없을 때 자유 메모
  freeNotes?: string;
};

export type AsIsToBe = {
  kind: "UI" | "API";
  asis: string;
  tobe: string;
};

export type AdditionalCheckItem = {
  slide: number;                           // 어느 슬라이드에서 체크됐는지
  field: string;                           // 어떤 항목인지 (라벨)
  reason?: string;                         // 사유 (옵션)
};

export type AttachmentPlaceholder = {
  name: string;
  size: number;
};

// ─────────────────────────────────────────────────────────────────────
// 위저드 전역 상태 + 액션
// ─────────────────────────────────────────────────────────────────────

export type WizardData = Partial<BackendDevRequest> & Partial<WizardExtraData>;

export type WizardState = {
  currentSlide: number;                    // 1..TOTAL_SLIDES
  direction: 1 | -1;                       // 슬라이드 방향(애니메이션용). 다음=1, 이전=-1
  data: WizardData;
};

export type WizardAction =
  | { type: "GOTO"; slide: number }
  | { type: "NEXT" }
  | { type: "PREV" }
  | { type: "PATCH"; patch: WizardData }
  | { type: "ADD_CHECK"; item: AdditionalCheckItem }
  | { type: "REMOVE_CHECK"; field: string }
  | { type: "ADD_ATTACHMENT"; file: AttachmentPlaceholder }
  | { type: "REMOVE_ATTACHMENT"; name: string };

// 위저드의 최대 단계. 실제 노출 슬라이드 수는 WizardShell에서 조건부로 계산한다.
export const TOTAL_SLIDES = 7;

/** S7(신규 지불수단 등록 폼)이 활성화되는 조건. */
export function isIntakeSlideActive(data: WizardData): boolean {
  return (
    data.funcTypeCode === "NEW" &&
    (data.category ?? "").includes("pg표준결제창") &&
    (data.subType ?? "").includes("카드")
  );
}
