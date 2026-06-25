import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import { AdditionalCheck } from "../shared/AdditionalCheck";
import { EmptyKbNotice } from "../shared/EmptyKbNotice";
import { FileAttachZone } from "../shared/FileAttachZone";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

// S6 — AI 동적 심층 질의.
// 분기 규칙:
//   ① 기존 서비스 수정·개선 → AS-IS / TO-BE UI (UI: 노출 문구/요소, API: 요청/응답 필수 항목)
//   ② 신규 → 분류 무관하게 자유 메모 (세부 정책은 KB 의존이라 EmptyKbNotice 안내)
//   ③ 모든 항목에 [잘 모름/추가 확인 필요] 체크박스 + 연동 규격서 파일 첨부 자리
export function Slide6AiDeepDive() {
  const { state, patch } = useWizard();
  const d = state.data;
  // funcType 라벨 매칭 대신 yaml 코드(funcTypeCode)로 안정적으로 분기.
  const isModify = d.funcTypeCode === "MODIFY";

  return (
    <SlideShell
      step={6}
      title="상세 3/3"
      description="앞 단계에서 고른 영역에 맞춰, 정책 확정에 필요한 추가 정보를 모읍니다. 모르거나 확정되지 않은 항목은 옆 체크박스로 [추가 확인]에 담아 두세요."
    >
      {/* 분기 ② — 기존 기능 수정/개선 */}
      {isModify && (
        <section className="space-y-4">
          <div className="text-xs font-semibold text-foreground">
            AS-IS → TO-BE
          </div>
          <RadioGroup
            value={d.s6?.asisTobe?.kind ?? ""}
            onValueChange={(v) =>
              patch({
                s6: {
                  asisTobe: {
                    kind: v as "UI" | "API",
                    asis: d.s6?.asisTobe?.asis ?? "",
                    tobe: d.s6?.asisTobe?.tobe ?? "",
                  },
                },
              })
            }
            className="flex gap-3"
          >
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

          {d.s6?.asisTobe?.kind && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
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
                <AdditionalCheck slide={6} field="AS-IS 정확성" />
              </div>
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
                <AdditionalCheck slide={6} field="TO-BE 명세 완성도" />
              </div>
            </div>
          )}
        </section>
      )}

      {/* 신규(NEW) 분기 — 분류에 관계없이 자유 메모만 노출 */}
      {!isModify && (
        <section className="rounded-lg border border-border bg-secondary/30 p-4 space-y-2">
          <div className="text-xs font-semibold text-foreground">
            추가로 알려주시고 싶은 내용
          </div>
          <EmptyKbNotice
            message={`'${d.category ?? "선택한 분류"}'에 대한 심층 질의 템플릿은 지식저장소 추가 학습 후 제공됩니다.`}
          />
          <Textarea
            rows={4}
            value={d.s6?.freeNotes ?? ""}
            onChange={(e) => patch({ s6: { freeNotes: e.target.value } })}
            placeholder="자유롭게 적어 주세요. 모르는 부분이 있다면 [잘 모름]을 체크해도 됩니다."
          />
          <AdditionalCheck slide={6} field={`${d.category ?? "선택 분류"} — 세부 정책`} />
        </section>
      )}

      {/* 첨부 자리 */}
      <div>
        <Label className="text-xs">연동 규격서 첨부 (선택)</Label>
        <div className="mt-1.5">
          <FileAttachZone />
        </div>
      </div>
    </SlideShell>
  );
}
