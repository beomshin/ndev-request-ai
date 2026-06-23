// "현재 참조 가능한 정보가 없음" 안내 — KB 데이터 없을 때 작게 노출 (프롬프트 요구사항).
export function EmptyKbNotice({ message }: { message?: string }) {
  return (
    <p className="text-[11px] text-muted-foreground/80 leading-snug">
      ◇ {message ?? "현재 참조 가능한 정보가 없음 (지식저장소 추가 학습 필요)"}
    </p>
  );
}
