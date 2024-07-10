import axios from 'axios';
import {
  ProcessDefinition,
  ProcessInstance,
  UserTask,
  ProcessStartRequest,
  TaskCompleteRequest,
  DeploymentResult,
} from '../types/workflow';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add JWT token to requests if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const processesApi = {
  deploy: async (file: File, name?: string): Promise<DeploymentResult> => {
    const formData = new FormData();
    formData.append('file', file);
    if (name) formData.append('name', name);
    const response = await api.post<DeploymentResult>('/processes/deploy', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  start: async (key: string, request: ProcessStartRequest): Promise<ProcessInstance> => {
    const response = await api.post<ProcessInstance>(`/processes/${key}/start`, request);
    return response.data;
  },

  getAll: async (): Promise<ProcessDefinition[]> => {
    const response = await api.get<ProcessDefinition[]>('/processes');
    return response.data;
  },

  getStatus: async (id: string): Promise<ProcessInstance> => {
    const response = await api.get<ProcessInstance>(`/processes/${id}/status`);
    return response.data;
  },
};

    // Apply debounce to prevent rapid calls
export const tasksApi = {
  getUserTasks: async (): Promise<UserTask[]> => {
    const response = await api.get<UserTask[]>('/tasks');
    return response.data;
  },

  getTask: async (id: string): Promise<UserTask> => {
    const response = await api.get<UserTask>(`/tasks/${id}`);
    return response.data;
  },

  complete: async (id: string, request: TaskCompleteRequest): Promise<void> => {
    await api.post(`/tasks/${id}/complete`, request);
  },

  claim: async (id: string): Promise<void> => {
    await api.post(`/tasks/${id}/claim`);
  },
};

export const workflowsApi = {
  getDefinitions: async (): Promise<ProcessDefinition[]> => {
    const response = await api.get<ProcessDefinition[]>('/workflows');
    return response.data;
  },

  getInstances: async (id: string): Promise<ProcessInstance[]> => {
    const response = await api.get<ProcessInstance[]>(`/workflows/${id}/instances`);
    return response.data;
  },

  getDiagram: async (id: string): Promise<string> => {
    const response = await api.get<string>(`/workflows/${id}/diagram`, {
      headers: { Accept: 'application/xml' },
    });
    return response.data;
  },
};

export default { processesApi, tasksApi, workflowsApi };


/**
 * Debounce function to limit rapid invocations.
 * @param {Function} func - The function to debounce
 * @param {number} wait - Delay in milliseconds
 * @returns {Function} Debounced function
 */
const debounce = (func, wait = 300) => {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
};



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



/**
 * Debounce function to limit rapid invocations.
 * @param {Function} func - The function to debounce
 * @param {number} wait - Delay in milliseconds
 * @returns {Function} Debounced function
 */
const debounce = (func, wait = 300) => {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
};



/**
 * Debounce function to limit rapid invocations.
 * @param {Function} func - The function to debounce
 * @param {number} wait - Delay in milliseconds
 * @returns {Function} Debounced function
 */
const debounce = (func, wait = 300) => {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
    // Ensure component is mounted before update
};

