---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.net_cancel_support.v1
ns: policy
policy_id: P-504
policy_name: 제휴사 망취소 제공 여부
category: CANCEL
version: "1.0"
effective_date: 2026-06-22
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team
source_doc: 신규연동개발_정의서_v1.0 (취소 - 제휴사 망취소 제공 여부)

# ============================================================
# [B] 검증 규칙
# ============================================================
rule:
  condition: "provider.endpoints.{env}.netcancel_url IS DEFINED AND provider.net_cancel.trigger_conditions IS NOT EMPTY"
  action: REJECT
  message: "제휴사 망취소 기능이 정의되지 않았거나 트리거 조건이 비어 있습니다."

# ============================================================
# [C] 적용 범위
# ============================================================
scope:
  providers: [ALL]
  payment_methods: [ALL]
  channels: [ALL]
  approval_flow: [TWO_STEP]              # ONE_STEP은 인증=승인이라 망취소 개념 불요

# ============================================================
# [D] 예외 케이스
# ============================================================
exceptions:
  - condition: "provider.approval_flow == 'ONE_STEP'"
    override_value: "망취소 불요"
    note: "단일 호출로 결제 완료되는 ONE_STEP 플로우는 망취소 트리거 발생 불가"
  - condition: "provider.endpoints.netcancel_url == provider.endpoints.cancel_url"
    override_value: "cancel/netcancel 통합 엔드포인트 허용"
    note: "KAKAOPAY/NAVERPAY와 같이 cancel API로 망취소 겸용 시 idempotency_key 필수"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes:
  - code: E-CCL-504-001
    http_status: 500
    message: 망취소 엔드포인트 미정의
  - code: E-CCL-504-002
    http_status: 500
    message: 망취소 트리거 조건 미정의
  - code: E-CCL-504-003
    http_status: 500
    message: idempotency_key 미정의 (통합 엔드포인트 사용 시)

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
  - spec.netcancel.v1                    # 망취소 표준 사양 (NICEPAY 기준)
related_policies:
  - policy.timeout.v1                    # P-408
  - policy.recancel_support.v1           # P-505 (기취소)
  - policy.partial_cancel.v1             # P-501
---

## 1. 정책 목적 및 배경

망취소(Net Cancel)는 **승인 요청은 전송됐으나 응답 수신 실패** 상황에서 거래 무결성을 보장하는 핵심 안전장치다. 제휴사가 망취소를 제공하지 않으면 가맹점 측에서 미결제 상태와 실결제 상태를 식별할 수 없어 정산 불일치·이중매입이 발생한다.

망취소 제공 형태 3가지
| 형태 | 설명 | 사례 |
|---|---|---|
| **별도 엔드포인트** | netcancel 전용 URL 운영 | PAYCO `/netCancel` |
| **통합 엔드포인트** | cancel API에 idempotency_key로 구분 | KAKAOPAY, NAVERPAY `/cancel` |
| **미제공** | 망취소 자체 불가 (반려 대상) | — |

제휴사별 망취소 현황
| 제휴사 | 엔드포인트 형태 | 타임아웃(ms) | 재시도 | idempotency_key |
|---|---|---|---|---|
| PAYCO | 별도 | 25,000 | 1회 | paymentId |
| KAKAOPAY | 통합 | 30,000 | 1회 | tid |
| NAVERPAY | 통합 | 30,000 | 2회 | paymentId |

## 2. 상세 검증 로직

```
# 1. ONE_STEP 플로우는 검증 면제
IF provider.approval_flow == 'ONE_STEP':
    SKIP P-504

# 2. 망취소 엔드포인트 존재
IF provider.endpoints.{env}.netcancel_url IS NULL:
    RAISE E-CCL-504-001

# 3. 트리거 조건 정의
IF provider.net_cancel.trigger_conditions IS EMPTY:
    RAISE E-CCL-504-002

# 4. 통합 엔드포인트인 경우 idempotency_key 필수
IF provider.endpoints.netcancel_url == provider.endpoints.cancel_url:
    IF provider.net_cancel.idempotency_key IS NULL:
        RAISE E-CCL-504-003
```

## 3. 위반 시 처리 절차

1. **엔드포인트 미정의(E-CCL-504-001)**: 제휴사 계약 단계로 회귀, 망취소 API 제공 협의
2. **트리거 미정의(E-CCL-504-002)**: provider MD의 §4 망취소 섹션 보완 후 재검토
3. **idempotency_key 누락(E-CCL-504-003)**: 통합 엔드포인트 사용 시 중복 취소 방지 키 강제 정의

표준 트리거 조건 (`spec.netcancel.v1` 기준)
- `APPROVAL_TIMEOUT` : 승인 응답 타임아웃 (P-408 참조)
- `NETWORK_ERROR` : 소켓/HTTP 단절
- `CLIENT_DISCONNECT` : 가맹점 서버 응답 불가

## 4. 과거 위반 사례 / 반려 히스토리

- 2025-06: 신규 소형 PG사 연동 시도 → 망취소 API 미제공으로 전면 반려, 계약 무산
- 2025-09: KAKAOPAY 연동 초기 버전에서 idempotency_key 미정의 → 동일 거래 2회 망취소 호출, 정산 불일치 발생. v2.0에서 `tid` 필수화로 수정
- 2026-02: NAVERPAY 재시도 2회 정책 반영 누락 → 1회만 호출 후 실패 종료, 운영 수기 대사 발생

## 5. 관련 법규 / 컴플라이언스 근거

- 전자금융거래법 제8조 (거래기록의 생성·보존)
- 전자금융감독규정 제15조 (거래내역의 정확성 확보)
- 한국은행 금융결제망 운영규칙 — 미결제·이중결제 방지 의무
