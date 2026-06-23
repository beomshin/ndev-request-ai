import { useCatalog } from "../catalog";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

// S1 — 기능구분(신규/수정). funcType 라벨은 카탈로그에서 가져온다(하드코딩 금지).
// 카탈로그 실패 시에도 위저드가 막히지 않도록 폴백 옵션을 보여준다.
export function Slide1FuncType() {
  const { state, patch } = useWizard();
  const { data: catalog, isLoading, isError } = useCatalog();

  const options =
    catalog?.funcTypes ?? [
      { code: "new", label: "신규 서비스 개발" },
      { code: "modify", label: "기존 서비스 수정·개선" },
    ];

  return (
    <SlideShell
      step={1}
      title="어떤 유형의 개발 요청인가요?"
      description="신규 기능을 새로 만드는 작업인지, 기존 기능을 고치거나 개선하는 작업인지 선택해 주세요."
    >
      {isLoading && (
        <p className="text-xs text-muted-foreground">카탈로그를 불러오는 중…</p>
      )}
      {isError && (
        <p className="text-xs text-[color:var(--warning)]">
          카탈로그 조회 실패 — 기본 옵션으로 표시합니다.
        </p>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {options.map((opt) => {
          // 라벨을 그대로 백엔드로 보낸다 (백엔드 DTO는 문자열 funcType을 받음)
          const selected = state.data.funcType === opt.label;
          return (
            <button
              key={opt.code}
              type="button"
              onClick={() => patch({ funcType: opt.label })}
              className={`text-left rounded-lg border p-5 transition ${
                selected
                  ? "border-primary bg-primary/5 ring-1 ring-primary"
                  : "border-border hover:border-primary/40 hover:bg-secondary/60"
              }`}
            >
              <div
                className="text-2xl mb-2 inline-flex h-9 w-9 items-center justify-center rounded-md text-primary-foreground"
                style={{ background: "var(--gradient-primary)" }}
              >
                {opt.code === "new" ? "✦" : "↻"}
              </div>
              <div
                className={`text-sm font-semibold ${
                  selected ? "text-primary" : "text-foreground"
                }`}
              >
                {opt.label}
              </div>
              <div className="text-xs text-muted-foreground mt-1 leading-snug">
                {opt.code === "new"
                  ? "이전에 없던 새 결제 흐름·기능을 추가합니다."
                  : "이미 운영 중인 기능을 수정/개선합니다."}
              </div>
            </button>
          );
        })}
      </div>
    </SlideShell>
  );
}
