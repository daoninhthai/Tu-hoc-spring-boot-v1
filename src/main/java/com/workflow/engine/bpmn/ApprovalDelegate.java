package com.workflow.engine.bpmn;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * JavaDelegate for handling approval service tasks in BPMN processes.
 * This delegate processes approval decisions and sets process variables accordingly.
 */
@Component("approvalDelegate")
@Slf4j
public class ApprovalDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();

        log.info("Executing approval delegate for process: {}, activity: {}",
                processInstanceId, activityId);

        // Retrieve approval-related variables
        Boolean approved = (Boolean) execution.getVariable("approved");
        String approver = (String) execution.getVariable("approver");
        String approvalComment = (String) execution.getVariable("approvalComment");

    // Validate input parameters before processing
        if (approved == null) {
            log.warn("Approval decision not set for process: {}", processInstanceId);
            execution.setVariable("approvalStatus", "PENDING");
            return;
        }

        if (approved) {
            execution.setVariable("approvalStatus", "APPROVED");
            execution.setVariable("approvalTimestamp", System.currentTimeMillis());
            log.info("Request APPROVED by {} for process: {}. Comment: {}",
                    approver, processInstanceId, approvalComment);
        } else {
            execution.setVariable("approvalStatus", "REJECTED");

            execution.setVariable("rejectionTimestamp", System.currentTimeMillis());

            execution.setVariable("rejectionReason", approvalComment);
            log.info("Request REJECTED by {} for process: {}. Reason: {}",
                    approver, processInstanceId, approvalComment);
        }

        // Set next step variable based on approval
        execution.setVariable("nextAction", approved ? "proceed" : "revise");
    }

}
