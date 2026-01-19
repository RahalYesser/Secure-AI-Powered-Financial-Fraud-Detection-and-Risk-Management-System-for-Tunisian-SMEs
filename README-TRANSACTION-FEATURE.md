# Financial Transaction Management Feature

## Overview

This document explains the implementation of Feature 2: **Financial Transaction Management** in the Financial Application. This feature provides a complete transaction processing system with fraud detection, role-based access control, validation, and comprehensive filtering capabilities.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Components](#components)
3. [Database Schema](#database-schema)
4. [API Endpoints](#api-endpoints)
5. [Business Rules](#business-rules)
6. [Security & Roles](#security--roles)
7. [Usage Examples](#usage-examples)
8. [Testing](#testing)

---

## Architecture Overview

The transaction feature follows a clean, layered architecture:

```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database (PostgreSQL)
```

### Key Design Patterns

- **DTO Pattern**: Separation between entities and API responses
- **Repository Pattern**: Abstract data access
- **Service Pattern**: Encapsulate business logic
- **Exception Handling**: Global exception handler for consistent error responses

---

## Components

### 1. Enumerations

#### TransactionType
Located: `src/main/java/com/tunisia/financial/enumerations/TransactionType.java`

```java
public enum TransactionType {
    PAYMENT,      // Bill payment, purchase
    TRANSFER,     // Transfer between accounts
    WITHDRAWAL,   // Cash out
    DEPOSIT       // Cash in
}
```

#### TransactionStatus
Located: `src/main/java/com/tunisia/financial/enumerations/TransactionStatus.java`

```java
public enum TransactionStatus {
    PENDING,          // Transaction is pending processing
    COMPLETED,        // Transaction completed successfully
    FAILED,           // Transaction failed due to an error
    FRAUD_DETECTED    // Transaction flagged as potential fraud
}
```

### 2. Entity

#### Transaction Entity
Located: `src/main/java/com/tunisia/financial/entity/Transaction.java`

**Key Attributes:**
- `id` (Long): Primary key
- `type` (TransactionType): Type of transaction
- `status` (TransactionStatus): Current status
- `amount` (BigDecimal): Transaction amount (min 0.01)
- `user` (User): ManyToOne relationship
- `description` (String): Optional description
- `fraudScore` (Double): Fraud detection score (0.0 - 1.0)
- `referenceNumber` (String): Unique transaction identifier
- `receipt` (String): Receipt/confirmation code
- `createdAt` (Instant): Creation timestamp
- `updatedAt` (Instant): Last update timestamp

**Indexes:**
- `idx_transaction_user`: On user_id for fast user queries
- `idx_transaction_status`: On status for filtering
- `idx_transaction_type`: On type for filtering
- `idx_transaction_created`: On created_at for date range queries

### 3. DTOs (Data Transfer Objects)

Located: `src/main/java/com/tunisia/financial/dto/transaction/`

#### TransactionRequest
```java
public record TransactionRequest(
    @NotNull TransactionType type,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    String description
)
```

#### TransactionResponse
```java
public record TransactionResponse(
    Long id,
    TransactionType type,
    TransactionStatus status,
    BigDecimal amount,
    UUID userId,
    String userEmail,
    String description,
    Double fraudScore,
    String referenceNumber,
    String receipt,
    Instant createdAt,
    Instant updatedAt
)
```

#### TransactionStatistics
```java
public record TransactionStatistics(
    Long totalTransactions,
    Long pendingTransactions,
    Long completedTransactions,
    Long failedTransactions,
    Long fraudDetectedTransactions,
    BigDecimal totalAmount,
    BigDecimal averageAmount,
    Long paymentCount,
    Long transferCount,
    Long withdrawalCount,
    Long depositCount
)
```

### 4. Repository

Located: `src/main/java/com/tunisia/financial/repository/TransactionRepository.java`

**Custom Query Methods:**
- `findByUserId(UUID userId, Pageable pageable)`: Get user transactions
- `findByStatus(TransactionStatus status, Pageable pageable)`: Filter by status
- `findByType(TransactionType type, Pageable pageable)`: Filter by type
- `findByReferenceNumber(String referenceNumber)`: Find by reference
- `findByDateRange(Instant startDate, Instant endDate, Pageable pageable)`: Date range filter
- `sumAmountByUserIdAndStatus(UUID userId, TransactionStatus status)`: Calculate totals
- `countByStatus(TransactionStatus status)`: Count by status
- And many more for comprehensive querying

### 5. Service

Located: `src/main/java/com/tunisia/financial/service/`

#### TransactionService Interface
Defines all transaction operations

#### TransactionServiceImpl Implementation
Located: `src/main/java/com/tunisia/financial/service/impl/TransactionServiceImpl.java`

**Key Features:**
- Transaction creation with validation
- Fraud detection scoring
- Balance checking for withdrawals
- Transaction limits by user role
- Status transition validation
- Comprehensive statistics calculation

### 6. Controller

Located: `src/main/java/com/tunisia/financial/controller/TransactionController.java`

**Base Path:** `/api/v1/transactions`

RESTful endpoints with:
- Pagination support
- Sorting capabilities
- Role-based access control
- Comprehensive Swagger documentation

### 7. Custom Exceptions

Located: `src/main/java/com/tunisia/financial/exception/transaction/`

- **InsufficientFundsException**: Thrown when user lacks funds for withdrawal
- **InvalidTransactionException**: Thrown for business rule violations
- **TransactionNotFoundException**: Thrown when transaction not found

All handled by `GlobalExceptionHandler` for consistent error responses.

---

## Database Schema

### Transaction Table

```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL CHECK (amount >= 0.01),
    user_id UUID NOT NULL REFERENCES users(id),
    description VARCHAR(500),
    fraud_score DOUBLE PRECISION,
    reference_number VARCHAR(50) UNIQUE,
    receipt VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_transaction_user ON transactions(user_id);
CREATE INDEX idx_transaction_status ON transactions(status);
CREATE INDEX idx_transaction_type ON transactions(type);
CREATE INDEX idx_transaction_created ON transactions(created_at);
```

---

## API Endpoints

### Public Endpoints
None - All transaction endpoints require authentication

### Authenticated User Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/v1/transactions` | Create new transaction | All authenticated users |
| GET | `/api/v1/transactions/{id}` | Get transaction by ID | Owner, ADMIN, AUDITOR, ANALYST |
| GET | `/api/v1/transactions/my-transactions` | Get my transactions | All authenticated users |
| GET | `/api/v1/transactions/reference/{refNum}` | Get by reference number | All authenticated users |
| POST | `/api/v1/transactions/{id}/cancel` | Cancel pending transaction | Owner or ADMIN |
| GET | `/api/v1/transactions/my-statistics` | Get my statistics | All authenticated users |

### Admin/Auditor/Analyst Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/v1/transactions` | Get all transactions | ADMIN, AUDITOR, ANALYST |
| GET | `/api/v1/transactions/user/{userId}` | Get user transactions | ADMIN, AUDITOR, ANALYST |
| GET | `/api/v1/transactions/status/{status}` | Filter by status | ADMIN, AUDITOR, ANALYST |
| GET | `/api/v1/transactions/type/{type}` | Filter by type | ADMIN, AUDITOR, ANALYST |
| GET | `/api/v1/transactions/date-range` | Filter by date range | ADMIN, AUDITOR, ANALYST |
| GET | `/api/v1/transactions/statistics` | Get overall statistics | ADMIN, AUDITOR, ANALYST |
| GET | `/api/v1/transactions/user/{userId}/statistics` | Get user statistics | ADMIN, AUDITOR, ANALYST |

### Admin/Analyst Only Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| PUT | `/api/v1/transactions/{id}/status?status={status}` | Update transaction status | ADMIN, ANALYST |

---

## Business Rules

### 1. Transaction Creation Rules

- **Minimum Amount**: 0.01 (enforced by validation)
- **Type Required**: Must specify PAYMENT, TRANSFER, WITHDRAWAL, or DEPOSIT
- **Initial Status**: All transactions start as PENDING (unless flagged for fraud)

### 2. Fraud Detection

The system calculates a fraud score (0.0 - 1.0) based on:
- **Large Transactions**: Amounts > 10,000 increase score by 0.3
- **High Frequency**: More than 10 transactions increase score by 0.2
- **Withdrawal Risk**: WITHDRAWAL type increases score by 0.15
- **Random Factor**: Small random component (0.0 - 0.1)

**Fraud Threshold**: 0.7
- If fraud score >= 0.7, transaction status set to `FRAUD_DETECTED`

### 3. Withdrawal Validation

- System checks user balance before allowing withdrawal
- Balance = Sum of completed deposits minus withdrawals
- Throws `InsufficientFundsException` if balance insufficient

### 4. Transaction Limits by Role

- **SME_USER**: Maximum transaction amount = 5,000
- **Others**: No specific limits (subject to fraud detection)

### 5. Status Transitions

Valid transitions:
- `PENDING` → `COMPLETED`
- `PENDING` → `FAILED`
- `PENDING` → `FRAUD_DETECTED`
- `FAILED` → `PENDING` (for retry)

Invalid transitions:
- Cannot change `COMPLETED` status
- Cannot change `FRAUD_DETECTED` status
- `FAILED` can only transition to `PENDING`

### 6. Cancellation Rules

- Only `PENDING` transactions can be cancelled
- Users can cancel their own transactions
- ADMINs can cancel any transaction
- Cancelled transactions are set to `FAILED` status

---

## Security & Roles

### Role Hierarchy

1. **ADMIN**: Full access to all transaction operations
2. **FINANCIAL_ANALYST**: Can view all transactions and update statuses
3. **AUDITOR**: Can view all transactions (read-only)
4. **SME_USER**: Limited transaction amounts (max 5,000)

### Access Control Matrix

| Operation | USER/SME | ANALYST | AUDITOR | ADMIN |
|-----------|----------|---------|---------|-------|
| Create transaction | ✓ | ✓ | ✓ | ✓ |
| View own transactions | ✓ | ✓ | ✓ | ✓ |
| View all transactions | ✗ | ✓ | ✓ | ✓ |
| Update status | ✗ | ✓ | ✗ | ✓ |
| Cancel own transaction | ✓ | ✓ | ✓ | ✓ |
| Cancel any transaction | ✗ | ✗ | ✗ | ✓ |
| View statistics | Own only | All | All | All |

---

## Usage Examples

### 1. Create a Transaction

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "PAYMENT",
    "amount": 150.50,
    "description": "Monthly subscription payment"
  }'
```

**Response:**
```json
{
  "id": 1,
  "type": "PAYMENT",
  "status": "PENDING",
  "amount": 150.50,
  "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "userEmail": "user@example.com",
  "description": "Monthly subscription payment",
  "fraudScore": 0.15,
  "referenceNumber": "TXN1737296855123-4567",
  "receipt": "RCP-A1B2C3D4",
  "createdAt": "2026-01-19T14:45:55.123Z",
  "updatedAt": "2026-01-19T14:45:55.123Z"
}
```

### 2. Get My Transactions

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/transactions/my-transactions?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "type": "PAYMENT",
      "status": "COMPLETED",
      "amount": 150.50,
      "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "userEmail": "user@example.com",
      "description": "Monthly subscription payment",
      "fraudScore": 0.15,
      "referenceNumber": "TXN1737296855123-4567",
      "receipt": "RCP-A1B2C3D4",
      "createdAt": "2026-01-19T14:45:55.123Z",
      "updatedAt": "2026-01-19T14:47:30.456Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1
}
```

### 3. Filter by Status (Admin)

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/transactions/status/FRAUD_DETECTED?page=0&size=10" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

### 4. Get Transaction Statistics

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/transactions/statistics \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

**Response:**
```json
{
  "totalTransactions": 150,
  "pendingTransactions": 25,
  "completedTransactions": 100,
  "failedTransactions": 20,
  "fraudDetectedTransactions": 5,
  "totalAmount": 125000.00,
  "averageAmount": 1250.00,
  "paymentCount": 80,
  "transferCount": 40,
  "withdrawalCount": 20,
  "depositCount": 10
}
```

### 5. Update Transaction Status (Admin/Analyst)

**Request:**
```bash
curl -X PUT "http://localhost:8080/api/v1/transactions/1/status?status=COMPLETED" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

### 6. Cancel Transaction

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/transactions/1/cancel \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 7. Filter by Date Range (Admin)

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/transactions/date-range?startDate=2026-01-01T00:00:00Z&endDate=2026-01-31T23:59:59Z&page=0&size=20" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

---

## Testing

### Testing with Swagger UI

1. **Access Swagger**: http://localhost:8080/swagger-ui.html
2. **Authorize**: Click "Authorize" and enter `Bearer YOUR_JWT_TOKEN`
3. **Navigate to Transaction Management**: Find the "Transaction Management" section
4. **Try Operations**: Execute various transaction operations

### Test Scenarios

#### Scenario 1: Normal Transaction Flow
1. Login as `sme1@financial.tn` (Password: `Sme123!`)
2. Create a PAYMENT transaction for 500.00
3. Verify transaction is PENDING with low fraud score
4. Login as admin1@financial.tn (Password: `Admin123!`)
5. Update transaction status to COMPLETED

#### Scenario 2: Fraud Detection
1. Login as any user
2. Create a WITHDRAWAL transaction for 15,000.00
3. Verify fraud score is high (>= 0.7)
4. Verify status is automatically set to FRAUD_DETECTED

#### Scenario 3: Insufficient Funds
1. Login as new user with no transactions
2. Attempt WITHDRAWAL of 100.00
3. Verify `InsufficientFundsException` is thrown

#### Scenario 4: Transaction Limits
1. Login as `sme1@financial.tn`
2. Attempt PAYMENT of 6,000.00
3. Verify `InvalidTransactionException` is thrown (SME limit is 5,000)

#### Scenario 5: Role-Based Access
1. Login as `analyst1@financial.tn` (Password: `Analyst123!`)
2. View all transactions (should succeed)
3. Update transaction status (should succeed)
4. Login as `auditor1@financial.tn` (Password: `Auditor123!`)
5. View all transactions (should succeed)
6. Attempt to update transaction status (should fail with 403)

#### Scenario 6: Pagination and Filtering
1. Login as admin
2. Create 25 transactions with different types and statuses
3. Test pagination: Get page 0, size 10
4. Filter by type: PAYMENT
5. Filter by status: COMPLETED
6. Filter by date range: Last 7 days

### Automated Testing

Run the test script:
```bash
./test-split-controllers.sh
```

This will test:
- Authentication endpoints
- Transaction creation
- Transaction retrieval
- Role-based access control
- Statistics endpoints

---

## Error Handling

### Common Errors

| Status Code | Error | Description |
|-------------|-------|-------------|
| 400 | InvalidTransactionException | Business rule violation |
| 400 | InsufficientFundsException | Not enough funds |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | AccessDeniedException | Insufficient permissions |
| 404 | TransactionNotFoundException | Transaction not found |
| 500 | Internal Server Error | Unexpected error |

### Error Response Format

```json
{
  "status": 400,
  "error": "Invalid transaction",
  "message": "Transaction amount exceeds limit for SME users: 5000",
  "timestamp": "2026-01-19T14:45:55.123Z"
}
```

---

## Performance Considerations

### Database Indexes
- Transactions are indexed on `user_id`, `status`, `type`, and `created_at`
- These indexes optimize common queries and filtering operations

### Pagination
- Always use pagination for large result sets
- Default page size: 10
- Maximum recommended page size: 100

### Caching
- Consider caching frequently accessed statistics
- Cache invalidation on transaction status changes

---

## Future Enhancements

1. **Real-time Notifications**: Notify users when transaction status changes
2. **Advanced Fraud Detection**: Machine learning model for fraud scoring
3. **Transaction Templates**: Save and reuse common transactions
4. **Batch Processing**: Process multiple transactions at once
5. **Transaction Reversal**: Ability to reverse completed transactions
6. **Scheduled Transactions**: Support for recurring/scheduled transactions
7. **Multi-currency Support**: Handle different currencies
8. **Transaction Attachments**: Allow users to upload receipts/documents

---

## Troubleshooting

### Issue: Transaction creation fails with 400
**Cause**: Validation error or business rule violation
**Solution**: Check error message for specific validation failure

### Issue: Cannot view other users' transactions
**Cause**: Insufficient role permissions
**Solution**: Only ADMIN, AUDITOR, and FINANCIAL_ANALYST can view all transactions

### Issue: Fraud score always high
**Cause**: Large amount or high transaction frequency
**Solution**: This is expected behavior. Review fraud detection rules.

### Issue: Cannot update transaction status
**Cause**: Invalid status transition
**Solution**: Review status transition rules. Only certain transitions are allowed.

---

## Support

For issues or questions:
1. Check Swagger documentation: http://localhost:8080/swagger-ui.html
2. Review application logs: `docker logs financial-app`
3. Check this README for business rules and examples

---

## Changelog

### Version 1.0.0 (2026-01-19)
- Initial implementation of transaction management feature
- Support for 4 transaction types: PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT
- Support for 4 transaction statuses: PENDING, COMPLETED, FAILED, FRAUD_DETECTED
- Fraud detection with configurable threshold
- Role-based access control
- Comprehensive filtering and pagination
- Transaction statistics
- Full Swagger API documentation
