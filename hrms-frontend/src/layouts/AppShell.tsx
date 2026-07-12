import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  User,
  CalendarDays,
  Briefcase,
  Users,
  Wallet,
  LogOut,
  Building2,
  Megaphone,
  Clock,
  ShieldAlert,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'

interface NavItem {
  to: string
  label: string
  icon: typeof LayoutDashboard
  roles: Array<'ROLE_ADMIN' | 'ROLE_MANAGER' | 'ROLE_EMPLOYEE'>
}

const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, roles: ['ROLE_ADMIN'] },
  { to: '/announcements', label: 'Announcements', icon: Megaphone, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'] },
  { to: '/time-tracking', label: 'Time tracking', icon: Clock, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'] },
  { to: '/reports', label: 'Anonymous reports', icon: ShieldAlert, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'] },
  { to: '/profile', label: 'My profile', icon: User, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'] },
  { to: '/leave', label: 'Leave', icon: CalendarDays, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'] },
  { to: '/payroll', label: 'Payroll & documents', icon: Wallet, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'] },
  { to: '/team', label: 'My team', icon: Users, roles: ['ROLE_MANAGER'] },
  { to: '/employees', label: 'Employees', icon: Users, roles: ['ROLE_ADMIN'] },
  { to: '/recruitment', label: 'Recruitment', icon: Briefcase, roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
]


export function AppShell() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()

  if (!session) return null

  const visibleItems = NAV_ITEMS.filter((item) => item.roles.includes(session.role))

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const initials = (session.fullName ?? session.email)
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  return (
    <div className="flex min-h-screen bg-ink-50">
      <aside className="flex w-64 shrink-0 flex-col border-r border-ink-200 bg-white">
        <div className="flex items-center gap-2.5 px-6 py-6">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-ink-900">
            <Building2 className="h-4.5 w-4.5 text-white" />
          </div>
          <div className="min-w-0">
            <span className="block text-[15px] font-semibold leading-tight tracking-tight text-ink-900">AperX</span>
            {session.companyName && (
              <span className="block truncate text-xs leading-tight text-ink-500">{session.companyName}</span>
            )}
          </div>
        </div>

        <nav className="flex flex-1 flex-col gap-0.5 px-3">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-accent-50 text-accent-700'
                    : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900'
                }`
              }
            >
              <item.icon className="h-4.5 w-4.5" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-ink-200 p-3">
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-ink-600 transition-colors hover:bg-ink-100 hover:text-danger-600"
          >
            <LogOut className="h-4.5 w-4.5" />
            Sign out
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 shrink-0 items-center justify-between border-b border-ink-200 bg-white px-8">
          <div />
          <div className="flex items-center gap-3">
            <div className="text-right">
              <p className="text-sm font-medium text-ink-900">{session.fullName ?? session.email}</p>
              <p className="text-xs text-ink-500">
                {session.role === 'ROLE_ADMIN' ? 'Administrator' : session.role === 'ROLE_MANAGER' ? 'Manager' : 'Employee'}
              </p>
            </div>
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-accent-100 text-xs font-semibold text-accent-700">
              {initials}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto px-8 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
