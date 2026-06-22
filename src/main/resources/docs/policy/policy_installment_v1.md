---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.installment.v1
ns: policy
policy_id: P-402
policy_name: 할부결제
category: APPROVAL
version: "1.0"
effective_date: 2026-06-22
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team
source_doc: 신규연동개발_정의서_v1.0 (승인 - 할부 개월수)

# ============================================================
# [B] 검증 규칙
# ============================================================
rule:
  condition: "request.installment_month IN provider.payment_methods[method].installment"
  action: REJECT
  message: "해당 결제수단/제휴사가 지원하지 않는 할부 개월수입니다."
  issuer_soft_max: 10                    # 카드사 통상 실한도(개월) — 초과 시 WARN(차단 아님)

# ============================================================
# [C] 적용 범위
# ============================================================
scope:
  providers: [ALL]
  payment_methods: [CARD]                # 할부는 CARD 전용 (POINT/BANK는 일시불=0만)
  channels: [ALL]
  approval_flow: [ALL]

# ============================================================
# [D] 예외 케이스
# ============================================================
exceptions:
  - condition: "request.installment_month == 0"
    override_value: "일시불 — 항상 허용"
    note: "0은 일시불. 모든 결제수단 공통 허용"
  - condition: "payment_method IN ['POINT','BANK']"
    override_value: "할부 불가 — 0(일시불) 외 입력 시 BLOCKER"
    note: "포인트/머니/계좌 결제는 할부 미지원 (provider installment=[0])"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes:
  - code: E-APR-402-001
    http_status: 400
    message: 미지원 할부 개월수 (게이트웨이 installment 목록 외)
  - code: E-APR-402-002
    http_status: 400
    message: 할부 미지원 결제수단에 할부 요청 (POINT/BANK)
  - code: E-APR-402-003
    http_status: 200
    message: 카드사 실한도 초과 가능성 (WARN — 차단 아님, 확인 필요)

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_policies:
  - policy.min_amount.v1                 # P-404
  - policy.composite_payment.v1          # P-405
  - policy.linked_payment_method.v1      # P-105
---

## 1. 정책 목적 및 배경

할부는 **CARD 결제수단에 한해** 적용되며, 결제 요청서/승인 단계에서 요청한 **할부 개월수(`installment_month`)가 실제로 처리 가능한지**를 사전 검증한다. 검증은 두 층으로 나뉜다.

1. **게이트웨이 지원 범위 (하드 룰, BLOCKER)** — 제휴사 MD의 `payment_methods[CARD].installment` 배열에 포함된 값만 허용. 미포함 시 거래 자체가 불가.
2. **카드사 실한도 (소프트 룰, WARN)** — 게이트웨이가 받더라도 **실제 카드사(issuer)별 최대 할부 개월수가 다를 수 있다.** 통상 일부 카드사는 **최대 10개월**까지만 승인하므로, `issuer_soft_max(10)` 초과 요청(11·12개월)은 **차단하지 않되 "카드사 확인 필요" 경고**로 표출한다.

제휴사·결제수단별 할부 지원 현황 (게이트웨이 기준)
| 제휴사 | CARD | POINT | BANK |
|---|---|---|---|
| PAYCO | 0,2~12 | 0 (일시불만) | 0 (일시불만) |
| KAKAOPAY | 0,2~12 | 0 (일시불만) | 0 (일시불만) |
| NAVERPAY | 0,2~12 | 0 (일시불만) | 0 (일시불만) |

> 0 = 일시불. 1개월 할부는 존재하지 않음(2개월부터). 게이트웨이 최대 = **12개월**.
> ⚠ 카드사 실한도(통상 10개월)는 게이트웨이 한도와 별개 — 11·12개월은 사전 확인 권고.

## 2. 상세 검증 로직

```
# 0. 할부 입력 정규화 (미입력 = 일시불)
inst = request.installment_month DEFAULT 0

# 1. 할부 미지원 결제수단 차단
IF request.method IN ['POINT','BANK'] AND inst != 0:
    RAISE E-APR-402-002

# 2. 게이트웨이 지원 목록 검증 (하드 룰)
allowed = provider.payment_methods[request.method].installment
IF inst NOT IN allowed:
    RAISE E-APR-402-001 (inst, allowed)

# 3. 카드사 실한도 소프트 경고 (차단 아님)
IF request.method == 'CARD' AND inst > issuer_soft_max(10):
    WARN E-APR-402-003 (inst, soft_max=10)
    # 거래는 진행, 요청서에 '카드사 할부 한도 확인' 추가확인 항목 승격
```

## 3. 위반 시 처리 절차

1. **미지원 개월수(E-APR-402-001)**: 요청서 작성 단계에서 입력 폼에 허용 개월수만 노출 / 실거래 시 HTTP 400 차단
2. **할부 미지원 결제수단(E-APR-402-002)**: POINT·BANK 선택 시 할부 옵션 비활성화, 입력 시 즉시 차단
3. **카드사 실한도 초과(E-APR-402-003)**: **차단하지 않음.** 요청서에 "11·12개월은 일부 카드사 미지원 — 대상 카드사 확인 필요" 추가확인 항목(F7)으로 승격

## 4. 과거 위반 사례 / 반려 히스토리

- 2025-09: "포인트 6개월 할부 프로모션" 기획 요청 → POINT 할부 미지원(E-APR-402-002)으로 반려, CARD 한정으로 기획 변경
- 2026-02: "전 카드사 12개월 무이자" 요청 → 게이트웨이는 통과하나 일부 카드사 10개월 한도(E-APR-402-003) 경고, 대상 카드사 한정 후 진행

## 5. 관련 법규 / 컴플라이언스 근거

- 여신전문금융업법 — 할부거래 한도 및 수수료 고지 의무
- 할부거래에 관한 법률 제6조 (할부계약의 서면주의)
- 카드사 약관 — 가맹점·카드사별 할부 개월수 및 무이자 적용 범위 상이
