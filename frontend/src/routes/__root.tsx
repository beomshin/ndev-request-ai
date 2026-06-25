import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  Outlet,
  Link,
  createRootRouteWithContext,
  useRouter,
} from "@tanstack/react-router";

// SPA 모드 루트 라우트. SSR(shellComponent/HeadContent/Scripts) 의존성 제거됨.
// 글로벌 메타는 index.html에서 처리하고, 페이지별 head는 각 라우트의 head() 사용.

/**
 * 404 페이지 컴포넌트.
 * 존재하지 않는 경로에 접근했을 때 표시되며, 새 요청서 작성 페이지로 이동하는 링크를 제공한다.
 */
function NotFoundComponent() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="max-w-md text-center">
        <h1 className="text-7xl font-bold text-foreground">404</h1>
        <h2 className="mt-4 text-xl font-semibold text-foreground">Page not found</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          요청하신 페이지를 찾을 수 없습니다.
        </p>
        <div className="mt-6">
          <Link
            to="/new"
            className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
          >
            새 요청서 작성으로
          </Link>
        </div>
      </div>
    </div>
  );
}

/**
 * 라우트 에러 경계 컴포넌트.
 * 라우트 로딩 또는 렌더링 중 에러가 발생하면 표시된다.
 * 라우터를 무효화(invalidate)한 뒤 에러 상태를 초기화하는 "다시 시도" 버튼을 제공한다.
 *
 * @param error - 발생한 에러 객체
 * @param reset - 에러 경계 상태를 초기화하는 콜백 함수
 */
function ErrorComponent({ error, reset }: { error: Error; reset: () => void }) {
  console.error(error);
  const router = useRouter();

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="max-w-md text-center">
        <h1 className="text-xl font-semibold tracking-tight text-foreground">
          페이지를 불러오지 못했습니다
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          잠시 후 다시 시도해 주세요.
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <button
            onClick={() => {
              // 라우터 캐시를 무효화하여 최신 상태로 재요청한 뒤 에러 경계를 초기화한다.
              router.invalidate();
              reset();
            }}
            className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
          >
            다시 시도
          </button>
          <a
            href="/"
            className="inline-flex items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-accent"
          >
            홈으로
          </a>
        </div>
      </div>
    </div>
  );
}

/**
 * 루트 라우트 정의.
 * TanStack Router의 컨텍스트로 `queryClient`를 주입받아 하위 라우트 전체에서 사용 가능하게 한다.
 * - `notFoundComponent`: 매칭 라우트 없을 때 렌더
 * - `errorComponent`: 라우트 에러 발생 시 렌더
 */
export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  component: RootComponent,
  notFoundComponent: NotFoundComponent,
  errorComponent: ErrorComponent,
});

/**
 * 루트 컴포넌트.
 * 라우트 컨텍스트에서 `queryClient`를 꺼내 `QueryClientProvider`로 앱 전체를 감싼다.
 * 실제 레이아웃은 `AppLayout`에 위임한다.
 */
function RootComponent() {
  const { queryClient } = Route.useRouteContext();
  return (
    <QueryClientProvider client={queryClient}>
      <AppLayout />
    </QueryClientProvider>
  );
}

/**
 * 앱 전체 레이아웃 컴포넌트.
 * 좌측 고정 사이드바(lg 이상에서만 표시)와 우측 콘텐츠 영역(`<Outlet />`)으로 구성된다.
 * 사이드바에는 주요 네비게이션 항목이 나열되며, 현재 활성 경로는 activeProps로 강조된다.
 */
function AppLayout() {
  /** 사이드바에 표시할 네비게이션 항목 목록. `to`는 라우트 경로, `icon`은 유니코드 문자 아이콘. */
  const navItems = [
    { to: "/new", label: "새 요청서 작성", icon: "✎" },
    { to: "/list", label: "생성 요청서 목록", icon: "▤" },
    { to: "/knowledge", label: "지식 저장소", icon: "◈" },
  ] as const;

  return (
    <div className="flex min-h-screen bg-background font-sans">
      {/* 사이드바 — lg(1024px) 미만 화면에서는 숨김 */}
      <aside className="hidden lg:flex w-64 shrink-0 flex-col border-r border-border bg-card">
        {/* 브랜드 로고 영역 */}
        <div className="flex items-center gap-2 px-6 py-5 border-b border-border">
          <div className="flex h-9 w-9 items-center justify-center rounded-md bg-primary text-primary-foreground font-bold">
            R
          </div>
          <div>
            <div className="text-sm font-semibold text-foreground leading-tight">Req-Genie</div>
            <div className="text-[11px] text-muted-foreground">AI 개발요청서 빌더</div>
          </div>
        </div>
        {/* 내비게이션 메뉴 */}
        <nav className="flex-1 px-3 py-4 space-y-1">
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className="flex items-center gap-3 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-secondary hover:text-foreground transition-colors"
              activeProps={{
                // 현재 경로와 일치하는 항목에 적용되는 활성 스타일
                className:
                  "flex items-center gap-3 rounded-md px-3 py-2 text-sm bg-secondary text-foreground font-medium",
              }}
            >
              <span className="text-base opacity-70">{item.icon}</span>
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>
      {/* 메인 콘텐츠 영역 — 현재 매칭된 라우트의 컴포넌트가 렌더된다 */}
      <main className="flex-1 min-w-0">
        <Outlet />
      </main>
    </div>
  );
}
