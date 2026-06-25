# DEVELOP REQ-N GENIE

PG 도메인 개발요청서 자동 작성 도구 — **단일 jar에 React FE + Spring Boot BE 통합**.

현업이 위저드로 6단계 입력하면 Gemini 2.5 Flash가 표준 양식 v1.7 MD와 거래 흐름 시퀀스 다이어그램을 생성해 H2 파일 DB에 저장하고, 상세 페이지에서 §1~§9로 표시 + SHARE 붙여넣기용 [⎘ MD 복사]·[⬇ zip 다운로드] 제공.

## 스택

| 영역 | 사용 기술 |
|---|---|
| BE | Java 21 / Gradle 8.10.2 / Spring Boot 3.5.0 / Spring Data JPA + H2 / Lombok / springdoc-openapi |
| LLM·렌더 | google-genai 1.58 (Gemini 2.5 Flash) / JGraphX 4.2 (mxGraph XML → PNG) |
| FE | React 19 / TypeScript / Vite 6 / TanStack Router·Query / Tailwind 4 / shadcn(Radix) |
| 통합 빌드 | `com.github.node-gradle.node` (gradle bootJar 한 번에 vite 빌드까지) |

## 화면 (사이드바 4개)

| 메뉴 | URL | 화면 |
|---|---|---|
| 대시보드 | `/` | lovable 기본 (mock) |
| 새 요청서 작성 | `/new` | Slide Wizard 6단계 (분류·기본·상세·심층질의) |
| 생성 요청서 목록 | `/list` | DB 저장 목록 (검색·status 필터·페이징) |
| 지식 저장소 | `/knowledge` | lovable 원본 (mock) |
| (라우트) 상세 | `/result/{id}` | DB 영구 상세 §1~§9 + 액션 버튼 |

## 처리 흐름

```
[/new 위저드 6단계 → 요청서 생성하기]
  └─ GET /api/dev-requests/generate
      → Gemini 1회 → ProjectMdResult JSON + markdown
  └─ POST /api/requests
      → DB(H2 파일)에 combinedMarkdown / details / unconfirmedSection 저장
  └─ navigate /result/{id}

[/result/{id} 영구 상세]
  ├─ §1 요청 개요 / §2 추진 배경 / §3 AS-IS·TO-BE / §4 정책 상세
  ├─ §5 개발 범위·복잡도 / §6 기대 효과
  ├─ §7 현업 재확인 필요 (pendingQuestions, 핑퐁 방지)
  ├─ §8 AI 추론 근거 (assumptionList)
  └─ §9 설계 흐름 다이어그램 (mxGraph PNG)
       └─ 첫 클릭 시 GET /api/requests/{id}/flow.png
           → Gemini 1회 → XML 생성 → DB flow_diagram 캐시
           → 다음부턴 Gemini 호출 없이 즉시 렌더

[우상단 액션]
  ├─ [⎘ MD 복사]     combinedMarkdown → 클립보드 (호출 없음)
  ├─ [⬇ zip 다운로드] POST → Gemini 2회 (MD + flow) → zip
  └─ [🗑 삭제]        소프트 삭제 (deleted=true)
```

**무료 한도 절약 원칙**: 자동 Gemini 호출은 위저드 1회뿐. flow·zip은 사용자 명시 클릭에서만.

## 디렉터리 구조 (요약)

```
src/main/java/com/nice/qa/
├─ controller/   DevRequestController, RequestController, CatalogController,
│                SpaFallbackController, ApiExceptionHandler
├─ service/
│  ├─ DocService / DocServiceImpl       (Gemini → ProjectMdResult + MD)
│  ├─ FlowService / FlowServiceImpl     (Gemini → mxGraph XML → PNG)
│  ├─ DevRequestStorageService          (JPA CRUD + 소프트 삭제)
│  ├─ knowledge/                        (StubKnowledgeClient + DocRepository)
│  └─ llm/                              GeminiLlmClient / LlmResponseParser /
│                                       MxGraphRenderer / ReferenceLinks
│                                       + dto/ProjectMdResult (33필드)
│                                       + promt/{Project,Diagram}PromptBuilder
│                                       + md/{StandardMarkdownRenderer, MarkdownBuilder}
├─ entity/       DevRequest, DevRequestStatus (DRAFT, AI_ANALYZED)
├─ repository/   DevRequestRepository (JpaRepository + Specifications)
├─ config/       GeminiProperties, JpaConfig
└─ model/api/dto DevRequestRequest / DevRequestSaveRequest / Summary / Detail

src/main/resources/
├─ application.yml
├─ static/        vite build 산출물 (Spring이 서빙)
└─ docs/          KB 시드 (policy/provider/spec/requests/templates/catalog)

frontend/
├─ vite.config.ts (outDir → ../src/main/resources/static)
└─ src/
   ├─ routes/      __root / index / new / list / result_.$id / knowledge
   ├─ components/
   │  ├─ wizard/   types / WizardContext / WizardShell / submit / catalog
   │  │            + shared/SlideShell·AdditionalCheck·EmptyKbNotice·FileAttachZone
   │  │            + slides/Slide1FuncType ~ Slide6AiDeepDive
   │  └─ ui/       shadcn 40종
   └─ lib/         requests.ts (API 훅), utils.ts
```

## 설정 (`application.yml`)

```yaml
server:
  port: ${SERVER_PORT:8090}

datasource:
  url: ${DB_URL:jdbc:h2:file:./data/req-genie;MODE=MYSQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1}
  driver-class-name: ${DB_DRIVER:org.h2.Driver}
  username: ${DB_USERNAME:sa}
  password: ${DB_PASSWORD:}
jpa:
  hibernate:
    ddl-auto: ${JPA_DDL_AUTO:update}    # 파일럿: update / 운영: validate + Flyway 권장

gemini:
  api-key: ${GEMINI_API_KEY:}            # 비면 호출 시 403
  model: ${GEMINI_MODEL:gemini-2.5-flash}
  temperature: 0.2
  max-output-tokens: 8192
```

⚠️ `GEMINI_API_KEY`가 비면 호출 시 403. 부팅은 됨. **추후 사내 DB 전환은 datasource yml만 갈아끼움**.

## 실행

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:GEMINI_API_KEY = 'AIzaSy...'
cd C:\Users\user\Documents\nice-qa
.\gradlew.bat bootJar       # vite 빌드까지 자동
java -jar build\libs\nice-qa-0.0.1-SNAPSHOT.jar
# → http://localhost:8090/
```

부가:
- `http://localhost:8090/h2-console` — DB 조회 (URL: `jdbc:h2:file:./data/req-genie`, user `sa`)
- `http://localhost:8090/swagger-ui.html` — API 명세

## API (요약)

| 메서드 | 경로 | 용도 |
|---|---|---|
| GET | `/api/catalog/` | 분기 트리 (위저드용) |
| GET | `/api/dev-requests/generate` | LLM 생성 결과 JSON (`devRequest`+`markdown`) |
| POST | `/api/dev-requests/generate` | zip(`requirements.md` + `flow.png`) 다운로드 |
| POST | `/api/requests` | 신규 저장 |
| PUT | `/api/requests/{id}` | 부분 갱신 |
| GET | `/api/requests` | 목록 (keyword·status·page·size·sort) |
| GET | `/api/requests/{id}` | 상세 |
| GET | `/api/requests/{id}/flow.png` | 다이어그램 PNG (DB 캐시 우선, 없으면 Gemini 1회 + 캐시) |
| DELETE | `/api/requests/{id}` | 소프트 삭제 |

## DB

- **테이블**: `dev_request` (id, title, category_path, status, author, dept, created_at, updated_at, details(CLOB), combined_markdown(CLOB), flow_diagram(CLOB), unconfirmed_section(CLOB), deleted)
- **소프트 삭제**: `@SQLDelete UPDATE deleted=true`. 모든 조회는 `findByIdAndDeletedFalse` / Specification으로 필터링
- **JPA Auditing**: `@CreatedDate` / `@LastModifiedDate` 자동 채움
- **검색**: `JpaSpecificationExecutor`로 keyword(LIKE)·status·category 동적 조합

## 알아둘 점

- **위저드 자동 저장 시 `flowDiagram`은 비워둠** — 사용자가 §9 [⟳ 다이어그램 생성] 클릭할 때만 Gemini 호출하여 채우고 캐시 (무료 한도 절약)
- **`/result/{id}` 라우트는 식별자 `/result_/$id`** (TanStack Router의 underscore로 nested 분리). 사용자 URL은 `/result/{id}` 그대로
- **빌드 산출물(`src/main/resources/static/assets/*`) git 추적 중** — 매 vite 빌드마다 해시 변경. 정리는 후속 작업
- **참고 문서 27건 (`ReferenceLinks.ALL`)** — GitHub raw URL로 LLM이 직접 열람 (추후 사내망으로 전환 시 KB 어댑터로 교체 필요)

## 추후 보강

- 추후 사내 AI LLM 어댑터 전환 시 : (`GeminiLlmClient` → 사내 엔드포인트로 스왑)
- `KnowledgeClient` 실어댑터 (현재 `DocRepository` 시드)
- 위저드 [저장] 흐름의 자동 vs 명시 분리 옵션
- 빌드 산출물 `.gitignore` + 다이어그램 "강제 재생성" 백엔드 옵션
- Flyway 마이그레이션 / 통합 테스트 코드
