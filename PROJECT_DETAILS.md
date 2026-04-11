# Smart Campus Operations Hub
### SLIIT · IT3030 – Programming Applications and Frameworks (PAF)
### Assignment 2026 — Semester 1 · Group Coursework

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Features & Functions](#3-features--functions)
4. [REST API Reference](#4-rest-api-reference)
5. [Database Collections](#5-database-collections)
6. [System Architecture (Marking Rubric)](#6-system-architecture-marking-rubric)
7. [Project Structure](#7-project-structure)
8. [CRUD Work Allocation — 4 Members](#8-crud-work-allocation--4-members)
9. [How to Run the Project](#9-how-to-run-the-project)
10. [Environment Variables](#10-environment-variables)
11. [Architecture — How Layers Communicate](#11-architecture--how-layers-communicate)
12. [Key Design Decisions](#12-key-design-decisions)

---

## 1. Project Overview

Smart Campus Operations Hub is a full-stack web platform for a university to manage:

- **Facility & asset bookings** — lecture halls, labs, meeting rooms, equipment
- **Maintenance & incident ticketing** — fault reports, technician assignments, resolutions
- **Role-based access control** — USER, ADMIN, TECHNICIAN
- **Real-time-style notifications** — booking decisions, ticket updates, comments
- **Admin analytics** — usage stats, charts, CSV export

**Stack:** Java Spring Boot REST API + React SPA

---

## 2. Technology Stack

### Backend

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.2.5 |
| Build Tool | Maven Wrapper (`mvnw`) | — |
| Database | MongoDB (Atlas or local Docker) | Any |
| Security | Spring Security | 6.x (via Boot) |
| Authentication | Email/Password + Google OAuth 2.0 | — |
| Token Standard | JWT — JJWT library | 0.12.5 |
| Password Hashing | BCryptPasswordEncoder | Spring Security |
| Environment Vars | dotenv-java | 3.0.0 |
| Boilerplate | Lombok | — |
| File Storage | Local disk (FileStorageService) | — |
| Architecture | Layered: Controller → Service → Repository → Entity | — |
| HATEOAS | Spring HATEOAS (HAL JSON: `_links`, `_embedded`) | via `spring-boot-starter-hateoas` |
| API docs | SpringDoc OpenAPI 3 + Swagger UI | `springdoc-openapi-starter-webmvc-ui` 2.5.0 |

### Frontend

| Layer | Technology | Version |
|---|---|---|
| Language | JavaScript (JSX) | ES2022+ |
| UI Library | React | 18.2 |
| Routing | React Router DOM | 6.x |
| Build Tool | Vite | 5.x |
| Styling | Tailwind CSS | 3.x |
| Animations | Framer Motion | 11.x |
| QR Code | qrcode | 1.5.4 |
| HTTP Client | Fetch API (custom `api/client.js`) | Browser native |
| Dev Proxy | Vite proxy → `http://127.0.0.1:8080` | — |
| State Management | React Context API (`AuthContext`) | — |

### Infrastructure

| Tool | Purpose |
|---|---|
| MongoDB Atlas | Cloud-hosted primary database |
| Docker Compose | Optional local MongoDB container |
| Google Cloud Console | OAuth 2.0 client credentials |

---

## 3. Features & Functions

### Module A — Facilities Catalogue (`/resources`)

- Create, read, update, delete campus resources
- Resource types: `LECTURE_HALL`, `LAB`, `MEETING_ROOM`, `EQUIPMENT`
- Fields per resource: name, type, capacity, location, **floor/map reference**, **amenities checklist** (e.g. Projector, AC, Whiteboard), status
- Capacity colour-coded bar chart on each card (green < 40 % · amber < 75 % · red ≥ 75 %)
- Amenities displayed as violet pill tags
- **Search by name** and **filter by type** (tab strip)
- Status: `ACTIVE` or `OUT_OF_SERVICE`
- Admin-only: create, edit, delete; all users can browse

---

### Module B — Booking Management (`/bookings`)

- Submit booking requests: resource, date/time range, purpose
- Workflow: `PENDING → APPROVED / REJECTED`; approved bookings can be `CANCELLED`
- **Conflict checking** — overlapping time slots rejected server-side
- Admin approves or rejects with a mandatory **decision reason**
- **Status filter tabs**: All / Pending / Approved / Rejected / Cancelled
- **"My bookings only" toggle** for admins (see own subset)
- **ICS calendar export** per booking (generates `.ics` file client-side — no server call)
- **QR check-in modal** for approved bookings (scannable QR containing booking ID, resource, time)
- Users see only their bookings; Admins see all with `?all=true`

---

### Module C — Maintenance & Incident Ticketing (`/maintenance`)

- Create incident tickets: title, description, priority, up to 3 image attachments
- Priorities: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- Workflow: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`
- **SLA elapsed-time chip** — live timer showing how long a ticket has been open
  - Normal colour < 24 h · Amber 24–48 h · Red > 48 h
- **Reopen button** — any RESOLVED or CLOSED ticket can be reopened
- **Status + priority filter bars** for quick navigation
- Technician assignment (admin-only)
- Resolution notes set by Technician or Admin
- **Comments** — add, edit (own), delete (own)
- Image attachments: upload (max 3) and inline view/download

---

### Module D — Notifications

- Auto-generated events:
  - Booking approved / rejected (with reason)
  - Ticket status changed / resolution updated
  - New comment on a ticket
- Notification bell in header with live unread count (polls every 60 s)
- **Type filter tabs**: All / Bookings / Tickets / Comments
- Colour-coded dot per type (violet = booking · blue = ticket · amber = comment)
- **Mark single notification read**
- **Mark all read**
- **Delete single notification** (appears on hover)
- **Clear all notifications** (inline confirm: "Sure? / Yes, clear / Cancel")

---

### Module E — Authentication & Authorization

- **Email/password registration & login** — returns JWT access token
- **Google OAuth 2.0 sign-in** (Authorization Code Flow via Spring Security OAuth2 Client)
- JWT stored in `localStorage`; every API request sends `Authorization: Bearer <token>`
- Server-side session also maintained
- **Roles**: `USER`, `ADMIN`, `TECHNICIAN`
- Admin emails configurable via `ADMIN_EMAILS` env variable
- Route guards: `ProtectedRoute` (must be logged in) · `RoleRoute` (admin-only pages)
- Password hashed with BCrypt before storage

---

### Admin Pages

| Page | Route | What it does |
|---|---|---|
| User Management | `/admin/users` | List all users, change roles |
| Analytics Dashboard | `/admin/analytics` | Charts, stats, CSV export |

---

### Admin Analytics Dashboard (`/admin/analytics`)

- **KPI tiles**: total bookings, active tickets, facilities, users
- **Top 5 most-booked resources** — horizontal bar chart (SVG, no external chart library)
- **Booking status breakdown** — SVG donut chart (Pending / Approved / Rejected)
- **Ticket status breakdown** — SVG donut chart
- **Ticket priority distribution** — horizontal bar chart
- **Peak booking hours heatmap** — 24-cell grid, colour intensity = booking volume
- **Export all bookings as CSV** — one click, streams from backend

---

## 4. REST API Reference

**Base URL (dev):** `http://localhost:5173/api` (via Vite proxy to `http://127.0.0.1:8080`)

### OpenAPI / Swagger UI (interactive docs)

The backend exposes **OpenAPI 3** JSON and **Swagger UI** via **SpringDoc** (`OpenApiConfig.java` defines the API title, JWT **bearerAuth** scheme, and rubric notes). Use these URLs when the Spring Boot app is running on port **8080**:

| Resource | URL |
|---|---|
| **Swagger UI** (main entry) | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| **Legacy redirect** (`/swagger-ui.html` → index) | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| **OpenAPI JSON** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

**Try secured endpoints:** click **Authorize**, paste `Bearer <accessToken>` from `POST /api/auth/login` or `POST /api/auth/register`. HAL responses (`_links`, `_embedded`) appear in the **response body** after **Execute**, even if the schema panel looks generic.

---

### Auth — `/api/auth`

| # | Method | Path | Auth Required | Description |
|---|---|---|---|---|
| 1 | POST | `/register` | Public | Register new user — returns JWT + user |
| 2 | POST | `/login` | Public | Login with email/password — returns JWT + user |
| 3 | GET | `/me` | Authenticated | Current user info |
| 4 | POST | `/logout` | Authenticated | Invalidate session |

---

### Resources — `/api/resources`

| # | Method | Path | Auth Required | Description |
|---|---|---|---|---|
| 5 | GET | `/` | Authenticated | List all resources |
| 6 | GET | `/{id}` | Authenticated | Get single resource |
| 7 | POST | `/` | **Admin** | Create resource |
| 8 | PUT | `/{id}` | **Admin** | Update resource |
| 9 | DELETE | `/{id}` | **Admin** | Delete resource |

---

### Bookings — `/api/bookings`

| # | Method | Path | Auth Required | Description |
|---|---|---|---|---|
| 10 | GET | `/` | Authenticated | List bookings (`?all=true` for admin) |
| 11 | GET | `/{id}` | Authenticated | Get single booking |
| 12 | POST | `/` | Authenticated | Create booking request |
| 13 | PUT | `/{id}/status` | **Admin** | Approve or reject (with reason) |
| 14 | PUT | `/{id}/times` | Authenticated | Update booking time range |
| 15 | POST | `/{id}/cancel` | Authenticated | Cancel own booking |

---

### Maintenance — `/api/maintenance/tickets`

| # | Method | Path | Auth Required | Description |
|---|---|---|---|---|
| 16 | GET | `/` | Authenticated | List all tickets |
| 17 | GET | `/{id}` | Authenticated | Get single ticket |
| 18 | POST | `/` | Authenticated | Create incident ticket |
| 19 | POST | `/{id}/images` | Authenticated | Upload image attachment (max 3) |
| 20 | GET | `/images/{imageId}/file` | Authenticated | Download image file |
| 21 | POST | `/{id}/comments` | Authenticated | Add comment |
| 22 | PUT | `/comments/{commentId}` | Authenticated | Edit own comment |
| 23 | DELETE | `/comments/{commentId}` | Authenticated | Delete own comment |
| 24 | PUT | `/{id}/resolution` | Technician / Admin | Set resolution notes + status |
| 25 | PUT | `/{id}/technician` | **Admin** | Assign technician |
| 26 | POST | `/{id}/reopen` | Authenticated | Reopen a resolved/closed ticket |

---

### Notifications — `/api/notifications`

| # | Method | Path | Auth Required | Description |
|---|---|---|---|---|
| 27 | GET | `/` | Authenticated | List all notifications |
| 28 | GET | `/unread-count` | Authenticated | Unread notification count |
| 29 | PUT | `/{id}/read` | Authenticated | Mark single notification read |
| 30 | PUT | `/read-all` | Authenticated | Mark all notifications read |
| 31 | DELETE | `/{id}` | Authenticated | Delete single notification |
| 32 | DELETE | `/clear-all` | Authenticated | Delete all notifications for user |

---

### Admin — Users `/api/admin/users`

| # | Method | Path | Auth Required | Description |
|---|---|---|---|---|
| 33 | GET | `/` | **Admin** | List all users |
| 34 | PUT | `/{id}/role` | **Admin** | Change a user's role |

---

### Admin — Analytics `/api/admin/analytics`

| # | Method | Path | Auth Required | Description |
|---|---|---|---|---|
| 35 | GET | `/` | **Admin** | Full analytics stats (JSON) |
| 36 | GET | `/export/bookings` | **Admin** | Download all bookings as CSV |

**Total: 36 REST endpoints across 6 controllers**

---

## 5. Database Collections

MongoDB database name: `smartcampus`

| Collection | Entity Class | Key Fields |
|---|---|---|
| `users` | `User` | id, name, email, passwordHash, googleId, picture, role, createdAt |
| `campus_resources` | `CampusResource` | id, name, type, capacity, location, floor, amenities[], status, createdAt, updatedAt |
| `bookings` | `Booking` | id, resourceId, userId, startTime, endTime, status, purpose, decisionReason, createdAt |
| `maintenance_tickets` | `MaintenanceTicket` | id, title, description, priority, status, reporterId, assignedTechnicianId, resolutionNotes, images[], comments[], createdAt, updatedAt |
| `notifications` | `Notification` | id, userId, type, message, relatedEntityType, relatedEntityId, readFlag, createdAt |

### Enums

| Enum | Values |
|---|---|
| `UserRole` | USER, ADMIN, TECHNICIAN |
| `ResourceType` | LECTURE_HALL, LAB, MEETING_ROOM, EQUIPMENT |
| `ResourceStatus` | ACTIVE, OUT_OF_SERVICE |
| `BookingStatus` | PENDING, APPROVED, REJECTED, CANCELLED |
| `TicketPriority` | LOW, MEDIUM, HIGH, CRITICAL |
| `TicketStatus` | OPEN, IN_PROGRESS, RESOLVED, CLOSED, REJECTED |
| `NotificationType` | BOOKING_STATUS, TICKET_UPDATE |

---

## 6. System Architecture (Marking Rubric)

This section summarises how the system is structured for coursework marking: **separation of concerns**, **REST + HATEOAS**, **security**, and **discoverable API documentation**.

### 6.1 High-level system view

The app is a **React SPA** talking to a **Spring Boot REST API** backed by **MongoDB**. In development, the Vite dev server proxies `/api` to the JVM process so the browser avoids CORS issues.

```mermaid
flowchart TB
  subgraph browser [Browser]
    UI[React 18 SPA\nVite :5173]
  end
  subgraph spring [Spring Boot :8080]
    SEC[Spring Security\nJWT + OAuth2]
    CTRL[REST Controllers]
    HAL[HATEOAS Assemblers\nHAL JSON]
    SVC[Services\nbusiness rules]
    REPO[MongoRepositories]
  end
  DB[(MongoDB\nAtlas or local)]
  UI -->|"/api/* proxy"| SEC
  SEC --> CTRL
  CTRL --> HAL
  CTRL --> SVC
  SVC --> REPO
  REPO --> DB
```

### 6.2 Backend layering — examples from this project

| Concern | Role | Concrete examples in repo |
|---|---|---|
| **HTTP / REST** | Map paths and HTTP verbs; return status codes and bodies | `BookingController.java`, `CampusResourceController.java`, `MaintenanceController.java`, `NotificationController.java` |
| **Security** | Authenticate JWT (and OAuth2 session where used); enforce `@PreAuthorize` | `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `JwtService.java` |
| **Application logic** | Validation, workflows, cross-collection reads, side effects (e.g. notifications) | `BookingService.java`, `MaintenanceService.java`, `NotificationService.java` |
| **Persistence** | CRUD and queries on MongoDB documents | `BookingRepository.java`, `MaintenanceTicketRepository.java`, … |
| **Domain model** | Document shape stored in MongoDB | `Booking.java`, `MaintenanceTicket.java`, `User.java`, … |
| **API contracts** | Request/response JSON shapes | `dto/booking/BookingRequest.java`, `dto/maintenance/TicketResponse.java`, … |
| **Cross-cutting errors** | Consistent JSON error payloads | `GlobalExceptionHandler.java`, `ApiException.java` |

Controllers **do not** contain business rules; they delegate to services. Services **do not** expose HTTP details.

### 6.3 HATEOAS (HAL) — examples from this project

**Dependency:** `spring-boot-starter-hateoas` in `backend/pom.xml`.

**Pattern:** DTOs are wrapped in `EntityModel` / `CollectionModel`; **link relations** (`self`, `bookings`, `cancel`, `tickets`, `mark-read`, etc.) are built with `WebMvcLinkBuilder` in dedicated assemblers under:

`backend/src/main/java/com/sliit/smartcampus/hateoas/`

| Assembler | Wraps | Used by controller |
|---|---|---|
| `BookingModelAssembler` | `BookingResponse` | `BookingController` |
| `CampusResourceModelAssembler` | `ResourceResponse` | `CampusResourceController` |
| `MaintenanceTicketModelAssembler` | `TicketResponse` | `MaintenanceController` |
| `TicketCommentModelAssembler` | `TicketCommentResponse` | `MaintenanceController` |
| `TicketImageModelAssembler` | `TicketImageResponse` | `MaintenanceController` |
| `NotificationModelAssembler` | `NotificationResponse` | `NotificationController` |

**Example:** `GET /api/bookings` returns a HAL **collection** (`_embedded` items each with `_links`). **Caching:** many safe `GET`s set `Cache-Control: private, max-age=…` and `Vary: Authorization` so caches stay per-user.

**Frontend:** `frontend/src/api/hateoas.js` exports `unwrapHalCollection()` so pages receive a plain array from `_embedded`; used by `BookingsPage.jsx`, `ResourcesPage.jsx`, `MaintenancePage.jsx`, `NotificationBell.jsx`, and `DashboardPage.jsx`.

### 6.4 API documentation — configuration in repo

| What | Where |
|---|---|
| OpenAPI bean (title, description, JWT scheme) | `backend/.../config/OpenApiConfig.java` |
| SpringDoc + Swagger UI dependency | `backend/pom.xml` — `springdoc-openapi-starter-webmvc-ui` |
| UI path redirect `/swagger-ui.html` | `backend/.../config/WebConfig.java` |
| Permitting `/swagger-ui/**`, `/v3/api-docs` without login | `backend/.../config/SecurityConfig.java` |
| Swagger UI sort order | `backend/src/main/resources/application.yml` — `springdoc.swagger-ui.*` |

**Live links (backend running):** same table as in [§4 OpenAPI / Swagger UI](#openapi--swagger-ui-interactive-docs).

---

## 7. Project Structure

```
PAF/
├── docker-compose.yml                  ← Optional local MongoDB container
├── PROJECT_DETAILS.md                  ← This file
│
├── backend/                            ← Spring Boot REST API
│   ├── .env                            ← Local secrets (never commit to Git)
│   ├── env.sample                      ← Template — copy to .env and fill values
│   ├── pom.xml                         ← Maven dependencies
│   └── src/main/java/com/sliit/smartcampus/
│       ├── SmartCampusApplication.java ← Entry point
│       ├── config/
│       │   ├── AppProperties.java      ← Typed env-var config (CORS, JWT, etc.)
│       │   ├── OAuth2ClientConfig.java ← Conditional Google OAuth2 bean registration
│       │   └── SecurityConfig.java     ← Spring Security filter chain
│       ├── controller/
│       │   ├── AuthController.java
│       │   ├── CampusResourceController.java
│       │   ├── BookingController.java
│       │   ├── MaintenanceController.java
│       │   ├── NotificationController.java
│       │   ├── AdminUserController.java
│       │   └── AdminAnalyticsController.java
│       ├── dto/                        ← Request/Response records
│       │   ├── booking/
│       │   ├── maintenance/
│       │   ├── notification/
│       │   ├── resource/
│       │   ├── admin/
│       │   └── user/
│       ├── entity/
│       │   ├── User.java
│       │   ├── CampusResource.java
│       │   ├── Booking.java
│       │   ├── MaintenanceTicket.java
│       │   ├── Notification.java
│       │   └── enums/                  ← All enum types
│       ├── exception/
│       │   ├── ApiException.java
│       │   └── GlobalExceptionHandler.java
│       ├── hateoas/                    ← HAL link assemblers (HATEOAS)
│       │   ├── BookingModelAssembler.java
│       │   ├── CampusResourceModelAssembler.java
│       │   ├── MaintenanceTicketModelAssembler.java
│       │   ├── TicketCommentModelAssembler.java
│       │   ├── TicketImageModelAssembler.java
│       │   └── NotificationModelAssembler.java
│       ├── repository/                 ← MongoRepository interfaces
│       ├── security/
│       │   ├── JwtService.java
│       │   ├── JwtAuthenticationFilter.java
│       │   ├── CampusUserDetails.java
│       │   ├── CampusUserDetailsService.java
│       │   ├── CampusOAuth2UserService.java
│       │   ├── OAuth2SuccessHandler.java
│       │   ├── OAuth2FailureHandler.java
│       │   └── CurrentUserService.java
│       └── service/
│           ├── AuthService.java
│           ├── CampusResourceService.java
│           ├── BookingService.java
│           ├── BookingOverlapChecker.java
│           ├── MaintenanceService.java
│           ├── FileStorageService.java
│           ├── NotificationService.java
│           ├── AdminUserService.java
│           └── AdminAnalyticsService.java
│
└── frontend/                           ← React SPA
    ├── index.html
    ├── package.json
    ├── vite.config.js                  ← Proxy /api → 127.0.0.1:8080
    ├── tailwind.config.js
    └── src/
        ├── main.jsx
        ├── App.jsx                     ← Routes + guards
        ├── api/
        │   ├── client.js              ← Fetch wrapper with JWT Bearer header
        │   └── hateoas.js             ← unwrapHalCollection() for HAL _embedded lists
        ├── context/
        │   └── AuthContext.jsx        ← Global auth state + JWT management
        ├── layouts/
        │   └── DashboardLayout.jsx    ← Sidebar + Header + Outlet
        ├── components/
        │   ├── Sidebar.jsx
        │   ├── Header.jsx
        │   ├── NotificationBell.jsx
        │   ├── StatCard.jsx
        │   └── CircularProgress.jsx
        └── pages/
            ├── LoginPage.jsx
            ├── RegisterPage.jsx
            ├── DashboardPage.jsx
            ├── ResourcesPage.jsx
            ├── BookingsPage.jsx
            ├── MaintenancePage.jsx
            ├── AdminUsersPage.jsx
            └── AdminAnalyticsPage.jsx
```

---

## 8. CRUD Work Allocation — 4 Members

Each member covers at least 4 endpoints across GET, POST, PUT/PATCH, DELETE as required by the assignment.

---

### Member 1 — Facilities Catalogue + Resource Management

**Backend files:**
- `CampusResource.java` (entity) — fields: name, type, capacity, location, **floor**, **amenities[]**, status
- `CampusResourceRepository.java`
- `CampusResourceService.java` — create, findAll, findById, update, delete
- `CampusResourceController.java`
- `ResourceRequest.java`, `ResourceResponse.java` (DTOs)
- Enums: `ResourceType`, `ResourceStatus`

**Endpoints owned:**

| Method | Path | Description |
|---|---|---|
| GET | `/api/resources` | List all resources |
| GET | `/api/resources/{id}` | Get single resource |
| POST | `/api/resources` | Create resource (admin) |
| PUT | `/api/resources/{id}` | Update resource (admin) |
| DELETE | `/api/resources/{id}` | Delete resource (admin) |

**Frontend files:**
- `ResourcesPage.jsx` — full CRUD UI with search bar, type tab filters, capacity bar chart, floor field, amenities pill tags, admin edit/delete

**CRUD coverage:** ✅ Create · ✅ Read · ✅ Update · ✅ Delete

---

### Member 2 — Booking Workflow + Conflict Checking

**Backend files:**
- `Booking.java` (entity) — fields: resourceId, userId, startTime, endTime, status, purpose, decisionReason
- `BookingRepository.java`
- `BookingService.java` — create (with overlap check), list, getById, updateStatus, updateTimes, cancel
- `BookingOverlapChecker.java` — prevents double-booking
- `BookingController.java`
- `BookingRequest.java`, `BookingResponse.java`, `BookingStatusUpdateRequest.java`, `BookingTimeUpdateRequest.java` (DTOs)
- Enum: `BookingStatus`

**Endpoints owned:**

| Method | Path | Description |
|---|---|---|
| GET | `/api/bookings` | List bookings (own / all) |
| GET | `/api/bookings/{id}` | Get single booking |
| POST | `/api/bookings` | Create booking request |
| PUT | `/api/bookings/{id}/status` | Approve/Reject (admin) |
| PUT | `/api/bookings/{id}/times` | Update booking times |
| POST | `/api/bookings/{id}/cancel` | Cancel booking |

**Frontend files:**
- `BookingsPage.jsx` — booking form, status filter tabs, "my bookings" toggle, approve/reject with reason, **ICS calendar export**, **QR check-in modal** for approved bookings

**CRUD coverage:** ✅ Create · ✅ Read · ✅ Update · ✅ Delete (cancel as status delete)

---

### Member 3 — Maintenance Tickets + File Attachments + Technician

**Backend files:**
- `MaintenanceTicket.java` (entity) — embedded `EmbeddedTicketImage[]`, `EmbeddedTicketComment[]`
- `MaintenanceTicketRepository.java`
- `MaintenanceService.java` — create, list, getById, addImage, comments CRUD, resolution, assignTechnician, **reopen**
- `FileStorageService.java` — stores uploaded images on disk
- `MaintenanceController.java`
- All maintenance DTOs: `TicketRequest`, `TicketResponse`, `TicketCommentRequest`, `TicketCommentResponse`, `TicketResolutionRequest`, `TicketImageResponse`, `AssignTechnicianRequest`
- Enums: `TicketStatus`, `TicketPriority`

**Endpoints owned:**

| Method | Path | Description |
|---|---|---|
| GET | `/api/maintenance/tickets` | List all tickets |
| GET | `/api/maintenance/tickets/{id}` | Get single ticket |
| POST | `/api/maintenance/tickets` | Create ticket |
| POST | `/api/maintenance/tickets/{id}/images` | Upload image |
| GET | `/api/maintenance/tickets/images/{imageId}/file` | Download image |
| POST | `/api/maintenance/tickets/{id}/comments` | Add comment |
| PUT | `/api/maintenance/tickets/comments/{commentId}` | Edit own comment |
| DELETE | `/api/maintenance/tickets/comments/{commentId}` | Delete own comment |
| PUT | `/api/maintenance/tickets/{id}/resolution` | Set resolution (tech/admin) |
| PUT | `/api/maintenance/tickets/{id}/technician` | Assign technician (admin) |
| POST | `/api/maintenance/tickets/{id}/reopen` | Reopen resolved/closed ticket |

**Frontend files:**
- `MaintenancePage.jsx` — ticket form, **live SLA elapsed-time chip**, **reopen button**, status/priority filter bars, image upload, comments, resolution panel

**CRUD coverage:** ✅ Create · ✅ Read · ✅ Update · ✅ Delete (comments)

---

### Member 4 — Notifications + Auth + Admin Analytics + OAuth / JWT

**Backend files:**
- `Notification.java` (entity), `NotificationRepository.java`
- `NotificationService.java` — listForUser, unreadCount, markRead, markAllRead, **delete**, **clearAll**
- `NotificationController.java`
- `User.java` (entity), `UserRepository.java`
- `AuthService.java`, `AuthController.java` — register, login, me, logout
- `AdminUserService.java`, `AdminUserController.java` — list users, change role
- `AdminAnalyticsService.java`, `AdminAnalyticsController.java` — stats JSON + bookings CSV
- `JwtService.java` — generate/validate HS256 tokens (JJWT)
- `JwtAuthenticationFilter.java` — Bearer token filter
- `CampusUserDetails.java`, `CampusUserDetailsService.java`
- `CampusOAuth2UserService.java` — Google → local User upsert
- `OAuth2SuccessHandler.java` — issues JWT on OAuth success, redirects to frontend
- `OAuth2FailureHandler.java` — redirects to `/login?error=...`
- `SecurityConfig.java`, `OAuth2ClientConfig.java`, `AppProperties.java`
- DTOs: `AuthResponse`, `LoginRequest`, `RegisterRequest`, `UserResponse`, `UserRoleUpdateRequest`, `NotificationResponse`
- Enum: `NotificationType`

**Endpoints owned:**

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register — returns JWT |
| POST | `/api/auth/login` | Login — returns JWT |
| GET | `/api/auth/me` | Current user |
| POST | `/api/auth/logout` | Logout |
| GET | `/api/notifications` | List notifications |
| GET | `/api/notifications/unread-count` | Unread count |
| PUT | `/api/notifications/{id}/read` | Mark one read |
| PUT | `/api/notifications/read-all` | Mark all read |
| DELETE | `/api/notifications/{id}` | Delete one notification |
| DELETE | `/api/notifications/clear-all` | Delete all notifications |
| GET | `/api/admin/users` | List all users |
| PUT | `/api/admin/users/{id}/role` | Change user role |
| GET | `/api/admin/analytics` | Usage stats |
| GET | `/api/admin/analytics/export/bookings` | Export bookings CSV |

**Frontend files:**
- `LoginPage.jsx` — email/password form, Google sign-in button, error handling
- `RegisterPage.jsx` — registration form
- `NotificationBell.jsx` — bell, panel, type filter tabs, mark read, **delete per item**, **clear all with confirm**
- `AdminUsersPage.jsx` — user list, role selector
- `AdminAnalyticsPage.jsx` — KPI tiles, bar charts, donut charts, peak-hours heatmap, CSV export
- `AuthContext.jsx` — JWT storage, OAuth redirect token capture

**CRUD coverage:** ✅ Create (auth/register) · ✅ Read (notifications, users, analytics) · ✅ Update (mark read, change role) · ✅ Delete (delete notification, clear all)

---

## 9. How to Run the Project

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Java JDK | 17 |
| Node.js | 18 |
| npm | 9 |
| MongoDB | Atlas account **or** Docker Desktop |

---

### Step 1 — Database setup

**Option A — MongoDB Atlas (already configured)**

Your Atlas connection string is in `backend/.env`. No extra steps needed.

**Option B — Local Docker container**

```powershell
cd f:\Projects\PAF
docker-compose up -d
```

Then set in `backend/.env`:
```
MONGODB_URI=mongodb://localhost:27017/smartcampus
```

---

### Step 2 — Configure `backend/.env`

Open `f:\Projects\PAF\backend\.env` and ensure these values are set:

```env
# MongoDB
MONGODB_URI=mongodb+srv://NewUser:PAF123@educare.fa1yvuh.mongodb.net/smartcampus

# Comma-separated emails that become ADMIN on first login
ADMIN_EMAILS=your@email.com

# JWT — minimum 32 characters, random string
JWT_SECRET=replace-with-a-long-random-secret-here
JWT_EXPIRATION_SECONDS=86400

# Google OAuth2 — leave blank to skip Google login
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://localhost:5173/login/oauth2/code/google
```

> ⚠️ **Save `.env` as UTF-8 without BOM.**
> In VS Code: click the encoding in the status bar → "Save with Encoding" → choose **UTF-8** (not "UTF-8 with BOM").

---

### Step 3 — Start the backend

```powershell
cd f:\Projects\PAF\backend
.\mvnw.cmd spring-boot:run
```

Wait for the log line:
```
Started SmartCampusApplication in X.XXX seconds
```

API is now running at **`http://localhost:8080`**

---

### Step 4 — Start the frontend

Open a **second terminal**:

```powershell
cd f:\Projects\PAF\frontend
npm install
npm run dev
```

App is now running at **`http://localhost:5173`**

---

### Step 5 — Open and test

Go to **`http://localhost:5173`** in your browser.

| Action | How |
|---|---|
| Register as Admin | Register with the email listed in `ADMIN_EMAILS` |
| Register as User | Register with any other email |
| Become Technician | Admin changes role on `/admin/users` page |
| Google sign-in | Only works when `GOOGLE_CLIENT_ID` is filled in `.env` |

---

### Build for Production

```powershell
# Build frontend static files
cd f:\Projects\PAF\frontend
npm run build
# Output: frontend/dist/

# Package backend as executable JAR
cd f:\Projects\PAF\backend
.\mvnw.cmd package -DskipTests
java -jar target\smart-campus-hub-1.0.0-SNAPSHOT.jar
```

---

### Useful URLs

| URL | Purpose |
|---|---|
| `http://localhost:5173` | React application |
| `http://localhost:5173/resources` | Facilities page |
| `http://localhost:5173/bookings` | Bookings page |
| `http://localhost:5173/maintenance` | Maintenance tickets |
| `http://localhost:5173/admin/users` | Admin — user management |
| `http://localhost:5173/admin/analytics` | Admin — analytics dashboard |
| `http://localhost:8080/api/...` | REST API direct access |
| `http://localhost:5173/api/...` | REST API via Vite proxy (use this in dev) |
| [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | Swagger UI (OpenAPI) |
| [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | OpenAPI 3 JSON |

---

## 10. Environment Variables

All variables live in `backend/.env` (copy from `backend/env.sample`).

| Variable | Required | Default | Description |
|---|---|---|---|
| `MONGODB_URI` | Yes | `mongodb://localhost:27017/smartcampus` | MongoDB connection string |
| `ADMIN_EMAILS` | Yes | _(empty)_ | Comma-separated emails auto-promoted to ADMIN |
| `JWT_SECRET` | Yes | dev default (insecure) | HS256 signing secret — min 32 characters |
| `JWT_EXPIRATION_SECONDS` | No | `86400` (24 h) | JWT validity window in seconds |
| `CORS_ORIGINS` | No | `http://localhost:5173` | Allowed CORS origins |
| `FRONTEND_URL` | No | `http://localhost:5173` | Used in OAuth redirect |
| `UPLOAD_DIR` | No | `uploads` | Directory for ticket image files |
| `GOOGLE_CLIENT_ID` | No | _(empty)_ | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | No | _(empty)_ | Google OAuth2 client secret |
| `GOOGLE_REDIRECT_URI` | No | `http://localhost:5173/login/oauth2/code/google` | OAuth callback URI |

---

## 11. Architecture — How Layers Communicate

### Layer Responsibility Summary

| Layer | File Pattern | Responsibility | Talks To |
|---|---|---|---|
| **Entity** | `entity/*.java` | MongoDB document shape — field definitions only, no logic | Repository only |
| **Repository** | `repository/*Repository.java` | Auto-generated database queries (Spring Data) | MongoDB directly |
| **Service** | `service/*Service.java` | **ALL business logic**, validation, rules, cross-collection lookups | Repository + other Services |
| **DTO (Request)** | `dto/*/XxxRequest.java` | Shape of JSON arriving **from** the frontend | Controller receives it |
| **DTO (Response)** | `dto/*/XxxResponse.java` | Shape of JSON sent **to** the frontend | Controller returns it |
| **Controller** | `controller/*Controller.java` | Route HTTP → Service, security annotations, zero business logic; HATEOAS endpoints return `EntityModel` / `CollectionModel` | Service + assemblers (`hateoas/*Assembler.java`), never Repository |
| **Exception Handler** | `exception/GlobalExceptionHandler.java` | Catches any thrown exception, converts to JSON error response | Entire application |
| **Frontend `client.js`** | `api/client.js` | Fetch wrapper — adds JWT Bearer header, parses responses, handles errors | All React pages use it |
| **React Pages** | `pages/*.jsx` | UI state, calls `apiGet` / `apiSend`, renders data | `client.js` only |

---

### Data Flow — Creating a Booking (End to End)

```
REACT (BookingsPage.jsx)
  User submits form
  → apiSend('/bookings', 'POST', { resourceId, startTime, endTime, purpose })
  → adds Authorization: Bearer <jwt> header, JSON.stringify(body)
        ↓ HTTP POST
VITE PROXY
  Forwards /api/* → http://127.0.0.1:8080/api/*
        ↓
SPRING SECURITY — JwtAuthenticationFilter
  Reads JWT → validates → loads User from DB → sets SecurityContext
  If invalid → returns 401 immediately
        ↓
@PreAuthorize("isAuthenticated()") on Controller method
  Checks SecurityContext → user present → passes
  If not logged in → returns 403
        ↓
CONTROLLER — BookingController.create()
  @RequestBody → Spring parses JSON → BookingRequest record
  Delegates to: bookingService.create(request, currentUser)
        ↓
SERVICE — BookingService.create()
  1. Validates resourceId not null              (throws 400 if missing)
  2. Validates startTime < endTime              (throws 400 if wrong)
  3. Calls campusResourceService.getEntity()    (throws 404 if not found)
  4. Checks resource.status == ACTIVE           (throws 400 if unavailable)
  5. bookingOverlapChecker.existsOverlapping()  (throws 409 if time clash)
  6. Builds Booking entity using Builder pattern
  7. b.touchCreated() → sets createdAt = now
  8. bookingRepository.save(b)                  → writes to MongoDB
  9. toResponse(b) → looks up resourceName + userEmail from other collections
        ↓
REPOSITORY — BookingRepository.save()
  Spring Data serialises Booking entity → MongoDB document
  Inserts into "bookings" collection, returns saved entity with _id
        ↓
MONGODB
  Stores document:
  { _id, resourceId, userId, startTime, endTime, status: "PENDING", purpose, createdAt }
        ↑ returns saved document
SERVICE converts Entity → Response DTO
  BookingResponse { id, resourceName, userEmail, status, startTime, endTime, ... }
        ↑ returned to Controller
CONTROLLER returns `EntityModel<BookingResponse>` (HAL: DTO fields + `_links`)
  Spring HATEOAS serialises to JSON → HTTP 201 Created
        ↑ HTTP response
REACT (client.js → handle())
  Parses JSON → returns booking object to page
  BookingsPage calls load() → list refreshes → user sees new PENDING booking
```

---

### Why Each Layer Exists (One-Line Reason Each)

| Layer | Why it exists |
|---|---|
| **Entity** | Defines what MongoDB stores — the source of truth for the data shape |
| **Repository** | Hides all database query code so services never write SQL/MQL |
| **Service** | Keeps business rules in one place — controllers and other services call it, not the DB |
| **Request DTO** | Lets Spring auto-parse incoming JSON and validates only the fields you expect |
| **Response DTO** | Controls exactly what the frontend sees — hides internal IDs, adds enriched fields |
| **Controller** | Single responsibility: receive HTTP, call service, return result — nothing else |
| **Exception Handler** | One central place to convert any error into a consistent JSON response format |
| **`client.js`** | Keeps JWT injection and error handling in one file so pages stay clean |

---

## 12. Key Design Decisions

| Decision | Reason |
|---|---|
| **JWT + Session dual auth** | Session for browser SSO; JWT Bearer for API / SPA clients and OAuth2 callback |
| **Conditional OAuth2 config** | App starts without Google credentials — `OAuth2ClientConfig` only registers beans when `GOOGLE_CLIENT_ID` is present |
| **Vite proxy to IPv4 loopback** | Node.js on Windows resolves `localhost` to `::1` (IPv6); Spring Boot binds to `127.0.0.1` — using the proxy avoids CORS and IPv6 mismatches |
| **Embedded comments/images in `MaintenanceTicket`** | One document = one ticket with all its child data; avoids extra joins in MongoDB |
| **`BookingOverlapChecker` service** | Isolated overlap-detection logic; queries by resourceId and time range, reusable |
| **`NotificationService` as cross-module event bus** | Booking and Maintenance services call it after state changes to decouple notification logic |
| **Amenities as `List<String>`** | Flexible tag list stored in MongoDB array field; no schema migration needed to add new amenity types |
| **ICS generated client-side** | Avoids an extra round-trip; standard RFC 5545 format works with Google Calendar, Outlook, Apple Calendar |
| **QR code in modal (client-side)** | Uses `qrcode` npm package to render canvas QR — no backend endpoint needed |
| **SLA timer as a React hook** | `useElapsedTime` recalculates every minute and colours the chip based on hours elapsed |
| **Analytics charts pure SVG** | No external chart library dependency; keeps bundle size small |
| **Notification type inferred from message text** | Avoids changing the `NotificationType` enum or database schema; works for all existing notifications |
| **Spring HATEOAS + HAL on core collections** | Bookings, resources, maintenance tickets, and notifications expose `_links` / `_embedded` for rubric REST maturity; clients use `unwrapHalCollection` where lists are HAL |
| **SpringDoc on `/swagger-ui` + `/v3/api-docs`** | Interactive API exploration and OpenAPI export without hand-maintaining a separate spec file |
