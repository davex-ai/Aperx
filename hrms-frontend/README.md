# AperX — Frontend

React 19 + TypeScript + Tailwind v4, built with Vite.

## Stack
- React Router 7 (client-side routing, role-gated)
- Tailwind CSS v4 (`@theme` tokens in `src/index.css`, no `tailwind.config.js` needed)
- lucide-react for all icons
- No external state library — auth session lives in `AuthContext`, everything else is fetched per-page

## Setup

```bash
npm install
npm run dev
```

Runs on `http://localhost:5173` by default. `vite.config.ts` proxies `/api/*`
to `http://localhost:8080` (the Spring Boot backend), so no CORS config is
needed in dev as long as the backend is running locally.

To point at a different backend, edit the `server.proxy` block in
`vite.config.ts`, or set `VITE_API_BASE` and adjust `src/api/client.ts` if you
prefer an env-driven base URL instead of the proxy.

## Structure

```
src/
  api/          One file per backend controller (auth, employees, leave, recruitment, payroll, dashboard)
  components/   Shared UI primitives (Button, Card, Form fields, StatusBadge, ApplicationPipeline)
  context/      AuthContext — session state, login/logout, token storage
  layouts/      AppShell — sidebar + top bar for authenticated views
  pages/        One file per route
  types/        TypeScript interfaces mirroring the backend's DTOs exactly
```

## Auth flow

Matches the backend's onboarding flow:
1. Admin creates an employee (`/employees` page) → backend emails a verification link
2. Employee opens `/onboarding/verify?token=...` → sets password
3. Employee logs in → backend flags `mustCompleteOnboarding: true` → redirected to `/onboarding/profile`
4. Employee completes the 3-step profile wizard (contact, bank, emergency contact) → dashboard unlocks

The JWT is stored in `localStorage` under `hrms_token`; session metadata
(email, role, name) under `hrms_session`. `RequireAuth` and `RequireRole` in
`src/components/RouteGuards.tsx` enforce access per-route.

## Design notes

- Palette, type, and spacing tokens are defined once in `src/index.css` under `@theme`
- The application pipeline stepper (`ApplicationPipeline.tsx`) is the one
  distinctive visual element — it reflects that hiring is a *process*, not
  just a status label
- Salary and bank data are only ever rendered on the owner's own profile page
  or to admins — the API also enforces this server-side, so this is
  defense-in-depth, not the only guard

## What's not done here

- No automated tests (unit or e2e)
- No dark mode
- No pagination on employee/application lists — fine for small-to-mid
  headcounts, would need it at scale
- Salary field in the "add employee" form has no currency/locale handling
  beyond `toLocaleString`
