/**
 * @file SlideShell.tsx
 * @description 모든 위저드 슬라이드가 공통으로 사용하는 카드 레이아웃 셸 컴포넌트.
 *
 * 슬라이드별 제목, 설명, 단계 번호를 일관된 디자인 토큰으로 렌더하고,
 * 그 안에 각 슬라이드의 고유 콘텐츠(`children`)를 배치한다.
 *
 * @remarks
 * 비주얼 토큰은 Lovable의 카드 스타일(`bg-card`, `border-border`, `rounded-xl`,
 * `shadow-[var(--shadow-card)]`)을 사용한다.
 * 새 슬라이드를 추가할 때 이 컴포넌트로 감싸면 시각적 일관성이 자동으로 유지된다.
 */

import { type ReactNode } from "react";

/**
 * 위저드 슬라이드 공통 카드 셸 컴포넌트.
 * 모든 슬라이드는 이 컴포넌트를 최상위 래퍼로 사용해야 한다.
 *
 * @param props.step - 슬라이드 순서 번호 (1-based, "STEP N / 6" 형식으로 표시)
 * @param props.title - 슬라이드 주 제목
 * @param props.description - 슬라이드 부연 설명 (선택, 있으면 제목 아래 소문자로 표시)
 * @param props.children - 슬라이드 고유 콘텐츠 (폼 필드, 선택지 등)
 *
 * @example
 * <SlideShell step={1} title="기능 유형 선택" description="개발 요청의 성격을 선택하세요.">
 *   <FuncTypeSelector />
 * </SlideShell>
 */
// 모든 슬라이드가 공통으로 쓰는 카드 셸. 비주얼 톤은 Lovable의 카드 토큰(bg-card/border-border/rounded-xl) 사용.
export function SlideShell({
  step,
  title,
  description,
  children,
}: {
  step: number;
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-6 sm:p-8 shadow-[var(--shadow-card)]">
      {/* 슬라이드 헤더 — 단계 번호, 제목, 설명 */}
      <div className="mb-6">
        {/* 단계 표시 (예: "STEP 3 / 6") — 소문자 강조체로 표시 */}
        <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
          STEP {step} / 6
        </div>
        <h2 className="mt-1 text-lg font-semibold text-foreground tracking-tight">{title}</h2>
        {/* 설명이 제공된 경우에만 렌더 */}
        {description && (
          <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">{description}</p>
        )}
      </div>
      {/* 슬라이드 고유 콘텐츠 — 항목 간 일정 간격 유지 */}
      <div className="space-y-5">{children}</div>
    </div>
  );
}
