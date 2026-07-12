import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Building2, ArrowRight, ShieldAlert } from 'lucide-react'
import { authApi } from '../api/auth'
import { Input } from '../components/Form'
import { Button } from '../components/Button'
import { Alert } from '../components/Alert'
import { HttpError } from '../api/client'
import { useAuth } from '../context/AuthContext'

const STEPS = ['Contact details', 'Bank details', 'Emergency contact'] as const

export function OnboardingProfilePage() {
  const navigate = useNavigate()
  const { session, refreshOnboardingFlag } = useAuth()
  const [step, setStep] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const [phoneNumber, setPhoneNumber] = useState('')
  const [bankName, setBankName] = useState('')
  const [accountHolderName, setAccountHolderName] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [routingNumber, setRoutingNumber] = useState('')
  const [emergencyContactName, setEmergencyContactName] = useState('')
  const [emergencyRelationship, setEmergencyRelationship] = useState('')
  const [emergencyPhoneNumber, setEmergencyPhoneNumber] = useState('')

  function goNext(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (step < STEPS.length - 1) {
      setStep((s) => s + 1)
    } else {
      void submit()
    }
  }

  async function submit() {
    setIsLoading(true)
    setError(null)
    try {
      await authApi.completeFirstLoginProfile({
        phoneNumber,
        bankName: bankName || undefined,
        accountHolderName: accountHolderName || undefined,
        accountNumber: accountNumber || undefined,
        routingNumber: routingNumber || undefined,
        emergencyContactName: emergencyContactName || undefined,
        emergencyRelationship: emergencyRelationship || undefined,
        emergencyPhoneNumber: emergencyPhoneNumber || undefined,
      })
      refreshOnboardingFlag(false)
      navigate(session?.role === 'ROLE_ADMIN' ? '/dashboard' : '/profile')
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-ink-50 px-6 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-ink-900">
            <Building2 className="h-4.5 w-4.5 text-white" />
          </div>
          <span className="text-[15px] font-semibold tracking-tight text-ink-900">AperX</span>
        </div>

        <div className="rounded-[var(--radius-card)] border border-ink-200 bg-white p-6">
          <div className="mb-6 flex items-center gap-2">
            {STEPS.map((label, index) => (
              <div key={label} className="flex flex-1 items-center gap-2">
                <div
                  className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                    index <= step ? 'bg-accent-600 text-white' : 'bg-ink-100 text-ink-400'
                  }`}
                >
                  {index + 1}
                </div>
                {index < STEPS.length - 1 && (
                  <div className={`h-0.5 flex-1 ${index < step ? 'bg-accent-600' : 'bg-ink-200'}`} />
                )}
              </div>
            ))}
          </div>

          <h1 className="text-lg font-semibold text-ink-900">{STEPS[step]}</h1>
          <p className="mt-1 text-sm text-ink-500">
            {step === 0 && 'Let HR know how to reach you.'}
            {step === 1 && 'Where should your salary be paid? You can skip this and add it later.'}
            {step === 2 && "Who should we contact if there's an emergency? Optional, but recommended."}
          </p>

          <form onSubmit={goNext} className="mt-6 flex flex-col gap-4">
            {error && <Alert variant="error">{error}</Alert>}

            {step === 0 && (
              <Input
                label="Phone number"
                required
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="+1 (555) 000-0000"
              />
            )}

            {step === 1 && (
              <>
                <Input label="Bank name" value={bankName} onChange={(e) => setBankName(e.target.value)} />
                <Input
                  label="Account holder name"
                  value={accountHolderName}
                  onChange={(e) => setAccountHolderName(e.target.value)}
                />
                <Input
                  label="Account number"
                  value={accountNumber}
                  onChange={(e) => setAccountNumber(e.target.value)}
                />
                <Input
                  label="Routing number"
                  value={routingNumber}
                  onChange={(e) => setRoutingNumber(e.target.value)}
                />
              </>
            )}

            {step === 2 && (
              <>
                <Input
                  label="Contact name"
                  value={emergencyContactName}
                  onChange={(e) => setEmergencyContactName(e.target.value)}
                />
                <Input
                  label="Relationship"
                  value={emergencyRelationship}
                  onChange={(e) => setEmergencyRelationship(e.target.value)}
                  placeholder="e.g. Spouse, Parent"
                />
                <Input
                  label="Phone number"
                  value={emergencyPhoneNumber}
                  onChange={(e) => setEmergencyPhoneNumber(e.target.value)}
                />
              </>
            )}

            <div className="mt-2 flex items-center gap-3">
              {step > 0 && (
                <Button type="button" variant="secondary" onClick={() => setStep((s) => s - 1)}>
                  Back
                </Button>
              )}
              <Button
                type="submit"
                isLoading={isLoading}
                className="flex-1"
                icon={step === STEPS.length - 1 ? undefined : <ArrowRight className="h-4 w-4" />}
              >
                {step === STEPS.length - 1 ? 'Finish setup' : 'Continue'}
              </Button>
            </div>
          </form>
        </div>

        <p className="mt-4 flex items-center justify-center gap-1.5 text-xs text-ink-500">
          <ShieldAlert className="h-3.5 w-3.5" />
          Your bank and contact details are only visible to HR administrators.
        </p>
      </div>
    </div>
  )
}
