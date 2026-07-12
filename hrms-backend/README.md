# AperX Backend

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
- Apache POI (parses uploaded .docx resumes for in-browser preview)

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

4. **Getting your first account**: there is no seeded default admin.
   `POST /api/auth/register-company` is the entry point — anyone can register
   a new company and becomes its first `ROLE_ADMIN` immediately (no invite
   flow needed for the founding admin, since they're setting their own
   password directly at signup).

> Note: this project was assembled in a sandboxed environment without access
> to Maven Central, so `mvn compile` could not be run here. The code was
> written and reviewed carefully against the Spring Boot 3.3 / jjwt 0.12 APIs,
> but please run `mvn compile` yourself on first checkout and let me know if
> anything needs adjusting.

## Onboarding flow (HR creates a hire, then assigns their company email)

1. `POST /api/admin/employees` (Admin) — HR enters the new hire's personal
   email, name, role, department, job title, salary, and pay type. This
   creates the `User` + `Employee` records in a **draft** state: no company
   email yet, no invite sent. `onboardingStage` on the response is `DRAFT`.
2. `PUT /api/admin/employees/{id}/assign-company-email` (Admin) — HR assigns
   the official company email (e.g. `john@company.com`). This generates a
   one-time verification token and emails the invite link **to the
   employee's personal email** (they don't have working access to the new
   company mailbox yet). `onboardingStage` becomes `INVITED`.
3. Employee opens the link in their personal inbox → `POST
   /api/auth/complete-signup` with the token and a new password.
4. Employee logs in with their new company email → `AuthResponse.
   mustCompleteOnboarding = true` tells the frontend to show the
   profile-setup wizard.
5. `POST /api/auth/first-login-profile` — phone number, bank details,
   emergency contact. This flips `mustCompleteOnboarding` to `false`,
   `onboardingStage` becomes `ACTIVE`, and the standard dashboard unlocks.

If step 2's email fails to send, `POST /api/admin/employees/{id}
/resend-invitation` retries it (only valid once a company email has been
assigned).

### Extending an offer from the ATS

`POST /api/jobs/applications/{id}/extend-offer` (Admin) — once a candidate's
application reaches `OFFERED`, this runs the exact same draft-creation logic
as step 1 above, pre-filled from the candidate's application (name, personal
email, phone). The application is marked `HIRED`. From there, the flow is
identical: assign a company email, invite gets sent, etc.

### Terminating an employee

`PUT /api/admin/employees/{id}/terminate` (Admin) — deactivates the
employee's account (`isActive = false`), which immediately blocks login.
Admins cannot terminate their own account through this endpoint.

## Payroll calculation

`POST /api/payroll/preview` computes payslips without saving anything —
review the numbers before committing. `POST /api/payroll/run` does the same
calculation and persists it (idempotent per company/month/year — running
twice for the same period is rejected).

- **Salaried employees**: gross = `salaryAmount / 12` (salaryAmount is
  treated as an annual figure).
- **Hourly employees**: gross = hourly rate × sum of hours from
  **`APPROVED`** weekly timesheets whose week falls inside the selected
  month. Submitted-but-not-yet-approved or rejected weeks are not paid —
  this is why timesheet approval has to happen before running payroll for
  hourly staff.
- Optional per-employee bonus/deduction adjustments can be passed in the
  request; bonuses are taxed, the flat deduction is applied after tax.
- Tax withholding (15% flat) and a simulated social security withholding
  (6.2% flat) are both simplified placeholders, not real jurisdiction-based
  tax tables — see the security notes below.
- **No money actually moves.** Confirming a run marks the `PayrollRun` as
  `PAID`, generates PDF payslips, and returns a success response — there is
  no bank integration or payment file generation.
- Expense reimbursements are not implemented — only hours and salary feed
  into the calculation.

## Manager privilege boundaries

Managers and Admins share most endpoints, but managers are scoped to their
own direct reports wherever it matters:

- `GET /api/leave-requests/pending` and `GET /api/time-tracking/timesheets
  /pending` return company-wide results for Admins, but only the caller's
  direct reports for Managers.
- Approving/rejecting a leave request or timesheet is rejected with a 400
  if the target employee doesn't report directly to the calling Manager.
- `GET /api/employees/{id}` only includes salary/hourly-rate fields if the
  caller is an Admin, is viewing their own profile, or is that employee's
  direct manager — otherwise those fields come back `null`.
- `GET /api/dashboard/stats` (company-wide headcount/turnover/financial
  reporting) is Admin-only; Managers use `GET /api/manager/team` instead.



| Area | Method & Path | Role |
|---|---|---|
| Auth | `POST /api/auth/login` | public |
| Auth | `POST /api/auth/complete-signup` | public (token-gated) |
| Auth | `POST /api/auth/first-login-profile` | authenticated |
| Profile | `GET/PUT /api/profile/me` | authenticated |
| Profile | `POST /api/profile/me/emergency-contacts` | authenticated |
| Auth | `POST /api/auth/register-company` | public |
| Employees | `GET /api/admin/employees`, `POST /api/admin/employees` | Admin |
| Employees | `PUT /api/admin/employees/{id}/assign-company-email` | Admin |
| Employees | `POST /api/admin/employees/{id}/resend-invitation` | Admin |
| Employees | `PUT /api/admin/employees/{id}/terminate` | Admin |
| Employees | `GET /api/manager/team` | Admin, Manager |
| Recruitment | `POST /api/jobs/applications/{id}/extend-offer` | Admin |
| Leave | `POST /api/leave-requests`, `GET /api/leave-requests/me`, `GET /api/leave-requests/balances/me` | authenticated |
| Leave | `GET /api/leave-requests/pending`, `PUT /api/leave-requests/{id}/review` | Admin, Manager |
| Careers (public) | `GET /api/careers/{companySlug}/jobs`, `GET /api/careers/{companySlug}/jobs/{id}`, `POST /api/careers/{companySlug}/jobs/{id}/apply` (multipart, resume required) | public |
| Jobs / ATS | `GET/POST/PUT /api/jobs`, `GET /api/jobs/{id}/applications`, `PUT /api/jobs/applications/{id}/status`, `GET /api/jobs/applications/{id}/resume-preview` | Admin (POST/PUT), Admin+Manager (GET) |
| Payroll | `POST /api/payroll/preview` | Admin |
| Payroll | `POST /api/payroll/run` | Admin |
| Payroll | `GET /api/payroll/payslips/me`, `GET /api/payroll/payslips/{id}/download` | authenticated (own payslips) |
| Documents | `GET /api/documents/me`, `GET /api/documents/{id}/download` | authenticated (own documents) |
| Dashboard | `GET /api/dashboard/stats` | Admin only |

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
