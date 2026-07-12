# 🏢 AperX — Multi-Tenant HR Operating System

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=4F46E5&height=200&section=header&text=AperX&fontSize=60&fontColor=ffffff" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Frontend-React%20%2B%20TypeScript-61dafb?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Database-PostgreSQL-336791?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Styling-Tailwind%20v4-38bdf8?style=for-the-badge" />
</p>

<p align="center">
  <img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&size=22&pause=1000&color=4F46E5&center=true&vCenter=true&width=650&lines=Multi-Tenant+SaaS+HR+Platform;Payroll%2C+Time+Tracking%2C+Recruitment;Role-Scoped+by+Design;Built+for+Real+Companies" />
</p>

---

## 🧠 What is AperX?

AperX is a **multi-tenant HR platform** — any company can register its own
isolated HR workspace and run its entire employee lifecycle through it:
hiring, onboarding, time tracking, leave, payroll, and internal
communication, all scoped so that no company can ever see another's data.

It's built as two independent projects that talk over a REST API:

- **`hrms-backend`** — Spring Boot + PostgreSQL, all business logic and
  tenant isolation
- **`hrms-frontend`** — React + TypeScript + Tailwind, the interface for
  Admins, Managers, and Employees

---

## ✨ Features

### 🏢 Multi-Tenant SaaS Core
- Self-serve company registration — anyone can spin up a new tenant
- Every table scoped by `company_id`; no cross-company data leakage
- Per-company public careers page at `/careers/:companySlug`

### 👤 Two-Phase Hiring & Onboarding
- HR drafts a hire with their personal email, role, and details
- HR assigns the official company email as a separate step
- Invite link is emailed to the candidate's **personal inbox** (they can't
  check the company one yet) — they set their password and log in
- Candidates can also apply **publicly, with no account needed**, and get
  promoted straight into this same flow once hired

### 🕒 Time Tracking
- Clock in/out with optional geolocation capture
- Weekly timesheets, submitted by employees and approved by their manager
- Approved hours feed directly into payroll for hourly staff

### 💰 Payroll Engine
- Two-step **preview → confirm** flow — nothing is calculated blind
- Hourly employees paid from *approved* timesheet hours × rate
- Salaried employees paid a fixed monthly share of annual salary
- Tax + simulated social security withholding calculated automatically
- PDF payslips generated per employee, per run
- No real money moves — this records payroll and generates documents only

### 🌴 Leave Management
- Employees request leave against real balances by leave type
- Managers approve only their own direct reports; Admins see company-wide
- Medical certificate requirement enforced for longer sick leave

### 📋 Recruitment / ATS
- Admins post jobs with custom, per-posting screening questions
- Public applicants attach a resume (PDF or Word) and answer directly —
  no login required
- Admins preview resumes in-browser and move candidates through a visual
  pipeline: Applied → Screening → Interview → Offered → Hired
- "Extend Offer" converts a candidate straight into a draft employee record

### 📣 Announcements
- Company-wide posts with threaded comments, open to every employee

### 🕵️ Anonymous Reports
- Employees can raise concerns under a randomly generated per-report
  identity — no name is ever shown, even to Admins
- Replying to your own report keeps you under that same anonymous handle

### 🔐 Role-Scoped by Design
Every feature above is gated three ways — see the [Roles & Permissions](#-roles--permissions) section below.

---

## 🛠️ Tech Stack

```bash
Backend:
- Java 17, Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Apache POI (resume preview)
- openhtmltopdf (payslip generation)
- Gmail SMTP (transactional email)

Frontend:
- React 19 + TypeScript
- Tailwind CSS v4
- React Router 7
- lucide-react (icons)
- Vite
```

---

## 📦 Project Structure

```bash
aperx/
├── hrms-backend/
│   ├── src/main/java/com/hrms/
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Spring Data repositories
│   │   ├── service/         # Business logic
│   │   ├── controller/      # REST endpoints
│   │   ├── dto/             # Request/response shapes
│   │   ├── security/        # JWT + auth
│   │   └── config/          # Security & app config
│   └── schema.sql
└── hrms-frontend/
    └── src/
        ├── api/              # One module per backend controller
        ├── components/       # Shared UI primitives
        ├── context/          # Auth session state
        ├── layouts/          # Authenticated app shell
        └── pages/            # One file per route
```

---

## ⚙️ Setup

### 1. Database
Point the backend at any PostgreSQL instance — local, Supabase, Neon, or
Render Postgres. See each project's own README for exact config.

### 2. Backend
```bash
cd hrms-backend
# set DB + JWT + mail vars in .env
mvn spring-boot:run
```
Runs on `http://localhost:8080`.

### 3. Frontend
```bash
cd hrms-frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`, proxying `/api` to the backend.

### 4. First login
There's no seeded account — go to `/register-company` and create your own
company. You become its first Admin.

---

## 🧭 Architecture

```txt
Browser → React SPA → REST (JWT) → Spring Boot → PostgreSQL
                                        ↓
                              Gmail SMTP (invites, notifications)
```

Every authenticated request carries a JWT; every service method resolves
the caller's `company_id` from it before touching the database, so tenant
isolation is enforced at the service layer, not just the UI.

---

## 🎮 Roles & Permissions

| Role | Scope |
|------|-------|
| 🛡️ **Admin** | Full company control — hiring, payroll, terminations, dashboard, company-wide reporting |
| 🧑‍💼 **Manager** | Direct reports only — approves their team's leave & timesheets, no salary visibility outside their own reports, no dashboard |
| 👤 **Employee** | Self-service — own profile, own leave, own timesheets, own payslips, company announcements, anonymous reports |

See the full breakdown below for exactly what each role can and cannot do.

### Full role breakdown

<details>
<summary><b>🛡️ Admin — click to expand</b></summary>

- Register the company and manage company-wide settings
- Create employee profiles, assign managers, assign company emails, resend invites
- Terminate employee accounts
- Post, edit, and archive job openings with custom screening questions
- Review applications, preview resumes, extend offers
- Run payroll (preview + confirm), view all payslips
- Approve or reject **any** employee's leave requests and timesheets
- View the company-wide dashboard (headcount, turnover, department mix)
- Post and moderate announcements
- View and moderate anonymous reports (still cannot see who submitted one)

</details>

<details>
<summary><b>🧑‍💼 Manager — click to expand</b></summary>

- View and manage only their own direct reports (`My Team` page)
- Approve or reject leave requests **only from their direct reports**
- Approve or reject timesheets **only from their direct reports**
- View salary details **only** for themselves or their direct reports —
  every other employee's salary comes back hidden, including peers and
  executives
- Review job applications and move candidates through the pipeline
- Post and comment on announcements
- Submit and comment on anonymous reports
- **Cannot** access the company-wide dashboard, run payroll, create or
  terminate employees, or assign company emails

</details>

<details>
<summary><b>👤 Employee — click to expand</b></summary>

- View and edit their own profile, phone number, and bank details
- Add emergency contacts
- Submit leave requests and track their own balances
- Clock in/out, submit weekly timesheets for approval
- Download their own payslips and any tax forms/letters issued to them
- Post and comment on announcements
- Submit and comment on anonymous reports under a private handle
- **Cannot** view any other employee's salary, approve anything, or access
  admin/manager-only pages

</details>



---

## ⚠️ Known Simplifications

Being upfront about what's *not* production-grade yet:

- Tax and social security withholding are flat simplified rates, not real
  jurisdiction-based tax tables
- No real bank transfer integration — payroll runs record data and
  generate payslips only
- No automated tests yet
- `.docx` resume preview is basic text/formatting extraction, not
  pixel-perfect rendering
- No performance review or team-calendar features yet, despite being part
  of the original manager-role spec

---

## 🧪 Status

```diff
+ Multi-tenant core: DONE
+ Hiring & onboarding: DONE
+ Time tracking: DONE
+ Payroll engine: DONE
+ Leave management: DONE
+ Recruitment / ATS: DONE
+ Announcements: DONE
+ Anonymous reports: DONE
! Performance reviews: NOT STARTED
! Real tax jurisdictions: NOT STARTED
```

---

<p align="center">
  <b>Built for real companies, by <a href="https://github.com/davex-ai">Dave</b>
</p>