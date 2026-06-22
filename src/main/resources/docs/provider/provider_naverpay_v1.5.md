---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: provider.naverpay.v1.5
ns: provider
provider_name: NAVERPAY
provider_code: NVP
provider_type: EASY_PAY
business_no: "165-87-01717"
version: "1.5"
doc_source: NaverPay_OnetimePayment_AuthWindow_v1.5
doc_url: https://docs.pay.naver.com/docs/onetime-payment/payment/payment-auth-window
effective_date: 2025-04-01
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team

# ============================================================
# [B] 연동 기술 메타
# ============================================================
integration:
  channel: [PC_WEB, MOBILE_WEB, APP]
  auth_method: SDK                       # Naver Pay JS SDK 호출
  approval_flow: TWO_STEP
  return_method: CALLBACK
  encryption: TLS
  hash_algo: HMAC-SHA256
  charset: UTF-8
  content_type: application/json
  timeout_sec: 900

# ============================================================
# [C] 지원 결제수단 매트릭스
# ============================================================
payment_methods:
  - code: CARD
    name: 신용/체크카드
    min_amount: 100
    max_amount: 20000000
    currency: KRW
    installment: [0,2,3,4,5,6,7,8,9,10,11,12]
    partial_cancel: true
    cancel_deadline_days: 365
  - code: POINT
    name: 네이버페이 포인트
    min_amount: 100
    max_amount: 1000000
    currency: KRW
    installment: [0]
    partial_cancel: true
    cancel_deadline_days: 365
  - code: BANK
    name: 네이버페이 머니(계좌)
    min_amount: 100
    max_amount: 2000000
    currency: KRW
    installment: [0]
    partial_cancel: true
    cancel_deadline_days: 365

# ============================================================
# [D] 엔드포인트 (환경 분리)
# ============================================================
endpoints:
  prod:
    auth_url:      https://apis.naver.com/naverpay-partner/naverpay/payments/v2.2/reserve
    approval_url:  https://apis.naver.com/naverpay-partner/naverpay/payments/v2.2/apply/payment
    cancel_url:    https://apis.naver.com/naverpay-partner/naverpay/payments/v1/cancel
    netcancel_url: https://apis.naver.com/naverpay-partner/naverpay/payments/v1/cancel
    status_url:    https://apis.naver.com/naverpay-partner/naverpay/payments/v2.2/inquiry
    sdk_url:       https://nsp.pay.naver.com/sdk/js/naverpay.min.js
  dev:
    auth_url:      https://dev-apis.naver.com/naverpay-partner/naverpay/payments/v2.2/reserve
    approval_url:  https://dev-apis.naver.com/naverpay-partner/naverpay/payments/v2.2/apply/payment
    cancel_url:    https://dev-apis.naver.com/naverpay-partner/naverpay/payments/v1/cancel
    netcancel_url: https://dev-apis.naver.com/naverpay-partner/naverpay/payments/v1/cancel
    status_url:    https://dev-apis.naver.com/naverpay-partner/naverpay/payments/v2.2/inquiry
    sdk_url:       https://test-nsp.pay.naver.com/sdk/js/naverpay.min.js

# ============================================================
# [E] 인증/계약 정보 (실값은 KMS/Vault, MD에는 ENV 참조만)
# ============================================================
credentials:
  merchant_id_format: "Partner ID: 영문소문자+숫자 조합 (예: np_xxxxxx)"
  merchant_id_ref: ${PROVIDER_NAVERPAY_PARTNER_ID}
  merchant_key_type: API_KEY
  merchant_key_length: 40
  merchant_key_ref: ${PROVIDER_NAVERPAY_CLIENT_SECRET}
  client_id_ref: ${PROVIDER_NAVERPAY_CLIENT_ID}
  cert_required: false
  ip_whitelist_required: true

# ============================================================
# [K] 상호 참조
# ============================================================
refs:
  - policy.min_amount.v1                 # P-404
  - policy.partial_cancel.v1             # P-501
  - policy.net_cancel_support.v1         # P-504
  - policy.timeout.v1                    # P-408
  - policy.target_channel.v1             # P-304
  - spec.signdata.v2
  - spec.auth.v2
  - spec.approval.v2
related_providers:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
---

## 1. 필수 요청 파라미터

### 1-1. 결제예약(reserve) 요청 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 조건 | 예시 | 비고 |
|---|---|---|---|---|---|---|---|
| merchantPayKey | 가맹점 주문번호 | string | 40 | Y | 영문+숫자 | `ORD20260622001` | 가맹점 유일값 |
| merchantUserKey | 가맹점 회원 ID | string | 40 | Y | - | `USER12345` | 비회원 시 임의값 |
| productName | 상품명 | string | 130 | Y | - | `결제테스트상품` | UTF-8 |
| productCount | 상품수량 | int | 4 | Y | ≥ 1 | `1` | - |
| totalPayAmount | 총 결제금액 | int | 10 | Y | ≥ 100 | `15000` | KRW |
| taxScopeAmount | 과세대상금액 | int | 10 | Y | ≥ 0 | `13636` | - |
| taxExScopeAmount | 면세대상금액 | int | 10 | Y | ≥ 0 | `0` | - |
| returnUrl | 결과 리턴 URL | string | 200 | Y | HTTPS | `https://shop.com/npay/return` | 인증결과 수신 |
| productItems | 상품정보 배열 | array | - | Y | - | `[{...}]` | 상품 단위 |

### 1-2. 결제승인(applyPayment) 요청 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 조건 | 예시 | 비고 |
|---|---|---|---|---|---|---|---|
| paymentId | NaverPay 거래키 | string | 30 | Y | reserve응답값 | `2026062201234567` | onAuthorize callback 수신 |

> NaverPay는 reserve로 등록된 정보가 paymentId에 바인딩되므로 승인 시 별도 금액·주문번호 재전송 불요. 단, 가맹점 서버에서 reserve 시 저장한 값과 inquiry 응답의 값을 **반드시 일치 검증**해야 함.

## 2. 응답 파라미터 / 결과 코드

### 2-1. 결제예약(reserve) 응답 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 예시 | 비고 |
|---|---|---|---|---|---|---|
| code | 결과코드 | string | 10 | Y | `Success` | §2-3 참조 |
| message | 결과메시지 | string | 200 | Y | `정상처리` | - |
| body.reserveId | 예약 ID | string | 30 | Y | `R20260622001` | SDK 호출 시 사용 |

### 2-2. 결제승인(applyPayment) 응답 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 예시 | 비고 |
|---|---|---|---|---|---|---|
| code | 결과코드 | string | 10 | Y | `Success` | - |
| message | 결과메시지 | string | 200 | Y | `정상처리` | - |
| body.paymentId | NaverPay 거래키 | string | 30 | Y | `2026062201234567` | 환불 시 사용 |
| body.detail.merchantPayKey | 가맹점 주문번호 | string | 40 | Y | `ORD20260622001` | - |
| body.detail.totalPayAmount | 최종 결제금액 | int | 10 | Y | `15000` | - |
| body.detail.primaryPayMeans | 주결제수단 | string | 20 | Y | `CARD` | CARD/POINT/BANK |
| body.detail.cardCorpCode | 카드사 코드 | string | 4 | N | `04` | CARD인 경우 |
| body.detail.cardInstallment | 할부개월 | int | 2 | N | `3` | CARD인 경우 |
| body.detail.admissionYmdt | 승인일시 | datetime | 14 | Y | `20260622103045` | yyyyMMddHHmmss |

### 2-3. 결과 코드표
| 결과코드 | 의미 | 처리방향 |
|---|---|---|
| `Success` | 정상승인 | SUCCESS |
| `UserCancel` | 사용자 결제 취소 | USER_CANCEL |
| `OwnerAuthFail` | 본인인증 실패 | FAIL |
| `BelowMinAmount` | 최소금액 미달 | FAIL (P-404 위반) |
| `OverMaxAmount` | 최대금액 초과 | FAIL |
| `InvalidMerchant` | 가맹점 인증 실패 | FAIL (spec.signdata.v2 위반) |
| `TimeExpired` | 결제창 시간만료(15분) | FAIL (P-408 위반) |
| `ProcessingTimeOut` | 처리시간 초과 | NET_CANCEL_TRIGGER |
| `Fail` | 기타 실패 | FAIL |

## 3. SignData 생성 규칙

```yaml
sign_data:
  algorithm: HMAC-SHA256
  encoding: BASE64
  input_order: [partner_id, merchantPayKey, totalPayAmount, timestamp]
  separator: "."
  example_input: "np_xxxxxx.ORD20260622001.15000.1719048645000"
  example_output: "k3K9...(BASE64)"
  authoritative_spec: spec.signdata.v2
  alternative_auth: HTTP_HEADER
  header_format: "X-NaverPay-Chain-Id: ${PROVIDER_NAVERPAY_CHAIN_ID}\nX-Naver-Client-Id: ${PROVIDER_NAVERPAY_CLIENT_ID}\nX-Naver-Client-Secret: ${PROVIDER_NAVERPAY_CLIENT_SECRET}"
```

**인증 방식**
```
HTTP Header:
  X-NaverPay-Chain-Id: {chain_id}
  X-Naver-Client-Id: {client_id}
  X-Naver-Client-Secret: {client_secret}
  Content-Type: application/json
```

- NaverPay는 **3종 헤더 인증 + 선택적 HMAC-SHA256 SignData** 병행
- 일반 API 호출은 헤더 인증만으로 충분, 위변조 검증 강화 시 HMAC 추가
- `paymentId`는 reserve 시점에 발급되어 1회 승인까지 유효

## 4. 망취소(Net Cancel) 트리거 사양

```yaml
net_cancel:
  trigger_conditions:
    - APPROVAL_TIMEOUT
    - NETWORK_ERROR
    - CLIENT_DISCONNECT
  timeout_ms: 30000
  retry_count: 2
  retry_interval_ms: 5000
  idempotency_key: paymentId             # cancel/netcancel 통합 엔드포인트 사용 — P-504 비고
  endpoint: endpoints.{env}.cancel_url
```

**트리거 규칙**
- applyPayment 요청 후 30초 내 응답 없으면 즉시 cancel API 호출
- 망취소 재시도 최대 2회 (5초 간격) — 타 원천사 대비 1회 추가 (P-408 오버라이드)
- inquiry API로 실제 결제 상태 선조회 후 망취소 권장 (NaverPay 권장 패턴)
- 망취소 성공 = `code: Success` + `body.cancelStatus: Success`

## 5. 정책 충돌 가능 지점

```yaml
policy_conflicts:
  - policy_id: P-304
    policy_name: 대상 채널 (연동 방식)
    provider_value: SDK          # NaverPay는 JS SDK 필수
    pg_value: REDIRECT           # PG 표준은 REDIRECT 권장
    affected_channels: [PC_WEB, MOBILE_WEB]
    note: "NaverPay는 Naver.Pay.create() JS SDK 호출이 필수이며 REDIRECT 단독 호출 불가. 가맹점 페이지에 SDK 스크립트 로드 필요. 요청서 생성 시 'SDK 로드' 사전요건 명시. policy.target_channel.v1 검증 분기."

  - policy_id: P-408
    policy_name: 타임아웃 (결제창 유효시간)
    provider_value: 900          # 15분
    pg_value: 1800               # PG 표준 30분
    affected_apis: [PAYMENT_WINDOW]
    note: "NaverPay 결제창 15분 자동만료(TimeExpired). KakaoPay와 동일하므로 EASY_PAY 공통 정책으로 검토 가능."

  - policy_id: P-408
    policy_name: 타임아웃 (망취소 재시도)
    provider_value: "30000ms x 2회"
    pg_value: "30000ms x 1회"
    affected_apis: [NET_CANCEL]
    note: "타임아웃 자체는 PG 표준과 일치. 단, 재시도 횟수 2회는 PG 표준(1회)을 초과 — policy.timeout.v1에서 provider별 오버라이드 허용."

# SignData 관련 스펙 충돌 (정책 아닌 spec 레벨 이슈 — P-014 마이그레이션 결과)
spec_conflicts:
  - spec_id: spec.signdata.v2
    spec_name: SignData 위변조 검증 사양
    provider_value: OPTIONAL     # HMAC 사용 선택적
    spec_standard: REQUIRED      # PG 표준은 SHA256 필수
    note: "NaverPay는 헤더 인증만으로 호출 가능하나, PG 정책상 위변조 검증 필수. HMAC-SHA256 SignData 적용 강제 필요. spec.signdata.v2 표준 우선."
```

## 6. 테스트/특이사항/변경이력

### 6-1. 테스트 계정
| 항목 | 값 |
|---|---|
| 테스트 Partner ID | `${PROVIDER_NAVERPAY_TEST_PARTNER_ID}` |
| 테스트 Client ID/Secret | `${PROVIDER_NAVERPAY_TEST_CLIENT_*}` |
| 테스트 SDK URL | `https://test-nsp.pay.naver.com/sdk/js/naverpay.min.js` |
| 테스트 환경 | 네이버페이 개발자센터 가입 → 가맹점 테스트 모드 활성화 |

### 6-2. 알려진 제약
- **SDK 의존성**: 가맹점 페이지에 `naverpay.min.js` 스크립트 로드 필수
- **결제창 유효시간**: 15분 — 초과 시 `TimeExpired` 응답
- **paymentId 1회성**: applyPayment 1회 호출 후 무효화
- **점검시간**: 매월 첫째주 수요일 02:00 ~ 04:00 (KST)
- **건당한도**: CARD 2,000만원 / POINT 100만원 / BANK 200만원
- **IP 화이트리스트**: 운영 Partner ID 사용 시 사전 등록 필수
- **APP 채널**: 네이버앱 미설치 시 자동 웹 결제창으로 fallback
- **결제수단 노출 제어**: reserve 시 `payMeansTypeCode` 파라미터로 특정 결제수단만 노출 가능

### 6-3. 변경 이력
| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2024-08-20 | 최초 작성 (v2.2 API 기준) | payment-team |
| 1.3 | 2025-01-10 | HMAC-SHA256 SignData 옵션 추가 | payment-team |
| 1.5 | 2025-04-01 | 망취소 재시도 2회로 확장, P-005(SDK 강제) 정책 충돌 명시 | payment-team |
| 1.5.1 | 2026-06-22 | 정책 ID 체계 마이그레이션 (P-005→P-304, P-019/P-018→P-408, P-014→spec.signdata.v2) | payment-team |

---
> **주의**: 본 문서의 파라미터·엔드포인트는 NaverPay 개발자센터(`doc_url` 참조) 기준으로 정형화된 예시이며, 실제 연동 전 최신 가이드와 대조 검증이 필요합니다. Partner ID/Client Secret/Chain ID 실값은 절대 본 MD에 기재 금지 — 반드시 `${ENV_VAR}` 참조 방식 사용.
