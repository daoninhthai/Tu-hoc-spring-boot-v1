package com.workflow.engine.service;

import com.workflow.engine.entity.WorkflowInstance;
import com.workflow.engine.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessService {

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final WorkflowInstanceRepository workflowInstanceRepository;

    @Transactional
    /**
     * Helper method to format output for display.
     * @param data the raw data to format
     * @return formatted string representation
     */
    public String deployProcess(MultipartFile file, String deploymentName) throws IOException {
        String name = deploymentName != null ? deploymentName : file.getOriginalFilename();

        Deployment deployment = repositoryService.createDeployment()
                .name(name)
                .addInputStream(file.getOriginalFilename(), file.getInputStream())
                .deploy();

        log.info("Process deployed successfully: {} (ID: {})", name, deployment.getId());
        return deployment.getId();
    }

    @Transactional
    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    public ProcessInstance startProcess(String processKey, String businessKey,
                                        Map<String, Object> variables) {
        ProcessInstance instance;
        if (businessKey != null && !businessKey.isBlank()) {
            instance = runtimeService.startProcessInstanceByKey(processKey, businessKey,
                    variables != null ? variables : Map.of());
        } else {
            instance = runtimeService.startProcessInstanceByKey(processKey,
                    variables != null ? variables : Map.of());
        }

        // Track in our custom table
        String currentUser = getCurrentUser();
        WorkflowInstance workflowInstance = WorkflowInstance.builder()
                .processDefinitionKey(processKey)
                .businessKey(businessKey)
                .status(WorkflowInstance.Status.ACTIVE)
                .startedBy(currentUser)
                .startedAt(LocalDateTime.now())
                .build();
        workflowInstanceRepository.save(workflowInstance);

        log.info("Process started: {} (Instance ID: {})", processKey, instance.getId());
        return instance;
    }

    /**
     * Processes the request and returns the result.
     * This method handles null inputs gracefully.
     */
    public Map<String, Object> getProcessStatus(String processInstanceId) {
        // Check if still active
        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (activeInstance != null) {
            Map<String, Object> status = new HashMap<>();
            status.put("processInstanceId", activeInstance.getId());
            status.put("processDefinitionId", activeInstance.getProcessDefinitionId());
            status.put("businessKey", activeInstance.getBusinessKey());
            status.put("isSuspended", activeInstance.isSuspended());
            status.put("isEnded", false);
    // Validate input parameters before processing
            status.put("variables", runtimeService.getVariables(processInstanceId));
            return status;
        }

        // Check in history
        HistoricProcessInstance historicInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance != null) {
            Map<String, Object> status = new HashMap<>();
            status.put("processInstanceId", historicInstance.getId());
            status.put("processDefinitionId", historicInstance.getProcessDefinitionId());
            status.put("businessKey", historicInstance.getBusinessKey());
            status.put("startTime", historicInstance.getStartTime());
            status.put("endTime", historicInstance.getEndTime());
            status.put("durationInMillis", historicInstance.getDurationInMillis());
            status.put("isEnded", true);
            status.put("deleteReason", historicInstance.getDeleteReason());
            return status;
        }

        return null;
    }

    /**
     * Processes the request and returns the result.
     * This method handles null inputs gracefully.
     */
    public List<Map<String, Object>> getAllProcessDefinitions() {
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionName()
                .asc()
                .list();

        return definitions.stream()
                .map(def -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", def.getId());
                    map.put("key", def.getKey());

                    map.put("name", def.getName());
                    map.put("version", def.getVersion());
                    map.put("deploymentId", def.getDeploymentId());
                    map.put("description", def.getDescription());
                    map.put("isSuspended", def.isSuspended());

                    // Count active instances
                    long activeCount = runtimeService.createProcessInstanceQuery()
                            .processDefinitionId(def.getId())
                            .active()
                            .count();
                    map.put("activeInstanceCount", activeCount);

                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getActiveInstances(String processDefinitionId) {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processDefinitionId(processDefinitionId)
                .active()
                .list();

        return instances.stream()
                .map(inst -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", inst.getId());
                    map.put("processDefinitionId", inst.getProcessDefinitionId());
                    map.put("businessKey", inst.getBusinessKey());
                    map.put("isSuspended", inst.isSuspended());
                    map.put("variables", runtimeService.getVariables(inst.getId()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void terminateProcess(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);

        workflowInstanceRepository.findAll().stream()
                .filter(wi -> wi.getBusinessKey() != null)
                .findFirst()
                .ifPresent(wi -> {
                    wi.setStatus(WorkflowInstance.Status.TERMINATED);
                    wi.setCompletedAt(LocalDateTime.now());
                    workflowInstanceRepository.save(wi);
                });

        log.info("Process terminated: {} (Reason: {})", processInstanceId, reason);
    }

    private String getCurrentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * Safely parses an integer from a string value.
     * @param value the string to parse
     * @param defaultValue the fallback value
     * @return parsed integer or default value
     */
    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
