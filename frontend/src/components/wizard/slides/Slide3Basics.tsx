import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

// S3 — 기본·배경 정보. 백엔드 DTO에 직접 매핑되는 필드 + 자유 텍스트(가맹점/원천사)는 본문에 합쳐 전달.
export function Slide3Basics() {
  const { state, patch } = useWizard();
  const d = state.data;

  return (
    <SlideShell
      step={3}
      title="기본·배경 정보를 알려 주세요"
      description="작성자/부서와 이 요청이 왜 필요한지(추진배경)를 적어 주세요. 자동 추론 가능한 정보는 다음 단계에서 AI가 채웁니다."
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label htmlFor="author" className="text-xs">작성자 *</Label>
          <Input
            id="author"
            value={d.author ?? ""}
            onChange={(e) => patch({ author: e.target.value })}
            placeholder="홍길동"
          />
        </div>
        <div>
          <Label htmlFor="department" className="text-xs">작성 부서 *</Label>
          <Input
            id="department"
            value={d.department ?? ""}
            onChange={(e) => patch({ department: e.target.value })}
            placeholder="PG결제개발실"
          />
        </div>
        <div>
          <Label className="text-xs">작성일 (자동)</Label>
          <Input value={d.createdAt ?? ""} disabled className="bg-secondary/40" />
        </div>
        <div>
          <Label htmlFor="merchantInfo" className="text-xs">가맹점 정보</Label>
          <Input
            id="merchantInfo"
            value={d.merchantInfo ?? ""}
            onChange={(e) => patch({ merchantInfo: e.target.value })}
            placeholder="가맹점 상호명/MID"
          />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="providerInfo" className="text-xs">원천사 정보 (선택)</Label>
          <Input
            id="providerInfo"
            value={d.providerInfo ?? ""}
            onChange={(e) => patch({ providerInfo: e.target.value })}
            placeholder="예: 카카오페이 / 토스페이먼츠 등"
          />
        </div>
      </div>

      <div>
        <Label htmlFor="background" className="text-xs">추진 배경 *</Label>
        <Textarea
          id="background"
          rows={4}
          value={d.background ?? ""}
          onChange={(e) => patch({ background: e.target.value })}
          placeholder="이 개발이 왜 필요한지 한두 문단으로 설명해 주세요. 비즈니스 언어로 적어도 괜찮습니다."
        />
      </div>
    </SlideShell>
  );
}
