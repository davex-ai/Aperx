import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Building2, ShieldCheck } from 'lucide-react'
import { authApi } from '../api/auth'
import { Input } from '../components/Form'
import { Button } from '../components/Button'
import { Alert } from '../components/Alert'
import { HttpError } from '../api/client'

export function VerifyAccountPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const navigate = useNavigate()

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isDone, setIsDone] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }
    if (!token) {
      setError('This verification link is missing its token. Please use the link from your email.')
      return
    }

    setIsLoading(true)
    try {
      await authApi.completeSignup(token, password)
      setIsDone(true)
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-ink-50 px-6">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-ink-900">
            <Building2 className="h-4.5 w-4.5 text-white" />
          </div>
          <span className="text-[15px] font-semibold tracking-tight text-ink-900">AperX</span>
        </div>

        {isDone ? (
          <div className="rounded-[var(--radius-card)] border border-ink-200 bg-white p-6 text-center">
            <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-success-50 text-success-700">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <h1 className="text-lg font-semibold text-ink-900">Account verified</h1>
            <p className="mt-1.5 text-sm text-ink-500">Your password is set. You can now sign in.</p>
            <Button className="mt-6 w-full" onClick={() => navigate('/login')}>
              Go to sign in
            </Button>
          </div>
        ) : (
          <>
            <h1 className="text-xl font-semibold text-ink-900">Set your password</h1>
            <p className="mt-1.5 text-sm text-ink-500">
              Welcome to AperX. Choose a password to activate your account.
            </p>

            <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
              {error && <Alert variant="error">{error}</Alert>}

              <Input
                label="New password"
                type="password"
                required
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                hint="At least 8 characters."
              />
              <Input
                label="Confirm password"
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />

              <Button type="submit" isLoading={isLoading} className="mt-2 w-full">
                Activate account
              </Button>
            </form>
          </>
        )}
      </div>
    </div>
  )
}
