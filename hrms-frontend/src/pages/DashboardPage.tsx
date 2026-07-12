import { useEffect, useState } from 'react'
import { Users, TrendingUp, TrendingDown, Briefcase, CalendarClock, Building2 } from 'lucide-react'
import { dashboardApi } from '../api/dashboard'
import type { DashboardStatsResponse } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Alert } from '../components/Alert'
import { HttpError } from '../api/client'

function StatCard({
  label,
  value,
  icon: Icon,
  trend,
}: {
  label: string
  value: string | number
  icon: typeof Users
  trend?: 'up' | 'down'
}) {
  return (
    <Card>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-ink-400">{label}</p>
          <p className="mt-2 text-2xl font-semibold text-ink-900">{value}</p>
        </div>
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent-50 text-accent-700">
          <Icon className="h-4.5 w-4.5" />
        </div>
      </div>
      {trend && (
        <div className={`mt-3 flex items-center gap-1 text-xs font-medium ${trend === 'up' ? 'text-success-700' : 'text-danger-700'}`}>
          {trend === 'up' ? <TrendingUp className="h-3.5 w-3.5" /> : <TrendingDown className="h-3.5 w-3.5" />}
        </div>
      )}
    </Card>
  )
}

export function DashboardPage() {
  const [stats, setStats] = useState<DashboardStatsResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    dashboardApi
      .getStats()
      .then(setStats)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load dashboard'))
  }, [])

  if (error) return <Alert variant="error">{error}</Alert>
  if (!stats) return <div className="text-sm text-ink-500">Loading dashboard…</div>

  const maxTrend = Math.max(...stats.headcountTrend.map((t) => t.count), 1)
  const maxDept = Math.max(...Object.values(stats.departmentDistribution), 1)

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Dashboard</h1>
        <p className="mt-1 text-sm text-ink-500">A snapshot of your workforce right now.</p>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Total headcount" value={stats.totalHeadcount} icon={Users} />
        <StatCard label="New hires this month" value={stats.newHiresThisMonth} icon={TrendingUp} />
        <StatCard label="Turnover rate" value={`${stats.turnoverRatePercent}%`} icon={TrendingDown} />
        <StatCard label="Open roles" value={stats.openJobPostings} icon={Briefcase} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title="Headcount trend" subtitle="Cumulative active employees, last 6 months" />
          <div className="flex h-48 items-end gap-4">
            {stats.headcountTrend.map((point) => (
              <div key={point.month} className="flex flex-1 flex-col items-center gap-2">
                <div className="flex w-full flex-1 items-end">
                  <div
                    className="w-full rounded-t-md bg-accent-500"
                    style={{ height: `${(point.count / maxTrend) * 100}%` }}
                  />
                </div>
                <span className="text-xs text-ink-500">{point.month}</span>
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <CardHeader title="Departments" action={<Building2 className="h-5 w-5 text-ink-400" />} />
          <div className="flex flex-col gap-3">
            {Object.entries(stats.departmentDistribution).map(([dept, count]) => (
              <div key={dept}>
                <div className="mb-1 flex items-center justify-between text-xs">
                  <span className="font-medium text-ink-700">{dept}</span>
                  <span className="text-ink-500">{count}</span>
                </div>
                <div className="h-1.5 w-full rounded-full bg-ink-100">
                  <div
                    className="h-1.5 rounded-full bg-accent-600"
                    style={{ width: `${(count / maxDept) * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card>
        <CardHeader title="Pending action" action={<CalendarClock className="h-5 w-5 text-ink-400" />} />
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-warning-50 text-warning-700">
            <CalendarClock className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-medium text-ink-900">
              {stats.pendingLeaveRequests} leave request{stats.pendingLeaveRequests === 1 ? '' : 's'} awaiting review
            </p>
            <p className="text-xs text-ink-500">Head to the Leave section to approve or reject.</p>
          </div>
        </div>
      </Card>
    </div>
  )
}
