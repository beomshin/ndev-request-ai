import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

// S5 — 상세 요청 2/2 + 비즈니스 임팩트.
// problemAndImprovement만 백엔드 DTO에 직접 들어가고, 나머지는 본문 섹션으로 합쳐 전달.
export function Slide5Impact() {
  const { state, patch } = useWizard();
  const d = state.data;

  return (
    <SlideShell
      step={5}
      title="상세 요청 정보 (2/2) · 비즈니스 임팩트"
      description="개선이 필요한 문제와 이 개발로 기대되는 효과를 적어 주세요."
    >
      <div>
        <Label htmlFor="problemAndImprovement" className="text-xs">
          문제점 / 개선점 *
        </Label>
        <Textarea
          id="problemAndImprovement"
          rows={5}
          value={d.problemAndImprovement ?? ""}
          onChange={(e) => patch({ problemAndImprovement: e.target.value })}
          placeholder="현재 어떤 문제가 있고, 무엇을 어떻게 개선하고 싶은지 구체적으로 적어 주세요."
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="problemDetectionMethod" className="text-xs">
            문제점 확인 방법 (선택)
          </Label>
          <Input
            id="problemDetectionMethod"
            value={d.problemDetectionMethod ?? ""}
            onChange={(e) => patch({ problemDetectionMethod: e.target.value })}
            placeholder="예: 운영 로그 / 가맹점 민원 통계 / VOC 분석"
          />
        </div>
        <div>
          <Label htmlFor="occurrenceFrequency" className="text-xs">
            발생 빈도 (정량, 선택)
          </Label>
          <Input
            id="occurrenceFrequency"
            value={d.occurrenceFrequency ?? ""}
            onChange={(e) => patch({ occurrenceFrequency: e.target.value })}
            placeholder="예: 월 12건 / 결제 시도의 약 0.3%"
          />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="competitorInfo" className="text-xs">경쟁사 정보 (선택)</Label>
          <Input
            id="competitorInfo"
            value={d.competitorInfo ?? ""}
            onChange={(e) => patch({ competitorInfo: e.target.value })}
            placeholder="예: A사·B사 이미 해당 결제 수단 지원 중 (전환율 +6.4%p)"
          />
        </div>
        <div>
          <Label htmlFor="expectedRevenue" className="text-xs">예상 수익 (선택)</Label>
          <Input
            id="expectedRevenue"
            value={d.expectedRevenue ?? ""}
            onChange={(e) => patch({ expectedRevenue: e.target.value })}
            placeholder="예: 월 결제액 3.2억 증가 예상"
          />
        </div>
        <div>
          <Label htmlFor="expectedLoss" className="text-xs">예상 손해 / 리스크 (선택)</Label>
          <Input
            id="expectedLoss"
            value={d.expectedLoss ?? ""}
            onChange={(e) => patch({ expectedLoss: e.target.value })}
            placeholder="예: 추가 PG 수수료 월 0.4억"
          />
        </div>
      </div>
    </SlideShell>
  );
}
