# DEVELOP REQ-N GENIE

개발요청서 자동 작성 도구 — **백엔드 Phase 0 수직 슬라이스**.

요청자 입력 + 사내 지식저장소(KB) 컨텍스트를 기반으로 LLM이 개발요청서와 설계 흐름도를 생성, **`requirements.md` + `flow.png`를 zip으로 다운로드**해 준다.

현재는 실제 LLM/KB 없이 **Stub로 end-to-end 동작**하는 상태. 사내 LLM·KB 연동, 프론트엔드 React 화면, 대시보드/지식저장소 화면, KB 적재, F12/F14 등은 **범위 밖**.

> ⚠️ **요청 포맷, 카테고리 시드, 산출 MD 구조는 모두 Phase 0 초안**이며 실 요구사항·KB 데이터 확정 시 변경 예정.

## 스택

Java 21 / Gradle 8.10.2 / Spring Boot 3.5.0 (`web`, `validation`) / PlantUML 1.2024.7 (in-process PNG 렌더)

## 디렉터리 구조

```
src/main/java/com/nice/qa/
├─ NiceQaApplication.java
├─ controller/      ApiExceptionHandler, CatalogController, DevRequestController, TestController
├─ dto/             DevRequestRequest
└─ service/
   ├─ RequestDocService, DesignFlowService
   ├─ llm/          LlmClient(+B안 도메인 메서드), StubLlmClient, *Command/*Result
   ├─ knowledge/    KnowledgeClient, StubKnowledgeClient, DocRepository, CategoryTree, KnowledgeChunk, KbFilter, SpecDocRef, PastRequestRef, SimilarQuery, KbStatus
   ├─ externalsync/ ExternalSyncPort, NoopExternalSyncPort, PublishedRequest, SyncResult
   └─ flow/         FlowImageRenderer, PlantUmlFlowImageRenderer
src/main/resources/
├─ application.yml
└─ docs/                  # KB 시드 마크다운 (StubKnowledgeClient의 mock 데이터 원천)
   ├─ policy/             # pg_internal_policy, pg_error_code_manual
   ├─ provider/           # provider_kakaopay, provider_payco, provider_tosspay
   ├─ spec/               # nicepay_auth_spec
   └─ templates/          # standard_dev_request_output
api.http                  # IntelliJ HTTP Client 테스트용
```

## 포트 인터페이스 (별도 추가 개발 필요)

외부 시스템 의존을 인터페이스 뒤로 격리해 둔 자리들. Phase 0는 모두 Stub/Noop으로 동작하지만, 운영에 들어가려면 각각 실제 어댑터 구현체를 **별도로 추가 개발해야 한다**. 어댑터 교체는 `application.yml`의 `provider` 값을 바꾸는 것만으로 가능하도록 `@ConditionalOnProperty`로 묶여 있다.

- **LlmClient** — 사내 LLM 호출 격리 지점. 도메인 메서드(`generateRequestDoc`, `precheck`, `generateDesignFlow`)만 노출하며, 프롬프트 설계와 실제 모델 호출은 이 인터페이스를 구현하는 별도 어댑터에서 다루는 구조. 현재는 `StubLlmClient`가 고정 mock을 반환.
- **KnowledgeClient** — 사내 지식저장소(KB) 조회 격리. 분기 트리·RAG 청크·규격서·유사 과거요청·학습 현황을 가져오는 메서드를 정의해두었으며, 실제 검색/임베딩/매칭 로직은 별도 KB 어댑터로 구현이 필요. 현재는 `StubKnowledgeClient`가 `src/main/resources/docs/`의 시드 마크다운을 읽어 mock을 반환한다 — 단순 placeholder가 아니라 실제 PG 도메인 콘텐츠로 채워져 산출 흐름을 그대로 확인할 수 있다.
- **ExternalSyncPort** — 확정 요청서를 SHARE/Plane/Confluence 같은 외부 협업 도구로 발행하는 F13 자리. 현재는 `NoopExternalSyncPort`가 `SKIPPED`만 돌려주며, 실제 연계는 별도 어댑터 개발 필요.
- **FlowImageRenderer** — 다이어그램 소스 → PNG 변환 격리. 현재는 `PlantUmlFlowImageRenderer`가 in-process로 실제 동작하며, 향후 Mermaid 등 다른 렌더러로 교체하고 싶을 때 이 인터페이스로 갈아끼우면 된다.

## 클래스 역할

### 진입점·예외처리

- **NiceQaApplication** — 스프링 부트 진입점. 기본 포트는 8090이며, `SERVER_PORT` 환경변수로 덮어쓸 수 있다.
- **ApiExceptionHandler** — `@Valid` 검증 실패(`MethodArgumentNotValidException`)를 잡아 400 JSON으로 응답한다. 정상 응답이 zip 바이너리라 검증 에러가 zip 스트림에 섞이지 않도록 별도 핸들러로 분리해 두었다.

### 컨트롤러

- **CatalogController** — `GET /api/catalog` 단일 엔드포인트. `KnowledgeClient.getCategoryTree()`를 그대로 노출해 프론트 위저드의 분기 선택지로 쓰이게 한다.
- **DevRequestController** — `POST /api/dev-requests/generate`. 요청을 검증한 뒤 `RequestDocService`로 종합 MD를, `DesignFlowService`로 흐름 PNG를 받아 한 zip(`requirements.md` + `flow.png`)으로 묶어 다운로드 응답으로 내려준다.
- **TestController** — `GET /test`. google-genai SDK(Gemini 2.5 Flash) 연결 PoC. 본 흐름과는 분리된 별도 실험용 엔드포인트이며, 추후 `LlmClient`의 실어댑터 구현 시 청사진으로 활용 예정. ⚠️ 현재 API key가 코드에 하드코딩돼 있어 환경변수화 필요.

### 서비스(오케스트레이션)

- **RequestDocService** — 요청서 본문 조립 흐름을 담당. `KnowledgeClient`로 규격서·KB 청크를 모은 뒤 `LlmClient.precheck`로 사전검토 경고를 받고, `LlmClient.generateRequestDoc`으로 본문을 받아 메타·추진배경·본문·추가확인·사전검토·규격서·KB청크·설계흐름 섹션이 들어간 종합 마크다운으로 만들어 돌려준다.
- **DesignFlowService** — `LlmClient.generateDesignFlow`가 돌려준 PlantUML 소스를 `FlowImageRenderer.render`에 넘겨 PNG 바이트로 변환해 반환한다.

### Stub/Noop 구현 (Phase 0 활성)

- **StubLlmClient** — 실제 LLM을 호출하지 않고, 본문 placeholder + 추가 확인 항목 3건 + 샘플 PlantUML 소스를 고정값으로 반환한다. 인터페이스 계약과 흐름 검증용.
- **StubKnowledgeClient** — 분기 트리는 시드 데이터(기능구분 2건 / 카테고리 4건과 세부유형들)로 반환하고, RAG 청크·규격서·유사요청은 `DocRepository`가 읽어둔 docs/*.md를 키워드 매칭해 돌려준다. 결과적으로 zip의 KB 청크/규격서 섹션이 실제 PG 도메인 문서(NICEPAY/Kakaopay/Payco/Toss 등)로 채워진다.
- **DocRepository** — 시동 시 `classpath:docs/**/*.md`를 한 번 로드해 메모리에 보관한다. YAML 프론트매터(`category`/`tags`/`reference_url`/`version` 등)와 본문을 분리해두고, 키워드 매칭 점수(태그 2.0 / 제목 1.5 / 본문 1.0) 기반의 단순 검색을 제공한다. 실제 KB가 붙기 전까지의 그럴듯한 mock 원천.
- **NoopExternalSyncPort** — 항상 `SyncResult.SKIPPED`만 반환. 외부 협업 도구 연계는 아직 붙이지 않았음을 명시하는 자리.
- **PlantUmlFlowImageRenderer** — `SourceStringReader.outputImage(PNG)`로 PlantUML 소스를 in-process 렌더링한다. Phase 0에서도 실제 PNG가 떨어지며, activity 문법만 쓰면 Graphviz 설치도 필요 없다.

### DTO

- **DevRequestRequest** — 요청 record. 위저드 분기 키(`funcType`/`category`/`subType`)와 공통 포맷 핵심 필드(작성자/부서/서비스명/추진배경/목표일정/문제점·개선점)를 모두 `@NotBlank`로 받는다.

## API

### `GET /api/catalog`

분기 트리(기능구분 + 카테고리/세부유형) 조회. FE 위저드용.

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

> ⚠️ 모든 필드 `@NotBlank`. 기타서비스처럼 세부유형이 없는 카테고리는 클라이언트가 `"(없음)"` 같은 값으로 채워 보낸다. 실 요구사항 확정 시 검증·필드 구성 변경.

응답: `application/zip`, `Content-Disposition: attachment; filename="devrequest_{yyyyMMdd_HHmmss}.zip"`
검증 실패 시: 400 JSON (`fieldErrors`)

### zip 산출물

```
devrequest_*.zip
├─ requirements.md   # 메타 / 추진배경 / 본문(LLM) / 추가확인(F7) / 사전검토(F8) / 규격서(F10) / KB청크 / 설계흐름(이미지 링크)
└─ flow.png          # PlantUML 렌더 PNG
```

규격서·KB청크 섹션은 `docs/*.md` 중 입력의 카테고리/세부유형/문제점 키워드와 매칭된 문서들로 채워진다. 매칭이 전무할 경우 `docs/spec/`과 `docs/provider/` 전체를 기본 후보로 보여줘 사용자가 어떤 자료 풀이 있는지 확인할 수 있게 한다.

> ⚠️ 산출 MD 섹션 구성도 초안이며 실 사용자 요구에 맞춰 변경 예정.

## 설정 (`application.yml`)

```yaml
llm:
  provider: ${LLM_PROVIDER:stub}      # stub | internal | external
  base-url: ${LLM_BASE_URL:}
  model:    ${LLM_MODEL:}
  api-key:  ${LLM_API_KEY:}
knowledge:
  provider: ${KB_PROVIDER:stub}       # stub | internal
  base-url: ${KB_BASE_URL:}
external-sync:
  provider: ${EXT_SYNC_PROVIDER:noop} # noop | share
```

`provider` 키가 `stub` / `noop`이면 위에서 설명한 Stub/Noop 구현이 활성화된다. 이 상태에서는 나머지 `base-url`·`api-key` 같은 값들이 실제로 호출에 사용되지 않으므로 placeholder를 그대로 둬도 무방하다. 실제 LLM/KB 어댑터를 추가 개발해 붙일 때 `provider` 값을 `internal` 등으로 바꾸고 나머지 값들도 환경변수로 채워 넣는 방식이다.

## 실행

IntelliJ에서 Project SDK = JDK 21 후 `NiceQaApplication` Run. 또는:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
.\gradlew.bat bootRun
```

## 테스트

### IntelliJ HTTP Client (권장)

루트의 `api.http`를 열면 요청 블록마다 좌측에 ▶️ 아이콘이 보인다. 클릭해 실행하면 응답이 하단 패널에 뜨고, 파일 끝에 적어둔 `>>! devrequest.zip` 지시문 덕분에 zip이 프로젝트 루트에 자동 저장된다. Git Bash 콘솔 인코딩 같은 문제와 무관하게 한글 요청을 그대로 보낼 수 있어 가장 편하다.

### curl (Git Bash) — 한글은 파일로

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

- `LlmClient` 실어댑터 — `TestController`에서 PoC된 google-genai(Gemini) SDK를 `GeminiLlmClient` 형태로 승격(`provider=external`). 동시에 `TestController`의 하드코딩된 API key는 `${GEMINI_API_KEY}` 환경변수로 분리 필요.
- `KnowledgeClient` 실어댑터 — 사내 KB(`internal` provider) 붙이면서 docs/는 시드/오프라인 모드로 전환. 분기 트리·RAG·규격·유사요청 실데이터로 교체.
- `ExternalSyncPort` SHARE/Plane/Confluence 어댑터 (F13)
- 프론트엔드 React 4화면, 대시보드, 지식저장소 화면
- 확정 표준 정형화 요청서 KB 적재 파이프라인
- 산출물 포맷·요청 스키마 확정에 따른 DTO·MD 템플릿 정비
- 테스트 코드
