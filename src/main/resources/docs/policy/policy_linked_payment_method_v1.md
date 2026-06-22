---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.linked_payment_method.v1
ns: policy
policy_id: P-105
policy_name: 연동 지불수단
category: SERVICE
version: "1.0"
effective_date: 2026-06-22
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team
source_doc: 신규연동개발_정의서_v1.0 (주요 서비스 정책 - 연동 지불수단)

# ============================================================
# [B] 검증 규칙
# ============================================================
rule:
  condition: "request.payment_method IN provider.payment_methods[*].code"
  action: REJECT
  message: "요청된 결제수단이 해당 제휴사에서 지원하지 않는 결제수단입니다."

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
  - condition: "request.payment_method == 'EASY_PAY_DEFAULT'"
    override_value: "provider 기본 결제수단 라우팅 허용"
    note: "결제수단 미지정 시 제휴사 기본 노출 정책에 위임 (P-306 참조)"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes:
  - code: E-SVC-105-001
    http_status: 400
    message: 미지원 결제수단 요청

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_policies:
  - policy.payment_method_control.v1   # P-306
  - policy.composite_payment.v1        # P-405
---

## 1. 정책 목적 및 배경

신규 제휴사 연동 시 **해당 제휴사가 실제로 제공할 지불수단(CARD/POINT/BANK/MONEY 등)을 명확히 정의**하기 위한 정책. 요청서에 명시된 결제수단이 제휴사의 지원 범위를 벗어나면 개발 단계에서 즉시 반려한다.

표준 결제수단 코드(PG 내부)
- `CARD` : 신용/체크카드
- `POINT` : 제휴사 포인트
- `BANK` : 계좌(머니/이체)
- `MOBILE` : 휴대폰 결제
- `EASY_PAY_DEFAULT` : 결제수단 미지정(제휴사 기본 노출에 위임)

## 2. 상세 검증 로직

```
FOR each pm IN request.payment_methods:
    IF pm NOT IN provider.payment_methods[*].code:
        RAISE E-SVC-105-001
    IF request.amount < provider.payment_methods[pm].min_amount:
        DELEGATE TO P-404 (최소 결제 금액)
    IF request.amount > provider.payment_methods[pm].max_amount:
        RAISE E-SVC-105-002
```

## 3. 위반 시 처리 절차

1. 요청서 자동 반려 + 위반 사유 명시
2. 기획자에게 지원 결제수단 목록 안내 (provider MD의 `payment_methods` 참조)
3. 대체 제휴사 추천 (`related_providers` 활용)

## 4. 과거 위반 사례 / 반려 히스토리

- 2025-09: PAYCO `MOBILE` 결제 요청 → 미지원 결제수단으로 반려, KAKAOPAY로 대체
- 2025-11: NAVERPAY `BANK` + 자동충전 요청 → 자동충전은 별도 계약 필요, 반려

## 5. 관련 법규 / 컴플라이언스 근거

- 전자금융거래법 제6조 (이용자에 대한 명시·고지의무) — 제공 가능 결제수단의 명시
- 여신전문금융업법 제19조의2 (신용카드 가맹점의 준수사항)
