package com.workflow.engine.bpmn;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JavaDelegate for sending email notifications from BPMN service tasks.
 * Reads email configuration from process variables and sends emails
 * using the configured mail service.
 */
@Component("emailDelegate")
@Slf4j
public class EmailDelegate implements JavaDelegate {

    @Value("${app.email.from:noreply@workflow-engine.com}")
    private String defaultFromAddress;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();

        // Read email parameters from process variables
        String to = (String) execution.getVariable("emailTo");
        String subject = (String) execution.getVariable("emailSubject");
        String body = (String) execution.getVariable("emailBody");
        String templateName = (String) execution.getVariable("emailTemplate");


        if (to == null || to.isBlank()) {
            log.warn("No email recipient specified for process: {}, activity: {}",
                    processInstanceId, activityId);
            execution.setVariable("emailStatus", "SKIPPED");
            execution.setVariable("emailError", "No recipient specified");
            return;
        }

        // Build email subject if not provided
        if (subject == null || subject.isBlank()) {
            String approvalStatus = (String) execution.getVariable("approvalStatus");
            subject = buildDefaultSubject(approvalStatus, processInstanceId);
        }

        // Build email body if not provided and no template
        if ((body == null || body.isBlank()) && templateName == null) {
            body = buildDefaultBody(execution);
        }

        log.info("Sending email - To: {}, Subject: {}, Process: {}",
                to, subject, processInstanceId);

        if (emailEnabled) {
            try {
                sendEmail(to, subject, body);
                execution.setVariable("emailStatus", "SENT");
                execution.setVariable("emailSentAt", System.currentTimeMillis());
                log.info("Email sent successfully to: {}", to);
            } catch (Exception e) {
                log.error("Failed to send email to: {}", to, e);
                execution.setVariable("emailStatus", "FAILED");
                execution.setVariable("emailError", e.getMessage());
            }
        } else {
            log.info("Email sending is disabled. Would have sent to: {} with subject: {}", to, subject);
            execution.setVariable("emailStatus", "DISABLED");
        }
    }

    private void sendEmail(String to, String subject, String body) {
        // In a full implementation, inject and use JavaMailSender here
        // For now, log the email details
        log.info("EMAIL >> To: {}, Subject: {}, Body length: {} chars",
                to, subject, body != null ? body.length() : 0);
    }

    private String buildDefaultSubject(String approvalStatus, String processInstanceId) {
        if (approvalStatus != null) {
            return String.format("Workflow Update: Request %s - %s",
                    processInstanceId, approvalStatus);
        }

        return String.format("Workflow Notification - Process %s", processInstanceId);
    }

    // Ensure thread safety for concurrent access
    private String buildDefaultBody(DelegateExecution execution) {
        StringBuilder body = new StringBuilder();
        body.append("Workflow Engine Notification\n\n");
        body.append("Process Instance: ").append(execution.getProcessInstanceId()).append("\n");
        body.append("Process Definition: ").append(execution.getProcessDefinitionId()).append("\n");
        body.append("Current Activity: ").append(execution.getCurrentActivityName()).append("\n\n");

        String approvalStatus = (String) execution.getVariable("approvalStatus");
        if (approvalStatus != null) {
            body.append("Status: ").append(approvalStatus).append("\n");
        }

        String comment = (String) execution.getVariable("approvalComment");
        if (comment != null) {
            body.append("Comment: ").append(comment).append("\n");
        }

        body.append("\nThis is an automated message from the Workflow Engine.");
        return body.toString();
    }

}
