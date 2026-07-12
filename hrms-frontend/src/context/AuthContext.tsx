import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import { setToken } from '../api/client'
import type { AuthResponse, UserRole } from '../types'

interface Session {
  email: string
  role: UserRole
  companySlug: string | null
  fullName: string | null
  employeeId: number | null
  mustCompleteOnboarding: boolean
  companyId: number | null
  companyName: string | null
}

interface AuthContextValue {
  session: Session | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<AuthResponse>
  logout: () => void
  refreshOnboardingFlag: (value: boolean) => void
  setSessionFromAuthResponse: (response: AuthResponse) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

const SESSION_KEY = 'hrms_session'

function sessionFromResponse(response: AuthResponse): Session {
  return {
    email: response.email,
    role: response.role,
    companySlug: response.companySlug,
    fullName: response.fullName,
    employeeId: response.employeeId,
    mustCompleteOnboarding: response.mustCompleteOnboarding,
    companyId: response.companyId,
    companyName: response.companyName,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem(SESSION_KEY)
    if (stored) {
      try {
        setSession(JSON.parse(stored))
      } catch {
        localStorage.removeItem(SESSION_KEY)
      }
    }
    setIsLoading(false)
  }, [])

  async function login(email: string, password: string) {
    const response = await authApi.login(email, password)
    setToken(response.token)
    const newSession = sessionFromResponse(response)
    setSession(newSession)
    localStorage.setItem(SESSION_KEY, JSON.stringify(newSession))
    return response
  }

  function setSessionFromAuthResponse(response: AuthResponse) {
    const newSession = sessionFromResponse(response)
    setSession(newSession)
    localStorage.setItem(SESSION_KEY, JSON.stringify(newSession))
  }

  function logout() {
    setToken(null)
    setSession(null)
    localStorage.removeItem(SESSION_KEY)
  }

  function refreshOnboardingFlag(value: boolean) {
    setSession((prev) => {
      if (!prev) return prev
      const next = { ...prev, mustCompleteOnboarding: value }
      localStorage.setItem(SESSION_KEY, JSON.stringify(next))
      return next
    })
  }

  return (
    <AuthContext.Provider
      value={{ session, isLoading, login, logout, refreshOnboardingFlag, setSessionFromAuthResponse }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
