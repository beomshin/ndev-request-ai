import { useCatalog } from "../catalog";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";
import type { FuncTypeCode } from "../types";

// S1 — 기능구분. 카탈로그(yaml)의 func_types를 그대로 노출.
// 클릭 시 funcType(라벨) + funcTypeCode(NEW/MODIFY) 둘 다 저장 — 슬라이드 분기에 사용.
export function Slide1FuncType() {
  const { state, patch } = useWizard();
  const { data: catalog, isLoading, isError } = useCatalog();

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
          const selected = state.data.funcType === opt.label;
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
              <div
                className="text-2xl mb-2 inline-flex h-9 w-9 items-center justify-center rounded-md text-primary-foreground"
                style={{ background: "var(--gradient-primary)" }}
              >
                {isNew ? "✦" : "↻"}
              </div>
              <div
                className={`text-sm font-semibold ${
                  selected ? "text-primary" : "text-foreground"
                }`}
              >
                {opt.label}
              </div>
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
