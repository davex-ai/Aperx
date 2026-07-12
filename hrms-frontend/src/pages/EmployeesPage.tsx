import { useEffect, useState, type FormEvent } from 'react'
import { UserPlus, Mail, Send, X, AtSign, UserX } from 'lucide-react'
import { employeeApi, type CreateEmployeePayload } from '../api/employees'
import type { EmployeeResponse, UserRole } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Input, Select } from '../components/Form'
import { Button } from '../components/Button'
import { Alert, EmptyState } from '../components/Alert'
import { HttpError } from '../api/client'

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'ROLE_EMPLOYEE', label: 'Employee' },
  { value: 'ROLE_MANAGER', label: 'Manager' },
  { value: 'ROLE_ADMIN', label: 'Admin' },
]

const STAGE_LABELS: Record<EmployeeResponse['onboardingStage'], string> = {
  DRAFT: 'Awaiting company email',
  INVITED: 'Invited',
  ACTIVE: 'Active',
}

const STAGE_STYLES: Record<EmployeeResponse['onboardingStage'], string> = {
  DRAFT: 'bg-warning-50 text-warning-700',
  INVITED: 'bg-accent-50 text-accent-700',
  ACTIVE: 'bg-success-50 text-success-700',
}

export function EmployeesPage() {
  const [employees, setEmployees] = useState<EmployeeResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [assigningEmailFor, setAssigningEmailFor] = useState<EmployeeResponse | null>(null)
  const [terminatingEmployee, setTerminatingEmployee] = useState<EmployeeResponse | null>(null)

  function loadEmployees() {
    employeeApi
      .getAllEmployees()
      .then(setEmployees)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load employees'))
      .finally(() => setIsLoading(false))
  }

  useEffect(() => {
    loadEmployees()
  }, [])

  async function handleResend(id: number) {
    setBusyId(id)
    setMessage(null)
    try {
      const sent = await employeeApi.resendInvitation(id)
      setMessage(sent ? 'Invitation email sent.' : 'Invitation could not be sent — check email provider configuration.')
    } catch (err) {
      setMessage(err instanceof HttpError ? err.message : 'Failed to resend invitation')
    } finally {
      setBusyId(null)
    }
  }

  async function handleTerminateConfirmed() {
    if (!terminatingEmployee) return
    setBusyId(terminatingEmployee.id)
    setMessage(null)
    try {
      await employeeApi.terminateEmployee(terminatingEmployee.id)
      setMessage(`${terminatingEmployee.firstName} ${terminatingEmployee.lastName} has been terminated.`)
      setTerminatingEmployee(null)
      loadEmployees()
    } catch (err) {
      setMessage(err instanceof HttpError ? err.message : 'Failed to terminate employee')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Employees</h1>
          <p className="mt-1 text-sm text-ink-500">Manage staff accounts, roles, and onboarding.</p>
        </div>
        <Button icon={<UserPlus className="h-4 w-4" />} onClick={() => setIsFormOpen(true)}>
          Add employee
        </Button>
      </div>

      {error && <Alert variant="error">{error}</Alert>}
      {message && <Alert variant="info">{message}</Alert>}

      {isFormOpen && (
        <CreateEmployeeForm
          onClose={() => setIsFormOpen(false)}
          onCreated={() => {
            setIsFormOpen(false)
            loadEmployees()
          }}
          managers={employees.filter((e) => e.role === 'ROLE_MANAGER' || e.role === 'ROLE_ADMIN')}
        />
      )}

      {assigningEmailFor && (
        <AssignCompanyEmailForm
          employee={assigningEmailFor}
          onClose={() => setAssigningEmailFor(null)}
          onAssigned={(sent) => {
            setAssigningEmailFor(null)
            setMessage(sent ? 'Company email assigned and invitation sent.' : 'Company email assigned, but the invitation email failed to send.')
            loadEmployees()
          }}
        />
      )}

      {terminatingEmployee && (
        <Card className="border-danger-200">
          <CardHeader
            title={`Terminate ${terminatingEmployee.firstName} ${terminatingEmployee.lastName}?`}
            subtitle="They will immediately lose access to sign in. This cannot be undone from here."
          />
          <div className="flex items-center gap-3">
            <Button
              variant="danger"
              isLoading={busyId === terminatingEmployee.id}
              onClick={handleTerminateConfirmed}
            >
              Confirm termination
            </Button>
            <Button variant="secondary" onClick={() => setTerminatingEmployee(null)}>
              Cancel
            </Button>
          </div>
        </Card>
      )}

      <Card>
        <CardHeader title={`${employees.length} employees`} />
        {isLoading ? (
          <p className="text-sm text-ink-500">Loading…</p>
        ) : employees.length === 0 ? (
          <EmptyState title="No employees yet" description="Add your first employee to get started." />
        ) : (
          <div className="flex flex-col divide-y divide-ink-100">
            {employees.map((emp) => (
              <div key={emp.id} className="flex items-center justify-between gap-4 py-3.5">
                <div className="flex items-center gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-accent-100 text-xs font-semibold text-accent-700">
                    {emp.firstName[0]}
                    {emp.lastName[0]}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-ink-900">
                      {emp.firstName} {emp.lastName}
                      {!emp.active && <span className="ml-2 text-xs font-normal text-danger-600">Terminated</span>}
                    </p>
                    <p className="text-xs text-ink-500">
                      {emp.jobTitle ?? '—'} · {emp.department ?? '—'}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${STAGE_STYLES[emp.onboardingStage]}`}>
                    {STAGE_LABELS[emp.onboardingStage]}
                  </span>
                  <span className="text-xs font-medium text-ink-500">
                    {ROLE_OPTIONS.find((r) => r.value === emp.role)?.label}
                  </span>

                  {emp.onboardingStage === 'DRAFT' && emp.active && (
                    <Button
                      variant="secondary"
                      size="sm"
                      icon={<AtSign className="h-3.5 w-3.5" />}
                      onClick={() => setAssigningEmailFor(emp)}
                    >
                      Assign company email
                    </Button>
                  )}

                  {emp.invitationSent === false && emp.onboardingStage === 'INVITED' && (
                    <Button
                      variant="secondary"
                      size="sm"
                      isLoading={busyId === emp.id}
                      icon={<Send className="h-3.5 w-3.5" />}
                      onClick={() => handleResend(emp.id)}
                    >
                      Resend invite
                    </Button>
                  )}

                  {emp.active && (
                    <button
                      onClick={() => setTerminatingEmployee(emp)}
                      className="text-ink-400 hover:text-danger-600"
                      title="Terminate employee"
                    >
                      <UserX className="h-4 w-4" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}

function AssignCompanyEmailForm({
  employee,
  onClose,
  onAssigned,
}: {
  employee: EmployeeResponse
  onClose: () => void
  onAssigned: (invitationSent: boolean) => void
}) {
  const [companyEmail, setCompanyEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      const updated = await employeeApi.assignCompanyEmail(employee.id, companyEmail)
      onAssigned(updated.invitationSent === true)
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to assign company email')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card>
      <CardHeader
        title={`Assign a company email to ${employee.firstName} ${employee.lastName}`}
        subtitle={`Personal email on file: ${employee.personalEmail}. The invitation link will be sent there.`}
        action={
          <button onClick={onClose} className="text-ink-400 hover:text-ink-600">
            <X className="h-5 w-5" />
          </button>
        }
      />
      {error && (
        <div className="mb-4">
          <Alert variant="error">{error}</Alert>
        </div>
      )}
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input
          label="Company email"
          type="email"
          required
          value={companyEmail}
          onChange={(e) => setCompanyEmail(e.target.value)}
          placeholder="firstname.lastname@yourcompany.com"
        />
        <div className="flex items-center gap-3">
          <Button type="submit" isLoading={isSubmitting} icon={<Mail className="h-4 w-4" />}>
            Assign & send invite
          </Button>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </form>
    </Card>
  )
}

function CreateEmployeeForm({
  onClose,
  onCreated,
  managers,
}: {
  onClose: () => void
  onCreated: () => void
  managers: EmployeeResponse[]
}) {
  const [form, setForm] = useState<CreateEmployeePayload>({
    personalEmail: '',
    firstName: '',
    lastName: '',
    department: '',
    jobTitle: '',
    hireDate: '',
    salaryAmount: 0,
    payType: 'SALARIED',
    role: 'ROLE_EMPLOYEE',
  })
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function update<K extends keyof CreateEmployeePayload>(key: K, value: CreateEmployeePayload[K]) {
    setForm((f) => ({ ...f, [key]: value }))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await employeeApi.createEmployee(form)
      onCreated()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to create employee')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card>
      <CardHeader
        title="Add a new employee"
        subtitle="This creates their profile. You'll assign their company email and send the invite as a separate step."
        action={
          <button onClick={onClose} className="text-ink-400 hover:text-ink-600">
            <X className="h-5 w-5" />
          </button>
        }
      />
      {error && (
        <div className="mb-4">
          <Alert variant="error">{error}</Alert>
        </div>
      )}
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input label="First name" required value={form.firstName} onChange={(e) => update('firstName', e.target.value)} />
          <Input label="Last name" required value={form.lastName} onChange={(e) => update('lastName', e.target.value)} />
          <Input
            label="Personal email"
            type="email"
            required
            value={form.personalEmail}
            onChange={(e) => update('personalEmail', e.target.value)}
            hint="Their own inbox — the invite link is sent here, not to the company email"
          />
          <Input label="Phone number" value={form.phoneNumber ?? ''} onChange={(e) => update('phoneNumber', e.target.value)} />
          <Input label="Department" required value={form.department} onChange={(e) => update('department', e.target.value)} />
          <Input label="Job title" required value={form.jobTitle} onChange={(e) => update('jobTitle', e.target.value)} />
          <Input label="Salary grade" value={form.salaryGrade ?? ''} onChange={(e) => update('salaryGrade', e.target.value)} />
          <Input
            label="Salary amount"
            type="number"
            required
            min={0}
            step="0.01"
            value={form.salaryAmount}
            onChange={(e) => update('salaryAmount', Number(e.target.value))}
            hint="Annual salary for salaried employees, or the base figure on record for hourly staff"
          />
          <Select
            label="Pay type"
            required
            value={form.payType}
            onChange={(e) => update('payType', e.target.value as CreateEmployeePayload['payType'])}
          >
            <option value="SALARIED">Salaried</option>
            <option value="HOURLY">Hourly</option>
          </Select>
          {form.payType === 'HOURLY' && (
            <Input
              label="Hourly rate"
              type="number"
              required
              min={0}
              step="0.01"
              value={form.hourlyRate ?? ''}
              onChange={(e) => update('hourlyRate', Number(e.target.value))}
            />
          )}
          <Input label="Hire date" type="date" required value={form.hireDate} onChange={(e) => update('hireDate', e.target.value)} />
          <Select label="Role" required value={form.role} onChange={(e) => update('role', e.target.value as UserRole)}>
            {ROLE_OPTIONS.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </Select>
          <Select
            label="Manager (optional)"
            value={form.managerId ?? ''}
            onChange={(e) => update('managerId', e.target.value ? Number(e.target.value) : undefined)}
          >
            <option value="">No manager</option>
            {managers.map((m) => (
              <option key={m.id} value={m.id}>
                {m.firstName} {m.lastName}
              </option>
            ))}
          </Select>
        </div>
        <div className="flex items-center gap-3">
          <Button type="submit" isLoading={isSubmitting} icon={<UserPlus className="h-4 w-4" />}>
            Create profile
          </Button>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </form>
    </Card>
  )
}
