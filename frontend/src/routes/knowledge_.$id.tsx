import { createFileRoute, Link, useParams } from "@tanstack/react-router";
import ReactMarkdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { useKnowledgeDoc } from "@/lib/knowledge";
import { PlantUmlBlock } from "@/components/knowledge/PlantUmlBlock";

// /knowledge/{id} — frontmatter 메타는 사이드 카드로, 본문 markdown은 메인에 HTML로 렌더.
// 개발요청서 상세(/result/{id})와 같은 좌측 본문 · 우측 메타 2열 레이아웃.

/**
 * `/knowledge/:id` 라우트 정의.
 * 지식 문서 상세 페이지의 메타 정보를 선언하고 `KnowledgeDetail` 컴포넌트를 등록한다.
 */
export const Route = createFileRoute("/knowledge_/$id")({
  head: () => ({
    meta: [{ title: "지식 문서 · Req-Genie" }],
  }),
  component: KnowledgeDetail,
});

/**
 * 지식 문서 상세 페이지 컴포넌트.
 * URL 파라미터 `id`로 문서를 조회하며, 로딩/에러/성공 상태를 각각 처리한다.
 * 성공 시 좌측에 마크다운 본문, 우측에 frontmatter 메타 정보를 2열로 렌더링한다.
 */
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
      {/* 브레드크럼 — 현재 문서의 위치를 표시하고 목록으로 돌아가는 링크를 제공 */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground mb-3">
        <Link to="/knowledge" className="hover:text-foreground">지식 저장소</Link>
        <span>/</span>
        <span className="text-foreground font-mono">{doc.id}</span>
      </div>

      {/* 문서 헤더 — 카테고리 뱃지, 버전, 상태, 제목, 메타 정보를 순서대로 표시 */}
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
        {/* 본문 마크다운 — remark-gfm으로 GFM 확장 문법을 지원하고 커스텀 컴포넌트로 PlantUML을 처리 */}
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
          {/* 기본 문서 정보 카드 */}
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

          {/* 태그 카드 — frontmatter의 tags 배열이 존재할 때만 표시 */}
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

          {/* 관련 문서 카드 — frontmatter의 related_docs 배열이 존재할 때만 표시 */}
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

          {/* 외부 링크 카드 — frontmatter의 external_links 객체가 존재할 때만 표시 */}
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

          {/* 빠른 이동 카드 */}
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

/**
 * react-markdown 커스텀 컴포넌트 맵.
 * ` ```plantuml ``` ` 또는 ` ```puml ``` ` 코드 블록만 `PlantUmlBlock`으로 위임하여 렌더링하고,
 * 그 외 코드 블록은 기본 `<code>` 태그로 처리한다.
 */
const MARKDOWN_COMPONENTS: Components = {
  code(props) {
    const { className, children, node: _node, ...rest } = props;
    // className은 "language-{lang}" 형태이므로 정규식으로 언어명을 추출
    const lang = /language-(\w+)/.exec(className ?? "")?.[1];
    if (lang === "plantuml" || lang === "puml") {
      // PlantUML 코드 블록은 서버 측 렌더 또는 외부 라이브러리를 통해 다이어그램 이미지로 변환
      return <PlantUmlBlock source={String(children).replace(/\n$/, "")} />;
    }
    return (
      <code className={className} {...rest}>
        {children}
      </code>
    );
  },
};

/**
 * 문서 정보 사이드바에서 키-값 쌍을 한 줄로 표시하는 컴포넌트.
 *
 * @param k - 항목 레이블
 * @param v - 표시할 값 (문자열 또는 ReactNode)
 */
function Field({ k, v }: { k: string; v: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-3">
      <span className="text-muted-foreground">{k}</span>
      <span className="text-foreground text-right break-all">{v}</span>
    </div>
  );
}

// frontmatter는 임의 키이므로 안전한 접근자

/**
 * frontmatter 객체에서 문자열 값을 안전하게 추출한다.
 * 값이 없거나 빈 문자열이면 `undefined`를 반환한다.
 *
 * @param o - frontmatter 데이터 객체
 * @param k - 추출할 키
 * @returns 추출된 문자열 또는 undefined
 */
function pickString(o: Record<string, unknown>, k: string): string | undefined {
  const v = o[k];
  return typeof v === "string" && v.trim() ? v : undefined;
}

/**
 * frontmatter 객체에서 숫자 값을 안전하게 추출한다.
 * 값이 없거나 숫자 타입이 아니면 `undefined`를 반환한다.
 *
 * @param o - frontmatter 데이터 객체
 * @param k - 추출할 키
 * @returns 추출된 숫자 또는 undefined
 */
function pickNumber(o: Record<string, unknown>, k: string): number | undefined {
  const v = o[k];
  return typeof v === "number" ? v : undefined;
}

/**
 * frontmatter 객체에서 문자열 배열 값을 안전하게 추출한다.
 * 배열이 아니거나 값이 없으면 빈 배열을 반환한다.
 * 배열 요소 중 문자열이 아닌 값은 걸러낸다.
 *
 * @param o - frontmatter 데이터 객체
 * @param k - 추출할 키
 * @returns 문자열 배열 (없으면 빈 배열)
 */
function pickArray(o: Record<string, unknown>, k: string): string[] {
  const v = o[k];
  if (!Array.isArray(v)) return [];
  return v.filter((x): x is string => typeof x === "string");
}

/**
 * frontmatter 객체에서 중첩 객체 값을 안전하게 추출한다.
 * 배열이거나 null이거나 객체가 아니면 `undefined`를 반환한다.
 *
 * @param o - frontmatter 데이터 객체
 * @param k - 추출할 키
 * @returns 추출된 객체 또는 undefined
 */
function pickObject(o: Record<string, unknown>, k: string): Record<string, unknown> | undefined {
  const v = o[k];
  if (v && typeof v === "object" && !Array.isArray(v)) return v as Record<string, unknown>;
  return undefined;
}
