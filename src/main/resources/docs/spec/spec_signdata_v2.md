---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: spec.signdata.v2
ns: spec
spec_name: SignData 위변조 방지 사양
spec_type: SECURITY
version: "2.2"
effective_date: 2025-03-01
deprecated_date: null
status: ACTIVE
authoritative: true                       # 충돌 시 본 문서가 최종 우선
last_updated: 2026-06-22
owner: payment-team
source_doc: NICEPAY 개발자 매뉴얼 — 인증/승인/취소 API (각 단계 SignData·Signature 공식)

# ============================================================
# [B] 알고리즘 메타
# ============================================================
algorithm:
  type: HASH
  name: SHA256
  encoding: HEX_LOWER
  charset: ASCII
  separator: ""                           # 구분자 없이 직접 연결
  output_length: 64                       # SHA256 hex = 64자 (필드 길이 제약 256/500 byte)
  # ── 단계별 SignData(요청) / Signature(응답) 공식 (전부 공식 확정) ──
  formulas:
    auth_request:      "EdiDate + MID + Amt + MerchantKey"            # 인증요청 SignData
    auth_response:     "AuthToken + MID + Amt + MerchantKey"          # 인증응답 Signature
    approval_request:  "AuthToken + MID + Amt + EdiDate + MerchantKey" # 승인요청 SignData
    approval_response: "TID + MID + Amt + MerchantKey"                # 승인응답 Signature
    netcancel_request: "AuthToken + MID + Amt + EdiDate + MerchantKey" # 망취소요청 SignData (승인요청과 동일)
    netcancel_response:"TID + MID + CancelAmt + MerchantKey"          # 망취소응답 Signature
    cancel_request:    "MID + CancelAmt + EdiDate + MerchantKey"      # 승인취소요청 SignData (AuthToken 없음)
    cancel_response:   "TID + MID + CancelAmt + MerchantKey"          # 승인취소응답 Signature

# ============================================================
# [C] 시퀀스
# ============================================================
sequence:
  - step: 1
    actor: CLIENT
    action: 각 단계별 공식에 맞춰 SignData 생성 (가맹점 server-side)
  - step: 2
    actor: NICEPAY
    action: 동일 알고리즘 재계산·비교 검증
  - step: 3
    actor: NICEPAY
    action: 응답에 Signature 포함 회신
  - step: 4
    actor: CLIENT
    action: 가맹점이 응답 Signature 재계산·비교로 응답 위변조 검증 (권고)

# ============================================================
# [D] 타임아웃
# ============================================================
timing:
  ttl_sec: 600                            # EdiDate 기반 전문 유효시간 (내부 운영정책)

# ============================================================
# [E] 호환성 분기 — provider별 (잠정, verified:false)
# ============================================================
compatibility:
  - provider: PAYCO
    method: SIGNDATA_SHA256
    verified: false
    note: "provider.payco.v1.2 §3 우선. 공식 재검증 전 잠정."
  - provider: KAKAOPAY
    method: HTTP_HEADER
    alternative: "Authorization: SECRET_KEY ${KEY}"
    verified: false
    note: "SignData 미사용(헤더 인증)."
  - provider: NAVERPAY
    method: HMAC_SHA256
    encoding: BASE64
    verified: false
    note: "헤더 3종 기본 + 선택적 HMAC-SHA256(BASE64)."
  - provider: ALL
    method: SIGNDATA_SHA256
    note: "위 외 provider는 본 spec 표준 따름."

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_specs:
  - spec.auth.v2
  - spec.approval.v2
  - spec.netcancel.v1
secrets:
  - name: MerchantKey
    ref: ${NICEPAY_MERCHANT_KEY}          # 해시 재료(가맹점 시크릿). 문서·로그·전문 노출 금지
---

## 1. 사양 목적 및 적용 범위

SignData/Signature는 결제 전문의 **위변조를 방지**하기 위한 SHA256 해시값이다. 모든 단계에서 표준 **SHA256 HEX(소문자, 64자)** 를 사용하되, **단계마다 입력 필드 구성이 다르다.** 가맹점은 단계별 공식을 정확히 구분해 생성해야 한다. NICEPAY 공식 매뉴얼(인증/승인/취소 API) 기준이다.

> ⚠ **핵심 주의** — 단계별로 ① `MerchantKey` 위치는 항상 **맨 뒤**, ② `EdiDate` 포함 여부와 위치가 다름, ③ 취소요청만 `AuthToken`이 없음, ④ 취소·망취소 응답은 `Amt`가 아니라 **`CancelAmt`** 를 사용.

## 2. 단계별 생성 공식 (전부 공식 확정)

모든 출력: **소문자 hex 64자**, 구분자 없이 직접 연결.

### 2-1. 인증 (spec.auth.v2)
```
요청 SignData  = hex(sha256( EdiDate + MID + Amt + MerchantKey ))
응답 Signature = hex(sha256( AuthToken + MID + Amt + MerchantKey ))
```

### 2-2. 승인 (spec.approval.v2)
```
요청 SignData  = hex(sha256( AuthToken + MID + Amt + EdiDate + MerchantKey ))
응답 Signature = hex(sha256( TID + MID + Amt + MerchantKey ))
```

### 2-3. 망취소 (spec.netcancel.v1, NetCancelURL)
```
요청 SignData  = hex(sha256( AuthToken + MID + Amt + EdiDate + MerchantKey ))   # 승인요청과 동일
응답 Signature = hex(sha256( TID + MID + CancelAmt + MerchantKey ))
```

### 2-4. 승인취소 (spec.netcancel.v1, cancel_process.jsp)
```
요청 SignData  = hex(sha256( MID + CancelAmt + EdiDate + MerchantKey ))   # AuthToken 없음
응답 Signature = hex(sha256( TID + MID + CancelAmt + MerchantKey ))
```

### 2-5. 단계별 입력 필드 비교표

| 단계 | 방향 | 입력 순서 | 비고 |
|---|---|---|---|
| 인증 | 요청 | `EdiDate + MID + Amt + MerchantKey` | EdiDate 선두 |
| 인증 | 응답 | `AuthToken + MID + Amt + MerchantKey` | |
| 승인 | 요청 | `AuthToken + MID + Amt + EdiDate + MerchantKey` | EdiDate 후위 추가 |
| 승인 | 응답 | `TID + MID + Amt + MerchantKey` | TID 선두 |
| 망취소 | 요청 | `AuthToken + MID + Amt + EdiDate + MerchantKey` | 승인요청과 동일 |
| 망취소 | 응답 | `TID + MID + CancelAmt + MerchantKey` | **CancelAmt** |
| 승인취소 | 요청 | `MID + CancelAmt + EdiDate + MerchantKey` | **AuthToken 없음**, CancelAmt |
| 승인취소 | 응답 | `TID + MID + CancelAmt + MerchantKey` | **CancelAmt** |

### 2-6. 샘플 코드 (Java)

```java
private String sha256Hex(String input) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] h = md.digest(input.getBytes(StandardCharsets.UTF_8));   // 입력 필드 전부 ASCII
    StringBuilder hex = new StringBuilder();
    for (byte b : h) hex.append(String.format("%02x", b));
    return hex.toString();
}
// 승인 요청 예시
String signData = sha256Hex(authToken + mid + amt + ediDate + merchantKey);
// 승인취소 요청 예시 (AuthToken 없음)
String cancelSign = sha256Hex(mid + cancelAmt + ediDate + merchantKey);
```

## 3. 검증 절차 (구현 의사코드)

```
# 가맹점/NICEPAY 공통 — 단계 식별 후 해당 공식 적용
1. 단계 판별: AUTH / APPROVAL / NETCANCEL / CANCEL
2. merchantKey 조회: KMS.get(MID)             # ${NICEPAY_MERCHANT_KEY}
3. 해당 공식으로 재계산 (위 §2)
4. IF expected != received: RAISE E-SEC-001 (위변조) + audit_log
5. TTL: IF abs(now() - EdiDate) > 600s: RAISE E-SEC-002
# 응답 검증 시 취소·망취소 단계는 Amt 대신 CancelAmt 사용에 주의
```

## 4. 호환성 분기 (provider별 예외)

> PAYCO/KAKAOPAY/NAVERPAY 항목은 각 provider 문서 기준 **잠정값**(`verified:false`), NICEPAY 표준결제창 공식과 별개.

- **PAYCO** — provider.payco.v1.2 §3 우선
- **KAKAOPAY** — SignData 미사용, `Authorization: SECRET_KEY ${KEY}` 헤더
- **NAVERPAY** — 헤더 3종 기본 + 선택적 HMAC-SHA256(BASE64)

## 5. 실패 처리 및 에러코드

| 에러코드 | HTTP | 의미 | 처리 |
|---|---|---|---|
| `E-SEC-001` | 401 | SignData/Signature 불일치 (위변조 의심) | 거래 즉시 거부 + 보안 알람 + 감사로그 |
| `E-SEC-002` | 401 | 전문 유효시간 초과 (EdiDate ±10분 위반) | 거래 거부 + 재요청 안내 |
| `E-SEC-003` | 400 | 형식 오류 (64자 hex 아님) | 거래 거부 + 형식 가이드 |
| `E-SEC-004` | 500 | merchantKey 조회 실패 | 운영팀 알람 + KMS 점검 |

### 보안 인시던트 대응
- E-SEC-001이 동일 MID에서 10분 내 5회 이상 발생 시 자동 차단 + 보안팀 호출
- 검증 실패 24개월 감사로그 보존 (전자금융감독규정)

## 6. 변경 이력

| 버전 | 일자 | 변경내용 | 작성자 |
|---|---|---|---|
| 1.0 | 2023-06-01 | 최초 작성 (MD5 기반) | payment-team |
| 1.5 | 2024-11-15 | SHA256 전환, TTL 도입 | payment-team |
| 2.0 | 2025-03-01 | input_format 표준화, compatibility 신설 | payment-team |
| 2.1 | 2026-06-22 | 인증 단계 공식 정정(`EdiDate+MID+Amt+MerchantKey`/응답 `AuthToken+MID+Amt+MerchantKey`), 승인·취소 TBD | payment-team |
| **2.2** | **2026-06-22** | NICEPAY 공식 승인/취소 API 기준 **단계별 8종 공식 전면 확정** — 승인요청 `AuthToken+MID+Amt+EdiDate+MerchantKey`, 승인응답 `TID+MID+Amt+MerchantKey`, 망취소응답·취소 `TID+MID+CancelAmt+MerchantKey`, 취소요청 `MID+CancelAmt+EdiDate+MerchantKey`(AuthToken 없음). TBD 0건 | payment-team |
