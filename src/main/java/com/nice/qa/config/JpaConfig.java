package com.nice.qa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 관련 Spring 설정 클래스 (Spring configuration class for JPA setup).
 *
 * <p>{@link EnableJpaAuditing}을 활성화하여 {@link org.springframework.data.annotation.CreatedDate}와
 * {@link org.springframework.data.annotation.LastModifiedDate} 어노테이션이
 * 엔티티 저장·수정 시 자동으로 현재 시각을 채워 넣도록 한다.
 *
 * <p>Auditing이 활성화되어 있어야 {@link com.nice.qa.entity.DevRequest}의
 * {@code createdAt} / {@code updatedAt} 필드가 자동 관리된다.
 * 이 설정이 없으면 두 필드가 {@code null}로 남는다.
 *
 * <p>현재는 Auditing 활성화만 담당하며, 추후 필요 시 EntityManagerFactory 커스터마이징,
 * 트랜잭션 매니저 설정, QueryDSL 설정 등을 이 클래스에 추가할 수 있다.
 */
@Configuration
@EnableJpaAuditing
// @CreatedDate / @LastModifiedDate 자동 채움을 위해 JPA Auditing 활성화
// (enables JPA Auditing so @CreatedDate and @LastModifiedDate are populated automatically)
public class JpaConfig {
}
