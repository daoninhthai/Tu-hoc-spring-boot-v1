package com.workflow.engine.service;

import com.workflow.engine.entity.TaskAssignment;
import com.workflow.engine.repository.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;

import org.camunda.bpm.engine.TaskService as CamundaTaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final CamundaTaskService camundaTaskService;
    private final HistoryService historyService;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public List<Map<String, Object>> getUserTasks(String userId) {
        // Get tasks assigned to user or in candidate groups
        List<Task> assignedTasks = camundaTaskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        List<Task> candidateTasks = camundaTaskService.createTaskQuery()
                .taskCandidateUser(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        Set<String> taskIds = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Task task : assignedTasks) {
            if (taskIds.add(task.getId())) {
                result.add(mapTask(task, true));
            }
        }

        for (Task task : candidateTasks) {
            if (taskIds.add(task.getId())) {
                result.add(mapTask(task, false));
            }
        }

        return result;
    }

    public Map<String, Object> getTaskDetails(String taskId) {
        Task task = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            return null;
        }

        Map<String, Object> details = mapTask(task, task.getAssignee() != null);
        details.put("variables", camundaTaskService.getVariables(taskId));
        return details;
    }

    @Transactional
    public void completeTask(String taskId, String userId, Map<String, Object> variables,
                              String comment) {
        Task task = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        // Add comment if provided
        if (comment != null && !comment.isBlank()) {
            camundaTaskService.createComment(taskId, task.getProcessInstanceId(), comment);
        }

        // Complete the task
        if (variables != null && !variables.isEmpty()) {
            camundaTaskService.complete(taskId, variables);
        } else {
            camundaTaskService.complete(taskId);
        }

        // Update tracking record
        taskAssignmentRepository.findByAssigneeAndStatus(userId, TaskAssignment.Status.CLAIMED)
                .stream()
                .filter(ta -> ta.getTaskId().equals(taskId))
                .findFirst()
                .ifPresent(ta -> {
                    ta.setStatus(TaskAssignment.Status.COMPLETED);
                    taskAssignmentRepository.save(ta);
                });

        log.info("Task completed: {} by user: {}", taskId, userId);
    }

    @Transactional
    public void claimTask(String taskId, String userId) {
        camundaTaskService.claim(taskId, userId);

        Task task = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        // Track the assignment
        TaskAssignment assignment = TaskAssignment.builder()
                .taskId(taskId)
                .processInstanceId(task.getProcessInstanceId())
                .assignee(userId)
                .taskName(task.getName())
                .status(TaskAssignment.Status.CLAIMED)
                .dueDate(task.getDueDate() != null
                        ? task.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .createdAt(LocalDateTime.now())
                .build();
        taskAssignmentRepository.save(assignment);

        log.info("Task claimed: {} by user: {}", taskId, userId);
    }

    @Transactional
    public void delegateTask(String taskId, String fromUserId, String toUserId) {
        camundaTaskService.delegateTask(taskId, toUserId);
        log.info("Task delegated: {} from {} to {}", taskId, fromUserId, toUserId);
    }

    public List<Map<String, Object>> getTaskHistory(String processInstanceId) {
        List<HistoricTaskInstance> historicTasks = historyService
                .createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();

        return historicTasks.stream()
                .map(task -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", task.getId());
                    map.put("name", task.getName());
                    map.put("assignee", task.getAssignee());
                    map.put("startTime", task.getStartTime());
                    map.put("endTime", task.getEndTime());
                    map.put("durationInMillis", task.getDurationInMillis());
                    map.put("deleteReason", task.getDeleteReason());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapTask(Task task, boolean isClaimed) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("name", task.getName());
        map.put("description", task.getDescription());
        map.put("assignee", task.getAssignee());
        map.put("processInstanceId", task.getProcessInstanceId());
        map.put("processDefinitionId", task.getProcessDefinitionId());
        map.put("createTime", task.getCreateTime());
        map.put("dueDate", task.getDueDate());
        map.put("priority", task.getPriority());
        map.put("isClaimed", isClaimed);
        return map;
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


    /**
     * Validates that the given value is within the expected range.
     * @param value the value to check
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @return true if value is within range
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

}
