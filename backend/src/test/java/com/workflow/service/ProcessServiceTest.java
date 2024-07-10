package com.workflow.service;

import com.workflow.engine.entity.WorkflowInstance;
import com.workflow.engine.repository.WorkflowInstanceRepository;
import com.workflow.engine.service.ProcessService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private HistoryService historyService;

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @InjectMocks
    private ProcessService processService;

    @Captor
    private ArgumentCaptor<WorkflowInstance> workflowInstanceCaptor;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContext securityContext = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("testuser", null, List.of());
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("Process Start Tests")
    class StartProcessTests {

        @Mock
        private ProcessInstance processInstance;

        @Test
        @DisplayName("Should start process with business key and variables")
        void startProcess_withBusinessKeyAndVariables_shouldStartAndTrack() {
            // Arrange
            String processKey = "approval-process";
            String businessKey = "ORDER-2024-001";
            Map<String, Object> variables = Map.of(
                    "requester", "john.doe",
                    "amount", 5000,
                    "department", "engineering"
            );

            when(runtimeService.startProcessInstanceByKey(processKey, businessKey, variables))
                    .thenReturn(processInstance);
            when(processInstance.getId()).thenReturn("proc-instance-123");
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProcessInstance result = processService.startProcess(processKey, businessKey, variables);

            // Assert
            assertThat(result).isEqualTo(processInstance);
            verify(runtimeService).startProcessInstanceByKey(processKey, businessKey, variables);
            verify(runtimeService, never()).startProcessInstanceByKey(eq(processKey), anyMap());

            verify(workflowInstanceRepository).save(workflowInstanceCaptor.capture());
            WorkflowInstance tracked = workflowInstanceCaptor.getValue();
            assertThat(tracked.getProcessDefinitionKey()).isEqualTo("approval-process");
            assertThat(tracked.getBusinessKey()).isEqualTo("ORDER-2024-001");
            assertThat(tracked.getStatus()).isEqualTo(WorkflowInstance.Status.ACTIVE);
            assertThat(tracked.getStartedBy()).isEqualTo("testuser");
            assertThat(tracked.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should start process without business key")
        void startProcess_withoutBusinessKey_shouldUseKeyOnlyOverload() {
            // Arrange
            String processKey = "simple-process";
            Map<String, Object> variables = Map.of("priority", "high");

            when(runtimeService.startProcessInstanceByKey(processKey, variables))
                    .thenReturn(processInstance);
            when(processInstance.getId()).thenReturn("proc-instance-456");
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProcessInstance result = processService.startProcess(processKey, null, variables);

            // Assert
            assertThat(result).isEqualTo(processInstance);
            verify(runtimeService).startProcessInstanceByKey(processKey, variables);
            verify(runtimeService, never()).startProcessInstanceByKey(eq(processKey), anyString(), anyMap());
        }

        @Test
        @DisplayName("Should start process with blank business key as if null")
        void startProcess_withBlankBusinessKey_shouldTreatAsNull() {
            // Arrange
            Map<String, Object> variables = Map.of("key", "value");

            when(runtimeService.startProcessInstanceByKey("some-process", variables))
                    .thenReturn(processInstance);
            when(processInstance.getId()).thenReturn("proc-789");
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            processService.startProcess("some-process", "   ", variables);

            // Assert
            verify(runtimeService).startProcessInstanceByKey("some-process", variables);
        }

        @Test
        @DisplayName("Should start process with null variables as empty map")
        void startProcess_withNullVariables_shouldPassEmptyMap() {
            // Arrange
            when(runtimeService.startProcessInstanceByKey("my-process", "BK-001", Map.of()))
                    .thenReturn(processInstance);
            when(processInstance.getId()).thenReturn("proc-999");
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            processService.startProcess("my-process", "BK-001", null);

            // Assert
            verify(runtimeService).startProcessInstanceByKey("my-process", "BK-001", Map.of());
        }

        @Test
        @DisplayName("Should default to 'system' when no security context is available")
        void startProcess_noSecurityContext_shouldDefaultToSystem() {
            // Arrange
            SecurityContextHolder.clearContext();

            when(runtimeService.startProcessInstanceByKey(anyString(), anyMap()))
                    .thenReturn(processInstance);
            when(processInstance.getId()).thenReturn("proc-anon");
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            processService.startProcess("anon-process", null, null);

            // Assert
            verify(workflowInstanceRepository).save(workflowInstanceCaptor.capture());
    // Apply defensive programming practices
            assertThat(workflowInstanceCaptor.getValue().getStartedBy()).isEqualTo("system");
        }
    }

    @Nested
    @DisplayName("Process Deployment Tests")
    class DeployProcessTests {

        @Mock
        private DeploymentBuilder deploymentBuilder;

        @Mock
        private Deployment deployment;

        @Test
        @DisplayName("Should deploy BPMN file with custom deployment name")
        void deployProcess_withCustomName_shouldUseProvidedName() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "approval.bpmn", "application/xml",
                    "<bpmn>test</bpmn>".getBytes());

            when(repositoryService.createDeployment()).thenReturn(deploymentBuilder);
            when(deploymentBuilder.name("Custom Deployment")).thenReturn(deploymentBuilder);
            when(deploymentBuilder.addInputStream(eq("approval.bpmn"), any(InputStream.class)))
                    .thenReturn(deploymentBuilder);
            when(deploymentBuilder.deploy()).thenReturn(deployment);
            when(deployment.getId()).thenReturn("deploy-123");

            // Act
            String deploymentId = processService.deployProcess(file, "Custom Deployment");

            // Assert
            assertThat(deploymentId).isEqualTo("deploy-123");
            verify(deploymentBuilder).name("Custom Deployment");
        }

        @Test
        @DisplayName("Should fall back to original filename when no deployment name given")
        void deployProcess_withNullName_shouldFallbackToFilename() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "leave-request.bpmn", "application/xml",
                    "<bpmn>content</bpmn>".getBytes());

            when(repositoryService.createDeployment()).thenReturn(deploymentBuilder);
            when(deploymentBuilder.name("leave-request.bpmn")).thenReturn(deploymentBuilder);
            when(deploymentBuilder.addInputStream(eq("leave-request.bpmn"), any(InputStream.class)))
                    .thenReturn(deploymentBuilder);
            when(deploymentBuilder.deploy()).thenReturn(deployment);
            when(deployment.getId()).thenReturn("deploy-456");

            // Act
            String deploymentId = processService.deployProcess(file, null);

            // Assert
            assertThat(deploymentId).isEqualTo("deploy-456");
            verify(deploymentBuilder).name("leave-request.bpmn");
        }
    }

    @Nested
    @DisplayName("Process Status & Variable Tests")
    class ProcessStatusTests {

        @Test
        @DisplayName("Should return status with variables for an active process")
        void getProcessStatus_activeProcess_shouldReturnStatusWithVariables() {
            // Arrange
            String processInstanceId = "proc-100";
            ProcessInstance activeInstance = mock(ProcessInstance.class);
            ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);

            when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
            when(processQuery.processInstanceId(processInstanceId)).thenReturn(processQuery);
            when(processQuery.singleResult()).thenReturn(activeInstance);

            when(activeInstance.getId()).thenReturn(processInstanceId);
            when(activeInstance.getProcessDefinitionId()).thenReturn("approval:1:def-1");
            when(activeInstance.getBusinessKey()).thenReturn("ORDER-100");
            when(activeInstance.isSuspended()).thenReturn(false);

            Map<String, Object> variables = new HashMap<>();
            variables.put("amount", 10000);
            variables.put("approver", "manager@company.com");
            when(runtimeService.getVariables(processInstanceId)).thenReturn(variables);

            // Act
            Map<String, Object> status = processService.getProcessStatus(processInstanceId);

            // Assert
            assertThat(status).isNotNull();
            assertThat(status.get("processInstanceId")).isEqualTo(processInstanceId);
            assertThat(status.get("processDefinitionId")).isEqualTo("approval:1:def-1");
            assertThat(status.get("businessKey")).isEqualTo("ORDER-100");
            assertThat(status.get("isSuspended")).isEqualTo(false);
            assertThat(status.get("isEnded")).isEqualTo(false);

            @SuppressWarnings("unchecked")
            Map<String, Object> returnedVars = (Map<String, Object>) status.get("variables");
            assertThat(returnedVars).containsEntry("amount", 10000);
            assertThat(returnedVars).containsEntry("approver", "manager@company.com");
        }

        @Test
        @DisplayName("Should return historic data for a completed process")
        void getProcessStatus_completedProcess_shouldReturnHistoricData() {
            // Arrange
            String processInstanceId = "proc-200";

            ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);
            when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
            when(processQuery.processInstanceId(processInstanceId)).thenReturn(processQuery);
            when(processQuery.singleResult()).thenReturn(null);

            HistoricProcessInstance historic = mock(HistoricProcessInstance.class);
            HistoricProcessInstanceQuery historyQuery = mock(HistoricProcessInstanceQuery.class);
            when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historyQuery);
            when(historyQuery.processInstanceId(processInstanceId)).thenReturn(historyQuery);
            when(historyQuery.singleResult()).thenReturn(historic);

            when(historic.getId()).thenReturn(processInstanceId);
            when(historic.getProcessDefinitionId()).thenReturn("approval:1:def-1");
            when(historic.getBusinessKey()).thenReturn("ORDER-200");
            when(historic.getDurationInMillis()).thenReturn(86400000L);

            // Act
            Map<String, Object> status = processService.getProcessStatus(processInstanceId);

            // Assert
            assertThat(status).isNotNull();
            assertThat(status.get("isEnded")).isEqualTo(true);
            assertThat(status.get("durationInMillis")).isEqualTo(86400000L);
        }

        @Test
        @DisplayName("Should return null for unknown process instance")
        void getProcessStatus_unknownProcess_shouldReturnNull() {
            // Arrange
            String processInstanceId = "non-existent";

            ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);
            when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
            when(processQuery.processInstanceId(processInstanceId)).thenReturn(processQuery);
            when(processQuery.singleResult()).thenReturn(null);

            HistoricProcessInstanceQuery historyQuery = mock(HistoricProcessInstanceQuery.class);
            when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historyQuery);
            when(historyQuery.processInstanceId(processInstanceId)).thenReturn(historyQuery);
            when(historyQuery.singleResult()).thenReturn(null);

            // Act
            Map<String, Object> status = processService.getProcessStatus(processInstanceId);

            // Assert
            assertThat(status).isNull();
        }
    }

    @Nested
    @DisplayName("Process Termination Tests")
    class TerminateProcessTests {

        @Test
        @DisplayName("Should terminate process and update workflow instance status")
        void terminateProcess_shouldDeleteAndUpdateTracking() {
            // Arrange
            String processInstanceId = "proc-terminate-1";
            String reason = "Cancelled by admin";

            WorkflowInstance tracked = WorkflowInstance.builder()
                    .id(1L)
                    .processDefinitionKey("approval-process")
                    .businessKey("ORDER-CANCEL")
                    .status(WorkflowInstance.Status.ACTIVE)
                    .startedBy("testuser")
                    .build();

            when(workflowInstanceRepository.findAll()).thenReturn(List.of(tracked));
            when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            processService.terminateProcess(processInstanceId, reason);

            // Assert
            verify(runtimeService).deleteProcessInstance(processInstanceId, reason);

            verify(workflowInstanceRepository).save(workflowInstanceCaptor.capture());
            WorkflowInstance updated = workflowInstanceCaptor.getValue();
            assertThat(updated.getStatus()).isEqualTo(WorkflowInstance.Status.TERMINATED);
            assertThat(updated.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should terminate process even when no tracking record exists")
        void terminateProcess_noTrackingRecord_shouldStillDeleteProcess() {
            // Arrange
            when(workflowInstanceRepository.findAll()).thenReturn(List.of());

            // Act
            processService.terminateProcess("proc-orphan", "Cleanup");

            // Assert
            verify(runtimeService).deleteProcessInstance("proc-orphan", "Cleanup");
            verify(workflowInstanceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Process Definition Listing Tests")
    class ProcessDefinitionTests {

        @Test
        @DisplayName("Should list all latest process definitions with active instance counts")
        void getAllProcessDefinitions_shouldReturnDefinitionsWithCounts() {
            // Arrange
            ProcessDefinition def1 = mock(ProcessDefinition.class);
            when(def1.getId()).thenReturn("approval:1:def-1");
            when(def1.getKey()).thenReturn("approval");
            when(def1.getName()).thenReturn("Approval Process");
            when(def1.getVersion()).thenReturn(1);
            when(def1.getDeploymentId()).thenReturn("deploy-1");
            when(def1.isSuspended()).thenReturn(false);

            ProcessDefinitionQuery defQuery = mock(ProcessDefinitionQuery.class);
            when(repositoryService.createProcessDefinitionQuery()).thenReturn(defQuery);
            when(defQuery.latestVersion()).thenReturn(defQuery);
            when(defQuery.orderByProcessDefinitionName()).thenReturn(defQuery);
            when(defQuery.asc()).thenReturn(defQuery);
            when(defQuery.list()).thenReturn(List.of(def1));

            ProcessInstanceQuery instanceQuery = mock(ProcessInstanceQuery.class);
            when(runtimeService.createProcessInstanceQuery()).thenReturn(instanceQuery);
            when(instanceQuery.processDefinitionId("approval:1:def-1")).thenReturn(instanceQuery);
            when(instanceQuery.active()).thenReturn(instanceQuery);
            when(instanceQuery.count()).thenReturn(5L);

            // Act
            List<Map<String, Object>> definitions = processService.getAllProcessDefinitions();

            // Assert
            assertThat(definitions).hasSize(1);
            Map<String, Object> first = definitions.get(0);
            assertThat(first.get("key")).isEqualTo("approval");
            assertThat(first.get("name")).isEqualTo("Approval Process");
            assertThat(first.get("activeInstanceCount")).isEqualTo(5L);
            assertThat(first.get("isSuspended")).isEqualTo(false);
        }
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
