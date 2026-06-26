/**
 * @file EmptyKbNotice.tsx
 * @description 지식저장소(KB) 데이터가 없을 때 표시하는 안내 메시지 컴포넌트.
 *
 * 슬라이드 내에서 관련 지식저장소 정보가 없는 경우 작은 회색 텍스트로
 * "현재 참조 가능한 정보가 없음" 안내를 노출한다.
 * 프롬프트 요구사항(`design.md §9`)에 따라 반드시 이 컴포넌트를 사용해야 한다.
 */

/**
 * 지식저장소 데이터가 없음을 알리는 인라인 안내 컴포넌트.
 *
 * @param props.message - 표시할 안내 문구. 생략 시 기본값을 사용한다.
 *
 * @example
 * // 기본 메시지 사용
 * <EmptyKbNotice />
 *
 * @example
 * // 커스텀 메시지
 * <EmptyKbNotice message="해당 카테고리의 참고 문서가 없습니다." />
 */
// "현재 참조 가능한 정보가 없음" 안내 — KB 데이터 없을 때 작게 노출 (프롬프트 요구사항).
export function EmptyKbNotice({ message }: { message?: string }) {
  return (
    <p className="text-[11px] text-muted-foreground/80 leading-snug">
      ◇ {message ?? "현재 참조 가능한 정보가 없음 (지식저장소 추가 학습 필요)"}
    </p>
  );
}
