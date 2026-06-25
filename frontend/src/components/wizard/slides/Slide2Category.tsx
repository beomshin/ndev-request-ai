import { useMemo } from "react";
import { useCatalog } from "../catalog";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";
import type { SubType } from "../types";

/**
 * @file Slide2Category.tsx
 * @description 위저드 2단계 — 대분류(카테고리) 및 세부유형 선택 슬라이드.
 *
 * 카탈로그(YAML)에서 카테고리 목록을 불러와 대분류 → 세부유형 순으로 선택합니다.
 *
 * @remarks
 * ### 필터링 규칙
 * - 각 세부유형(`SubType`)의 `availableFuncTypes` 배열에 현재 `funcTypeCode`가 포함되어야 노출됩니다.
 * - `availableFuncTypes`가 없거나 빈 배열이면 NEW/MODIFY 모두에서 노출됩니다.
 * - 예) `API__NEW '신규API생성'`은 `NEW`에서만 표시됩니다.
 *
 * ### 세부유형이 없는 카테고리
 * - 선택된 카테고리에 세부유형이 없으면 안내 문구를 표시하고 바로 다음 단계로 진행할 수 있습니다.
 */

/**
 * 위저드 2단계 컴포넌트 — 작업 영역의 대분류와 세부유형을 선택합니다.
 *
 * @returns 대분류 선택 카드 및 세부유형 태그 목록을 포함한 슬라이드 UI.
 *
 * @example
 * ```tsx
 * <Slide2Category />
 * ```
 */
// S2 — 대분류(카테고리) + 세부유형. 둘 다 카탈로그(yaml)에서 가져온다.
// funcType이 정해지면 yaml의 available_func_types로 sub_type을 필터링한다
// (예: API__NEW '신규API생성'은 NEW에서만 노출).
export function Slide2Category() {
  /** 위저드 전역 상태 및 패치 함수 */
  const { state, patch } = useWizard();
  /** 카탈로그 원격 데이터 (로딩/오류 상태 포함) */
  const { data: catalog, isLoading, isError } = useCatalog();

  /** 1단계에서 선택된 기능 유형 코드 (NEW | MODIFY). 세부유형 필터링에 사용됩니다. */
  const funcTypeCode = state.data.funcTypeCode;
  /** 카탈로그에서 가져온 전체 카테고리 목록. 로딩 실패 시 빈 배열. */
  const categories = catalog?.categories ?? [];

  /**
   * funcType에 따라 세부유형이 사전 필터링된 카테고리 목록.
   *
   * @remarks
   * `categories` 또는 `funcTypeCode`가 변경될 때만 재계산됩니다.
   * `availableFuncTypes`가 없거나 비어있으면 해당 세부유형은 모든 funcType에서 가용합니다.
   */
  const filteredCategories = useMemo(
    () =>
      categories.map((c) => ({
        ...c,
        subTypes: c.subTypes.filter((s) => isSubTypeAvailable(s, funcTypeCode)),
      })),
    [categories, funcTypeCode],
  );

  /** 현재 선택된 카테고리 객체 (필터링된 목록 기준). */
  const selectedCategory = filteredCategories.find((c) => c.label === state.data.category);
  /** 선택된 카테고리에 표시할 세부유형이 하나 이상 있는지 여부. */
  const hasSubTypes = (selectedCategory?.subTypes.length ?? 0) > 0;

  return (
    <SlideShell
      step={2}
      title="어떤 영역의 작업인가요?"
      description={
        // funcTypeCode에 따라 설명 문구를 동적으로 변경하여 맥락 정보를 제공합니다.
        funcTypeCode === "NEW"
          ? "신규 개발에 해당하는 분류·세부유형만 보입니다."
          : funcTypeCode === "MODIFY"
            ? "기존 서비스 수정·개선에 해당하는 분류·세부유형만 보입니다."
            : "결제·API·해외결제 같은 큰 분류 → 세부 유형 순으로 골라 주세요."
      }
    >
      {/* 카탈로그 로딩 중 안내 메시지 */}
      {isLoading && (
        <p className="text-xs text-muted-foreground">카탈로그를 불러오는 중…</p>
      )}
      {/* 카탈로그 로딩 실패 시 오류 메시지 (백엔드 URL 안내 포함) */}
      {isError && (
        <p className="text-xs text-destructive">
          카탈로그 조회에 실패했습니다. 백엔드(GET /api/catalog/)를 확인해 주세요.
        </p>
      )}

      {/* 대분류 선택 카드 그리드 (모바일: 2열, 태블릿+: 4열) */}
      <div>
        <div className="text-xs font-medium text-foreground mb-2">대분류</div>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          {filteredCategories.map((c) => {
            /** 현재 카테고리가 선택된 상태인지 여부 (라벨 기준 비교) */
            const selected = state.data.category === c.label;
            return (
              <button
                key={c.code}
                type="button"
                onClick={() => {
                  // 카테고리 변경 시 세부유형은 항상 초기화됩니다.
                  patch({ category: c.label, subType: undefined });
                }}
                className={`text-left rounded-lg border p-4 transition ${
                  selected
                    ? "border-primary bg-primary/5 ring-1 ring-primary"
                    : "border-border hover:border-primary/40 hover:bg-secondary/60"
                }`}
              >
                {/* 카테고리 코드 기반 아이콘 (시각적 구분용) */}
                <div className="text-base mb-1.5">{iconFor(c.code)}</div>
                {/* 카테고리 라벨 — 선택 시 primary 색상으로 강조 */}
                <div
                  className={`text-sm font-medium ${
                    selected ? "text-primary" : "text-foreground"
                  }`}
                >
                  {c.label}
                </div>
                {/* 필터링 후 남은 세부유형 개수 표시 */}
                <div className="text-[11px] text-muted-foreground mt-0.5 leading-snug">
                  {c.subTypes.length > 0
                    ? `세부유형 ${c.subTypes.length}개`
                    : "세부유형 없음"}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* 세부유형 태그 목록 — 카테고리 선택했고, 세부유형이 있을 때만 노출 */}
      {selectedCategory && hasSubTypes && (
        <div>
          <div className="text-xs font-medium text-foreground mb-2">
            세부유형 · {selectedCategory.label}
          </div>
          <div className="flex flex-wrap gap-2">
            {selectedCategory.subTypes.map((s) => {
              /** 현재 세부유형이 선택된 상태인지 여부 (라벨 기준 비교) */
              const selected = state.data.subType === s.label;
              return (
                <button
                  key={s.code}
                  type="button"
                  onClick={() => patch({ subType: s.label })}
                  className={`text-xs rounded-full border px-3 py-1.5 transition ${
                    selected
                      ? "bg-primary text-primary-foreground border-primary"
                      : "bg-card text-foreground border-border hover:border-primary/40"
                  }`}
                >
                  {s.label}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* 세부유형이 없는 카테고리를 선택한 경우 안내 문구 */}
      {selectedCategory && !hasSubTypes && (
        <p className="text-[11px] text-muted-foreground">
          이 분류는 세부유형이 없습니다. 그대로 다음으로 진행할 수 있습니다.
        </p>
      )}
    </SlideShell>
  );
}

/**
 * 주어진 세부유형이 특정 `funcTypeCode` 환경에서 선택 가능한지 판별합니다.
 *
 * @param s - 판별 대상 세부유형 객체.
 * @param funcTypeCode - 1단계에서 선택된 기능 유형 코드 (예: `"NEW"` | `"MODIFY"`).
 * @returns `true`이면 해당 funcType에서 노출 가능, `false`이면 필터링되어 숨김.
 *
 * @remarks
 * `availableFuncTypes`가 없거나 빈 배열이면 모든 funcType에서 가용합니다.
 * `funcTypeCode`가 undefined인 경우에도 가용으로 처리합니다(아직 1단계 미선택 상태).
 */
// available_func_types가 없거나 비어있으면 둘 다 가용. 명시되면 그 funcType만.
function isSubTypeAvailable(s: SubType, funcTypeCode?: string): boolean {
  const aft = s.availableFuncTypes;
  if (!aft || aft.length === 0) return true;
  if (!funcTypeCode) return true;
  return aft.includes(funcTypeCode);
}

/**
 * 카테고리 코드에 대응하는 시각적 아이콘 문자를 반환합니다.
 *
 * @param code - 카탈로그 카테고리 코드 (예: `"PG_STD"`, `"API"`, `"GLOBAL"`, `"ETC_SERVICE"`).
 * @returns 해당 카테고리를 나타내는 유니코드 아이콘 문자열.
 *
 * @remarks
 * - 이 함수는 **시각적 구분만을 위한 임시 매핑**입니다. 실제 비즈니스 의미와 무관합니다.
 * - 카탈로그에 새 카테고리가 추가되면 이 `switch` 문에 케이스를 추가해야 합니다.
 * - 매핑되지 않은 코드는 기본값 `"◇"`를 반환합니다.
 */
// 시각적 구분만을 위한 임시 아이콘. 실제 의미와 무관 — 카테고리 늘어나면 매핑 추가.
function iconFor(code: string): string {
  switch (code) {
    case "PG_STD":       // PG 표준결제창
    case "pg_std_pay":
      return "▣";
    case "API":          // API 연동
    case "api":
      return "⌥";
    case "GLOBAL":       // 해외결제
    case "overseas":
      return "◐";
    case "ETC_SERVICE":  // 기타 서비스
    case "etc":
      return "⋯";
    default:
      return "◇";
  }
}
