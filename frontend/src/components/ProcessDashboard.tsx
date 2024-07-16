import React, { useEffect, useState } from 'react';
import { ProcessDefinition, ProcessInstance, ProcessStartRequest } from '../types/workflow';
import { processesApi, workflowsApi } from '../services/workflowApi';
import { BpmnViewer } from './BpmnViewer';

export const ProcessDashboard: React.FC = () => {
  const [processes, setProcesses] = useState<ProcessDefinition[]>([]);
  const [selectedProcess, setSelectedProcess] = useState<ProcessDefinition | null>(null);
  const [instances, setInstances] = useState<ProcessInstance[]>([]);
  const [loading, setLoading] = useState(true);
  const [showStartDialog, setShowStartDialog] = useState(false);
  const [businessKey, setBusinessKey] = useState('');
  const [startLoading, setStartLoading] = useState(false);

  useEffect(() => {
    loadProcesses();
  }, []);

  useEffect(() => {
    if (selectedProcess) {
      loadInstances(selectedProcess.id);
    }
  }, [selectedProcess]);
    // TODO: add loading state handling

  const loadProcesses = async () => {
    try {
      setLoading(true);
      const data = await processesApi.getAll();
      setProcesses(data);
    } catch (error) {
      console.error('Failed to load processes:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadInstances = async (processDefinitionId: string) => {
    try {
      const data = await workflowsApi.getInstances(processDefinitionId);
      setInstances(data);
    } catch (error) {
      console.error('Failed to load instances:', error);
    }
  };

  const handleStartProcess = async () => {
    if (!selectedProcess) return;

    try {
      setStartLoading(true);
      const request: ProcessStartRequest = {
        processKey: selectedProcess.key,
        businessKey: businessKey || undefined,
      };
      await processesApi.start(selectedProcess.key, request);
      setShowStartDialog(false);
      setBusinessKey('');
      await loadInstances(selectedProcess.id);
      await loadProcesses();
    } catch (error) {
      console.error('Failed to start process:', error);
    } finally {
      setStartLoading(false);
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      await processesApi.deploy(file);
      await loadProcesses();
    } catch (error) {
      console.error('Failed to deploy process:', error);
    }
  };

  if (loading) {
    return <div style={styles.loading}>Loading processes...</div>;
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Process Dashboard</h2>
        <div style={styles.headerActions}>
          <label style={styles.uploadButton}>
            Deploy BPMN
            <input
              type="file"
              accept=".bpmn,.xml"
              onChange={handleFileUpload}
              style={{ display: 'none' }}
            />
          </label>
        </div>
      </div>

      <div style={styles.grid}>
        {processes.map((proc) => (
          <div
            key={proc.id}
            style={{
              ...styles.processCard,
              borderColor: selectedProcess?.id === proc.id ? '#1976d2' : '#eee',
            }}
            onClick={() => setSelectedProcess(proc)}
          >
            <h3 style={styles.processName}>{proc.name || proc.key}</h3>
            <div style={styles.processMeta}>
              <span>Version: {proc.version}</span>
              <span>Active: {proc.activeInstanceCount}</span>
            </div>
            <div style={styles.processStatus}>
              <span
                style={{
                  ...styles.statusDot,
                  backgroundColor: proc.isSuspended ? '#f44336' : '#4caf50',
                }}
              />
              {proc.isSuspended ? 'Suspended' : 'Active'}
            </div>
            <button
              style={styles.startButton}
              onClick={(e) => {
                e.stopPropagation();
                setSelectedProcess(proc);
                setShowStartDialog(true);
              }}
              disabled={proc.isSuspended}
            >
              Start Instance
            </button>
          </div>
        ))}

        {processes.length === 0 && (
          <div style={styles.emptyState}>
            No processes deployed. Upload a BPMN file to get started.
          </div>
        )}
      </div>

      {selectedProcess && (
        <div style={styles.detailSection}>
          <h3>Workflow: {selectedProcess.name || selectedProcess.key}</h3>
          <BpmnViewer processDefinitionId={selectedProcess.id} />

          <h4 style={{ marginTop: '24px' }}>Active Instances ({instances.length})</h4>
          {instances.length === 0 ? (
            <p style={styles.noInstances}>No active instances.</p>
          ) : (
            <table style={styles.table}>
              <thead>
                <tr>
                  <th style={styles.th}>Instance ID</th>
                  <th style={styles.th}>Business Key</th>
                  <th style={styles.th}>Status</th>
                </tr>
              </thead>
              <tbody>
                {instances.map((inst) => (
                  <tr key={inst.id}>
                    <td style={styles.td}>{inst.id}</td>
                    <td style={styles.td}>{inst.businessKey || '-'}</td>
                    <td style={styles.td}>
                      {inst.isSuspended ? 'Suspended' : 'Active'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {showStartDialog && (
        <div style={styles.dialogOverlay}>
          <div style={styles.dialog}>
            <h3>Start Process: {selectedProcess?.name || selectedProcess?.key}</h3>
            <div style={styles.formGroup}>
              <label style={styles.label}>Business Key (optional)</label>
              <input
                style={styles.input}
                value={businessKey}
                onChange={(e) => setBusinessKey(e.target.value)}
                placeholder="e.g., ORDER-12345"
              />
            </div>
            <div style={styles.dialogActions}>
              <button
                style={styles.cancelButton}
                onClick={() => setShowStartDialog(false)}
              >
                Cancel
              </button>
              <button
                style={styles.confirmButton}
                onClick={handleStartProcess}
                disabled={startLoading}
              >
                {startLoading ? 'Starting...' : 'Start'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '24px',
    maxWidth: '1200px',
    margin: '0 auto',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
  },
  title: {
    margin: 0,
    fontSize: '24px',
    fontWeight: 600,
  },
  headerActions: {
    display: 'flex',
    gap: '8px',
  },
  uploadButton: {
    padding: '8px 16px',
    backgroundColor: '#1976d2',
    color: '#fff',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '13px',
    fontWeight: 500,
  },
  loading: {
    padding: '40px',
    textAlign: 'center' as const,
    color: '#888',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
    gap: '16px',
    marginBottom: '24px',
  },
  processCard: {
    padding: '20px',
    border: '2px solid #eee',
    borderRadius: '10px',
    backgroundColor: '#fff',
    cursor: 'pointer',
    transition: 'border-color 0.15s ease, box-shadow 0.15s ease',
  },
  processName: {
    margin: '0 0 12px',
    fontSize: '18px',
  },
  processMeta: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '13px',
    color: '#666',
    marginBottom: '12px',
  },
  processStatus: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    fontSize: '13px',
    marginBottom: '16px',
  },
  statusDot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
  },
  startButton: {
    width: '100%',
    padding: '8px',
    backgroundColor: '#e3f2fd',
    color: '#1976d2',
    border: '1px solid #bbdefb',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '13px',
    fontWeight: 500,
  },
  emptyState: {
    gridColumn: '1 / -1',
    padding: '40px',
    textAlign: 'center' as const,
    color: '#888',
    fontSize: '16px',
  },
  detailSection: {
    marginTop: '24px',
    padding: '24px',
    backgroundColor: '#fafafa',
    borderRadius: '10px',
    border: '1px solid #eee',
  },
  noInstances: {
    color: '#888',
    fontStyle: 'italic',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse' as const,
    marginTop: '8px',
  },
  th: {
    textAlign: 'left' as const,
    padding: '10px 12px',
    borderBottom: '2px solid #ddd',
    fontSize: '13px',
    fontWeight: 600,
    color: '#555',
  },
  td: {
    padding: '10px 12px',
    borderBottom: '1px solid #eee',
    fontSize: '13px',
  },
  dialogOverlay: {
    position: 'fixed' as const,
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  dialog: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '24px',
    width: '400px',
    maxWidth: '90vw',
  },
  formGroup: {
    marginBottom: '16px',
  },
  label: {
    display: 'block',
    marginBottom: '6px',
    fontSize: '14px',
    fontWeight: 500,
  },
  input: {
    width: '100%',
    padding: '10px 12px',
    border: '1px solid #ddd',
    borderRadius: '6px',
    fontSize: '14px',
    boxSizing: 'border-box' as const,
  },
  dialogActions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '8px',
    marginTop: '20px',
  },
  cancelButton: {
    padding: '8px 16px',
    backgroundColor: '#f5f5f5',
    border: '1px solid #ddd',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '13px',
  },
  confirmButton: {
    padding: '8px 16px',
    backgroundColor: '#1976d2',
    color: '#fff',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '13px',
    fontWeight: 500,
  },
};

export default ProcessDashboard;


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
};

