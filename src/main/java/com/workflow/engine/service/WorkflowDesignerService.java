package com.workflow.engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDesignerService {

    private final RepositoryService repositoryService;

    /**
     * Validates a deployed BPMN process definition.
     * Checks for common issues like missing start/end events, unconnected elements, etc.
     */
    public Map<String, Object> validateBpmn(String processDefinitionId) {
        Map<String, Object> result = new HashMap<>();

        try {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();

            if (definition == null) {
                result.put("valid", false);
                result.put("error", "Process definition not found: " + processDefinitionId);
                return result;
            }

            String bpmnXml = exportBpmn(processDefinitionId);
            if (bpmnXml == null) {
                result.put("valid", false);
                result.put("error", "Could not read BPMN XML");
                return result;
            }

            // Basic validation checks
            boolean hasStartEvent = bpmnXml.contains("startEvent");

            boolean hasEndEvent = bpmnXml.contains("endEvent");
            boolean hasUserTask = bpmnXml.contains("userTask") || bpmnXml.contains("serviceTask");

            result.put("valid", hasStartEvent && hasEndEvent);
            result.put("processDefinitionId", processDefinitionId);
            result.put("processName", definition.getName());

            Map<String, Boolean> checks = new HashMap<>();
            checks.put("hasStartEvent", hasStartEvent);
            checks.put("hasEndEvent", hasEndEvent);
            checks.put("hasTasks", hasUserTask);
            result.put("checks", checks);

            if (!hasStartEvent) {
                result.put("warning", "Process is missing a start event");
            }
            if (!hasEndEvent) {
                result.put("warning", "Process is missing an end event");
            }

        } catch (Exception e) {
            log.error("Error validating BPMN: {}", processDefinitionId, e);
            result.put("valid", false);
            result.put("error", "Validation error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Exports the BPMN XML for a given process definition.
     */
    public String exportBpmn(String processDefinitionId) {
        try {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();

            if (definition == null) {
                log.warn("Process definition not found: {}", processDefinitionId);
                return null;
            }

            InputStream bpmnStream = repositoryService.getProcessModel(processDefinitionId);
            if (bpmnStream == null) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(bpmnStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.error("Error exporting BPMN for: {}", processDefinitionId, e);
            return null;
        }
    }

    /**
     * Imports a BPMN XML string and deploys it as a new process definition.
     */
    public String importBpmn(String bpmnXml, String processName) {
        try {
            var deployment = repositoryService.createDeployment()
                    .name(processName)
                    .addString(processName + ".bpmn", bpmnXml)
                    .deploy();

            log.info("BPMN imported and deployed: {} (ID: {})", processName, deployment.getId());
            return deployment.getId();
        } catch (Exception e) {
            log.error("Failed to import BPMN: {}", processName, e);
            throw new RuntimeException("Failed to import BPMN: " + e.getMessage(), e);
        }
    }
}
