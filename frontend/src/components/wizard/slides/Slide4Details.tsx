import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

/**
 * @file Slide4Details.tsx
 * @description 위저드 4단계 — 상세 요청 정보 1/3 슬라이드.
 *
 * 개발 서비스명, 목표 일정, 일정 근거를 수집합니다.
 * 전체 3개의 상세 요청 슬라이드(S4, S5, S6) 중 첫 번째 슬라이드입니다.
 *
 * @remarks
 * ### 포함 필드
 * - `serviceName`: 개발 서비스의 이름. 요청서 제목 역할을 합니다.
 * - `targetSchedule`: 완료 목표 날짜. `YYYY-MM-DD` 형식을 권장하며,
 *   `YYYYMMDD` 형식으로 입력해도 자동 변환됩니다.
 * - `scheduleRationale`: 목표 일정의 이유(예: 프로모션 런칭일, 카드사 규격 변경 시점).
 *   일정의 타당성을 뒷받침하는 선택 항목입니다.
 *
 * ### 필수/선택
 * `serviceName`과 `targetSchedule`은 필수(`*`) 항목입니다.
 * `scheduleRationale`은 선택 항목입니다.
 */

/**
 * 위저드 4단계 컴포넌트 — 개발 서비스명과 목표 일정을 입력합니다.
 *
 * @returns 서비스명·목표 일정·일정 근거 입력 폼을 포함한 슬라이드 UI.
 *
 * @example
 * ```tsx
 * <Slide4Details />
 * ```
 */
// S4 — 상세 요청 1/2: 목표일정 + 근거 + 서비스명
export function Slide4Details() {
  /** 위저드 전역 상태 및 패치 함수 */
  const { state, patch } = useWizard();
  /** 위저드 데이터 단축 참조 */
  const d = state.data;

  return (
    <SlideShell
      step={4}
      title="상세 요청 정보 (1/3)"
      description="언제까지, 무엇을 만들지 알려 주세요."
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* 개발 서비스명 — 필수(*). 이 요청서가 다루는 개발 서비스 또는 기능의 이름.
            완성된 요청서의 제목으로 사용됩니다. */}
        <div>
          <Label htmlFor="serviceName" className="text-xs">개발 서비스명 *</Label>
          <Input
            id="serviceName"
            value={d.serviceName ?? ""}
            onChange={(e) => patch({ serviceName: e.target.value })}
            placeholder="예: 신규 카드 결제 모듈"
          />
        </div>
        {/* 목표 일정 — 필수(*). 개발 완료를 목표하는 날짜.
            YYYY-MM-DD 형식 권장. YYYYMMDD로 입력 시 자동 변환됩니다. */}
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
        {/* 목표 일정 근거 — 선택. 위 일정이 왜 그 날짜인지 이유를 기재합니다.
            예: 가맹점 프로모션 런칭일, 카드사 규격 변경 시점 등.
            전체 너비(sm:col-span-2)를 사용합니다. */}
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
