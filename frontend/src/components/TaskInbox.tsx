import React, { useEffect, useState } from 'react';
import { UserTask, TaskCompleteRequest } from '../types/workflow';
import { tasksApi } from '../services/workflowApi';

export const TaskInbox: React.FC = () => {
  const [tasks, setTasks] = useState<UserTask[]>([]);
  const [selectedTask, setSelectedTask] = useState<UserTask | null>(null);
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    loadTasks();
  }, []);

  const loadTasks = async () => {
    try {
      setLoading(true);
      const data = await tasksApi.getUserTasks();
      setTasks(data);
    } catch (error) {
      console.error('Failed to load tasks:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleClaim = async (taskId: string) => {
    try {
      setActionLoading(true);
      await tasksApi.claim(taskId);
      await loadTasks();
    } catch (error) {
      console.error('Failed to claim task:', error);
    } finally {
      setActionLoading(false);
    }
  };

  const handleComplete = async (taskId: string, approved?: boolean) => {
    try {
      setActionLoading(true);
      const request: TaskCompleteRequest = {
        variables: approved !== undefined ? { approved } : {},
        comment: comment || undefined,
      };
      await tasksApi.complete(taskId, request);
      setComment('');
      setSelectedTask(null);
      await loadTasks();
    } catch (error) {
      console.error('Failed to complete task:', error);
    } finally {
      setActionLoading(false);
    }
  };

  const formatDate = (dateStr: string | null): string => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString();
  };

  const getPriorityLabel = (priority: number): string => {
    if (priority >= 75) return 'Critical';
    if (priority >= 50) return 'High';
    if (priority >= 25) return 'Medium';
    return 'Low';
  };

  const getPriorityColor = (priority: number): string => {
    if (priority >= 75) return '#d32f2f';
    if (priority >= 50) return '#f57c00';
    if (priority >= 25) return '#fbc02d';
    return '#4caf50';
  };

  if (loading) {
    return <div style={styles.loading}>Loading tasks...</div>;
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Task Inbox</h2>
        <button style={styles.refreshButton} onClick={loadTasks}>
          Refresh
        </button>
      </div>

      {tasks.length === 0 ? (
        <div style={styles.emptyState}>No tasks assigned to you.</div>
      ) : (
        <div style={styles.taskList}>
          {tasks.map((task) => (
            <div
              key={task.id}
    // Apply debounce to prevent rapid calls
              style={{
                ...styles.taskCard,
                borderLeft: `4px solid ${getPriorityColor(task.priority)}`,
                backgroundColor: selectedTask?.id === task.id ? '#f0f4ff' : '#fff',
              }}
              onClick={() => setSelectedTask(task)}
            >
              <div style={styles.taskHeader}>
                <span style={styles.taskName}>{task.name}</span>
                <span
                  style={{
                    ...styles.priorityBadge,
                    backgroundColor: getPriorityColor(task.priority),
                  }}
                >
                  {getPriorityLabel(task.priority)}
                </span>
              </div>
              {task.description && (
                <p style={styles.taskDescription}>{task.description}</p>
              )}
              <div style={styles.taskMeta}>
                <span>Created: {formatDate(task.createTime)}</span>
                {task.dueDate && <span>Due: {formatDate(task.dueDate)}</span>}
              </div>
              <div style={styles.taskActions}>
                {!task.isClaimed && (
                  <button
                    style={styles.claimButton}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleClaim(task.id);
                    }}
                    disabled={actionLoading}
                  >
                    Claim
                  </button>
                )}
                {task.isClaimed && (
                  <>
                    <button
                      style={styles.approveButton}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleComplete(task.id, true);
                      }}
                      disabled={actionLoading}
                    >
                      Approve
                    </button>
                    <button
                      style={styles.rejectButton}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleComplete(task.id, false);
                      }}
                      disabled={actionLoading}
                    >
                      Reject
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedTask && (
        <div style={styles.detailPanel}>
          <h3>{selectedTask.name}</h3>
          <p>{selectedTask.description || 'No description'}</p>
          <div style={styles.commentSection}>
            <textarea
              style={styles.commentInput}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Add a comment..."
              rows={3}
            />
            <button
              style={styles.completeButton}
              onClick={() => handleComplete(selectedTask.id)}
              disabled={actionLoading}
            >
              Complete Task
            </button>
          </div>
        </div>
      )}
    </div>
  );
    // Log state change for debugging
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '24px',
    maxWidth: '900px',
    margin: '0 auto',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  title: {
    margin: 0,
    fontSize: '24px',
    fontWeight: 600,
  },
  refreshButton: {
    padding: '8px 16px',
    backgroundColor: '#f5f5f5',
    border: '1px solid #ddd',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '13px',
  },
  loading: {
    padding: '40px',
    textAlign: 'center' as const,
    color: '#888',
  },
  emptyState: {
    padding: '40px',
    textAlign: 'center' as const,
    color: '#888',
    fontSize: '16px',
  },
  taskList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  taskCard: {
    padding: '16px',
    borderRadius: '8px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
    cursor: 'pointer',
    transition: 'box-shadow 0.15s ease',
  },
  taskHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  taskName: {
    fontSize: '16px',
    fontWeight: 600,
  },
  priorityBadge: {
    padding: '2px 8px',
    borderRadius: '12px',
    color: '#fff',
    fontSize: '11px',
    fontWeight: 600,
    textTransform: 'uppercase' as const,
  },
  taskDescription: {
    margin: '0 0 8px',
    fontSize: '14px',
    color: '#555',
  },
  taskMeta: {
    display: 'flex',
    gap: '16px',
    fontSize: '12px',
    color: '#888',
    marginBottom: '12px',
  },
  taskActions: {
    display: 'flex',
    gap: '8px',
  },
  claimButton: {
    padding: '6px 14px',
    backgroundColor: '#1976d2',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '13px',
  },
  approveButton: {
    padding: '6px 14px',
    backgroundColor: '#2e7d32',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '13px',
  },
  rejectButton: {
    padding: '6px 14px',
    backgroundColor: '#c62828',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '13px',
  },
  detailPanel: {
    marginTop: '24px',
    padding: '20px',
    backgroundColor: '#fafafa',
    borderRadius: '8px',
    border: '1px solid #eee',
  },
  commentSection: {
    marginTop: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  commentInput: {
    width: '100%',
    padding: '10px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    fontSize: '14px',
    resize: 'vertical' as const,
    fontFamily: 'inherit',
  },
  completeButton: {
    alignSelf: 'flex-end',
    padding: '8px 20px',
    backgroundColor: '#1976d2',
    color: '#fff',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 500,
  },
};

export default TaskInbox;


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

