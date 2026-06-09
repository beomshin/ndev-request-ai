# nice-qa

QA 테스트케이스 자동 생성기 백엔드. 결제수단/PG/요구사항을 받아 LLM이 만든 케이스를 xlsx로 다운로드.

현재는 스켈레톤 단계. LLM 호출부는 스텁(`StubLlmClient`)으로 대체돼 실제 LLM 없이 end-to-end 동작.

## 스택

Java 21 / Gradle 8.10.2 / Spring Boot 3.5.0 (`web`, `validation`) / Apache POI 5.4.0

## 디렉터리 구조

```
src/main/java/com/nice/qa/
├─ NiceQaApplication.java
├─ controller/      TestCaseController, ApiExceptionHandler
├─ service/         TestCaseService
│  ├─ excel/        ExcelExporter
│  └─ llm/          LlmClient, StubLlmClient, PromptBuilder
└─ dto/             TestCaseRequest, TestCaseDto
src/main/resources/application.yml
api.http            # IntelliJ HTTP Client 테스트용
```

## 클래스 역할

- **NiceQaApplication** — 진입점 (8080).
- **TestCaseController** — `POST /api/testcases/generate`. 검증 후 xlsx 바이너리 응답.
- **ApiExceptionHandler** — 검증 실패를 400 JSON으로 반환 (xlsx 응답과 분리).
- **TestCaseService** — 흐름 오케스트레이션: 프롬프트 → LLM → JSON 파싱 → Excel.
- **LlmClient** — 인터페이스. 사내 LLM 교체 지점.
- **StubLlmClient** — 고정 mock JSON 반환 (현재 활성).
- **PromptBuilder** — 프롬프트 조립부, **자리만 잡아둠** (타 파트 담당).
- **ExcelExporter** — POI XSSF로 xlsx 생성. 헤더/색상/freeze/자동필터 적용.
- **TestCaseRequest / TestCaseDto** — 요청/응답 DTO.

## API

`POST /api/testcases/generate`

```json
{
  "paymentMethod": "신용카드",
  "provider": "나이스페이먼츠",
  "requirements": "ISP 인증, 일시불/할부"
}
```

> ⚠️ **현재 요청 포맷은 흐름 검증용 임시 스펙입니다.** 실제 요구사항 확정 시 필드 구성·검증 규칙이 변경됩니다.

응답: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename="testcases_{yyyyMMdd_HHmmss}.xlsx"`
검증 실패 시: 400 JSON (`fieldErrors`)

### xlsx 포맷

시트 `QA_테스트케이스`, 컬럼 `번호 / 결제수단 / 인증방식 / 할부 / 케이스분류 / 테스트내용 / 예상결과 / 근거`, 케이스분류별 색상(정상=연초록, 예외=연빨강, 경계=연주황).

> ⚠️ **현재 엑셀 포맷도 테스트용 초안입니다.** 실 사용자 요구에 맞춰 컬럼·스타일이 변경됩니다.

## 설정

`application.yml`의 `llm.base-url / llm.model / llm.api-key`는 환경변수 `LLM_BASE_URL / LLM_MODEL / LLM_API_KEY`로 덮어쓰기 가능. 현재 스텁은 이 값을 사용하지 않아 placeholder 그대로 동작.

## 실행

IntelliJ에서 Project SDK = JDK 21로 설정 후 `NiceQaApplication` Run. 또는:

```powershell
.\gradlew.bat bootRun
```

## 테스트

`api.http` 열고 ▶️ 클릭하면 응답이 `testcases.xlsx`로 자동 저장됨 (IntelliJ HTTP Client 권장 — Git Bash/curl은 한글 인코딩 이슈 있음).

## TODO

- `LlmClient` 실제 구현체 (사내 LLM)
- `PromptBuilder` 실 프롬프트 작성 (타 파트)
- 프론트엔드
- 요청/엑셀 포맷 확정에 따른 DTO·Exporter 수정
- 테스트 코드
