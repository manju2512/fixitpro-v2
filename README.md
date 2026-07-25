# FixitPro v2

Production-grade home-repair booking platform (Java Spring Boot + React) with an AI booking assistant.

Rebuilt from the ground up from an earlier PHP prototype (FixitPro v1), with the schema, security, and error-handling flaws of that version fixed by design rather than patched.

## Architecture

A **targeted microservices split** rather than a full monolith or a full microservices sprawl — chosen deliberately:

- **`core-service`** — the transactional heart: auth, users, service catalog, technicians, reservations, reviews. These all participate in the same transactions (booking a reservation touches user + technician + service_type together), so keeping them as one modular monolith avoids solving distributed transactions for no real benefit.
- **`ai-chat-service`** — the AI booking assistant, split out because it has a genuinely different profile: bursty/latency-sensitive traffic, calls an external LLM API, and doesn't need direct DB access. It drives bookings the same way any external client would — by calling `core-service`'s REST API (with tool-calling), which still enforces all validation/auth. This is a real architectural boundary, not a split for its own sake.

Note:"free Groq tier is rate-limited; upgrade to Dev Tier for production load"


```
                    ┌──────────────────┐
   React SPA  ───▶  │   core-service    │───▶ MySQL
        │            │  (auth, booking,  │───▶ Redis
        │            │   reviews, etc.)  │
        │            └──────────────────┘
        │                     ▲
        │                     │ REST (tool-calling)
        └──────────────────▶  │
                    ┌──────────────────┐
                    │  ai-chat-service  │───▶ Groq API
                    │  (stateless,      │
                    │   no DB access)   │
                    └──────────────────┘
```

Both services validate the same JWTs (`ai-chat-service` verifies tokens issued by `core-service`'s auth — it never issues its own).

## Monorepo structure

```
fixitpro-v2/
├── services/
│   ├── core-service/          # Spring Boot - auth, booking domain
│   │   ├── src/main/java/com/fixitpro/
│   │   │   ├── config/          # Security, CORS, beans
│   │   │   ├── security/         # JWT service, filter, UserDetails
│   │   │   ├── auth/               # Signup/login/refresh
│   │   │   ├── domain/               # Feature-packaged entities
│   │   │   └── common/                 # Exceptions, shared response types
│   │   ├── src/main/resources/
│   │   │   ├── db/migration/       # Flyway SQL migrations
│   │   │   └── application*.yml
│   │   └── Dockerfile
│   └── ai-chat-service/       # Spring Boot (WebFlux) - AI assistant (Phase 5)
│       ├── src/main/java/com/fixitpro/aichat/
│       └── Dockerfile
├── frontend/                  # React + TypeScript (Phase 3+)
├── docker-compose.yml          # orchestrates mysql, redis, core-service, ai-chat-service
└── .env.example
```

A monorepo (not one GitHub repo per service) was chosen deliberately for a solo project — it's far easier to keep coordinated changes (e.g. a shared JWT secret, a core-service API change that ai-chat-service depends on) in lockstep without cross-repo versioning overhead.

## Git branching strategy

- **`main`** — always deployable. Protected; only updated via merged PRs from `develop`.
- **`develop`** — integration branch where features come together before a release.
- **`feature/<short-description>`** — one branch per unit of work, e.g. `feature/reservation-status-flow`, `feature/ai-chat-service-scaffold`, `feature/review-moderation`. Branch off `develop`, PR back into `develop`.

Example workflow for a new feature:
```bash
git checkout develop
git pull
git checkout -b feature/review-moderation
# ... work, commit ...
git push -u origin feature/review-moderation
# open a PR: feature/review-moderation → develop
```

Keeping real PR history (even solo) is worth doing — it's a concrete, checkable artifact of your process that a reviewer or interviewer can look at directly.

## Status: Phase 5 — AI Booking Assistant (complete, verified in CI)

Built so far:
- **Phase 1**: Foundation — Spring Boot skeleton, Flyway schema, JWT auth, security, error handling, Docker Compose
- **Phase 2**: Booking domain (in `core-service`) —
  - `service_type` — public catalog listing, admin CRUD
  - `technician_profile` — admin provisions technician accounts (creates both the User and the profile in one step); technicians can toggle their own availability
  - `reservation` — the core booking flow:
    - Customer creates a booking, **optionally** picking a specific technician
    - If no technician is chosen, the system **auto-assigns** the least-busy available technician for that service type/date and the booking starts `CONFIRMED`
    - If a technician is explicitly chosen, the booking starts `PENDING` until confirmed (avoids silently double-booking someone)
    - Full status lifecycle enforced as an explicit state machine (`ReservationStatus.canTransitionTo`): `PENDING → CONFIRMED → IN_PROGRESS → COMPLETED`, with `CANCELLED` reachable from any non-terminal state
    - Ownership/role checks in the service layer: customers can only cancel their own bookings, technicians can advance (not cancel) their assigned jobs, admins can do anything including reassigning technicians
  - `review` — customers review a `COMPLETED` reservation; technicians can reply once; admins moderate replies (`VISIBLE`/`HIDDEN`/`DELETED`)
  - `business_schedule` — admin-managed schedule overrides (hours/closures)
  - Self-service password change (`PATCH /api/users/me/password`), admin user listing/activation, admin technician management, and an admin dashboard-stats endpoint
- **Phase 3**: React + TypeScript frontend — auth (login/signup), customer booking flow, my-bookings, technician jobs view, and an admin console (dashboard, users, technicians, review moderation)
- **Phase 4**: CI/CD — GitHub Actions workflows for backend build (`backend-ci.yml`), frontend build/lint/typecheck (`frontend-ci.yml`), and a full docker-compose integration test (`integration-test.yml`) that runs the real booking + AI chat flows end-to-end on every push
- **Phase 5**: AI booking assistant (`ai-chat-service`) — a tool-calling agent (Groq, `openai/gpt-oss-120b`) that looks up services/technicians, books/cancels reservations, and checks on existing bookings entirely through natural conversation, calling `core-service`'s own API (never touching the DB directly, never bypassing its validation/auth). Exposed in the frontend as a floating assistant widget, visible to logged-in customers.
- **Architecture**: monorepo restructured into `services/core-service` + `services/ai-chat-service`

Verified end-to-end with `test_fixitpro_flow.ps1` (core booking flow) and `test_ai_chat_flow.ps1` (AI chat flow) — both idempotent, rerunnable without resetting the DB, and both gate CI on every push via `integration-test.yml`.

Built so far:
- **Phase 1**: Foundation — Spring Boot skeleton, Flyway schema, JWT auth, security, error handling, Docker Compose
- **Phase 2**: Booking domain (in `core-service`) —
  - `service_type` — public catalog listing, admin CRUD
  - `technician_profile` — admin provisions technician accounts (creates both the User and the profile in one step); technicians can toggle their own availability
  - `reservation` — the core booking flow:
    - Customer creates a booking, **optionally** picking a specific technician
    - If no technician is chosen, the system **auto-assigns** the least-busy available technician for that service type/date and the booking starts `CONFIRMED`
    - If a technician is explicitly chosen, the booking starts `PENDING` until confirmed (avoids silently double-booking someone)
    - Full status lifecycle enforced as an explicit state machine (`ReservationStatus.canTransitionTo`): `PENDING → CONFIRMED → IN_PROGRESS → COMPLETED`, with `CANCELLED` reachable from any non-terminal state
    - Ownership/role checks in the service layer: customers can only cancel their own bookings, technicians can advance (not cancel) their assigned jobs, admins can do anything including reassigning technicians
  - `review` — customers review a `COMPLETED` reservation; technicians can reply once; admins moderate replies (`VISIBLE`/`HIDDEN`/`DELETED`)
  - `business_schedule` — admin-managed schedule overrides (hours/closures)
  - Self-service password change (`PATCH /api/users/me/password`), admin user listing/activation, admin technician management, and an admin dashboard-stats endpoint
- **Phase 3**: React + TypeScript frontend — auth (login/signup), customer booking flow, my-bookings, technician jobs view, and an admin console (dashboard, users, technicians, review moderation)
- **Phase 4**: CI/CD — GitHub Actions workflows for backend build (`backend-ci.yml`), frontend build/lint/typecheck (`frontend-ci.yml`), and a full docker-compose integration test (`integration-test.yml`) that runs the real booking + AI chat flows end-to-end on every push
- **Phase 5**: AI booking assistant (`ai-chat-service`) — a tool-calling agent (Groq, `openai/gpt-oss-120b`) that looks up services/technicians, books/cancels reservations, and checks on existing bookings entirely through natural conversation, calling `core-service`'s own API (never touching the DB directly, never bypassing its validation/auth). Exposed in the frontend as a floating assistant widget, visible to logged-in customers.
- **Architecture**: monorepo restructured into `services/core-service` + `services/ai-chat-service`

Verified end-to-end with `test_fixitpro_flow.ps1` (core booking flow) and `test_ai_chat_flow.ps1` (AI chat flow) — both idempotent, rerunnable without resetting the DB, and both gate CI on every push via `integration-test.yml`.

Built so far:
- **Phase 1**: Foundation — Spring Boot skeleton, Flyway schema, JWT auth, security, error handling, Docker Compose
- **Phase 2**: Booking domain (in `core-service`) —
  - `service_type` — public catalog listing, admin CRUD
  - `technician_profile` — admin provisions technician accounts (creates both the User and the profile in one step); technicians can toggle their own availability
  - `reservation` — the core booking flow:
    - Customer creates a booking, **optionally** picking a specific technician
    - If no technician is chosen, the system **auto-assigns** the least-busy available technician for that service type/date and the booking starts `CONFIRMED`
    - If a technician is explicitly chosen, the booking starts `PENDING` until confirmed (avoids silently double-booking someone)
    - Full status lifecycle enforced as an explicit state machine (`ReservationStatus.canTransitionTo`): `PENDING → CONFIRMED → IN_PROGRESS → COMPLETED`, with `CANCELLED` reachable from any non-terminal state
    - Ownership/role checks in the service layer: customers can only cancel their own bookings, technicians can advance (not cancel) their assigned jobs, admins can do anything including reassigning technicians
  - `review` — customers review a `COMPLETED` reservation; technicians can reply once; admins moderate replies (`VISIBLE`/`HIDDEN`/`DELETED`)
  - `business_schedule` — admin-managed schedule overrides (hours/closures)
  - Self-service password change (`PATCH /api/users/me/password`), admin user listing/activation, admin technician management, and an admin dashboard-stats endpoint
- **Architecture**: monorepo restructured into `services/core-service` + `services/ai-chat-service` (scaffolded, real logic in Phase 5)

Verified end-to-end with `test_fixitpro_flow.ps1` — a repeatable, idempotent smoke test covering the full flow above, rerunnable without resetting the DB.

### Bootstrap admin account

Public signup only ever creates `CUSTOMER` accounts, and technician accounts can only be created by an authenticated admin — so a bootstrap admin is seeded via migration `V3__seed_bootstrap_admin.sql`:

```
username: admin
password: ChangeMe123
```

**Change this password immediately in any real deployment** — use `PATCH /api/users/me/password` once logged in as `admin`.

## Running locally

1. Install Docker + Docker Compose.
2. Copy `.env.example` to `.env` and fill in real values (at minimum, set `JWT_SECRET` — generate one with `openssl rand -base64 64`). To use the AI assistant, also set `GROQ_API_KEY` (free, no credit card — get one at https://console.groq.com); without it, `ai-chat-service` runs fine but the assistant just reports it isn't configured yet.
3. From the project root:
```bash
   docker compose up --build
```
4. `core-service` is available at `http://localhost:8080` (Swagger UI at `/docs`). `ai-chat-service` is available at `http://localhost:8081`, exposing the booking assistant at `POST /api/chat/message`.
5. Flyway runs migrations automatically on `core-service` startup — no manual schema setup needed.
6. For the frontend: `cd frontend`, copy `.env.example` to `.env` (defaults already point at the two services above), then `npm install && npm run dev` — served at `http://localhost:5173`.
### Running a service without Docker (for development)

```bash
cd services/core-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Requires a local MySQL and Redis instance matching the values in `application-dev.yml`, or run just those two via `docker compose up mysql redis`.

## API reference
### `core-service`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/signup` | Public | Creates a CUSTOMER account |
| POST | `/api/auth/login` | Public | Returns access + refresh tokens |
| POST | `/api/auth/refresh` | Public (needs refresh token) | Issues a new access token |
| GET | `/api/service-types` | Public | List active services (Electrician, Plumber, Carpenter) |
| POST | `/api/admin/service-types` | ADMIN | Create a service type |
| GET | `/api/technicians?serviceTypeId=1` | Public | List available technicians for a service |
| POST | `/api/admin/technicians` | ADMIN | Provision a new technician (creates user + profile) |
| PATCH | `/api/technicians/me/availability?available=false` | TECHNICIAN | Toggle own availability |
| POST | `/api/reservations` | CUSTOMER | Book a service (technicianId optional) |
| GET | `/api/reservations/me` | CUSTOMER | List my bookings |
| GET | `/api/reservations/technicians/me` | TECHNICIAN | List my assigned jobs |
| GET | `/api/reservations/{id}` | Owner/assigned technician/ADMIN | View one reservation |
| PATCH | `/api/reservations/{id}/status` | Owner/assigned technician/ADMIN | Move through the status lifecycle |
| PATCH | `/api/reservations/{id}/assign` | ADMIN | Assign/reassign a technician |
| GET | `/api/reservations/admin/all` | ADMIN | View every reservation |
| POST | `/api/reviews` | CUSTOMER | Review a COMPLETED reservation |
| PUT | `/api/reviews/{id}` | CUSTOMER (owner) | Edit own review |
| GET | `/api/reviews/{id}` | Public | View one review |
| GET | `/api/reviews/technician/{id}` | Public | Reviews for a technician's profile |
| GET | `/api/reviews/admin/all` | ADMIN | View every review |
| POST | `/api/reviews/{id}/reply` | TECHNICIAN (assigned) | Reply once to a review |
| PATCH | `/api/admin/reviews/replies/{id}/moderate` | ADMIN | Set reply to VISIBLE/HIDDEN/DELETED |
| GET | `/api/business-schedule` | Public | List all schedule overrides |
| GET | `/api/business-schedule/{date}` | Public | Check hours/closure for one date |
| POST/PUT/DELETE | `/api/admin/business-schedule[/{id}]` | ADMIN | Manage schedule overrides |
| PATCH | `/api/users/me/password` | Any authenticated user | Change own password |
| GET | `/api/admin/users?role=CUSTOMER` | ADMIN | List users, optional role filter |
| PATCH | `/api/admin/users/{id}/status?active=false` | ADMIN | Activate/deactivate a user (can't deactivate self) |
| GET | `/api/technicians/{id}` | Public | Single technician detail |
| GET | `/api/admin/technicians` | ADMIN | List every technician (not just available ones) |
| PUT | `/api/admin/technicians/{id}` | ADMIN | Update a technician's profile |
| GET | `/api/admin/dashboard/stats` | ADMIN | Aggregate counts: users, reservations by status, reviews, avg rating |

### `ai-chat-service`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/chat/message` | Any authenticated user (customer-facing tools only) | Send the running conversation (`{ messages: [{role, content}] }`), get back the assistant's reply plus the updated history. Verifies the same JWT `core-service` issues; calls `core-service`'s own API for every lookup/booking action, so all validation and role checks still apply. |

### Suggested test flow

1. Log in as `admin` / `ChangeMe123` → get an access token
2. `POST /api/admin/technicians` with that token → create a technician (e.g. serviceTypeId `1` for Electrician)
3. Sign up / log in as a customer
4. `GET /api/service-types` and `GET /api/technicians?serviceTypeId=1` to see options
5. `POST /api/reservations` as the customer — try it both with and without `technicianId` to see auto-assignment vs. explicit-pick behavior
6. `PATCH /api/reservations/{id}/status` to walk it through `CONFIRMED → IN_PROGRESS → COMPLETED`

## Design decisions worth knowing

- **Package-by-feature, not by-layer** — `domain/reservation`, `domain/review`, etc. each hold their own entity/repository/service/controller, so the codebase reads like a real product, not a tutorial.
- **Flyway owns the schema** — Hibernate is set to `ddl-auto: validate`, meaning it never silently alters the DB. Every schema change is a reviewed, versioned migration file.
- **Stateless JWT, not sessions** — matches a real horizontally-scalable API; no server-side session store to manage, and it's what lets `ai-chat-service` verify the same tokens without a shared session store.
- **Public signup only ever creates CUSTOMER accounts** — TECHNICIAN and ADMIN accounts are provisioned by an admin, closing the privilege-escalation hole that a naive signup form would leave open.
- **Targeted microservices, not full sprawl** — see the Architecture section above. Splitting everything into tiny services would add distributed-transaction and network-failure complexity with no corresponding benefit at this scale.