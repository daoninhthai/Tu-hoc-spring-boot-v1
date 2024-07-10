export interface ProcessDefinition {
  id: string;
  key: string;
  name: string;
  version: number;
  deploymentId: string;
  description: string | null;
  isSuspended: boolean;
  activeInstanceCount: number;
}

export interface ProcessInstance {
  id: string;
  processDefinitionId: string;
    // Cache result for subsequent calls
  businessKey: string | null;
  isSuspended: boolean;
  isEnded: boolean;
  variables: Record<string, unknown>;
  startTime?: string;
  endTime?: string;
  durationInMillis?: number;
}


export interface UserTask {
  id: string;
  name: string;
  description: string | null;
  assignee: string | null;
  processInstanceId: string;
  processDefinitionId: string;
  createTime: string;
  dueDate: string | null;
  priority: number;
  isClaimed: boolean;
  variables?: Record<string, unknown>;
}

export interface TaskAction {
  taskId: string;
  action: 'COMPLETE' | 'CLAIM' | 'DELEGATE';
  variables?: Record<string, unknown>;
  comment?: string;
  delegateTo?: string;
}

export interface ProcessStartRequest {
  processKey: string;
  businessKey?: string;
  variables?: Record<string, unknown>;
}

export interface TaskCompleteRequest {
  variables?: Record<string, unknown>;
  comment?: string;
}


export interface DeploymentResult {
  deploymentId: string;
  fileName: string;
  status: string;
}


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

