---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: provider.payco.v1.2
ns: provider
provider_name: PAYCO
provider_code: PC
provider_type: EASY_PAY
business_no: "120-86-65063"
version: "1.2"
doc_source: PAYCO_연동가이드_v1.1.9.48_간편,바로구매_통합형.pdf
doc_url: https://devcenter.payco.com/guide/online/easypay/flow?id=220401002
effective_date: 2025-08-01
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
  encryption: AES256
  hash_algo: SHA256
  charset: UTF-8
  content_type: application/json
  timeout_sec: 1800

# ============================================================
# [C] 지원 결제수단 매트릭스
# ============================================================
payment_methods:
  - code: CARD
    name: 신용/체크카드
    min_amount: 100
    max_amount: 10000000
    currency: KRW
    installment: [0,2,3,4,5,6,7,8,9,10,11,12]
    partial_cancel: true
    cancel_deadline_days: 180
  - code: POINT
    name: PAYCO 포인트
    min_amount: 1
    max_amount: 500000
    currency: KRW
    installment: [0]
    partial_cancel: false
    cancel_deadline_days: 180
  - code: BANK
    name: 계좌이체(PAYCO 머니)
    min_amount: 100
    max_amount: 2000000
    currency: KRW
    installment: [0]
    partial_cancel: true
    cancel_deadline_days: 180

# ============================================================
# [D] 엔드포인트 (환경 분리)
# ============================================================
endpoints:
  prod:
    auth_url:      https://online-pay.payco.com/easypay/reserveOrder
    approval_url:  https://online-pay.payco.com/easypay/approval
    cancel_url:    https://online-pay.payco.com/easypay/cancel
    netcancel_url: https://online-pay.payco.com/easypay/netCancel
    status_url:    https://online-pay.payco.com/easypay/status
  dev:
    auth_url:      https://stg-online-pay.payco.com/easypay/reserveOrder
    approval_url:  https://stg-online-pay.payco.com/easypay/approval
    cancel_url:    https://stg-online-pay.payco.com/easypay/cancel
    netcancel_url: https://stg-online-pay.payco.com/easypay/netCancel
    status_url:    https://stg-online-pay.payco.com/easypay/status

# ============================================================
# [E] 인증/계약 정보 (실값은 KMS/Vault, MD에는 ENV 참조만)
# ============================================================
credentials:
  merchant_id_format: "영문대문자 + 숫자 조합 10자리"
  merchant_id_ref: ${PROVIDER_PAYCO_MID}
  merchant_key_type: SECRET_KEY
  merchant_key_length: 64
  merchant_key_ref: ${PROVIDER_PAYCO_SECRET_KEY}
  client_id_ref: ${PROVIDER_PAYCO_CLIENT_ID}
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
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
---

## 1. 필수 요청 파라미터

### 1-1. 인증(reserveOrder) 요청 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 조건 | 예시 | 비고 |
|---|---|---|---|---|---|---|---|
| sellerKey | 가맹점 키 | string | 64 | Y | - | `${PROVIDER_PAYCO_SECRET_KEY}` | 환경변수 참조 |
| orderNo | 주문번호 | string | 40 | Y | 영문+숫자 | `ORD20260622001` | 가맹점 유일값 |
| productName | 상품명 | string | 100 | Y | - | `결제테스트상품` | UTF-8 |
| totalAmount | 총 결제금액 | int | 10 | Y | ≥ 100 | `15000` | KRW, 원단위 |
| currency | 통화 | string | 3 | Y | - | `KRW` | ISO 4217 |
| returnUrl | 결과 수신 URL | string | 200 | Y | HTTPS | `https://shop.com/payco/return` | 인증결과 POST |
| cancelUrl | 취소 수신 URL | string | 200 | Y | HTTPS | `https://shop.com/payco/cancel` | 사용자 취소 시 |
| userCi | 사용자 CI | string | 88 | N | - | - | 본인인증 연동 시 |
| deviceType | 디바이스 구분 | string | 10 | Y | PC/MOBILE | `MOBILE` | 채널 분기 |

### 1-2. 승인(approval) 요청 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 조건 | 예시 | 비고 |
|---|---|---|---|---|---|---|---|
| paymentId | PAYCO 거래키 | string | 30 | Y | 인증응답값 | `2026062201234567` | 인증 완료 후 발급 |
| sellerOrderReferenceKey | 가맹점 주문번호 | string | 40 | Y | - | `ORD20260622001` | 인증 시 orderNo와 동일 |
| totalAmount | 총 결제금액 | int | 10 | Y | 인증값 일치 | `15000` | 위변조 검증 대상 |
| signData | 위변조 해시 | string | 64 | Y | SHA256 | `a3f9...` | §3 참조 |

## 2. 응답 파라미터 / 결과 코드

### 2-1. 인증 응답 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 예시 | 비고 |
|---|---|---|---|---|---|---|
| code | 결과코드 | string | 4 | Y | `0000` | §2-3 참조 |
| message | 결과메시지 | string | 200 | Y | `정상처리` | - |
| paymentId | PAYCO 거래키 | string | 30 | Y | `2026062201234567` | 승인 요청 시 사용 |
| orderNo | 주문번호 | string | 40 | Y | `ORD20260622001` | 가맹점 주문번호 |
| reserveOrderNo | 인증 거래키 | string | 30 | Y | `R20260622001` | 추적용 |

### 2-2. 승인 응답 파라미터
| 파라미터 | 한글명 | 타입 | 길이 | 필수 | 예시 | 비고 |
|---|---|---|---|---|---|---|
| code | 결과코드 | string | 4 | Y | `0000` | - |
| paymentId | PAYCO 거래키 | string | 30 | Y | `2026062201234567` | - |
| totalAmount | 최종 결제금액 | int | 10 | Y | `15000` | - |
| approvedAt | 승인일시 | datetime | 14 | Y | `20260622103045` | yyyyMMddHHmmss |
| paymentMethodType | 결제수단 | string | 10 | Y | `CARD` | CARD/POINT/BANK |
| cardCorpCode | 카드사 코드 | string | 4 | N | `01` | CARD인 경우 |
| cardInstallmentMonth | 할부개월 | int | 2 | N | `3` | CARD인 경우 |

### 2-3. 결과 코드표
| 결과코드 | 의미 | 처리방향 |
|---|---|---|
| `0000` | 정상승인 | SUCCESS |
| `2001` | 사용자 결제 취소 | USER_CANCEL |
| `3001` | 한도초과 | FAIL |
| `3002` | 잔액부족 | FAIL |
| `3010` | 최소금액 미달 | FAIL (P-404 위반) |
| `4001` | SignData 위변조 검증 실패 | FAIL (spec.signdata.v2 위반) |
| `5001` | 승인 타임아웃 | NET_CANCEL_TRIGGER |
| `5002` | 네트워크 오류 | NET_CANCEL_TRIGGER |
| `9999` | 시스템 오류 | NET_CANCEL_TRIGGER |

## 3. SignData 생성 규칙

```yaml
sign_data:
  algorithm: SHA256
  encoding: HEX_LOWER
  input_order: [sellerKey, orderNo, totalAmount, paymentId]
  separator: ""
  example_input: "SECRET_KEY_64CHARS+ORD20260622001+15000+2026062201234567"
  example_output: "a3f9b2c8e1d4...(64자리 hex)"
  authoritative_spec: spec.signdata.v2
```

**생성 의사코드**
```
signData = SHA256(sellerKey + orderNo + totalAmount + paymentId).toHexLower()
```

**검증 시점**
- 승인 요청 직전 가맹점이 생성하여 전송
- PAYCO가 동일 알고리즘으로 재계산 후 일치 검증
- 불일치 시 `4001` 응답 + 거래 무효 처리

## 4. 망취소(Net Cancel) 트리거 사양

```yaml
net_cancel:
  trigger_conditions:
    - APPROVAL_TIMEOUT
    - NETWORK_ERROR
    - CLIENT_DISCONNECT
  timeout_ms: 25000
  retry_count: 1
  retry_interval_ms: 3000
  idempotency_key: paymentId
  endpoint: endpoints.{env}.netcancel_url
```

**트리거 규칙**
- 승인 요청 후 25초 내 응답 없으면 즉시 망취소 호출
- 망취소 1회 재시도 가능 (3초 간격)
- 망취소 성공 = `code: 0000` 응답 수신 시
- 망취소 실패 시 운영 알람 발송 + 수기 정산 처리

## 5. 정책 충돌 가능 지점

```yaml
policy_conflicts:
  - policy_id: P-404
    policy_name: 최소 결제 금액
    provider_value: 1            # POINT는 1원부터 허용
    pg_value: 100                # PG 정책은 100원 이상
    affected_methods: [POINT]
    note: "PAYCO 포인트는 1원부터 허용되나, PG 표준 정책상 100원 미만 거래는 차단 대상. 요청서 작성 시 POINT 결제수단의 min_amount는 100으로 오버라이드 권고. 상세 검증 로직은 policy.min_amount.v1 §2 참조."

  - policy_id: P-501
    policy_name: 부분취소
    provider_value: false        # POINT 부분취소 불가
    pg_value: true               # PG 표준은 부분취소 지원 요구
    affected_methods: [POINT]
    note: "PAYCO 포인트는 전체취소만 가능. 부분취소 요건 포함 시 결제수단을 CARD/BANK로 제한해야 함. 상세 규칙은 policy.partial_cancel.v1 참조."

  - policy_id: P-408
    policy_name: 타임아웃
    provider_value: 25000        # PAYCO 권장 25초
    pg_value: 30000              # PG 표준 30초
    affected_apis: [APPROVAL, NET_CANCEL]
    note: "PAYCO는 25초 타임아웃 권장. PG 표준(30초)보다 짧으므로 PAYCO 기준 우선 적용. policy.timeout.v1에서 provider별 오버라이드 허용."
```

## 6. 테스트/특이사항/변경이력

### 6-1. 테스트 계정
| 항목 | 값 |
|---|---|
| 테스트 MID | `${PROVIDER_PAYCO_TEST_MID}` |
| 테스트 카드번호 | `9410-xxxx-xxxx-xxxx` (개발자센터 제공) |
| 테스트 비밀번호 | PAYCO 개발자센터 발급 |

### 6-2. 알려진 제약
- **점검시간**: 매주 화요일 02:00 ~ 04:00 (KST) — 결제 불가
- **일일한도**: 가맹점별 5억원 (계약 시 조정 가능)
- **건당한도**: CARD 1,000만원 / POINT 50만원 / BANK 200만원
- **IP 화이트리스트**: 운영계 호출 시 사전 등록 필수
- **POINT 결제**: 부분취소 불가, 전체취소만 허용

### 6-3. 변경 이력
| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2024-03-15 | 최초 작성 (간편결제 통합형 기준) | payment-team |
| 1.1 | 2025-01-20 | POINT 결제수단 추가, 부분취소 정책 충돌 명시 | payment-team |
| 1.2 | 2025-08-01 | SignData 입력순서 변경(paymentId 추가), 망취소 타임아웃 25초로 단축 | payment-team |
| 1.2.1 | 2026-06-22 | 정책 ID 체계 마이그레이션 (P-007→P-404, P-012→P-501, P-018→P-408, P-014→spec.signdata.v2) | payment-team |

---
> **주의**: 본 문서의 파라미터·엔드포인트는 PAYCO 개발자센터 가이드(`doc_url` 참조) 기준으로 정형화된 예시이며, 실제 연동 전 최신 가이드와 대조 검증이 필요합니다. 키/시크릿 실값은 절대 본 MD에 기재 금지 — 반드시 `${ENV_VAR}` 참조 방식 사용.
