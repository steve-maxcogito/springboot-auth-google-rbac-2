# Spring Boot Auth (Local + Google) with RBAC and Postgres

A ready-to-run Spring Boot 3 project that exposes a REST API for:
- Register/Login with **username + password**
- Login with **Google ID token** (backend verifies the token)
- Stateless **JWT** issuance for API access
- **Role-based** authorization (USER, ADMIN, SECURITY_SERVICE, DATA_SERVICE)
- User profile fields: username, first/middle/last name, address, email, phone

## Stack
- Spring Boot 3.3.x, Spring Security
- JPA (Hibernate) + PostgreSQL
- Flyway for role seeding
- JJWT for JWT generation/validation
- Google API Client for ID token verification

## Quick Start

1. **Create Postgres DB and user** (example):
   ```sql
   CREATE DATABASE maxcogito_auth;
   CREATE USER maxcogito WITH PASSWORD 'changeMe';
   GRANT ALL PRIVILEGES ON DATABASE maxcogito_auth TO maxcogito;
   ```

2. **Configure application.yml**:
   - Set `spring.datasource.*`
   - Set `app.jwt.secret` to a long random string
   - Add your Google OAuth **client ID(s)** under `google.clientIds`

3. **Build & Run**:
   ```bash
   mvn clean spring-boot:run
   ```

4. **Endpoints**:
   - `POST /api/auth/register` → body:
     ```json
     {
       "username": "alice",
       "password": "P@ssw0rd123",
       "email": "alice@example.com",
       "firstName": "Alice",
       "lastName": "Anderson",
       "roles": ["ROLE_USER","ROLE_DATA_SERVICE"]  // optional; defaults to ROLE_USER
     }
     ```
     Returns `{ token, tokenType, username, email, roles }`.

   - `POST /api/auth/login` → body:
     ```json
     { "usernameOrEmail": "alice", "password": "P@ssw0rd123" }
     ```
     Returns JWT.

   - `POST /api/auth/google` → body:
     ```json
     { "idToken": "<Google ID token from your frontend>" }
     ```
     Backend validates the token issuer/signature/audience and either creates or updates the user, then returns a JWT.

   - `GET /api/auth/me` with header `Authorization: Bearer <token>` returns identity info.

   - Secured demo endpoints:
     - `/api/user/ping` requires any of: USER, ADMIN, SECURITY_SERVICE, DATA_SERVICE
     - `/api/admin/ping` requires ADMIN
     - `/api/security/ping` requires SECURITY_SERVICE
     - `/api/data/ping` requires DATA_SERVICE

## Google Sign-In Notes
- On the **frontend**, obtain a Google ID token (e.g., with Google Identity Services).
- Send the ID token to `POST /api/auth/google`. The backend verifies it using allowed client IDs configured in `application.yml`.
- First-time Google login auto-provisions a user with `ROLE_USER` by default (customize in `UserService`).

## Role Names
Use Spring Security style names when stored:
- `ROLE_USER`
- `ROLE_ADMIN`
- `ROLE_SECURITY_SERVICE`
- `ROLE_DATA_SERVICE`

When writing access rules with `@PreAuthorize("hasRole('ADMIN')")`, drop the `ROLE_` prefix in the expression.

## Passwords
- Stored as **BCrypt** hashes (`BCryptPasswordEncoder`).
- Never store plaintext passwords.

## Migrations
- JPA auto-creates tables (`ddl-auto: update`).
- Flyway migration `V1__seed_roles.sql` ensures required roles exist.

## Production Hardening
- Replace `app.jwt.secret` with a 256-bit+ random value and store in a secret manager.
- Consider setting `ddl-auto: none` and manage schema with Flyway.
- Add rate limiting, CORS config, refresh tokens, account lockout, email verification, etc.
- Use HTTPS/TLS and secure headers.
- Add auditing for login attempts.

## Build
```
mvn -v
mvn clean package
java -jar target/springboot-auth-google-rbac-1.0.0.jar
```

## Postman Collection (Create Your Own)
- Import requests matching the endpoints above. Add an `Authorization` header with `Bearer {{token}}` after login.

---

**Author:** Maxcogito sample • MIT License


## Refresh Tokens
- Login/register/google now return `{ token, refreshToken }`.
- `POST /api/auth/refresh` with body `{"refreshToken":"<rt>"}` issues a new access token and refresh token (rotation). Old token is revoked.
- `POST /api/auth/logout` with `{"refreshToken":"<rt>"}` revokes it.

## Email Verification
- Configure `spring.mail.*` in `application.yml`.
- `POST /api/auth/verify/start` with `{"email":"user@example.com"}` sends a verification email.
- `POST /api/auth/verify/confirm?token=<token>` marks it verified (sample does not block login by verification status; you can add a `verified` field on `User` to enforce).

---

# React Login Sample
See separate zip: `react-auth-sample/`

