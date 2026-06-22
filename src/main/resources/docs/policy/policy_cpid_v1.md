---
# ============================================================
# [A] 식별·메타 정보
# ============================================================
doc_id: policy.cpid.v1
ns: policy
policy_id: P-201
policy_name: CPID
category: KEY
version: "1.0"
effective_date: 2026-06-22
deprecated_date: null
status: ACTIVE
last_updated: 2026-06-22
owner: payment-team
source_doc: 신규연동개발_정의서_v1.0 (제휴사 연동 Key - CPID, 용도별 발급 구분 기재)

# ============================================================
# [B] 검증 규칙
# ============================================================
rule:
  condition: "provider.credentials.merchant_id_ref IS DEFINED AND format(${ENV_VAR})"
  action: REJECT
  message: "CPID(가맹점 식별자)가 정의되지 않았거나, 실값이 MD에 직접 노출되어 있습니다."

# ============================================================
# [C] 적용 범위
# ============================================================
scope:
  providers: [ALL]
  payment_methods: [ALL]
  channels: [ALL]
  approval_flow: [ALL]

# ============================================================
# [D] 예외 케이스
# ============================================================
exceptions:
  - condition: "environment == 'dev' AND provider.credentials.merchant_id_ref.endswith('_TEST_MID')"
    override_value: "테스트 환경 별도 CPID 사용 허용"
    note: "개발/스테이징은 운영 CPID와 반드시 분리 발급"

# ============================================================
# [E] 연관 에러코드
# ============================================================
error_codes:
  - code: E-KEY-201-001
    http_status: 500
    message: CPID 미정의
  - code: E-KEY-201-002
    http_status: 500
    message: CPID 실값 노출 (보안 위반)
  - code: E-KEY-201-003
    http_status: 500
    message: 환경별 CPID 미분리

# ============================================================
# [F] 상호 참조
# ============================================================
refs:
  - provider.payco.v1.2
  - provider.kakaopay.v2.0
  - provider.naverpay.v1.5
related_policies:
  - policy.api_access_control.v1       # P-202
---

## 1. 정책 목적 및 배경

CPID(Channel Partner ID)는 제휴사가 가맹점을 식별하는 **유일 키**로, 정산·매입·취소·대사 전 과정의 추적 기준이 된다. 정의서 비고 "용도별 발급 구분 기재"에 따라 **운영/개발/테스트 환경별로 분리 발급**되어야 하며, 동일 가맹점이라도 서비스 라인(예: 일반결제/정기결제/해외결제)별로 별도 CPID를 발급받아야 한다.

제휴사별 CPID 명칭 매핑
| 제휴사 | CPID 명칭 | 형식 예시 |
|---|---|---|
| PAYCO | MID | 영문대문자+숫자 10자리 |
| KAKAOPAY | CID | `TC0ONETIME`(테스트), `CID0XXXXXX`(운영) |
| NAVERPAY | Partner ID | `np_xxxxxx` (소문자+숫자) |

## 2. 상세 검증 로직

```
# 1. CPID 정의 여부
IF provider.credentials.merchant_id_ref IS NULL:
    RAISE E-KEY-201-001

# 2. 환경변수 참조 형식 (실값 노출 방지)
IF NOT provider.credentials.merchant_id_ref.matches(/^\$\{[A-Z_]+\}$/):
    RAISE E-KEY-201-002

# 3. 환경별 분리 발급
IF environment == 'prod' AND merchant_id_ref.contains('TEST'):
    RAISE E-KEY-201-003
IF environment == 'dev' AND NOT merchant_id_ref.contains('TEST'):
    RAISE E-KEY-201-003
```

## 3. 위반 시 처리 절차

1. **실값 노출(E-KEY-201-002)**: 즉시 git 이력에서 제거 + 키 재발급 요청 (제휴사 통보)
2. **환경 혼용(E-KEY-201-003)**: 배포 차단 + 운영팀 알람
3. **미정의(E-KEY-201-001)**: 제휴사 계약 단계로 회귀, CPID 발급 요청서 작성

## 4. 과거 위반 사례 / 반려 히스토리

- 2025-07: provider MD 파일에 PAYCO 운영 MID 실값 commit → 보안 인시던트, 키 즉시 폐기/재발급
- 2025-10: 개발자가 운영 KAKAOPAY CID로 테스트 진행 → 실거래 발생, 망취소 처리

## 5. 관련 법규 / 컴플라이언스 근거

- 전자금융감독규정 제13조 (접근통제)
- ISMS-P 통제항목 2.7 (정보 자원의 보호) — 인증키 분리 관리
- 개인정보보호법 제29조 (안전조치 의무) — 암호화 및 분리 보관
