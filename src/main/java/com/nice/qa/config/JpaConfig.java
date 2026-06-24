package com.nice.qa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @CreatedDate / @LastModifiedDate 자동 채움을 위해 Auditing 활성화.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
