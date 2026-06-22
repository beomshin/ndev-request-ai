---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
policy_id: P-002
doc_id: policy.error_message_mapping.v1
ns: policy
policy_name: 결제 오류 코드 및 고객 안내 문구 표준 매핑
category: GOVERNANCE
version: "1.0"
effective_date: 2026-06-22
status: ACTIVE
authoritative: true
last_updated: 2026-06-22
owner: PG인증개발팀
source_doc: PG 공통 결제 오류 코드 및 안내 문구 표준 가이드 v1.0

# ============================================================
# [B] 적용 범위
# ============================================================
scope:
  applies_to: ALL                          # 결제 실패 예외처리 기획 검증
  assist_features:
    - F8_PRECHECK                          # 예외처리 문구 기획 여부 검증
  principle: "원천사/PG 에러코드를 가맹점 화면에 그대로 노출 금지 → 고객 친화 문구로 치환"

# ============================================================
# [C] 정책 규칙 (에러코드 → 문구·개발룰 매핑)
# ============================================================
rules:
  - id: ERR_4032
    meaning: 부분 취소 금액 한도 초과 / 부분 취소 불가 거래
    customer_message: "부분 취소가 불가능한 거래입니다. 전체 취소 후 재결제 해주세요."
    dev_rule: "백엔드 환불 모듈에서 부분취소 가능 여부 플래그(isPartialCancelable) 사전 검증 인터페이스 구현"
    maps_to: policy.partial_cancel.v1      # P-501
    internal_code: E-CAN-002
  - id: ERR_LIMIT
    meaning: 카드사 한도 초과 / 잔액 부족
    customer_message: "결제 금액 한도가 초과되었습니다. 다른 결제 수단을 이용해 주세요."
    dev_rule: "결제창 이탈 시 주문서 미파괴 — 기존 주문 데이터 유지한 채 결제수단 선택창으로 복귀 동선 확보"
    maps_to: spec.approval.v2
    internal_code: E-APV-004
  - id: ERR_TIMEOUT
    meaning: 제휴 원천사 응답 타임아웃 (승인 API 무응답)
    customer_message: "결제 처리 중 시간이 초과되었습니다. 주문 내역을 확인해 주세요."
    dev_rule: "실패 팝업 직전 백엔드에서 망취소(NetCancel) API 즉시 자동 호출 강제 + 거래 대사 원인 기록 로그 생성"
    maps_to: spec.netcancel.v1
    internal_code: E-APV-003
  - id: ERR_ALREADY
    meaning: 중복된 주문번호(Moid/orderId) 인입
    customer_message: "이미 처리 중인 주문입니다. 잠시 후 다시 시도해 주세요."
    dev_rule: "클라이언트 UI에서 '결제하기' 더블클릭·새로고침 방지(Debounce/Loading 스피너) 스펙을 UI 기획서에 포함"
    maps_to: spec.approval.v2
    internal_code: E-APV-006

# ============================================================
# [D] 상호 참조
# ============================================================
related_policies:
  - policy.partial_cancel.v1               # P-501 (ERR_4032)
  - policy.new_payment_checklist.v1        # P-001 (CHK-16 부분취소)
related_specs:
  - spec.approval.v2                       # ERR_LIMIT/ERR_ALREADY
  - spec.netcancel.v1                      # ERR_TIMEOUT
---

## 1. 정책 목적 및 적용 범위

개발팀은 결제 실패 시 원천사/PG사에서 내려오는 에러 코드를 **가맹점 화면에 그대로 노출하지 않고, 고객 친화적인 문구로 치환**하여 노출하는 스펙을 기획에 반영해야 한다. AI 어시스트는 현업 인터뷰 시 이 **예외 처리 문구 기획 여부를 검증**한다(F8).

## 2. 에러코드 → 고객 안내 문구 표준 매핑

| 에러코드 | 의미 | 고객 노출 권장 문구 | 우리 내부 코드 |
|---|---|---|---|
| `ERR_4032` | 부분 취소 한도 초과 / 부분 취소 불가 거래 | "부분 취소가 불가능한 거래입니다. 전체 취소 후 재결제 해주세요." | E-CAN-002 |
| `ERR_LIMIT` | 카드사 한도 초과 / 잔액 부족 | "결제 금액 한도가 초과되었습니다. 다른 결제 수단을 이용해 주세요." | E-APV-004 |
| `ERR_TIMEOUT` | 제휴 원천사 응답 타임아웃 | "결제 처리 중 시간이 초과되었습니다. 주문 내역을 확인해 주세요." | E-APV-003 |
| `ERR_ALREADY` | 중복 주문번호(Moid) 인입 | "이미 처리 중인 주문입니다. 잠시 후 다시 시도해 주세요." | E-APV-006 |

## 3. 개발팀 예외 처리 룰 (기획서 반영 필수)

| 에러코드 | 기획서에 반영할 개발 룰 |
|---|---|
| `ERR_4032` | 백엔드 환불 모듈에서 부분취소 가능 여부 플래그(`isPartialCancelable`) 사전 검증 인터페이스 구현. (승인응답 `CcPartCl` 연계, P-501) |
| `ERR_LIMIT` | 결제창 이탈 시 주문서 미파괴 — 기존 주문 데이터를 유지한 채 결제수단 선택창(주문서)으로 자연스럽게 리다이렉트하는 복귀 동선 확보 |
| `ERR_TIMEOUT` | 실패 팝업 표시 직전, 백엔드에서 **망취소(NetCancel) API**를 즉시 자동 호출하도록 설계 강제 + 거래 대사 원인 기록 로그 생성 (spec.netcancel.v1) |
| `ERR_ALREADY` | 클라이언트 UI에서 '결제하기' 더블클릭·새로고침 방지(Debounce/Loading 스피너) 스펙을 UI 기획서에 포함 |

## 4. 예외처리 검증 동작 (F8 연계)

```
1. 요청서에 결제 실패/취소/타임아웃 시나리오 포함 여부 확인
2. FOR each 시나리오:
       IF 해당 ERR_* 코드의 고객 노출 문구 기획 누락:
           추가확인 항목 승격 (RAISE E-GOV-020)
       IF 해당 ERR_* 코드의 개발 예외처리 룰 미반영:
           추가확인 항목 승격 (RAISE E-GOV-021)
3. ERR_TIMEOUT 시나리오는 망취소 자동호출(spec.netcancel.v1) 설계 필수 — 누락 시 critical
```

## 5. 내부 에러코드 교차참조

본 매뉴얼의 원천사/PG 코드(`ERR_*`)는 우리 표준 규격의 내부 에러코드(`E-*`)와 다음과 같이 대응한다.

| 외부(ERR_*) | 내부(E-*) | 관련 규격/정책 |
|---|---|---|
| `ERR_4032` | `E-CAN-002` | spec.netcancel.v1 §5, P-501 부분취소 |
| `ERR_LIMIT` | `E-APV-004` | spec.approval.v2 §5 (매입사 승인 거절) |
| `ERR_TIMEOUT` | `E-APV-003` | spec.approval.v2 §5 → spec.netcancel.v1 망취소 |
| `ERR_ALREADY` | `E-APV-006` | spec.approval.v2 §5 (TID/Moid 중복, 이중매입 방지) |
| (검증 누락) | `E-GOV-020/021` | 본 정책 §4 |

## 6. 변경 이력

| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2026-06-22 | PG 공통 에러코드 가이드 v1.0을 KB 규격(정책)으로 변환 — ERR_4032/LIMIT/TIMEOUT/ALREADY 4종을 고객문구·개발룰로 정형화, 우리 내부 E-코드(E-CAN-002/E-APV-003/004/006) 교차참조, 오타(부부분→부분) 정정 | payment-team |
