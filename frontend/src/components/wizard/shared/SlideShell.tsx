import { type ReactNode } from "react";

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
      <div className="mb-6">
        <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
          STEP {step} / 6
        </div>
        <h2 className="mt-1 text-lg font-semibold text-foreground tracking-tight">{title}</h2>
        {description && (
          <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">{description}</p>
        )}
      </div>
      <div className="space-y-5">{children}</div>
    </div>
  );
}
