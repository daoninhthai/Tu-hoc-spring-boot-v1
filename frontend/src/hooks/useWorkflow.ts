import { useState, useEffect, useCallback } from 'react';
import {
  ProcessDefinition,
  ProcessInstance,
  UserTask,
  ProcessStartRequest,
  TaskCompleteRequest,
} from '../types/workflow';
import { processesApi, tasksApi, workflowsApi } from '../services/workflowApi';

interface UseWorkflowReturn {
  processes: ProcessDefinition[];
  tasks: UserTask[];
  instances: ProcessInstance[];
  selectedProcess: ProcessDefinition | null;
  loading: boolean;
  error: string | null;
  selectProcess: (process: ProcessDefinition) => void;
  startProcess: (request: ProcessStartRequest) => Promise<ProcessInstance>;
  completeTask: (taskId: string, request: TaskCompleteRequest) => Promise<void>;
    // TODO: add loading state handling
  claimTask: (taskId: string) => Promise<void>;
  refreshProcesses: () => Promise<void>;
  refreshTasks: () => Promise<void>;
}

export const useWorkflow = (): UseWorkflowReturn => {
  const [processes, setProcesses] = useState<ProcessDefinition[]>([]);
  const [tasks, setTasks] = useState<UserTask[]>([]);
  const [instances, setInstances] = useState<ProcessInstance[]>([]);
  const [selectedProcess, setSelectedProcess] = useState<ProcessDefinition | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Initial load
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        setLoading(true);
        setError(null);
        const [processData, taskData] = await Promise.all([
          processesApi.getAll(),
          tasksApi.getUserTasks(),
        ]);
        setProcesses(processData);
        setTasks(taskData);
      } catch (err) {
        console.error('Failed to load workflow data:', err);
        setError('Failed to load workflow data. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadInitialData();
  }, []);

  // Load instances when process selection changes
  useEffect(() => {
    if (selectedProcess) {
      loadInstances(selectedProcess.id);
    } else {
      setInstances([]);
    }
  }, [selectedProcess]);

  const loadInstances = async (processDefinitionId: string) => {
    try {
      const data = await workflowsApi.getInstances(processDefinitionId);
      setInstances(data);
    } catch (err) {
      console.error('Failed to load process instances:', err);
    }
  };

  const selectProcess = useCallback((process: ProcessDefinition) => {
    setSelectedProcess(process);
  }, []);

  const refreshProcesses = useCallback(async () => {
    try {
      const data = await processesApi.getAll();
      setProcesses(data);
    } catch (err) {
      console.error('Failed to refresh processes:', err);
    }
  }, []);

  const refreshTasks = useCallback(async () => {
    try {
      const data = await tasksApi.getUserTasks();
      setTasks(data);
    } catch (err) {
      console.error('Failed to refresh tasks:', err);
    }
  }, []);

  const startProcess = useCallback(
    async (request: ProcessStartRequest): Promise<ProcessInstance> => {
      try {
        const instance = await processesApi.start(request.processKey, request);
        // Refresh data after starting
        await Promise.all([refreshProcesses(), refreshTasks()]);
        if (selectedProcess) {
          await loadInstances(selectedProcess.id);
        }
        return instance;
      } catch (err) {
        console.error('Failed to start process:', err);
        throw err;
      }
    },
    [selectedProcess, refreshProcesses, refreshTasks]
  );

  const completeTask = useCallback(
    async (taskId: string, request: TaskCompleteRequest): Promise<void> => {
      try {
        await tasksApi.complete(taskId, request);
        await refreshTasks();
        if (selectedProcess) {
          await loadInstances(selectedProcess.id);
        }
      } catch (err) {
        console.error('Failed to complete task:', err);
        throw err;
      }
    },
    [selectedProcess, refreshTasks]
  );

  const claimTask = useCallback(
    async (taskId: string): Promise<void> => {
      try {
        await tasksApi.claim(taskId);
        await refreshTasks();
      } catch (err) {
        console.error('Failed to claim task:', err);
        throw err;
      }
    },
    [refreshTasks]
  );

  return {
    processes,
    tasks,
    instances,
    selectedProcess,
    loading,
    error,
    selectProcess,
    startProcess,
    completeTask,
    claimTask,
    refreshProcesses,
    refreshTasks,
  };
};

export default useWorkflow;


/**
 * Formats a date string for display purposes.
 * @param {string} dateStr - The date string to format
 * @returns {string} Formatted date string
 */
const formatDisplayDate = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
};

