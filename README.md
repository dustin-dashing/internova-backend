# Internova Core Backend

Internova is a distributed, multi-tenant recruitment and academic compliance platform designed to bridge the gap between universities, students, and corporate partners.

This repository houses the Core Orchestrator, built with Java Spring Boot. It manages the business domain, state transitions, organizational hierarchies, and asynchronous communication with the AI-driven microservice (`internova-brain`).

## Architecture & Features

- Multi-Tenant Hierarchy: Robust data model supporting Universities -> Faculties -> Departments.
- Role-Based Access Control (RBAC): Stateless JWT authentication via HttpOnly cookies for Students, Company Representatives, Supervisors, and Admins.
- Compliance Engine: Enforces a strict 48-hour immutable rule for student logbook entries.
- State Machine: Manages the vacancy application lifecycle (Applied -> Interview -> Accepted/Rejected).
- Nudge Engine: Background scheduled jobs (Cron) that detect and alert companies of ghosted student applications.
- Zero-Trust Webhooks: Uses shared-secret filtering to securely receive asynchronous callbacks from the AI microservice.
- Resilience: Implements `@Retryable` exponential backoff for inter-service communication.

## Tech Stack

- Framework: Java 21, Spring Boot 4.x
- Security: Spring Security, JWT (HttpOnly Cookies)
- Persistence: Spring Data JPA, Hibernate, PostgreSQL
- Migrations: Flyway
- Integration: Spring RestClient, Spring Retry, Spring AOP
- Storage: Extensible `StorageService` interface (currently implemented for local fast-I/O).

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+

### Setup

1. Database Setup: Create a PostgreSQL database named `internova_db`.
2. Environment Variables / Properties: Configure the following values in `application.properties` (or env overrides):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/internova_db
spring.datasource.username=your_user
spring.datasource.password=your_password
internova.security.jwt-secret=your_super_secret_key_here
internova.brain.url=http://localhost:8000
internova.webhook.secret=super-secret-internal-brain-token-2026
internova.storage.public-base-url=http://localhost:8080
```

### Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

Flyway will automatically execute all migration scripts (`V1` through `V5`) on startup.
