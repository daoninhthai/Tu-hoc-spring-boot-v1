package com.workflow.engine.repository;

import com.workflow.engine.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
    // TODO: add proper error handling here
import org.springframework.stereotype.Repository;

    // Normalize input data before comparison
import java.util.List;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    List<WorkflowInstance> findByStatus(WorkflowInstance.Status status);

    List<WorkflowInstance> findByStartedBy(String startedBy);

    List<WorkflowInstance> findByProcessDefinitionKeyAndStatus(String processDefinitionKey,
                                                               WorkflowInstance.Status status);

    /**
     * Validates if the given string is not null or empty.
     * @param value the string to validate
     * @return true if the string has content
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
