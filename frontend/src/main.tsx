import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider, createRouter } from "@tanstack/react-router";
import { routeTree } from "./routeTree.gen";
import "./styles.css";

/**
 * SPA 애플리케이션 진입점(Entry Point).
 *
 * Spring Boot가 단일 JAR로 정적 파일을 서빙하며,
 * `/api/**` 이외의 모든 경로는 `SpaFallbackController`에 의해 `index.html`로 fallback된다.
 * 이를 통해 TanStack Router의 클라이언트 사이드 라우팅이 정상 동작한다.
 */

/**
 * TanStack Query 전역 클라이언트 인스턴스.
 *
 * 모든 `useQuery` / `useMutation` 훅이 이 인스턴스를 통해 캐시를 공유한다.
 * `QueryClientProvider`의 `client` prop으로 전달되어 React 트리 전체에 주입된다.
 */
const queryClient = new QueryClient();

/**
 * TanStack Router 인스턴스.
 *
 * `routeTree`는 `@tanstack/router-vite-plugin`이 파일 기반 라우팅 구조를 분석하여
 * 자동 생성한 `routeTree.gen.ts`에서 가져온다.
 *
 * `context.queryClient`를 router context에 주입하여 라우트 로더에서
 * React Query 캐시에 직접 접근할 수 있도록 한다.
 *
 * `defaultPreloadStaleTime: 0`으로 설정하여 링크 호버 시 항상 최신 데이터를 프리로드한다.
 */
const router = createRouter({
  routeTree,
  context: { queryClient },
  // 링크 프리로드 시 staleTime을 0으로 설정 → 항상 최신 데이터 fetch
  defaultPreloadStaleTime: 0,
});

/**
 * TanStack Router 타입 안전성을 위한 모듈 보강(Module Augmentation).
 *
 * `Register` 인터페이스에 실제 `router` 타입을 등록하여 `useNavigate`, `useParams` 등
 * 라우터 훅에서 완전한 타입 추론이 가능해진다.
 */
declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

/** React 앱을 마운트할 DOM 루트 엘리먼트. `index.html`의 `<div id="root">` */
const rootEl = document.getElementById("root");

// root 엘리먼트가 없으면 즉시 실패 (빌드·HTML 설정 오류 조기 발견)
if (!rootEl) throw new Error("#root 엘리먼트를 찾을 수 없습니다.");

/**
 * React 앱을 DOM에 렌더링한다.
 *
 * 컴포넌트 트리 구성:
 * - `StrictMode`: 개발 환경에서 잠재적 문제를 감지하기 위한 이중 렌더링 활성화.
 * - `QueryClientProvider`: TanStack Query 전역 캐시를 React Context로 제공.
 * - `RouterProvider`: TanStack Router 기반의 클라이언트 사이드 라우팅 제공.
 */
createRoot(rootEl).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
