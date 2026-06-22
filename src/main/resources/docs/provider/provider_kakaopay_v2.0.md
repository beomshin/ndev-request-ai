---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: provider.kakaopay.v2.0
ns: provider
provider_name: KAKAOPAY
provider_code: KKO
provider_type: EASY_PAY
business_no: "120-81-93979"
version: "2.0"
doc_source: KakaoPay_OnlineSinglePayment_API_v2.0
doc_url: https://developers.kakaopay.com/docs/payment/online/single-payment
effective_date: 2025-05-01
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team

# ============================================================
# [B] 연동 기술 메타
# ============================================================
integration:
  channel: [PC_WEB, MOBILE_WEB, APP]
  auth_method: REDIRECT
  approval_flow: TWO_STEP
  return_method: RETURN_URL
  encryption: TLS
  hash_algo: NONE
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
    name: 카카오페이 머니/포인트
    min_amount: 100
    max_amount: 2000000
    currency: KRW
    installment: [0]
    partial_cancel: true
    cancel_deadline_days: 365
  - code: BANK
    name: 카카오페이 머니(계좌)
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
    auth_url:      https://open-api.kakaopay.com/online/v1/payment/ready
    approval_url:  https://open-api.kakaopay.com/online/v1/payment/approve
    cancel_url:    https://open-api.kakaopay.com/online/v1/payment/cancel
    netcancel_url: https://open-api.kakaopay.com/online/v1/payment/cancel
    status_url:    https://open-api.kakaopay.com/online/v1/payment/order
  dev:
    auth_url:      https://open-api.kakaopay.com/online/v1/payment/ready
    approval_url:  https://open-api.kakaopay.com/online/v1/payment/approve
    cancel_url:    https://open-api.kakaopay.com/online/v1/payment/cancel
    netcancel_url: https://open-api.kakaopay.com/online/v1/payment/cancel
    status_url:    https://open-api.kakaopay.com/online/v1/payment/order

# ============================================================
# [E] 인증/계약 정보 (실값은 KMS/Vault, MD에는 ENV 참조만)
# ============================================================
credentials:
  merchant_id_format: "CID: 영문대문자+숫자 10자리 (예: TC0ONETIME=테스트)"
  merchant_id_ref: ${PROVIDER_KAKAOPAY_CID}
  merchant_key_type: SECRET_KEY
  merchant_key_length: 64
  merchant_key_ref: ${PROVIDER_KAKAOPAY_SECRET_KEY}
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
  - spec.signdata.v2
  - spec.auth.v2
  - spec.approval.v2
related_providers:
  - provider.payco.v1.2
  - provider.naverpay.v1.5
---

## 1. 필수 요청 파라미터

### 1-1. 결제준비(ready) 요청 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 조건 | 예시 | 비고 |
|---|---|---|---|---|---|---|---|
| cid | 가맹점 코드 | string | 10 | Y | - | `TC0ONETIME` | 테스트/운영 분리 |
| partner_order_id | 가맹점 주문번호 | string | 100 | Y | 영문+숫자 | `ORD20260622001` | 가맹점 유일값 |
| partner_user_id | 가맹점 회원 ID | string | 100 | Y | - | `USER12345` | 비로그인 시 게스트 |
| item_name | 상품명 | string | 100 | Y | - | `결제테스트상품` | UTF-8 |
| quantity | 상품수량 | int | 10 | Y | ≥ 1 | `1` | - |
| total_amount | 총 결제금액 | int | 10 | Y | ≥ 100 | `15000` | KRW, 원단위 |
| tax_free_amount | 비과세 금액 | int | 10 | Y | ≥ 0 | `0` | 면세상품 |
| approval_url | 성공 리턴 URL | string | 200 | Y | HTTPS | `https://shop.com/kkopay/approve` | pg_token 수신 |
| fail_url | 실패 리턴 URL | string | 200 | Y | HTTPS | `https://shop.com/kkopay/fail` | - |
| cancel_url | 취소 리턴 URL | string | 200 | Y | HTTPS | `https://shop.com/kkopay/cancel` | 사용자 취소 시 |

### 1-2. 결제승인(approve) 요청 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 조건 | 예시 | 비고 |
|---|---|---|---|---|---|---|---|
| cid | 가맹점 코드 | string | 10 | Y | ready값 일치 | `TC0ONETIME` | - |
| tid | KakaoPay 거래키 | string | 20 | Y | ready응답값 | `T1234567890123456789` | - |
| partner_order_id | 가맹점 주문번호 | string | 100 | Y | ready값 일치 | `ORD20260622001` | - |
| partner_user_id | 가맹점 회원 ID | string | 100 | Y | ready값 일치 | `USER12345` | - |
| pg_token | 인증 토큰 | string | 100 | Y | approval_url 수신값 | `xxxxxxxxxxxxxxxxxxxx` | 1회용 |
| total_amount | 총 결제금액 | int | 10 | N | ready값과 일치 검증 | `15000` | 위변조 방지 |

## 2. 응답 파라미터 / 결과 코드

### 2-1. 결제준비 응답 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 예시 | 비고 |
|---|---|---|---|---|---|---|
| tid | KakaoPay 거래키 | string | 20 | Y | `T1234567890123456789` | approve 시 사용 |
| next_redirect_pc_url | PC 결제창 URL | string | 300 | Y | `https://online-pay.kakao.com/...` | PC 채널 |
| next_redirect_mobile_url | 모바일 결제창 URL | string | 300 | Y | `https://online-pay.kakao.com/...` | MO 채널 |
| next_redirect_app_url | 앱 스킴 | string | 300 | Y | `kakaotalk://kakaopay/pg?url=...` | APP 채널 |
| android_app_scheme | 안드로이드 앱 스킴 | string | 300 | N | - | - |
| ios_app_scheme | iOS 앱 스킴 | string | 300 | N | - | - |
| created_at | 결제 요청시각 | datetime | 19 | Y | `2026-06-22T10:30:00` | ISO 8601 |

### 2-2. 결제승인 응답 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 예시 | 비고 |
|---|---|---|---|---|---|---|
| aid | 요청 고유번호 | string | 20 | Y | `A1234567890123456789` | 승인 고유키 |
| tid | KakaoPay 거래키 | string | 20 | Y | `T1234567890123456789` | - |
| cid | 가맹점 코드 | string | 10 | Y | `TC0ONETIME` | - |
| partner_order_id | 가맹점 주문번호 | string | 100 | Y | `ORD20260622001` | - |
| payment_method_type | 결제수단 | string | 10 | Y | `CARD` | CARD/MONEY |
| amount.total | 최종 결제금액 | int | 10 | Y | `15000` | - |
| amount.tax_free | 비과세금액 | int | 10 | Y | `0` | - |
| amount.discount | 할인금액 | int | 10 | Y | `0` | - |
| card_info.purchase_corp | 매입카드사 | string | 20 | N | `KAKAOBANK` | CARD인 경우 |
| card_info.install_month | 할부개월 | string | 2 | N | `00` | CARD인 경우 |
| approved_at | 승인시각 | datetime | 19 | Y | `2026-06-22T10:30:45` | ISO 8601 |

### 2-3. 결과 코드표
| HTTP | 에러코드 | 의미 | 처리방향 |
|---|---|---|---|
| 200 | - | 정상승인 | SUCCESS |
| 400 | `-780` | 사용자 결제 취소 | USER_CANCEL |
| 400 | `-781` | 결제창 만료(15분 초과) | FAIL (P-408 위반) |
| 400 | `-782` | 금액 한도초과 | FAIL |
| 400 | `-783` | 최소금액 미달 | FAIL (P-404 위반) |
| 400 | `-784` | 잘못된 파라미터 | FAIL |
| 401 | `-401` | 인증실패(Secret Key 오류) | FAIL (spec.signdata.v2 위반) |
| 500 | `-500` | 서버 오류 | NET_CANCEL_TRIGGER |
| 504 | `-504` | 타임아웃 | NET_CANCEL_TRIGGER |

## 3. SignData 생성 규칙

```yaml
sign_data:
  algorithm: NONE                        # KakaoPay는 별도 SignData 미사용
  encoding: NONE
  input_order: []
  separator: ""
  example_input: ""
  example_output: ""
  authoritative_spec: spec.signdata.v2
  alternative_auth: HTTP_HEADER          # Authorization 헤더로 대체
  header_format: "Authorization: SECRET_KEY ${PROVIDER_KAKAOPAY_SECRET_KEY}"
```

**인증 방식 (SignData 대체)**
```
HTTP Header:
  Authorization: SECRET_KEY DEV...(64자)
  Content-Type: application/json
```

- 카카오페이는 위변조 해시(SignData) 대신 **HTTP Authorization 헤더 기반 Secret Key 인증**을 사용
- 모든 API 호출 시 헤더 필수 (`ready`/`approve`/`cancel`/`order` 동일)
- `pg_token`은 1회용 인증 토큰으로 승인 시 자동 검증됨

## 4. 망취소(Net Cancel) 트리거 사양

```yaml
net_cancel:
  trigger_conditions:
    - APPROVAL_TIMEOUT
    - NETWORK_ERROR
    - CLIENT_DISCONNECT
  timeout_ms: 30000
  retry_count: 1
  retry_interval_ms: 5000
  idempotency_key: tid                   # cancel/netcancel 통합 엔드포인트 사용 — P-504 비고
  endpoint: endpoints.{env}.cancel_url
```

**트리거 규칙**
- approve 요청 후 30초 내 응답 없으면 동일 `cid` + `tid`로 cancel 호출
- cancel API는 정상취소·망취소 공통 사용 (구분은 가맹점 책임, idempotency_key 필수 — `policy.net_cancel_support.v1` 검증 항목)
- 망취소 성공 = HTTP 200 + `status: CANCEL_PAYMENT` 응답
- pg_token 미사용 상태에서는 자동 만료되므로 명시적 망취소 불필요한 케이스 존재

## 5. 정책 충돌 가능 지점

```yaml
policy_conflicts:
  - policy_id: P-408
    policy_name: 타임아웃 (결제창 유효시간)
    provider_value: 900          # 15분
    pg_value: 1800               # PG 표준 30분
    affected_apis: [PAYMENT_WINDOW]
    note: "카카오페이 결제창은 15분 후 자동만료(-781). PG 표준(30분)보다 짧으므로 카카오페이 기준 우선 적용. 사용자 안내 문구에 반영 필요."

  - policy_id: P-408
    policy_name: 타임아웃 (망취소)
    provider_value: 30000        # 30초
    pg_value: 30000              # PG 표준 30초
    affected_apis: [APPROVAL, NET_CANCEL]
    note: "PG 표준과 일치. 충돌 없음 (참고용 기록)."

# SignData 관련 스펙 충돌 (정책 아닌 spec 레벨 이슈 — P-014 마이그레이션 결과)
spec_conflicts:
  - spec_id: spec.signdata.v2
    spec_name: SignData 위변조 검증 사양
    provider_value: NONE         # 카카오페이는 SignData 미사용
    spec_standard: SHA256_HEX_LOWER
    note: "카카오페이는 Authorization 헤더 인증으로 대체. 요청서 생성 시 SignData 섹션 대신 헤더 인증 가이드로 분기 필요. spec.signdata.v2의 'alternative_auth: HTTP_HEADER' 조항 적용."
```

## 6. 테스트/특이사항/변경이력

### 6-1. 테스트 계정
| 항목 | 값 |
|---|---|
| 테스트 CID | `TC0ONETIME` (공용 테스트) |
| 테스트 Secret Key | `${PROVIDER_KAKAOPAY_TEST_SECRET_KEY}` |
| 테스트 결제수단 | 카카오톡 로그인 후 카카오페이 머니 사용 |

### 6-2. 알려진 제약
- **결제창 유효시간**: 15분 — 초과 시 `-781` 응답, 사용자 재진입 시 ready부터 재호출
- **pg_token 1회성**: approve 1회 호출 후 무효화. 재호출 시 `-784` 응답
- **점검시간**: 매월 둘째주 화요일 03:00 ~ 05:00 (KST)
- **건당한도**: CARD 2,000만원 / MONEY 200만원
- **IP 화이트리스트**: 운영 CID 사용 시 사전 등록 필수
- **APP 채널**: 카카오톡 미설치 디바이스는 자동 웹 결제창으로 fallback

### 6-3. 변경 이력
| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2024-06-10 | 최초 작성 (구 KakaoPay V1 API 기준) | payment-team |
| 1.5 | 2025-01-15 | open-api.kakaopay.com 신규 엔드포인트 전환 | payment-team |
| 2.0 | 2025-05-01 | Authorization 헤더 인증 방식 정형화, 결제창 유효시간 15분 정책 충돌 명시 | payment-team |
| 2.0.1 | 2026-06-22 | 정책 ID 체계 마이그레이션 (P-019→P-408, P-018→P-408, P-014→spec.signdata.v2) | payment-team |

---
> **주의**: 본 문서의 파라미터·엔드포인트는 카카오페이 개발자센터(`doc_url` 참조) 기준으로 정형화된 예시이며, 실제 연동 전 최신 가이드와 대조 검증이 필요합니다. CID/Secret Key 실값은 절대 본 MD에 기재 금지 — 반드시 `${ENV_VAR}` 참조 방식 사용.
