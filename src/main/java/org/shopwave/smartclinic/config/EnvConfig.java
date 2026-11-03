package org.shopwave.smartclinic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Loads .env file as Spring properties for development/testing.
 * Ignores if .env doesn't exist (production uses system environment variables).
 */
@Configuration
@PropertySource(value = "classpath:.env", ignoreResourceNotFound = true)
public class EnvConfig {
    // Loading .env file as Spring properties
    // Values in .env override application.properties defaults
}