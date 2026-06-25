import { createFileRoute, redirect } from "@tanstack/react-router";

// 루트(/)는 별도 대시보드 없이 새 요청서 작성 화면으로 즉시 이동.

/**
 * 루트 인덱스 라우트 (`/`).
 * 별도 랜딩 페이지 없이 `/new`(새 요청서 작성)로 즉시 리다이렉트한다.
 * `beforeLoad`는 컴포넌트 렌더 전에 실행되므로 빈 화면 없이 바로 전환된다.
 */
export const Route = createFileRoute("/")({
  beforeLoad: () => {
    // throw redirect는 TanStack Router의 선언적 리다이렉트 방식.
    // 예외를 던지면 라우터가 이를 잡아 해당 경로로 네비게이션 처리한다.
    throw redirect({ to: "/new" });
  },
});
