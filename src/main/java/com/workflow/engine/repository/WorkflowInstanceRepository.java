package com.workflow.engine.repository;

import com.workflow.engine.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    List<WorkflowInstance> findByStatus(WorkflowInstance.Status status);

    List<WorkflowInstance> findByStartedBy(String startedBy);

    List<WorkflowInstance> findByProcessDefinitionKeyAndStatus(String processDefinitionKey,
                                                               WorkflowInstance.Status status);

}
