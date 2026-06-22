import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { useWizard } from "../WizardContext";

// 각 입력 옆에 두는 [잘 모름 / 추가 확인 필요] 체크박스. 체크 시 사유 입력칸이 펼쳐진다.
// 누적 상태의 additionalCheckItems[]에 자동 add/remove.
export function AdditionalCheck({
  slide,
  field,
  label = "잘 모름 / 추가 확인 필요",
}: {
  slide: number;
  field: string;
  label?: string;
}) {
  const { state, addCheck, removeCheck, patch } = useWizard();
  const items = state.data.additionalCheckItems ?? [];
  const current = items.find((it) => it.field === field);
  const checked = !!current;

  return (
    <div className="mt-2 text-xs">
      <label className="inline-flex items-center gap-2 cursor-pointer select-none text-muted-foreground hover:text-foreground">
        <Checkbox
          checked={checked}
          onCheckedChange={(v) => {
            if (v) addCheck({ slide, field, reason: "" });
            else removeCheck(field);
          }}
        />
        <span>{label}</span>
      </label>
      {checked && (
        <Input
          className="mt-1.5 h-8 text-xs"
          placeholder="(선택) 어떤 부분이 불확실한지 짧게 메모"
          value={current?.reason ?? ""}
          onChange={(e) => {
            // 사유 업데이트는 항목 자체를 갈아끼우는 식으로 처리
            removeCheck(field);
            addCheck({ slide, field, reason: e.target.value });
            // 변경이 다른 곳에서 일관되도록 강제 패치 호출 — reducer 호출 순서로도 동작하지만 안전망
            patch({});
          }}
        />
      )}
    </div>
  );
}
