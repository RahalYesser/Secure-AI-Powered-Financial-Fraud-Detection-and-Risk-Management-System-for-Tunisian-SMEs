# Backend Overview

This document describes the Spring Boot backend for the financial risk and fraud management platform.

## Responsibilities

- Authenticate and authorize users with JWT and role-based access control.
- Manage users, roles, and account status.
- Persist and manage financial transactions.
- Run AI-powered fraud detection and store fraud patterns.
- Provide credit-risk assessments and statistics for SME users.
- Expose analytics endpoints for the frontend dashboard.

## Main Modules

- `auth` – login, registration, JWT issuance and validation.
- `user` – user CRUD, role management, lock/unlock, statistics.
- `transaction` – CRUD, filtering, statistics, trends.
- `fraud` – fraud detection, patterns, statistics.
- `credit-risk` – credit-risk assessment, storage, statistics.

## Key Components

- **Controllers**: REST endpoints under `/api/v1/**`.
- **Services**: Business logic for auth, users, transactions, fraud, and credit risk.
- **Repositories**: Spring Data JPA repositories for all entities.
- **Entities**: `User`, `Transaction`, `FraudPattern`, `CreditRiskAssessment` and related enums.
- **DTOs/Records**: Request/response shapes matching what the frontend consumes.

## Data Model (High Level)

- `users`: application accounts with role, status, and login metadata.
- `transactions`: financial movements tied to users, with type and status.
- `fraud_patterns`: stored fraud detections with metadata and review flags.
- `credit_risk_assessments`: stored assessments with scores, categories, and review notes.
