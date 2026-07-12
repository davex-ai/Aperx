import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { RequireAuth, RequireRole } from './components/RouteGuards'
import { AppShell } from './layouts/AppShell'
import { LoginPage } from './pages/LoginPage'
import { RegisterCompanyPage } from './pages/RegisterCompanyPage'
import { VerifyAccountPage } from './pages/VerifyAccountPage'
import { OnboardingProfilePage } from './pages/OnboardingProfilePage'
import { DashboardPage } from './pages/DashboardPage'
import { AnnouncementsPage } from './pages/AnnouncementsPage'
import { TimeTrackingPage } from './pages/TimeTrackingPage'
import { ReportsPage } from './pages/ReportsPage'
import { ProfilePage } from './pages/ProfilePage'
import { LeavePage } from './pages/LeavePage'
import { PayrollPage } from './pages/PayrollPage'
import { TeamPage } from './pages/TeamPage'
import { EmployeesPage } from './pages/EmployeesPage'
import { RecruitmentPage } from './pages/RecruitmentPage'
import { CareersPage } from './pages/CareersPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/careers/:companySlug" element={<CareersPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register-company" element={<RegisterCompanyPage />} />
          <Route path="/onboarding/verify" element={<VerifyAccountPage />} />
          <Route path="/onboarding/profile" element={<OnboardingProfilePage />} />

          <Route
            element={
              <RequireAuth>
                <AppShell />
              </RequireAuth>
            }
          >
            <Route
              path="/dashboard"
              element={
                <RequireRole roles={['ROLE_ADMIN']}>
                  <DashboardPage />
                </RequireRole>
              }
            />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/announcements" element={<AnnouncementsPage />} />
            <Route path="/time-tracking" element={<TimeTrackingPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/leave" element={<LeavePage />} />
            <Route path="/payroll" element={<PayrollPage />} />
            <Route
              path="/team"
              element={
                <RequireRole roles={['ROLE_MANAGER']}>
                  <TeamPage />
                </RequireRole>
              }
            />
            <Route
              path="/employees"
              element={
                <RequireRole roles={['ROLE_ADMIN']}>
                  <EmployeesPage />
                </RequireRole>
              }
            />
            <Route
              path="/recruitment"
              element={
                <RequireRole roles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
                  <RecruitmentPage />
                </RequireRole>
              }
            />
          </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
