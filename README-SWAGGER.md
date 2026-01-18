# Swagger/OpenAPI Documentation

## Access Swagger UI

Once the application is running, access the interactive API documentation at:

**Swagger UI**: http://localhost:8080/swagger-ui.html

**OpenAPI JSON**: http://localhost:8080/api-docs

## Features

### Interactive API Testing
- Test all API endpoints directly from the browser
- View request/response schemas
- See example payloads
- Execute real API calls

### Authentication
Most endpoints require JWT authentication:
1. First, register a user via `POST /api/v1/users/register`
2. Login via `POST /api/v1/users/login` to get a JWT token
3. Click "Authorize" button in Swagger UI
4. Enter: `Bearer YOUR_JWT_TOKEN`
5. Now you can test protected endpoints

### Available API Groups

#### User Management
- **POST** `/api/v1/users/register` - Register new user
- **POST** `/api/v1/users/login` - User authentication
- **GET** `/api/v1/users/me` - Get current user profile
- **GET** `/api/v1/users/{userId}` - Get user by ID
- **PUT** `/api/v1/users/{userId}` - Update user
- **POST** `/api/v1/users/{userId}/change-password` - Change password
- **POST** `/api/v1/users/{userId}/lock` - Lock account (Admin)
- **POST** `/api/v1/users/{userId}/unlock` - Unlock account (Admin)
- **DELETE** `/api/v1/users/{userId}` - Delete user (Admin)
- **GET** `/api/v1/users` - List all users (Admin/Auditor)
- **GET** `/api/v1/users/by-role/{role}` - Filter by role (Admin/Auditor)
- **GET** `/api/v1/users/search` - Search users (Admin/Auditor)
- **GET** `/api/v1/users/statistics` - User statistics (Admin/Auditor)

## Quick Start Example

### 1. Register a User
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@financial.com",
    "password": "Admin@123",
    "firstName": "Admin",
    "lastName": "User",
    "role": "ADMIN"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@financial.com",
    "password": "Admin@123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_token_...",
  "userId": "uuid-here",
  "email": "admin@financial.com",
  "role": "ADMIN",
  "expiresIn": 3600
}
```

### 3. Use Token for Protected Endpoints
```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## User Roles

- **ADMIN**: Full system access, user management
- **FINANCIAL_ANALYST**: Fraud analysis, risk assessment, reporting
- **SME_USER**: SME business owners (view own data)
- **AUDITOR**: Read-only access for compliance

## Configuration

Swagger settings can be customized in `application.properties`:

```properties
# Swagger/OpenAPI Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.show-actuator=true
```

## Disabling Swagger in Production

To disable Swagger in production, set:

```properties
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

Or use environment variables:
```bash
SPRINGDOC_SWAGGER_UI_ENABLED=false
SPRINGDOC_API_DOCS_ENABLED=false
```

## Additional Resources

- [SpringDoc Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
