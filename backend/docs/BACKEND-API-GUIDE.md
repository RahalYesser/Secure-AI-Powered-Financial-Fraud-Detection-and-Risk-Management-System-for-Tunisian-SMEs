# Backend API Guide

This guide summarizes the most important REST endpoints exposed by the Spring Boot backend.

All endpoints are prefixed with `/api/v1` and secured with JWT unless stated otherwise.

## Authentication

- `POST /api/v1/auth/login` – authenticate user and return JWT.
- `POST /api/v1/auth/register` – register a new user (configurable access).

## Users

- `GET /api/v1/users` – list users (ADMIN, AUDITOR).
- `GET /api/v1/users/{id}` – get a single user (ADMIN, AUDITOR, self).
- `PUT /api/v1/users/{id}` – update user details (ADMIN, self with constraints).
- `POST /api/v1/users/{id}/lock` – lock account (ADMIN).
- `POST /api/v1/users/{id}/unlock` – unlock account (ADMIN).
- `GET /api/v1/users/statistics` – aggregated user statistics by role and status.

## Transactions

- `POST /api/v1/transactions` – create a transaction (triggers fraud analysis path).
- `GET /api/v1/transactions` – list transactions with pagination and filtering.
- `GET /api/v1/transactions/{id}` – get transaction details.
- `GET /api/v1/transactions/user/{userId}` – list a users transactions.
- `PUT /api/v1/transactions/{id}/status` – update status (ADMIN/ANALYST).
- `DELETE /api/v1/transactions/{id}/cancel` – cancel a pending transaction.
- `GET /api/v1/transactions/statistics` – high-level transaction statistics.
- `GET /api/v1/transactions/trends` – time-series data for dashboard charts.

## Fraud Detection & Patterns

- `POST /api/v1/fraud/detect/{transactionId}` – run fraud detection for one transaction.
- `GET /api/v1/fraud/patterns` – paginated fraud patterns (ADMIN, AUDITOR, ANALYST).
- `GET /api/v1/fraud/patterns/unreviewed` – only unreviewed patterns.
- `PUT /api/v1/fraud/patterns/{id}/review` – mark pattern as reviewed.
- `GET /api/v1/fraud/statistics` – aggregate statistics (total, unresolved, by type, etc.).

## Credit Risk

- `POST /api/v1/credit-risk/assess` – run a new credit-risk assessment.
- `GET /api/v1/credit-risk` – list assessments (paginated).
- `GET /api/v1/credit-risk/{id}` – get assessment details.
- `GET /api/v1/credit-risk/report/{id}` – detailed credit-risk report.
- `GET /api/v1/credit-risk/user/{userId}` – assessments for a specific SME user.
- `GET /api/v1/credit-risk/statistics` – counts per risk category.
- `GET /api/v1/credit-risk/high-risk` – high-risk assessments.
- `GET /api/v1/credit-risk/unreviewed/high-risk` – unreviewed high-risk assessments.
- `PUT /api/v1/credit-risk/{id}/review` – add review notes and mark as reviewed.

These endpoints are the ones primarily used by the frontend. For live schemas and examples, use Swagger UI at `http://localhost:8080/swagger-ui.html`.
