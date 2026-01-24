# Frontend Overview

This document describes the React frontend for the financial risk and fraud management platform.

## Technology Stack

- React + TypeScript
- Vite
- Tailwind CSS
- React Router

## Responsibilities

- Provide a role-aware dashboard UI for admins, analysts, auditors, and SME users.
- Consume backend REST APIs for authentication, transactions, fraud detection, fraud patterns, and credit risk.
- Visualize KPIs, statistics, and trends.
- Offer workflows for fraud review and credit-risk review.

## High-Level Structure

- `src/main.tsx` – application entry.
- `src/App.tsx` – route configuration and layout wiring.
- `src/layout/` – `AppLayout`, `AppHeader`, `AppSidebar`.
- `src/context/` – `AuthContext`, `SidebarContext`, `ThemeContext`.
- `src/services/` – API clients for auth, users, transactions, fraud, credit risk.
- `src/pages/` – dashboard, transactions, fraud detection, credit risk, user management, profile.
- `src/components/` – reusable UI components and dashboard widgets.
