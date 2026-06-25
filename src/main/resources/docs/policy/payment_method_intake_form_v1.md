---
# ============================================================
# 신규 지불수단 등록 — 입력 폼 스키마 (33개 정책 전체)
# - 프론트는 이 front matter(form_schema)만 파싱하여 폼을 렌더링
# - 본문(아래 마크다운)은 담당자가 읽는 작성 가이드
# - 단일 소스: policy_index.yaml (33개 정책) ↔ 본 폼의 fields 가 1:1 대응
# ============================================================
doc_id: form.payment_method_intake.v1
ns: form
title: 신규 지불수단 등록 요청서
version: "1.0"
last_updated: 2026-06-25
source_index: policy_index.yaml
total_fields: 33
submit:
  method: POST
  endpoint: /api/payment-methods            # 폼 제출 대상 (프론트 연동 시 교체)
  success_redirect: /payment-methods/list

# ---- 화면 그룹(탭/아코디언) 정의 : policy_index.yaml 의 6 카테고리와 동일 ----
sections:
  - code: SERVICE
    name: 주요 서비스 정책
    order: 1
  - code: KEY
    name: 제휴사 연동 Key
    order: 2
  - code: AUTH
    name: 인증
    order: 3
  - code: APPROVAL
    name: 승인
    order: 4
  - code: CANCEL
    name: 취소
    order: 5
  - code: ETC
    name: 기타
    order: 6

# ============================================================
# 입력 필드 정의 (정책 1개 = 폼 필드 1개)
#   inputType : text | number | boolean | select | multiselect | group
#   각 필드의 policyId / sourceDocId 로 정책 원본 추적
# ============================================================
fields:
  # ======================= SERVICE (9) =======================
  - policyId: P-101
    section: SERVICE
    label: 인증주체
    inputType: select
    required: true
    options: [PG, 제휴사/원천사]
    defaultValue: PG
    helpText: 결제 인증을 수행하는 주체를 선택하세요.
    sourceDocId: policy.auth_subject.v1

  - policyId: P-102
    section: SERVICE
    label: 승인주체
    inputType: select
    required: true
    options: [PG, 제휴사/원천사]
    defaultValue: PG
    sourceDocId: policy.approval_subject.v1

  - policyId: P-103
    section: SERVICE
    label: 매입주체
    inputType: select
    required: true
    options: [PG, 제휴사/원천사, 매입사]
    defaultValue: PG
    sourceDocId: policy.acquiring_subject.v1

  - policyId: P-104
    section: SERVICE
    label: 현금영수증 발행 주체
    inputType: select
    required: true
    options: [PG, 제휴사/원천사, 가맹점, NONE]
    defaultValue: PG
    sourceDocId: policy.cash_receipt_subject.v1

  - policyId: P-105
    section: SERVICE
    label: 연동 지불수단
    inputType: multiselect
    required: true
    options: [CARD, POINT, MONEY, CASH]
    placeholder: 지원할 결제수단을 모두 선택
    helpText: 제휴사가 실제 제공하는 결제수단만 선택하세요.
    sourceDocId: policy.linked_payment_method.v1

  - policyId: P-106
    section: SERVICE
    label: 서비스 제공 채널
    inputType: multiselect
    required: true
    options: [PC웹, 모바일웹, APP]
    sourceDocId: policy.service_channel.v1

  - policyId: P-107
    section: SERVICE
    label: 통화종류
    inputType: multiselect
    required: true
    options: [KRW, USD, JPY]
    defaultValue: KRW
    sourceDocId: policy.currency.v1

  - policyId: P-108
    section: SERVICE
    label: 제휴사 자체 할인쿠폰
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 제휴사가 자체 발행하는 할인쿠폰 사용 여부입니다.
    sourceDocId: policy.provider_coupon.v1

  - policyId: P-109
    section: SERVICE
    label: 복합과세
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 과세/면세가 혼합된 상품 처리 여부입니다.
    sourceDocId: policy.composite_tax.v1

  # ======================= KEY (2) =======================
  - policyId: P-201
    section: KEY
    label: CPID (가맹점 식별자)
    inputType: text
    required: true
    pattern: "^\\$\\{[A-Z_]+\\}$"
    placeholder: "${PROVIDER_XXX_MID}"
    helpText: 실값 입력 금지. 반드시 환경변수 참조 형식으로 입력하세요.
    validation:
      message: "환경변수 참조 형식(${ENV})만 허용됩니다."
    sourceDocId: policy.cpid.v1

  - policyId: P-202
    section: KEY
    label: API 접근제어
    inputType: select
    required: true
    options: [IP_WHITELIST, OAUTH, MTLS, NONE]
    defaultValue: IP_WHITELIST
    helpText: API 호출 시 적용할 접근제어 방식입니다.
    sourceDocId: policy.api_access_control.v1

  # ======================= AUTH (6) =======================
  - policyId: P-301
    section: AUTH
    label: 인증/비인증 여부
    inputType: multiselect
    required: true
    options: [인증, 비인증]
    defaultValue: AUTH
    helpText: 본인인증 절차 적용 여부입니다.
    sourceDocId: policy.auth_required.v1

  - policyId: P-302
    section: AUTH
    label: 다이렉트 호출 지원 여부
    inputType: boolean
    required: true
    defaultValue: false
    helpText: 결제창 없이 다이렉트 호출을 지원하는지 여부입니다.
    sourceDocId: policy.direct_call.v1

  - policyId: P-303
    section: AUTH
    label: 현금영수증 발행 정보
    inputType: multiselect
    required: false
    options: [NONE, 소득공제, 지출증빙]
    defaultValue: NONE
    helpText: "0:미발행 / 소득공제 / 지출증빙"
    sourceDocId: policy.cash_receipt_info.v1

  - policyId: P-304
    section: AUTH
    label: 대상 채널 (연동 방식)
    inputType: multiselect
    required: true
    options: [PC, 모바일]
    defaultValue: REDIRECT
    helpText: "SDK 선택 시 'SDK 로드 URL' 입력란이 추가로 표시됩니다."
    sourceDocId: policy.target_channel.v1

  - policyId: P-305
    section: AUTH
    label: 결제창 노출 제휴사명
    inputType: group
    required: true
    helpText: 결제창 화면에 노출되는 제휴사명을 언어별로 입력하세요.
    fields:
      - key: ko
        label: 국문
        inputType: text
        required: true
        placeholder: 예) 네이버페이
        maxLength: 30
      - key: en
        label: 영문
        inputType: text
        required: true
        placeholder: 예) NaverPay
        maxLength: 50
      - key: zh
        label: 중문
        inputType: text
        required: false
        placeholder: 예) Naver支付
        maxLength: 30
      - key: ja
        label: 일문
        inputType: text
        required: false
        placeholder: 예) ネイバーペイ
        maxLength: 30
    sourceDocId: policy.payment_window_display.v1

  - policyId: P-306
    section: AUTH
    label: 결제창 제휴사 CI
    inputType: text
    format: url
    required: true
    placeholder: https://cdn.example.com/ci/provider_logo.png
    pattern: "^https://.+\\.(png|jpg|jpeg|svg)$"
    maxLength: 500
    helpText: 결제창에 노출되는 제휴사 CI 이미지의 URL을 입력하세요. (HTTPS, png/jpg/svg)
    validation:
      message: "HTTPS로 시작하는 이미지 URL(png/jpg/jpeg/svg)만 허용됩니다."
    sourceDocId: policy.payment_window_display.v1

  - policyId: P-307
    section: AUTH
    label: 연동 지불수단 제어 여부
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 특정 결제수단만 노출하도록 제어 가능한지 여부입니다.
    sourceDocId: policy.payment_method_control.v1

  # ======================= APPROVAL (8) =======================
  - policyId: P-401
    section: APPROVAL
    label: 할인쿠폰
    inputType: boolean
    required: false
    defaultValue: false
    sourceDocId: policy.discount_coupon.v1

  - policyId: P-402
    section: APPROVAL
    label: 할부결제 지원
    inputType: boolean
    required: true
    defaultValue: false
    helpText: 체크 시 '최대 할부개월' 입력란이 표시됩니다.
    sourceDocId: policy.installment.v1

  - policyId: P-403
    section: APPROVAL
    label: 분담무이자
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 가맹점 분담 무이자 할부 이벤트 사용 여부입니다.
    sourceDocId: policy.shared_interest_free.v1

  - policyId: P-404
    section: APPROVAL
    label: 최소 결제 금액
    inputType: number
    required: true
    defaultValue: 100
    unit: KRW
    validation:
      min: 100
      message: "PG 표준상 100원 이상이어야 합니다."
    helpText: 원천사 값이 100원 미만이면 100으로 자동 보정됩니다.
    sourceDocId: policy.min_amount.v1

  - policyId: P-405
    section: APPROVAL
    label: 복합결제
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 2개 이상 결제수단 동시 사용 지원 여부입니다.
    sourceDocId: policy.composite_payment.v1

  - policyId: P-406
    section: APPROVAL
    label: 영중소 지원 여부
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 영세·중소 가맹점 우대 수수료 지원 여부입니다.
    sourceDocId: policy.sme_support.v1

  - policyId: P-407
    section: APPROVAL
    label: 연동 제휴사
    inputType: multiselect
    required: true
    options: [PAYCO, KAKAOPAY, NAVERPAY, ETC]
    helpText: 본 지불수단과 연동되는 제휴사를 선택하세요.
    sourceDocId: policy.linked_provider.v1

  - policyId: P-408
    section: APPROVAL
    label: 타임아웃 (API별)
    inputType: group              # 반복 입력 그룹
    required: true
    fields:
      - key: PAYMENT_WINDOW
        label: 결제창 유효시간
        inputType: number
        unit: sec
        defaultValue: 1800
      - key: AUTH
        label: 인증 응답
        inputType: number
        unit: ms
        defaultValue: 30000
      - key: APPROVAL
        label: 승인 응답
        inputType: number
        unit: ms
        defaultValue: 30000
      - key: CANCEL
        label: 취소 응답
        inputType: number
        unit: ms
        defaultValue: 30000
      - key: NET_CANCEL
        label: 망취소 응답
        inputType: number
        unit: ms
        defaultValue: 30000
    helpText: 각 API별 응답 대기 한계 시간을 입력하세요.
    sourceDocId: policy.timeout.v1

  # ======================= CANCEL (6) =======================
  - policyId: P-501
    section: CANCEL
    label: 부분취소 지원
    inputType: boolean
    required: true
    defaultValue: true
    sourceDocId: policy.partial_cancel.v1

  - policyId: P-502
    section: CANCEL
    label: 복합과세 부분취소
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 복합과세 거래의 부분취소 지원 여부입니다.
    sourceDocId: policy.composite_tax_partial_cancel.v1

  - policyId: P-503
    section: CANCEL
    label: 취소가능 기한
    inputType: number
    required: true
    unit: days
    defaultValue: 365
    validation:
      min: 1
      max: 365
    sourceDocId: policy.cancel_deadline.v1

  - policyId: P-504
    section: CANCEL
    label: 제휴사 망취소 제공
    inputType: boolean
    required: true
    defaultValue: true
    helpText: 미제공(false) 시 연동 자체가 반려됩니다(BLOCKER).
    sourceDocId: policy.net_cancel_support.v1

  - policyId: P-505
    section: CANCEL
    label: 제휴사 기취소 제공 여부
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 이미 취소된 거래의 재취소(기취소) 제공 여부입니다.
    sourceDocId: policy.recancel_support.v1

  - policyId: P-506
    section: CANCEL
    label: 제휴사 거래대사
    inputType: select
    required: false
    options: [DAILY, WEEKLY, MONTHLY, NONE]
    defaultValue: DAILY
    helpText: 제휴사와의 거래대사(정산 대조) 주기입니다.
    sourceDocId: policy.reconciliation.v1

  # ======================= ETC (2) =======================
  - policyId: P-601
    section: ETC
    label: 장바구니
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 다건 상품 묶음 결제(장바구니) 지원 여부입니다.
    sourceDocId: policy.cart.v1

  - policyId: P-602
    section: ETC
    label: 정기결제 지원
    inputType: boolean
    required: false
    defaultValue: false
    helpText: 빌링키 기반 정기/자동 결제 지원 여부입니다.
    sourceDocId: policy.recurring_payment.v1
---

# 신규 지불수단 등록 — 작성 가이드 (본문)

이 문서의 front matter(`form_schema`)는 FE 위저드 S7 슬라이드가 동적으로 렌더링하는 폼 스키마입니다.
필드 추가/수정은 `fields:` 배열을 수정하고, 섹션 추가/수정은 `sections:` 배열을 수정합니다.
변경 후 별도 코드 수정 없이 백엔드 재기동만으로 반영됩니다.