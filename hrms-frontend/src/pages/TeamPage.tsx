import { useEffect, useState } from 'react'
import { Users } from 'lucide-react'
import { employeeApi } from '../api/employees'
import type { EmployeeResponse } from '../types'
import { Card, CardHeader } from '../components/Card'
import { EmptyState, Alert } from '../components/Alert'
import { HttpError } from '../api/client'

export function TeamPage() {
  const [team, setTeam] = useState<EmployeeResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    employeeApi
      .getMyTeam()
      .then(setTeam)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load team'))
      .finally(() => setIsLoading(false))
  }, [])

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink-900">My team</h1>
        <p className="mt-1 text-sm text-ink-500">Employees who report directly to you.</p>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      <Card>
        <CardHeader title={`${team.length} direct report${team.length === 1 ? '' : 's'}`} action={<Users className="h-5 w-5 text-ink-400" />} />
        {isLoading ? (
          <p className="text-sm text-ink-500">Loading…</p>
        ) : team.length === 0 ? (
          <EmptyState title="No direct reports" description="Employees assigned to you will appear here." />
        ) : (
          <div className="flex flex-col divide-y divide-ink-100">
            {team.map((member) => (
              <div key={member.id} className="flex items-center justify-between gap-4 py-3.5">
                <div className="flex items-center gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-accent-100 text-xs font-semibold text-accent-700">
                    {member.firstName[0]}
                    {member.lastName[0]}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-ink-900">
                      {member.firstName} {member.lastName}
                    </p>
                    <p className="text-xs text-ink-500">{member.jobTitle ?? '—'}</p>
                  </div>
                </div>
                <p className="text-xs text-ink-500">{member.department ?? '—'}</p>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}
