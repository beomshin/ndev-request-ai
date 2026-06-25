# DEVELOP REQ-N GENIE

PG 도메인 개발요청서 자동 작성 도구 — **단일 jar에 React FE + Spring Boot BE 통합**.

현업이 위저드로 입력하면 Gemini 2.5 Flash가 표준 양식 MD와 거래 흐름 시퀀스 다이어그램을 생성해 H2 파일 DB에 저장하고, 상세 페이지에서 §1~§9로 표시. 사이드 메뉴의 **지식 저장소**에는 원천사 규격·결제창·WEB API 문서를 마크다운으로 노출하고 본문의 PlantUML 코드 블록은 백엔드에서 SVG로 렌더링한다.

## 스택

| 영역 | 사용 기술 |
|---|---|
| BE | Java 21 / Gradle 8.10.2 / Spring Boot 3.5.0 / Spring Data JPA + H2 / Lombok / springdoc-openapi |
| LLM·렌더 | google-genai 1.58 (Gemini 2.5 Flash) · JGraphX 4.2 (mxGraph XML → PNG, 요청서 flow 전용) · PlantUML 1.2024.7 (KB 본문 전용) |
| 데이터 | jackson-dataformat-yaml (catalog/IntakeForm/KB frontmatter 파싱) |
| FE | React 19 / TypeScript / Vite 6 / TanStack Router·Query / Tailwind 4 / shadcn(Radix) / react-markdown + remark-gfm |
| 통합 빌드 | `com.github.node-gradle.node` (gradle bootJar 한 번에 vite 빌드까지) |

## 화면

| 메뉴 | URL | 화면 |
|---|---|---|
| 새 요청서 작성 | `/new` | Slide Wizard (조건부 6 또는 7단계) |
| 생성 요청서 목록 | `/list` | DB 저장 목록 (검색·status 필터·페이징) |
| 지식 저장소 | `/knowledge` | docs/knowledge_base 스캔 결과 (3 카테고리, 8 문서) |
| 지식 문서 상세 | `/knowledge/{docId}` | 마크다운 HTML 렌더 + PlantUML 인라인 SVG |
| 요청서 상세 | `/result/{id}` | DB 영구 상세 §1~§9 |

## 개발요청서 위저드 단계

| Step | 라벨 | 비고 |
|---|---|---|
| 1 | 유형 | NEW(신규) / MODIFY(기존 수정) — 이후 단계의 동적 분기 기준 |
| 2 | 대분류 | catalog yaml의 categories 트리에서 분기. funcType별 available_func_types로 필터링 |
| 3 | 기본·배경 | author / department / background |
| 4 | 상세 1/3 | serviceName / targetSchedule |
| 5 | 상세 2/3 · 임팩트 | NEW일 땐 문제점·발생빈도 필드 숨김 (자동 placeholder 보충) |
| 6 | 상세 3/3 | AS-IS/TO-BE · 카드 옵션 · 자유 메모 |
| 7 | 신규 지불수단 폼 | **NEW + 표준결제창 + 카드일 때만** 추가 노출 (34필드 / 6섹션 / group 2건) |

## 처리 흐름

```
[/new 위저드 → 요청서 생성하기]
  └─ GET /api/dev-requests/generate
      → Gemini 1회 → ProjectMdResult JSON + markdown
  └─ POST /api/requests
      → DB(H2 파일)에 combinedMarkdown / details / unconfirmedSection 저장
      → details JSON 안에 wizard.paymentMethodIntake도 그대로 영속
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

[/knowledge → /knowledge/{docId}]
  ├─ 목록: GET /api/knowledge   (8건, last_updated 최신값 상단 노출)
  └─ 상세: GET /api/knowledge/{docId}
       → react-markdown 렌더, ```plantuml``` 블록은 PlantUmlBlock으로 위임
       → POST /api/plantuml/render?format=svg  (Blob URL → <img>)
```

**Gemini 호출 정책**: 자동 호출은 위저드 제출 시 1회뿐. flow 다이어그램은 사용자 명시 클릭에서만. KB의 PlantUML은 LLM과 무관(자체 라이브러리 렌더).

## 디렉터리 구조

```
src/main/java/com/nice/qa/
├─ controller/
│  ├─ DevRequestController      (위저드 → 생성)
│  ├─ RequestController         (저장/조회/삭제/flow.png)
│  ├─ CatalogController         (위저드 분기 트리)
│  ├─ IntakeFormController      (신규 지불수단 폼 스키마)
│  ├─ KnowledgeBaseController   (지식 저장소 목록/상세)
│  ├─ PlantUmlController        (KB 본문 PlantUML → SVG/PNG)
│  ├─ SpaFallbackController     (vite 빌드 SPA 라우팅)
│  └─ ApiExceptionHandler
├─ service/
│  ├─ DocService / impl         (Gemini → ProjectMdResult + MD)
│  ├─ FlowService / impl        (Gemini → mxGraph XML → PNG)
│  ├─ DevRequestStorageService  (JPA CRUD + 소프트 삭제)
│  ├─ intake/                   IntakeFormService (md frontmatter → 폼 스키마)
│  ├─ knowledge/
│  │  ├─ StubKnowledgeClient    (catalog yaml 로더 — funcType/categories 분기)
│  │  └─ kb/                    KnowledgeBaseService + KnowledgeDoc
│  └─ llm/                      GeminiLlmClient / LlmResponseParser /
│                               MxGraphRenderer / ReferenceLinks +
│                               dto/ProjectMdResult / prompt/{Project,Diagram}PromptBuilder /
│                               md/{StandardMarkdownRenderer, MarkdownBuilder}
├─ entity/       DevRequest, DevRequestStatus (DRAFT, AI_ANALYZED)
├─ repository/   DevRequestRepository (+ Specifications)
├─ config/       GeminiProperties, JpaConfig
└─ model/api/dto DevRequestRequest / DevRequestSaveRequest / Summary / Detail

src/main/resources/
├─ application.yml
├─ static/        vite build 산출물 (Spring이 서빙)
└─ docs/
   ├─ catalog/catalog_category_tree_v1.yaml     (위저드 분기 ground truth)
   ├─ policy/payment_method_intake_form_v1.md   (S7 폼 스키마 — frontmatter)
   └─ knowledge_base/{provider,ui,webapi}/*.md  (지식 저장소 8건)

frontend/
├─ vite.config.ts (outDir → ../src/main/resources/static)
└─ src/
   ├─ routes/
   │   __root / index / new / list /
   │   result_.$id (요청서 상세, flat) /
   │   knowledge (목록) / knowledge_.$id (상세, flat)
   ├─ components/
   │  ├─ wizard/
   │  │   types / WizardContext / WizardShell / submit /
   │  │   shared/{SlideShell,AdditionalCheck,EmptyKbNotice,FileAttachZone} /
   │  │   slides/{Slide1FuncType ~ Slide6AiDeepDive, Slide7PaymentMethodIntake} /
   │  │   intake/IntakeFormView
   │  ├─ knowledge/PlantUmlBlock     (```plantuml``` → POST → SVG)
   │  └─ ui/                         shadcn 40종
   └─ lib/
      requests.ts / intakeForm.ts / knowledge.ts / utils.ts
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
  api-key: ${GEMINI_API_KEY:}            # 비면 호출 시 403, 부팅은 됨
  model: ${GEMINI_MODEL:gemini-2.5-flash}
  temperature: 0.2
  max-output-tokens: 8192
```

⚠️ `GEMINI_API_KEY`가 비면 위저드 제출과 flow 생성 API만 실패. 부팅·KB·IntakeForm·PlantUML은 모두 정상 동작.

## 실행

**모드 1 — 단일 jar (운영 형태)**
```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:GEMINI_API_KEY = 'AIzaSy...'
cd C:\Users\user\Documents\nice-qa
.\gradlew.bat bootJar       # vite 빌드까지 자동
java -jar build\libs\nice-qa-0.0.1-SNAPSHOT.jar
# → http://localhost:8090/
```

**모드 2 — 개발 모드 (FE hot reload)**
```powershell
# 터미널 1 (BE)
.\gradlew.bat bootRun --exclude-task frontendBuild
# 터미널 2 (FE)
cd frontend; npm run dev    # http://localhost:5173 (vite proxy: /api → :8090)
```

부가:
- `http://localhost:8090/h2-console` — DB 조회 (URL `jdbc:h2:file:./data/req-genie`, user `sa`)
- `http://localhost:8090/swagger-ui.html` — API 명세

## API

| 메서드 | 경로 | 용도 |
|---|---|---|
| GET | `/api/catalog/` | 위저드 분기 트리 (catalog yaml 기반) |
| GET | `/api/forms/payment-method-intake` | S7 신규 지불수단 폼 스키마 (md frontmatter 파싱) |
| GET | `/api/dev-requests/generate` | LLM 생성 결과 JSON (`devRequest`+`markdown`) |
| POST | `/api/dev-requests/generate` | zip(`requirements.md` + `flow.png`) — 위저드 흐름 외 |
| POST | `/api/requests` | 신규 저장 (위저드 → DB) |
| PUT | `/api/requests/{id}` | 부분 갱신 |
| GET | `/api/requests` | 목록 (keyword·status·page·size·sort) |
| GET | `/api/requests/{id}` | 상세 |
| GET | `/api/requests/{id}/flow.png` | 다이어그램 PNG (DB 캐시 우선, 없으면 Gemini 1회 + 캐시) |
| DELETE | `/api/requests/{id}` | 소프트 삭제 |
| GET | `/api/knowledge` | 지식 저장소 목록 (markdown 제외 메타만) |
| GET | `/api/knowledge/{id}` | 지식 문서 상세 (frontmatter 전체 + markdown 본문) · id에 점 포함 가능 |
| POST | `/api/plantuml/render?format=svg\|png` | text/plain body의 PlantUML → SVG/PNG (KB 본문 전용) |

## DB

- **테이블**: `dev_request` (id, title, category_path, status, author, dept, created_at, updated_at, details(CLOB), combined_markdown(CLOB), flow_diagram(CLOB), unconfirmed_section(CLOB), deleted)
- **소프트 삭제**: `@SQLDelete UPDATE deleted=true`. 모든 조회는 `findByIdAndDeletedFalse` / Specification으로 필터링
- **JPA Auditing**: `@CreatedDate` / `@LastModifiedDate` 자동 채움
- **검색**: `JpaSpecificationExecutor`로 keyword(LIKE)·status·category 동적 조합
- **위저드 입력 보존**: `details` CLOB에 `{wizard, projectMd}` JSON으로 영속 — S7의 `paymentMethodIntake` 답변도 그 안에 자동 포함

## 데이터 소스 (yaml/md 기반 ground truth)

| 파일 | 역할 |
|---|---|
| `docs/catalog/catalog_category_tree_v1.yaml` | 위저드 분기 트리 (funcTypes·categories·subTypes) |
| `docs/policy/payment_method_intake_form_v1.md` | S7 신규 지불수단 폼 스키마 (frontmatter에 6 sections / 34 fields) |
| `docs/knowledge_base/provider/*.md` | 원천사 규격 (KAKAOPAY / NAVERPAY / PAYCO) |
| `docs/knowledge_base/ui/*.md` | 결제창 흐름 |
| `docs/knowledge_base/webapi/*.md` | WEB API (billing_key / cancel / card_keyin / vbank) |

폼 스키마·카탈로그·KB 모두 **md/yaml만 고치고 재기동**하면 반영됨 (재컴파일 불필요).

## 알아둘 점

- **위저드 자동 저장 시 `flowDiagram`은 비워둠** — §9 [⟳ 다이어그램 생성] 클릭 시에만 Gemini 호출하여 채우고 캐시
- **flat route 분리**: `/result/{id}` 실 식별자 `/result_/$id`, `/knowledge/{id}` 실 식별자 `/knowledge_/$id` — TanStack Router의 underscore 패턴 (사용자 URL은 슬래시)
- **PlantUML 통합은 KB 본문 전용**: `/result/{id}`의 LLM flow 다이어그램은 별도 (JGraphX/mxGraph 경로). 두 렌더 경로는 분리됨
- **빌드 산출물(`src/main/resources/static/assets/*`) git 추적 중** — 매 vite 빌드마다 해시 변경. 정리는 후속 작업