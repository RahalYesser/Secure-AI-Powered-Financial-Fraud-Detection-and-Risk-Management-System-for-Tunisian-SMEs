# Docker Deployment Guide

## Prerequisites

- Docker Desktop or Docker Engine (20.10+)
- Docker Compose (2.0+)
- At least 4GB of available RAM

## Quick Start

### 1. Build and Run All Services

```bash
# Build and start all containers
docker-compose up -d

# View logs
docker-compose logs -f app

# Check container status
docker-compose ps
```

### 2. Access the Application

- **API**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **PgAdmin**: http://localhost:5050
  - Email: admin@financial.com
  - Password: admin

### 3. Stop the Application

```bash
# Stop all containers
docker-compose down

# Stop and remove volumes (WARNING: This deletes all data)
docker-compose down -v
```

## Service Details

### PostgreSQL Database
- **Port**: 5444
- **Database**: financial_fraud_detection
- **Username**: postgres
- **Password**: postgres

### Spring Boot Application
- **Port**: 8080
- **Auto-restart**: Enabled
- **Health Check**: Automatic

### PgAdmin (Database Management)
- **Port**: 5050
- **Login**: admin@financial.com / admin

## Development Workflow

### Rebuild After Code Changes

```bash
# Rebuild and restart the application
docker-compose up -d --build app

# Or rebuild everything
docker-compose up -d --build
```

### View Application Logs

```bash
# Follow application logs
docker-compose logs -f app

# Follow database logs
docker-compose logs -f postgres

# View all logs
docker-compose logs -f
```

### Execute Commands Inside Container

```bash
# Access Spring Boot container shell
docker exec -it financial-app sh

# Access PostgreSQL container
docker exec -it financial-postgres psql -U postgres -d financial_fraud_detection
```

## Database Management

### Connect to PostgreSQL

Using PgAdmin:
1. Open http://localhost:5050
2. Login with credentials
3. Add Server:
   - Name: Financial DB
   - Host: postgres
   - Port: 5432
   - Username: postgres
   - Password: postgres

Using psql:
```bash
docker exec -it financial-postgres psql -U postgres -d financial_fraud_detection
```

### Backup Database

```bash
# Create backup
docker exec financial-postgres pg_dump -U postgres financial_fraud_detection > backup.sql

# Restore backup
docker exec -i financial-postgres psql -U postgres financial_fraud_detection < backup.sql
```

## Environment Variables

You can customize the application by editing `docker-compose.yml`:

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/financial_fraud_detection
  JWT_SECRET: YourSecureJWTSecretKey
  LOGGING_LEVEL_COM_TUNISIA_FINANCIAL: DEBUG
```

## Troubleshooting

### Container won't start
```bash
# Check logs
docker-compose logs app

# Restart specific service
docker-compose restart app
```

### Database connection issues
```bash
# Check if database is healthy
docker-compose ps postgres

# Check database logs
docker-compose logs postgres
```

### Port already in use
```bash
# Change ports in docker-compose.yml
ports:
  - "8081:8080"  # Change host port
```

### Clean start (remove all data)
```bash
docker-compose down -v
docker-compose up -d --build
```

## Production Considerations

1. **Change Default Passwords**: Update all passwords in `docker-compose.yml`
2. **Use Secrets**: Store sensitive data in Docker secrets or environment files
3. **Enable SSL/TLS**: Configure HTTPS for the application
4. **Resource Limits**: Add memory and CPU limits to services
5. **Monitoring**: Add Prometheus and Grafana for monitoring
6. **Backup Strategy**: Implement automated database backups

## Testing the API

### Register a User
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

### Login
```bash
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@financial.com",
    "password": "Admin@123"
  }'
```

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)
