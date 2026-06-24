import type {
  AdditionalCheckItem,
  BackendDevRequest,
  GenerateJsonResponse,
  GenerateResponse,
  GenerateResult,
  ProjectMdResult,
  WizardData,
} from "./types";

const GENERATE_URL = "/api/dev-requests/generate";

// 백엔드 DevRequestRequest는 6필드 + funcType/category/subType 총 9필드만 받는다.
// 위저드는 더 많이 모으므로(목표일정 근거, 빈도, 경쟁사, S6 답변 등) 일부는
// problemAndImprovement 본문에 자연스럽게 합쳐 보낸다. 백엔드 DTO 확장 시 이 매핑 정리 권장.
// 도메인 매핑(가맹점/원천사 ID 등)은 추측하지 않는다 — 사용자가 입력한 텍스트를 그대로 전달.
export function toBackendPayload(data: WizardData): BackendDevRequest {
  return {
    funcType: data.funcType ?? "",
    category: data.category ?? "",
    subType: data.subType && data.subType.length > 0 ? data.subType : "(없음)",
    author: data.author ?? "",
    department: data.department ?? "",
    serviceName: data.serviceName ?? "",
    background: data.background ?? "",
    targetSchedule: normalizeDate(data.targetSchedule ?? ""),
    problemAndImprovement: composeProblemAndImprovement(data),
  };
}

// "YYYYMMDD" 또는 "YYYY-MM-DD" 둘 다 허용해 백엔드로는 YYYY-MM-DD로 보낸다.
function normalizeDate(raw: string): string {
  const onlyDigits = raw.replace(/[^0-9]/g, "");
  if (onlyDigits.length === 8) {
    return `${onlyDigits.slice(0, 4)}-${onlyDigits.slice(4, 6)}-${onlyDigits.slice(6, 8)}`;
  }
  return raw;
}

// 백엔드 DTO에 자리 없는 필드들을 problemAndImprovement에 섹션으로 합친다.
// 백엔드가 LLM에게 그대로 전달하기 때문에, 본문 안에서 항목별로 명시되면 모델이 해석한다.
function composeProblemAndImprovement(data: WizardData): string {
  const parts: string[] = [];

  if (data.problemAndImprovement?.trim()) {
    parts.push(`[문제점/개선점]\n${data.problemAndImprovement.trim()}`);
  }
  if (data.problemDetectionMethod?.trim()) {
    parts.push(`[문제 확인 방법]\n${data.problemDetectionMethod.trim()}`);
  }
  if (data.occurrenceFrequency?.trim()) {
    parts.push(`[발생 빈도]\n${data.occurrenceFrequency.trim()}`);
  }
  if (data.competitorInfo?.trim()) {
    parts.push(`[경쟁사 정보]\n${data.competitorInfo.trim()}`);
  }
  if (data.expectedRevenue?.trim() || data.expectedLoss?.trim()) {
    const lines: string[] = [];
    if (data.expectedRevenue?.trim()) lines.push(`- 예상 수익: ${data.expectedRevenue.trim()}`);
    if (data.expectedLoss?.trim()) lines.push(`- 예상 손해: ${data.expectedLoss.trim()}`);
    parts.push(`[기대효과]\n${lines.join("\n")}`);
  }
  if (data.scheduleRationale?.trim()) {
    parts.push(`[목표일정 근거]\n${data.scheduleRationale.trim()}`);
  }
  if (data.merchantInfo?.trim()) {
    parts.push(`[가맹점 정보]\n${data.merchantInfo.trim()}`);
  }
  if (data.providerInfo?.trim()) {
    parts.push(`[원천사 정보]\n${data.providerInfo.trim()}`);
  }

  // S6 동적 답변
  const s6Lines = composeS6Section(data);
  if (s6Lines) parts.push(s6Lines);

  // 추가 확인 필요 항목 — 위저드가 모은 것을 본문에 함께 노출
  const checkLines = composeChecksSection(data.additionalCheckItems ?? []);
  if (checkLines) parts.push(checkLines);

  return parts.join("\n\n");
}

function composeS6Section(data: WizardData): string | null {
  const s6 = data.s6;
  if (!s6) return null;
  const lines: string[] = [];

  if (s6.paymentMethod) lines.push(`- 지불수단: ${s6.paymentMethod}`);
  if (s6.cardOptions) {
    if (s6.cardOptions.installments?.length) {
      lines.push(`- 할부 옵션: ${s6.cardOptions.installments.join(", ")}`);
    }
    if (typeof s6.cardOptions.usePoint === "boolean") {
      lines.push(`- 포인트/머니 사용: ${s6.cardOptions.usePoint ? "예" : "아니오"}`);
    }
    if (typeof s6.cardOptions.usePromotion === "boolean") {
      lines.push(`- 프로모션 쿠폰 사용: ${s6.cardOptions.usePromotion ? "예" : "아니오"}`);
    }
  }
  if (s6.asisTobe) {
    lines.push(`- 변경 종류: ${s6.asisTobe.kind}`);
    lines.push(`- AS-IS: ${s6.asisTobe.asis}`);
    lines.push(`- TO-BE: ${s6.asisTobe.tobe}`);
  }
  if (s6.freeNotes?.trim()) {
    lines.push(`- 추가 메모: ${s6.freeNotes.trim()}`);
  }

  if (lines.length === 0) return null;
  return `[심층 질의 응답]\n${lines.join("\n")}`;
}

function composeChecksSection(items: AdditionalCheckItem[]): string | null {
  if (items.length === 0) return null;
  const lines = items.map((it) => {
    const reason = it.reason?.trim() ? ` — ${it.reason.trim()}` : "";
    return `- (slide ${it.slide}) ${it.field}${reason}`;
  });
  return `[추가 확인 필요 항목 — 위저드 수집]\n${lines.join("\n")}`;
}

/**
 * GET /api/dev-requests/generate — JSON 응답으로 ProjectMdResult + 표준 양식 MD를 한 번에 받는다.
 * Gemini 호출은 백엔드에서 1회로 끝나고, MD는 같은 결과를 markdownRenderer로 변환한 것.
 */
export async function fetchDevRequestJson(data: WizardData): Promise<GenerateResult> {
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
    let detail = "";
    try {
      detail = JSON.stringify(await res.json());
    } catch {
      detail = await res.text();
    }
    throw new Error(`요청서 생성 실패 ${res.status}: ${detail}`);
  }

  const json = (await res.json()) as GenerateJsonResponse;
  if (!json?.devRequest) {
    throw new Error("응답에 devRequest 가 없습니다.");
  }
  return { result: json.devRequest, markdown: json.markdown ?? "" };
}

/**
 * 백엔드 ProjectMdResult + 위저드 입력 + 추가확인을 합쳐
 * /api/requests 의 DevRequestSaveRequest 페이로드로 변환.
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
  const r = generate.result;
  const title = r.productName?.trim() || data.serviceName?.trim() || "(제목 없음)";
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

  // 위저드의 [잘 모름/추가확인] 항목을 MD 섹션 또는 JSON으로 보존
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
    author: r.author ?? data.author,
    dept: r.department ?? data.department,
    details,
    combinedMarkdown: generate.markdown,
    // 현재 흐름에선 위저드 단계에서 PlantUML/mxGraph 원본 텍스트를 보유하지 않음.
    // 추후 flow 별도 엔드포인트가 생기면 그 응답을 여기 채운다.
    flowDiagram: undefined,
    unconfirmedSection: unconfirmedSection || undefined,
  };
}

/**
 * POST /api/dev-requests/generate — zip(requirements.md + flow.png) 다운로드.
 * 호출 측에서 Blob을 받아 a.download 트리거. 현재 /result의 [다운로드] 버튼에서만 사용.
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
