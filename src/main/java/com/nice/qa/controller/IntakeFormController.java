package com.nice.qa.controller;

import com.nice.qa.service.intake.IntakeForm;
import com.nice.qa.service.intake.IntakeFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 동적 입력 폼 스키마 노출 컨트롤러.
 *
 * <p>FE 위저드의 특정 슬라이드(S7: 신규 지불수단 등록)는
 * 서버에서 폼 스키마를 받아 동적으로 입력 필드를 렌더링한다.
 * 이 컨트롤러는 해당 폼의 스키마 정의를 JSON으로 제공한다.
 *
 * <p>설계 의도: 폼 필드 구성이 바뀔 때 FE 코드 수정 없이
 * 서버 측 정책 파일(또는 DB 설정)만 변경하면 즉시 반영되도록 한다.
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class IntakeFormController {

    /**
     * 지불수단 등록 폼 스키마 생성 서비스 — 정책 파일 또는 설정을 기반으로
     * {@link IntakeForm} 스키마 객체를 조립한다.
     */
    private final IntakeFormService service;

    /**
     * 신규 지불수단 등록 폼 스키마 조회 (GET /api/forms/payment-method-intake).
     *
     * <p>FE 위저드 슬라이드 S7("신규 지불수단 등록")이 화면 진입 시 호출한다.
     * 반환되는 {@link IntakeForm}에는 폼 필드 목록, 각 필드의 타입·레이블·유효성 규칙 등이 포함된다.
     * FE는 이 스키마를 해석하여 입력 폼을 동적으로 렌더링한다.
     *
     * @return 신규 지불수단 등록에 필요한 동적 폼 스키마
     */
    @GetMapping("/payment-method-intake")
    public IntakeForm paymentMethodIntake() {
        return service.get();
    }
}
