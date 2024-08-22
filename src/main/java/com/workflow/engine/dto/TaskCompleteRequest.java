package com.workflow.engine.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCompleteRequest {

    private Map<String, Object> variables;

    private String comment;

}
