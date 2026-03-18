# Internova Backend - Development Log

## Overview
This document tracks all commits, bugs encountered, and solutions implemented for the `Internova-backend` repository. Each entry captures rationale, implementation details, and verification notes.

---

## Commits

### [2026-03-11] Module 10.4: Integration Resilience & Hardening - Production Bridge Safety
- **Commit**: `7b5edae`
- **Status**: ✅ Complete & Pushed
- **Files Updated**:
  - `src/main/java/com/internova/core/service/impl/LocalStorageService.java` - Deterministic routable URLs + strict PDF validation
  - `src/main/java/com/internova/integration/brain/client/BrainClient.java` - Async trigger with retry/backoff and recovery logging
  - `src/main/java/com/internova/ApiApplication.java` - Enabled async and retry infrastructure
  - `pom.xml` - Added AOP and retry dependencies for method-level resilience
  - `src/main/resources/application.properties` - Added `internova.storage.public-base-url`
- **Storage Hardening (LocalStorageService)**:
  - Added MIME type enforcement: only `application/pdf` is accepted
  - Empty uploads now rejected with `IllegalArgumentException`
  - Forced stored filename format to `<uuid>.pdf` to avoid extension spoofing
  - Added explicit trailing-slash-safe `publicBaseUrl` normalization
  - URL generation no longer depends on incoming request Host header
  - Resulting URL format: `{internova.storage.public-base-url}/uploads/{prefix}/{uuid}.pdf`
  - Security check retained to block directory traversal and out-of-root writes
- **Network Namespace Fix**:
  - Replaced `ServletUriComponentsBuilder.fromCurrentContextPath()` behavior with externalized base URL
  - Prevents Docker namespace mismatch (`localhost` in one container != host service endpoint)
  - Enables environment-specific routing for Python brain accessibility
- **AI Trigger Decoupling (BrainClient)**:
  - Added `@Async` to `triggerResumeParse(...)` so upload response is no longer blocked by Python availability
  - Added `@Retryable` on `ResourceAccessException` with exponential backoff:
    - max attempts: 3
    - delay: 2s, then 4s, then 8s
  - Added `@Recover` fallback to log persistent delivery failure after retries
  - Outcome: student gets `202 Accepted` even if Python service is temporarily unavailable
- **App Bootstrap Changes**:
  - Added `@EnableAsync` and `@EnableRetry` to `ApiApplication`
  - Activates asynchronous execution and retry proxies globally
- **Dependency Adjustments**:
  - Added `org.springframework:spring-aop` (required for retry proxying)
  - Added `org.springframework.retry:spring-retry:2.0.12`
  - Note: `spring-boot-starter-aop` artifact for this setup was unavailable; switched to `spring-aop` directly and verified compile success
- **Configuration**:
  - Added `internova.storage.public-base-url=http://localhost:8080`
  - Supports future override for compose/service DNS (e.g., `http://internova-backend:8080`)
- **Operational Impact**:
  - Upload path is now resilient and deterministic:
    1. file validated (PDF only)
    2. file stored and routable URL returned
    3. AI trigger dispatched asynchronously
    4. temporary Python outage no longer breaks student upload transaction

### [2026-03-10] Module 10.3: Document Storage & The AI Trigger - Resume Upload Bridge
- **Commit**: `d6ba247`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/core/service/StorageService.java` - Storage abstraction for swappable file backends
  - `src/main/java/com/internova/core/service/impl/LocalStorageService.java` - Local filesystem implementation for development
  - `src/main/java/com/internova/config/StorageWebConfig.java` - Static resource mapping for uploaded files
  - `src/main/java/com/internova/modules/student/controller/StudentProfileController.java` - Student resume upload endpoint and AI trigger
- **Files Updated**:
  - `src/main/java/com/internova/config/SecurityConfig.java` - Allowed `/uploads/**` for Python file retrieval
  - `src/main/resources/application.properties` - Added `internova.storage.upload-dir=uploads`
- **StorageService Abstraction**:
  - Introduced `StorageService` interface with `store(MultipartFile file, String pathPrefix)`
  - Design keeps current local-disk implementation replaceable by S3/object storage later without changing controllers
- **LocalStorageService**:
  - Stores files under configurable root directory (`internova.storage.upload-dir`, default `uploads`)
  - Creates upload directory on startup if missing
  - Uses UUID-prefixed filenames to avoid collisions
  - Creates subdirectories from `pathPrefix` (current use: `resumes`)
  - Normalizes and validates storage paths to avoid path traversal outside upload root
  - Returns URL in the form `/uploads/resumes/<generated-filename>` using `ServletUriComponentsBuilder`
- **Static File Exposure**:
  - `StorageWebConfig` maps `/uploads/**` to the filesystem upload directory via `WebMvcConfigurer`
  - This is required so the Python brain can dereference the returned `cv_url` over HTTP
- **StudentProfileController**:
  - Endpoint: `POST /api/v1/students/me/resume`
  - Security: `@PreAuthorize("hasRole('STUDENT')")`
  - Accepts `MultipartFile` as request param `file`
  - Flow:
    1. Store file via `StorageService`
    2. Persist generated `cvUrl` onto authenticated `Student`
    3. Trigger async AI parsing through `BrainClient.triggerResumeParse(...)`
    4. Return `202 Accepted` with upload message and `cv_url`
  - This closes the user-facing loop: upload -> storage -> Python parse -> Java webhook callback
- **Security Adjustment**:
  - Added `/uploads/**` to Spring Security permit list
  - Without this, Python would receive a file URL that resolves to `403 Forbidden`
  - User-auth remains required for the upload action itself; only the file retrieval URL is public/internal-accessible
- **Configuration**:
  - Added `internova.storage.upload-dir=uploads`
  - Keeps storage root externalized for local/dev/prod overrides or future mounted volumes
- **Architectural Outcome**:
  - The full AI ingestion loop is now operational on the Java side:
    - student uploads resume
    - file is stored and exposed
    - Java triggers Python parse job
    - Python can fetch the file from Java
    - Python later calls secure webhook back to Java

### [2026-03-10] Module 10.2: Service-to-Service Security - Zero Trust Webhook Bridge
- **Commit**: `6074c72`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/integration/brain/security/WebhookSecurityFilter.java` - Shared-secret filter for internal webhook calls
- **Files Updated**:
  - `src/main/java/com/internova/config/SecurityConfig.java` - Injected and registered webhook security filter
  - `src/main/resources/application.properties` - Added `internova.webhook.secret`
- **WebhookSecurityFilter**:
  - Extends `OncePerRequestFilter`
  - Loads secret from `internova.webhook.secret`
  - Intercepts requests under `/api/v1/webhooks/`
  - Reads `X-Webhook-Secret` header and rejects missing or invalid values with `403 Forbidden`
  - Uses `MessageDigest.isEqual(...)` for safer constant-time style secret comparison instead of plain `String.equals(...)`
  - Fails fast before controller logic or database work executes
- **Security Chain Wiring**:
  - `SecurityConfig` now injects `WebhookSecurityFilter`
  - Filter registered with `.addFilterBefore(webhookSecurityFilter, UsernamePasswordAuthenticationFilter.class)`
  - Webhook route remains `permitAll()` for user-auth purposes, but it is no longer open because machine-auth is enforced by the dedicated filter
  - Existing JWT filter remains in place for user-facing traffic
- **Configuration**:
  - Added `internova.webhook.secret=super-secret-internal-brain-token-2026`
  - Property externalizes the shared secret so local/dev/prod values can differ without code changes
  - Production recommendation: replace with a long random secret sourced from environment variables or a secret manager
- **Why This Matters**:
  - Eliminates spoofed callback risk where attackers could fake AI completion payloads
  - Enforces Zero Trust between internal services instead of assuming network secrecy
  - Preserves the async webhook architecture from Module 10.1 while making it production-credible
- **Bug Encountered & Fixed During Build**:
  - Initial logger call in `WebhookSecurityFilter` used SLF4J-style `{}` placeholders
  - `OncePerRequestFilter` exposes a standard logger, so compilation failed
  - Fixed by switching to string concatenation and re-running compile successfully

### [2026-03-10] Module 10.1: Internal API Integration - Java/Python Bridge
- **Commit**: `c75667d`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/integration/brain/dto/SkillSetDto.java` - Java contract for parsed skill groups
  - `src/main/java/com/internova/integration/brain/dto/ResumeParseResponseDto.java` - Webhook payload contract for resume parsing results
  - `src/main/java/com/internova/integration/brain/dto/ResumeParseRequest.java` - Request contract for async resume parsing trigger
  - `src/main/java/com/internova/integration/brain/dto/LogbookEntryPayload.java` - Request contract for logbook analysis calls
  - `src/main/java/com/internova/integration/brain/dto/LogbookAnalysisResponseDto.java` - Response contract for synchronous logbook analysis
  - `src/main/java/com/internova/integration/brain/client/BrainClient.java` - Spring `RestClient` wrapper for Python service calls
  - `src/main/java/com/internova/integration/brain/controller/BrainWebhookController.java` - Webhook callback receiver for async AI results
  - `src/main/java/com/internova/modules/student/repository/StudentRepository.java` - Student persistence seam used by webhook updates
- **Files Updated**:
  - `src/main/java/com/internova/config/SecurityConfig.java` - Allowed webhook callback route without JWT
  - `src/main/resources/application.properties` - Added `internova.brain.url=http://localhost:8000`
- **Integration DTO Layer**:
  - Used Java records for lightweight, immutable schema contracts
  - `SkillSetDto` mirrors grouped skills (`hardSkills`, `softSkills`, `tools`)
  - `ResumeParseResponseDto` models Python callback payload including `profileCompletionSuggestion`
  - Added request/response DTOs needed by the Java client so `BrainClient` compiles cleanly and keeps transport contracts explicit
- **BrainClient**:
  - Uses Spring's modern `RestClient` with configurable base URL from `internova.brain.url`
  - `analyzeLogbook(String content)` performs synchronous POST to `/api/logbook/analyze`
  - `triggerResumeParse(String studentId, String fileUrl)` performs async trigger POST to `/api/resume/parse`
  - Design: hides raw HTTP details behind a service boundary so future retries, auth headers, or observability can be added centrally
- **BrainWebhookController**:
  - Endpoint: `POST /api/v1/webhooks/brain/resume-parsed/{studentId}`
  - Accepts parsed resume callback payload from Python service
  - Resolves `Student` by UUID and updates `profileCompletion` using the AI suggestion when present
  - Logs email mismatch warnings if callback payload email does not match stored student email
  - Returns `200 OK` after successful ingestion
- **Security Fix Required For Webhooks**:
  - Existing security only permitted `/api/v1/auth/**`, so the webhook would have been blocked with `403 Forbidden`
  - Added `/api/v1/webhooks/brain/**` to permit-all matchers so internal callbacks can reach Java without a browser JWT cookie
  - This makes the integration operational immediately; shared-secret hardening can be added later if needed
- **Configuration**:
  - Added `internova.brain.url=http://localhost:8000` to `application.properties`
  - Keeps Python service location externalized for local/dev/prod environment overrides
- **Architectural Outcome**:
  - Java backend now has both halves of the bridge:
    - outbound client calls into Python
    - inbound webhook receiver for async AI completion
  - Resume parsing can now run as background work without blocking Java request threads

### [2026-03-10] Module 6.3.7: Notification & Audit Engine - Ghosting Alert Automation
- **Commit**: `b244c65`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/notification/model/Notification.java` - Internal notification entity
  - `src/main/java/com/internova/modules/notification/repository/NotificationRepository.java` - Notification persistence repository
  - `src/main/java/com/internova/modules/notification/service/NotificationService.java` - Notification creation helper service
  - `src/main/java/com/internova/modules/notification/service/NudgeService.java` - Scheduled ghosting checker
  - `src/main/resources/db/migration/V5__Add_Notifications_Table.sql` - Notifications schema migration
- **Files Updated**:
  - `src/main/java/com/internova/ApiApplication.java` - Added `@EnableScheduling`
  - `src/main/java/com/internova/modules/application/repository/ApplicationRepository.java` - Added stale-application finder query
- **Notification Entity**:
  - Table: `notifications`
  - Recipient Link: `@ManyToOne(fetch = LAZY)` to `User` via `recipient_id`
  - Message Shape: `title` + `message` (`TEXT`) for future email/SMS adapter reuse
  - Read Tracking: `isRead` default `false`
  - Audit Field: `createdAt` (`@CreationTimestamp`)
- **NotificationService**:
  - Method: `send(User recipient, String title, String message)`
  - Responsibility: Centralized persistence of internal notifications
  - Benefit: Clean seam for future transport adapters (SendGrid/Twilio) without changing business logic callers
- **NudgeService (Scheduled Ghosting Monitor)**:
  - Cron: `@Scheduled(cron = "0 0 0 * * *")` (runs daily at midnight)
  - Query Window: `LocalDateTime.now().minusDays(7)`
  - Source Set: applications still in `APPLIED` state with `updatedAt` before one week ago
  - Action: sends company notification
    - Title: `Action Required: Pending Application`
    - Message includes waiting student's email for quick triage
  - Transactional execution added to ensure batch notification writes are atomic
- **ApplicationRepository Enhancements**:
  - Added `findAllByStatusAndUpdatedAtBefore(ApplicationStatus status, LocalDateTime timestamp)`
  - Added explicit `ApplicationStatus` import and cleaned method signature usage
  - Outcome: Scheduler can pull stale applications with a single derived query
- **Scheduling Enablement**:
  - `@EnableScheduling` added to `ApiApplication`
  - Outcome: Spring scheduler infrastructure active at startup with no extra XML/config
- **Database Schema (V5 Migration)**:
  - New table: `notifications`
  - PK: `id UUID DEFAULT gen_random_uuid()`
  - FK: `recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`
  - Columns: `title`, `message`, `is_read`, `created_at`
  - Indexes:
    - `idx_notifications_recipient_created_at` for inbox listing by newest first
    - `idx_notifications_recipient_is_read` for unread/read filtering
- **Business Outcome**:
  - Closes the "Black Hole" gap by automatically nudging companies on stale applications
  - Produces an auditable trail of platform interventions
  - Keeps monitoring logic isolated from application lifecycle transitions

### [2026-03-10] Module 6.3.6: Application Lifecycle & Ghosting Prevention - State Machine Core
- **Commit**: `504bb28`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/application/enums/ApplicationStatus.java` - Formal application lifecycle states
  - `src/main/java/com/internova/modules/application/model/Application.java` - Student-to-vacancy association entity with unique constraint
  - `src/main/java/com/internova/modules/application/repository/ApplicationRepository.java` - Lifecycle guardrail query methods
  - `src/main/java/com/internova/modules/application/service/ApplicationService.java` - Apply/status transition business logic
  - `src/main/resources/db/migration/V4__Add_Applications_Table.sql` - Schema migration for lifecycle tracking
- **ApplicationStatus Enum**:
  - States: `APPLIED`, `SHORTLISTED`, `INTERVIEWING`, `OFFERED`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`
  - Purpose: Explicit workflow modeling for candidate progression and anti-ghosting analytics
- **Application Entity**:
  - Table: `applications` with unique key on `(student_id, vacancy_id)`
  - Relationships:
    - `student` -> `Student` (`@ManyToOne(fetch = LAZY)`)
    - `vacancy` -> `Vacancy` (`@ManyToOne(fetch = LAZY)`)
  - Status Persistence: `@Enumerated(EnumType.STRING)` for human-readable and migration-safe values
  - Audit Fields:
    - `appliedAt` (`@CreationTimestamp`) captures application creation time
    - `updatedAt` (`@UpdateTimestamp`) captures last status update time
  - Anti-spam Constraint: DB-level uniqueness blocks duplicate applications to same vacancy
- **ApplicationRepository**:
  - `existsByStudentIdAndVacancyId(UUID studentId, UUID vacancyId)`
    - Prevents duplicate apply attempts before DB constraint violation
  - `countByStudentIdAndStatus(UUID studentId, ApplicationStatus status)`
    - Supports single-placement guardrail for `ACCEPTED` status
- **ApplicationService**:
  - `apply(Student student, Vacancy vacancy)`
    - Guardrail 1: Requires `student.getProfileCompletion() >= 60.0`
    - Guardrail 2: Rejects duplicate student-vacancy applications
    - Creates new `Application` with default status `APPLIED`
  - `updateStatus(UUID applicationId, ApplicationStatus newStatus)`
    - Loads target application or throws "Application not found"
    - Placement rule: when transitioning to `ACCEPTED`, reject if student already has another `ACCEPTED` application
    - Persists status transition and updates `updatedAt` automatically
  - Transactional Safety: methods run in `@Transactional` boundaries
- **Ghosting Prevention Foundation**:
  - `updatedAt` plus state transitions enables future SLA metrics (e.g., time from `APPLIED` to first response)
  - Explicit status machine avoids ambiguous free-text states and supports dashboard analytics
- **Database Schema (V4 Migration)**:
  - New table: `applications`
  - PK: `id UUID DEFAULT gen_random_uuid()`
  - FKs:
    - `student_id` -> `students(user_id)` with `ON DELETE CASCADE`
    - `vacancy_id` -> `vacancies(id)` with `ON DELETE CASCADE`
  - Status Column: `VARCHAR(32) NOT NULL DEFAULT 'APPLIED'`
  - Timestamps: `applied_at`, `updated_at` default to `CURRENT_TIMESTAMP`
  - Indexes:
    - `idx_applications_student_status` for student lifecycle queries
    - `idx_applications_vacancy_status` for recruiter pipeline queries

### [2026-03-10] Module 6.3.5: Vacancy Management & Partnership Ranking - Discovery Feed Logic
- **Commit**: `0180cb0`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/vacancy/model/Vacancy.java` - Vacancy posting entity linked to Company
  - `src/main/java/com/internova/modules/vacancy/repository/VacancyRepository.java` - JPQL ranking query for department discovery feed
  - `src/main/java/com/internova/modules/vacancy/service/VacancyService.java` - Posting and retrieval business logic
  - `src/main/resources/db/migration/V3__Add_Vacancies_Table.sql` - Schema migration for vacancies table
- **Vacancy Entity**:
  - Primary Key: UUID (auto-generated)
  - Relationship: `@ManyToOne(fetch = FetchType.LAZY)` to `Company` via `company_id`
  - Core Fields: `title`, `description`, `requirements`, `location`
  - Lifecycle Fields: `isActive` (default true), `createdAt` (`@CreationTimestamp`, immutable)
  - LAZY Relationship Rationale: Prevents eager loading overhead when listing many vacancies
- **VacancyRepository Ranking Query**:
  - Method: `findRankedVacanciesForDepartment(UUID deptId)`
  - Uses JPQL `LEFT JOIN ... ON ...` between `Vacancy` and `DepartmentPartnership`
  - Ranking via `CASE` in `ORDER BY`:
    - `EXCLUSIVE` partnership → priority 1
    - `PREFERRED` partnership → priority 2
    - Other non-null partnership → priority 3
    - No partnership → priority 4
  - Secondary Sort: `v.createdAt DESC` (newer listings first within same partnership tier)
  - Outcome: Database performs ranking efficiently; no in-memory Java sort required
- **VacancyService Guardrails**:
  - `postVacancy(Company company, String title, String description, String requirements)`
    - Enforces verification rule: rejects unverified companies (`company.getIsVerified() == false`)
    - Throws `RuntimeException("Company must be verified by an admin to post vacancies.")`
    - Creates and saves vacancy only when guardrail passes
  - `getDiscoveryFeed(UUID departmentId)`
    - Delegates to ranked JPQL query
    - Returns active opportunities pre-sorted by partnership strength + recency
  - Read Path uses `@Transactional(readOnly = true)` for retrieval optimization
- **Database Schema (V3 Migration)**:
  - New table: `vacancies`
  - PK: `id UUID DEFAULT gen_random_uuid()`
  - FK: `company_id UUID NOT NULL REFERENCES companies(user_id) ON DELETE CASCADE`
  - Columns: `title`, `description`, `requirements`, `location`, `is_active`, `created_at`
  - Indexes:
    - `idx_vacancies_company` on `(company_id)` for company scoped operations
    - `idx_vacancies_active_created_at` on `(is_active, created_at DESC)` for feed retrieval
- **Architectural Insight**:
  - Marketplace fairness and strategy are encoded at query-level, not controller-level
  - Partnership ranking operationalizes institutional agreements in student discovery experience
  - Verification guard prevents unapproved organizations from posting opportunities
  - Migration-first schema keeps parity with `spring.jpa.hibernate.ddl-auto=validate`

### [2026-03-10] Module 6.3.4: Logbook API & Traffic Light Analytics - Compliance Dashboard
- **Commit**: `fa9d4a1`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/logbook/dto/LogbookResponse.java` - DTO with calculated status
  - `src/main/java/com/internova/modules/logbook/controller/LogbookController.java` - Student-only endpoints
- **Files Modified**:
  - `src/main/java/com/internova/modules/logbook/repository/LogbookRepository.java` - Added `findTop5ByStudentIdOrderByEntryDateDesc()` for analytics
  - `src/main/java/com/internova/modules/logbook/service/LogbookService.java` - Added `getLogsByStudent()` and `calculateComplianceStatus()`
- **LogbookResponse DTO**:
  - Decouples REST API contract from entity structure
  - Eliminates exposure of administrative fields (avoid leaking system data)
  - Key Field: `status` - Calculated by mapper as "ON_TIME" or "LATE" (determined by `submittedAt.toLocalDate().isAfter(entryDate.plusDays(2))`)
  - Calculation Logic:
    - If submitted date is after (entryDate + 2 days), mark as "LATE"
    - Otherwise, mark as "ON_TIME"
    - Example: Entry for 2026-03-10 submitted on 2026-03-12 (within 48h) → "ON_TIME"; submitted on 2026-03-13 → "LATE"
- **LogbookController**:
  - Base Path: `/api/v1/logbooks`
  - Security: `@PreAuthorize("hasRole('STUDENT')")` on all endpoints
  - Two Endpoints:
    1. **POST `/api/v1/logbooks/submit`** (Student-only):
       - Requires Authentication: `@AuthenticationPrincipal Student student` auto-resolves StudentDetails from JWT cookie
       - Request: `?date=2026-03-10` (ISO format via `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)`) + JSON body `{ "content": "...", "tags": "..." }`
       - Flow: Calls `LogbookService.submitEntry()` → applies 48-hour window validation + duplicate check → saves via repository
       - Returns: `{ "message": "Log submitted successfully", "id": "<UUID>" }` (200 OK) or error via GlobalExceptionHandler (400)
       - Design Note: `@RequestBody Map<String, String>` is flexible for optional fields (content required, tags optional)
    2. **GET `/api/v1/logbooks/my-logs`** (Student-only):
       - No query params; autofetches logs for authenticated student via `@AuthenticationPrincipal`
       - Calls `LogbookService.getLogsByStudent(studentId)` → queries ALL logs in DESC order (most recent first)
       - Returns: List of `LogbookResponse` objects (200 OK)
       - Mapper applies status calculation on each entry (`mapToResponse` private helper)
       - Use Case: Frontend calls on page load to populate student's logbook UI
- **LogbookService Enhancements**:
  - `getLogsByStudent(UUID studentId) -> List<LogbookEntry>`: Retrieves all logs for a student (read-only transaction)
  - `calculateComplianceStatus(UUID studentId) -> String`: Traffic Light Algorithm
    - Fetches top 5 recent logs (most recent entry point for efficiency)
    - Calculates days gap: `ChronoUnit.DAYS.between(mostRecentEntryDate, LocalDate.now())`
    - Returns Compliance Status:
      - `"RED"` if no logs exist OR missing days > 7 (hasn't logged in over a week) → Non-compliant
      - `"YELLOW"` if missing days > 3 but ≤ 7 → Warning zone (starting to slack)
      - `"GREEN"` otherwise → Active and compliant
    - Design: Threshold-based (not averaging) provides clear dashboard signal
- **LogbookRepository Enhancements**:
  - `findTop5ByStudentIdOrderByEntryDateDesc(UUID studentId) -> List<LogbookEntry>`: Spring Data auto-generates this query
  - Purpose: Efficient analytics without scanning entire student logbook (constant-time lookup)
  - Ordering: DESC ensures oldest entry in top 5 is the earliest we need to check for compliance window
- **API Design Patterns**:
  - **Role-Based Access Control**: `@PreAuthorize` annotation enforces STUDENT role at method level (Spring Security 6.x)
    - Attempting access with non-STUDENT role → 403 Forbidden (handled by SecurityConfig)
    - Examples: Company rep accessing `/submit` → 403; Guest → 403
  - **On-the-Fly Analytics**: Status calculation happens in mapper (not stored in DB) → keeps database clean while providing UI value
  - **ISO Date Format Handling**: `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` provides standardized date format (2026-03-10) to server regardless of client locale
  - **DTO Mapper Pattern**: Private helper `mapToResponse()` encapsulates conversion logic, separating concerns from endpoint logic
  - **Read-Only Transactions**: `@Transactional(readOnly=true)` on analytics methods hints to database for optimization
- **Architectural Insight**:
  - **Traffic Light Visibility**: Supervisors will later query `calculateComplianceStatus()` endpoints to see student compliance at a glance
  - **Behavioral Data**: Separation of `entryDate` vs `submittedAt` combined with compliance thresholds creates audit trail of student procrastination patterns
  - **Stateless API**: No session affinity needed; each request authenticates via JWT cookie (enabling horizontal scaling)
  - **Future Enhancement**: Alert system can query students with `"RED"` status and send reminder emails to supervisors weekly

### [2026-03-10] Module 6.3.3: Logbook Entry & 48-Hour Submission Service - Compliance Artifact
- **Commit**: `b344477`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/logbook/model/LogbookEntry.java` - Compliance artifact entity
  - `src/main/java/com/internova/modules/logbook/repository/LogbookRepository.java` - Custom query repository
  - `src/main/java/com/internova/modules/logbook/service/LogbookService.java` - Business logic with temporal enforcement
  - `src/main/resources/db/migration/V2__Add_Logbook_Entries_Table.sql` - Versioned schema migration
- **LogbookEntry Entity**:
  - Primary Key: UUID (auto-generated)
  - Key Fields:
    - `student` (ManyToOne LAZY to Student) - FK with ON DELETE CASCADE
    - `entryDate` (LocalDate) - The day the work was done (user-provided, not auto-timestamped)
    - `content` (TEXT NOT NULL) - Work description/evidence
    - `tags` (VARCHAR 500) - Comma-separated skills or activity categories
    - `submittedAt` (@CreationTimestamp, NOT NULL) - Automatic capture of submission moment (immutable, `updatable=false`)
    - `isStamped` (Boolean, default false) - Supervisor approval flag
    - `stampedAt` (LocalDateTime, nullable) - When supervisor approved (set only on stamp)
    - `supervisorRemarks` (TEXT, nullable) - Optional feedback from approver
  - Unique Constraint: `UNIQUE(student_id, entry_date)` - Prevents duplicate entries per day
  - Database Constraint: `CHECK(entry_date <= CURRENT_DATE)` - Cannot log future dates
- **LogbookRepository**:
  - `findByStudentIdOrderByEntryDateDesc(UUID studentId)` → List entries for a student in reverse chronological order (most recent first)
  - `findByStudentIdAndEntryDate(UUID studentId, LocalDate entryDate)` → Optional for checking duplicates before submission
  - Extends JpaRepository for basic CRUD (save, findById, delete, etc.)
- **LogbookService**:
  - **Core Method**: `submitEntry(Student student, LocalDate entryDate, String content, String tags) -> LogbookEntry`
  - **Rule 1 - Duplicate Prevention**: Check if entry already exists for `(student, entryDate)` pair → throw `RuntimeException`
  - **Rule 2 - 48-Hour Submission Window Enforcement**:
    - Uses `ChronoUnit.HOURS.between(entryDate.atStartOfDay(), LocalDateTime.now())`
    - Calculation: Convert entryDate to start-of-day (00:00), compute hours elapsed until now()
    - Constraint: `if (hoursSinceWorkDate > 48 + 24)` → throw RuntimeException "Submission window closed..."
    - Rationale: 24-hour buffer for each day + 48-hour rolling window = students must stay current
    - Example: Entry for 2026-03-10 can be submitted until end of 2026-03-12 23:59:59
  - **@Transactional**: Ensures atomicity (student + entry must both save, or both rollback)
- **Database Schema (V2 Migration)**:
  - `logbook_entries` table with 9 columns
  - PK: `id UUID DEFAULT gen_random_uuid()`
  - FK: `student_id UUID NOT NULL REFERENCES students(user_id) ON DELETE CASCADE`
  - Unique Index: `UNIQUE(student_id, entry_date)` prevents same-day duplicates at DB level
  - Composite Index: `idx_logbook_student_date ON logbook_entries(student_id, entry_date DESC)` - Optimizes queries by student + latest-first ordering
  - Check Constraint: `entry_date <= CURRENT_DATE` - Prevents accidental future dates
- **Architectural Insight**:
  - Temporal Integrity: Separation of `entryDate` (when work happened) vs `submittedAt` (when logged) enables audit trails
  - Audit Trail Example: Admin can query "Students who submit logs within 1 hour (vs. students batch-filling end-of-semester)"
  - Separation of Concerns: Service takes Student object (not UUID) → enables polymorphic reuse (web, mobile, batch import all use same logic)
  - Immutability: `submittedAt` is `updatable=false` → once set, cannot be recalculated by mistake
  - Future Extension: `stampedAt` triggers workflow to calculate supervisor workload metrics
- **Bug Discovered & Fixed** (Commit `49a491b`):
  - Issue: Initial Student import path incorrectly used `com.internova.core.model.Student`
  - Root Cause: Student entity is located at `com.internova.modules.student.model.Student` (organized under modules, not core)
  - Fix: Updated imports in LogbookEntry.java and LogbookService.java to correct package path
  - Verification: `mvn clean compile -q` passes with no errors after fix
  - Lesson: Verify import paths match actual codebase organization before pushing

### [2026-03-10] Module 6.3.2: AuthController & Global Exception Handling - REST API Exposure
- **Commit**: `7003236`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/common/exception/GlobalExceptionHandler.java` - @ControllerAdvice for standardized error responses
  - `src/main/java/com/internova/modules/auth/controller/AuthController.java` - REST endpoints for auth operations
- **GlobalExceptionHandler**:
  - Implements `@ControllerAdvice` (global exception handler for all controllers)
  - `@ExceptionHandler(RuntimeException.class)` catches all runtime exceptions (email exists, department not found, etc.)
  - Returns standardized JSON error response: `{ "error": "message" }` instead of stack traces
  - Status: HTTP 400 Bad Request
  - Future enhancement: Add specific handlers for `AuthenticationException`, `ValidationException`, etc.
- **AuthController**:
  - Placed at `/api/v1/auth` base path (aligns with SecurityConfig public endpoint pattern)
  - Four endpoints:
    1. **POST `/api/v1/auth/register`**:
       - Accepts `RegisterRequest` with validation (`@Valid` triggers Jakarta validation)
       - Calls `AuthService.register()`
       - Returns: `{ "message": "Registration successful. Please check your status." }`
       - Status: 200 OK or 400 Bad Request (caught by GlobalExceptionHandler)
    2. **POST `/api/v1/auth/login`**:
       - Accepts `LoginRequest` with email/password
       - Calls `AuthService.login()` which sets JWT HttpOnly cookie on response
       - Returns: `{ "message": "Login successful" }`
       - Cookie set: JWT (HttpOnly, SameSite=Strict, 24h expiration)
       - Status: 200 OK or 401 Unauthorized (if credentials invalid)
    3. **GET `/api/v1/auth/me`** (Status Check Endpoint):
       - **CRITICAL FOR SPA**: Frontend uses this to check if session still valid after page refresh
       - Requires authentication (`@AuthenticationPrincipal User user` auto-resolves from SecurityContext)
       - No query params; relies entirely on JWT cookie being present and valid
       - Returns: `{ "email": "user@example.com", "role": "STUDENT", "status": "ACTIVE" }`
       - Status: 200 OK if authenticated, 403 Forbidden if JWT invalid/absent
       - **Design Pattern**: SPA flow = GET /me on load → if 200, user logged in; if 403, redirect to login
    4. **POST `/api/v1/auth/logout`**:
       - Stateless logout: cannot delete token server-side, so instruct browser to discard cookie
       - Creates new cookie with same name ("JWT") but `maxAge=0` (browser deletes immediately)
       - Returns: `{ "message": "Logged out successfully" }`
       - Status: 200 OK
- **Spring Security Integration**:
  - `@AuthenticationPrincipal User user` - Spring Security shortcut that injects currently authenticated user from SecurityContext
  - Works because JwtAuthenticationFilter populates SecurityContext on every authenticated request
  - Type-safe: injection fails at compile time if User not found in SecurityContext (unlike casting)
- **Validation Chain**:
  - `@Valid @RequestBody RegisterRequest` triggers Jakarta Validation (JSR-380)
  - Validates before method body executes; errors caught by GlobalExceptionHandler
  - Prevents database operations on malformed input
- **HttpOnly Cookie Flow**:
  - Login: Server sets cookie in response headers
  - Subsequent requests: Browser auto-includes cookie in request
  - Logout: Server sets `MaxAge=0`; browser discards cookie
  - No JavaScript access: impossible for XSS attacks to steal token
- **Error Handling Examples**:
  - Email already exists → RuntimeException → GlobalExceptionHandler → 400 Bad Request + JSON error
  - Invalid credentials → Spring Security 401 Unauthorized (or custom handler)
  - Missing JWT for protected endpoint → JwtAuthenticationFilter + SecurityFilterChain → 403 Forbidden
- **Frontend SPA Pattern** (Now Possible):
  ```javascript
  // On app load
  const response = await fetch('/api/v1/auth/me');
  if (response.ok) {
    user = await response.json();  // Already logged in
  } else {
    redirectToLogin();  // Not logged in
  }
  ```
- **Dependencies**:
  - AuthService (Module 6.3.1)
  - User entity (Module 6.1.2, implements UserDetails)
  - SecurityContext from Spring Security (populated by JwtAuthenticationFilter)
  - Jakarta Validation API (spring-boot-starter-validation)
  - Lombok (@RequiredArgsConstructor)
- **Next Phase** (Module 6.3.3):
  - Create more service & repository interfaces (StudentRepository, CompanyRepository, etc.)
  - Create service layer for business logic (StudentService, CompanyService, DepartmentService)
  - Implement role-specific endpoints with `@PreAuthorize` restrictions
  - Create response DTOs for student profile, company profile, etc.

### [2026-03-10] Module 6.3.1: Authentication & Onboarding Service Layer
- **Commit**: `b3d274d`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/auth/dto/RegisterRequest.java` - DTO for user registration with role-specific fields
  - `src/main/java/com/internova/modules/auth/dto/LoginRequest.java` - DTO for login credentials
  - `src/main/java/com/internova/modules/organization/repository/DepartmentRepository.java` - JPA repository for Department lookups
  - `src/main/java/com/internova/modules/auth/service/AuthService.java` - Business logic for registration and login
- **Data Transfer Objects**:
  - **RegisterRequest**:
    - `email` (validated with @Email)
    - `password` (not-blank)
    - `role` (STUDENT, COMPANY_REP, ACADEMIC_SUPERVISOR, UNIVERSITY_ADMIN)
    - Role-specific fields: `studentIdNumber`, `departmentId`, `companyName`, `registrationNumber`, `industry`
    - Decouples API contract from internal entity structure (prevents exposing sensitive fields)
  - **LoginRequest**:
    - `email` (validated with @Email)
    - `password` (not-blank)
    - Minimal payload: only credentials needed for authentication
- **DepartmentRepository Interface**:
  - Extends `JpaRepository<Department, UUID>` for basic CRUD (findById, save, delete, etc.)
  - Bridge between AuthService and Department persistence
  - Used to validate department exists before creating student
- **AuthService Bean** (Onboarding Logic):
  - **`register(RegisterRequest request)`** method:
    - Validates email uniqueness via `UserRepository.existsByEmail()`
    - **Polymorphic Entity Creation** (joined-table inheritance):
      - STUDENT role: Create Student entity with studentIdNumber, lookup & link Department
      - COMPANY_REP role: Create Company entity with name, registration, industry; set status to PENDING_VERIFICATION
      - Other roles: Throw IllegalArgumentException (supervisors/admins created only by admin)
    - Sets shared User fields: email, encoded password, role, (status defaults from AccountStatus enum)
    - `@Transactional` ensures atomicity: if student insert fails, user record rolls back
    - Returns success message
  - **`login(LoginRequest request, HttpServletResponse response)`** method:
    - Calls `authenticationManager.authenticate()` with email/password
    - AuthManager delegates to DaoAuthenticationProvider → CustomUserDetailsService → UserRepository
    - If invalid credentials: throws BadCredentialsException (Spring Security handles 401)
    - If valid: retrieve User from database
    - Generate JWT via JwtService.generateToken() (encodes email + role in token)
    - Create HttpOnly cookie with JWT:
      - `HttpOnly = true` (XSS protection: JavaScript cannot read token)
      - `Secure = true` (HTTPS-only; should be true in production)
      - `Path = /` (sent on all API requests)
      - `MaxAge = 86400` (24 hours, matches jwt-expiration-ms)
      - `SameSite = Strict` (CSRF protection: cookie only sent to same site)
    - Add cookie to response headers
- **Domain Verification Logic**:
  - **Students**: Required to provide `studentIdNumber` and belong to valid `departmentId`
    - Future enhancement: Validate email domain against university domain
    - Example: Only @example-university.edu emails allowed for specific university
  - **Companies**: Set to `PENDING_VERIFICATION` upon registration
    - Only verified companies can post vacancies and interact with students
    - Admin approval required before status changed to ACTIVE
  - **Supervisors/Admins**: Not allowed to self-register
    - Created only by super-admin via private admin endpoints (future module)
- **Security & Atomicity**:
  - `@Transactional`: Registration is atomic; if department lookup fails, no user record created
  - `@RequiredArgsConstructor`: Clean constructor injection of dependencies (Lombok generates it)
  - Password encoding: BCrypt never stored plain; validation happens at authentication layer
  - Role discrimination: Switch statement ensures type-safe polymorphic entity creation
- **Dependencies**:
  - UserRepository (Module 6.2.1)
  - DepartmentRepository (new, extends JpaRepository)
  - CustomUserDetailsService & AuthenticationManager (Module 6.2.3)
  - JwtService (Module 6.2.2)
  - PasswordEncoder (Module 6.2.1)
  - Student, Company entity models (Module ???)
  - Jakarta Validation (JSR-380 annotations: @Email, @NotBlank, @NotNull)
  - Lombok (@RequiredArgsConstructor, @Data on DTOs)
- **API Contract** (For AuthController in next phase):
  - POST `/api/v1/auth/register` → RegisterRequest → String (success message)
  - POST `/api/v1/auth/login` → LoginRequest → JWT cookie in response (HttpOnly)
- **Next Phase** (Module 6.3.2):
  - Create AuthController (REST endpoints wrapping AuthService)
  - Implement error handling (@RestControllerAdvice for exception mapping)
  - Create response DTOs (AuthResponse with user role/email)
  - Add logging for audit trail
  - Implement email domain validation for students (future enhancement)

### [2026-03-10] Module 6.2.3: UserDetails Implementation & Complete Authentication Filter Registration
- **Commit**: `09f2af5`
- **Status**: ✅ Complete & Pushed
- **Files Modified/Created**:
  - `src/main/java/com/internova/core/model/User.java` - Updated to implement `UserDetails` interface
  - `src/main/java/com/internova/modules/auth/service/CustomUserDetailsService.java` - Implements `UserDetailsService` to load users from DB
  - `src/main/java/com/internova/config/SecurityConfig.java` - Updated with `AuthenticationProvider`, `AuthenticationManager`, and `@RequiredArgsConstructor`
- **User Entity - UserDetails Implementation** (Updated):
  - `getAuthorities()` → Returns `List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))`
    - Prefixes role with "ROLE_" to align with Spring Security default behavior
    - Enables `@PreAuthorize("hasRole('STUDENT')")` annotations on controllers
  - `getUsername()` → Returns `email` (username in Spring Security = email in Internova)
  - `isAccountNonExpired()` → Always `true` (no credential expiration in current design)
  - `isAccountNonLocked()` → Returns `status != AccountStatus.SUSPENDED`
    - Locked users cannot authenticate even with valid password/token
  - `isCredentialsNonExpired()` → Always `true` (no password expiration)
  - `isEnabled()` → Returns `status == AccountStatus.ACTIVE || status == AccountStatus.PENDING_VERIFICATION`
    - Deactivated users cannot access system; pending users can (pre-verification access)
- **CustomUserDetailsService Bean**:
  - Implements Spring's `UserDetailsService` interface (single method: `loadUserByUsername`)
  - `loadUserByUsername(String email)` → Calls `UserRepository.findByEmail()`
    - Returns User (which now implements UserDetails) if found
    - Throws `UsernameNotFoundException` if user doesn't exist
  - Acts as bridge between JWT validation filter and User database
  - Allows JwtAuthenticationFilter to load user data without direct DB access in filter
- **SecurityConfig Updates** (Enhanced):
  - Added `@RequiredArgsConstructor` annotation for field injection (replaces manual constructor)
  - Added field: `private final UserDetailsService userDetailsService`
  - Added bean: `authenticationProvider()`
    - Creates `DaoAuthenticationProvider` (Data Access Object pattern)
    - Sets `CustomUserDetailsService` as user lookup mechanism
    - Sets `BCryptPasswordEncoder` for password comparison
    - Registers both into single authentication unit
  - Added bean: `authenticationManager(AuthenticationConfiguration config)`
    - Exposes `AuthenticationManager` as injectable bean (used in AuthService for login)
    - Obtained from Spring's `AuthenticationConfiguration`
  - Updated `securityFilterChain()`:
    - Added `.authenticationProvider(authenticationProvider())` before filter registration
    - Ensures Spring uses our DaoAuthenticationProvider for `/api/v1/auth/login` calls
- **The Complete Authentication Flow** (Now Operable):
  1. Client sends POST /api/v1/auth/login with email/password
  2. AuthController (future) calls AuthService.authenticate()
  3. AuthService uses AuthenticationManager.authenticate() with UsernamePasswordAuthenticationToken
  4. AuthenticationManager delegates to DaoAuthenticationProvider
  5. DaoAuthenticationProvider calls CustomUserDetailsService.loadUserByUsername()
  6. CustomUserDetailsService queries UserRepository.findByEmail()
  7. If user found: compare provided password vs user.getPassword() using BCryptPasswordEncoder
  8. If valid: AuthService calls JwtService.generateToken() with UserDetails
  9. JwtService creates JWT with email + role claims, signs with secret key
  10. AuthController sets JWT in HttpOnly cookie on response
  11. Client's subsequent requests include JWT cookie
  12. JwtAuthenticationFilter intercepts: extracts cookie, validates with JwtService
  13. If valid: populates SecurityContext; request proceeds to controller
  14. If invalid/expired: SecurityContext remains empty; request gets 403 Forbidden
- **Why This Completes the Guardrail**:
  - **Role Enforcement**: User.getAuthorities() now feeds directly into @PreAuthorize checks
  - **Account Status Checks**: isEnabled() and isAccountNonLocked() prevent suspended/deactivated users even if they have valid tokens
  - **Zero Trust Model**: Every request re-validates the JWT and checks user status (no cached decisions)
  - **Department-Level Isolation**: JwtService can encode departmentId in token (future enhancement); SecurityContext + UserDetails enable row-level filtering
- **Dependencies**:
  - User entity (already exists, now enhanced)
  - UserRepository (from Module 6.2.1)
  - JwtService and JwtAuthenticationFilter (from Module 6.2.2)
  - Spring Security core (authentication, authorization, UserDetails interface)
  - Lombok (for @RequiredArgsConstructor)
- **Verification** (Application now operational):
  - Run `./mvnw spring-boot:run`
  - CustomUserDetailsService bean registers successfully
  - AuthenticationProvider bean wires UserDetailsService + PasswordEncoder
  - JwtAuthenticationFilter is online in filter chain
  - Trying unauthenticated request to /api/v1/students → 403 Forbidden (filter running)
  - Trying /api/v1/auth/** → 200 OK (public endpoint)
  - Next module will implement AuthService/AuthController to issue JWTs and test full flow
- **Next Phase** (Module 6.2.4):
  - Create AuthService (registration, login, password hashing)
  - Create AuthController (POST /api/v1/auth/register, POST /api/v1/auth/login)
  - Implement JWT cookie setting with HttpOnly, SameSite=Strict, Secure flags
  - Create AuthResponse DTO for login response
  - Complete end-to-end authentication flow testing

### [2026-03-10] Module 6.2.2: JWT Token Infrastructure - HttpOnly Cookie Authentication
- **Commit**: `dfea10e`
- **Status**: ✅ Complete & Pushed
- **Files Created/Modified**:
  - `pom.xml` - Added JJWT dependencies (jjwt-api, jjwt-impl runtime, jjwt-jackson runtime) version 0.12.5
  - `src/main/java/com/internova/modules/auth/service/JwtService.java` - JWT token factory (generation, validation, claims extraction)
  - `src/main/java/com/internova/config/JwtAuthenticationFilter.java` - HttpOnly cookie JWT validator (OncePerRequestFilter)
  - `src/main/java/com/internova/config/SecurityConfig.java` - Updated to register JwtAuthenticationFilter in filter chain
  - `src/main/resources/application.properties` - Added internova.security.jwt-secret (Base64-encoded)
- **JwtService Bean** (Token Factory):
  - `generateToken(UserDetails userDetails)` → Create signed JWT with username + role claims
  - `extractUsername(String token)` → Parse token and extract email (Subject claim)
  - `extractClaim(String token, Function<Claims, T> claimsResolver)` → Generic claim extraction
  - `isTokenValid(String token, UserDetails userDetails)` → Verify token signature and expiration
  - `getSignInKey()` → Decode Base64 secret and create HMAC-SHA256 signing key
  - Token includes: username (Subject), issued time, expiration, role, signature
  - Expiration: 24 hours (86400000 ms from application.properties)
- **JwtAuthenticationFilter** (Request Interceptor):
  - Extends `OncePerRequestFilter` (runs once per HTTP request)
  - **HttpOnly Cookie Extraction**: Iterates through request cookies to find "JWT" cookie
  - **Stateless Validation**:
    1. Extract JWT from HttpOnly cookie
    2. If no JWT, pass to next filter (unprotected endpoint or no auth)
    3. If JWT exists, extract username and load UserDetails from UserDetailsService
    4. Validate token signature and expiration
    5. Populate SecurityContext with UsernamePasswordAuthenticationToken (marks request as authenticated)
  - **No Session Storage**: Each request is independently validated; no JSESSIONID stored
- **HttpOnly Cookies vs localStorage**:
  - **Why HttpOnly (chosen strategy)**:
    - JavaScript cannot access HttpOnly cookies (XSS protection)
    - Browser automatically includes cookie on each request (transparent to client)
    - Prevents token theft via `document.localStorage.getItem('token')`
  - **XSS Defense**: If malicious script executed, it cannot read/steal the JWT
  - **Trade-off**: Client cannot inspect token (resolved by decoding on backend during validation)
- **SecurityConfig Updates**:
  - Constructor injection of `JwtAuthenticationFilter` bean
  - Added `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`
  - JwtAuthenticationFilter runs BEFORE UsernamePasswordAuthenticationFilter in Spring's filter chain
  - Order matters: JWT validation must occur before other auth mechanisms
- **Application Configuration**:
  - `internova.security.jwt-secret`: Base64-encoded 256-bit key (96 character Base64 string = 72 bytes = 576 bits)
  - `internova.security.jwt-expiration-ms`: 86400000 ms = 24 hours (configurable per environment)
- **Why JJWT (Java JWT Library)**:
  - Industry standard: used by Auth0, Spring Security, AWS Cognito integrations
  - Version 0.12.5: Latest stable release with strong crypto
  - Handles HMAC-SHA256 signing automatically
  - Validates signature integrity and expiration in one step
  - Type-safe claim extraction via Function API
- **Data Isolation Pattern (Foundation)**:
  - JwtService will encode role and other claims into token
  - JwtAuthenticationFilter populates SecurityContext with authorities
  - Later modules (AuthService, Controllers) will use @PreAuthorize("hasRole('...')") from JWT claims
  - Example: A STUDENT role can only access /api/v1/students/{studentId} where studentId matches their own ID
- **Security Implications**:
  - Token cannot be forged (signed with secret key known only to backend)
  - Token cannot be modified (signature verification catches tampering)
  - Token cannot be replayed indefinitely (expiration enforced)
  - Token theft via XSS impossible (HttpOnly cookie)
- **Dependencies**:
  - JwtService depends on JwtAuthenticationFilter (filter uses service to validate)
  - JwtAuthenticationFilter depends on UserDetailsService (loads user from DB)
  - UserDetailsService will be implemented in Module 6.2.3 (UserDetailsServiceImpl)
  - SecurityConfig wires both into the filter chain
- **Next Phase** (Module 6.2.3):
  - Implement UserDetailsService (loads User from UserRepository and converts to Spring UserDetails)
  - Create AuthService (registration, login, password hashing/validation)
  - Create AuthController (POST /api/v1/auth/register, POST /api/v1/auth/login endpoints)
  - Handle JWT cookie setting on login response (HttpOnly, SameSite=Strict, Secure flags)
  - Handle JWT cookie clearing on logout response
- **Verification** (Manual Testing):
  1. Run `./mvnw spring-boot:run` - Should start without ClassNotFound for JJWT
  2. Application bootstraps JwtService and JwtAuthenticationFilter beans
  3. Try unauthenticated request to /api/v1/students → 403 Forbidden (JwtAuthenticationFilter doesn't set SecurityContext)
  4. Once AuthController is implemented: login → receive JWT cookie → make authenticated request → JwtAuthenticationFilter validates JWT

### [2026-03-10] Module 6.2.1: Spring Security Configuration - Stateless JWT Authentication Guardrails
- **Commit**: `d175870`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/auth/repository/UserRepository.java` - JPA repository for User identity lookups
  - `src/main/java/com/internova/config/SecurityConfig.java` - Spring Security configuration with BCrypt and stateless filters
- **Architecture**: Stateless Authentication System
  - No JSESSIONID cookies; every request contains JWT proof of identity/role in Authorization header
  - Enables horizontal scaling: any API instance can serve any request (no session affinity required)
  - Aligns with cloud-native, microservice-ready deployment patterns
- **UserRepository Interface**:
  - Extends `JpaRepository<User, UUID>` for basic CRUD operations
  - `Optional<User> findByEmail(String email)` - Find user by email (used in login flows)
  - `boolean existsByEmail(String email)` - Quick check for email availability (used in registration validation)
  - Placed in `modules/auth/repository` as the identity gateway (auth is the entry point for all user operations)
- **SecurityConfig Bean Configuration**:
  - **PasswordEncoder**: `BCryptPasswordEncoder()` with default strength (10 rounds)
    - BCrypt automatically generates unique salts; passwords are never reversible
    - Password verification is slow by design (prevents brute-force attacks)
  - **SecurityFilterChain**: HTTP request filter configuration
    - `csrf(AbstractHttpConfigurer::disable)` - CSRF protection disabled because JWT + stateless model is naturally CSRF-resistant
    - `sessionManagement(STATELESS)` - Disables session creation; no HttpSession objects stored server-side
    - **Public Endpoints**: `/api/v1/auth/**` (registration/login), `/v3/api-docs/**`, `/swagger-ui/**`
    - **Protected Endpoints**: All others require `.authenticated()` (will be checked by JWT filter in Module 6.2.2)
  - **Method Security**: `@EnableMethodSecurity` enables `@PreAuthorize("hasRole(...)")` annotations on controller methods
    - Example: `@PreAuthorize("hasRole('COMPANY_REP')")` on "Post Vacancy" endpoint enforces fine-grained access control
- **Data Isolation Pattern Enforced**:
  - By requiring authentication on all non-public endpoints and enabling method security, we establish the "Guardrail"
  - Will combine with JWT claims (role + organizationalScope) in Module 6.2.2 to enforce row-level security
  - Example: Student can only see their own logbook entries; supervisors see students in their department only
- **Why Stateless**:
  - **Scalability**: No sticky sessions; load balancer can round-robin requests without affinity
  - **Testability**: Each request is self-contained; no test setup for managing sessions
  - **Security**: Session hijacking requires stealing the JWT; no session store to compromise
  - **API Design**: RESTful APIs are stateless by definition; this enforces that principle
- **Why BCrypt**:
  - **Adaptive**: Strength factor increases as hardware becomes faster (built-in future-proofing)
  - **Salted**: Each password gets unique salt; identical passwords produce different hashes
  - **Slow**: ~100ms per check; brute-force attacks become computationally infeasible
  - **Industry Standard**: Used by major platforms (GitHub, AWS, Microsoft)
- **Dependencies**:
  - Spring Security (spring-boot-starter-security in pom.xml)
  - User entity (from commit `bffcd47`)
- **Verification**:
  - Run `./mvnw spring-boot:run`
  - Try to access `/api/v1/students` (should get 403 Forbidden or redirect to login)
  - Try to access `/api/v1/auth/register` (should succeed; endpoint is public)
  - Confirms SecurityFilterChain is active and request matcher pattern is working
- **Next Phase** (Module 6.2.2):
  - Create JWT Token Provider (generate, validate, extract claims)
  - Create JwtAuthenticationFilter (intercept requests, validate JWT, populate SecurityContext)
  - Create AuthService (password hashing, user creation, login logic)
  - Create AuthController (registration/login endpoints)

### [2026-03-10] Module 6.1.3: Flyway Database Migration - Initial Schema
- **Commit**: `2d8c6ee`
- **Status**: ✅ Complete & Pushed
- **Files Modified/Created**:
  - `pom.xml` - Added Flyway dependencies (flyway-core, flyway-database-postgresql)
  - `src/main/resources/db/migration/V1__Initial_Schema.sql` - Create initial database schema with all entities and relationships
- **Migration Strategy**: Versioned SQL migrations (Flyway best practice)
  - File naming: `V1__Initial_Schema.sql` (double underscore required by Flyway)
  - Flyway automatically tracks migrations in `flyway_schema_history` table
  - Each new migration gets a new version number (V2, V3, etc.)
  - Guarantees every developer and production environment has identical schema
- **Schema Components** (translated from JPA entities):
  1. **Identity Core**: `users` table (id, email, password, role, status, created_at, updated_at)
  2. **Organizational Hierarchy**: `universities`, `faculties`, `departments` with cascading FKs
  3. **Role-Specific Tables** (Joined-Table Inheritance):
     - `students` (user_id FK→users, department_id FK→departments, student_id_number, course, cv_url, profile_completion)
     - `companies` (user_id FK→users, company_name, registration_number, industry, is_verified)
     - `academic_supervisors` (user_id FK→users, department_id FK→departments, staff_id)
  4. **Relationship Links** (Associative Entities):
     - `supervision_relationships` (id, student_id FK, supervisor_id FK, academic_year, semester)
     - `department_partnerships` (id, department_id FK, company_id FK, partnership_type, established_at)
- **Data Integrity Features**:
  - **ON DELETE CASCADE**: Joined-table child records (students, companies, supervisors) automatically deleted when parent User is deleted
  - **Foreign Key Constraints**: Enforced at DB level (e.g., Department must exist before linking Student to it)
  - **Unique Constraints**: email, student_id_number, registration_number, staff_id prevent duplicate identities
  - **Default Values**: timestamps, profile_completion, is_verified, established_at set at DB layer for consistency
- **Why Versioned Migrations**:
  - **Immutability**: Old migrations never change; recreate or add V2, V3, etc. for future changes
  - **Auditability**: `flyway_schema_history` table tracks who deployed which migration and when
  - **Rollback Safety**: Explicit migrations easier to understand and debug than auto-generated schema
  - **Production Safety**: Never use `ddl-auto: create-drop` or `ddl-auto: update` in production; always use `ddl-auto: validate`
  - **Team Coordination**: All developers run identical scripts; no local database drift
- **How to Verify**:
  1. Ensure PostgreSQL is running and `application.properties` has correct credentials (DB_USERNAME, DB_PASSWORD)
  2. Run `./mvnw spring-boot:run` or execute the application
  3. Flyway will initialize on startup; you should see: `Successfully applied 1 migration to schema "public"` in logs
  4. Query `SELECT * FROM flyway_schema_history;` to see migration history
  5. Query `SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';` to verify all 8 tables exist
- **Next Steps**:
  - Create JPA Repositories for data access
  - Implement Service layer for business logic
  - Build REST Controllers for CRUD operations
  - Test entity relationships and migrations with integration tests

### [2026-03-10] Domain Model Phase 4: Supervision Relationships & Department Partnerships
- **Commit**: `1b6d81e`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/supervision/model/AcademicSupervisor.java` - Supervisor role entity extending User
  - `src/main/java/com/internova/modules/supervision/model/SupervisionRelationship.java` - Associative entity linking Student to AcademicSupervisor
  - `src/main/java/com/internova/modules/company/model/DepartmentPartnership.java` - Associative entity linking Company to Department
- **Design Pattern**: Explicit Link Entities (Associative Tables)
  - SupervisionRelationship and DepartmentPartnership are standalone entities (not implicit @ManyToMany annotations)
  - Allows storage of additional metadata about the relationships (academicYear, semester, partnershipType, establishedAt)
  - Enables efficient filtering, historical tracking, and auditing
- **AcademicSupervisor Entity Features**:
  - staffId (unique string, e.g., "FAC001" - NOT autogenerated, assigned by institution)
  - department (ManyToOne LAZY to Department with NOT NULL FK - supervisor scoped to department)
  - Inherits from User: email, password, role (ACADEMIC_SUPERVISOR), status, timestamps
  - Enables joined-table inheritance pattern: supervisors table FKs back to users table
- **SupervisionRelrelationship Entity Features**:
  - UUID primary key (audit trail: can track supervisor changes per semester)
  - student (ManyToOne LAZY to Student, nullable = false)
  - supervisor (ManyToOne LAZY to AcademicSupervisor, nullable = false)
  - academicYear (String, e.g., "2025-2026")
  - semester (String, e.g., "Spring", "Fall")
  - Business Rule: One student can have multiple supervisors across different years/semesters (auditability)
- **DepartmentPartnership Entity Features**:
  - UUID primary key
  - department (ManyToOne LAZY to Department)
  - company (ManyToOne LAZY to Company)
  - partnershipType (String: 'EXCLUSIVE', 'PREFERRED', 'REGULAR' - configurable per business need)
  - establishedAt (LocalDate, defaults to current date - tracks partnership creation date)
  - Business Rule: Enables fast SQL queries for "Show vacancies ranked by partnership status"
- **Why Explicit Link Entities**:
  - **Auditability**: SupervisionRelationship as its own table enables historical tracking of supervisor changes mid-semester (GDPR/compliance requirement)
  - **Filtering Performance**: DepartmentPartnership allows single SQL query to join departments, companies, and partnerships for vacancy ranking without O(n²) joins
  - **Extensibility**: Future requirements like supervisorQualifications, partnershipBenefits, or supervisorWorkload can be added without schema redesign
  - **Data Integrity**: Explicit entities enforce strong typing at ORM level (supervisor must be type AcademicSupervisor, not any User)
- **Dependencies**:
  - User base class (from commit `bffcd47`)
  - Student entity (from commit `0b49db7`)
  - Company entity (from commit `0b49db7`)
  - Department entity (from commit `0b49db7`)
  - Jakarta Persistence, Lombok
- **Next Phase**:
  - Create JPA Repositories for all entities (UserRepository, StudentRepository, CompanyRepository, AcademicSupervisorRepository, SupervisionRelationshipRepository, DepartmentPartnershipRepository)
  - Implement Service layer for business logic (enroll student, assign supervisor, create partnership, etc.)
  - Implement REST Controllers for CRUD operations and complex queries
  - Design Partnership ranking algorithm and vacancy filtering logic
  - Design Logbook entity and supervisor-student interaction workflows

### [2026-03-10] Domain Model Phase 2: Organizational Hierarchy + Role-Specific Entities
- **Commit**: `0b49db7`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/modules/organization/model/University.java` - Root organizational unit
  - `src/main/java/com/internova/modules/organization/model/Faculty.java` - Faculty within University with ManyToOne LAZY relationship
  - `src/main/java/com/internova/modules/organization/model/Department.java` - Department within Faculty with ManyToOne LAZY relationship
  - `src/main/java/com/internova/modules/student/model/Student.java` - Student role entity extending User with @PrimaryKeyJoinColumn
  - `src/main/java/com/internova/modules/company/model/Company.java` - Company role entity extending User with @PrimaryKeyJoinColumn
- **Design Pattern**: Joined-Table Inheritance (continued from User base class)
  - Student and Company both extend User via `@Inheritance(strategy = InheritanceType.JOINED)` and `@PrimaryKeyJoinColumn(name = "user_id")`
  - Ensures enforced type safety: StudentRole and CompanyRepRole users must exist in their respective subclass tables
  - Shared user identity columns in users table; type-specific data in students and companies tables
- **Organizational Hierarchy Structure**:
  - University (top-level organizational root)
  - Faculty (belongs to University via ManyToOne LAZY)
  - Department (belongs to Faculty via ManyToOne LAZY)
  - Business Rule: Students and Supervisors must be linked to exactly one Department
  - LAZY fetching strategy: relationships not loaded unless explicitly accessed (performance optimization)
- **Student Entity Features**:
  - studentIdNumber (unique string, e.g., "S20240101")
  - course (String, e.g., "Computer Science")
  - cvUrl (String, link to uploaded resume)
  - profileCompletion (Double, 0.0-100.0 percentage)
  - department (ManyToOne LAZY to Department with NOT NULL foreign key)
- **Company Entity Features**:
  - companyName (String, NOT NULL - business requirement)
  - registrationNumber (String, unique)
  - industry (String, e.g., "Technology", "Finance")
  - isVerified (Boolean, default false - company must be verified before recruitment access)
- **Why This Design**:
  - Multi-factorial organizational structure enables multi-faculty scaling (each university can have multiple faculties/departments).
  - Department-level scoping enforces role-based data isolation: student data is scoped to their department only.
  - LAZY fetching prevents N+1 query problems when loading multiple students with their departments.
  - Joined-table inheritance ensures every Student record has a corresponding User record (referential integrity at JPA level).
  - Student profile completion and CV storage support profile maturity tracking for recruitment readiness.
  - Company verification flag enforces business rule: unverified companies cannot post vacancies or contact students.
- **Dependencies**:
  - User base class (from previous commit `bffcd47`)
  - Jakarta Persistence (jakarta.persistence.*)
  - Lombok (for boilerplate elimination)
  - Optional pattern: StudentRepository, CompanyRepository (next phase)
- **Next Phase**:
  - Create AcademicSupervisor entity (extends User, linked to Department, supervises Students)
  - Implement JPA Repositories for Student, Company, University, Faculty, Department
  - Create Service layer for entity business logic (enroll student, verify company, etc.)
  - Implement REST Controllers for CRUD operations
  - Design Supervisor-Student assignment and logbook workflows

### [2026-03-10] Application Properties Setup for PostgreSQL + JPA Validate Mode
- **Commit**: `d7cac67`
- **Status**: ✅ Complete & Pushed
- **Files Modified**:
  - `src/main/resources/application.properties` - Added datasource, JPA validate mode, Hibernate dialect, and Internova business-rule properties
- **Configuration Added**:
  - `spring.datasource.url=jdbc:postgresql://localhost:5432/internova`
  - `spring.datasource.username=${DB_USERNAME}`
  - `spring.datasource.password=${DB_PASSWORD}`
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.jpa.show-sql=true`
  - `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect`
  - `internova.logbook.submission-window-hours=48`
  - `internova.security.jwt-expiration-ms=86400000`
- **Why This Change**:
  - Aligns application bootstrap config with ERD-first database workflow.
  - Enforces schema validation instead of auto-generation (`ddl-auto=validate`).
  - Adds initial business-rule properties required for logbook and JWT policy defaults.
- **Notes**:
  - Implemented in `.properties` format (equivalent to your provided YAML blocks).

### [2026-03-10] Module 6.1.2: Identity Core Domain Model - Joined-Table Inheritance
- **Commit**: `bffcd47`
- **Status**: ✅ Complete & Pushed
- **Files Created**:
  - `src/main/java/com/internova/core/enums/UserRole.java` - Four user roles: STUDENT, COMPANY_REP, ACADEMIC_SUPERVISOR, UNIVERSITY_ADMIN
  - `src/main/java/com/internova/core/enums/AccountStatus.java` - Four account statuses: PENDING_VERIFICATION, ACTIVE, DEACTIVATED, SUSPENDED
  - `src/main/java/com/internova/core/model/User.java` - Abstract base entity with joined-table inheritance strategy
- **Design Pattern**: Joined-Table Inheritance (InheritanceType.JOINED)
  - Enforces that User is abstract (cannot be instantiated directly)
  - Subclasses (Student, CompanyRep, etc.) will have dedicated tables linking back to users table
  - Shared columns (email, password, role, status, timestamps) in users table
  - Type-specific columns in child tables (students, company_reps, etc.)
- **User Entity Features**:
  - UUID primary key (prevents ID guessing in URLs)
  - Email unique constraint (business requirement)
  - Role and Status as enums stored as strings (debugging-friendly)
  - Automatic timestamps: createdAt (immutable), updatedAt (auto-updated)
  - Hibernate annotations for JPA mapping
  - Lombok annotations (@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor) for boilerplate elimination
- **Why This Design**:
  - Enforces business rule: every user must have a specific role/type.
  - Joined-table inheritance allows shared user data across all roles while maintaining type safety.
  - Abstract User class prevents runtime errors (can't create generic User).
  - UUID prevents sequential ID enumeration attacks.
  - Enums as strings allow easy debugging and data inspection.
- **Dependencies**:
  - Jakarta Persistence (jakarta.persistence.*)
  - Lombok (already in pom.xml)
  - Hibernate Annotations (org.hibernate.annotations.*)
  - Java UUID and LocalDateTime
- **Next Phase**:
  - Create Student subclass (extends User, adds student-specific fields)
  - Create CompanyRep subclass (extends User, adds company-specific fields)
  - Create UserRepository for persistence operations
  - Implement account creation and role-based authorization

### [2026-03-10] Backend-Only Repository Baseline + Package Structure Scaffold
- **Commit**: `e2c8170`
- **Status**: ✅ Complete & Pushed
- **Files Modified**:
  - `src/main/java/com/internova/ApiApplication.java` - package moved from `com.internova.api` to `com.internova`
  - `src/test/java/com/internova/ApiApplicationTests.java` - package moved from `com.internova.api` to `com.internova`
  - `src/main/resources/application.properties` - application name property retained
- **Folders Created**:
  - `src/main/java/com/internova/common/`
  - `src/main/java/com/internova/config/`
  - `src/main/java/com/internova/core/`
  - `src/main/java/com/internova/modules/auth/`
  - `src/main/java/com/internova/modules/student/`
  - `src/main/java/com/internova/modules/vacancy/`
  - `src/main/java/com/internova/modules/logbook/`
  - `src/main/java/com/internova/modules/organization/`
  - `src/main/java/com/internova/modules/supervision/`
- **Frontend Cleanup**:
  - Removed `src/main/resources/static/`
  - Removed `src/main/resources/templates/`
- **Why This Change**:
  - Establishes the target backend package architecture for future domain modules.
  - Enforces repository boundary so no frontend artifacts are stored in backend.
  - Keeps module directories empty as requested, ready for guided implementation.
- **Verification**:
  - Confirmed no frontend file types or frontend config files exist in repository.
  - Confirmed module folders exist and are empty.
- **Notes**:
  - Maven wrapper invocation in this environment failed due to wrapper runtime shell constraints.
  - Direct Maven test run reached Spring context bootstrapping and failed at datasource configuration (`Failed to determine a suitable driver class`), unrelated to folder restructuring.

---

## Bugs & Solutions

### [2026-03-10] Maven Wrapper Runtime Shell Error on Windows Session
- **Issue**: `mvnw.cmd` could not start Maven because `powershell` was not recognized by the runtime shell used in this session.
- **Root Cause**: The active shell environment did not expose a callable `powershell` command for wrapper execution.
- **Solution**: Used system Maven (`mvn test`) as fallback verification path.
- **Commit**: `e2c8170`
- **Status**: ✅ Resolved (workaround applied)
- **Prevention**: Prefer validating shell availability (`powershell`/`pwsh`) early before invoking wrapper scripts.
- **Notes**: Wrapper scripts remain intact; issue is environment-specific.

---

## Architecture & Setup Notes

### Tech Stack
- **Framework**: Spring Boot
- **Language**: Java
- **Build Tool**: Maven
- **Repository Scope**: Backend-only (no frontend source or assets)

---

**Last Updated**: 2026-03-10
**Project Status**: 🚀 Active Development
