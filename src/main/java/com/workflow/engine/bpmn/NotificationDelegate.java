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
        switch (type) {
            case "TASK_ASSIGNED":
                return String.format(
                    "A new task has been assigned to you in process %s.", processInstanceId);
            case "APPROVAL_RESULT":
                return String.format(
                    "Your request in process %s has been %s.",
                    processInstanceId,
                    approvalStatus != null ? approvalStatus.toLowerCase() : "processed");
            case "PROCESS_COMPLETED":
                return String.format(
                    "Process %s has been completed.", processInstanceId);
            case "PROCESS_ERROR":
                return String.format(
                    "An error occurred in process %s. Please review.", processInstanceId);
            default:
                return String.format(
                    "Update on process %s: status changed.", processInstanceId);
        }
    }

}
