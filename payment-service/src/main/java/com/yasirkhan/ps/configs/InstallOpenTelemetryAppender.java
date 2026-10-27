package com.yasirkhan.ps.configs;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Connects the Logback "OTEL" appender (declared in logback-spring.xml) to the OpenTelemetry SDK
 * that Spring Boot created. Logback starts BEFORE Spring, so the appender has no SDK at first;
 * this bean hands it over once Spring is ready. Without it, no logs are exported.
 * Log lines written during the first seconds of startup (before this runs) stay console-only.
 */
@Component
public class InstallOpenTelemetryAppender implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    public InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
