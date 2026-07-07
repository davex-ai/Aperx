# HRMS Backend

Spring Boot 3 REST API for the HR Management System — JWT auth with role-based
access (Admin / Manager / Employee), employee self-service, leave management,
payroll, and an applicant tracking system (ATS).

## Stack
- Java 17, Spring Boot 3.3
- Spring Security + JWT (jjwt 0.12)
- Spring Data JPA + PostgreSQL
- Resend (transactional email — onboarding / leave-decision emails)
- spring-dotenv (loads a `.env` file from the project root, if present)
- openhtmltopdf (payslip PDF generation)

## Getting started

1. **Create the database**
   ```sql
   CREATE DATABASE hrms_db;
   CREATE USER hrms_user WITH PASSWORD 'changeme';
   GRANT ALL PRIVILEGES ON DATABASE hrms_db TO hrms_user;
   ```
   `schema.sql` is provided for reference — with `ddl-auto: update` (default in
   `application.yml`) Hibernate will create/update tables automatically on boot.

2. **Set environment variables**

   Create a `.env` file in the project root (same folder as `pom.xml`) —
   `spring-dotenv` loads it automatically on boot:
   ```
   DB_USERNAME=hrms_user
   DB_PASSWORD=changeme
   JWT_SECRET=<a long random string, 256+ bits>
   RESEND_API_KEY=re_your_actual_api_key
   MAIL_FROM=hr-noreply@yourcompany.com
   FRONTEND_URL=http://localhost:5173
   ```
   Make sure `.env` is in `.gitignore` (it already is) — never commit real
   API keys or secrets.

   **Resend setup:**
   - Sign up at [resend.com](https://resend.com) and grab an API key.
   - `MAIL_FROM` must be an address on a domain you've verified with Resend
     (Resend rejects sends from unverified domains) — you cannot send from
     an arbitrary `@yourcompany.com` address until DNS records are verified
     in the Resend dashboard.
   - `EmailService` calls Resend's REST API directly (see `ResendClient.java`)
     — no SMTP relay involved.

3. **Run**
   ```
   mvn spring-boot:run
   ```
   The API starts on `http://localhost:8080`.

4. **Default admin account** (seeded automatically on first boot — change
   the password immediately):
   ```
   email:    admin@company.com
   password: ChangeMe123!
   ```

> Note: this project was assembled in a sandboxed environment without access
> to Maven Central, so `mvn compile` could not be run here. The code was
> written and reviewed carefully against the Spring Boot 3.3 / jjwt 0.12 APIs,
> but please run `mvn compile` yourself on first checkout and let me know if
> anything needs adjusting.

## Onboarding flow (admin creates an employee)

1. `POST /api/admin/employees` (Admin) → creates the `User` + `Employee`
   records, generates a one-time verification token, emails a secure link.
2. Employee opens the link → `POST /api/auth/complete-signup` with the token
   and a new password.
3. Employee logs in → `AuthResponse.mustCompleteOnboarding = true` tells the
   frontend to show the profile-setup wizard.
4. `POST /api/auth/first-login-profile` — phone number, bank details,
   emergency contact. This flips `mustCompleteOnboarding` to `false` and
   unlocks the standard dashboard.

## Key endpoints

| Area | Method & Path | Role |
|---|---|---|
| Auth | `POST /api/auth/login` | public |
| Auth | `POST /api/auth/complete-signup` | public (token-gated) |
| Auth | `POST /api/auth/first-login-profile` | authenticated |
| Profile | `GET/PUT /api/profile/me` | authenticated |
| Profile | `POST /api/profile/me/emergency-contacts` | authenticated |
| Employees | `GET /api/admin/employees`, `POST /api/admin/employees` | Admin |
| Employees | `POST /api/admin/employees/{id}/resend-invitation` | Admin |
| Employees | `GET /api/manager/team` | Admin, Manager |
| Leave | `POST /api/leave-requests`, `GET /api/leave-requests/me`, `GET /api/leave-requests/balances/me` | authenticated |
| Leave | `GET /api/leave-requests/pending`, `PUT /api/leave-requests/{id}/review` | Admin, Manager |
| Careers (public) | `GET /api/careers/jobs`, `POST /api/careers/jobs/{id}/apply` | public |
| Jobs / ATS | `GET/POST/PUT /api/jobs`, `GET /api/jobs/{id}/applications`, `PUT /api/jobs/applications/{id}/status` | Admin (POST/PUT), Admin+Manager (GET) |
| Payroll | `POST /api/payroll/run` | Admin |
| Payroll | `GET /api/payroll/payslips/me`, `GET /api/payroll/payslips/{id}/download` | authenticated (own payslips) |
| Documents | `GET /api/documents/me`, `GET /api/documents/{id}/download` | authenticated (own documents) |
| Dashboard | `GET /api/dashboard/stats` | Admin, Manager |

## Schema extensions beyond the original DDL

The original schema didn't have tables for several requested features, so
these were added (see `schema.sql`):
- `bank_accounts`, `emergency_contacts` — employee self-service data
- `leave_balances` — tracked separately from `leave_requests` so balances
  can be checked before submitting a request
- `payroll_runs`, `payslips` — monthly payroll batches and generated payslips
- `employee_documents` — tax forms, employment letters
- `verification_tokens` — the admin-initiated signup/verification flow
- Added columns: `users.must_change_password`, `employees.salary_grade`,
  `leave_requests.certificate_url` / `review_comment`,
  `job_postings.posted_by`

## Security notes for production

- Set a strong, random `JWT_SECRET` (never use the default).
- Put `ddl-auto` on `validate` and manage schema changes with Flyway/Liquibase.
- Serve behind HTTPS; the CORS config currently allows any `localhost` origin
  for local frontend development — restrict `allowedOriginPatterns` before
  deploying.
- Payslip/document PDF storage currently uses local disk (`app.storage.documents-dir`)
  — swap for S3/blob storage in production.
