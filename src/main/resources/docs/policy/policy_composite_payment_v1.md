---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
policy_id: P-405
doc_id: policy.composite_payment.v1
ns: policy
policy_name: 복합결제
category: APPROVAL
version: "1.0"
effective_date: 2026-06-22
status: ACTIVE
authoritative: true
last_updated: 2026-06-22
owner: payment-team
source_doc: NICEPAY 개발자 매뉴얼 — 승인 API 응답(카드, 복합결제 필드 MultiCl/MultiCardAcquAmt/MultiPointAmt/MultiCouponAmt)

# ============================================================
# [B] 적용 범위
# ============================================================
scope:
  payment_methods: [CARD]                 # 복합결제는 카드 승인 응답에 실림
  providers: [PAYCO, KAKAOPAY, TOSSPAY]   # 페이코·카카오·토스 간편결제 복합결제 지원
  applies_to: APPROVAL_RESPONSE
  trigger_field: MultiCl                  # 승인응답 MultiCl=1 시 복합결제

# ============================================================
# [C] 정책 규칙
# ============================================================
rules:
  - id: R-405-1
    condition: "MultiCl == '1'"
    description: "복합결제 사용 거래 — 금액 분해 필드 합산 검증"
    action: VALIDATE_SUM
    error_code: E-APR-405-001

  - id: R-405-2
    condition: "provider NOT IN [PAYCO, KAKAOPAY, TOSSPAY] AND MultiCl == '1'"
    description: "복합결제 미지원 제휴사에 복합결제 응답"
    action: BLOCK
    error_code: E-APR-405-002

  - id: R-405-3
    condition: "MultiCl == '1' AND (MultiCardAcquAmt + MultiPointAmt + MultiCouponAmt) != Amt"
    description: "복합결제 구성 금액 합이 총 승인금액과 불일치"
    action: BLOCK
    error_code: E-APR-405-003

# ============================================================
# [D] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
related_specs:
  - spec.approval.v2                       # 응답 MultiCl/MultiCardAcquAmt/MultiPointAmt/MultiCouponAmt
related_policies:
  - policy.partial_cancel.v1               # P-501 (복합결제 부분취소 제약)
  - policy.min_amount.v1                   # P-404
---

## 1. 정책 목적 및 적용 범위

복합결제(Composite Payment)는 **하나의 거래를 신용카드 + 포인트(또는 머니) + 쿠폰 등 둘 이상의 수단으로 분할 결제**하는 방식이다. 페이코·카카오페이·토스 간편결제에서 지원하며, NICEPAY 승인 응답의 카드 영역에 복합결제 필드가 실린다.

본 정책은 복합결제 거래의 **금액 무결성(구성 금액 합 = 총 승인금액)** 과 **지원 제휴사 여부**를 검증한다.

## 2. 상세 검증 로직

```
# 1. 복합결제 여부 판별 (승인 응답)
IF approval_response.MultiCl != '1':
    SKIP P-405                            # 단일수단 결제는 검증 면제

# 2. 지원 제휴사 확인
IF provider NOT IN [PAYCO, KAKAOPAY, TOSSPAY]:
    RAISE E-APR-405-002                   # 복합결제 미지원 제휴사

# 3. 구성 금액 합산 검증
card   = to_int(approval_response.MultiCardAcquAmt)   # 신용카드 금액
point  = to_int(approval_response.MultiPointAmt)      # 포인트(페이코포인트/카카오머니/토스머니)
coupon = to_int(approval_response.MultiCouponAmt)     # 쿠폰(페이코쿠폰/카카오포인트/토스포인트)
IF (card + point + coupon) != to_int(approval_response.Amt):
    RAISE E-APR-405-003                   # 금액 불일치 (위변조/계산오류)

# 4. 페이코 머니 현금영수증 대상 (참고)
IF provider == PAYCO AND approval_response.MultiRcptAmt > 0:
    현금영수증 발급 대상 금액으로 기록      # MultiRcptAmt
```

## 3. 복합결제 응답 필드 (spec.approval.v2 §2-3)

| 필드 | 의미 |
|---|---|
| `MultiCl` | 복합결제 여부 (0:미사용 1:사용) |
| `MultiCardAcquAmt` | 복합결제 신용카드 금액 |
| `MultiPointAmt` | 복합결제 포인트 금액 (페이코포인트/카카오머니/토스머니) |
| `MultiCouponAmt` | 복합결제 쿠폰 금액 (페이코쿠폰/카카오포인트/토스포인트) |
| `MultiRcptAmt` | (페이코 전용) 페이코머니 현금영수증 발급 대상 금액 |

## 4. 위반 시 처리 절차

1. **E-APR-405-002 (미지원 제휴사)** — 거래 차단, 단일수단 결제로 안내
2. **E-APR-405-003 (금액 불일치)** — 거래 차단 + 보안 알람 (위변조 의심), 감사로그 기록
3. **부분취소 제약** — 복합결제 거래의 부분취소는 P-501(부분취소) 정책과 함께 검토. 수단별 분할 취소 가능 여부는 제휴사 계약에 따름

## 5. 실패 처리 및 에러코드

| 에러코드 | 의미 | 처리 |
|---|---|---|
| `E-APR-405-001` | 복합결제 금액 분해 검증 필요 | 합산 검증 수행 |
| `E-APR-405-002` | 복합결제 미지원 제휴사 | 거래 차단 |
| `E-APR-405-003` | 구성 금액 합 ≠ 총 승인금액 | 거래 차단 + 위변조 알람 |

## 6. 변경 이력

| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2026-06-22 | 최초 작성 — NICEPAY 공식 승인응답 복합결제 필드(MultiCl 등) 기준. P-405 TODO→ACTIVE | payment-team |
