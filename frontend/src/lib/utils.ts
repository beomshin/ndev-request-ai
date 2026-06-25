import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * 여러 CSS 클래스 값을 조건부로 합산하고 Tailwind CSS 충돌을 자동으로 해결하는 유틸리티 함수.
 *
 * 내부적으로 `clsx`로 조건부 클래스를 처리한 뒤 `tailwind-merge`로 중복·충돌 클래스를 정리한다.
 * 동일한 Tailwind 유틸리티 그룹(예: `p-2`와 `p-4`)에서 마지막으로 전달된 값이 우선 적용된다.
 *
 * @param inputs - 클래스 값의 가변 인수. 문자열·객체·배열·`undefined`·`false` 등
 *   `clsx`가 지원하는 모든 형식을 허용한다.
 * @returns 병합 및 중복 제거된 최종 CSS 클래스 문자열.
 *
 * @example
 * ```ts
 * cn("px-2 py-1", "px-4")
 * // → "py-1 px-4"  (px-2가 px-4로 대체됨)
 *
 * cn("flex", isActive && "bg-blue-500", "text-sm")
 * // → "flex text-sm" (isActive가 false이면 bg-blue-500 제외)
 * ```
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
