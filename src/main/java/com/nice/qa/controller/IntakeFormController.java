package com.nice.qa.controller;

import com.nice.qa.service.intake.IntakeForm;
import com.nice.qa.service.intake.IntakeFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 동적 폼 스키마 노출 — FE 위저드의 신규 지불수단 등록 슬라이드(S7)가 사용.
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class IntakeFormController {

    private final IntakeFormService service;

    @GetMapping("/payment-method-intake")
    public IntakeForm paymentMethodIntake() {
        return service.get();
    }
}
