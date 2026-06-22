---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: spec.{slug}.v{semver}             # 예: spec.signdata.v2
ns: spec
spec_name: {사양 명칭}
spec_type: SECURITY                       # SECURITY | AUTH | APPROVAL | CANCEL | NETCANCEL
version: "0.0"
effective_date: YYYY-MM-DD
deprecated_date: null
status: DRAFT                             # ACTIVE | DEPRECATED | DRAFT
authoritative: true                       # 충돌 시 본 문서가 최종 우선
last_updated: YYYY-MM-DD
owner: payment-team
source_doc: NICEPAY 표준 사양서 (출처/버전)

# ============================================================
# [B] 알고리즘/프로토콜 메타
# ============================================================
algorithm:
  type: ""                                # HASH | HMAC | AES | RSA | NONE
  name: ""                                # SHA256 | HMAC-SHA256 | AES256 | RSA-OAEP
  encoding: ""                            # HEX_LOWER | HEX_UPPER | BASE64 | UTF-8
  input_format: ""                        # 입력 조립 규칙

# ============================================================
# [C] 시퀀스 (단계별 actor·action)
# ============================================================
sequence: []
# - step: 1
#   actor: CLIENT
#   action: 요청 발생
#   payload_ref: ""

# ============================================================
# [D] 타임아웃·재시도
# ============================================================
timing:
  timeout_ms: 0
  retry_count: 0
  retry_interval_ms: 0

# ============================================================
# [E] 호환성 분기 (provider별 alternative_auth 등)
# ============================================================
compatibility: []
# - provider: KAKAOPAY
#   alternative: HTTP_HEADER
#   note: ""

# ============================================================
# [F] 상호 참조
# ============================================================
refs: []
related_specs: []
---

## 1. 사양 목적 및 적용 범위

## 2. 입출력 포맷 / 생성 공식

## 3. 검증 절차 (구현 의사코드)

## 4. 호환성 분기 (provider별 예외)

## 5. 실패 처리 및 에러코드

## 6. 변경 이력
