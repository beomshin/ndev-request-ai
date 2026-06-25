import { createFileRoute, redirect } from "@tanstack/react-router";

// 루트(/)는 별도 대시보드 없이 새 요청서 작성 화면으로 즉시 이동.
export const Route = createFileRoute("/")({
  beforeLoad: () => {
    throw redirect({ to: "/new" });
  },
});
