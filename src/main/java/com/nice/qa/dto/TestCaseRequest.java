package com.nice.qa.dto;

import jakarta.validation.constraints.NotBlank;

// 테스트케이스 생성 요청 DTO. 세 필드 모두 필수.
public record TestCaseRequest(
        @NotBlank(message = "paymentMethod는 필수입니다") String paymentMethod,
        @NotBlank(message = "provider는 필수입니다") String provider,
        @NotBlank(message = "requirements는 필수입니다") String requirements
) {
}
