---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.{slug}.v{semver}          # 예: policy.min_amount.v1
ns: policy
policy_id: P-XXX                         # 카테고리 prefix + 일련번호 (예: P-404)
policy_name: {한글 정책명}
category: SERVICE                        # SERVICE | KEY | AUTH | APPROVAL | CANCEL | ETC
version: "1.0"
effective_date: YYYY-MM-DD
deprecated_date: null
status: DRAFT                            # ACTIVE | DEPRECATED | DRAFT
last_updated: YYYY-MM-DD
owner: payment-team
source_doc: {정의서 출처}

# ============================================================
# [B] 검증 규칙 (LLM reasoning 입력)
# ============================================================
rule:
  condition: ""                          # 의사코드 (예: "amount >= 100")
  action: REJECT                         # REJECT | WARN | ALLOW
  message: ""                            # 위반 시 표출 메시지

# ============================================================
# [C] 적용 범위
# ============================================================
scope:
  providers: [ALL]                       # [ALL] 또는 [PAYCO, KAKAOPAY, NAVERPAY]
  payment_methods: [ALL]                 # [ALL] 또는 [CARD, POINT, BANK]
  channels: [ALL]                        # [ALL] 또는 [PC_WEB, MOBILE_WEB, APP]
  approval_flow: [ALL]                   # [ALL] 또는 [ONE_STEP, TWO_STEP]

# ============================================================
# [D] 예외 케이스
# ============================================================
exceptions: []
# 예시:
# - condition: "payment_method == POINT"
#   override_value: 1
#   note: "포인트는 1원부터 허용"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes: []
# 예시:
# - code: E-AMT-001
#   http_status: 400
#   message: 최소금액 미달

# ============================================================
# [F] 상호 참조
# ============================================================
refs: []
related_policies: []
---

## 1. 정책 목적 및 배경

## 2. 상세 검증 로직
```
# 의사코드
```

## 3. 위반 시 처리 절차

## 4. 과거 위반 사례 / 반려 히스토리

## 5. 관련 법규 / 컴플라이언스 근거
