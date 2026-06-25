import { createFileRoute, Link, useParams } from "@tanstack/react-router";
import ReactMarkdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { useKnowledgeDoc } from "@/lib/knowledge";
import { PlantUmlBlock } from "@/components/knowledge/PlantUmlBlock";

// /knowledge/{id} — frontmatter 메타는 사이드 카드로, 본문 markdown은 메인에 HTML로 렌더.
// 개발요청서 상세(/result/{id})와 같은 좌측 본문 · 우측 메타 2열 레이아웃.
export const Route = createFileRoute("/knowledge_/$id")({
  head: () => ({
    meta: [{ title: "지식 문서 · Req-Genie" }],
  }),
  component: KnowledgeDetail,
});

function KnowledgeDetail() {
  const { id } = useParams({ from: "/knowledge_/$id" });
  const { data: doc, isLoading, isError, error } = useKnowledgeDoc(id);

  if (isLoading) {
    return (
      <div className="px-10 py-16 text-center text-sm text-muted-foreground">
        문서 불러오는 중…
      </div>
    );
  }
  if (isError || !doc) {
    return (
      <div className="px-10 py-16 text-center">
        <p className="text-sm text-destructive">
          문서 조회 실패: {error instanceof Error ? error.message : "데이터 없음"}
        </p>
        <Link to="/knowledge" className="mt-3 inline-block text-sm text-primary hover:underline">
          ← 지식 저장소로
        </Link>
      </div>
    );
  }

  // frontmatter에서 표시할 추가 메타 (목록에는 없지만 상세에서 보여줄 것)
  const meta = doc.meta ?? {};
  const author = pickString(meta, "author");
  const tags = pickArray(meta, "tags");
  const difficulty = pickString(meta, "difficulty");
  const estimatedReadMin = pickNumber(meta, "estimated_read_min");
  const relatedDocs = pickArray(meta, "related_docs");
  const externalLinks = pickObject(meta, "external_links");

  return (
    <div className="px-10 py-8 max-w-[1280px] mx-auto">
      {/* 브레드크럼 */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground mb-3">
        <Link to="/knowledge" className="hover:text-foreground">지식 저장소</Link>
        <span>/</span>
        <span className="text-foreground font-mono">{doc.id}</span>
      </div>

      {/* 헤더 */}
      <div className="mb-6">
        <div className="flex flex-wrap items-center gap-2 mb-2">
          {doc.category && (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium bg-accent/15 text-accent">
              {doc.category}
            </span>
          )}
          {doc.version && (
            <span className="text-[11px] font-mono text-muted-foreground">{doc.version}</span>
          )}
          {doc.status && (
            <span className="text-[10px] font-medium text-[color:var(--success)] bg-[color:var(--success)]/10 px-2 py-0.5 rounded">
              {doc.status}
            </span>
          )}
        </div>
        <h1 className="text-2xl font-semibold text-foreground tracking-tight">{doc.title}</h1>
        <p className="text-sm text-muted-foreground mt-1.5">
          {doc.lastUpdated && <>최종 업데이트: <b className="text-foreground">{doc.lastUpdated}</b></>}
          {author && <> · 작성자: {author}</>}
          {doc.fileSize && <> · {doc.fileSize}</>}
          {doc.chunkCount != null && <> · 청크 {doc.chunkCount.toLocaleString()}</>}
          {estimatedReadMin != null && <> · 약 {estimatedReadMin}분 소요</>}
        </p>
      </div>

      <div className="grid grid-cols-4 gap-6">
        {/* 본문 마크다운 */}
        <article className="col-span-3 rounded-xl border border-border bg-card overflow-hidden">
          <div className="p-8">
            <div className="markdown-body">
              <ReactMarkdown remarkPlugins={[remarkGfm]} components={MARKDOWN_COMPONENTS}>
                {doc.markdown ?? ""}
              </ReactMarkdown>
            </div>
          </div>
        </article>

        {/* 사이드 — frontmatter에서 뽑은 부가 정보 */}
        <aside className="space-y-4">
          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
              문서 정보
            </h3>
            <div className="space-y-2.5 text-xs">
              <Field k="doc_id" v={<code className="font-mono text-[11px]">{doc.id}</code>} />
              {doc.filename && <Field k="파일" v={<code className="font-mono text-[11px]">{doc.filename}</code>} />}
              {difficulty && <Field k="난이도" v={difficulty} />}
            </div>
          </div>

          {tags.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-5">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                태그
              </h3>
              <div className="flex flex-wrap gap-1.5">
                {tags.map((t) => (
                  <span
                    key={t}
                    className="text-[10px] font-medium px-2 py-0.5 rounded bg-secondary text-foreground"
                  >
                    {t}
                  </span>
                ))}
              </div>
            </div>
          )}

          {relatedDocs.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-5">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                관련 문서
              </h3>
              <ul className="space-y-1.5 text-xs">
                {relatedDocs.map((d) => (
                  <li key={d}>
                    <Link
                      to="/knowledge/$id"
                      params={{ id: d }}
                      className="text-primary hover:underline font-mono"
                    >
                      {d}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {externalLinks && Object.keys(externalLinks).length > 0 && (
            <div className="rounded-xl border border-border bg-card p-5">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                외부 링크
              </h3>
              <ul className="space-y-1.5 text-xs">
                {Object.entries(externalLinks).map(([k, v]) => (
                  <li key={k}>
                    <a
                      href={String(v)}
                      target="_blank"
                      rel="noreferrer noopener"
                      className="text-primary hover:underline break-all"
                    >
                      {k} ↗
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
              빠른 이동
            </h3>
            <Link to="/knowledge" className="text-sm text-primary hover:underline">
              ← 지식 저장소 목록
            </Link>
          </div>
        </aside>
      </div>
    </div>
  );
}

// react-markdown 커스텀 컴포넌트 — ```plantuml ... ``` 코드 블록만 PlantUmlBlock으로 위임.
const MARKDOWN_COMPONENTS: Components = {
  code(props) {
    const { className, children, node: _node, ...rest } = props;
    const lang = /language-(\w+)/.exec(className ?? "")?.[1];
    if (lang === "plantuml" || lang === "puml") {
      return <PlantUmlBlock source={String(children).replace(/\n$/, "")} />;
    }
    return (
      <code className={className} {...rest}>
        {children}
      </code>
    );
  },
};

function Field({ k, v }: { k: string; v: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-3">
      <span className="text-muted-foreground">{k}</span>
      <span className="text-foreground text-right break-all">{v}</span>
    </div>
  );
}

// frontmatter는 임의 키이므로 안전한 접근자
function pickString(o: Record<string, unknown>, k: string): string | undefined {
  const v = o[k];
  return typeof v === "string" && v.trim() ? v : undefined;
}
function pickNumber(o: Record<string, unknown>, k: string): number | undefined {
  const v = o[k];
  return typeof v === "number" ? v : undefined;
}
function pickArray(o: Record<string, unknown>, k: string): string[] {
  const v = o[k];
  if (!Array.isArray(v)) return [];
  return v.filter((x): x is string => typeof x === "string");
}
function pickObject(o: Record<string, unknown>, k: string): Record<string, unknown> | undefined {
  const v = o[k];
  if (v && typeof v === "object" && !Array.isArray(v)) return v as Record<string, unknown>;
  return undefined;
}
