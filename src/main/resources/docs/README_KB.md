# 지식저장소(KB) 데이터 구성 문서

> 개발요청서 자동 작성 도구(Req-Genie) — **데이터 정형화(KB) 영역** 산출물 정리
> 본 문서는 KB 담당 영역의 폴더·파일 구조와 각 산출물의 역할을 기술한다.
> 설계 근거: 개발요청서 자동 작성 도구 설계 문서 v0.3 (§2 담당 경계, §4 포트, §5 데이터 모델)
> 최종 갱신: 2026-06-22 (spec 4종 NICEPAY 공식 매뉴얼 기준 정정 반영)

---

## 0. 한눈에 보기

KB는 LLM을 **fine-tune 하지 않는다.** 요청 시점에 `KnowledgeClient` 포트로 **검색해 컨텍스트로 주입(RAG)** 한다.
따라서 KB 담당의 역할은 "검색했을 때 빠짐없이·정확하게 나오는 데이터"를 갖춰두는 것이다.

```
project
├─ catalog/    동적폼 분기 트리 (입력 분류 마스터)        → getCategoryTree()
├─ policy/     결제 검증 정책 + 마스터 인덱스             → search() / matchSpecDocs()
├─ provider/   간편결제 사업자 사양 (기존)               → search()
├─ requests/   요청서 양식·예시·과거이력·정책 트리거 맵   → form / precheck / findSimilarRequests()
└─ spec/       인증·승인·취소 규격 사양 (기존)            → matchSpecDocs()
```

### 어시스트 파이프라인 ↔ 데이터 매핑

| 단계 | 기능 | 사용 데이터 | 포트 |
|---|---|---|---|
| ① 분기 선택 | F2/F3 | `catalog/catalog_category_tree_v1.yaml` | `getCategoryTree()` |
| ② 양식 제공 | F4/F5 | `requests/request_form_template_v1.md` | (form) |
| ③ 초안 생성 | F6/F7 | `requests/request_examples_v1.md` (few-shot) | `generateRequestDoc()` |
| ④ 모순/누락 탐지 | F8 | `requests/request_policy_trigger_map_v1.yaml` + `policy/*` | `precheck()` |
| ⑤ 규격 매칭 | F10 | `policy/policy_index.yaml` + `spec/*` + `provider/*` | `matchSpecDocs()` |
| ⑥ 유사요청 추천 | F11 | `requests/request_history_v1.yaml` | `findSimilarRequests()` |

---

## 1. `catalog/` — 동적폼 분기 트리 (입력 분류 마스터)

설계 §4.4 / §10-2. 위저드의 입력 분류 체계를 보유한다. **운영 중 상시 수정 가능**(`mutable: true`).
`policy/`의 6대 정책 카테고리와는 **별개 체계**다(정책=검증 기준, 카탈로그=입력 분류).

| 파일 | 역할 | 핵심 내용 |
|---|---|---|
| `catalog_category_tree_v1.yaml` | 동적폼 3단계 분기 트리 | `func_types`(MODIFY/NEW) → `categories`(pg표준결제창·API·해외결제·기타서비스) → `sub_types`. `payment_method_vocabulary`로 세부유형↔provider 코드(CARD/BANK/POINT) 정규화 |

- `input_mode`: `SELECT`(세부유형 택1) / `FREE_TEXT`(기타서비스, 3단계 생략)
- `spec_match_hints`: 세부유형별 F10 후보 doc_id (느슨한 연결)
- 스키마 계약은 파일 내 `[SCHEMA]` 주석으로 명시(§10-2 "스키마는 KB가 정의")
- ⚠ **미결 항목**: `가상계좌(VBANK)`·`휴대폰결제(CELLPHONE)`를 `UNSUPPORTED`로 표기했으나, NICEPAY 표준결제창 공식 `PayMethod`에는 **정식 포함**(§5 참조). 표준결제창=지원 / 간편결제 3사=미지원으로 구분 정정 필요 (TODO)

---

## 2. `policy/` — 결제 검증 정책 + 마스터 인덱스

결제 도메인의 검증 규칙(`rule.condition`/`action`/`error_code`)을 보유. F8 모순탐지·F10 규격매칭의 근거.

| 파일 | 정책ID | 역할 |
|---|---|---|
| `policy_index.yaml` | — | **마스터 인덱스(Ground Truth)**. 정책 항목 / 카테고리·doc_id·status 관리 |
| `policy_installment_v1.md` | **P-402** | **할부결제** — 게이트웨이 한도(BLOCKER)/카드사 실한도(WARN) 2층 검증. 승인응답 `CardQuota` 근거 |
| `policy_min_amount_v1.md` | P-404 | 최소 결제금액 |
| `policy_cpid_v1.md` | — | CPID(가맹점 식별) 정책 |
| `policy_linked_payment_method_v1.md` | — | 연동 결제수단 정책 |
| `policy_net_cancel_support_v1.md` | — | 망취소 지원 정책 |
| `policy_partial_cancel_v1.md` | P-501 | 부분취소 정책. 승인응답 `CcPartCl` + 취소요청 `PartialCancelCode` 근거 |
| `policy_target_channel_v1.md` | P-304 | 대상 채널 정책 |
| `policy_timeout_v1.md` | P-408 | 타임아웃 정책 |
| `policy_template.md` | — | 정책 작성 템플릿 |

- **인덱스 동기화 완료**: P-402·P-501·P-304·P-408 = `ACTIVE`
- 남은 갭: 복합결제(**P-405**) 문서 미작성. 단, 공식 승인응답 `MultiCl`/`MultiCardAcquAmt`/`MultiPointAmt` 실필드 근거가 확보되어 작성 가능 상태

---

## 3. `provider/` — 간편결제 사업자 사양 (기존)

각 사업자의 지원 결제수단·할부·망취소·타임아웃 등 사양 데이터. 정책 검증의 실제 수치 출처.

| 파일 | 사업자 |
|---|---|
| `provider_kakaopay_v2.0.md` | 카카오페이 |
| `provider_naverpay_v1.5.md` | 네이버페이 |
| `provider_payco_v1.2.md` | 페이코 |

- 할부 데이터: CARD `[0,2,3,...,12]`(최대 12) / POINT·BANK `[0]`(일시불) → P-402 검증 근거

---

## 4. `requests/` — 요청서 양식·예시·이력·정책 트리거

KB 담당이 신규 작성한 요청서 영역(`ns: request`) 산출물 4종.

| 파일 | 역할 | 연결 기능 |
|---|---|---|
| `request_form_template_v1.md` | **표준 요청서 양식**. `{{INPUT}}`/`{{SYS}}`/`{{GEN}}` placeholder 출처 표기 | F4/F5 |
| `request_examples_v1.md` | **모범 작성 예시 2건**(few-shot). 모든 필드를 정량·구체 서술로 채움 | F6/F7 |
| `request_history_v1.yaml` | **과거 요청 더미 10건**. 카테고리·상태·키워드 분산 | F11 |
| `request_policy_trigger_map_v1.yaml` | **필드→정책 트리거 맵**. 입력 신호→정책→검사내용→경고문구 | F8 |

### 4.1 `request_form_template_v1.md`
- 5개 섹션(요청정보·배경·요청사항·기대효과·처리상태)
- `funcType/category/subType`은 `catalog.category_tree.v1` 노드와 연결

### 4.2 `request_examples_v1.md` (v1.1)
- EX-1: NEW/API/빌링 · EX-2: MODIFY/pg표준결제창/카드 (분류 대비)
- few-shot 기준: 추진배경(원인+영향+정량), 문제점([문제]/[개선] 구조), 재현경로(단계 서술), 발생빈도(수치만)
- **v1.1 정정**: EX-2 재현경로를 NICEPAY 공식 흐름 용어(`goPay`→`AuthToken`/`NextAppURL` 승인 POST→`NetCancelURL` 망취소)로 갱신
- `usage_hint`: 빈칸·정성표현만 있으면 F7 추가확인 항목으로 승격

### 4.3 `request_history_v1.yaml`
- 분포: 카테고리(API 4·해외 3·pg 2·기타 1) / 기능구분(MODIFY 6·NEW 4) / 상태 4종
- 각 건 `keywords` 배열로 유사도 검색 지원

### 4.4 `request_policy_trigger_map_v1.yaml`
- 활성 트리거 7건 + P-402(할부) 정식 트리거 = 8개 정책 연결
- 구성: `source_fields` / `signal`(분류+키워드) / `checks` / `warning_template` / `severity` / `error_code_refs`
- 남은 갭: 복합결제(P-405) 1건

---

## 5. `spec/` — 규격 사양 (NICEPAY 공식 매뉴얼 기준 정정 완료)

인증·승인·취소 규격. F10 규격 자동매칭의 대상 문서. **2026-06-22 NICEPAY 개발자 매뉴얼 기준으로 전면 정정**.

| 파일 | 규격 | 버전 | authoritative | 핵심 |
|---|---|---|---|---|
| `spec_auth_v2.md` | 인증 사양 | **2.1** | true | `goPay(formObject)` JS 호출 + euc-kr. 요청(MID/Moid/Amt/EdiDate/SignData/PayMethod), 응답(AuthResultCode/AuthToken/TxTid/NextAppURL/NetCancelURL) |
| `spec_approval_v2.md` | 승인 사양 | **2.2** | true | 인증응답 `NextAppURL`로 POST(form-urlencoded). 요청(TID/AuthToken/EdiDate/SignData), 응답 ResultCode(3001/4000/4100/A000/7001) + 카드/VBANK/BANK 필드 |
| `spec_signdata_v2.md` | SignData 사양 | **2.2** | true | 단계별 SignData/Signature **8종 공식 전면 확정**(§5-1) |
| `spec_netcancel_v1.md` | 망취소·승인취소 사양 | **1.1** | true | 망취소(NetCancelURL, `NetCancel=1`) + 승인취소(cancel_process.jsp, `PartialCancelCode`) 분리 |
| `spec_template.md` | 규격 작성 템플릿 | — | — | 신규 규격 작성 기준 |

### 5-1. SignData/Signature 단계별 공식 (spec_signdata_v2 §2, 전부 공식 확정)

모든 출력: 소문자 hex 64자, 구분자 없이 직접 연결. `MerchantKey`는 항상 맨 뒤.

| 단계 | 방향 | 입력 순서 |
|---|---|---|
| 인증 | 요청 SignData | `EdiDate + MID + Amt + MerchantKey` |
| 인증 | 응답 Signature | `AuthToken + MID + Amt + MerchantKey` |
| 승인 | 요청 SignData | `AuthToken + MID + Amt + EdiDate + MerchantKey` |
| 승인 | 응답 Signature | `TID + MID + Amt + MerchantKey` |
| 망취소 | 요청 SignData | `AuthToken + MID + Amt + EdiDate + MerchantKey` (승인요청과 동일) |
| 망취소 | 응답 Signature | `TID + MID + CancelAmt + MerchantKey` |
| 승인취소 | 요청 SignData | `MID + CancelAmt + EdiDate + MerchantKey` (**AuthToken 없음**) |
| 승인취소 | 응답 Signature | `TID + MID + CancelAmt + MerchantKey` |

> 주의: 취소·망취소 응답은 `Amt`가 아니라 **`CancelAmt`** 사용. 승인취소 요청만 `AuthToken`이 없음.

### 5-2. 결제 흐름 (TWO_STEP)
```
goPay(formObject) ─인증─▶ AuthResultCode=0000 / AuthToken / NextAppURL / NetCancelURL
                          │
                NextAppURL로 승인 POST ──▶ ResultCode(3001 등) / Signature
                          │ (응답 누락·지연)
                NetCancelURL로 망취소 POST(NetCancel=1) ──▶ 거래 무효화
```

---

## 6. 명명·버전 규약

- 파일명: `{ns}_{slug}_v{ver}.{ext}` (예: `policy_installment_v1.md`, `catalog_category_tree_v1.yaml`)
- `doc_id`: `{ns}.{slug}.v{ver}` 형식 (예: `policy.installment.v1`)
- `ns` ↔ 폴더 일치: `catalog`/`policy`/`provider`/`request`/`spec`
- 버전 갱신: `_v2`로 나란히 두거나 교체 (provider `_v2.0` / spec `_v2` 컨벤션 참고)
- 비밀값: `${ENV_VAR}` 또는 더미값(`nicepay00m`)만 사용, 실데이터 미포함

---

## 7. 정합성 현황 (검증 완료)

- spec 4종 SignData 공식이 `spec_signdata_v2` 진실표 8종과 **전부 일치**
- 가상 필드(`paymentId`/`/payments/approval` 등) 활성 규격 잔재 **0건** (변경이력 기록만 존재)
- 트리거 맵이 참조하는 **8개 정책 전건이 인덱스에서 ACTIVE** — 끊긴 참조 0건
- P-402 트리거 에러코드 3종이 정책 MD에 실재
- 공식 승인응답 필드로 정책 검증근거 연결: P-402(CardQuota)·P-501(CcPartCl/PartialCancelCode)·P-405(MultiCl 등)
- CategoryTree 세부유형 ↔ provider 결제수단 코드 정합

### 남은 오픈 이슈
- 복합결제(**P-405**) 정책 문서 미작성 (단, MultiCl 등 실필드 근거 확보됨)
- CategoryTree **VBANK/CELLPHONE** 지원 표기 정정 필요 (공식 PayMethod 정식 포함 — §1·§5)
- 해외결제(GLOBAL) 결제수단 통제어휘 미정
- provider SignData 입력순서(PAYCO/NAVER) 공식 재검증 (`verified:false`)

---

## 8. 변경 이력 (KB 문서)

| 일자 | 변경내용 |
|---|---|
| 2026-06-22 | 최초 작성 (산출물 5종) |
| 2026-06-22 | spec 4종 NICEPAY 공식 매뉴얼 기준 정정 반영 — auth v2.1 / approval v2.2 / signdata v2.2 / netcancel v1.1, SignData 8종 공식표(§5-1) 추가, examples v1.1, 정책 검증근거 연결, VBANK/CELLPHONE 미결 명시 |
