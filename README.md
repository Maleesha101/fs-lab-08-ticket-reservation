# Secure Ticket Reservation System

## COSC 32133 / BECS 32263 — Full-Stack Software Development, Lab Sheet 08
Student: EC/2022/042

## Prerequisites

- Java 17 or 21 (`java -version`)
- Maven (`mvn -v`)
- Git (`git --version`)
- Node.js and Newman (for Postman collection testing)
- MongoDB reachable on `localhost:27017` (or via `MONGODB_URI` env var)

## Environment Setup

1. **MongoDB**: Must be running. Use Docker:
   ```
   docker run -d --name mongo -p 27017:27017 mongo:7
   ```
   Or start native: `mongod --dbpath /home/maleesha/mongodb-data --bind_ip 127.0.0.1 --port 27017`

2. **GitHub access**: `gh auth login` if not already logged in.

3. **Clone / enter project**:
   ```
   git init -b main
   # .gitignore already covers target/, .idea/, .vscode/, *.iml/, .env/, node_modules/ (newman reports allowed only in docs/evidence/)
   gh repo create ticket-reservation --public --source=. --remote=origin
   ```

4. **Set credentials** (only if empty):
   ```
   git config user.name "<your name>"
   git config user.email "<your email>"
   ```

## Building and Running

```bash
mvn clean package -DskipTests
java -jar target/ticket-reservation-0.0.1-SNAPSHOT.jar &
# or: ./mvnw spring-boot:run
```

The app starts on port **8080**. Default seed data:
- Users: `admin/admin123` (ADMIN), `organizer/organizer123` (ORGANIZER), `customer/customer123` (CUSTOMER)
- Passwords are BCrypt-hashed (verified: starts with `$2a$`/`$2b$`)
- 3 sample events owned by `organizer`

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | `TicketReservationLab08SecretKeyForHS384Sign!!!!!` (48 chars) | Secret for HS384 JWT signing |
| `MONGODB_URI` | `mongodb://localhost:27017/ticket_reservation_db` | MongoDB connection string |
| `JWT_EXPIRATION_MS` | `3600000` | Token expiry in ms (1 hour) |

## Seeded Accounts

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| organizer | organizer123 | ORGANIZER |
| customer | customer123 | CUSTOMER |
| EC/2022/042 | EC/2022/042 | CUSTOMER (via registration) |

## REST API Endpoints

### Auth (Public)

| Method | Endpoint | Access | Notes |
|--------|----------|--------|-------|
| POST | `/api/auth/register` | Public | Default role CUSTOMER. ADMIN/ORGANIZER requires ADMIN caller. Duplicate → 409 |
| POST | `/api/auth/login` | Public | Bad credentials → 401. Returns AuthResponse JSON |
| GET | `/api/auth/me` | Any logged-in | Returns UserResponse |

### Events

| Method | Endpoint | Access | Notes |
|--------|----------|--------|-------|
| GET | `/api/events` | Any logged-in | Optional `?name=` case-insensitive search |
| GET | `/api/events/{id}` | Any logged-in | 404 if missing |
| POST | `/api/events` | ADMIN, ORGANIZER | 201; organizer = caller |
| PUT | `/api/events/{id}` | ADMIN, ORGANIZER (own) | 403 if editing another's event |
| DELETE | `/api/events/{id}` | ADMIN | 409 if reservations exist |

### Reservations

| Method | Endpoint | Access | Notes |
|--------|----------|--------|-------|
| POST | `/api/reservations` | CUSTOMER | Atomic seat take; insufficient → 409 |
| GET | `/api/reservations/my` | CUSTOMER | Caller's reservations |
| GET | `/api/reservations` | ADMIN, ORGANIZER | ADMIN all; ORGANIZER only own events |
| DELETE | `/api/reservations/{id}` | CUSTOMER (own), ADMIN | CANCELLED + releases seats |

### Admin

| Method | Endpoint | Access | Notes |
|--------|----------|--------|-------|
| GET | `/api/admin/users` | ADMIN | |
| GET | `/api/admin/stats` | ADMIN | StatsResponse: totalUsers, totalEvents, totalReservations, ticketsSold, revenue |

## Error Handling

All errors return JSON via `ErrorResponse`: `{timestamp, status, error, message, path}`

- Validation → 400 (field errors listed)
- `ApiException` → its HTTP status
- `BadCredentialsException` → 401
- `AccessDeniedException` → 403
- Unreadable JSON → 400
- Fallback → 500 (no stack traces leaked)

## Running Newman Tests

```bash
newman run TicketReservation.postman_collection.json --reporters cli
# or with htmlextra if available
newman run TicketReservation.postman_collection.json --reporters cli,htmlextra
```

Run on a fresh database to prove idempotency:
```bash
# Drop the DB, restart, re-run
mongosh --quiet --eval "db.dropDatabase()" ticket_reservation_db
# then restart app and run Newman
```

## Git Workflow (Conventional Commits)

One commit per logical step, pushed immediately. Expected sequence:
1. `chore: initialize Spring Boot 3.2 project with Maven and .gitignore`
2. `chore(deps): add MongoDB, Security, Validation, Lombok and JJWT 0.11.5`
3. `chore(config): configure MongoDB URI and JWT settings`
4. `feat(model): add Role, ReservationStatus, AppUser, Event and Reservation documents`
5. `feat(repository): add User, Event and Reservation repositories`
6. `feat(dto): add request/response records with bean validation`
7. `feat(exception): add ApiException and GlobalExceptionHandler with JSON errors`
8. `feat(security): add JwtService and JwtAuthenticationFilter`
9. `feat(security): add SecurityConfig, BCrypt, 401/403 JSON handlers`
10. `feat(auth): implement register, login and me endpoints`
11. `feat(events): implement event CRUD with ownership rule`
12. `feat(events): add atomic seat take/release using MongoTemplate`
13. `feat(reservations): implement booking, listing and cancellation`
14. `feat(admin): add users list and system statistics endpoints`
15. `feat(config): add DataLoader seeding accounts and sample events`
16-20. test, docs, fix commits as needed

After final push:
```bash
git tag -a lab08-submission -m "Lab 08 submission - EC/2022/042"
git push origin lab08-submission
git log --oneline
```

## Deliverables

- Complete source in `ticket-reservation/`
- `docs/evidence/`: Newman report, test output, and mongosh BCrypt output
- `docs/REPORT_CODE.md`: all source files concatenated with headings
- GitHub repository with full commit history pushed to `origin/main`, plus `lab08-submission` tag
- README.md as above