---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: spec.netcancel.v1
ns: spec
spec_name: 망취소·승인취소(Cancel) 표준 사양
spec_type: CANCEL
version: "1.1"
effective_date: 2025-03-01
deprecated_date: null
status: ACTIVE
authoritative: true                       # ✅ NICEPAY 공식 매뉴얼(취소 API) 기준 전면 확정
verification_status: OFFICIAL
last_updated: 2026-06-22
owner: payment-team
source_doc: NICEPAY 개발자 매뉴얼 — 취소 API (망취소 / 승인취소 요청·응답)

# ============================================================
# [B] 프로토콜 메타
# ============================================================
algorithm:
  type: PROTOCOL
  name: NICEPAY_CANCEL_v1
  http_method: POST
  content_type: application/x-www-form-urlencoded
  encoding: euc-kr                        # CharSet=utf-8 지정 가능
  endpoints:
    net_cancel: "{NetCancelURL}"          # 인증 응답으로 회신: dc1/dc2 .../webapi/cancel_process.jsp
    approval_cancel: "https://pg-api.nicepay.co.kr/webapi/cancel_process.jsp"
  sign_data:
    net_cancel_request:  "hex(sha256(AuthToken + MID + Amt + EdiDate + MerchantKey))"
    approval_cancel_request: "hex(sha256(MID + CancelAmt + EdiDate + MerchantKey))"   # AuthToken 없음
    response_signature:  "hex(sha256(TID + MID + CancelAmt + MerchantKey))"           # 망취소·승인취소 공통

# ============================================================
# [C] 시퀀스
# ============================================================
sequence:
  - step: 1
    actor: CLIENT
    action: |
      [망취소] 승인(spec.approval.v2) 요청 후 Network 지연/내부오류로 응답 누락 →
              인증응답 NetCancelURL로 망취소 요청
      [승인취소] 정상 승인된 거래를 가맹점이 취소 → cancel_process.jsp로 취소 요청
  - step: 2
    actor: CLIENT
    action: 취소 요청 전문 구성 (단계별 SignData) — server-side
    payload_ref: §2-1 / §2-3
  - step: 3
    actor: NICEPAY
    action: 검증 → 취소 처리 → 결과 응답 (ResultCode + Signature)
    payload_ref: §2-2 / §2-4
  - step: 4
    actor: CLIENT
    action: 응답 Signature 재검증 + 거래상태 갱신 (RemainAmt 반영)

# ============================================================
# [D] 타임아웃·멱등
# ============================================================
timing:
  net_cancel_timeout_ms: 30000
  idempotency_key: TID                    # 동일 TID 망취소 1회 보장 (중복 취소 방지)
  retry_policy: "망취소 응답 누락 시 동일 TID로 재시도 가능(멱등 보장)"

# ============================================================
# [E] 호환성 분기 (provider별 — 잠정값)
# ============================================================
compatibility:
  - provider: PAYCO
    verified: false
  - provider: KAKAOPAY
    endpoint: /online/v1/payment/cancel
    verified: false
  - provider: NAVERPAY
    endpoint: /payments/v2.2/cancel
    verified: false

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_specs:
  - spec.signdata.v2
  - spec.auth.v2
  - spec.approval.v2
related_policies:
  - policy.net_cancel_support.v1
  - policy.partial_cancel.v1             # P-501 (PartialCancelCode)
secrets:
  - name: MerchantKey
    ref: ${NICEPAY_MERCHANT_KEY}
---

## 1. 사양 목적 및 적용 범위

본 사양은 **두 종류의 취소**를 정의한다.

1. **망취소(Net Cancel)** — 승인 요청 후 Network 지연·가맹점 내부 처리오류로 **승인 응답을 받지 못한 경우**, 거래대사 불일치를 막기 위해 인증응답 `NetCancelURL`로 즉시 취소. (거래 무효화)
2. **승인취소(Approval Cancel)** — 정상 승인 완료된 거래를 가맹점이 취소(전체/부분). `https://pg-api.nicepay.co.kr/webapi/cancel_process.jsp`로 요청.

모든 취소 요청·응답은 **server-side에서 처리**하며 민감정보가 외부에 노출되지 않아야 한다. 결제 취소 API 도메인은 서비스에 따라 다를 수 있으므로 각 서비스 가이드의 도메인을 반드시 확인한다.

## 2. 입출력 포맷

### 2-1. 망취소 요청 (NetCancelURL로 POST)

```
Method: POST   Content-Type: application/x-www-form-urlencoded   Encoding: euc-kr
```

| 파라미터 | 타입/길이 | 필수 | 설명 |
|---|---|---|---|
| `TID` | 30 byte | Y | 거래 ID |
| `AuthToken` | 40 byte | Y | 인증 토큰 |
| `MID` | 10 byte | Y | 가맹점 ID |
| `Amt` | 12 byte | N | 금액 |
| `EdiDate` | 14 byte | Y | 전문생성일시 `YYYYMMDDHHMMSS` |
| `NetCancel` | 1 byte | Y | **`1` 고정** (망취소 여부) |
| `SignData` | 256 byte | Y | `hex(sha256(AuthToken + MID + Amt + EdiDate + MerchantKey))` |
| `CharSet` | 10 byte | N | `euc-kr`(default) / `utf-8` |
| `EdiType` | 10 byte | N | `JSON` / `KV` |
| `MallReserved` | 500 byte | N | 가맹점 여분 필드 |

### 2-2. 망취소 응답

| 파라미터 | 타입/길이 | 필수 | 설명 |
|---|---|---|---|
| `ResultCode` | 4 byte | Y | 취소 결과 코드 (예: `2001` 취소 성공) |
| `ResultMsg` | 100 byte | Y | 취소 결과 메시지 |
| `CancelAmt` | 12 byte | Y | 취소 금액 (zero-pad, 예: `000000001000`) |
| `MID` | 10 byte | Y | 가맹점 ID |
| `Moid` | 64 byte | Y | 가맹점 주문번호 |
| `Signature` | 500 byte | - | `hex(sha256(TID + MID + CancelAmt + MerchantKey))` (가맹점 비교 권고) |
| `PayMethod` | 10 byte | - | `CARD` / `BANK` / `VBANK` / `CELLPHONE` |
| `TID` | 30 byte | - | 거래 ID |
| `CancelDate` / `CancelTime` | 8 / 6 byte | - | 취소일자 `YYYYMMDD` / 취소시간 `HHmmss` |
| `CancelNum` | 8 byte | - | 취소번호 |
| `RemainAmt` | 12 byte | - | 취소 후 잔액 |
| `MallReserved` | 500 byte | - | 가맹점 여분 필드 |

### 2-3. 승인취소 요청 (cancel_process.jsp로 POST)

```
Target: https://pg-api.nicepay.co.kr/webapi/cancel_process.jsp
Method: POST   Content-Type: application/x-www-form-urlencoded   Encoding: euc-kr
```

| 파라미터 | 타입/길이 | 필수 | 설명 |
|---|---|---|---|
| `TID` | 30 byte | Y | 거래 ID |
| `MID` | 10 byte | Y | 가맹점 ID |
| `Moid` | 64 byte | Y | 주문번호 (부분취소 시 중복취소 방지용, 별도 계약) |
| `CancelAmt` | 12 byte | Y | 취소금액 |
| `CancelMsg` | 100 byte | Y | 취소사유 (euc-kr) |
| `PartialCancelCode` | 1 byte | Y | **`0`:전체취소 / `1`:부분취소**(별도 계약) → P-501 |
| `EdiDate` | 14 byte | Y | 전문생성일시 `YYYYMMDDHHMMSS` |
| `SignData` | 256 byte | Y | `hex(sha256(MID + CancelAmt + EdiDate + MerchantKey))` — **AuthToken 없음** |
| `CharSet` | 10 byte | N | `euc-kr`(default) / `utf-8` |
| `EdiType` | 10 byte | N | `JSON` / `KV` |
| `MallReserved` | 500 byte | N | 가맹점 여분 필드 |
| `RefundAcctNo` | 16 byte | 조건부 | 가상계좌·휴대폰 익월환불 전용 — 환불계좌번호(숫자만) |
| `RefundBankCd` | 3 byte | 조건부 | 환불계좌코드 (은행코드 참조) |
| `RefundAcctNm` | 10 byte | 조건부 | 환불계좌주명 (euc-kr) |

### 2-4. 승인취소 응답

> 망취소 응답(§2-2)과 동일 구조: `ResultCode` / `ResultMsg` / `CancelAmt` / `MID` / `Moid` / `Signature`(=`hex(sha256(TID + MID + CancelAmt + MerchantKey))`) / `PayMethod` / `TID` / `CancelDate` / `CancelTime` / `CancelNum` / `RemainAmt` / `MallReserved`.

## 3. 검증 절차 (구현 의사코드)

```
# 망취소 — 승인 응답 누락 시
1. ediDate = now('YYYYMMDDHHMMSS')
2. signData = sha256Hex(AuthToken + MID + Amt + ediDate + ${NICEPAY_MERCHANT_KEY})
3. POST {NetCancelURL}  body={TID, AuthToken, MID, Amt, EdiDate, NetCancel:'1', SignData}
4. 응답 Signature 검증: sha256Hex(TID + MID + CancelAmt + MerchantKey) == response.Signature
5. 멱등: 동일 TID 망취소는 1회만 유효 (중복 무시)

# 승인취소 (전체/부분)
1. IF 부분취소: PartialCancelCode='1' (별도 계약 + 카드 응답 CcPartCl=1 확인, P-501)
2. signData = sha256Hex(MID + CancelAmt + ediDate + ${NICEPAY_MERCHANT_KEY})   # AuthToken 없음
3. POST cancel_process.jsp  body={TID, MID, Moid, CancelAmt, CancelMsg, PartialCancelCode, EdiDate, SignData}
4. 응답 RemainAmt로 잔액 갱신, Signature 재검증
```

## 4. 호환성 분기 (provider별 — 잠정값)

| provider | 비고 |
|---|---|
| **NICEPAY 표준** | 본 사양 (공식 확정) |
| **PAYCO/KAKAOPAY/NAVERPAY** | 각 provider 문서 기준 (verified:false) |

## 5. 실패 처리 및 에러코드

| 에러코드 | 의미 | 처리 |
|---|---|---|
| `E-CAN-001` | SignData 불일치 | 거래 차단 (spec.signdata.v2 §5 E-SEC-001) |
| `E-CAN-002` | 부분취소 미계약 거래의 부분취소 시도 | 거래 거부 (P-501) |
| `E-CAN-003` | 취소금액 > 잔액(RemainAmt) | 거래 거부 |
| `E-CAN-004` | 망취소 대상 거래 없음/이미 취소 | 멱등 처리 (성공 간주) |
| `E-CAN-005` | 환불계좌 정보 누락 (가상계좌·휴대폰 익월환불) | 거래 거부 + 입력 요청 |

## 6. 변경 이력

| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2024-06-01 | 최초 작성 (망취소 JSON 모델) | payment-team |
| **1.1** | **2026-06-22** | NICEPAY 공식 취소 API 기준 **전면 확정** — 망취소(NetCancelURL, `NetCancel=1`)·승인취소(cancel_process.jsp, `PartialCancelCode`) 분리, form-urlencoded/euc-kr 반영, SignData 공식(망취소 `AuthToken+MID+Amt+EdiDate+MerchantKey` / 취소 `MID+CancelAmt+EdiDate+MerchantKey`)·응답 Signature(`TID+MID+CancelAmt+MerchantKey`) 확정, 환불계좌 필드(RefundAcctNo/BankCd/AcctNm) 추가. 가상 필드(paymentId/cancelReason/idempotencyKey JSON) 제거 | payment-team |
