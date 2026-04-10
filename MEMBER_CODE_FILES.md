# Member → Feature → Code File Locations

This document maps each of the **four team members** (as used in the IT3030 PAF allocation) to the **features** they own and the **exact file paths** in this repository.

All paths are relative to the project root: **`PAF/`** (same folder as this file).

---

## Quick reference

| Member | Feature area | Main UI page | Main API controller |
|--------|----------------|--------------|---------------------|
| **1** | Facilities / catalogue | `frontend/src/pages/ResourcesPage.jsx` | `CampusResourceController.java` |
| **2** | Bookings / workflow | `frontend/src/pages/BookingsPage.jsx` | `BookingController.java` |
| **3** | Maintenance / tickets | `frontend/src/pages/MaintenancePage.jsx` | `MaintenanceController.java` |
| **4** | Auth, notifications, admin, JWT, OAuth | `LoginPage`, `RegisterPage`, `NotificationBell`, `AdminUsersPage`, `AdminAnalyticsPage` + shared `AuthContext` | `AuthController`, `NotificationController`, `AdminUserController`, `AdminAnalyticsController` |

### CRUD at a glance (4 members)

Each member’s work spans **Create, Read, Update, Delete** across REST (assignment-style “at least four endpoint types”). Primary entity vs. how Delete is expressed:

| Member | Primary resource | Create | Read | Update | Delete |
|--------|------------------|--------|------|--------|--------|
| **1** | `CampusResource` | POST `/api/resources` | GET list + GET by id | PUT `/api/resources/{id}` | DELETE `/api/resources/{id}` |
| **2** | `Booking` | POST `/api/bookings` | GET list + GET by id | PUT status, PUT times | POST `/api/bookings/{id}/cancel` (lifecycle) |
| **3** | `MaintenanceTicket` + **comments** | POST tickets (+ images, comments) | GET list + GET by id | PUT resolution, technician, PUT comment | DELETE comment; reopen via POST |
| **4** | `User` / `Notification` / admin views | POST register (JWT) | GET me, notifications, admin users, analytics | PUT mark read, PUT role | DELETE notification(s) |

---

## Member 1 — Facilities & assets catalogue

**Feature:** CRUD for facilities (lecture halls, labs, meeting rooms, equipment), floor, amenities, search/filter.

### CRUD & REST endpoints (base `/api/resources`)

| Method | Path | Action |
|--------|------|--------|
| GET | `/api/resources` | List all |
| GET | `/api/resources/{id}` | Read one |
| POST | `/api/resources` | Create (admin) |
| PUT | `/api/resources/{id}` | Update (admin) |
| DELETE | `/api/resources/{id}` | Delete (admin) |

**CRUD coverage:** Create · Read · Update · Delete on the **resource** entity.

### Backend (`backend/src/main/java/com/sliit/smartcampus/`)

| Role | Path |
|------|------|
| REST entry | `controller/CampusResourceController.java` |
| Business logic | `service/CampusResourceService.java` |
| DB access | `repository/CampusResourceRepository.java` |
| Mongo document | `entity/CampusResource.java` |
| Request / response DTOs | `dto/resource/ResourceRequest.java`, `dto/resource/ResourceResponse.java` |
| Enums | `entity/enums/ResourceType.java`, `entity/enums/ResourceStatus.java` |

### Frontend

| Role | Path |
|------|------|
| Main screen | `frontend/src/pages/ResourcesPage.jsx` |
| Routing | `frontend/src/App.jsx` (route `resources`) |
| Navigation link | `frontend/src/components/Sidebar.jsx` (Facilities) |

---

## Member 2 — Booking management & conflict checking

**Feature:** Booking requests, approve/reject, cancel, overlap checking, ICS export & QR (UI), filters.

### CRUD & REST endpoints (base `/api/bookings`)

| Method | Path | Action |
|--------|------|--------|
| GET | `/api/bookings` | List (scope by role / “my bookings”) |
| GET | `/api/bookings/{id}` | Read one |
| POST | `/api/bookings` | Create request |
| PUT | `/api/bookings/{id}/status` | Approve / reject (admin) |
| PUT | `/api/bookings/{id}/times` | Reschedule times |
| POST | `/api/bookings/{id}/cancel` | Cancel (lifecycle end) |

**CRUD coverage:** Create · Read · Update · Delete, where **Delete** is expressed as **cancel** (booking row may remain with cancelled status).

### Backend (`backend/src/main/java/com/sliit/smartcampus/`)

| Role | Path |
|------|------|
| REST entry | `controller/BookingController.java` |
| Business logic | `service/BookingService.java` |
| Overlap rules | `service/BookingOverlapChecker.java` |
| DB access | `repository/BookingRepository.java` |
| Mongo document | `entity/Booking.java` |
| DTOs | `dto/booking/BookingRequest.java`, `BookingResponse.java`, `BookingStatusUpdateRequest.java`, `BookingTimeUpdateRequest.java` |
| Enum | `entity/enums/BookingStatus.java` |

### Frontend

| Role | Path |
|------|------|
| Main screen | `frontend/src/pages/BookingsPage.jsx` |
| Routing | `frontend/src/App.jsx` (route `bookings`) |
| Navigation link | `frontend/src/components/Sidebar.jsx` (Bookings) |

> **Note:** Approving/rejecting triggers notifications via `NotificationService` (Member 4’s service), called from `BookingService`.

---

## Member 3 — Maintenance tickets, images, technician workflow

**Feature:** Tickets, comments, image upload (disk storage), resolution, reopen, SLA UI, filters.

### CRUD & REST endpoints (base `/api/maintenance/tickets`)

| Method | Path | Action |
|--------|------|--------|
| GET | `/api/maintenance/tickets` | List tickets |
| GET | `/api/maintenance/tickets/{id}` | Read one |
| POST | `/api/maintenance/tickets` | Create ticket |
| POST | `/api/maintenance/tickets/{id}/images` | Add image (multipart) |
| GET | `/api/maintenance/tickets/images/{imageId}/file` | Download image |
| POST | `/api/maintenance/tickets/{id}/comments` | Create comment |
| PUT | `/api/maintenance/tickets/comments/{commentId}` | Update own comment |
| DELETE | `/api/maintenance/tickets/comments/{commentId}` | Delete own comment |
| PUT | `/api/maintenance/tickets/{id}/resolution` | Resolve (tech/admin) |
| PUT | `/api/maintenance/tickets/{id}/technician` | Assign technician |
| POST | `/api/maintenance/tickets/{id}/reopen` | Reopen closed ticket |

**CRUD coverage:** Full CRUD on **comments**; ticket **Create / Read / Update**; **Delete** for comments; ticket removal is not a hard DELETE endpoint (use status / reopen workflow).

### Backend (`backend/src/main/java/com/sliit/smartcampus/`)

| Role | Path |
|------|------|
| REST entry | `controller/MaintenanceController.java` |
| Business logic | `service/MaintenanceService.java` |
| File storage (uploads folder) | `service/FileStorageService.java` |
| DB access | `repository/MaintenanceTicketRepository.java` |
| Mongo document + embedded images/comments | `entity/MaintenanceTicket.java` |
| DTOs | `dto/maintenance/TicketRequest.java`, `TicketResponse.java`, `TicketCommentRequest.java`, `TicketCommentResponse.java`, `TicketImageResponse.java`, `TicketResolutionRequest.java`, `AssignTechnicianRequest.java` |
| Enums | `entity/enums/TicketStatus.java`, `entity/enums/TicketPriority.java` |

### Frontend

| Role | Path |
|------|------|
| Main screen | `frontend/src/pages/MaintenancePage.jsx` |
| Routing | `frontend/src/App.jsx` (route `maintenance`) |
| Navigation link | `frontend/src/components/Sidebar.jsx` (Maintenance) |

> Uploaded images are stored on disk under `UPLOAD_DIR` (default `uploads/tickets/{ticketId}/...`). Paths are in MongoDB; binary is **not** in the database.

> **Note:** Ticket updates trigger notifications via `NotificationService` (Member 4).

---

## Member 4 — Authentication, notifications, admin users, analytics, JWT, Google OAuth

**Feature:** Register/login, JWT, optional Google sign-in, notification list/delete/delete-all, admin user roles, analytics + CSV.

### CRUD & REST endpoints

**Auth** (`/api/auth`)

| Method | Path | Action |
|--------|------|--------|
| POST | `/api/auth/register` | Create user + JWT |
| POST | `/api/auth/login` | Session via JWT |
| POST | `/api/auth/auto-login` | Demo user + JWT (optional) |
| GET | `/api/auth/me` | Read current user |

Logout: frontend clears stored JWT and navigates to Spring Security **`GET /logout`** (see `SecurityConfig.java`), not under `/api/auth`.

**Notifications** (`/api/notifications`)

| Method | Path | Action |
|--------|------|--------|
| GET | `/api/notifications` | List |
| GET | `/api/notifications/unread-count` | Read count |
| PUT | `/api/notifications/{id}/read` | Update one |
| PUT | `/api/notifications/read-all` | Update all |
| DELETE | `/api/notifications/{id}` | Delete one |
| DELETE | `/api/notifications/clear-all` | Delete all |

**Admin users** (`/api/admin/users`)

| Method | Path | Action |
|--------|------|--------|
| GET | `/api/admin/users` | List users |
| PUT | `/api/admin/users/{id}/role` | Update role |

**Admin analytics** (`/api/admin/analytics`)

| Method | Path | Action |
|--------|------|--------|
| GET | `/api/admin/analytics` | Read stats JSON |
| GET | `/api/admin/analytics/export/bookings` | Read CSV export |

**CRUD coverage:** **Create** (register); **Read** (me, notifications, users, analytics); **Update** (mark read, role); **Delete** (notification, clear all). End-user **profile** has no public DELETE-user endpoint in this API (admin-focused).

### Backend — Auth & user

| Role | Path |
|------|------|
| REST entry | `controller/AuthController.java` |
| Business logic | `service/AuthService.java` |
| User persistence | `entity/User.java`, `repository/UserRepository.java` |
| DTOs | `dto/user/RegisterRequest.java`, `LoginRequest.java`, `UserResponse.java`, `AuthResponse.java` |
| Enum | `entity/enums/UserRole.java` |
| Spring Security user | `security/CampusUserDetails.java`, `security/CampusUserDetailsService.java`, `security/CurrentUserService.java` |
| JWT | `security/JwtService.java`, `security/JwtAuthenticationFilter.java` |
| Google OAuth | `config/OAuth2ClientConfig.java`, `security/CampusOAuth2UserService.java`, `security/OAuth2SuccessHandler.java`, `security/OAuth2FailureHandler.java` |
| Security chain | `config/SecurityConfig.java` |
| App config (JWT secret, admin emails, CORS, etc.) | `config/AppProperties.java`, `backend/src/main/resources/application.yml`, `backend/.env` (local) |

### Backend — Notifications

| Role | Path |
|------|------|
| REST entry | `controller/NotificationController.java` |
| Business logic | `service/NotificationService.java` |
| DB access | `repository/NotificationRepository.java` |
| Mongo document | `entity/Notification.java` |
| DTO | `dto/notification/NotificationResponse.java` |
| Enum | `entity/enums/NotificationType.java` |

### Backend — Admin users

| Role | Path |
|------|------|
| REST entry | `controller/AdminUserController.java` |
| Business logic | `service/AdminUserService.java` |
| DTO | `dto/admin/UserRoleUpdateRequest.java` |

### Backend — Admin analytics

| Role | Path |
|------|------|
| REST entry | `controller/AdminAnalyticsController.java` |
| Business logic | `service/AdminAnalyticsService.java` |

### Frontend — Auth & tokens

| Role | Path |
|------|------|
| Login UI | `frontend/src/pages/LoginPage.jsx` |
| Register UI | `frontend/src/pages/RegisterPage.jsx` |
| Global auth state + JWT | `frontend/src/context/AuthContext.jsx` |
| HTTP + `Authorization: Bearer` | `frontend/src/api/client.js` |

### Frontend — Notifications

| Role | Path |
|------|------|
| Bell + panel | `frontend/src/components/NotificationBell.jsx` |
| Mounted in header | `frontend/src/components/Header.jsx` |

### Frontend — Admin

| Role | Path |
|------|------|
| User management UI | `frontend/src/pages/AdminUsersPage.jsx` |
| Analytics UI | `frontend/src/pages/AdminAnalyticsPage.jsx` |
| Routes + `RoleRoute` guard | `frontend/src/App.jsx` (`admin/users`, `admin/analytics`) |
| Sidebar admin links | `frontend/src/components/Sidebar.jsx` (Admin section) |

---

## Shared across members (everyone should know)

These files are used by multiple features; coordinate before large changes.

| Path | Why shared |
|------|------------|
| `backend/.../SmartCampusApplication.java` | App entry; dotenv load |
| `backend/.../exception/GlobalExceptionHandler.java` | All REST errors → JSON |
| `backend/.../exception/ApiException.java` | Domain errors from services |
| `backend/.../config/WebConfig.java` | CORS / web tweaks |
| `frontend/src/App.jsx` | All routes and route guards |
| `frontend/src/layouts/DashboardLayout.jsx` | Shell around main pages |
| `frontend/src/components/Header.jsx` | Top bar + notification bell |
| `frontend/src/components/Sidebar.jsx` | All nav links |
| `frontend/src/pages/DashboardPage.jsx` | Home dashboard (aggregates stats from several APIs) |
| `frontend/vite.config.js` | Dev server proxy to backend |
| `frontend/package.json` | Frontend dependencies |

---

## Optional assets / config (not Java/JS source)

| Path | Purpose |
|------|---------|
| `backend/pom.xml` | Maven dependencies |
| `backend/env.sample` | Environment variable template |
| `backend/.env` | Local secrets (do not commit) |
| `docker-compose.yml` | Optional local MongoDB |
| `PROJECT_DETAILS.md` | Full project documentation |

---

## Folder convention summary

```
PAF/
├── backend/src/main/java/com/sliit/smartcampus/
│   ├── controller/     ← HTTP layer (Member 1–4 by feature)
│   ├── service/        ← Business logic
│   ├── repository/     ← MongoDB access
│   ├── entity/         ← MongoDB document shapes
│   ├── dto/            ← JSON request/response types
│   ├── security/       ← Mostly Member 4 (JWT, OAuth, UserDetails)
│   ├── config/         ← Security, OAuth beans, properties
│   └── exception/      ← Shared error handling
│
└── frontend/src/
    ├── api/client.js   ← All API calls (shared)
    ├── context/        ← Auth (Member 4)
    ├── pages/          ← One main page per major feature
    ├── components/     ← Reusable UI (NotificationBell = Member 4)
    └── layouts/        ← Dashboard shell (shared)
```

---

*Last updated to match the Smart Campus Operations Hub codebase layout.*
