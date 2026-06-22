---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: spec.auth.v2
ns: spec
spec_name: 결제 인증(Authentication) 표준 사양
spec_type: AUTH
version: "2.0"
effective_date: 2025-03-01
deprecated_date: null
status: ACTIVE
authoritative: true
last_updated: 2026-06-22
owner: payment-team
source_doc: NICEPAY_표준인증승인사양서_v2.0 (인증 섹션)

# ============================================================
# [B] 프로토콜 메타
# ============================================================
algorithm:
  type: PROTOCOL
  name: NICEPAY_AUTH_v2
  encoding: UTF-8
  input_format: "JSON request body + signData"
  approval_flow: TWO_STEP                # 인증(reserveOrder) → 승인(approval)

# ============================================================
# [C] 시퀀스
# ============================================================
sequence:
  - step: 1
    actor: USER
    action: 가맹점 결제 페이지 진입
  - step: 2
    actor: CLIENT
    action: 인증요청 전문 생성 (mid, ordNo, amt, ediDate, signData)
    payload_ref: §2-1
  - step: 3
    actor: CLIENT
    action: NICEPAY 결제창 호출 (POST /auth/reserveOrder)
  - step: 4
    actor: USER
    action: 결제수단 선택 + 본인인증 + 비밀번호 입력
  - step: 5
    actor: NICEPAY
    action: 인증 결과 콜백 (returnUrl로 POST)
    payload_ref: §2-2
  - step: 6
    actor: CLIENT
    action: paymentId 수신 → 다음 단계(spec.approval.v2) 진입

# ============================================================
# [D] 타임아웃·재시도
# ============================================================
timing:
  payment_window_sec: 1800                # 결제창 유효시간 30분 (P-408 표준)
  auth_timeout_ms: 30000                  # 인증 요청 응답 타임아웃
  retry_count: 0                          # 인증은 재시도 금지 (사용자 재진입 필요)
  callback_timeout_ms: 10000              # returnUrl 콜백 응답 대기

# ============================================================
# [E] 호환성 분기
# ============================================================
compatibility:
  - provider: PAYCO
    endpoint: /easypay/reserveOrder
    flow: REDIRECT
  - provider: KAKAOPAY
    endpoint: /online/v1/payment/ready
    flow: REDIRECT
    note: "next_redirect_*_url 응답으로 채널별 결제창 URL 제공"
  - provider: NAVERPAY
    endpoint: /payments/v2.2/reserve
    flow: SDK
    note: "reserveId 수신 후 Naver.Pay.create().open() JS 호출"

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_specs:
  - spec.signdata.v2
  - spec.approval.v2
related_policies:
  - policy.target_channel.v1             # P-304
  - policy.payment_window_display.v1     # P-305
  - policy.timeout.v1                    # P-408
---

## 1. 사양 목적 및 적용 범위

결제 인증(Authentication) 단계는 사용자가 **결제수단을 선택하고 본인 인증을 완료**하는 단계로, 실제 금액 차감은 발생하지 않는다. 본 사양은 인증 요청·응답 전문 규격과 결제창 호출 프로토콜을 표준화한다.

TWO_STEP 결제 흐름의 첫 단계로, 본 단계 완료 후 발급된 `paymentId`(거래키)를 통해 `spec.approval.v2` 승인 단계로 진입한다.

## 2. 입출력 포맷

### 2-1. 인증 요청 전문 (Request)

```json
POST /auth/reserveOrder
Content-Type: application/json

{
  "mid": "nicepay00m",
  "ordNo": "ORD20260622001",
  "amt": "15000",
  "goodsName": "결제테스트상품",
  "buyerName": "홍길동",
  "ediDate": "20260622103045",
  "signData": "a3f9b2c8e1d4...8f0e",
  "returnUrl": "https://shop.com/payment/return",
  "deviceType": "MOBILE"
}
```

| 필드 | 타입 | 길이 | 필수 | 설명 |
|---|---|---|---|---|
| `mid` | string | 10 | Y | 가맹점 ID |
| `ordNo` | string | 40 | Y | 가맹점 주문번호 (유일값) |
| `amt` | string | 10 | Y | 결제금액 (원단위) |
| `goodsName` | string | 100 | Y | 상품명 |
| `buyerName` | string | 30 | N | 구매자명 |
| `ediDate` | string | 14 | Y | 전문 생성시각 (yyyyMMddHHmmss) |
| `signData` | string | 64 | Y | SHA256 hex (spec.signdata.v2 §2-1) |
| `returnUrl` | string | 200 | Y | 결과 수신 URL (HTTPS) |
| `deviceType` | string | 10 | Y | PC / MOBILE |

### 2-2. 인증 응답 (returnUrl POST 수신)

```
POST {returnUrl}
Content-Type: application/x-www-form-urlencoded

resultCode=0000
&resultMsg=정상처리
&paymentId=2026062201234567
&ordNo=ORD20260622001
&amt=15000
&authToken=AT-xxxxxxxxxxxx
&ediDate=20260622103120
```

| 필드 | 설명 |
|---|---|
| `resultCode` | `0000`=성공, 그 외 실패 |
| `paymentId` | NICEPAY 거래키 (승인 시 필수) |
| `authToken` | 인증 토큰 (10분 TTL, 승인 시 검증) |

## 3. 검증 절차 (구현 의사코드)

```
# 가맹점 서버 — returnUrl 수신 시점
1. signData 재검증:
   IF spec.signdata.v2 §3 검증 실패: RAISE E-AUT-001
2. ordNo 매칭:
   가맹점 DB 조회 → 동일 ordNo 존재 + 상태='REQUESTED' 확인
3. amt 무결성:
   IF response.amt != db.amt: RAISE E-AUT-002 (금액 위변조)
4. authToken TTL:
   IF (now() - ediDate) > 600s: RAISE E-AUT-003 (인증 만료)
5. 통과 → paymentId 저장 + spec.approval.v2 호출 준비
```

## 4. 호환성 분기 (provider별 예외)

| provider | 차이점 |
|---|---|
| **PAYCO** | 엔드포인트 `/easypay/reserveOrder`, sellerKey 추가 필드 |
| **KAKAOPAY** | 응답에 `next_redirect_pc_url` / `next_redirect_mobile_url` / `next_redirect_app_url` 3종 제공, 클라이언트가 디바이스 분기 |
| **NAVERPAY** | 응답이 `reserveId` 단일값, SDK(`Naver.Pay.create()`)에 전달하여 결제창 호출 — `policy.target_channel.v1` SDK 분기 적용 |

## 5. 실패 처리 및 에러코드

| 에러코드 | 의미 | 처리 |
|---|---|---|
| `E-AUT-001` | signData 불일치 | 거래 차단 + `spec.signdata.v2` §5 E-SEC-001 처리 위임 |
| `E-AUT-002` | 금액 위변조 의심 | 거래 차단 + 보안 알람 |
| `E-AUT-003` | 인증 만료 (10분 초과) | 사용자 재시도 안내 |
| `E-AUT-004` | returnUrl 콜백 미수신 | `spec.netcancel.v1` 망취소 트리거 |
| `E-AUT-005` | 결제창 유효시간 만료 | P-408 `E-APR-408-002` 처리 |

## 6. 변경 이력

| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2023-06-01 | 최초 작성 (ONE_STEP 모델) | payment-team |
| **2.0** | **2025-03-01** | TWO_STEP 모델 전환, paymentId/authToken 분리, returnUrl 콜백 표준화 | payment-team |
