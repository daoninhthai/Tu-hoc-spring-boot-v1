package com.workflow.engine.config;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.Ordering;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(Ordering.DEFAULT_ORDER + 1)
public class CamundaConfig extends AbstractCamundaConfiguration {

    @Override
    public void preInit(SpringProcessEngineConfiguration configuration) {
        // Set history level to FULL for complete audit trail
        configuration.setHistoryLevel(HistoryLevel.HISTORY_LEVEL_FULL);

        // Job executor configuration
        configuration.setJobExecutorActivate(true);
        configuration.setJobExecutorDeploymentAware(true);

        // Database configuration
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        // Enable metrics and telemetry
        configuration.setMetricsEnabled(true);
        configuration.setTelemetryReporterActivate(false);

        // Configure async executor
        configuration.setDefaultNumberOfRetries(3);
    }

}
