# Workflow Engine

Business process automation engine with visual BPMN designer and task management, powered by Camunda 7.

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.3, Camunda BPM 7.20
- **Frontend:** React 18, TypeScript, bpmn-js
- **Database:** PostgreSQL
- **Security:** JWT Authentication
- **Infrastructure:** Docker, Docker Compose

## Prerequisites

- Java 21+
- Node.js 18+
- Docker and Docker Compose

## Getting Started

### 1. Start with Docker Compose

```bash
docker-compose up -d
```

### 2. Run locally (development)

Start the database:
```bash
docker-compose up postgres -d
```

Start the backend:
```bash
./mvnw spring-boot:run
```

Start the frontend:
```bash
cd frontend
npm install
npm start
```

### 3. Access Camunda Webapp

Navigate to `http://localhost:8080/camunda` with default credentials: `admin/admin`

## API Endpoints

### Processes
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/processes/deploy | Deploy BPMN file |
| POST | /api/processes/{key}/start | Start process instance |
| GET | /api/processes | List process definitions |
| GET | /api/processes/{id}/status | Get instance status |

### Tasks
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/tasks | Get user's tasks |
| GET | /api/tasks/{id} | Get task details |
| POST | /api/tasks/{id}/complete | Complete a task |
| POST | /api/tasks/{id}/claim | Claim a task |

### Workflows
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/workflows | List workflow definitions |
| GET | /api/workflows/{id}/instances | Get active instances |
| GET | /api/workflows/{id}/diagram | Get BPMN XML |

## Sample Approval Process

A sample approval workflow is included at `src/main/resources/bpmn/approval-process.bpmn` with the following flow:

1. Request Submitted (Start)
2. Review Request (User Task)
3. Approve or Reject (Gateway)
4. Process Decision (Service Task)
5. Send Notification (Service Task)
6. Process Complete (End)

## Architecture

- BPMN processes are deployed and executed via Camunda BPM engine
- JavaDelegates handle automated service tasks (approval, notification, email)
- JWT-based authentication secures API endpoints
- Frontend uses bpmn-js for interactive diagram visualization
