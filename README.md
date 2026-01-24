# AI-Powered Financial Risk & Fraud Management Platform

This repository contains a full-stack platform with:
- A Spring Boot backend exposing REST APIs for authentication, transactions, fraud detection, fraud pattern analytics, and credit-risk assessment.
- A React + TypeScript + Tailwind CSS frontend providing a role-aware dashboard for admins, analysts, auditors, and SME users.

Docker files are present in the repository, but the project is currently designed and validated to run locally (no Docker-based workflow is actively maintained).

---

## Table of Contents

- [High-Level Architecture](#high-level-architecture)
- [Repository Structure](#repository-structure)
- [Running the Project Locally (No Docker)](#running-the-project-locally-no-docker)
- [Core Features](#core-features)
- [Documentation](#documentation)
- [Testing and Tools](#testing-and-tools)
- [Roadmap (High Level)](#roadmap-high-level)

---

## High-Level Architecture

### Technology Stack

| Layer       | Technology                                                |
|------------|-----------------------------------------------------------|
| Backend    | Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security|
| Database   | PostgreSQL 16                                             |
| AI / ML    | DJL (PyTorch), ONNX Runtime, TensorFlow Java             |
| Frontend   | React, TypeScript, Vite, Tailwind CSS, React Router      |
| Build/Tools| Maven, Node.js, npm                                      |

### System Overview

- Backend exposes REST APIs under `/api/v1/**` for:
  - Authentication and user management.
  - Transaction creation, listing, and statistics.
  - Fraud detection (ensemble of 3 models) and fraud pattern analytics.
  - Credit-risk assessment and credit-risk statistics.
- Frontend consumes these APIs to render:
  - A consolidated dashboard (KPIs, charts, trends).
  - Dedicated pages for transactions, fraud detection, and credit risk.
  - Role-based navigation and restricted areas (Admin, Analyst, Auditor, SME).
- PostgreSQL stores users, transactions, fraud patterns, credit-risk assessments, and related metadata.

For a deeper backend view, see:
- [backend/docs/BACKEND-OVERVIEW.md](backend/docs/BACKEND-OVERVIEW.md)
- [backend/docs/BACKEND-API-GUIDE.md](backend/docs/BACKEND-API-GUIDE.md)

For a deeper frontend view, see:
- [frontend/docs/FRONTEND-OVERVIEW.md](frontend/docs/FRONTEND-OVERVIEW.md)
- [frontend/docs/FRONTEND-FEATURES.md](frontend/docs/FRONTEND-FEATURES.md)

---

## Repository Structure

```text
.
├── backend/                # Spring Boot application
│   ├── src/main/java/      # REST controllers, services, entities, DTOs
│   ├── src/main/resources/ # application.properties, model files, seeds
│   ├── scripts/            # Shell scripts to exercise APIs end-to-end
│   └── pom.xml             # Maven build definition
├── frontend/               # React + TypeScript SPA
│   ├── src/                # Components, pages, hooks, context, services
│   ├── public/             # Static assets (logos, images)
│   └── package.json        # Frontend dependencies and scripts
└── README.md               # This global documentation
```

### Backend Folder Layout (Overview)

- `backend/src/main/java/...`
  - `controller/` – REST controllers exposing `/api/v1/**` endpoints.
  - `service/` – business logic for auth, users, transactions, fraud, credit risk.
  - `entity/` – JPA entities for `User`, `Transaction`, `FraudPattern`, `CreditRiskAssessment`, etc.
  - `dto/` – request/response records used by the API and frontend.
  - `repository/` – Spring Data JPA repositories.
  - `config/` – security (JWT, CORS), OpenAPI/Swagger, and other infrastructure.
  - `ai/fraud/` – wrappers around DJL, ONNX, and TensorFlow fraud detectors.
- `backend/src/main/resources/`
  - `application.properties` – Spring Boot and PostgreSQL configuration.
  - `models/` – serialized AI model files (e.g., `.onnx`, `.pt`) if present.
- `backend/scripts/`
  - Shell scripts to run typical flows (auth, fraud detection, credit-risk, full flow).

### Frontend Folder Layout (Overview)

- `frontend/src/`
  - `pages/` – top-level screens (Dashboard, Transactions, Fraud Detection, Credit Risk, Users, Profile).
  - `components/` – reusable UI building blocks (tables, forms, cards, modals).
  - `layout/` – `AppLayout`, header, sidebar, and protected route handling.
  - `context/` – `AuthContext`, `SidebarContext`, `ThemeContext`.
  - `services/` – API clients for backend endpoints.
  - `types/` – TypeScript types matching backend DTOs.
  - `utils/` – helper functions (formatting, charts, etc.).

### Backend Modules (Conceptual)

- Authentication & Security
  - JWT-based stateless authentication.
  - Role-based authorization guards on controllers.
- User Management
  - CRUD, locking/unlocking, and statistics by role.
- Transactions
  - Support for PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT.
  - Status lifecycle: PENDING, COMPLETED, FAILED, FRAUD_DETECTED, CANCELLED.
  - Statistics and time-series trends for dashboard KPIs.
- Fraud Detection & Patterns
  - Ensemble of DJL, ONNX, and TensorFlow-based detectors.
  - Fraud pattern storage with pattern types, confidence, description, and metadata.
  - Aggregated fraud statistics and pattern review workflow.
- Credit Risk
  - Credit-risk assessments for SME users based on financial metrics.
  - Risk categories (LOW, MEDIUM, HIGH, CRITICAL) with scores and summaries.
  - Sector-based and user-based statistics, plus high-risk and unreviewed views.

### Frontend Modules (Conceptual)

- Authentication & Layout
  - Global `AuthContext` for authenticated user state and roles.
  - `AppLayout` with header, sidebar, and protected routes.
- Dashboard
  - Aggregated KPIs for transactions, fraud patterns, credit risk, and users.
  - Charts for transaction trends and credit-risk distribution.
- Transactions Page
  - Paginated table with filtering and detail views.
- Fraud Detection Page
  - Manual fraud detection trigger for a transaction.
  - Fraud patterns table with review status and derived severity.
- Credit Risk Page
  - List of credit-risk assessments with filters.
  - Modal to run a new credit-risk assessment for an SME user.
  - Detail view for a single assessment.

---

## Running the Project Locally (No Docker)

The recommended and validated way to run the platform is fully local (no containers). Docker files exist but are not maintained in this version.

### Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 18+ (20+ recommended)
- PostgreSQL 16 installed and running locally

### Step 1 – Clone the Repository

```bash
git clone https://github.com/<your-username>/financial.git
cd financial
```

### Step 2 – Configure PostgreSQL (Backend Database)

1. Ensure PostgreSQL is running (default port `5432`).
2. Create the database and user (example configuration):

```bash
psql -U postgres
CREATE DATABASE financial_db;
CREATE USER financial_user WITH PASSWORD 'financial_pass';
GRANT ALL PRIVILEGES ON DATABASE financial_db TO financial_user;
\q
```

3. Set the Spring Boot datasource to point to this database. Edit `backend/src/main/resources/application.properties` so the datasource section is consistent with your PostgreSQL setup, for example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/financial_db
spring.datasource.username=financial_user
spring.datasource.password=financial_pass

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

You can alternatively externalize these values using environment variables, but the above in-file configuration is sufficient for local development.

### Step 3 – Build and Run the Backend

From the project root:

```bash
cd backend
./mvnw clean package
./mvnw spring-boot:run
```

The backend will listen on `http://localhost:8080` and expose interactive API docs (Swagger UI) at `http://localhost:8080/swagger-ui.html`.

### Step 4 – (Optional) Run Backend Test Scripts

With the backend running, you can execute example flows from `backend/scripts/`, e.g.:

```bash
cd backend
./scripts/test-complete-fraud-flow.sh
```

These scripts log in with seeded users, create transactions, run fraud detection, and exercise credit-risk endpoints.

### Step 5 – Install Dependencies and Run the Frontend

In a new terminal, from the project root:

```bash
cd frontend
npm install
npm run dev
```

By default, the frontend runs on `http://localhost:5173` (Vite), calling the backend at `http://localhost:8080`.

---

## Core Features

### Authentication and Roles

- JWT-based login and stateless auth.
- Roles: ADMIN, FINANCIAL_ANALYST, SME_USER, AUDITOR.
- Role-aware navigation and permission checks in both backend and frontend.

Default test users (seeded on first run):

- ADMIN: `admin1@financial.tn` / `Admin123!`
- AUDITOR: `auditor1@financial.tn` / `Audit123!`
- FINANCIAL_ANALYST: `analyst1@financial.tn` / `Analyst123!`
- SME_USER: `sme1@company.tn` / `Sme123!`

### Transaction Management

- Create and manage transactions of multiple types (PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT).
- Role-based access to global vs. own transactions.
- Transaction statistics and trends used by the dashboard.

### Fraud Detection & Patterns

- Manual fraud detection endpoint for any transaction.
- Ensemble of three models (DJL, ONNX, TensorFlow) with feature extraction.
- Fraud patterns persisted with:
  - Pattern type (e.g., high-amount, late-night, borderline suspicious).
  - Confidence and detector model.
  - Structured metadata (amount, hour, isWeekend, etc.).
- Review workflow: mark patterns as reviewed, and see unresolved vs. reviewed counts.

### Credit-Risk Assessment

- Accepts structured financial data per SME user (revenue, assets, liabilities, cash flow, credit history, sector, etc.).
- Produces a risk score, risk category, assessment summary, and model-level predictions.
- Stores persistent `CreditRiskAssessment` entities with review notes and timestamps.
- Provides statistics:
  - Counts by risk category.
  - High-risk and unreviewed assessments.
  - Sector-based and user-based aggregations.

### Dashboard & Analytics (Frontend)

- Combined view of:
  - Total transactions, amounts, and per-status breakdown.
  - Fraud patterns and unresolved vs. reviewed counts.
  - Credit-risk distribution by risk category (LOW/MEDIUM/HIGH/CRITICAL).
  - User statistics by role and account status.

---

## Documentation

- Global overview (this file): architecture, structure, and critical features.
- Backend docs (more detail):
  - [backend/docs/BACKEND-OVERVIEW.md](backend/docs/BACKEND-OVERVIEW.md) – modules, entities, and data model.
  - [backend/docs/BACKEND-API-GUIDE.md](backend/docs/BACKEND-API-GUIDE.md) – main REST endpoints and flows.
- Frontend docs (more detail):
  - [frontend/docs/FRONTEND-OVERVIEW.md](frontend/docs/FRONTEND-OVERVIEW.md) – structure, routing, and state.
  - [frontend/docs/FRONTEND-FEATURES.md](frontend/docs/FRONTEND-FEATURES.md) – pages, components, and UX.

---

## Testing and Tools

The backend includes several shell scripts under `backend/scripts/` to exercise end-to-end flows (authentication, transactions, fraud detection, credit-risk).

Basic local test flow:

1. Start the backend (`./mvnw spring-boot:run`).
2. Run a test script from `backend/scripts/` (for example, full fraud flow or credit-risk test).
3. Start the frontend (`npm run dev` in `frontend/`) and navigate through the dashboard, transactions, fraud detection, and credit-risk pages.

---

## Roadmap (High Level)

- Improve ONNX model integration with a fully trained model.
- Add automatic fraud detection triggers on transaction creation.
- Enable transaction blocking and alerting for high-confidence fraud.
- Extend credit-risk models and introduce model training pipelines.
- Add richer monitoring, logging, and production readiness features.

---

This README is the single source of truth for the global architecture. For implementation-level detail, refer to the backend and frontend documentation linked above.
