import { useEffect, useState } from 'react'
import { Clock, MapPin, LogIn, LogOut, Send, Check, X, History } from 'lucide-react'
import { timeTrackingApi } from '../api/timeTracking'
import type { TimeEntryResponse, WeeklyTimesheetResponse } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Button } from '../components/Button'
import { Alert, EmptyState } from '../components/Alert'
import { StatusBadge } from '../components/StatusBadge'
import { HttpError } from '../api/client'
import { useAuth } from '../context/AuthContext'

function formatDuration(hours: number): string {
  const h = Math.floor(hours)
  const m = Math.round((hours - h) * 60)
  return `${h}h ${m}m`
}

function getLocation(): Promise<{ latitude?: number; longitude?: number }> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({})
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
      () => resolve({}),
      { timeout: 5000 },
    )
  })
}

export function TimeTrackingPage() {
  const { session } = useAuth()
  const isReviewer = session?.role === 'ROLE_ADMIN' || session?.role === 'ROLE_MANAGER'

  const [activeEntry, setActiveEntry] = useState<TimeEntryResponse | null>(null)
  const [recentEntries, setRecentEntries] = useState<TimeEntryResponse[]>([])
  const [myTimesheets, setMyTimesheets] = useState<WeeklyTimesheetResponse[]>([])
  const [pendingTimesheets, setPendingTimesheets] = useState<WeeklyTimesheetResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isClocking, setIsClocking] = useState(false)
  const [isSubmittingWeek, setIsSubmittingWeek] = useState(false)
  const [reviewingId, setReviewingId] = useState<number | null>(null)

  function loadAll() {
    const calls: Promise<void>[] = [
      timeTrackingApi.getActive().then(setActiveEntry),
      timeTrackingApi.getMyEntries().then(setRecentEntries),
      timeTrackingApi.getMyTimesheets().then(setMyTimesheets),
    ]
    if (isReviewer) {
      calls.push(timeTrackingApi.getPendingTimesheets().then(setPendingTimesheets))
    }
    Promise.all(calls)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load time tracking data'))
      .finally(() => setIsLoading(false))
  }

  useEffect(() => {
    loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleClockIn() {
    setIsClocking(true)
    setError(null)
    try {
      const location = await getLocation()
      await timeTrackingApi.clockIn(location)
      loadAll()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to clock in')
    } finally {
      setIsClocking(false)
    }
  }

  async function handleClockOut() {
    setIsClocking(true)
    setError(null)
    try {
      const location = await getLocation()
      await timeTrackingApi.clockOut(location)
      loadAll()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to clock out')
    } finally {
      setIsClocking(false)
    }
  }

  async function handleSubmitWeek() {
    setIsSubmittingWeek(true)
    setError(null)
    try {
      await timeTrackingApi.submitCurrentWeek()
      loadAll()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to submit timesheet')
    } finally {
      setIsSubmittingWeek(false)
    }
  }

  async function handleReview(id: number, status: 'APPROVED' | 'REJECTED') {
    setReviewingId(id)
    setError(null)
    try {
      await timeTrackingApi.reviewTimesheet(id, status)
      loadAll()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to review timesheet')
    } finally {
      setReviewingId(null)
    }
  }

  const currentWeekSheet = myTimesheets.find((ts) => ts.status === 'OPEN' || ts.status === 'REJECTED')

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Time tracking</h1>
        <p className="mt-1 text-sm text-ink-500">Clock in and out, and submit your hours for approval.</p>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      <Card>
        <div className="flex flex-col items-center gap-4 py-4 text-center sm:flex-row sm:justify-between sm:text-left">
          <div className="flex items-center gap-3">
            <div
              className={`flex h-12 w-12 items-center justify-center rounded-full ${
                activeEntry ? 'bg-success-50 text-success-700' : 'bg-ink-100 text-ink-400'
              }`}
            >
              <Clock className="h-6 w-6" />
            </div>
            <div>
              <p className="text-sm font-medium text-ink-900">
                {activeEntry ? 'Currently clocked in' : 'Not clocked in'}
              </p>
              <p className="text-xs text-ink-500">
                {activeEntry
                  ? `Since ${new Date(activeEntry.clockInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
                  : 'Clock in to start tracking your time'}
              </p>
            </div>
          </div>
          {activeEntry ? (
            <Button variant="secondary" isLoading={isClocking} icon={<LogOut className="h-4 w-4" />} onClick={handleClockOut}>
              Clock out
            </Button>
          ) : (
            <Button isLoading={isClocking} icon={<LogIn className="h-4 w-4" />} onClick={handleClockIn}>
              Clock in
            </Button>
          )}
        </div>
      </Card>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader
              title="This week"
              subtitle={currentWeekSheet ? `${currentWeekSheet.weekStartDate} – ${currentWeekSheet.weekEndDate}` : undefined}
            />
            {currentWeekSheet ? (
              <>
                <p className="text-3xl font-semibold text-ink-900">{formatDuration(currentWeekSheet.totalHours)}</p>
                <p className="mt-1 text-xs text-ink-500">logged so far</p>
                {currentWeekSheet.status === 'REJECTED' && currentWeekSheet.reviewComment && (
                  <div className="mt-3">
                    <Alert variant="error">Rejected: {currentWeekSheet.reviewComment}</Alert>
                  </div>
                )}
                <Button
                  className="mt-4 w-full"
                  isLoading={isSubmittingWeek}
                  icon={<Send className="h-4 w-4" />}
                  onClick={handleSubmitWeek}
                  disabled={!!activeEntry}
                >
                  Submit for approval
                </Button>
                {activeEntry && <p className="mt-2 text-xs text-ink-500">Clock out before submitting this week.</p>}
              </>
            ) : (
              <p className="text-sm text-ink-500">No hours logged yet this week.</p>
            )}
          </Card>
        </div>

        <div className="lg:col-span-3">
          <Card>
            <CardHeader title="Recent entries" action={<History className="h-5 w-5 text-ink-400" />} />
            {isLoading ? (
              <p className="text-sm text-ink-500">Loading…</p>
            ) : recentEntries.length === 0 ? (
              <EmptyState title="No time entries yet" description="Clock in above to log your first entry." />
            ) : (
              <div className="flex flex-col divide-y divide-ink-100">
                {recentEntries.slice(0, 8).map((entry) => (
                  <div key={entry.id} className="flex items-center justify-between gap-4 py-3">
                    <div>
                      <p className="text-sm font-medium text-ink-900">
                        {new Date(entry.clockInAt).toLocaleDateString(undefined, {
                          weekday: 'short',
                          month: 'short',
                          day: 'numeric',
                        })}
                      </p>
                      <p className="flex items-center gap-1 text-xs text-ink-500">
                        {new Date(entry.clockInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} –{' '}
                        {entry.clockOutAt
                          ? new Date(entry.clockOutAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                          : 'active'}
                        {entry.clockInLat && (
                          <span className="flex items-center gap-0.5 text-ink-400">
                            <MapPin className="h-3 w-3" /> located
                          </span>
                        )}
                      </p>
                    </div>
                    <p className="text-sm font-medium text-ink-700">
                      {entry.isActive ? '—' : formatDuration(entry.durationHours)}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>

      <Card>
        <CardHeader title="Timesheet history" />
        {myTimesheets.length === 0 ? (
          <EmptyState title="No timesheets yet" description="Submitted weeks will appear here." />
        ) : (
          <div className="flex flex-col divide-y divide-ink-100">
            {myTimesheets.map((ts) => (
              <div key={ts.id} className="flex items-center justify-between gap-4 py-3.5">
                <div>
                  <p className="text-sm font-medium text-ink-900">
                    {ts.weekStartDate} – {ts.weekEndDate}
                  </p>
                  <p className="text-xs text-ink-500">{formatDuration(ts.totalHours)} logged</p>
                </div>
                <StatusBadge
                  status={
                    ts.status === 'SUBMITTED'
                      ? 'PENDING'
                      : ts.status === 'APPROVED'
                        ? 'APPROVED'
                        : ts.status === 'REJECTED'
                          ? 'REJECTED'
                          : 'CLOSED'
                  }
                />
              </div>
            ))}
          </div>
        )}
      </Card>

      {isReviewer && (
        <Card>
          <CardHeader title="Pending timesheet approvals" subtitle="Review hours before payroll runs." />
          {pendingTimesheets.length === 0 ? (
            <EmptyState title="Nothing pending" description="All submitted timesheets have been reviewed." />
          ) : (
            <div className="flex flex-col divide-y divide-ink-100">
              {pendingTimesheets.map((ts) => (
                <div key={ts.id} className="flex items-center justify-between gap-4 py-3.5">
                  <div>
                    <p className="text-sm font-medium text-ink-900">{ts.employeeName}</p>
                    <p className="text-xs text-ink-500">
                      {ts.weekStartDate} – {ts.weekEndDate} · {formatDuration(ts.totalHours)}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="secondary"
                      size="sm"
                      isLoading={reviewingId === ts.id}
                      icon={<X className="h-3.5 w-3.5" />}
                      onClick={() => handleReview(ts.id, 'REJECTED')}
                    >
                      Reject
                    </Button>
                    <Button
                      size="sm"
                      isLoading={reviewingId === ts.id}
                      icon={<Check className="h-3.5 w-3.5" />}
                      onClick={() => handleReview(ts.id, 'APPROVED')}
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
