---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.partial_cancel.v1
ns: policy
policy_id: P-501
policy_name: 부분취소
category: CANCEL
version: "1.0"
effective_date: 2026-06-22
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team
source_doc: 신규연동개발_정의서_v1.0 (취소 - 부분취소)

# ============================================================
# [B] 검증 규칙
# ============================================================
rule:
  condition: "request.cancel_type == 'PARTIAL' IMPLIES provider.payment_methods[method].partial_cancel == true"
  action: REJECT
  message: "해당 결제수단은 부분취소를 지원하지 않습니다. 전체취소만 가능합니다."

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
  - condition: "request.cancel_amount == request.original_amount"
    override_value: "전체취소로 자동 변경"
    note: "부분취소 요청이지만 잔액 전액인 경우 전체취소로 라우팅"
  - condition: "payment_method == 'POINT' AND provider == 'PAYCO'"
    override_value: "부분취소 차단 (전체취소만 허용)"
    note: "PAYCO 포인트는 원천사 기준 부분취소 불가"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes:
  - code: E-CCL-501-001
    http_status: 400
    message: 부분취소 미지원 결제수단
  - code: E-CCL-501-002
    http_status: 400
    message: 부분취소 금액이 잔여금액 초과
  - code: E-CCL-501-003
    http_status: 400
    message: 부분취소 누적 회수 한도 초과

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_policies:
  - policy.cancel_deadline.v1            # P-503
  - policy.composite_tax_partial_cancel.v1  # P-502
  - policy.linked_payment_method.v1      # P-105
---

## 1. 정책 목적 및 배경

부분취소는 결제 완료 후 **일부 금액만 환불**하는 기능. 결제수단별로 원천사가 부분취소를 지원하지 않는 경우가 있으므로(특히 포인트성 결제), 요청서 작성 단계에서 제휴사/결제수단 조합의 부분취소 가능 여부를 사전 검증한다.

제휴사·결제수단별 부분취소 지원 현황
| 제휴사 | CARD | POINT | BANK |
|---|---|---|---|
| PAYCO | O | **X** ⚠ | O |
| KAKAOPAY | O | O | O |
| NAVERPAY | O | O | O |

⚠ PAYCO POINT는 전체취소만 가능 — 부분취소 요건 시 결제수단 제한 필요

부분취소 회수 한도 (PG 표준)
- 단일 거래당 최대 부분취소 횟수: **10회**
- 잔액 < 0 발생 시 마지막 부분취소를 전체취소로 자동 변환

## 2. 상세 검증 로직

```
# 1. 부분취소 요청 여부 확인
IF request.cancel_type != 'PARTIAL':
    SKIP P-501

# 2. 결제수단의 부분취소 지원 여부
provider_pm = provider.payment_methods[request.method]
IF provider_pm.partial_cancel == false:
    RAISE E-CCL-501-001

# 3. 잔여금액 검증
remaining = original_amount - SUM(previous_partial_cancels.amount)
IF request.cancel_amount > remaining:
    RAISE E-CCL-501-002

# 4. 누적 회수 한도
IF COUNT(previous_partial_cancels) >= 10:
    RAISE E-CCL-501-003

# 5. 전액 부분취소 시 자동 전체취소 라우팅
IF request.cancel_amount == remaining:
    ROUTE TO cancel_type='FULL'
```

## 3. 위반 시 처리 절차

1. **미지원 결제수단(E-CCL-501-001)**: 요청서 작성 단계에서 결제수단 변경 안내 또는 전체취소로 변경 안내
2. **잔액 초과(E-CCL-501-002)**: 잔여금액 표시 + 재입력 요청
3. **회수 한도 초과(E-CCL-501-003)**: 운영팀 수동 처리 안내 (예외승인 절차)

## 4. 과거 위반 사례 / 반려 히스토리

- 2025-10: PAYCO POINT 부분취소 기능 포함 요청 → 결제수단을 CARD로 제한하거나 전체취소로 변경 권고, 요청서 수정 후 통과
- 2026-01: 11회차 부분취소 시도 → E-CCL-501-003 발생, 운영팀 수기 처리

## 5. 관련 법규 / 컴플라이언스 근거

- 전자상거래법 제17조 (청약철회 등) — 부분 환불 의무
- 여신전문금융업법 제19조 — 카드 거래 부분취소 처리
- 카드사 약관 — 부분취소 누적 회수·기간 제한
