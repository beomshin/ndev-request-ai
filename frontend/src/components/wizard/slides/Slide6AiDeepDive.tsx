import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import { AdditionalCheck } from "../shared/AdditionalCheck";
import { EmptyKbNotice } from "../shared/EmptyKbNotice";
import { FileAttachZone } from "../shared/FileAttachZone";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

/**
 * @file Slide6AiDeepDive.tsx
 * @description 위저드 6단계 — AI 동적 심층 질의 슬라이드 (상세 3/3).
 *
 * 앞 단계에서 선택한 기능 유형(funcTypeCode)에 따라 분기하여
 * 정책 확정에 필요한 추가 정보를 수집합니다.
 *
 * @remarks
 * ### 분기 규칙
 *
 * | 조건 | 표시 내용 |
 * |------|----------|
 * | `funcTypeCode === "MODIFY"` | AS-IS / TO-BE 섹션 (UI 또는 API 유형 선택 후 각각 입력) |
 * | `funcTypeCode === "NEW"` | 자유 메모 텍스트영역 + `EmptyKbNotice` 안내 |
 * | 공통 | `AdditionalCheck` 체크박스, `FileAttachZone` 첨부 영역 |
 *
 * ### AS-IS / TO-BE 섹션 (MODIFY 전용)
 * - **UI 유형**: 화면 노출 문구·UI 요소의 현재 상태와 변경 후 상태를 기술합니다.
 * - **API 유형**: 요청·응답의 필수 항목 스펙을 현재/변경 후로 나누어 기술합니다.
 * - kind(UI/API)를 선택하기 전까지 AS-IS / TO-BE 텍스트영역은 표시되지 않습니다.
 *
 * ### 신규(NEW) 분기
 * - 분류(category)에 무관하게 자유 메모 필드만 노출합니다.
 * - 심층 질의 템플릿은 지식저장소 추가 학습 후 제공되므로 `EmptyKbNotice`로 안내합니다.
 *
 * ### AdditionalCheck
 * 모르거나 확정되지 않은 항목은 [잘 모름/추가 확인 필요] 체크박스로 표시할 수 있습니다.
 * 체크된 항목은 요청서에 "추가 확인 필요" 메모로 포함됩니다.
 *
 * ### 파일 첨부
 * 연동 규격서(PDF, XLSX, PNG 등)를 첨부할 수 있습니다.
 * `FileAttachZone`을 통해 파일을 업로드하면 AI가 내용을 참조해 요청서를 보완합니다.
 */

/**
 * 위저드 6단계 컴포넌트 — AI 심층 질의를 위한 추가 정보를 수집합니다.
 *
 * @returns funcTypeCode에 따라 조건부 렌더링된 AS-IS/TO-BE 또는 자유 메모 섹션과
 *          파일 첨부 영역을 포함한 슬라이드 UI.
 *
 * @example
 * ```tsx
 * <Slide6AiDeepDive />
 * ```
 */
// S6 — AI 동적 심층 질의.
// 분기 규칙:
//   ① 기존 서비스 수정·개선 → AS-IS / TO-BE UI (UI: 노출 문구/요소, API: 요청/응답 필수 항목)
//   ② 신규 → 분류 무관하게 자유 메모 (세부 정책은 KB 의존이라 EmptyKbNotice 안내)
//   ③ 모든 항목에 [잘 모름/추가 확인 필요] 체크박스 + 연동 규격서 파일 첨부 자리
export function Slide6AiDeepDive() {
  /** 위저드 전역 상태 및 패치 함수 */
  const { state, patch } = useWizard();
  /** 위저드 데이터 단축 참조 */
  const d = state.data;
  /**
   * 기존 서비스 수정·개선(MODIFY) 여부.
   * `true`이면 AS-IS/TO-BE 섹션을 표시하고, `false`이면 자유 메모 섹션을 표시합니다.
   * funcType 라벨 매칭 대신 yaml 코드(funcTypeCode)로 안정적으로 분기합니다.
   */
  const isModify = d.funcTypeCode === "MODIFY";

  return (
    <SlideShell
      step={6}
      title="상세 3/3"
      description="앞 단계에서 고른 영역에 맞춰, 정책 확정에 필요한 추가 정보를 모읍니다. 모르거나 확정되지 않은 항목은 옆 체크박스로 [추가 확인]에 담아 두세요."
    >
      {/* 분기 ① — 기존 기능 수정/개선(MODIFY)일 때: AS-IS → TO-BE 섹션 */}
      {isModify && (
        <section className="space-y-4">
          <div className="text-xs font-semibold text-foreground">
            AS-IS → TO-BE
          </div>

          {/* 변경 유형 선택 라디오 버튼 — UI(화면) 또는 API(스펙) 중 선택.
              선택한 kind에 따라 AS-IS / TO-BE 입력 필드의 placeholder가 달라집니다. */}
          <RadioGroup
            value={d.s6?.asisTobe?.kind ?? ""}
            onValueChange={(v) =>
              patch({
                s6: {
                  asisTobe: {
                    kind: v as "UI" | "API",
                    // kind 변경 시 기존 입력값을 유지합니다.
                    asis: d.s6?.asisTobe?.asis ?? "",
                    tobe: d.s6?.asisTobe?.tobe ?? "",
                  },
                },
              })
            }
            className="flex gap-3"
          >
            {/* UI 유형: 화면 노출 문구·요소 변경 */}
            <Label
              className={`flex items-center gap-2 rounded-md border px-3 py-2 text-xs cursor-pointer ${
                d.s6?.asisTobe?.kind === "UI"
                  ? "border-primary bg-primary/5 text-primary"
                  : "border-border"
              }`}
            >
              <RadioGroupItem value="UI" />
              UI (화면 노출 문구/요소)
            </Label>
            {/* API 유형: 요청·응답 스펙 변경 */}
            <Label
              className={`flex items-center gap-2 rounded-md border px-3 py-2 text-xs cursor-pointer ${
                d.s6?.asisTobe?.kind === "API"
                  ? "border-primary bg-primary/5 text-primary"
                  : "border-border"
              }`}
            >
              <RadioGroupItem value="API" />
              API (요청/응답 필수 항목)
            </Label>
          </RadioGroup>

          {/* AS-IS / TO-BE 텍스트영역 — kind가 선택된 후에만 표시됩니다. */}
          {d.s6?.asisTobe?.kind && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* AS-IS: 현재 상태 기술 영역.
                  UI 유형이면 현재 화면·문구를, API 유형이면 현재 요청·응답 스펙을 기재합니다. */}
              <div>
                <Label className="text-xs">AS-IS (현재)</Label>
                <Textarea
                  rows={4}
                  value={d.s6.asisTobe.asis}
                  onChange={(e) =>
                    patch({
                      s6: {
                        asisTobe: {
                          kind: d.s6!.asisTobe!.kind,
                          asis: e.target.value,
                          tobe: d.s6!.asisTobe!.tobe,
                        },
                      },
                    })
                  }
                  placeholder={
                    d.s6.asisTobe.kind === "UI"
                      ? "현재 화면이 어떻게 보이는지 / 현재 문구"
                      : "현재 요청·응답 스펙 (필드명, 필수 여부 등)"
                  }
                />
                {/* AS-IS 정확성에 대해 확신이 없을 경우 [추가 확인] 체크박스 */}
                <AdditionalCheck slide={6} field="AS-IS 정확성" />
              </div>
              {/* TO-BE: 변경 후 목표 상태 기술 영역.
                  UI 유형이면 바꾸고 싶은 화면·문구를, API 유형이면 새 요청·응답 스펙을 기재합니다. */}
              <div>
                <Label className="text-xs">TO-BE (변경 후)</Label>
                <Textarea
                  rows={4}
                  value={d.s6.asisTobe.tobe}
                  onChange={(e) =>
                    patch({
                      s6: {
                        asisTobe: {
                          kind: d.s6!.asisTobe!.kind,
                          asis: d.s6!.asisTobe!.asis,
                          tobe: e.target.value,
                        },
                      },
                    })
                  }
                  placeholder={
                    d.s6.asisTobe.kind === "UI"
                      ? "바꾸고 싶은 화면 / 새 문구"
                      : "바뀐 후 요청·응답 스펙"
                  }
                />
                {/* TO-BE 명세 완성도에 대해 확신이 없을 경우 [추가 확인] 체크박스 */}
                <AdditionalCheck slide={6} field="TO-BE 명세 완성도" />
              </div>
            </div>
          )}
        </section>
      )}

      {/* 분기 ② — 신규(NEW)일 때: 분류에 관계없이 자유 메모만 노출.
          심층 질의 템플릿은 지식저장소 추가 학습 완료 후 제공됩니다. */}
      {!isModify && (
        <section className="rounded-lg border border-border bg-secondary/30 p-4 space-y-2">
          <div className="text-xs font-semibold text-foreground">
            추가로 알려주시고 싶은 내용
          </div>
          {/* 선택된 카테고리에 대한 심층 질의 템플릿이 아직 없음을 안내하는 컴포넌트 */}
          <EmptyKbNotice
            message={`'${d.category ?? "선택한 분류"}'에 대한 심층 질의 템플릿은 지식저장소 추가 학습 후 제공됩니다.`}
          />
          {/* 자유 메모 입력 영역 — 신규 개발 관련 추가 정보를 자유롭게 기재합니다. */}
          <Textarea
            rows={4}
            value={d.s6?.freeNotes ?? ""}
            onChange={(e) => patch({ s6: { freeNotes: e.target.value } })}
            placeholder="자유롭게 적어 주세요. 모르는 부분이 있다면 [잘 모름]을 체크해도 됩니다."
          />
          {/* 선택된 분류의 세부 정책에 대해 확인이 필요한 경우 [추가 확인] 체크박스 */}
          <AdditionalCheck slide={6} field={`${d.category ?? "선택 분류"} — 세부 정책`} />
        </section>
      )}

      {/* 분기 ③ — 공통: 연동 규격서 파일 첨부 영역.
          PDF·XLSX·PNG 등 규격서를 첨부하면 AI가 참조하여 요청서 내용을 보완합니다. */}
      <div>
        <Label className="text-xs">연동 규격서 첨부 (선택)</Label>
        <div className="mt-1.5">
          <FileAttachZone />
        </div>
      </div>
    </SlideShell>
  );
}
