package com.clinic.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for the Clinic Administration System.
 *
 * Multi-module configuration:
 * - @EntityScan: Scans com.clinic.common.entity for JPA entities
 * - @ComponentScan: Scans com.clinic.backend and com.clinic.common for Spring components
 *
 * Features enabled:
 * - JPA Auditing: Automatic @CreatedDate, @LastModifiedDate handling
 * - Caching: Redis distributed caching with tenant-aware keys
 * - Async: Asynchronous method execution
 * - Scheduling: Scheduled tasks (materialized view refresh, cleanup)
 */
@SpringBootApplication
@EntityScan(basePackages = {"com.clinic.common.entity"})
@ComponentScan(basePackages = {"com.clinic.backend", "com.clinic.common"})
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class ClinicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicApplication.class, args);
    }
}
