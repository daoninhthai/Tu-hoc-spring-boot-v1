package com.workflow.engine.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessStartRequest {

    private String processKey;

    private String businessKey;

    private Map<String, Object> variables;

}
