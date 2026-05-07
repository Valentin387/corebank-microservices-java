package com.corebank.core.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j configuration for core-service.
 * Circuit breaker and retry settings are defined in application.yaml.
 * This class serves as an extension point for programmatic configuration if needed.
 */
@Configuration
public class ResilienceConfig {
    // Configuration is driven by application.yaml
    // Add programmatic customizers here if needed in the future
}
