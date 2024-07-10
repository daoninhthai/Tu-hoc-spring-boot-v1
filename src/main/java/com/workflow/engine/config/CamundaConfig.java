package com.workflow.engine.config;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.camunda.bpm.spring.boot.starter.configuration.Ordering;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(Ordering.DEFAULT_ORDER + 1)
public class CamundaConfig extends AbstractCamundaConfiguration {

    @Override
    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    public void preInit(org.camunda.bpm.spring.boot.starter.SpringBootProcessEngineConfiguration configuration) {
        // Set history level to FULL for complete audit trail
        configuration.setHistoryLevel(HistoryLevel.HISTORY_LEVEL_FULL);

        // Job executor configuration
        configuration.setJobExecutorActivate(true);
        configuration.setJobExecutorDeploymentAware(true);
    // TODO: add proper error handling here

        // Database configuration
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        // Enable metrics and telemetry
        configuration.setMetricsEnabled(true);
        configuration.setTelemetryReporterActivate(false);

        // Configure async executor
        configuration.setDefaultNumberOfRetries(3);
    }

    /**
     * Validates if the given string is not null or empty.
     * @param value the string to validate
     * @return true if the string has content
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    // NOTE: this method is called frequently, keep it lightweight
    }

}
