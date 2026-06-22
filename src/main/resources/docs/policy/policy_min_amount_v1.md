---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.min_amount.v1
ns: policy
policy_id: P-404
policy_name: 최소 결제 금액
category: APPROVAL
version: "1.0"
effective_date: 2026-06-22
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team
source_doc: 신규연동개발_정의서_v1.0 (승인 - 최소 결제 금액)

# ============================================================
# [B] 검증 규칙
# ============================================================
rule:
  condition: "request.amount >= max(PG_STANDARD_MIN, provider.payment_methods[method].min_amount)"
  action: REJECT
  message: "결제금액이 최소 결제금액 기준 미만입니다."
  pg_standard_min: 100                   # PG 표준 최소금액 (KRW)

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
  - condition: "payment_method == 'POINT' AND provider == 'PAYCO'"
    override_value: "PG 표준값(100) 강제 적용 — provider min_amount(1) 무시"
    note: "PAYCO POINT는 원천사 기준 1원부터 가능하나 PG 정책상 100원 미만 BLOCKER"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes:
  - code: E-APR-404-001
    http_status: 400
    message: 최소금액 미달
  - code: E-APR-404-002
    http_status: 400
    message: provider 최소금액 < PG 표준 최소금액 (정책 충돌)

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_policies:
  - policy.linked_payment_method.v1    # P-105
  - policy.composite_payment.v1        # P-405
---

## 1. 정책 목적 및 배경

결제 시스템 운영 안정성과 정산 비용 효율을 위한 **PG 표준 최소 결제금액(100원)** 강제 정책. 제휴사별 결제수단마다 자체 최소금액 기준이 다르므로, 둘 중 **더 큰 값**을 실제 검증 기준으로 사용한다.

PG 표준 최소금액: **100 KRW**

제휴사별 결제수단 최소금액 현황
| 제휴사 | CARD | POINT | BANK |
|---|---|---|---|
| PAYCO | 100 | **1** ⚠ | 100 |
| KAKAOPAY | 100 | 100 | 100 |
| NAVERPAY | 100 | 100 | 100 |

⚠ PAYCO POINT(1원)는 PG 표준(100원)과 충돌 — 100원 강제 적용

## 2. 상세 검증 로직

```
# 1. 적용 최소금액 산출 (PG 표준과 제휴사 값 중 큰 값)
provider_min = provider.payment_methods[request.method].min_amount
effective_min = MAX(100, provider_min)

# 2. 요청 금액 검증
IF request.amount < effective_min:
    RAISE E-APR-404-001 (effective_min, request.amount)

# 3. 정책 충돌 사전 경고 (요청서 생성 단계)
IF provider_min < 100:
    LOG E-APR-404-002 (provider=..., method=..., provider_min=...)
    APPLY policy_override (effective_min=100)
```

## 3. 위반 시 처리 절차

1. **요청서 작성 단계**: 100원 미만 금액 입력 시 즉시 입력 폼 차단
2. **승인 호출 단계**: provider MD의 min_amount가 100 미만이면 자동 오버라이드 + 운영팀 INFO 알람
3. **실거래 100원 미달**: HTTP 400 + `E-APR-404-001` 응답, 거래 무효

## 4. 과거 위반 사례 / 반려 히스토리

- 2025-08: PAYCO POINT 50원 테스트 거래 시도 → P-404 BLOCKER, 거래 차단
- 2025-12: 광고 프로모션 "10원 결제 이벤트" 기획 요청 → P-404 위반으로 반려, 100원 이상으로 기획 변경

## 5. 관련 법규 / 컴플라이언스 근거

- 여신전문금융업법 시행령 — 카드 가맹점 거래 최소금액 관행
- 전자금융거래법 제21조 (안전성 확보 의무) — 소액 거래 남용 방지
- 카드사 약관 — 통상 100원 미만 거래 매입 거부
