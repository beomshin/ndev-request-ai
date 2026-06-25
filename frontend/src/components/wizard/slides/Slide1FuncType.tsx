import { useCatalog } from "../catalog";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";
import type { FuncTypeCode } from "../types";

/**
 * @file Slide1FuncType.tsx
 * @description 위저드 1단계 — 기능구분(신규/수정) 선택 슬라이드.
 *
 * 카탈로그(YAML)에서 `func_types` 목록을 가져와 카드 형태로 나열하고,
 * 사용자가 선택한 값을 위저드 상태(state)에 저장합니다.
 *
 * @remarks
 * - `funcType`: 선택된 옵션의 **라벨** 문자열. 백엔드 LLM 프롬프트에 그대로 전달됩니다.
 * - `funcTypeCode`: `"NEW"` | `"MODIFY"` 코드값. 이후 슬라이드 분기(예: S5, S6)에 사용됩니다.
 * - funcType이 변경되면 카테고리·세부유형이 초기화됩니다.
 *   (카탈로그 `available_func_types` 필터에 의해 유효 범위가 달라지기 때문입니다.)
 * - 카탈로그 로딩 실패 시, 하드코딩된 기본 옵션(NEW/MODIFY) 두 개를 대신 표시합니다.
 */

/**
 * 위저드 1단계 컴포넌트 — 개발 요청의 기능 유형(신규/수정)을 선택합니다.
 *
 * @returns 기능 유형 선택 카드 목록을 포함한 슬라이드 UI.
 *
 * @example
 * ```tsx
 * <Slide1FuncType />
 * ```
 */
// S1 — 기능구분. 카탈로그(yaml)의 func_types를 그대로 노출.
// 클릭 시 funcType(라벨) + funcTypeCode(NEW/MODIFY) 둘 다 저장 — 슬라이드 분기에 사용.
export function Slide1FuncType() {
  /** 위저드 전역 상태 및 패치 함수 */
  const { state, patch } = useWizard();
  /** 카탈로그 원격 데이터 (로딩/오류 상태 포함) */
  const { data: catalog, isLoading, isError } = useCatalog();

  /**
   * 표시할 기능 유형 옵션 목록.
   * 카탈로그에서 정상적으로 불러오지 못한 경우 하드코딩된 기본값을 사용합니다.
   */
  const options =
    catalog?.funcTypes ?? [
      { code: "NEW", label: "신규 서비스 개발", description: "이전에 없던 새 결제 흐름·기능을 추가합니다." },
      { code: "MODIFY", label: "기존 서비스 수정·개선", description: "이미 운영 중인 기능을 수정/개선합니다." },
    ];

  return (
    <SlideShell
      step={1}
      title="어떤 유형의 개발 요청인가요?"
      description="신규 기능을 새로 만드는 작업인지, 기존 기능을 고치거나 개선하는 작업인지 선택해 주세요."
    >
      {/* 카탈로그 로딩 중 안내 메시지 */}
      {isLoading && (
        <p className="text-xs text-muted-foreground">카탈로그를 불러오는 중…</p>
      )}
      {/* 카탈로그 로딩 실패 시 경고 메시지 (기본 옵션으로 대체) */}
      {isError && (
        <p className="text-xs text-[color:var(--warning)]">
          카탈로그 조회 실패 — 기본 옵션으로 표시합니다.
        </p>
      )}

      {/* 기능 유형 선택 카드 그리드 (모바일: 1열, 태블릿+: 2열) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {options.map((opt) => {
          /** 현재 옵션이 선택된 상태인지 여부 (라벨 기준 비교) */
          const selected = state.data.funcType === opt.label;
          /** 신규 개발 여부 (아이콘 분기에 사용) */
          const isNew = opt.code === "NEW";
          return (
            <button
              key={opt.code}
              type="button"
              onClick={() =>
                patch({
                  funcType: opt.label,                    // 라벨은 백엔드 LLM 프롬프트로 전달
                  funcTypeCode: opt.code as FuncTypeCode, // 코드는 FE 분기 로직에서 사용
                  // funcType이 바뀌면 카테고리/세부유형은 재선택해야 함 — yaml에 따라 가용 범위가 달라짐
                  category: undefined,
                  subType: undefined,
                })
              }
              className={`text-left rounded-lg border p-5 transition ${
                selected
                  ? "border-primary bg-primary/5 ring-1 ring-primary"
                  : "border-border hover:border-primary/40 hover:bg-secondary/60"
              }`}
            >
              {/* 유형 구분 아이콘: 신규(✦) / 수정(↻) */}
              <div
                className="text-2xl mb-2 inline-flex h-9 w-9 items-center justify-center rounded-md text-primary-foreground"
                style={{ background: "var(--gradient-primary)" }}
              >
                {isNew ? "✦" : "↻"}
              </div>
              {/* 옵션 라벨 — 선택 시 primary 색상으로 강조 */}
              <div
                className={`text-sm font-semibold ${
                  selected ? "text-primary" : "text-foreground"
                }`}
              >
                {opt.label}
              </div>
              {/* 옵션 설명 — 카탈로그에 description이 없으면 하드코딩된 기본 문구 사용 */}
              <div className="text-xs text-muted-foreground mt-1 leading-snug">
                {opt.description || (isNew
                  ? "이전에 없던 새 결제 흐름·기능을 추가합니다."
                  : "이미 운영 중인 기능을 수정/개선합니다.")}
              </div>
            </button>
          );
        })}
      </div>
    </SlideShell>
  );
}
