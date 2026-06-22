---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.target_channel.v1
ns: policy
policy_id: P-304
policy_name: 대상 채널
category: AUTH
version: "1.0"
effective_date: 2026-06-22
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team
source_doc: 신규연동개발_정의서_v1.0 (인증 - 대상 채널, 웹/모바일 통합 결제창)

# ============================================================
# [B] 검증 규칙
# ============================================================
rule:
  condition: "request.channel IN provider.integration.channel AND provider.integration.auth_method IS DEFINED"
  action: REJECT
  message: "요청 채널이 제휴사 지원 범위를 벗어났거나, 연동 방식이 정의되지 않았습니다."

# ============================================================
# [C] 적용 범위
# ============================================================
scope:
  providers: [ALL]
  payment_methods: [ALL]
  channels: [ALL]
  approval_flow: [ALL]

# ============================================================
# [D] 예외 케이스
# ============================================================
exceptions:
  - condition: "provider.integration.auth_method == 'SDK'"
    override_value: "SDK 스크립트 사전 로드 요건 추가"
    note: "NaverPay 등 SDK 강제 케이스 — 가맹점 페이지에 JS 로드 필수"
  - condition: "request.channel == 'APP' AND provider.integration.channel NOT CONTAINS 'APP'"
    override_value: "MOBILE_WEB로 자동 fallback"
    note: "앱 미지원 제휴사는 인앱브라우저 경로로 라우팅"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes:
  - code: E-AUT-304-001
    http_status: 400
    message: 미지원 채널 요청
  - code: E-AUT-304-002
    http_status: 500
    message: 연동 방식(auth_method) 미정의
  - code: E-AUT-304-003
    http_status: 500
    message: SDK URL 미정의 (auth_method=SDK인 경우)

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_policies:
  - policy.service_channel.v1            # P-106
  - policy.payment_window_display.v1     # P-305
  - policy.direct_call.v1                # P-302
---

## 1. 정책 목적 및 배경

정의서상 "웹/모바일 통합 결제창" 원칙에 따라, 신규 연동 시 **PC_WEB과 MOBILE_WEB은 단일 통합 결제창**으로 호출되어야 한다. 채널별로 분리된 별도 결제창 운영은 원칙적으로 금지되며, 디바이스 판별은 결제창 내부에서 자동 분기한다.

### 표준 채널 코드
| 코드 | 설명 | 비고 |
|---|---|---|
| `PC_WEB` | PC 웹브라우저 | 통합 결제창 기본값 |
| `MOBILE_WEB` | 모바일 웹브라우저 | PC_WEB과 통합 운영 |
| `APP` | 모바일 앱 (네이티브) | 별도 SDK 또는 인앱 처리 |
| `IN_APP_BROWSER` | 앱 내 웹뷰 | MOBILE_WEB과 동일 라우팅 |

### 연동 방식별 사전 요건
| auth_method | 사전 요건 |
|---|---|
| `REDIRECT` | returnUrl HTTPS 필수 |
| `POPUP` | 팝업 차단 회피 가이드 필요 |
| `IFRAME` | X-Frame-Options 허용 도메인 등록 |
| `API` | 서버 to 서버 호출 |
| **`SDK`** | **JS 스크립트 URL 사전 로드 + 도메인 화이트리스트** |

### 제휴사별 채널·연동 방식 매트릭스
| 제휴사 | PC_WEB | MOBILE_WEB | APP | auth_method |
|---|---|---|---|---|
| PAYCO | O | O | O | REDIRECT |
| KAKAOPAY | O | O | O | REDIRECT |
| NAVERPAY | O | O | O | **SDK** ⚠ |

⚠ NaverPay는 JS SDK 강제 — 정적 REDIRECT 단독 호출 불가

## 2. 상세 검증 로직

```
# 1. 채널 지원 여부
IF request.channel NOT IN provider.integration.channel:
    IF request.channel == 'APP' AND 'MOBILE_WEB' IN provider.integration.channel:
        ROUTE TO MOBILE_WEB  # fallback
    ELSE:
        RAISE E-AUT-304-001

# 2. 연동 방식 정의 여부
IF provider.integration.auth_method IS NULL:
    RAISE E-AUT-304-002

# 3. SDK 방식 추가 검증
IF provider.integration.auth_method == 'SDK':
    IF provider.endpoints.{env}.sdk_url IS NULL:
        RAISE E-AUT-304-003
    REQUIRE 가맹점 페이지에 SDK 스크립트 로드 사전요건 명시

# 4. 통합 결제창 원칙 검증 (정의서 비고)
IF provider.integration.channel CONTAINS 'PC_WEB' AND 'MOBILE_WEB':
    REQUIRE 단일 결제창 URL로 통합 호출 (channel별 분리 호출 금지)
```

## 3. 위반 시 처리 절차

1. **미지원 채널(E-AUT-304-001)**: APP→MOBILE_WEB 자동 fallback 또는 대체 제휴사 추천
2. **연동 방식 미정의(E-AUT-304-002)**: provider MD §B `integration.auth_method` 보강
3. **SDK URL 누락(E-AUT-304-003)**: provider MD §D `endpoints.{env}.sdk_url` 추가 요청
4. **분리 결제창 사용(통합 위반)**: 요청서 반려 + 통합 결제창 가이드 첨부

## 4. 과거 위반 사례 / 반려 히스토리

- 2025-07: PC/모바일 별도 결제창 URL 분리 운영 요청 → 통합 결제창 원칙 위반으로 반려, 단일 URL + UA 기반 자동 분기로 수정
- 2025-11: NaverPay 신규 연동 시 SDK 로드 가이드 누락 → 결제창 호출 실패, P-304 SDK 사전요건 강제 항목 신설
- 2026-04: APP 채널 미지원 PG로 앱 결제 요청 → MOBILE_WEB fallback 적용, 정상 처리

## 5. 관련 법규 / 컴플라이언스 근거

- 전자금융거래법 제6조 (이용자에 대한 명시·고지의무) — 결제 채널 정보 명시
- 정보통신망법 — 모바일 환경 사용자 동의·인증 절차
- 웹 접근성 지침 (WCAG 2.1) — 다양한 디바이스 지원
