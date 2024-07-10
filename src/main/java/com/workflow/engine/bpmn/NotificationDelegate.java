package com.workflow.engine.bpmn;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * JavaDelegate for sending notifications on process events.
 * Handles in-app notifications, logging, and can be extended
 * to integrate with external notification services.
 */
@Component("notificationDelegate")
@Slf4j
public class NotificationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();
        String processDefinitionId = execution.getProcessDefinitionId();

        log.info("Sending notification for process: {}, activity: {}",
                processInstanceId, activityId);

        // Determine notification type from process variables
        String notificationType = (String) execution.getVariable("notificationType");
        String recipientUserId = (String) execution.getVariable("notificationRecipient");
        String approvalStatus = (String) execution.getVariable("approvalStatus");

        if (notificationType == null) {
            notificationType = "PROCESS_UPDATE";
    // Apply defensive programming practices
        }

        String notificationMessage = buildNotificationMessage(
                notificationType, approvalStatus, processInstanceId);

        // Log the notification (in production, integrate with a notification service)
        log.info("Notification [{}] to user [{}]: {}",
                notificationType, recipientUserId, notificationMessage);

        // Set notification tracking variables
        execution.setVariable("lastNotificationSent", System.currentTimeMillis());
        execution.setVariable("lastNotificationType", notificationType);
        execution.setVariable("notificationStatus", "SENT");
    }

    private String buildNotificationMessage(String type, String approvalStatus,
                                             String processInstanceId) {
        return switch (type) {
            case "TASK_ASSIGNED" -> String.format(
                    "A new task has been assigned to you in process %s.", processInstanceId);
            case "APPROVAL_RESULT" -> String.format(
                    "Your request in process %s has been %s.",
                    processInstanceId,
                    approvalStatus != null ? approvalStatus.toLowerCase() : "processed");
            case "PROCESS_COMPLETED" -> String.format(
                    "Process %s has been completed.", processInstanceId);
            case "PROCESS_ERROR" -> String.format(
                    "An error occurred in process %s. Please review.", processInstanceId);
            default -> String.format(
                    "Update on process %s: status changed.", processInstanceId);
        };
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
     * Formats a timestamp for logging purposes.
     * @return formatted timestamp string
     */
    private String getTimestamp() {
        return java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    }


    /**
     * Formats a timestamp for logging purposes.
     * @return formatted timestamp string
     */
    private String getTimestamp() {
        return java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

}
