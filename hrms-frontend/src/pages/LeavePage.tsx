import { useEffect, useState, type FormEvent } from 'react'
import { CalendarPlus, Check, X, ClipboardList } from 'lucide-react'
import { leaveApi } from '../api/leave'
import type { LeaveBalanceResponse, LeaveRequestResponse, LeaveType } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Input, Select, Textarea } from '../components/Form'
import { Button } from '../components/Button'
import { Alert, EmptyState } from '../components/Alert'
import { StatusBadge } from '../components/StatusBadge'
import { HttpError } from '../api/client'
import { useAuth } from '../context/AuthContext'

const LEAVE_TYPES: { value: LeaveType; label: string }[] = [
  { value: 'ANNUAL', label: 'Annual' },
  { value: 'SICK', label: 'Sick' },
  { value: 'MATERNITY', label: 'Maternity' },
  { value: 'CASUAL', label: 'Casual' },
]

export function LeavePage() {
  const { session } = useAuth()
  const isReviewer = session?.role === 'ROLE_ADMIN' || session?.role === 'ROLE_MANAGER'

  const [balances, setBalances] = useState<LeaveBalanceResponse[]>([])
  const [myRequests, setMyRequests] = useState<LeaveRequestResponse[]>([])
  const [pendingRequests, setPendingRequests] = useState<LeaveRequestResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [type, setType] = useState<LeaveType>('ANNUAL')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [reason, setReason] = useState('')
  const [certificateUrl, setCertificateUrl] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitSuccess, setSubmitSuccess] = useState(false)

  const [reviewingId, setReviewingId] = useState<number | null>(null)

  function loadAll() {
    const calls: Promise<void>[] = [
      leaveApi.myBalances().then(setBalances),
      leaveApi.myRequests().then(setMyRequests),
    ]
    if (isReviewer) {
      calls.push(leaveApi.pendingRequests().then(setPendingRequests))
    }
    Promise.all(calls)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load leave data'))
      .finally(() => setIsLoading(false))
  }

  useEffect(() => {
    loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitSuccess(false)
    setIsSubmitting(true)
    try {
      await leaveApi.submit({
        type,
        startDate,
        endDate,
        reason: reason || undefined,
        certificateUrl: certificateUrl || undefined,
      })
      setSubmitSuccess(true)
      setStartDate('')
      setEndDate('')
      setReason('')
      setCertificateUrl('')
      loadAll()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to submit request')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleReview(id: number, status: 'APPROVED' | 'REJECTED') {
    setReviewingId(id)
    setError(null)
    try {
      await leaveApi.review(id, status)
      loadAll()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to review request')
    } finally {
      setReviewingId(null)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Leave</h1>
        <p className="mt-1 text-sm text-ink-500">Check your balance, request time off, and track approvals.</p>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {balances.map((b) => (
          <Card key={b.type} padded={false} className="p-5">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">
              {LEAVE_TYPES.find((t) => t.value === b.type)?.label}
            </p>
            <p className="mt-1.5 text-2xl font-semibold text-ink-900">{b.remainingDays}</p>
            <p className="text-xs text-ink-500">of {b.totalDays} days remaining</p>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader title="Request time off" />
            {submitSuccess && (
              <div className="mb-4">
                <Alert variant="success">Your request has been submitted for review.</Alert>
              </div>
            )}
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <Select label="Leave type" value={type} onChange={(e) => setType(e.target.value as LeaveType)} required>
                {LEAVE_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </Select>
              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="Start date"
                  type="date"
                  required
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
                <Input
                  label="End date"
                  type="date"
                  required
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </div>
              <Textarea
                label="Reason (optional)"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Add any context for your manager"
              />
              {type === 'SICK' && (
                <Input
                  label="Medical certificate URL (optional)"
                  value={certificateUrl}
                  onChange={(e) => setCertificateUrl(e.target.value)}
                  hint="Required for sick leave of 3 or more days"
                />
              )}
              <Button type="submit" isLoading={isSubmitting} icon={<CalendarPlus className="h-4 w-4" />}>
                Submit request
              </Button>
            </form>
          </Card>
        </div>

        <div className="lg:col-span-3">
          <Card>
            <CardHeader title="My requests" />
            {isLoading ? (
              <p className="text-sm text-ink-500">Loading…</p>
            ) : myRequests.length === 0 ? (
              <EmptyState title="No leave requests yet" description="Submit your first request to see it here." />
            ) : (
              <div className="flex flex-col divide-y divide-ink-100">
                {myRequests.map((r) => (
                  <div key={r.id} className="flex items-center justify-between gap-4 py-3.5">
                    <div>
                      <p className="text-sm font-medium text-ink-900">
                        {LEAVE_TYPES.find((t) => t.value === r.type)?.label} leave
                      </p>
                      <p className="text-xs text-ink-500">
                        {new Date(r.startDate).toLocaleDateString()} – {new Date(r.endDate).toLocaleDateString()}
                      </p>
                      {r.reviewComment && (
                        <p className="mt-1 text-xs italic text-ink-500">"{r.reviewComment}"</p>
                      )}
                    </div>
                    <StatusBadge status={r.status} />
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>

      {isReviewer && (
        <Card>
          <CardHeader
            title="Pending approvals"
            subtitle="Requests waiting on your review."
            action={<ClipboardList className="h-5 w-5 text-ink-400" />}
          />
          {pendingRequests.length === 0 ? (
            <EmptyState title="Nothing pending" description="All caught up — no leave requests awaiting review." />
          ) : (
            <div className="flex flex-col divide-y divide-ink-100">
              {pendingRequests.map((r) => (
                <div key={r.id} className="flex items-center justify-between gap-4 py-3.5">
                  <div>
                    <p className="text-sm font-medium text-ink-900">{r.employeeName}</p>
                    <p className="text-xs text-ink-500">
                      {LEAVE_TYPES.find((t) => t.value === r.type)?.label} ·{' '}
                      {new Date(r.startDate).toLocaleDateString()} – {new Date(r.endDate).toLocaleDateString()}
                    </p>
                    {r.reason && <p className="mt-1 text-xs text-ink-500">{r.reason}</p>}
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="secondary"
                      size="sm"
                      isLoading={reviewingId === r.id}
                      icon={<X className="h-3.5 w-3.5" />}
                      onClick={() => handleReview(r.id, 'REJECTED')}
                    >
                      Reject
                    </Button>
                    <Button
                      size="sm"
                      isLoading={reviewingId === r.id}
                      icon={<Check className="h-3.5 w-3.5" />}
                      onClick={() => handleReview(r.id, 'APPROVED')}
                    >
                      Approve
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      )}
    </div>
  )
}
