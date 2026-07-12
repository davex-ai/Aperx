import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Building2, ArrowRight } from 'lucide-react'
import { authApi } from '../api/auth'
import { setToken } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { Input } from '../components/Form'
import { Button } from '../components/Button'
import { Alert } from '../components/Alert'
import { HttpError } from '../api/client'

export function RegisterCompanyPage() {
  const navigate = useNavigate()
  const { setSessionFromAuthResponse } = useAuth()

  const [companyName, setCompanyName] = useState('')
  const [adminFirstName, setAdminFirstName] = useState('')
  const [adminLastName, setAdminLastName] = useState('')
  const [adminEmail, setAdminEmail] = useState('')
  const [adminPassword, setAdminPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsLoading(true)
    try {
      const response = await authApi.registerCompany({
        companyName,
        adminFirstName,
        adminLastName,
        adminEmail,
        adminPassword,
      })
      setToken(response.token)
      setSessionFromAuthResponse(response)
      navigate('/dashboard')
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Something went wrong. Please try again.')
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
            Set up your company's HR space in minutes.
          </p>
          <p className="mt-4 max-w-md text-sm leading-relaxed text-ink-300">
            You'll be the first administrator. Invite your team once you're in.
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

          <h1 className="text-xl font-semibold text-ink-900">Create your company</h1>
          <p className="mt-1.5 text-sm text-ink-500">Register your company and set up your admin account.</p>

          <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
            {error && <Alert variant="error">{error}</Alert>}

            <Input
              label="Company name"
              required
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              placeholder="Acme Inc."
            />
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Your first name"
                required
                value={adminFirstName}
                onChange={(e) => setAdminFirstName(e.target.value)}
              />
              <Input
                label="Your last name"
                required
                value={adminLastName}
                onChange={(e) => setAdminLastName(e.target.value)}
              />
            </div>
            <Input
              label="Your work email"
              type="email"
              required
              value={adminEmail}
              onChange={(e) => setAdminEmail(e.target.value)}
              placeholder="you@company.com"
            />
            <Input
              label="Password"
              type="password"
              required
              minLength={8}
              value={adminPassword}
              onChange={(e) => setAdminPassword(e.target.value)}
              hint="At least 8 characters."
            />

            <Button type="submit" isLoading={isLoading} className="mt-2 w-full" icon={<ArrowRight className="h-4 w-4" />}>
              Create company
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-ink-500">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-accent-600 hover:text-accent-700">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
