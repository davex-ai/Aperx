import { useEffect, useState, type FormEvent } from 'react'
import { Landmark, Phone, Briefcase, Calendar, Save, UserPlus } from 'lucide-react'
import { employeeApi } from '../api/employees'
import type { EmployeeResponse } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Input } from '../components/Form'
import { Button } from '../components/Button'
import { Alert } from '../components/Alert'
import { HttpError } from '../api/client'

export function ProfilePage() {
  const [profile, setProfile] = useState<EmployeeResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [phoneNumber, setPhoneNumber] = useState('')
  const [bankName, setBankName] = useState('')
  const [accountHolderName, setAccountHolderName] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [routingNumber, setRoutingNumber] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [saveSuccess, setSaveSuccess] = useState(false)

  const [contactName, setContactName] = useState('')
  const [relationship, setRelationship] = useState('')
  const [contactPhone, setContactPhone] = useState('')
  const [contactEmail, setContactEmail] = useState('')
  const [isSavingContact, setIsSavingContact] = useState(false)
  const [contactSuccess, setContactSuccess] = useState(false)

  useEffect(() => {
    employeeApi
      .getMyProfile()
      .then((data) => {
        setProfile(data)
        setPhoneNumber(data.phoneNumber ?? '')
      })
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load profile'))
      .finally(() => setIsLoading(false))
  }, [])

  async function handleSaveContact(e: FormEvent) {
    e.preventDefault()
    setIsSaving(true)
    setSaveSuccess(false)
    setError(null)
    try {
      const updated = await employeeApi.updateMyProfile({
        phoneNumber,
        bankName: bankName || undefined,
        accountHolderName: accountHolderName || undefined,
        accountNumber: accountNumber || undefined,
        routingNumber: routingNumber || undefined,
      })
      setProfile(updated)
      setSaveSuccess(true)
      setBankName('')
      setAccountHolderName('')
      setAccountNumber('')
      setRoutingNumber('')
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to save changes')
    } finally {
      setIsSaving(false)
    }
  }

  async function handleAddEmergencyContact(e: FormEvent) {
    e.preventDefault()
    setIsSavingContact(true)
    setContactSuccess(false)
    setError(null)
    try {
      await employeeApi.addEmergencyContact({
        contactName,
        relationship: relationship || undefined,
        phoneNumber: contactPhone,
        email: contactEmail || undefined,
      })
      setContactSuccess(true)
      setContactName('')
      setRelationship('')
      setContactPhone('')
      setContactEmail('')
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to add contact')
    } finally {
      setIsSavingContact(false)
    }
  }

  if (isLoading) {
    return <div className="text-sm text-ink-500">Loading profile…</div>
  }

  if (!profile) {
    return <Alert variant="error">{error ?? 'Profile unavailable'}</Alert>
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink-900">My profile</h1>
        <p className="mt-1 text-sm text-ink-500">Your role, job details, and personal information.</p>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      <Card>
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-semibold text-ink-900">
              {profile.firstName} {profile.lastName}
            </h2>
            <p className="text-sm text-ink-500">{profile.companyEmail ?? profile.personalEmail}</p>
          </div>
        </div>

        <div className="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2">
          <div className="flex items-start gap-3">
            <Briefcase className="mt-0.5 h-4 w-4 text-ink-400" />
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Job title</p>
              <p className="text-sm text-ink-900">{profile.jobTitle ?? '—'}</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Briefcase className="mt-0.5 h-4 w-4 text-ink-400" />
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Department</p>
              <p className="text-sm text-ink-900">{profile.department ?? '—'}</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Calendar className="mt-0.5 h-4 w-4 text-ink-400" />
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Hire date</p>
              <p className="text-sm text-ink-900">{new Date(profile.hireDate).toLocaleDateString()}</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <UserPlus className="mt-0.5 h-4 w-4 text-ink-400" />
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Salary grade</p>
              <p className="text-sm text-ink-900">{profile.salaryGrade ?? '—'}</p>
            </div>
          </div>
          {profile.managerName && (
            <div className="flex items-start gap-3">
              <UserPlus className="mt-0.5 h-4 w-4 text-ink-400" />
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Manager</p>
                <p className="text-sm text-ink-900">{profile.managerName}</p>
              </div>
            </div>
          )}
          {profile.salaryAmount !== null && (
            <div className="flex items-start gap-3">
              <Landmark className="mt-0.5 h-4 w-4 text-ink-400" />
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Salary</p>
                <p className="text-sm text-ink-900">
                  {profile.salaryAmount.toLocaleString(undefined, { style: 'currency', currency: 'USD' })}
                </p>
              </div>
            </div>
          )}
        </div>
      </Card>

      <Card>
        <CardHeader title="Contact & bank details" subtitle="Only visible to you and HR administrators." />
        {saveSuccess && (
          <div className="mb-4">
            <Alert variant="success">Your details have been updated.</Alert>
          </div>
        )}
        <form onSubmit={handleSaveContact} className="flex flex-col gap-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="Phone number"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              placeholder="+1 (555) 000-0000"
            />
          </div>
          <div className="border-t border-ink-100 pt-4">
            <p className="mb-3 flex items-center gap-1.5 text-sm font-medium text-ink-700">
              <Landmark className="h-4 w-4" /> Bank account
            </p>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Input label="Bank name" value={bankName} onChange={(e) => setBankName(e.target.value)} />
              <Input
                label="Account holder name"
                value={accountHolderName}
                onChange={(e) => setAccountHolderName(e.target.value)}
              />
              <Input label="Account number" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} />
              <Input label="Routing number" value={routingNumber} onChange={(e) => setRoutingNumber(e.target.value)} />
            </div>
          </div>
          <div>
            <Button type="submit" isLoading={isSaving} icon={<Save className="h-4 w-4" />}>
              Save changes
            </Button>
          </div>
        </form>
      </Card>

      <Card>
        <CardHeader title="Add an emergency contact" subtitle="You can add more than one." />
        {contactSuccess && (
          <div className="mb-4">
            <Alert variant="success">Emergency contact added.</Alert>
          </div>
        )}
        <form onSubmit={handleAddEmergencyContact} className="flex flex-col gap-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input label="Contact name" required value={contactName} onChange={(e) => setContactName(e.target.value)} />
            <Input
              label="Relationship"
              value={relationship}
              onChange={(e) => setRelationship(e.target.value)}
              placeholder="e.g. Spouse, Parent"
            />
            <Input
              label="Phone number"
              required
              value={contactPhone}
              onChange={(e) => setContactPhone(e.target.value)}
            />
            <Input
              label="Email (optional)"
              type="email"
              value={contactEmail}
              onChange={(e) => setContactEmail(e.target.value)}
            />
          </div>
          <div>
            <Button type="submit" isLoading={isSavingContact} icon={<Phone className="h-4 w-4" />}>
              Add contact
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
