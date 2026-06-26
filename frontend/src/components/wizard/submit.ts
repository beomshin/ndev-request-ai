/**
 * @file submit.ts
 * @description 위저드 제출 흐름에 관련된 모든 API 함수와 데이터 변환 유틸리티 모음.
 *
 * 주요 책임:
 * - 위저드 내부 데이터(`WizardData`)를 백엔드 DTO(`BackendDevRequest`)로 변환
 * - 백엔드 DTO에 자리가 없는 위저드 전용 필드들을 `problemAndImprovement` 본문에 합성
 * - GET `/api/dev-requests/generate` 를 통해 LLM 생성 결과 수신
 * - 생성 결과를 DB 저장 페이로드로 변환
 * - (레거시) ZIP 다운로드 및 POST 제출 함수
 *
 * @remarks
 * 백엔드 `DevRequestRequest` 는 9개 필드(funcType, category, subType, author,
 * department, serviceName, background, targetSchedule, problemAndImprovement)만 수신한다.
 * 위저드는 그보다 많은 데이터를 수집하므로, 일부 필드는 `problemAndImprovement` 본문에
 * 섹션([문제점/개선점], [기대효과], [심층 질의 응답] 등)으로 합쳐 전달한다.
 * 백엔드 DTO가 확장되면 매핑 함수를 정리하는 것을 권장한다.
 */

import type {
  AdditionalCheckItem,
  BackendDevRequest,
  GenerateJsonResponse,
  GenerateResponse,
  GenerateResult,
  ProjectMdResult,
  WizardData,
} from "./types";

/** 개발요청서 생성 API 엔드포인트 경로 */
const GENERATE_URL = "/api/dev-requests/generate";

// 백엔드 DevRequestRequest는 6필드 + funcType/category/subType 총 9필드만 받는다.
// 위저드는 더 많이 모으므로(목표일정 근거, 빈도, 경쟁사, S6 답변 등) 일부는
// problemAndImprovement 본문에 자연스럽게 합쳐 보낸다. 백엔드 DTO 확장 시 이 매핑 정리 권장.
// 도메인 매핑(가맹점/원천사 ID 등)은 추측하지 않는다 — 사용자가 입력한 텍스트를 그대로 전달.

/**
 * 위저드 데이터를 백엔드 DTO(`BackendDevRequest`)로 변환한다.
 *
 * @remarks
 * - `subType`이 비어 있으면 백엔드 `@NotBlank` 조건을 통과시키기 위해 `"(없음)"` 으로 채운다.
 * - `targetSchedule`은 `YYYYMMDD` 또는 `YYYY-MM-DD` 둘 다 허용해 정규화 후 전송한다.
 * - `problemAndImprovement`는 백엔드 `@NotBlank` 이므로 빈값이면 NEW 유형용 플레이스홀더로 채운다.
 *
 * @param data - 현재 위저드 데이터
 * @returns 백엔드 DTO 객체
 */
export function toBackendPayload(data: WizardData): BackendDevRequest {
  return {
    funcType: data.funcType ?? "",
    category: data.category ?? "",
    // subType이 없으면 백엔드 @NotBlank 실패 방지를 위해 "(없음)" 삽입
    subType: data.subType && data.subType.length > 0 ? data.subType : "(없음)",
    author: data.author ?? "",
    department: data.department ?? "",
    serviceName: data.serviceName ?? "",
    background: data.background ?? "",
    // 날짜 포맷 정규화 (YYYYMMDD → YYYY-MM-DD)
    targetSchedule: normalizeDate(data.targetSchedule ?? ""),
    // 백엔드 @NotBlank이라 빈값 못 보냄. NEW에선 S5 필드를 숨겼으므로 placeholder 보충.
    problemAndImprovement:
      composeProblemAndImprovement(data) ||
      (data.funcTypeCode === "NEW" ? "신규 서비스 개발 — 별도 문제점 없음" : ""),
  };
}

/**
 * 날짜 문자열을 `YYYY-MM-DD` 형식으로 정규화한다.
 *
 * @remarks
 * 사용자가 `YYYYMMDD` (8자리 숫자) 형태로 입력하면 하이픈을 삽입해 반환한다.
 * 이미 `YYYY-MM-DD` 형태거나 그 외 형식이면 원본 그대로 반환한다.
 *
 * @param raw - 사용자가 입력한 원본 날짜 문자열
 * @returns 정규화된 날짜 문자열
 */
// "YYYYMMDD" 또는 "YYYY-MM-DD" 둘 다 허용해 백엔드로는 YYYY-MM-DD로 보낸다.
function normalizeDate(raw: string): string {
  // 숫자만 추출해 8자리면 YYYYMMDD 형식으로 간주
  const onlyDigits = raw.replace(/[^0-9]/g, "");
  if (onlyDigits.length === 8) {
    return `${onlyDigits.slice(0, 4)}-${onlyDigits.slice(4, 6)}-${onlyDigits.slice(6, 8)}`;
  }
  return raw;
}

/**
 * 백엔드 DTO에 자리 없는 위저드 전용 필드들을 하나의 `problemAndImprovement` 문자열로 합성한다.
 *
 * @remarks
 * 백엔드가 이 텍스트를 LLM에게 그대로 전달하기 때문에, 섹션 헤더(`[문제점/개선점]` 등)를
 * 명시하면 LLM이 해당 항목을 구분해 해석할 수 있다.
 *
 * 포함 섹션:
 * - `[문제점/개선점]` — S5 핵심 필드
 * - `[문제 확인 방법]` — 문제 감지 방법
 * - `[발생 빈도]` — 정량 빈도
 * - `[경쟁사 정보]` — 경쟁사 현황
 * - `[기대효과]` — 예상 수익/손해
 * - `[목표일정 근거]` — 일정 근거
 * - `[가맹점 정보]` — 가맹점 텍스트
 * - `[원천사 정보]` — 원천사 텍스트
 * - `[심층 질의 응답]` — S6 동적 답변
 * - `[추가 확인 필요 항목]` — 위저드 체크 항목
 *
 * @param data - 현재 위저드 데이터
 * @returns 합성된 본문 문자열 (섹션 구분자 `\n\n` 사용)
 */
// 백엔드 DTO에 자리 없는 필드들을 problemAndImprovement에 섹션으로 합친다.
// 백엔드가 LLM에게 그대로 전달하기 때문에, 본문 안에서 항목별로 명시되면 모델이 해석한다.
function composeProblemAndImprovement(data: WizardData): string {
  const parts: string[] = [];

  // S5 핵심 필드 — 문제점 및 개선점 본문
  if (data.problemAndImprovement?.trim()) {
    parts.push(`[문제점/개선점]\n${data.problemAndImprovement.trim()}`);
  }
  // 문제 확인 방법 (예: 모니터링 알람 기준)
  if (data.problemDetectionMethod?.trim()) {
    parts.push(`[문제 확인 방법]\n${data.problemDetectionMethod.trim()}`);
  }
  // 발생 빈도 정량 기술 (예: 월 12건)
  if (data.occurrenceFrequency?.trim()) {
    parts.push(`[발생 빈도]\n${data.occurrenceFrequency.trim()}`);
  }
  // 경쟁사 현황
  if (data.competitorInfo?.trim()) {
    parts.push(`[경쟁사 정보]\n${data.competitorInfo.trim()}`);
  }
  // 기대효과 — 수익/손해가 하나라도 있으면 섹션 생성
  if (data.expectedRevenue?.trim() || data.expectedLoss?.trim()) {
    const lines: string[] = [];
    if (data.expectedRevenue?.trim()) lines.push(`- 예상 수익: ${data.expectedRevenue.trim()}`);
    if (data.expectedLoss?.trim()) lines.push(`- 예상 손해: ${data.expectedLoss.trim()}`);
    parts.push(`[기대효과]\n${lines.join("\n")}`);
  }
  // 목표 일정 근거
  if (data.scheduleRationale?.trim()) {
    parts.push(`[목표일정 근거]\n${data.scheduleRationale.trim()}`);
  }
  // 가맹점 정보
  if (data.merchantInfo?.trim()) {
    parts.push(`[가맹점 정보]\n${data.merchantInfo.trim()}`);
  }
  // 원천사 정보
  if (data.providerInfo?.trim()) {
    parts.push(`[원천사 정보]\n${data.providerInfo.trim()}`);
  }

  // S6 동적 답변 섹션 합산
  const s6Lines = composeS6Section(data);
  if (s6Lines) parts.push(s6Lines);

  // 추가 확인 필요 항목 — 위저드가 모은 것을 본문에 함께 노출
  const checkLines = composeChecksSection(data.additionalCheckItems ?? []);
  if (checkLines) parts.push(checkLines);

  // 섹션 간 빈 줄 하나로 구분
  return parts.join("\n\n");
}

/**
 * S6 심층 질의 응답(`s6` 필드)을 `[심층 질의 응답]` 섹션 문자열로 변환한다.
 *
 * @param data - 현재 위저드 데이터
 * @returns 섹션 문자열, S6 데이터가 없거나 비어 있으면 `null`
 */
function composeS6Section(data: WizardData): string | null {
  const s6 = data.s6;
  if (!s6) return null;
  const lines: string[] = [];

  // 지불수단 선택값
  if (s6.paymentMethod) lines.push(`- 지불수단: ${s6.paymentMethod}`);

  // 카드 세부 옵션 (지불수단이 "카드"일 때만 노출)
  if (s6.cardOptions) {
    if (s6.cardOptions.installments?.length) {
      lines.push(`- 할부 옵션: ${s6.cardOptions.installments.join(", ")}`);
    }
    // boolean 타입이므로 typeof 체크로 undefined와 구분
    if (typeof s6.cardOptions.usePoint === "boolean") {
      lines.push(`- 포인트/머니 사용: ${s6.cardOptions.usePoint ? "예" : "아니오"}`);
    }
    if (typeof s6.cardOptions.usePromotion === "boolean") {
      lines.push(`- 프로모션 쿠폰 사용: ${s6.cardOptions.usePromotion ? "예" : "아니오"}`);
    }
  }

  // AS-IS / TO-BE 변경 내용 (funcType = MODIFY일 때)
  if (s6.asisTobe) {
    lines.push(`- 변경 종류: ${s6.asisTobe.kind}`);
    lines.push(`- AS-IS: ${s6.asisTobe.asis}`);
    lines.push(`- TO-BE: ${s6.asisTobe.tobe}`);
  }

  // KB 데이터 없을 때 사용자가 직접 입력한 자유 메모
  if (s6.freeNotes?.trim()) {
    lines.push(`- 추가 메모: ${s6.freeNotes.trim()}`);
  }

  if (lines.length === 0) return null;
  return `[심층 질의 응답]\n${lines.join("\n")}`;
}

/**
 * 위저드에서 수집된 추가 확인 필요 항목 목록을 `[추가 확인 필요 항목]` 섹션 문자열로 변환한다.
 *
 * @param items - 추가 확인 항목 목록
 * @returns 섹션 문자열, 빈 배열이면 `null`
 */
function composeChecksSection(items: AdditionalCheckItem[]): string | null {
  if (items.length === 0) return null;
  // 각 항목을 "- (slide N) 필드명 — 사유" 형식으로 줄 별 변환
  const lines = items.map((it) => {
    const reason = it.reason?.trim() ? ` — ${it.reason.trim()}` : "";
    return `- (slide ${it.slide}) ${it.field}${reason}`;
  });
  return `[추가 확인 필요 항목 — 위저드 수집]\n${lines.join("\n")}`;
}

/**
 * GET /api/dev-requests/generate — JSON 응답으로 ProjectMdResult + 표준 양식 MD를 한 번에 받는다.
 * Gemini 호출은 백엔드에서 1회로 끝나고, MD는 같은 결과를 markdownRenderer로 변환한 것.
 *
 * @param data - 현재 위저드 데이터 (백엔드 DTO로 변환 후 쿼리스트링으로 전송)
 * @returns `GenerateResult` — LLM 생성 메타 결과 + 표준 양식 마크다운
 * @throws 응답 상태가 `ok`가 아니거나 `devRequest` 필드가 없으면 에러를 던진다.
 */
export async function fetchDevRequestJson(data: WizardData): Promise<GenerateResult> {
  // 위저드 데이터를 백엔드 DTO로 변환
  const payload = toBackendPayload(data);
  const qs = new URLSearchParams();
  // 한글 자동 인코딩 — Spring의 @ModelAttribute가 record 파라미터로 받음
  (Object.entries(payload) as [keyof BackendDevRequest, string][]).forEach(([k, v]) => {
    if (v != null) qs.set(k, v);
  });

  const res = await fetch(`${GENERATE_URL}?${qs.toString()}`, {
    method: "GET",
    headers: { Accept: "application/json" },
  });

  if (!res.ok) {
    // 오류 본문을 JSON 또는 text로 파싱해 에러 메시지에 포함
    let detail = "";
    try {
      detail = JSON.stringify(await res.json());
    } catch {
      detail = await res.text();
    }
    throw new Error(`요청서 생성 실패 ${res.status}: ${detail}`);
  }

  const json = (await res.json()) as GenerateJsonResponse;
  // devRequest 필드 누락 시 하위 처리 불가 — 즉시 에러
  if (!json?.devRequest) {
    throw new Error("응답에 devRequest 가 없습니다.");
  }
  return { result: json.devRequest, markdown: json.markdown ?? "" };
}

/**
 * 백엔드 ProjectMdResult + 위저드 입력 + 추가확인을 합쳐
 * `/api/requests` 의 `DevRequestSaveRequest` 페이로드로 변환한다.
 *
 * @param args.data - 현재 위저드 데이터
 * @param args.generate - `fetchDevRequestJson` 으로 받은 LLM 생성 결과
 * @returns DB 저장용 페이로드 객체
 *
 * @remarks
 * - `title` : LLM이 채운 `productName` 우선, 없으면 사용자 입력 `serviceName`, 없으면 `"(제목 없음)"`
 * - `details` : 위저드 입력과 LLM 결과를 JSON으로 직렬화해 재현·감사용으로 보존
 * - `unconfirmedSection` : 추가 확인 필요 항목을 MD 체크리스트 형식으로 보존
 * - `flowDiagram` : 현재 위저드에서 PlantUML/mxGraph 원본을 보유하지 않으므로 `undefined`
 */
export function toSavePayload(args: {
  data: WizardData;
  generate: GenerateResult;
}): {
  title: string;
  categoryPath: string;
  status: "AI_ANALYZED";
  author?: string;
  dept?: string;
  details: string;
  combinedMarkdown: string;
  flowDiagram?: string;
  unconfirmedSection?: string;
} {
  const { data, generate } = args;
  const r: ProjectMdResult = generate.result;

  // 제목 우선순위: LLM productName > 사용자 serviceName > 기본값
  const title = r.productName?.trim() || data.serviceName?.trim() || "(제목 없음)";

  // 카테고리 경로를 "funcType / category / subType" 형식으로 조합
  const parts: string[] = [];
  if (data.funcType) parts.push(data.funcType);
  if (data.category) parts.push(data.category);
  if (data.subType) parts.push(data.subType);
  const categoryPath = parts.join(" / ");

  // 위저드 입력과 LLM 결과를 한 묶음 JSON으로 details에 저장 — 재현/감사용
  const details = JSON.stringify(
    {
      wizard: data,
      projectMd: r,
    },
    null,
    2,
  );

  // 위저드의 [잘 모름/추가확인] 항목을 MD 체크리스트 형식으로 변환
  const checks = data.additionalCheckItems ?? [];
  const unconfirmedSection = checks.length === 0
    ? ""
    : checks
        .map((c) => `- (S${c.slide}) ${c.field}${c.reason ? ` — ${c.reason}` : ""}`)
        .join("\n");

  return {
    title,
    categoryPath,
    status: "AI_ANALYZED",
    // LLM 결과가 있으면 우선 사용, 없으면 위저드 입력값 fallback
    author: r.author ?? data.author,
    dept: r.department ?? data.department,
    details,
    combinedMarkdown: generate.markdown,
    // 현재 흐름에선 위저드 단계에서 PlantUML/mxGraph 원본 텍스트를 보유하지 않음.
    // 추후 flow 별도 엔드포인트가 생기면 그 응답을 여기 채운다.
    flowDiagram: undefined,
    // 추가 확인 항목이 없으면 undefined로 전달해 DB에 null 저장
    unconfirmedSection: unconfirmedSection || undefined,
  };
}

/**
 * POST /api/dev-requests/generate — zip(requirements.md + flow.png) 다운로드.
 *
 * @remarks
 * 호출 측에서 반환된 `Blob` 을 `<a>.download` 트리거로 저장한다.
 * 현재는 `/result` 상세 페이지의 [다운로드] 버튼에서만 사용한다.
 *
 * @param data - 현재 위저드 데이터
 * @returns 다운로드용 ZIP `Blob`
 * @throws 응답이 `ok`가 아니면 에러를 던진다.
 */
export async function downloadDevRequestZip(data: WizardData): Promise<Blob> {
  const payload = toBackendPayload(data);
  const res = await fetch(GENERATE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      Accept: "application/zip",
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    let detail = "";
    try { detail = JSON.stringify(await res.json()); } catch { detail = await res.text(); }
    throw new Error(`zip 다운로드 실패 ${res.status}: ${detail}`);
  }
  return await res.blob();
}

/**
 * POST /api/dev-requests/generate — JSON 또는 ZIP 응답을 처리하는 레거시 제출 함수.
 *
 * @deprecated 현재 주 흐름은 `fetchDevRequestJson` + `toSavePayload` 를 사용한다.
 * 이 함수는 구형 호환용으로만 남아 있으며 새 코드에서는 호출하지 않는 것을 권장한다.
 *
 * @param data - 현재 위저드 데이터
 * @returns `GenerateResponse` — JSON 응답이면 파싱, ZIP 응답이면 빈 객체 반환
 * @throws 응답이 `ok`가 아니면 에러를 던진다.
 */
export async function submitDevRequest(data: WizardData): Promise<GenerateResponse> {
  const payload = toBackendPayload(data);
  const res = await fetch(GENERATE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      Accept: "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    // 400(검증)/500(서버) 모두 메시지 한 줄로 던지고 호출 측에서 처리
    let detail = "";
    try {
      detail = JSON.stringify(await res.json());
    } catch {
      detail = await res.text();
    }
    throw new Error(`요청서 생성 실패 ${res.status}: ${detail}`);
  }

  // 현재 백엔드는 zip 바이너리를 반환. design.md §6는 JSON 반환을 목표로 명시(§12-1).
  // JSON으로 오는 경우만 정상 파싱하고, 아니면 빈 응답으로 처리해 /result는 별도 호출에 의존.
  const ctype = res.headers.get("content-type") ?? "";
  if (ctype.includes("application/json")) {
    return (await res.json()) as GenerateResponse;
  }
  // zip(현 구현) — 응답 본문은 무시하고 빈 GenerateResponse로 채워 라우팅만 진행.
  return {
    requestId: "",
    requirementsMarkdown: "",
    projectMeta: null,
    additionalChecks: [],
  };
}
