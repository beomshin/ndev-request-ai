import * as React from "react";

/**
 * 모바일 뷰포트 판단 기준 너비(픽셀).
 *
 * 이 값 미만이면 모바일로 간주한다. (`< 768px`)
 * Tailwind CSS의 `md` 브레이크포인트와 동일한 기준을 사용한다.
 */
const MOBILE_BREAKPOINT = 768;

/**
 * 현재 뷰포트가 모바일 크기인지 감지하는 커스텀 훅.
 *
 * `window.matchMedia`를 사용하여 브라우저 뷰포트 너비 변화를 구독한다.
 * 뷰포트 너비가 {@link MOBILE_BREAKPOINT}(768px) 미만이면 `true`를 반환한다.
 *
 * SSR 환경에서는 초기값이 `undefined`로 시작하지만, `!!` 연산으로 `false`로 변환되어
 * 서버와 클라이언트 간 hydration 불일치를 방지한다.
 *
 * @returns 현재 뷰포트가 모바일 크기이면 `true`, 아니면 `false`.
 *
 * @example
 * ```tsx
 * function Navbar() {
 *   const isMobile = useIsMobile();
 *   return isMobile ? <MobileMenu /> : <DesktopMenu />;
 * }
 * ```
 */
export function useIsMobile() {
  // 초기값을 undefined로 설정하여 첫 렌더링 전 미확정 상태를 표현
  const [isMobile, setIsMobile] = React.useState<boolean | undefined>(undefined);

  React.useEffect(() => {
    // matchMedia로 브레이크포인트 미만 여부를 감지하는 미디어 쿼리 객체 생성
    const mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`);

    /**
     * 미디어 쿼리 상태가 변경될 때 호출되는 핸들러.
     * `window.innerWidth`를 직접 확인하여 정확한 픽셀 값 기반으로 판단한다.
     */
    const onChange = () => {
      setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);
    };

    // 뷰포트 크기 변경 이벤트 구독
    mql.addEventListener("change", onChange);

    // 컴포넌트 마운트 시 현재 뷰포트 크기로 초기 상태 설정
    setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);

    // 언마운트 시 이벤트 리스너 해제하여 메모리 누수 방지
    return () => mql.removeEventListener("change", onChange);
  }, []); // 의존성 배열이 비어 있으므로 마운트/언마운트 시 한 번만 실행

  // undefined 초기값을 boolean으로 강제 변환 (!! 연산: undefined → false)
  return !!isMobile;
}
