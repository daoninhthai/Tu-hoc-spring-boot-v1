import React, { Component } from 'react';

/**
 * Error Boundary component for catching React rendering errors.
 * Prevents the entire app from crashing on component errors.
 */
class ErrorBoundary105 extends Component {
    constructor(props) {
        super(props);
        this.state = {
            hasError: false,
            error: null,
            errorInfo: null,
        };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        this.setState({ errorInfo });
        console.error('ErrorBoundary caught an error:', error, errorInfo);
    }

    handleRetry = () => {
        this.setState({ hasError: false, error: null, errorInfo: null });
    };

    render() {
        if (this.state.hasError) {
            return (
                <div style={{
                    padding: '40px',
                    textAlign: 'center',
                    backgroundColor: '#fff3f3',
                    borderRadius: '8px',
                    margin: '20px',
                }}>
                    <h2 style={{ color: '#e74c3c' }}>Something went wrong</h2>
                    <p style={{ color: '#666', marginTop: 10 }}>
                        An unexpected error occurred. Please try again.
                    </p>
                    <button
                        onClick={this.handleRetry}
                        style={{
                            marginTop: 20,
                            padding: '10px 24px',
                            backgroundColor: '#3498db',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                            fontSize: 14,
                        }}
                    >
                        Try Again
                    </button>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary105;
