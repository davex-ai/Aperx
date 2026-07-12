import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { UserRole } from '../types'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { session, isLoading } = useAuth()

  if (isLoading) return null
  if (!session) return <Navigate to="/login" replace />
  if (session.mustCompleteOnboarding) return <Navigate to="/onboarding/profile" replace />

  return <>{children}</>
}

export function RequireRole({ roles, children }: { roles: UserRole[]; children: ReactNode }) {
  const { session } = useAuth()
  if (!session) return null
  if (!roles.includes(session.role)) return <Navigate to="/profile" replace />
  return <>{children}</>
}
