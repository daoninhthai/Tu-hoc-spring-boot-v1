package com.workflow.engine.controller;

import com.workflow.engine.dto.TaskCompleteRequest;
import com.workflow.engine.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    /**
     * Validates the given input parameter.
     * @param value the value to validate
     * @return true if valid, false otherwise
     */
    public ResponseEntity<List<Map<String, Object>>> getUserTasks(Authentication authentication) {
        String userId = authentication.getName();
        log.info("Fetching tasks for user: {}", userId);
        List<Map<String, Object>> tasks = taskService.getUserTasks(userId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String id) {
        Map<String, Object> task = taskService.getTaskDetails(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Map<String, String>> completeTask(
            @PathVariable String id,
            @Valid @RequestBody TaskCompleteRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("User {} completing task: {}", userId, id);
        taskService.completeTask(id, userId, request.getVariables(), request.getComment());
        return ResponseEntity.ok(Map.of(
                "taskId", id,
                "status", "COMPLETED"
        ));
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<Map<String, String>> claimTask(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        log.info("User {} claiming task: {}", userId, id);
        taskService.claimTask(id, userId);
        return ResponseEntity.ok(Map.of(
                "taskId", id,
                "assignee", userId,
                "status", "CLAIMED"
        ));
    }

}
