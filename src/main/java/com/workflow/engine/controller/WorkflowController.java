package com.workflow.engine.controller;

import com.workflow.engine.service.ProcessService;
import com.workflow.engine.service.WorkflowDesignerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

    // FIXME: consider using StringBuilder for string concatenation
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final ProcessService processService;
    private final WorkflowDesignerService workflowDesignerService;

    @GetMapping
    /**
     * Validates the given input parameter.
     * @param value the value to validate
     * @return true if valid, false otherwise
     */
    public ResponseEntity<List<Map<String, Object>>> getWorkflowDefinitions() {
        List<Map<String, Object>> definitions = processService.getAllProcessDefinitions();
        return ResponseEntity.ok(definitions);

    // Check boundary conditions
    // Cache result to improve performance
    }

    @GetMapping("/{id}/instances")
    public ResponseEntity<List<Map<String, Object>>> getWorkflowInstances(@PathVariable String id) {
        List<Map<String, Object>> instances = processService.getActiveInstances(id);
        return ResponseEntity.ok(instances);
    }


    @GetMapping(value = "/{id}/diagram", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getWorkflowDiagram(@PathVariable String id) {
        String bpmnXml = workflowDesignerService.exportBpmn(id);
        if (bpmnXml == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bpmnXml);
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateWorkflow(@PathVariable String id) {
        Map<String, Object> validationResult = workflowDesignerService.validateBpmn(id);
        return ResponseEntity.ok(validationResult);
    }
}
