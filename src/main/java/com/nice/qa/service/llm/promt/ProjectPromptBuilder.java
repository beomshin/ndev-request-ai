package com.nice.qa.service.llm.promt;

import com.nice.qa.model.api.dto.DevRequestRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link com.nice.qa.service.llm.dto.ProjectMdResult} 추출·추론용 LLM 프롬프트 조립기.
 *
 * <p>목표: 현업(비IT)이 작성한 추상적·불완전한 개발 요청을 AI가 한 번에 구조화해
 * IT 개발팀이 추가 문의 없이 착수할 수 있는 수준의 요청서로 변환한다.
 * 전형적으로 3~5회 발생하는 현업↔IT 핑퐁을 0~1회로 줄이는 것이 핵심 목표다.
 *
 * <p>프롬프트 구성 섹션:
 * <ol>
 *   <li>{@link #CONTEXT} — AI 역할 및 목표 정의 (PG 도메인 전문 번역가)</li>
 *   <li>{@link #PING_PONG_PATTERNS} — 핑퐁이 발생하는 전형적인 패턴 목록</li>
 *   <li>{@link #PROCEDURE} — 8단계 분석 절차 (의도 파악 → R&amp;R 판정 → 필드 채우기)</li>
 *   <li>참고 링크 목록 — LLM이 직접 열람해야 할 지식베이스 문서 URL</li>
 *   <li>개발요청서 입력 — 현업이 작성한 원문 필드</li>
 *   <li>{@link #ISSUE_RULE} — {@code issueAndImprovement} 필드 4단 작성 규칙</li>
 *   <li>{@link #COMMON_RULES} — 추론·출력 공통 규칙 (JSON만 출력, 코드펜스 금지 등)</li>
 *   <li>{@link #OUTPUT_SCHEMA} — 출력 JSON 스키마 정의</li>
 * </ol>
 *
 * <p>LLM 응답은 {@link com.nice.qa.service.llm.dto.ProjectMdResult} 구조에 맞는
 * 순수 JSON이어야 한다. 코드펜스가 붙은 경우
 * {@link com.nice.qa.service.llm.LlmResponseParser#stripCodeFence(String)}로 제거한 뒤
 * Jackson으로 역직렬화한다.
 *
 * @see com.nice.qa.service.llm.dto.ProjectMdResult  LLM 응답 역직렬화 대상 DTO
 * @see com.nice.qa.service.llm.ReferenceLinks        프롬프트에 주입되는 참조 문서 링크 목록
 * @see com.nice.qa.service.llm.md.StandardMarkdownRenderer  결과 DTO를 마크다운으로 렌더링
 */
@Component
public class ProjectPromptBuilder {

    // ── 맥락 ─────────────────────────────────────────────────────────────────
    /**
     * AI의 역할과 목표를 정의하는 맥락(context) 섹션.
     *
     * <p>LLM에게 "PG 도메인 전문 번역가 겸 요구사항 분석가" 역할을 부여한다.
     * 이 섹션은 LLM이 현업 언어와 기술 명세 사이를 정확히 번역하도록 유도하는
     * System Prompt 역할을 한다.
     */
    private static final String CONTEXT = """
            # 역할과 목표

            ## 상황
            - 요청자(현업)는 IT 지식이 부족한 비개발 담당자다. PG 기술 용어·시스템 구조를 잘 모른다.
            - 현업이 작성한 요청서는 비즈니스 언어로 쓰여 있어 IT 개발팀이 그대로는 이해·착수하기 어렵다.
            - 통상 현업↔IT 사이에 3~5회 이상의 핑퐁(재문의→답변→재문의)이 발생한 후에야 요청이 확정된다.

            ## 네 역할
            너는 이 핑퐁을 0~1회로 줄이기 위한 **PG 도메인 전문 번역가 겸 요구사항 분석가**다.
            - 현업의 비즈니스 언어를 PG 기술 명세로 번역·구조화한다.
            - 추론 가능한 부분은 참고 문서와 PG 지식을 근거로 직접 채운다.
            - 추론할 수 없는 부분(확정 정보 부재)은 IT가 현업에게 한 번에 물어볼 수 있도록 질문 목록으로 뽑는다.
            - 모든 추론에는 근거와 가정을 명시해 IT 담당자가 검토·수정할 수 있게 한다.
            """;

    // ── 핑퐁 패턴 인식 ────────────────────────────────────────────────────────
    /**
     * 현업↔IT 핑퐁이 발생하는 전형적인 패턴을 LLM에 인식시키는 섹션.
     *
     * <p>가맹점 정보 누락, 서비스 채널 불명확, 원천사 계약 미확인, 정책 미지정 등
     * 재문의를 유발하는 체크리스트를 제공한다. LLM은 이 패턴을 참고하여
     * {@code pendingQuestions}를 생성한다.
     */
    private static final String PING_PONG_PATTERNS = """
            # 핑퐁이 발생하는 전형적인 패턴 (반드시 인식하고 예방할 것)

            현업 요청서에 아래 정보가 빠지거나 모호하면 IT가 반드시 재문의한다.
            각 항목을 체크하고, 빠진 항목은 pendingQuestions에 담아라.

            [가맹점 관련]
            - MID(가맹점 ID)가 명시되지 않음 → "적용 대상 MID 또는 가맹점명을 알려주세요."
            - 가맹점이 다수일 때 전체 적용인지 일부 적용인지 불명확

            [서비스 채널/환경]
            - PC·Mobile·App 중 어디에 적용하는지 불명확
            - 결제 방식(단건·빌링·링크결제·복합결제 등) 미명시

            [원천사·카드사]
            - 특정 원천사 연동이 필요한데 협의/계약 여부 불명확
            - 원천사에서 별도 규격을 제공하는지 여부 미확인

            [일정·우선순위]
            - 목표일이 너무 촉박하거나 불명확
            - 반드시 이 일정에 완료되어야 하는 외부 의존 이유 미명시 (예: 가맹점 오픈일, 캠페인일)

            [정책·예외처리]
            - 부분취소·환불 정책 미지정
            - 최소/최대 결제 금액 미지정
            - 오류 처리 방침(실패 시 현업이 원하는 동작) 미지정

            [검증 방법]
            - 개발이 완료됐을 때 현업이 어떻게 확인할지 불명확
            - 테스트 환경(sandbox/staging) 사용 가능 여부 미언급
            """;

    // ── 분석 절차 ─────────────────────────────────────────────────────────────
    /**
     * LLM이 반드시 따라야 할 8단계 분석 절차.
     *
     * <p>Step 1(의도 파악) → Step 2(R&amp;R 판정) → Step 3(정보 분류) →
     * Step 4(요청 구체화) → Step 5(부수 업무 체크) → Step 6(선행 조건 도출) →
     * Step 7(핑퐁 방지 질문 생성) → Step 8(IT 명세화) 순서로 수행하도록 지시한다.
     * Chain-of-Thought 방식으로 단계적 추론을 유도하여 출력 품질을 높인다.
     */
    private static final String PROCEDURE = """
            # 분석 절차 (반드시 이 순서대로 수행)

            ## Step 1 — 의도 파악
            현업이 진짜로 해결하고 싶은 비즈니스 문제와 목표를 파악한다.
            표면 요청("A 기능 추가해주세요")에 숨겨진 실제 의도("B라는 상황에서 C가 안 돼서 매출 손실")를 찾는다.

            ## Step 2 — R&R 판정
            이 요청이 PG개발실에서 처리할 일인지 판단한다.
            결제창/인증·승인·취소·환불/정산/웹훅 등 PG 연동 영역 → developmentInProgress=true
            그 외(단순 마케팅 페이지, 타 시스템 영역 등) → false, transferGuide에 이관 부서·행동을 구체적으로 기술

            ## Step 3 — 확보 정보 vs 미확보 정보 분류
            입력된 필드를 분석해 두 묶음으로 나눈다.
            - **확정 정보**: 현업이 명시적으로 제공한 값 → 그대로 반영
            - **추론 정보**: 참고 문서·PG 지식으로 합리적으로 채울 수 있는 값 → 채우되 assumptionList에 근거 기록
            - **미확보 정보**: 추론도 불가능하고 현업에게 확인해야 하는 값 → pendingQuestions에 질문으로 변환

            ## Step 4 — 요청사항 구체화
            현업 입력과 참고 문서를 근거로, 명시되지 않았지만 이 목표 달성에 실제로 필요한
            기능·정책·연동 항목을 추론해 developmentScope와 issueAndImprovement에 구체화한다.
            - "결제 추가"라고만 쓰여 있으면 → 인증·승인·취소·환불·웹훅·정산 중 어디까지인지 추론
            - 추론 근거는 항상 참고 문서 문서명·항목명을 인용한다

            ## Step 5 — 부수 업무 체크
            요청사항 구현 시 함께 처리해야 할 부수 업무와 4개 Boolean을 판정한다.
            - merchantRelatedDevelopment: 특정 MID·상호명과 직접 얽힌 개발인가
            - providerRelatedDevelopment: 원천사(카드사/PG사) 규격·연동 변경이 필요한가
            - newServiceOrSelfImprovement: 신규 서비스이거나 자체 개선 성격인가 (funcType 참고)
            - securityAndAuditDevelopment: 보안/감사/컴플라이언스 대응 성격인가

            ## Step 6 — 선행 조건 도출
            개발 착수 전에 반드시 해결되어야 할 사항을 prerequisiteActions에 기술한다.
            예: 원천사 연동 계약 체결, MID 발급 신청, 보안 심의 통과, 타팀 API 제공 일정 확인

            ## Step 7 — 핑퐁 방지 질문 목록 생성
            위 [핑퐁 패턴] 섹션의 체크리스트와 Step 3 미확보 정보를 기반으로
            pendingQuestions를 생성한다. 질문은 현업이 바로 답할 수 있도록 구체적으로 작성한다.
            예: "적용 대상 MID를 알려주세요 (예: nicepay_test_mid_001)"
            예: "PC/Mobile/앱 중 어느 채널에 먼저 적용할지 알려주세요"

            ## Step 8 — IT 명세화 및 필드 채우기
            현업의 비즈니스 표현을 참고 문서의 PG/기술 용어로 매핑한다.
            (결제창 파라미터, 승인/취소/환불 API, 할부 옵션 필드, 웹훅 노티 등)
            모든 필드를 아래 스키마에 정확히 채운다.
            """;

    // ── issueAndImprovement 작성 규칙 ────────────────────────────────────────
    /**
     * {@code issueAndImprovement} 필드 작성을 위한 4단 구조 규칙.
     *
     * <p>현업이 작성한 {@code problemAndImprovement}를 그대로 복사하지 않고,
     * (1) 핵심 의도 요약 → (2) IT 기술 해석 → (3) 기술 체크포인트 → (4) 근거 표기
     * 4단으로 재해석하도록 지시한다. 이 필드가 개발자 착수의 핵심 근거가 된다.
     */
    private static final String ISSUE_RULE = """
            # issueAndImprovement 작성 규칙

            현업이 쓴 problemAndImprovement를 그대로 복사하지 말고, 아래 4단을 순서대로 담는다.

            (1) 현업 요청의 핵심 의도 요약
                비즈니스 관점에서 무엇을 원하는지 한두 문장으로. "무엇이 문제이고 무엇을 원한다"의 구조.

            (2) IT 기술 해석
                참고 문서·PG 지식 기반으로 어떤 기능·파라미터·API로 구현되는지 구체화.
                가능하면 실제 필드·전문 항목명을 인용한다.
                예: "NICEPAY 인증 API의 CardInstallMonth 파라미터를 통해 할부 개월 수를 전달"

            (3) 개발자가 착수하기 위해 필요한 기술 체크포인트
                - 신규 API 추가가 필요한지, 기존 로직 수정인지
                - 원천사·카드사 규격서 참조 여부
                - DB 스키마 변경 여부
                - 웹훅·노티 처리 변경 여부

            (4) 근거 표기
                판단의 출처가 된 문서·항목을 명시한다.
                예: "spec_auth_v2.md § 3.2 CardInstallMonth"

            추론이 불확실한 부분은 문장 안에 "(가정)" 표기. 의도를 왜곡하거나 없는 요구사항을 만들지 않는다.
            """;

    // ── 공통 규칙 ─────────────────────────────────────────────────────────────
    /**
     * 추론·출력 전반에 적용되는 공통 규칙 8개.
     *
     * <p>주요 규칙:
     * <ul>
     *   <li>입력 값은 그대로 반영, {@code problemAndImprovement}는 재해석</li>
     *   <li>추론 시 참고 문서 → PG 도메인 지식 → 웹 검색 우선순위 적용</li>
     *   <li>근거가 없으면 빈 문자열/null — 절대 지어내지 않음</li>
     *   <li>응답은 JSON 하나만, 코드펜스·설명·머리말 금지</li>
     * </ul>
     */
    private static final String COMMON_RULES = """
            # 공통 규칙

            1. 입력에 명시된 값은 그대로 반영한다.
               단, problemAndImprovement는 위 issueAndImprovement 규칙에 따라 재해석한다.

            2. 입력에 없는 값은 참고 문서 → PG 도메인 지식 → 웹 검색 순으로 합리적으로 추론한다.
               추론한 모든 항목은 assumptionList에 "항목명: 근거" 형식으로 기록한다.

            3. 추론 근거가 약하거나 알 수 없으면 빈 문자열 또는 null을 쓴다. 절대 지어내지 않는다.

            4. Boolean은 true/false로만, 판단 불가 시 null.

            5. pendingQuestions는 **IT가 현업에게 한 번의 답변으로 모두 해결할 수 있도록**
               꼭 필요한 질문만 넣는다. 추론 가능한 것은 질문하지 말고 직접 채운다.
               질문은 "~를 알려주세요 (예: ...)" 형식으로 구체적으로 작성한다.

            6. estimatedComplexity 판정 기준:
               LOW: 기존 파라미터 값 추가·설정 변경·단순 화면 수정 수준
               MID: 신규 API 엔드포인트 추가·기존 결제 로직 수정·원천사 기존 규격 내 변경
               HIGH: 원천사 신규 계약·협의 필요, 복수 시스템 동시 영향, 보안 심의 수반, 아키텍처 변경

            7. 스키마의 키만 출력한다. 키 추가·삭제·이름변경 금지.

            8. 응답은 오직 JSON 하나만 출력한다. 코드펜스(백틱 3개)·설명·머리말 금지.
            """;

    // ── 출력 스키마 ───────────────────────────────────────────────────────────
    /**
     * LLM이 출력해야 할 JSON 스키마 정의.
     *
     * <p>{@link com.nice.qa.service.llm.dto.ProjectMdResult}의 모든 필드에 대응하는
     * JSON 키·타입·설명을 포함한다. LLM은 이 스키마의 키를 정확히 따라야 하며
     * 키 추가·삭제·이름 변경은 금지된다.
     *
     * <p>이 스키마가 Few-shot 출력 포맷 가이드 역할을 하여 Jackson 역직렬화 성공률을 높인다.
     */
    private static final String OUTPUT_SCHEMA = """
            # 출력 형식 (JSON 스키마 — 키와 타입을 정확히 따른다)
            {
              "author": "작성자(문자열)",
              "createdDate": "작성일 YYYY-MM-DD(문자열)",
              "department": "작성 부서(문자열)",
              "projectId": "프로젝트 ID(문자열, 없으면 null)",

              "mid": "MID 가맹점 ID — 가맹점 관련 개발일 때만(문자열)",
              "merchantName": "가맹점 상호명 — 가맹점 관련 개발일 때만(문자열)",
              "merchantBusinessNumber": "가맹점 사업자번호 — 가맹점 관련 개발일 때만(문자열)",

              "providerName": "원천사 상호명 — 원천사 관련 개발일 때만(문자열)",
              "providerCollaborationBackground": "원천사 협업 배경(문자열)",

              "promotionBackground": "추진 배경 — 어떤 현상이 있고 무엇을 해결/목적하는지(문자열)",
              "additionalInfo": "원천사 규격서 매핑 데이터 및 테크니컬 체크포인트 요약(문자열)",
              "targetDate": "목표 일정 YYYY-MM-DD(문자열)",
              "productName": "제품명(문자열)",

              "issueAndImprovement": "(1)의도요약 (2)IT기술해석 (3)기술체크포인트 (4)문서근거 4단으로 작성(문자열)",
              "issueVerificationMethod": "개발자가 직접 재현·확인할 수 있는 경로 및 테스트 조건(문자열)",
              "serviceChannelAndPaymentMethod": "서비스 채널/결제 방식. 예: 온라인 Web&App / 단건결제 및 빌링결제(문자열)",
              "authApprovalAcquirerSubject": "인증/승인/매입 주체. 예: 인증-원천사, 승인-자사, 매입-카드사 직매입(문자열)",

              "minimumPaymentAmount": "최소 결제 금액 하한선. 단위 원, 숫자만. 예: 1000(문자열)",
              "partialCancelAndRefundPolicy": "부분취소/환불 정책. 예: 부분취소 가능 여부, 복합결제 시 취소 우선순위(문자열)",
              "cashReceiptIssuer": "현금영수증 발행 주체. 예: PG 대행 / 제휴사 직접 발행 / 해당 없음(문자열)",

              "expectedRevenue": "예상 수익(문자열)",
              "expectedLoss": "예상 손해(문자열)",
              "expectedEffect": "개발 완료 후 기대하는 정성적/정량적 효과(문자열)",

              "transferGuide": "developmentInProgress=false일 때만 — 이관할 부서 및 구체적 행동 가이드(문자열)",
              "developmentInProgress": "PG개발실 R&R 해당 여부(true/false/null)",
              "merchantRelatedDevelopment": "가맹점 관련 개발 여부(true/false/null)",
              "providerRelatedDevelopment": "원천사 관련 개발 여부(true/false/null)",
              "newServiceOrSelfImprovement": "신규 서비스/자체 개선 여부(true/false/null)",
              "securityAndAuditDevelopment": "보안 및 감사 대응 여부(true/false/null)",

              "pendingQuestions": [
                "현업에게 재확인이 필요한 질문1 (예: 형식 포함)",
                "현업에게 재확인이 필요한 질문2"
              ],
              "assumptionList": [
                "serviceChannelAndPaymentMethod: 입력에 채널 미명시 → PC/Mobile 모두 적용으로 가정 (근거: 동종 요청 사례 pattern)",
                "추론 항목명: 가정 내용 (근거: 문서명 또는 PG 도메인 관행)"
              ],
              "prerequisiteActions": "개발 착수 전 필요한 선행 조건. 예: 카카오페이 연동 계약 체결 확인, MID 발급 후 테스트 환경 등록(문자열)",
              "developmentScope": "IT 관점 개발 범위. 신규/수정 대상 모듈·API·화면과 기존 서비스 영향 범위(문자열)",
              "estimatedComplexity": "LOW | MID | HIGH",
              "riskAndConstraints": "일정 리스크, 원천사 대응 지연, 정책 충돌, 기술 제약 등(문자열)"
            }
            """;

    /**
     * 현업 개발 요청과 참조 문서 링크를 조합하여 완성된 LLM 프롬프트를 반환한다.
     *
     * <p>각 섹션은 {@code \n\n}으로 구분되어 LLM이 섹션 경계를 명확히 인식하도록 한다.
     * 조립 순서: 역할 맥락 → 핑퐁 패턴 → 분석 절차 → 참고 링크 → 입력 데이터
     * → issueAndImprovement 규칙 → 공통 규칙 → 출력 스키마.
     *
     * @param req   현업이 입력한 개발 요청 원문 DTO
     *              ({@link com.nice.qa.model.api.dto.DevRequestRequest})
     * @param links {@link com.nice.qa.service.llm.ReferenceLinks#ALL} 등 참조 문서 URL 목록.
     *              {@code null} 또는 빈 리스트 허용.
     * @return Gemini LLM에 전달할 완성된 프롬프트 문자열
     */
    public String build(DevRequestRequest req, List<String> links) {
        return String.join("\n\n",
                CONTEXT.strip(),
                PING_PONG_PATTERNS.strip(),
                PROCEDURE.strip(),
                referenceLinks(links),
                requestInput(req),
                ISSUE_RULE.strip(),
                COMMON_RULES.strip(),
                OUTPUT_SCHEMA.strip());
    }

    /**
     * 참고 링크 섹션을 생성한다.
     *
     * <p>LLM에게 "반드시 직접 열람하여 추론에 활용하라"고 지시하는 URL 목록을 불릿 형식으로 조립한다.
     * 링크가 없는 경우 "(참고 링크 없음)"으로 명시하여 LLM이 빈 섹션을 인식하도록 한다.
     *
     * @param links 참조 문서 URL 목록. {@code null} 또는 빈 리스트 허용.
     * @return 참고 링크 섹션 문자열
     */
    private String referenceLinks(List<String> links) {
        // 링크가 없는 경우에도 섹션 헤더는 유지하고 "없음"으로 명시
        String body = (links == null || links.isEmpty())
                ? "- (참고 링크 없음)"
                : links.stream().map(link -> "- " + link).collect(Collectors.joining("\n"));
        return "# 참고 링크 (반드시 직접 열람하여 내용을 추론에 활용할 것)\n" + body;
    }

    /**
     * 현업 개발 요청 원문을 프롬프트 섹션 형식으로 포맷팅한다.
     *
     * <p>각 필드는 LLM이 명확히 구분할 수 있도록 {@code - 필드명(영문키): 값} 형식으로 출력한다.
     * null 값은 {@link #nz(String)}로 빈 문자열로 처리하여 LLM이 null 문자열을 보지 않도록 한다.
     *
     * @param req 현업 입력 DTO
     * @return 포맷팅된 개발요청서 입력 섹션 문자열
     */
    private String requestInput(DevRequestRequest req) {
        // 현업이 입력한 9개 필드를 LLM이 구조화할 원문으로 포맷팅
        // null 값은 nz()로 빈 문자열 처리 — LLM이 "null" 문자열을 출력하지 않도록 방어
        return """
                # 개발요청서 입력 (현업 작성 원문 — 아래 내용만으로 개발 착수 가능한 수준으로 구조화하라)
                - 기능구분(funcType): %s
                - 카테고리(category): %s
                - 세부유형(subType): %s
                - 작성자(author): %s
                - 부서(department): %s
                - 서비스명(serviceName): %s
                - 추진배경(background): %s
                - 목표일정(targetSchedule): %s
                - 추가사항(problemAndImprovement): %s"""
                .formatted(
                        nz(req.funcType()), nz(req.category()), nz(req.subType()),
                        nz(req.author()), nz(req.department()), nz(req.serviceName()),
                        nz(req.background()), nz(req.targetSchedule()), nz(req.problemAndImprovement()));
    }

    /**
     * null-safe 문자열 변환 헬퍼 (null → 빈 문자열).
     *
     * <p>프롬프트에 "null" 문자열이 그대로 포함되면 LLM이 오동작하거나
     * 출력 JSON에 "null" 문자열을 값으로 채울 수 있으므로 방어적으로 변환한다.
     *
     * @param value 원본 문자열 ({@code null} 허용)
     * @return {@code null}이면 빈 문자열, 그 외에는 원본 값
     */
    private static String nz(String value) {
        return Objects.toString(value, "");
    }
}
