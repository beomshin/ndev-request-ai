---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.timeout.v1
ns: policy
policy_id: P-408
policy_name: 타임아웃
category: APPROVAL
version: "1.0"
effective_date: 2026-06-22
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team
source_doc: 신규연동개발_정의서_v1.0 (승인 - 타임아웃, 각 API별 타임아웃 시간 기재)

# ============================================================
# [B] 검증 규칙
# ============================================================
rule:
  condition: "FOR each api IN [PAYMENT_WINDOW, AUTH, APPROVAL, CANCEL, NET_CANCEL]: provider.timeout[api] IS DEFINED"
  action: REJECT
  message: "각 API별 타임아웃 시간이 정의되지 않았습니다."

# ============================================================
# [C] 적용 범위
# ============================================================
scope:
  providers: [ALL]
  payment_methods: [ALL]
  channels: [ALL]
  approval_flow: [ALL]

# ============================================================
# [D] 예외 케이스 (provider별 오버라이드 허용)
# ============================================================
exceptions:
  - condition: "provider.timeout[api] < pg_standard[api]"
    override_value: "provider 값 우선 적용"
    note: "원천사 타임아웃이 PG 표준보다 짧으면 원천사 기준 강제 — 응답 누락 방지"
  - condition: "api == 'NET_CANCEL' AND provider.net_cancel.retry_count > 1"
    override_value: "재시도 횟수 provider 값 허용"
    note: "NaverPay 등 재시도 2회 정책 수용 (INFO 로깅)"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes:
  - code: E-APR-408-001
    http_status: 504
    message: API 타임아웃 (provider 응답 미수신)
  - code: E-APR-408-002
    http_status: 408
    message: 결제창 유효시간 만료 (사용자 미조작)
  - code: E-APR-408-003
    http_status: 500
    message: 타임아웃 설정 미정의

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
  - spec.netcancel.v1
related_policies:
  - policy.net_cancel_support.v1         # P-504
  - policy.api_access_control.v1         # P-202
---

## 1. 정책 목적 및 배경

결제 거래의 각 단계(결제창 노출/인증/승인/취소/망취소)별로 **응답 대기 한계 시간**을 명시하여, 응답 누락 시 정확한 후속 조치(망취소 트리거 등)를 보장한다. 정의서 비고 "각 API별 타임아웃 시간 기재"에 따라 **5개 API 카테고리 전부에 대해 timeout 값을 정의**해야 한다.

### PG 표준 타임아웃 매트릭스
| API 단계 | 표준값 | 단위 | 비고 |
|---|---|---|---|
| `PAYMENT_WINDOW` | 1800 | sec | 결제창 사용자 유효시간 (30분) |
| `AUTH` | 30000 | ms | 인증 요청 응답 |
| `APPROVAL` | 30000 | ms | 승인 요청 응답 |
| `CANCEL` | 30000 | ms | 정상취소 응답 |
| `NET_CANCEL` | 30000 | ms | 망취소 응답 |

### 제휴사별 타임아웃 현황
| 제휴사 | PAYMENT_WINDOW | APPROVAL | NET_CANCEL | 재시도 |
|---|---|---|---|---|
| PAYCO | 1800s | 25000ms ⚠ | 25000ms ⚠ | 1회 |
| KAKAOPAY | 900s ⚠ | 30000ms | 30000ms | 1회 |
| NAVERPAY | 900s ⚠ | 30000ms | 30000ms | **2회** ⚠ |

⚠ PG 표준과 차이 — provider 값이 더 짧으면 provider 우선 적용

## 2. 상세 검증 로직

```
# 1. 5개 API 타임아웃 정의 여부
required_apis = [PAYMENT_WINDOW, AUTH, APPROVAL, CANCEL, NET_CANCEL]
FOR api IN required_apis:
    IF provider.timeout[api] IS NULL:
        RAISE E-APR-408-003

# 2. 적용 타임아웃 결정 (provider 우선)
effective_timeout[api] = MIN(provider.timeout[api], pg_standard[api])

# 3. 실행 시점 검증
IF api_call.elapsed_time > effective_timeout[api]:
    IF api IN [APPROVAL, AUTH]:
        TRIGGER NET_CANCEL                 # P-504 위임
    ELSE IF api == PAYMENT_WINDOW:
        RAISE E-APR-408-002 + 결제창 폐기
    ELSE:
        RAISE E-APR-408-001 + 운영 알람
```

## 3. 위반 시 처리 절차

1. **타임아웃 미정의(E-APR-408-003)**: provider MD §B의 `integration.timeout_sec` + §4 망취소 `timeout_ms` 보강 후 재검토
2. **승인/인증 타임아웃(E-APR-408-001)**: 즉시 망취소 트리거 (`policy.net_cancel_support.v1` 위임)
3. **결제창 유효시간 만료(E-APR-408-002)**: 결제창 폐기 + 사용자에게 재시도 안내 문구 노출

### 결제창 유효시간 사용자 안내 문구 표준
- 표준 30분 적용 시: "30분 내 결제를 완료해 주세요."
- 카카오페이/네이버페이 15분 적용 시: "15분 내 결제를 완료해 주세요."

## 4. 과거 위반 사례 / 반려 히스토리

- 2025-08: KAKAOPAY 결제 시 안내 문구 "30분" 노출 → 실제 15분 만료 → 사용자 CS 다발, P-408 사용자 안내 문구 표준 신설
- 2025-12: NAVERPAY 망취소 1회 시도 후 종료 → 미결제 잔여, P-408 재시도 2회 허용 예외 추가
- 2026-03: 신규 PG 연동 시 `NET_CANCEL` timeout 누락 → E-APR-408-003 발생, 정의서 보강 후 재제출

## 5. 관련 법규 / 컴플라이언스 근거

- 전자금융감독규정 제15조 (거래내역 정확성 확보) — 타임아웃 시 명확한 거래 상태 판정
- ISO 8583 — 금융 메시지 타임아웃 표준 (참고)
- 전자상거래법 — 결제 절차 사용자 안내 의무
