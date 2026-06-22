---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: spec.approval.v2
ns: spec
spec_name: 결제 승인(Approval) 표준 사양
spec_type: APPROVAL
version: "2.2"
effective_date: 2025-03-01
deprecated_date: null
status: ACTIVE
authoritative: true                       # ✅ NICEPAY 공식 매뉴얼(승인 API) 기준 전면 확정
verification_status: OFFICIAL
last_updated: 2026-06-22
owner: payment-team
source_doc: NICEPAY 개발자 매뉴얼 — 승인 API (승인 요청/응답 파라미터)

# ============================================================
# [B] 프로토콜 메타
# ============================================================
algorithm:
  type: PROTOCOL
  name: NICEPAY_APPROVAL_v2
  http_method: POST
  content_type: application/x-www-form-urlencoded
  encoding: euc-kr                        # CharSet=utf-8 지정 가능
  approval_flow: TWO_STEP
  call_method: "인증 응답 NextAppURL로 승인 요청 POST"
  next_app_url:                           # 인증 응답 NextAppURL로 둘 중 하나 회신됨
    - https://dc1-api.nicepay.co.kr/webapi/pay_process.jsp
    - https://dc2-api.nicepay.co.kr/webapi/pay_process.jsp
  sign_data_formula: "hex(sha256(AuthToken + MID + Amt + EdiDate + MerchantKey))"
  response_signature_formula: "hex(sha256(TID + MID + Amt + MerchantKey))"

# ============================================================
# [C] 시퀀스
# ============================================================
sequence:
  - step: 1
    actor: CLIENT
    action: spec.auth.v2 완료 → AuthToken / TxTid / NextAppURL / NetCancelURL / Amt / MID / Moid 보유
  - step: 2
    actor: CLIENT
    action: 승인 요청 전문 구성 (TID=TxTid, AuthToken, MID, Amt, EdiDate, SignData) — server-side
    payload_ref: §2-1
  - step: 3
    actor: CLIENT
    action: NextAppURL(pay_process.jsp)로 application/x-www-form-urlencoded POST
  - step: 4
    actor: NICEPAY
    action: SignData 검증 → 매입사 호출 → 승인 결과 응답 (ResultCode + Signature)
    payload_ref: §2-2
  - step: 5
    actor: CLIENT
    action: 응답 Signature 재검증 + Amt 무결성 확인 → 가맹점 DB 거래상태 갱신
  - step: 6
    actor: CLIENT
    action: 타임아웃/응답 누락 시 NetCancelURL(cancel_process.jsp)로 망취소 (spec.netcancel.v1)

# ============================================================
# [D] 타임아웃·재시도
# ============================================================
timing:
  approval_timeout_ms: 30000              # P-408 표준
  retry_count: 0                          # 승인 재시도 절대 금지 (이중매입 위험)
  netcancel_trigger_after_ms: 30000       # 타임아웃 시 즉시 망취소 트리거

# ============================================================
# [E] 호환성 분기  (※ provider 문서 기준 잠정값, 공식 재검증 전)
# ============================================================
compatibility:
  - provider: PAYCO
    endpoint: /easypay/approval
    timeout_ms: 25000
    verified: false
  - provider: KAKAOPAY
    endpoint: /online/v1/payment/approve
    timeout_ms: 30000
    auth_method: HTTP_HEADER
    verified: false
  - provider: NAVERPAY
    endpoint: /payments/v2.2/apply/payment
    timeout_ms: 30000
    auth_method: HEADER_3
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
  - spec.netcancel.v1
related_policies:
  - policy.min_amount.v1                 # P-404
  - policy.timeout.v1                    # P-408
  - policy.installment.v1                # P-402 (응답 CardQuota)
  - policy.composite_payment.v1          # P-405 (응답 MultiCl/MultiCardAcquAmt/MultiPointAmt)
  - policy.partial_cancel.v1             # P-501 (응답 CcPartCl)
---

## 1. 사양 목적 및 적용 범위

결제 승인(Approval) 단계는 인증 완료된 거래의 **실제 금액 차감을 확정**하는 단계다.

승인은 별도 REST 엔드포인트가 아니라, 인증 응답으로 회신된 **`NextAppURL`** (`https://dc1-api...` 또는 `dc2-api.nicepay.co.kr/webapi/pay_process.jsp`)로 인증 결과를 **`POST`(application/x-www-form-urlencoded, euc-kr)** 하여 수행한다. 승인 처리 중 오류(Network 지연·내부 처리오류) 발생 시 `NetCancelURL`(`.../cancel_process.jsp`)로 망취소(`spec.netcancel.v1`)를 요청한다.

승인은 **재시도 절대 금지**가 핵심 — 응답 누락 시 즉시 `NetCancelURL` 망취소로 분기한다.

## 2. 입출력 포맷

### 2-1. 승인 요청 파라미터 (NextAppURL로 POST)

```
Method: POST   Content-Type: application/x-www-form-urlencoded   Encoding: euc-kr
```

| 파라미터 | 타입/길이 | 필수 | 설명 |
|---|---|---|---|
| `TID` | 30 byte | Y | 거래번호 (인증 응답 `TxTid` 사용) |
| `AuthToken` | 40 byte | Y | 인증 토큰 |
| `MID` | 10 byte | Y | 가맹점 아이디 (예: `nicepay00m`) |
| `Amt` | 12 byte | Y | 금액 (숫자만) |
| `EdiDate` | 14 byte | Y | 전문생성일시 `YYYYMMDDHHMMSS` |
| `SignData` | 256 byte | Y | `hex(sha256(AuthToken + MID + Amt + EdiDate + MerchantKey))` |
| `CharSet` | 10 byte | N | 응답 인코딩 `euc-kr`(default) / `utf-8` |
| `EdiType` | 10 byte | N | 응답전문 유형 `JSON` / `KV`(Key=value) |
| `MallReserved` | 500 byte | N | 가맹점 여분 필드 |

> ⚠ 승인 요청 SignData는 인증요청과 **공식이 다르다** — `EdiDate`가 포함되며 순서는 `AuthToken + MID + Amt + EdiDate + MerchantKey`. (§spec.signdata.v2 §2-3)

### 2-2. 승인 응답 파라미터 — 공통

| 파라미터 | 타입/길이 | 설명 |
|---|---|---|
| `ResultCode` | 4 byte | 결과코드. `3001`:신용카드 성공 / `4000`:계좌이체 / `4100`:가상계좌 발급 / `A000`:휴대폰 / `7001`:현금영수증 |
| `ResultMsg` | 100 byte | 결과메시지 (euc-kr) |
| `Amt` | 12 byte | 금액 (zero-pad, 예: 1000원 → `000000001000`) |
| `MID` | 10 byte | 가맹점 ID |
| `Moid` | 64 byte | 가맹점 주문번호 |
| `Signature` | 500 byte | `hex(sha256(TID + MID + Amt + MerchantKey))` — 위변조 검증 (가맹점 비교 권고) |
| `TID` | 30 byte | 거래 ID |
| `AuthCode` | 30 byte | (옵션) 승인 번호 (카드/계좌이체/휴대폰) |
| `AuthDate` | 12 byte | 승인일시 `YYMMDDHHMMSS` |
| `PayMethod` | 10 byte | `CARD` / `BANK` / `VBANK` / `CELLPHONE` |
| `GoodsName` | 40 byte | 상품명 |
| `BuyerName/BuyerTel/BuyerEmail` | 30/20/60 byte | (옵션) 구매자 정보 |
| `MallReserved` | 500 byte | 가맹점 여분 필드 |

### 2-3. 승인 응답 — 카드(CARD) 추가 필드

| 파라미터 | 길이 | 설명 |
|---|---|---|
| `CardCode` / `CardName` | 3 / 20 byte | 결제 카드사 코드·이름 (예: `01` 비씨) |
| `CardNo` | 20 byte | 마스킹 카드번호 (예: `53611234****1234`) |
| `CardQuota` | 2 byte | **할부개월** (`00`=일시불, `03`=3개월) → **P-402 검증 근거** |
| `CardInterest` | 1 byte | 가맹점분담 무이자 여부 (0:미적용 1:적용) |
| `AcquCardCode` / `AcquCardName` | 3 / 100 byte | 매입 카드사 코드·이름 |
| `CardCl` | 3 byte | 0:신용 1:체크 |
| `CcPartCl` | 1 byte | **부분취소 가능 여부** (0:불가 1:가능) → **P-501 검증 근거** |
| `CardType` | 2 byte | 01:개인 02:법인 03:해외 |
| `ClickpayCl` | 2 byte | (옵션) 간편결제 구분: 15:PAYCO 16:KAKAOPAY 20:NAVERPAY 21:SAMSUNGPAY 22:APPLEPAY 25:TOSSPAY 6:SKPAY 7:SSGPAY 18:LPAY |
| `MultiCl` | 1 byte | (옵션) **복합결제 여부** (0:미사용 1:사용) → **P-405 검증 근거** |
| `MultiCardAcquAmt` | 12 byte | (옵션) 복합결제 신용카드 금액 |
| `MultiPointAmt` | 12 byte | (옵션) 복합결제 포인트(페이코포인트/카카오머니/토스머니) 금액 |
| `MultiCouponAmt` | 12 byte | (옵션) 복합결제 쿠폰 금액 |
| `CouponAmt` / `PointAppAmt` | 12 byte | (옵션) 쿠폰/포인트 승인금액 |
| `RcptType` / `RcptTID` / `RcptAuthCode` | - | (옵션) 네이버페이 포인트결제 현금영수증 |

### 2-4. 승인 응답 — 가상계좌(VBANK) / 계좌이체(BANK) 추가 필드

| 결제수단 | 필드 |
|---|---|
| VBANK | `VbankBankCode`(3) `VbankBankName`(20) `VbankNum`(20) `VbankExpDate`(8, yyyyMMdd) `VbankExpTime`(6, HHmmss) |
| BANK | `BankCode`(3) `BankName`(20) `RcptType`(1, 0:발행안함 1:소득공제 2:지출증빙) `RcptTID`(30) `RcptAuthCode`(30) |

## 3. 검증 절차 (구현 의사코드)

```
# 가맹점 server-side — 인증 응답 수신 후 승인 호출
1. 사전 검증:
   IF AuthResultCode != '0000': 승인 호출 금지 (spec.auth.v2 §5)
   IF 인증응답 Signature 재검증 실패: RAISE E-APV-005 (spec.signdata.v2 §2-2)
   IF Amt < policy.min_amount.v1: RAISE P-404
2. 승인 요청 구성:
   ediDate = now('YYYYMMDDHHMMSS')
   signData = sha256Hex(AuthToken + MID + Amt + ediDate + ${NICEPAY_MERCHANT_KEY})
   body = {TID, AuthToken, MID, Amt, EdiDate:ediDate, SignData:signData}
3. 승인 호출:
   response = POST {NextAppURL}  (form-urlencoded, euc-kr, timeout=effective_timeout_ms)
4. 응답 처리:
   IF timeout OR response IS NULL:
       TRIGGER POST {NetCancelURL}   (spec.netcancel.v1)   # 재호출 절대 금지
       RAISE E-APV-003
   VERIFY sha256Hex(TID + MID + Amt + MerchantKey) == response.Signature  # 응답 위변조
   IF mismatch: RAISE E-APV-005
   VERIFY response.Amt == Amt                                              # 금액 무결성
   IF response.ResultCode in [3001,4000,4100,A000,7001]:
       UPDATE db.status = 'APPROVED'
   ELSE: RAISE E-APV-004 (매입사 거절, ResultCode별 안내)
```

## 4. 호환성 분기 (provider별 — 잠정값)

| provider | timeout | 인증/서명 | 비고 |
|---|---|---|---|
| **NICEPAY 표준결제창** | 30000ms | NextAppURL POST + SignData | 본 사양(공식 확정) |
| **PAYCO** | 25000ms ⚠ | SignData 변형 | P-408 오버라이드 (verified:false) |
| **KAKAOPAY** | 30000ms | HTTP 헤더 | SignData 미사용 (verified:false) |
| **NAVERPAY** | 30000ms | 헤더 3종 + 선택 HMAC | (verified:false) |

### 응답 누락 → 망취소 자동 분기 (공통)
```
승인 POST(NextAppURL) → 30초 → 응답 누락 → NetCancelURL POST (spec.netcancel.v1)
                                              ↓  NetCancel=1, idempotency=TID
```

## 5. 실패 처리 및 에러코드

| 에러코드 | 의미 | 처리 |
|---|---|---|
| `E-APV-001` | 인증 미완료 거래의 승인 시도 | 거래 차단 + spec.auth.v2 회귀 |
| `E-APV-002` | 인증 만료 | 사용자 재시도 안내 |
| `E-APV-003` | 승인 응답 누락 | **즉시 NetCancelURL 망취소** (재호출 금지) |
| `E-APV-004` | 매입사 승인 거절 (ResultCode 비성공) | 결과코드별 사용자 안내 |
| `E-APV-005` | 금액/Signature 무결성 위반 | 보안 알람 + 거래 무효화 |
| `E-APV-006` | AuthToken/TID 중복 사용 | 거래 차단 (이중매입 방지) |

### 핵심 안전 원칙
1. 승인 재호출 절대 금지 — 응답 누락 시 무조건 망취소
2. AuthToken/TID 1회성 — 재사용 차단
3. 금액 무결성 — 인증·승인·응답 3시점 Amt 일치 필수

## 6. 변경 이력

| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2023-06-01 | 최초 작성 | payment-team |
| 1.5 | 2024-08-10 | 매입사 코드 응답 필드 추가 | payment-team |
| 2.0 | 2025-03-01 | 망취소 트리거 자동화, paymentId 1회성 강제 | payment-team |
| 2.1 | 2026-06-22 | 인증 응답 기준 부분 정정(NextAppURL 핸드오프), 승인 상세 TBD | payment-team |
| **2.2** | **2026-06-22** | NICEPAY 공식 승인 API 기준 **전면 확정** — 요청 파라미터(TID/AuthToken/EdiDate/SignData), SignData 공식 `AuthToken+MID+Amt+EdiDate+MerchantKey`, 응답 ResultCode(3001/4000/4100/A000/7001)·Signature `TID+MID+Amt+MerchantKey`, 카드/VBANK/BANK 응답 필드 확정. CardQuota·CcPartCl·MultiCl로 P-402/P-501/P-405 검증근거 연결. `authoritative:true` | payment-team |
