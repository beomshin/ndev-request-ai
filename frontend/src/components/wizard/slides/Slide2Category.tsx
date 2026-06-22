import { useCatalog } from "../catalog";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

// S2 — 대분류(카테고리) + 세부유형. 둘 다 카탈로그에서 동적으로 가져온다.
// 기타서비스는 세부유형이 없으므로 subType 단계 자체를 생략한다.
export function Slide2Category() {
  const { state, patch } = useWizard();
  const { data: catalog, isLoading, isError } = useCatalog();

  const categories = catalog?.categories ?? [];
  const selectedCategory = categories.find((c) => c.label === state.data.category);
  const hasSubTypes = (selectedCategory?.subTypes.length ?? 0) > 0;

  return (
    <SlideShell
      step={2}
      title="어떤 영역의 작업인가요?"
      description="결제·API·해외결제 같은 큰 분류 → 세부 유형 순으로 골라 주세요."
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
          {categories.map((c) => {
            const selected = state.data.category === c.label;
            return (
              <button
                key={c.code}
                type="button"
                onClick={() => {
                  // 카테고리 바꾸면 이전에 고른 세부유형은 리셋
                  patch({ category: c.label, subType: undefined });
                }}
                className={`text-left rounded-lg border p-4 transition ${
                  selected
                    ? "border-primary bg-primary/5 ring-1 ring-primary"
                    : "border-border hover:border-primary/40 hover:bg-secondary/60"
                }`}
              >
                <div className="text-base mb-1.5">
                  {iconFor(c.code)}
                </div>
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

// 시각적 구분만을 위한 임시 아이콘. 실제 의미와 무관 — 카테고리 늘어나면 매핑 추가.
function iconFor(code: string): string {
  switch (code) {
    case "pg_std_pay":
      return "▣";
    case "api":
      return "⌥";
    case "overseas":
      return "◐";
    case "etc":
      return "⋯";
    default:
      return "◇";
  }
}
