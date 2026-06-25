import { createFileRoute } from "@tanstack/react-router";
import { WizardShell } from "@/components/wizard/WizardShell";

// 새 요청서 작성 라우트 — Slide Wizard 6단계로 진행.
// 위저드 메인/슬라이드 로직은 components/wizard/* 에 있다.

/**
 * `/new` 라우트 정의.
 * 페이지 `<head>` 메타(title, description)를 선언하고,
 * `NewRequest` 컴포넌트를 라우트 컴포넌트로 등록한다.
 */
export const Route = createFileRoute("/new")({
  head: () => ({
    meta: [
      { title: "새 요청서 작성 · Req-Genie" },
      {
        name: "description",
        content: "AI 위저드를 따라 결제 도메인 개발요청서를 단계별로 작성합니다.",
      },
    ],
  }),
  component: NewRequest,
});

/**
 * 새 요청서 작성 페이지 컴포넌트.
 * 6단계 슬라이드 위저드(`WizardShell`)를 렌더링한다.
 * 위저드 내부 상태 관리 및 단계 전환 로직은 `WizardShell`에 캡슐화되어 있다.
 */
function NewRequest() {
  return <WizardShell />;
}
