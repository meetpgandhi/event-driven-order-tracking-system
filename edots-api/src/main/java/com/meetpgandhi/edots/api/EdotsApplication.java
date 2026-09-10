package com.meetpgandhi.edots.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.meetpgandhi.edots")
@EnableJpaRepositories(basePackages = "com.meetpgandhi.edots.domain.repository")
@EntityScan(basePackages = "com.meetpgandhi.edots.domain.entity")
@EnableScheduling
@OpenAPIDefinition(
    info = @Info(
        title = "EDOTS - Event-Driven Order Tracking System API",
        version = "1.0.0",
        description = "Production-grade Order Fulfilment Tracking Platform for EDOTS featuring 8-stage state machine, Kafka event spine, and role-based access control.",
        contact = @Contact(name = "EDOTS Engineering", email = "engineering@edots.dev")
    )
)
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter JWT Bearer token obtained from /api/auth/login"
)
public class EdotsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdotsApplication.class, args);
    }
}
