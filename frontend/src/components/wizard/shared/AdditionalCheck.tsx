/**
 * @file AdditionalCheck.tsx
 * @description 입력 필드 옆에 함께 배치하는 "잘 모름 / 추가 확인 필요" 체크박스 컴포넌트.
 *
 * 체크 시 사유 입력칸이 펼쳐지며, 입력한 내용은 위저드 전역 상태의
 * `additionalCheckItems` 배열에 자동으로 추가/제거된다.
 *
 * @remarks
 * - 동일 `field` 값은 중복 추가되지 않는다 (`WizardContext` reducer 내 중복 체크).
 * - 체크를 해제하면 해당 `field` 의 항목이 배열에서 즉시 제거된다.
 * - 사유 변경 시 기존 항목을 제거하고 새 항목으로 교체하는 방식을 사용한다.
 */

import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { useWizard } from "../WizardContext";

/**
 * "잘 모름 / 추가 확인 필요" 체크박스 컴포넌트.
 * 체크하면 `additionalCheckItems` 에 항목이 추가되고, 사유 입력칸이 나타난다.
 *
 * @param props.slide - 이 컴포넌트가 위치한 슬라이드 번호 (1-based)
 * @param props.field - 이 항목을 식별하는 필드 라벨 (유일한 값이어야 함)
 * @param props.label - 체크박스 옆에 표시할 텍스트 (기본값: "잘 모름 / 추가 확인 필요")
 *
 * @example
 * <AdditionalCheck slide={3} field="가맹점 MID" />
 */
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

  // 현재 additionalCheckItems 배열에서 이 field에 해당하는 항목을 찾는다
  const items = state.data.additionalCheckItems ?? [];
  const current = items.find((it) => it.field === field);
  // current가 존재하면 체크된 상태
  const checked = !!current;

  return (
    <div className="mt-2 text-xs">
      <label className="inline-flex items-center gap-2 cursor-pointer select-none text-muted-foreground hover:text-foreground">
        <Checkbox
          checked={checked}
          onCheckedChange={(v) => {
            if (v) {
              // 체크 ON: 빈 reason으로 항목 추가
              addCheck({ slide, field, reason: "" });
            } else {
              // 체크 OFF: 해당 field 항목 제거
              removeCheck(field);
            }
          }}
        />
        <span>{label}</span>
      </label>

      {/* 체크된 경우에만 사유 입력칸을 렌더 */}
      {checked && (
        <Input
          className="mt-1.5 h-8 text-xs"
          placeholder="(선택) 어떤 부분이 불확실한지 짧게 메모"
          value={current?.reason ?? ""}
          onChange={(e) => {
            // 사유 업데이트는 항목 자체를 갈아끼우는 식으로 처리:
            // 1) 기존 항목 제거 → 2) 새 reason으로 다시 추가
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
