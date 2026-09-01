# Secure RESTful Resource Booking System

A production-quality Spring Boot backend for booking shared resources (conference
rooms, vehicles, equipment) with JWT authentication and role-based access control.

## Project Overview

Users authenticate with email/password and receive a JWT. Admins manage the catalog
of bookable resources; both Admins and Users can create reservations. A User can only
ever see, edit, or cancel their **own** reservations — ownership is always derived
from the authenticated JWT identity, never from client-supplied input. Admins have
full visibility and control over all resources and reservations.

## Features

- JWT-based stateless authentication (BCrypt password hashing)
- Role-based access control (`ADMIN`, `USER`) enforced via `@PreAuthorize`
- Resource CRUD (Admin-managed catalog)
- Reservation CRUD with strict ownership security
- Filtering by status, min price, max price
- Pagination with sane defaults and metadata
- Sorting with a server-side field allowlist
- Bean Validation on all request DTOs + custom business-rule validation
- Centralized exception handling with a consistent error contract
- Overlap detection for reservations (409 Conflict)
- Swagger / OpenAPI UI with JWT bearer auth support
- Unit + integration tests (JUnit 5, Mockito, Spring Boot Test, H2)

## Technology Stack

| Layer          | Technology                          |
|----------------|--------------------------------------|
| Language       | Java 17                              |
| Framework      | Spring Boot 3.3.x                    |
| Security       | Spring Security + JWT (jjwt 0.12.x)  |
| Persistence    | Spring Data JPA / Hibernate          |
| Database       | MySQL 8+ (H2 for tests)              |
| Docs           | springdoc-openapi (Swagger UI)       |
| Build          | Maven                                |
| Testing        | JUnit 5, Mockito, Spring Boot Test   |
| Boilerplate    | Lombok                               |

## Project Structure

```text
src/main/java/com/example/booking
├── config            OpenApiConfig, DataSeeder
├── controller        AuthController, ResourceController, ReservationController
├── dto/request       LoginRequest, ResourceRequest, ReservationRequest
├── dto/response      LoginResponse, ResourceResponse, ReservationResponse,
│                     PagedResponse, ErrorResponse
├── entity            User, Resource, Reservation
├── enums             Role, ReservationStatus
├── exception         GlobalExceptionHandler + custom exceptions
├── repository        UserRepository, ResourceRepository, ReservationRepository,
│                     ReservationSpecifications
├── security          JwtService, JwtAuthenticationFilter,
│                     CustomUserDetailsService, SecurityConfig, SecurityUtils
├── service           Interfaces
├── service/impl      Implementations
├── util              PageableFactory (pagination + sort allowlist)
└── ResourceBookingApplication.java
```

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+ (running locally or reachable over the network)

## Database Setup

```sql
CREATE DATABASE resource_booking_db;
```

A ready-to-run script is provided at `database/create_database.sql`. Hibernate
(`spring.jpa.hibernate.ddl-auto=update`) creates/updates the tables automatically
on first startup — no manual migrations are required.

## Environment Variables

| Variable         | Description                              | Default                  |
|------------------|-------------------------------------------|---------------------------|
| `DB_HOST`        | MySQL host                                | `localhost`               |
| `DB_PORT`        | MySQL port                                | `3306`                    |
| `DB_NAME`        | Database name                             | `resource_booking_db`     |
| `DB_USERNAME`    | MySQL username                            | `root`                    |
| `DB_PASSWORD`    | MySQL password                            | *(empty)*                 |
| `JWT_SECRET`     | HMAC signing key for JWTs (256-bit+)      | dev default (override in prod) |
| `JWT_EXPIRATION` | Token lifetime in milliseconds            | `86400000` (24h)          |
| `SERVER_PORT`    | HTTP port                                 | `8080`                    |

**Never use the default `JWT_SECRET` in production** — set a strong, random value
via environment variable.

## Running the Application

```bash
mvn clean install
mvn spring-boot:run
```

Or run the packaged jar:

```bash
java -jar target/resource-booking-system.jar
```

On first startup, `DataSeeder` creates baseline accounts and sample resources
(idempotent — safe to restart).

## Seed Users

| Role  | Email               | Password    |
|-------|---------------------|-------------|
| ADMIN | admin@example.com   | Admin@123   |
| USER  | user@example.com    | User@123    |

## API Documentation (Swagger)

```text
http://localhost:8080/swagger-ui/index.html
```

## Authentication Instructions

1. `POST /auth/login` with email/password to receive a JWT.
2. Copy the `token` value from the response.
3. In Swagger UI, click **Authorize** and enter: `Bearer <JWT_TOKEN>`.
4. Call any protected endpoint — the token is sent automatically.

Example login request:

```json
POST /auth/login
{
  "email": "admin@example.com",
  "password": "Admin@123"
}
```

Example response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "email": "admin@example.com",
  "role": "ADMIN"
}
```

## API Endpoints

### Authentication

| Method | Endpoint       | Access | Description        |
|--------|----------------|--------|---------------------|
| POST   | `/auth/login`  | Public | Authenticate, get JWT |

### Resources

| Method | Endpoint              | Access      | Description                  |
|--------|-----------------------|-------------|-------------------------------|
| POST   | `/resources`          | ADMIN       | Create a resource             |
| GET    | `/resources`          | ADMIN, USER | List resources (paginated)    |
| GET    | `/resources/{id}`     | ADMIN, USER | Get a resource by id          |
| PUT    | `/resources/{id}`     | ADMIN       | Update a resource             |
| DELETE | `/resources/{id}`     | ADMIN       | Delete a resource             |

### Reservations

| Method | Endpoint                 | Access      | Description                                   |
|--------|--------------------------|-------------|-------------------------------------------------|
| POST   | `/reservations`          | ADMIN, USER | Create a reservation (owner = JWT identity)    |
| GET    | `/reservations`          | ADMIN, USER | ADMIN: all reservations. USER: own only.       |
| GET    | `/reservations/{id}`     | ADMIN, USER | ADMIN: any. USER: own only (else 403).         |
| PUT    | `/reservations/{id}`     | ADMIN, USER | ADMIN: any. USER: own only (else 403).         |
| DELETE | `/reservations/{id}`     | ADMIN, USER | ADMIN: any. USER: own only (else 403).         |

### Query Parameters (GET /reservations, GET /resources)

```text
page=0&size=10
sortBy=price&direction=desc      (allowlist: startTime, endTime, createdAt,
                                   price, status, name, updatedAt, id)
status=PENDING|CONFIRMED|CANCELLED
minPrice=100&maxPrice=1000
```

## Security Notes

- Reservation ownership is **always** resolved from `SecurityContextHolder` —
  the client can never assign a reservation to another user, even by supplying
  a `userId` field (the DTO does not expose one, and any extra field in the
  request body is simply ignored).
- Passwords are BCrypt-hashed and never returned in any API response.
- All protected endpoints require a valid `Bearer` JWT; missing/invalid tokens
  return `401`, and authenticated-but-unauthorized requests return `403`.
- Overlapping reservations for the same resource return `409 Conflict`.
  Cancelled reservations do not block availability.

## Running Tests

```bash
mvn test
```

Tests run against an in-memory H2 database (`application-test.properties`) and
cover authentication, role-based authorization, reservation ownership security,
input validation, and core business rules (overlap detection, not-found
handling, pagination).
