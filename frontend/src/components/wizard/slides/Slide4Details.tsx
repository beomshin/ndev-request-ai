import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

// S4 — 상세 요청 1/2: 목표일정 + 근거 + 서비스명
export function Slide4Details() {
  const { state, patch } = useWizard();
  const d = state.data;

  return (
    <SlideShell
      step={4}
      title="상세 요청 정보 (1/2)"
      description="언제까지, 무엇을 만들지 알려 주세요."
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="serviceName" className="text-xs">개발 서비스명 *</Label>
          <Input
            id="serviceName"
            value={d.serviceName ?? ""}
            onChange={(e) => patch({ serviceName: e.target.value })}
            placeholder="예: 신규 카드 결제 모듈"
          />
        </div>
        <div>
          <Label htmlFor="targetSchedule" className="text-xs">목표 일정 * (YYYY-MM-DD)</Label>
          <Input
            id="targetSchedule"
            value={d.targetSchedule ?? ""}
            onChange={(e) => patch({ targetSchedule: e.target.value })}
            placeholder="2026-09-30"
          />
          <p className="text-[11px] text-muted-foreground mt-1">
            YYYYMMDD 형식으로 입력해도 자동으로 변환합니다.
          </p>
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="scheduleRationale" className="text-xs">목표 일정 근거 (선택)</Label>
          <Input
            id="scheduleRationale"
            value={d.scheduleRationale ?? ""}
            onChange={(e) => patch({ scheduleRationale: e.target.value })}
            placeholder="예: 가맹점 프로모션 런칭일 / 카드사 규격 변경 시점"
          />
        </div>
      </div>
    </SlideShell>
  );
}
