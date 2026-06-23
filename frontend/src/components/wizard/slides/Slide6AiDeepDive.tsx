import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import { useCatalog } from "../catalog";
import { AdditionalCheck } from "../shared/AdditionalCheck";
import { EmptyKbNotice } from "../shared/EmptyKbNotice";
import { FileAttachZone } from "../shared/FileAttachZone";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

// S6 — AI 동적 심층 질의.
// 분기 규칙(프롬프트 요구사항):
//   ① funcType === '신규' && category === 'pg표준결제창'
//     → (1) 지불수단 노출(=catalog의 세부유형) (2) 카드 선택 (3) 할부/포인트/프로모션
//   ② funcType === '기존 서비스 수정·개선'
//     → AS-IS/TO-BE UI (UI: 노출 문구/요소, API: 요청/응답 필수 항목)
//   ③ (3)번 이후 세부 정책 질문은 지식저장소 의존 → 데이터 없으면 EmptyKbNotice
//   ④ 모든 항목에 [잘 모름/추가 확인 필요] 체크박스
//   ⑤ 연동 규격서 파일 첨부 자리
export function Slide6AiDeepDive() {
  const { state, patch } = useWizard();
  const d = state.data;
  const { data: catalog } = useCatalog();

  const isModify = (d.funcType ?? "").includes("수정") || (d.funcType ?? "").includes("개선");
  const isPgStdPay = (d.category ?? "").includes("pg표준결제창");

  // 카탈로그에서 카드/계좌이체 등 지불수단 후보를 다시 꺼낸다 (S2에서 골랐던 그 트리)
  const paymentMethods =
    catalog?.categories.find((c) => c.label === d.category)?.subTypes ?? [];

  return (
    <SlideShell
      step={6}
      title="AI 심층 질의 (마지막)"
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

      {/* 분기 ① — 결제창 신규 */}
      {!isModify && isPgStdPay && (
        <section className="space-y-5">
          {/* (1) 지불수단 노출 */}
          <div>
            <div className="text-xs font-semibold text-foreground mb-2">
              (1) 어떤 지불수단을 노출할까요?
            </div>
            <div className="flex flex-wrap gap-2">
              {paymentMethods.length > 0 ? (
                paymentMethods.map((m) => {
                  const selected = d.s6?.paymentMethod === m.label;
                  return (
                    <button
                      key={m.code}
                      type="button"
                      onClick={() =>
                        patch({
                          s6: {
                            paymentMethod: m.label,
                            // 지불수단 바꾸면 하위 옵션 리셋
                            cardOptions: undefined,
                          },
                        })
                      }
                      className={`text-xs rounded-full border px-3 py-1.5 transition ${
                        selected
                          ? "bg-primary text-primary-foreground border-primary"
                          : "bg-card text-foreground border-border hover:border-primary/40"
                      }`}
                    >
                      {m.label}
                    </button>
                  );
                })
              ) : (
                <EmptyKbNotice message="카탈로그에 등록된 지불수단이 없습니다." />
              )}
            </div>
            <AdditionalCheck slide={6} field="지불수단 노출 범위 확정 여부" />
          </div>

          {/* (2)~(3) 카드 선택 시 옵션 */}
          {d.s6?.paymentMethod === "카드" && (
            <div className="space-y-4 rounded-lg border border-border bg-secondary/30 p-4">
              <div className="text-xs font-semibold text-foreground">
                (2)·(3) 카드 결제 옵션
              </div>

              {/* 할부 개월 */}
              <div>
                <Label className="text-xs">할부 개월 (복수 선택)</Label>
                <div className="mt-1.5 flex flex-wrap gap-2">
                  {["일시불", "2개월", "3개월", "6개월", "12개월"].map((opt) => {
                    const checked = d.s6?.cardOptions?.installments?.includes(opt) ?? false;
                    return (
                      <button
                        key={opt}
                        type="button"
                        onClick={() => {
                          const prev = d.s6?.cardOptions?.installments ?? [];
                          const next = checked
                            ? prev.filter((x) => x !== opt)
                            : [...prev, opt];
                          patch({ s6: { cardOptions: { installments: next } } });
                        }}
                        className={`text-xs rounded-md border px-2.5 py-1 transition ${
                          checked
                            ? "bg-primary text-primary-foreground border-primary"
                            : "bg-card text-foreground border-border hover:border-primary/40"
                        }`}
                      >
                        {opt}
                      </button>
                    );
                  })}
                </div>
                <AdditionalCheck slide={6} field="할부 정책" />
              </div>

              {/* 포인트/머니 사용 */}
              <div>
                <Label className="text-xs flex items-center gap-2">
                  <Checkbox
                    checked={!!d.s6?.cardOptions?.usePoint}
                    onCheckedChange={(v) =>
                      patch({ s6: { cardOptions: { usePoint: v === true } } })
                    }
                  />
                  포인트 / 머니 동시 사용 허용
                </Label>
                <AdditionalCheck slide={6} field="포인트·머니 사용 정책" />
              </div>

              {/* 프로모션 쿠폰 */}
              <div>
                <Label className="text-xs flex items-center gap-2">
                  <Checkbox
                    checked={!!d.s6?.cardOptions?.usePromotion}
                    onCheckedChange={(v) =>
                      patch({ s6: { cardOptions: { usePromotion: v === true } } })
                    }
                  />
                  프로모션 쿠폰 / 즉시할인 적용
                </Label>
                <AdditionalCheck slide={6} field="프로모션·즉시할인 정책" />
              </div>

              {/* 그 이후 세부 정책은 KB 데이터 없을 때 placeholder */}
              <div className="pt-2 border-t border-border/70">
                <EmptyKbNotice />
              </div>
            </div>
          )}

          {/* 카드가 아닌 지불수단 선택 시 — 해당 수단 정책은 KB 학습 필요 */}
          {d.s6?.paymentMethod && d.s6.paymentMethod !== "카드" && (
            <div className="rounded-lg border border-border bg-secondary/30 p-4">
              <EmptyKbNotice
                message={`'${d.s6.paymentMethod}' 세부 정책 질문지는 지식저장소 추가 학습 필요`}
              />
            </div>
          )}
        </section>
      )}

      {/* 그 외(API, 해외결제, 기타 등) — KB 의존이라 자유 메모만 */}
      {!isModify && !isPgStdPay && (
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
