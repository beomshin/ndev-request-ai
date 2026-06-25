import { useMemo } from "react";
import { useCatalog } from "../catalog";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";
import type { SubType } from "../types";

// S2 — 대분류(카테고리) + 세부유형. 둘 다 카탈로그(yaml)에서 가져온다.
// funcType이 정해지면 yaml의 available_func_types로 sub_type을 필터링한다
// (예: API__NEW '신규API생성'은 NEW에서만 노출).
export function Slide2Category() {
  const { state, patch } = useWizard();
  const { data: catalog, isLoading, isError } = useCatalog();

  const funcTypeCode = state.data.funcTypeCode;
  const categories = catalog?.categories ?? [];

  // funcType에 따라 sub_types를 사전 필터링한 카테고리 목록.
  // available_func_types가 없거나 비어있으면 둘 다 가용으로 본다.
  const filteredCategories = useMemo(
    () =>
      categories.map((c) => ({
        ...c,
        subTypes: c.subTypes.filter((s) => isSubTypeAvailable(s, funcTypeCode)),
      })),
    [categories, funcTypeCode],
  );

  const selectedCategory = filteredCategories.find((c) => c.label === state.data.category);
  const hasSubTypes = (selectedCategory?.subTypes.length ?? 0) > 0;

  return (
    <SlideShell
      step={2}
      title="어떤 영역의 작업인가요?"
      description={
        funcTypeCode === "NEW"
          ? "신규 개발에 해당하는 분류·세부유형만 보입니다."
          : funcTypeCode === "MODIFY"
            ? "기존 서비스 수정·개선에 해당하는 분류·세부유형만 보입니다."
            : "결제·API·해외결제 같은 큰 분류 → 세부 유형 순으로 골라 주세요."
      }
    >
      {isLoading && (
        <p className="text-xs text-muted-foreground">카탈로그를 불러오는 중…</p>
      )}
      {isError && (
        <p className="text-xs text-destructive">
          카탈로그 조회에 실패했습니다. 백엔드(GET /api/catalog/)를 확인해 주세요.
        </p>
      )}

      {/* 대분류 */}
      <div>
        <div className="text-xs font-medium text-foreground mb-2">대분류</div>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          {filteredCategories.map((c) => {
            const selected = state.data.category === c.label;
            return (
              <button
                key={c.code}
                type="button"
                onClick={() => {
                  patch({ category: c.label, subType: undefined });
                }}
                className={`text-left rounded-lg border p-4 transition ${
                  selected
                    ? "border-primary bg-primary/5 ring-1 ring-primary"
                    : "border-border hover:border-primary/40 hover:bg-secondary/60"
                }`}
              >
                <div className="text-base mb-1.5">{iconFor(c.code)}</div>
                <div
                  className={`text-sm font-medium ${
                    selected ? "text-primary" : "text-foreground"
                  }`}
                >
                  {c.label}
                </div>
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

      {/* 세부유형 — 카테고리 선택했고, 세부유형이 있을 때만 노출 */}
      {selectedCategory && hasSubTypes && (
        <div>
          <div className="text-xs font-medium text-foreground mb-2">
            세부유형 · {selectedCategory.label}
          </div>
          <div className="flex flex-wrap gap-2">
            {selectedCategory.subTypes.map((s) => {
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

      {selectedCategory && !hasSubTypes && (
        <p className="text-[11px] text-muted-foreground">
          이 분류는 세부유형이 없습니다. 그대로 다음으로 진행할 수 있습니다.
        </p>
      )}
    </SlideShell>
  );
}

// available_func_types가 없거나 비어있으면 둘 다 가용. 명시되면 그 funcType만.
function isSubTypeAvailable(s: SubType, funcTypeCode?: string): boolean {
  const aft = s.availableFuncTypes;
  if (!aft || aft.length === 0) return true;
  if (!funcTypeCode) return true;
  return aft.includes(funcTypeCode);
}

// 시각적 구분만을 위한 임시 아이콘. 실제 의미와 무관 — 카테고리 늘어나면 매핑 추가.
function iconFor(code: string): string {
  switch (code) {
    case "PG_STD":
    case "pg_std_pay":
      return "▣";
    case "API":
    case "api":
      return "⌥";
    case "GLOBAL":
    case "overseas":
      return "◐";
    case "ETC_SERVICE":
    case "etc":
      return "⋯";
    default:
      return "◇";
  }
}
