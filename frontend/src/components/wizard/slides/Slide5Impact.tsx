import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { SlideShell } from "../shared/SlideShell";
import { useWizard } from "../WizardContext";

/**
 * @file Slide5Impact.tsx
 * @description 위저드 5단계 — 상세 요청 정보 2/3 및 비즈니스 임팩트 슬라이드.
 *
 * 개발 요청의 문제점·개선점과 기대 효과(비즈니스 임팩트)를 수집합니다.
 *
 * @remarks
 * ### funcTypeCode에 따른 조건부 렌더링
 * 이 슬라이드는 `funcTypeCode`(NEW/MODIFY)에 따라 표시 내용이 달라집니다.
 *
 * | 필드 | NEW (신규) | MODIFY (수정) |
 * |------|-----------|--------------|
 * | 문제점 / 개선점 | 숨김 | 표시 (필수) |
 * | 문제점 확인 방법 | 숨김 | 표시 (선택) |
 * | 발생 빈도 | 숨김 | 표시 (선택) |
 * | 경쟁사 정보 | 표시 (선택) | 표시 (선택) |
 * | 예상 수익 | 표시 (선택) | 표시 (선택) |
 * | 예상 손해/리스크 | 표시 (선택) | 표시 (선택) |
 *
 * ### 백엔드 @NotBlank 제약 처리
 * 백엔드 `DevRequestRequest.problemAndImprovement`는 `@NotBlank` 제약이 있어 빈 값을 전송할 수 없습니다.
 * NEW 유형으로 제출 시 `submit.ts`에서 자동으로 `"신규 서비스 개발"` 문자열을 채워 보냅니다.
 */

/**
 * 위저드 5단계 컴포넌트 — 문제점·개선점과 비즈니스 임팩트를 입력합니다.
 *
 * @returns funcTypeCode에 따라 조건부 렌더링된 임팩트 입력 폼을 포함한 슬라이드 UI.
 *
 * @example
 * ```tsx
 * <Slide5Impact />
 * ```
 */
// S5 — 상세 요청 2/2 + 비즈니스 임팩트.
// funcType=NEW(신규)면 "문제점/개선점"·"발생 빈도" 필드를 숨긴다 — 신규 개발엔 해당 없음.
//   - 백엔드 DevRequestRequest.problemAndImprovement는 @NotBlank라 빈값 못 보냄
//     → submit.ts에서 NEW일 때 "신규 서비스 개발"로 자동 채워 보낸다.
export function Slide5Impact() {
  /** 위저드 전역 상태 및 패치 함수 */
  const { state, patch } = useWizard();
  /** 위저드 데이터 단축 참조 */
  const d = state.data;
  /**
   * 신규 개발(NEW) 여부.
   * `true`이면 문제점·발생 빈도 등 수정 전용 필드를 숨깁니다.
   */
  const isNew = d.funcTypeCode === "NEW";

  return (
    <SlideShell
      step={5}
      // NEW/MODIFY에 따라 슬라이드 제목을 다르게 표시합니다.
      title={isNew ? "비즈니스 임팩트 (2/3)" : "상세 요청 정보 (2/3) · 비즈니스 임팩트"}
      description={
        isNew
          ? "신규 개발의 기대 효과와 경쟁 환경을 적어 주세요. (문제점·발생 빈도는 기존 서비스 수정 시에만 입력)"
          : "개선이 필요한 문제와 이 개발로 기대되는 효과를 적어 주세요."
      }
    >
      {/* 문제점/개선점 — MODIFY(기존 서비스 수정)일 때만 노출.
          백엔드 @NotBlank 제약으로 NEW 제출 시 submit.ts에서 자동 placeholder를 채웁니다. */}
      {!isNew && (
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
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* MODIFY 전용 필드 그룹 — NEW에서는 해당 사항이 없어 숨깁니다. */}
        {!isNew && (
          <>
            {/* 문제점 확인 방법 — 선택. 문제를 어떻게 인지했는지 방법을 기재합니다.
                예: 운영 로그 분석, 가맹점 민원 통계, VOC 분석 등. */}
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
            {/* 발생 빈도 — 선택. 문제가 얼마나 자주 발생하는지 정량적으로 기재합니다.
                정량 수치를 포함하면 우선순위 산정에 도움이 됩니다. */}
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
          </>
        )}

        {/* 경쟁사 정보 — NEW/MODIFY 공통 선택 항목.
            경쟁사 현황을 기재하면 비즈니스 긴급도 판단에 참고됩니다.
            전체 너비(sm:col-span-2)를 사용합니다. */}
        <div className="sm:col-span-2">
          <Label htmlFor="competitorInfo" className="text-xs">경쟁사 정보 (선택)</Label>
          <Input
            id="competitorInfo"
            value={d.competitorInfo ?? ""}
            onChange={(e) => patch({ competitorInfo: e.target.value })}
            placeholder="예: A사·B사 이미 해당 결제 수단 지원 중 (전환율 +6.4%p)"
          />
        </div>
        {/* 예상 수익 — 선택. 이 개발로 인해 기대되는 매출 또는 결제액 증가를 기재합니다. */}
        <div>
          <Label htmlFor="expectedRevenue" className="text-xs">예상 수익 (선택)</Label>
          <Input
            id="expectedRevenue"
            value={d.expectedRevenue ?? ""}
            onChange={(e) => patch({ expectedRevenue: e.target.value })}
            placeholder="예: 월 결제액 3.2억 증가 예상"
          />
        </div>
        {/* 예상 손해 / 리스크 — 선택. 이 개발로 인해 발생할 수 있는 비용·리스크를 기재합니다.
            예: 추가 PG 수수료, 보안 취약점 노출 위험 등. */}
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
