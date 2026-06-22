# DEVELOP REQ-N GENIE

개발요청서 자동 작성 도구 — **(BE + FE 통합)**.

요청자 입력을 받아 LLM(Gemini)이 표준 양식의 개발요청서와 거래 흐름 시퀀스 다이어그램을 생성, **`requirements.md` + `flow.png`를 zip으로 다운로드**해 준다. **단일 jar에 FE(React + Vite)와 BE(Spring Boot)가 함께 패키징**되어 한 프로세스로 띄운다.

본문 MD와 시퀀스 다이어그램 모두 **Gemini 2.5 Flash 실제 연결** 상태. FE는 위저드(`/new`) 6단계로 동작. KB는 `docs/*.md`를 GitHub raw URL로 LLM이 직접 열람하는 방식. 사내 LLM·KB 연동, 대시보드 본구현, 지식저장소 화면, KB 적재, F12/F14 등은 **추후 보강 필요**.

> ⚠️ **요청 포맷, 카테고리 시드, 산출 MD 구조는 모두 Phase 0 초안**이며 실 요구사항·KB 데이터 확정 시 변경 예정.

## 🆕 2026-06-22 20:00  FE 통합 + Gradle 일원화 + fail-fast

- 🆕 **`frontend/`** — React 19 + TypeScript + Vite + TanStack Router + Tailwind 4 + shadcn(Radix). 위저드(`Slide Wizard` 6단계, `components/wizard/`)로 새 요청서 작성 화면 구현.
- 🆕 **`controller/SpaFallbackController`** — `/api`/`swagger-ui`/`v3`/`assets` 외 모든 경로를 `index.html`로 forward (SPA 라우트 fallback).
- 🆕 **`build.gradle`에 `com.github.node-gradle.node` 플러그인** — `gradle bootJar` 한 번에 `frontendInstall → frontendBuild(vite) → processResources → bootJar`까지 자동. vite outDir이 `src/main/resources/static/`이라 단일 jar로 묶임.
- 🆕 **`DocServiceImpl.initGemini()` fail-fast** — `Assert.hasText(geminiApiKey, ...)`로 키 누락 시 **부팅 자체가 즉시 실패**. 이전엔 부팅은 되고 호출 시점에 403이라 진단이 어려웠음.

## 🆕 2026-06-22 다이어그램 구현 + 구조 정리

### 핵심: PlantUML → draw.io(mxGraph) 시퀀스 다이어그램으로 전환

- 🆕 `service/impl/FlowServiceImpl` — Gemini가 **거래 흐름 시퀀스(가맹점·자사PG·인증기관·카드사 등) draw.io 호환 mxGraph XML**을 생성하고 JGraphX로 PNG 렌더. 좌표가 의미를 가져 자동 레이아웃 미사용.
- 🆕 `service/llm/promt/DiagramPromptBuilder` — SEQUENCE/FLOWCHART 두 타입을 enum으로 지원. 좌표 규칙·골격 XML까지 프롬프트에 박아넣어 LLM 응답 정합성 확보.
- 🆕 `com.github.vlsi.mxgraph:jgraphx:4.2.2` 의존성 추가.
- ✏️ **`FlowService.renderPng(...)` 시그니처 변경**: `DevRequestRequest` → `String markdown`. **DocService가 만든 종합 MD를 받아 그 위에서 다이어그램을 그리므로 본문/flow 정합성 확보**(이전 비대칭 해소).

### 추가/이동 (구현 분리)

- 🆕 `service/impl/DocServiceImpl` (← 이전 `LLMDocService` 대체) — 인터페이스/구현 분리. Gemini Client는 `@PostConstruct`로 1회 초기화 재사용.
- 🆕 `service/llm/promt/ProjectPromptBuilder` — 본문 MD용 프롬프트 빌더가 별도 컴포넌트로 분리.
- 🆕 `service/llm/md/StandardMarkdownRenderer` — `ProjectMdResult` → 표준 양식 v1.6 MD 변환 전담.
- 🆕 `service/llm/md/MarkdownBuilder` — fluent API 마크다운 헬퍼(`.h2()/.field()/.checkbox()`).

### 삭제 (대정리)

- 🗑️ `service/llm/LlmClient` 인터페이스 + `StubLlmClient` — 추상화 자체 제거. DocService/FlowService 두 인터페이스로 한 단계 위에서 분리.
- 🗑️ `service/llm/dto/*Command·*Result` — `RequestDocCommand/Result`, `PreCheckCommand/Result`, `DesignFlowCommand/Result` 전부 제거. `ProjectMdResult`만 남음.
- 🗑️ `service/RequestDocService`, `service/DesignFlowService` — stub 흐름 정리.
- 🗑️ `config/GeminiConfig` — 별도 `@Bean Client` 대신 각 Impl이 자체 `@PostConstruct`로 초기화.
- 🗑️ `service/GeminiFlowService` (직전 작업물) — `FlowServiceImpl`로 통합/대체.

## 스택

Java 21 / Gradle 8.10.2 / Spring Boot 3.5.0 (`web`, `validation`) / Lombok / google-genai 1.58.0 (Gemini 2.5 Flash) / **JGraphX 4.2.2 (draw.io 호환 mxGraph)** / springdoc-openapi 2.8.14

## 디렉터리 구조

```
nice-qa/
├─ build.gradle                                # 🆕 node 플러그인 + frontendBuild task
├─ src/main/java/com/nice/qa/
│  ├─ NiceQaApplication.java
│  ├─ controller/
│  │  ├─ ApiExceptionHandler / CatalogController / DevRequestController
│  │  └─ SpaFallbackController                 🆕 SPA 라우트 fallback
│  ├─ model/api/dto/ DevRequestRequest
│  └─ service/
│     ├─ DocService / FlowService              (인터페이스)
│     ├─ impl/   DocServiceImpl / FlowServiceImpl   (Gemini 실호출)
│     ├─ llm/
│     │  ├─ dto/  ProjectMdResult              (Gemini 응답 28필드)
│     │  ├─ md/   StandardMarkdownRenderer, MarkdownBuilder
│     │  └─ promt/ ProjectPromptBuilder, DiagramPromptBuilder   (※ promt 오타)
│     ├─ knowledge/ KnowledgeClient, StubKnowledgeClient, DocRepository, dto/*
│     └─ flow/   FlowImageRenderer, PlantUmlFlowImageRenderer  🗑️ 사용처 없음
├─ src/main/resources/
│  ├─ application.yml
│  ├─ static/                                  🆕 vite build 산출물 (Spring이 서빙, gradle clean 시 삭제)
│  └─ docs/  policy/ provider/ spec/ templates/   # KB 시드 마크다운
└─ frontend/                                   🆕 FE 프로젝트 (Vite + React)
   ├─ package.json / vite.config.ts / tsconfig.json / index.html
   └─ src/
      ├─ main.tsx                              (SPA 진입점)
      ├─ styles.css                            (oklch 디자인 토큰)
      ├─ routes/    __root, index, new, result, knowledge
      ├─ components/
      │  ├─ ui/*                               (shadcn 40개)
      │  └─ wizard/                            🆕 새 요청서 위저드
      │     ├─ types.ts, WizardContext.tsx, WizardShell.tsx
      │     ├─ catalog.ts, submit.ts
      │     ├─ shared/ SlideShell, AdditionalCheck, EmptyKbNotice, FileAttachZone
      │     └─ slides/ Slide1FuncType ~ Slide6AiDeepDive
      └─ lib/utils.ts, hooks/
api.http
```

## 처리 흐름

```
POST /api/dev-requests/generate
└─ DevRequestController
   ├─ DocServiceImpl.assembleMarkdown(request)
   │   ├─ ProjectPromptBuilder.build(request, REFERENCE_LINKS)   # GitHub raw URL 7건
   │   ├─ Gemini 2.5 Flash + GoogleSearch Tool 호출
   │   ├─ ProjectMdResult JSON 파싱
   │   ├─ applyDeterministicFields(...)                          # author/department/createdDate 등 입력값 우선
   │   └─ StandardMarkdownRenderer.render(result)                # 표준 양식 v1.6 MD
   │  → markdown
   └─ FlowServiceImpl.renderPng(markdown)
       ├─ DiagramPromptBuilder.build(markdown, REFERENCE_LINKS, SEQUENCE)
       ├─ Gemini 2.5 Flash + GoogleSearch Tool 호출
       ├─ mxGraph XML 추출 (extractMxGraph: 코드펜스/wrapper 제거)
       └─ mxGraph 디코드 → JGraphX BufferedImage → PNG 바이트
          (XML 비정상 / 렌더 실패 시 FALLBACK_XML 사용)
      → flow.png
   → zip(requirements.md, flow.png) 다운로드 응답
```

**정합성 포인트**: flow는 본문 MD를 입력으로 받으므로 본문에서 정한 시나리오·시퀀스가 그대로 다이어그램에 반영된다.

## 클래스 역할

### 진입점·예외처리

- **NiceQaApplication** — 8090 포트. `SERVER_PORT` 환경변수로 덮어쓰기 가능.
- **ApiExceptionHandler** — 검증 실패 → 400 JSON / 그 외 모든 `Exception` → 500 JSON. 500 케이스는 응답에 메시지를 노출하지 않고 서버 로그에 스택을 남긴다.

### 컨트롤러

- **CatalogController** — `GET /api/catalog/` 단일 엔드포인트. ⚠️ `@GetMapping("/")` 매핑이라 슬래시 없는 `/api/catalog`는 404.
- **DevRequestController** — `POST /api/dev-requests/generate`. `DocService`로 종합 MD → `FlowService`에 MD 전달하여 PNG → zip.
- **SpaFallbackController** 🆕 — `/api`/`swagger-ui`/`v3`/`assets`/`favicon.ico` 제외한 모든 경로를 `index.html`로 forward. `/new`·`/result`·`/knowledge` 같은 클라이언트 라우트가 새로고침/직접 진입 시 404 안 나게 해줌.

### 서비스 인터페이스

- **DocService** — `String assembleMarkdown(DevRequestRequest)` 단일 메서드. 표준 양식 MD를 만들어 돌려준다.
- **FlowService** — `byte[] renderPng(String markdown)` 단일 메서드. ✏️ MD 입력으로 변경된 시그니처.

### 서비스 구현 (Phase 0 활성, 모두 Gemini 실호출)

- **DocServiceImpl** — `ProjectPromptBuilder`로 프롬프트 조립 → Gemini 호출 → `ProjectMdResult` JSON 파싱 → 결정론적 필드 보정(`author`/`department`/`createdDate`/`newServiceOrSelfImprovement`) → `StandardMarkdownRenderer`로 MD 변환. Gemini Client/Config는 `@PostConstruct`에서 1회 초기화 재사용.
- **FlowServiceImpl** — 종합 MD + 참고 링크를 `DiagramPromptBuilder`(SEQUENCE)로 프롬프트화 → Gemini 호출 → 응답에서 `<mxGraphModel>` 블록만 안전 추출 → JGraphX로 PNG 렌더. 응답 깨짐/렌더 실패 시 `FALLBACK_XML`로 "다이어그램 생성 실패" 박스 PNG 출력.

### 프롬프트 빌더 (`service/llm/promt`)

- **ProjectPromptBuilder** — 본문 MD용. 맥락(IT 지식 낮은 현업) → 분석 절차 6단계 → 참고 링크 → 입력 → issueAndImprovement 작성 규칙 → 공통 규칙 → 28필드 JSON 스키마. 정적 지침은 텍스트 블록 상수, 동적 입력만 메서드.
- **DiagramPromptBuilder** — 다이어그램용. `DiagramType.SEQUENCE`(거래 흐름 시퀀스)와 `FLOWCHART`(처리 흐름) 두 타입. 시퀀스는 기관 헤더(x=40부터 200씩) + 세로 점선 라이프라인 + 가로 floating edge(y=50씩 증가) + ①②③ 순번 라벨까지 좌표 규칙을 프롬프트에 박아둠. 골격 XML 예시도 함께 제공.

### MD 렌더 (`service/llm/md`)

- **StandardMarkdownRenderer** — `ProjectMdResult` → 표준 양식 v1.6 MD. 메타 frontmatter + 6개 섹션(요청 정보 / 배경·R&R / 요청 사항 / 기술·정책 / 기대 효과 / 처리 상태). `formatAmount`는 숫자만 추출해 천단위 콤마.
- **MarkdownBuilder** — fluent helper. `.h1()/.h2()/.field("라벨", 값)/.checkbox("라벨", Boolean)` 등. 인스턴스 가변이라 렌더링 호출마다 새로 생성.

### KB 어댑터

- **StubKnowledgeClient** — 분기 트리는 시드 데이터, 청크/규격서는 `DocRepository`가 읽어둔 docs/*.md를 키워드 매칭.
- **DocRepository** — 시동 시 `classpath:docs/**/*.md` 로드, YAML frontmatter + 본문 분리, 가중치(태그 2.0 / 제목 1.5 / 본문 1.0) 매칭.

### DTO

- **DevRequestRequest** (`model/api/dto`) — 요청 record. 분기 키(`funcType`/`category`/`subType`) + 공통 포맷 핵심 필드 모두 `@NotBlank`.
- **ProjectMdResult** (`service/llm/dto`) — Gemini가 돌려주는 표준 양식 JSON 매핑 28필드 POJO. 기본 정보/가맹점/원천사/추진배경/문제점·서비스/결제 정책/기대효과/R&R 판정 Boolean 4종.

## API

### `GET /api/catalog/`

분기 트리(기능구분 + 카테고리/세부유형) 조회. FE 위저드용. ⚠️ 끝 슬래시 필수.

### `POST /api/dev-requests/generate`

```json
{
  "funcType": "신규 서비스 개발",
  "category": "pg표준결제창",
  "subType": "카드",
  "author": "홍길동",
  "department": "결제개발팀",
  "serviceName": "신규 카드결제 모듈",
  "background": "기존 결제창 사용성 개선",
  "targetSchedule": "2026-08-31",
  "problemAndImprovement": "카드사 할부 옵션 동적 노출 필요"
}
```

응답: `application/zip`, `Content-Disposition: attachment; filename="devrequest_{yyyyMMdd_HHmmss}.zip"`
검증 실패: 400 JSON (`fieldErrors`)
처리 실패: 500 JSON (`status`/`error` 만 노출, 상세는 서버 로그)

### Swagger UI

`http://localhost:8090/swagger-ui.html` / `/v3/api-docs`

### zip 산출물

```
devrequest_*.zip
├─ requirements.md   # DocServiceImpl이 만든 표준 양식 v1.6 MD (ProjectMdResult 28필드 기반)
└─ flow.png          # FlowServiceImpl이 만든 거래 흐름 시퀀스 다이어그램 (mxGraph → JGraphX 렌더)
```

> 본문과 flow가 같은 MD를 공유하므로 시나리오 정합성이 확보된다.

## 설정 (`application.yml`)

```yaml
server:
  port: ${SERVER_PORT:8090}

llm:
  provider: ${LLM_PROVIDER:stub}      # stub | internal | external   ※ 현재 미사용
  base-url: ${LLM_BASE_URL:}
  model:    ${LLM_MODEL:}
  api-key:  ${LLM_API_KEY:}
knowledge:
  provider: ${KB_PROVIDER:stub}       # stub | internal
  base-url: ${KB_BASE_URL:}
external-sync:
  provider: ${EXT_SYNC_PROVIDER:noop} # ※ 현재 구현체 없음

gemini:                                # DocServiceImpl/FlowServiceImpl이 사용하는 Gemini API 키
  api-key: ${GEMINI_API_KEY:}

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

⚠️ `gemini.api-key`가 비어있으면 **부팅 자체가 실패**한다 (`DocServiceImpl.initGemini()`의 fail-fast). `Started NiceQaApplication`이 안 보이면 환경변수부터 점검.

## 실행

### 단일 jar (운영 형태)

```powershell
$env:GEMINI_API_KEY = 'AIzaSy...'
cd C:\Users\user\Documents\nice-qa
.\gradlew.bat bootJar      # vite 빌드까지 자동 (frontendInstall → frontendBuild → bootJar)
java -jar build\libs\nice-qa-0.0.1-SNAPSHOT.jar
# → http://localhost:8090/ (UI + API 한 프로세스)
```

### 개발 모드 (HMR)

터미널 1 — 백엔드: IntelliJ에서 `NiceQaApplication` Run (Env vars에 `GEMINI_API_KEY` 추가). 또는 `.\gradlew.bat bootRun --exclude-task frontendBuild`.
터미널 2 — 프론트: `cd frontend && npm run dev` → `http://localhost:5173/new` (vite proxy가 `/api`를 8090으로 전달)

## 테스트

### IntelliJ HTTP Client (권장)

루트의 `api.http`에서 ▶️ 클릭. `>>! devrequest.zip`로 zip 자동 저장.

### curl (Git Bash)

```bash
cat > body.json <<'EOF'
{"funcType":"신규 서비스 개발","category":"pg표준결제창","subType":"카드","author":"홍길동","department":"결제개발팀","serviceName":"신규 카드결제 모듈","background":"기존 결제창 사용성 개선","targetSchedule":"2026-08-31","problemAndImprovement":"카드사 할부 옵션 동적 노출 필요"}
EOF

curl -X POST http://localhost:8090/api/dev-requests/generate \
  -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @body.json \
  -o devrequest.zip

mkdir -p out && unzip -o devrequest.zip -d out
head -30 out/requirements.md
file out/flow.png    # "PNG image data" 떠야 OK
```

## 추후 보강 가능 사항

- **사용되지 않는 빈/의존성 정리** — `FlowImageRenderer`/`PlantUmlFlowImageRenderer` 클래스 + PlantUML 의존성 제거 (FlowServiceImpl로 대체됨)
- **Gemini Client 빈 통합** — DocServiceImpl/FlowServiceImpl이 각자 `@PostConstruct`로 Client 보유 중. `@Bean Client` 하나로 통합하면 키 누락 시 fail-fast/테스트 모킹도 편함
- **`promt` 오타 정정** → `prompt`
- **`javax/jakarta PostConstruct` 통일** (Spring Boot 3은 jakarta)
- **REFERENCE_LINKS 외부화** — DocServiceImpl/FlowServiceImpl 모두 GitHub URL 하드코딩. `application.yml`이나 `KnowledgeClient`를 통한 공급 권장
- **KB 우회 제거** — LLM이 GitHub raw로 docs를 다시 끌어오는 방식. 이미 메모리에 있는 `DocRepository.search()` 결과를 프롬프트에 직접 주입하면 네트워크 의존 제거 + 사내망에서도 동작
- **두 Gemini 호출 순차 강제** — 본문이 끝나야 flow 가능. 응답 8~15초. 본문 청크 스트리밍이나 캐시로 개선 여지
- **CatalogController 매핑** — `@GetMapping("/")` → `@GetMapping`
- **사내 LLM 어댑터** — DocServiceImpl/FlowServiceImpl을 인터페이스 두 단계 위로 한 번 더 추상화하거나 `@ConditionalOnProperty`로 stub ↔ 실어댑터 스왑
- **KnowledgeClient 실어댑터** / **`/result` 화면 데이터 바인딩** (백엔드 응답 zip → JSON 보강 동반, design.md §12-1) / **위저드 → `/result` 응답 전달** / **확정 요청서 KB 적재 파이프라인**
