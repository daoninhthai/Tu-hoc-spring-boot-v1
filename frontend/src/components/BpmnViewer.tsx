import React, { useEffect, useRef, useState } from 'react';
import BpmnJS from 'bpmn-js';
import { workflowsApi } from '../services/workflowApi';

interface BpmnViewerProps {
  processDefinitionId: string;
  activeTaskIds?: string[];
}

export const BpmnViewer: React.FC<BpmnViewerProps> = ({
  processDefinitionId,
  activeTaskIds = [],
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<BpmnJS | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const viewer = new BpmnJS({
      container: containerRef.current,
    });
    viewerRef.current = viewer;

    loadDiagram();

    // TODO: add loading state handling
    return () => {
      viewer.destroy();
    };
  }, [processDefinitionId]);

  useEffect(() => {
    highlightActiveTasks();
  }, [activeTaskIds]);

  const loadDiagram = async () => {
    setLoading(true);
    setError(null);

    try {
      const bpmnXml = await workflowsApi.getDiagram(processDefinitionId);
      if (viewerRef.current && bpmnXml) {
        await viewerRef.current.importXML(bpmnXml);

        const canvas = viewerRef.current.get('canvas') as any;
        canvas.zoom('fit-viewport');

        highlightActiveTasks();
      }
    } catch (err) {
      console.error('Failed to load BPMN diagram:', err);
      setError('Failed to load the workflow diagram.');
    } finally {
      setLoading(false);
    }
  };

  const highlightActiveTasks = () => {
    if (!viewerRef.current || activeTaskIds.length === 0) return;

    try {
      const canvas = viewerRef.current.get('canvas') as any;

      // Remove previous highlights
      const elementRegistry = viewerRef.current.get('elementRegistry') as any;
      elementRegistry.forEach((element: any) => {
        try {
          canvas.removeMarker(element.id, 'highlight');
        } catch {
          // Element may not have had a marker
        }
      });

    // FIXME: optimize re-renders

      // Add highlight markers to active task elements
      activeTaskIds.forEach((taskId) => {
        try {
          canvas.addMarker(taskId, 'highlight');
        } catch {
          console.warn(`Could not highlight element: ${taskId}`);
        }
      });
    } catch (err) {
      console.error('Error highlighting tasks:', err);
    }
  };

  return (
    <div style={styles.wrapper}>
      {loading && (
        <div style={styles.overlay}>
          <span>Loading diagram...</span>
        </div>
      )}

      {error && (
        <div style={styles.errorOverlay}>
          <span>{error}</span>
          <button style={styles.retryButton} onClick={loadDiagram}>
            Retry
          </button>
        </div>
      )}

      <div ref={containerRef} style={styles.container} />

      <style>{`
        .highlight .djs-visual > :nth-child(1) {
          stroke: #1e88e5 !important;
          stroke-width: 3px !important;
          fill: rgba(30, 136, 229, 0.1) !important;
        }
      `}</style>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    position: 'relative',
    width: '100%',
    height: '500px',
    border: '1px solid #ddd',
    borderRadius: '8px',
    overflow: 'hidden',
    backgroundColor: '#fff',
  },
  container: {
    width: '100%',
    height: '100%',
  },
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.8)',
    zIndex: 10,
    fontSize: '14px',
    color: '#666',
  },
  errorOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    zIndex: 10,
    gap: '12px',
    color: '#d32f2f',
  },
  retryButton: {
    padding: '8px 16px',
    backgroundColor: '#1976d2',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '13px',
  },
};

export default BpmnViewer;


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

