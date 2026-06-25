import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

/**
 * @file Slide3Basics.tsx
 * @description 위저드 3단계 — 기본·배경 정보 입력 슬라이드.
 *
 * 요청서의 메타 정보(작성자, 부서, 작성일)와 요청 맥락 정보(가맹점 정보, 원천사 정보, 추진 배경)를 수집합니다.
 *
 * @remarks
 * ### 백엔드 매핑
 * 이 슬라이드의 각 필드는 백엔드 DTO에 직접 매핑됩니다.
 * - `author` → `DevRequestRequest.author`
 * - `department` → `DevRequestRequest.department`
 * - `createdAt` → `DevRequestRequest.createdAt` (자동 설정, 수정 불가)
 * - `merchantInfo` · `providerInfo` → 백엔드 LLM 프롬프트 본문에 합쳐서 전달됩니다.
 * - `background` → `DevRequestRequest.background`
 *
 * ### 필수 항목
 * `*` 표시된 작성자, 작성 부서, 추진 배경은 필수 입력 항목입니다.
 *
 * ### 자동 추론
 * 자동 추론 가능한 정보(기술 스펙, API 명세 등)는 이후 AI 단계에서 채워집니다.
 */

/**
 * 위저드 3단계 컴포넌트 — 요청서의 기본 정보 및 추진 배경을 입력합니다.
 *
 * @returns 작성자·부서·작성일·가맹점·원천사·추진배경 입력 폼을 포함한 슬라이드 UI.
 *
 * @example
 * ```tsx
 * <Slide3Basics />
 * ```
 */
// S3 — 기본·배경 정보. 백엔드 DTO에 직접 매핑되는 필드 + 자유 텍스트(가맹점/원천사)는 본문에 합쳐 전달.
export function Slide3Basics() {
  /** 위저드 전역 상태 및 패치 함수 */
  const { state, patch } = useWizard();
  /** 위저드 데이터 단축 참조 */
  const d = state.data;

  return (
    <SlideShell
      step={3}
      title="기본·배경 정보를 알려 주세요"
      description="작성자/부서와 이 요청이 왜 필요한지(추진배경)를 적어 주세요. 자동 추론 가능한 정보는 다음 단계에서 AI가 채웁니다."
    >
      {/* 상단 2열 그리드: 작성자 / 작성 부서 / 작성일 / 가맹점 정보 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* 작성자 — 필수(*). 요청서 담당자 이름을 입력합니다. */}
        <div>
          <Label htmlFor="author" className="text-xs">작성자 *</Label>
          <Input
            id="author"
            value={d.author ?? ""}
            onChange={(e) => patch({ author: e.target.value })}
            placeholder="홍길동"
          />
        </div>
        {/* 작성 부서 — 필수(*). 요청을 제출하는 팀 또는 부서명을 입력합니다. */}
        <div>
          <Label htmlFor="department" className="text-xs">작성 부서 *</Label>
          <Input
            id="department"
            value={d.department ?? ""}
            onChange={(e) => patch({ department: e.target.value })}
            placeholder="PG결제개발실"
          />
        </div>
        {/* 작성일 — 위저드 초기화 시 자동으로 설정되며 사용자가 수정할 수 없습니다. */}
        <div>
          <Label className="text-xs">작성일 (자동)</Label>
          <Input value={d.createdAt ?? ""} disabled className="bg-secondary/40" />
        </div>
        {/* 가맹점 정보 — 선택. 연관된 가맹점의 상호명 또는 MID를 입력합니다. */}
        <div>
          <Label htmlFor="merchantInfo" className="text-xs">가맹점 정보</Label>
          <Input
            id="merchantInfo"
            value={d.merchantInfo ?? ""}
            onChange={(e) => patch({ merchantInfo: e.target.value })}
            placeholder="가맹점 상호명/MID"
          />
        </div>
        {/* 원천사 정보 — 선택. 결제 수단을 제공하는 원천사(PG사, 간편결제사 등)를 입력합니다.
            전체 너비(sm:col-span-2)를 사용합니다. */}
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

      {/* 추진 배경 — 필수(*). 이 개발 요청이 왜 필요한지 비즈니스 맥락을 자유롭게 서술합니다.
          LLM 프롬프트의 핵심 컨텍스트로 사용됩니다. */}
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
