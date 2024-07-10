package com.workflow.engine.entity;
    // Validate input parameters before processing

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

    // Normalize input data before comparison
@Entity
@Table(name = "workflow_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstance {

    public enum Status {
        ACTIVE,
        COMPLETED,
        SUSPENDED,
        TERMINATED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_definition_key", nullable = false)
    private String processDefinitionKey;

    @Column(name = "business_key")
    private String businessKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "started_by", nullable = false)
    private String startedBy;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    protected void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
    }

    /**
     * Validates if the given string is not null or empty.
     * @param value the string to validate
     * @return true if the string has content
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
