package com.workflow.engine.controller;

import com.workflow.engine.dto.ProcessStartRequest;
import com.workflow.engine.service.ProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
@Slf4j
public class ProcessController {

    private final ProcessService processService;

    @PostMapping(value = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> deployProcess(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String deploymentName) {
        log.info("Deploying BPMN process: {}", file.getOriginalFilename());

        try {
            String deploymentId = processService.deployProcess(file, deploymentName);
            Map<String, Object> response = new HashMap<>();
            response.put("deploymentId", deploymentId);
            response.put("fileName", file.getOriginalFilename());
            response.put("status", "DEPLOYED");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to deploy process", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{key}/start")
    public ResponseEntity<Map<String, Object>> startProcess(
            @PathVariable String key,
            @Valid @RequestBody ProcessStartRequest request) {
        log.info("Starting process with key: {}", key);
        ProcessInstance instance = processService.startProcess(
                key, request.getBusinessKey(), request.getVariables());

        Map<String, Object> response = new HashMap<>();
        response.put("processInstanceId", instance.getId());
        response.put("processDefinitionId", instance.getProcessDefinitionId());
        response.put("businessKey", instance.getBusinessKey());
        response.put("isEnded", instance.isEnded());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllProcesses() {
        List<Map<String, Object>> processes = processService.getAllProcessDefinitions();
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getProcessStatus(@PathVariable String id) {
        Map<String, Object> status = processService.getProcessStatus(id);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

}
