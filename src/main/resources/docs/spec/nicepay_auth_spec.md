---
category: "규격서/PG"
document_type: "NICEPAY 인증 규격 사양서"
reference_url: "https://developers.nicepay.co.kr/"
version: "v2.0 종합 가이드"
last_updated: "2026-06-18"
tags: ["나이스페이", "API레퍼런스", "인증", "빌링", "가상계좌", "에러코드"]
---

# [PG 규격] NICEPAY 웹 인증 결제 API 표준 규격

본 문서는 사내 결제 인프라의 기본 PG 베이스가 되는 NICEPAY의 기능별 연동 명세서 목차와 핵심 요약입니다. 신규 간편결제나 수단 추가 시, 이 연동 포맷의 파라미터 규격 및 예외 처리 아키텍처를 충족해야 합니다.

## 3.1 기능별 연동 사양서 요약 (Guides v2)

- **[인증결제 이해하기](https://developers.nicepay.co.kr/manual-auth.php):** 인증결제는 고객이 결제창에서 신용카드 비밀번호나 생체인증을 거쳐 안전하게 결제하는 가장 기본적인 방식입니다. PC와 모바일 환경별 표준 호출 흐름과 인증 성공 후 백엔드 승인 프로세스를 설명합니다.
- **[키인결제 이해하기](https://developers.nicepay.co.kr/manual-auth.php):** 키인결제는 별도의 결제창 팝업 없이, 가맹점 자체 화면에서 카드번호와 유효기간을 직접 입력받아 결제를 요청하는 수동 승인 방식입니다. 주로 전화 주문이나 수동 정산 시스템을 구현할 때 사용됩니다.
- **[빌링결제 이해하기](https://developers.nicepay.co.kr/manual-card-billing.php):** 빌링결제는 정기 구독, 월간 렌탈, 자동 충전처럼 주기적인 대금 청구에 사용되는 방식입니다. 최초 1회 고객 카드를 인증하여 안전한 빌링키(Billing Key)를 발급받는 과정과 사후 자동 승인 요청 방법을 다룹니다.
- **[가상계좌발급 이해하기](https://developers.nicepay.co.kr/manual-virtual-account.php):** 가상계좌발급은 고객별로 고유한 일회성 입금 계좌를 실시간으로 생성해 주는 서비스입니다. 은행 지정, 입금 만료일 설정 스펙과 함께 무통장 입금을 선호하는 고객층을 위한 기획 시 참고할 수 있습니다.
- **[안드로이드/IOS 연동 이해하기](https://developers.nicepay.co.kr/manual-app.php):** 모바일 앱(App) 내부의 웹뷰(WebView) 환경에서 결제창을 띄울 때 필요한 전용 가이드입니다. 각 OS별 App-to-App 호출을 위한 앱 스키마(App Scheme) 설정 및 외부 인증 앱 이탈/복귀 시 예외 처리 방법을 설명합니다.
- **[결제조회 API 이해하기](https://developers.nicepay.co.kr/manual-app.php):** 결제조회는 망 순단이나 타임아웃으로 인해 결제 성공 여부가 모호할 때, 나이스페이 서버에 직접 거래 고유 ID(TxTid)나 주문번호(Moid)를 찔러서 실시간 결제 상태를 교차 검증하고 원인을 추적하는 안전장치 API입니다.
- **[결제통보 API 이해하기](https://developers.nicepay.co.kr/manual-noti.php):** 결제통보는 가상계좌 입금처럼 고객의 행위가 비동기적으로 완료되었을 때, 나이스페이 시스템이 가맹점 백엔드(Webhook)로 결제 완료 전문을 실시간으로 던져주는(Push) 데이터 통보 규격입니다.
- **[영수증 이해하기](https://developers.nicepay.co.kr/manual-noti.php):** 결제가 최종 완료된 후 고객 증빙 자료로 제공할 신용카드 매출전표 및 현금영수증의 조회 화면을 자사 주문서 UI나 알림톡에 자연스럽게 연동하고 발급 결과를 매핑하는 명세서입니다.
- **[카드사/은행코드](https://developers.nicepay.co.kr/manual-code-partner.php):** 결제 요청 및 승인 응답 전문에서 카드사와 은행을 식별하기 위해 사용하는 나이스페이 표준 2자리 기관 코드(ex: 신한 06, 국민 11) 매핑 테이블입니다. 화면에 금융기관명을 매핑할 때 필수적입니다.
- **[결과코드](https://developers.nicepay.co.kr/manual-code.php):** 결제 인증 및 승인 결과에 대해 나이스페이가 리턴하는 4자리 결과 코드 매뉴얼입니다. `0000`(성공)을 제외한 나머지 실패 코드들을 분석하여 고객 친화적인 에러 문구로 치환하는 기획의 기준이 됩니다.
- **[예외/보안처리](https://developers.nicepay.co.kr/manual-exception.php):** 거래 데이터 위변조 및 어뷰징을 원천 차단하기 위해 요청 시간, 금액, 상점 키를 조합하여 해시값(`SignData`)을 생성하는 암호화 알고리즘 규격과 강력한 SSL/TLS 통신 필수 준수 사양 가이드입니다.
- **[자주 물어보는 오류 FAQ](https://developers.nicepay.co.kr/tip.php):** 세션 만료, 상점 도메인 불일치, 해시 검증 실패 등 연동 개발 및 테스트 과정에서 개발팀과 현업이 가장 흔하게 겪는 비즈니스/기술적 에러 케이스와 신속한 조치 방법을 모아둔 트러블슈팅 리포트입니다.

## 3.2 결제창 호출 및 승인 필수 항목 (Request/Response)
- `MID` (10 byte, 필수): 가맹점 식별 아이디
- `Amt` (12 byte, 필수): 결제 금액 (숫자만 가능)
- `GoodsName` (40 byte, 필수): 결제 상품명 (EUC-KR 인코딩 주의)
- `Moid` (64 byte, 필수): 상품 주문번호 (고유값 필수, 특수문자 금지)
- `SignData` (500 byte, 필수): 위변조 검증 데이터 `hex(sha256(EdiDate + MID + Amt + MerchantKey))`
- `AuthResultCode`: 인증 결과 코드 ('0000'인 경우에만 가맹점 백엔드가 `NextAppURL`로 최종 승인 API를 호출할 수 있음)
- **망취소 필수 정책:** 승인 요청 중 타임아웃 발생 시, 거래대사 불일치 방지를 위해 가맹점 백엔드에서 `NetCancelURL` 호출 로직을 의무 구현해야 함.