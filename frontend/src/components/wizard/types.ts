// 위저드 전반에서 쓰이는 타입 정의.
// 한쪽에서만 임포트하도록 모았다 — 슬라이드 늘리거나 백엔드 DTO가 바뀌면 여기서 시작.

// ─────────────────────────────────────────────────────────────────────
// 백엔드 계약
// ─────────────────────────────────────────────────────────────────────

// 백엔드 GET /api/catalog/ 응답
export type CategoryTree = {
  funcTypes: FuncType[];
  categories: CategoryNode[];
};

export type FuncType = { code: string; label: string };
export type CategoryNode = { code: string; label: string; subTypes: SubType[] };
export type SubType = { code: string; label: string };

// 백엔드 POST /api/dev-requests/generate 요청 DTO (DevRequestRequest)
// design.md §6 / 백엔드 DevRequestRequest와 1:1 매핑.
export type BackendDevRequest = {
  funcType: string;
  category: string;
  subType: string;
  author: string;
  department: string;
  serviceName: string;
  background: string;
  targetSchedule: string;          // YYYYMMDD or YYYY-MM-DD (백엔드는 문자열로 받음)
  problemAndImprovement: string;
};

// 백엔드 응답 (design.md §6 generate)
export type GenerateResponse = {
  requestId: string;
  requirementsMarkdown: string;
  projectMeta: unknown;             // ProjectMdResult — /result 화면이 사용
  additionalChecks: unknown[];
  flowImageUrl?: string;
  downloadUrl?: string;
};

// ─────────────────────────────────────────────────────────────────────
// 위저드 추가 수집 데이터 (백엔드 DTO에 직접 들어가지 않는 것들)
// 현 백엔드 DTO 확장 전까지는 클라이언트에서만 유지되며,
// submit 시 problemAndImprovement 등에 자연어로 합쳐 전달한다.
// ─────────────────────────────────────────────────────────────────────

export type WizardExtraData = {
  createdAt: string;                       // 자동 (YYYY-MM-DD)

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

export const TOTAL_SLIDES = 6;
