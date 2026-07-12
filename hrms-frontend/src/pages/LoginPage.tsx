import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Building2, ArrowRight } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { Input } from '../components/Form'
import { Button } from '../components/Button'
import { Alert } from '../components/Alert'
import { HttpError } from '../api/client'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsLoading(true)
    try {
      const response = await login(email, password)
      if (response.mustCompleteOnboarding) {
        navigate('/onboarding/profile')
      } else if (response.role === 'ROLE_ADMIN') {
        navigate('/dashboard')
      } else {
        navigate('/profile')
      }
    } catch (err) {
      if (err instanceof HttpError) {
        setError(err.message)
      } else {
        setError('Something went wrong. Please try again.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen">
      <div className="hidden w-1/2 flex-col justify-between bg-ink-900 px-16 py-12 lg:flex">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/10">
            <Building2 className="h-4.5 w-4.5 text-white" />
          </div>
          <span className="text-[15px] font-semibold tracking-tight text-white">AperX</span>
        </div>

        <div>
          <p className="text-3xl font-semibold leading-snug text-white">
            One record for every employee, from their first day to their last.
          </p>
          <p className="mt-4 max-w-md text-sm leading-relaxed text-ink-300">
            Profiles, leave, payroll, and hiring — in one place your whole
            company can rely on.
          </p>
        </div>

        <p className="text-xs text-ink-500">© {new Date().getFullYear()} AperX</p>
      </div>

      <div className="flex w-full flex-col items-center justify-center px-6 py-12 lg:w-1/2">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-ink-900">
                <Building2 className="h-4.5 w-4.5 text-white" />
              </div>
              <span className="text-[15px] font-semibold tracking-tight text-ink-900">AperX</span>
            </div>
          </div>

          <h1 className="text-xl font-semibold text-ink-900">Sign in</h1>
          <p className="mt-1.5 text-sm text-ink-500">Enter your work email and password to continue.</p>

          <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
            {error && <Alert variant="error">{error}</Alert>}

            <Input
              label="Work email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
            />
            <Input
              label="Password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />

            <Button type="submit" isLoading={isLoading} className="mt-2 w-full" icon={<ArrowRight className="h-4 w-4" />}>
              Sign in
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-ink-500">
            New to AperX?{' '}
            <Link to="/register-company" className="font-medium text-accent-600 hover:text-accent-700">
              Register your company
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
