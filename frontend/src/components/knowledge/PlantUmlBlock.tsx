import { useEffect, useState } from "react";

/**
 * PlantUML 소스 코드를 SVG 다이어그램으로 렌더링하는 컴포넌트.
 *
 * ` ```plantuml ... ``` ` 마크다운 코드 블록에서 추출한 소스를 받아
 * 백엔드 `/api/plantuml/render` 엔드포인트에 POST 요청으로 SVG 이미지를 얻어 표시한다.
 *
 * 헤더 영역의 "소스 보기" 버튼으로 원본 PlantUML 소스와 렌더링된 다이어그램을 전환할 수 있다.
 *
 * @remarks
 * 이 컴포넌트는 지식저장소(KB) 문서 본문 렌더링 전용이다.
 * `/result` 화면의 LLM 흐름 다이어그램(JGraphX 기반)과는 별개로 동작한다.
 *
 * @param props.source - 렌더링할 PlantUML 소스 문자열.
 *
 * @example
 * ```tsx
 * <PlantUmlBlock source="@startuml\nAlice -> Bob: Hello\n@enduml" />
 * ```
 */
export function PlantUmlBlock({ source }: { source: string }) {
  /**
   * 렌더링된 SVG 이미지의 Object URL.
   * Blob URL로 생성하며, 컴포넌트 언마운트 또는 소스 변경 시 해제된다.
   */
  const [svgUrl, setSvgUrl] = useState<string | null>(null);

  /** 렌더링 실패 시 표시할 에러 메시지. */
  const [err, setErr] = useState<string | null>(null);

  /** `true`이면 SVG 대신 원본 PlantUML 소스를 표시한다. */
  const [showSource, setShowSource] = useState(false);

  useEffect(() => {
    /**
     * 비동기 fetch가 완료되기 전에 컴포넌트가 언마운트되거나 `source`가 변경된 경우
     * 이전 요청의 결과로 상태를 업데이트하지 않도록 막는 플래그.
     */
    let cancelled = false;

    /**
     * Blob URL 참조를 cleanup 함수에서도 접근할 수 있도록 클로저 외부에 보관.
     * 메모리 누수 방지를 위해 URL.revokeObjectURL 호출에 사용된다.
     */
    let createdUrl: string | null = null;

    (async () => {
      // 새 source로 재요청 시 이전 에러와 SVG URL 초기화
      setErr(null);
      setSvgUrl(null);
      try {
        const res = await fetch("/api/plantuml/render?format=svg", {
          method: "POST",
          headers: { "Content-Type": "text/plain; charset=utf-8" },
          body: source,
        });
        if (!res.ok) throw new Error(`렌더 실패 ${res.status}`);

        const blob = await res.blob();
        // SVG Blob을 브라우저 메모리에 로드하여 <img src>에서 사용 가능한 URL 생성
        createdUrl = URL.createObjectURL(blob);

        // 요청이 취소되지 않은 경우에만 상태 업데이트
        if (!cancelled) setSvgUrl(createdUrl);
      } catch (e) {
        if (!cancelled) setErr(e instanceof Error ? e.message : String(e));
      }
    })();

    return () => {
      // source 변경 또는 언마운트 시: 진행 중인 요청 결과 무시 + Blob URL 메모리 해제
      cancelled = true;
      if (createdUrl) URL.revokeObjectURL(createdUrl);
    };
  }, [source]); // source가 바뀔 때마다 SVG를 재요청

  return (
    <div className="my-3 rounded-md border border-border bg-white overflow-hidden">
      {/* 헤더: 컴포넌트 레이블과 소스/다이어그램 전환 버튼 */}
      <div className="flex items-center justify-between px-3 py-1.5 border-b border-border bg-secondary/40 text-[11px]">
        <span className="text-muted-foreground font-mono">PlantUML</span>
        <button
          type="button"
          onClick={() => setShowSource((s) => !s)}
          className="text-muted-foreground hover:text-foreground"
        >
          {showSource ? "다이어그램 보기" : "소스 보기"}
        </button>
      </div>

      {/* 본문: showSource 상태와 렌더링 결과에 따라 4가지 상태 중 하나를 표시 */}
      {showSource ? (
        // 소스 보기 모드: 원본 PlantUML 텍스트 표시
        <pre className="m-0 p-3 text-xs overflow-x-auto bg-white text-foreground">
          {source}
        </pre>
      ) : err ? (
        // 렌더링 실패 시 에러 메시지 표시
        <div className="p-4 text-xs text-destructive">
          PlantUML 렌더 실패: {err}
        </div>
      ) : !svgUrl ? (
        // SVG URL이 아직 없는 경우 (API 응답 대기 중)
        <div className="p-4 text-xs text-muted-foreground">다이어그램 렌더 중…</div>
      ) : (
        // 렌더링 성공: Blob URL을 <img>에 적용하여 SVG 표시
        <div className="p-3 overflow-x-auto">
          <img src={svgUrl} alt="PlantUML diagram" className="max-w-full h-auto" />
        </div>
      )}
    </div>
  );
}
