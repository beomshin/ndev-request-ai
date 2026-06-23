import { createFileRoute } from "@tanstack/react-router";
import { WizardShell } from "@/components/wizard/WizardShell";

// 새 요청서 작성 라우트 — Slide Wizard 6단계로 진행.
// 위저드 메인/슬라이드 로직은 components/wizard/* 에 있다.
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

function NewRequest() {
  return <WizardShell />;
}
