import { useEffect, useState } from "react";

/**
 * ```plantuml ... ``` 코드 블록을 백엔드 /api/plantuml/render 로 보내 SVG로 받아 표시.
 * 본 컴포넌트는 KB(지식저장소) 본문 렌더 전용 — /result 화면의 LLM flow 다이어그램(JGraphX)은 별도.
 */
export function PlantUmlBlock({ source }: { source: string }) {
  const [svgUrl, setSvgUrl] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [showSource, setShowSource] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let createdUrl: string | null = null;

    (async () => {
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
        createdUrl = URL.createObjectURL(blob);
        if (!cancelled) setSvgUrl(createdUrl);
      } catch (e) {
        if (!cancelled) setErr(e instanceof Error ? e.message : String(e));
      }
    })();

    return () => {
      cancelled = true;
      if (createdUrl) URL.revokeObjectURL(createdUrl);
    };
  }, [source]);

  return (
    <div className="my-3 rounded-md border border-border bg-white overflow-hidden">
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
      {showSource ? (
        <pre className="m-0 p-3 text-xs overflow-x-auto bg-white text-foreground">
          {source}
        </pre>
      ) : err ? (
        <div className="p-4 text-xs text-destructive">
          PlantUML 렌더 실패: {err}
        </div>
      ) : !svgUrl ? (
        <div className="p-4 text-xs text-muted-foreground">다이어그램 렌더 중…</div>
      ) : (
        <div className="p-3 overflow-x-auto">
          <img src={svgUrl} alt="PlantUML diagram" className="max-w-full h-auto" />
        </div>
      )}
    </div>
  );
}
