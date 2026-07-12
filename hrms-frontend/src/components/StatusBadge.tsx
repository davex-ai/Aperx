import type { ApplicationStatus, JobStatus, LeaveStatus } from '../types'

type AnyStatus = LeaveStatus | ApplicationStatus | JobStatus

const statusStyles: Record<string, string> = {
  PENDING: 'bg-warning-50 text-warning-700 border-warning-100',
  APPROVED: 'bg-success-50 text-success-700 border-success-100',
  REJECTED: 'bg-danger-50 text-danger-700 border-danger-100',
  OPEN: 'bg-success-50 text-success-700 border-success-100',
  CLOSED: 'bg-ink-100 text-ink-600 border-ink-200',
  ARCHIVED: 'bg-ink-100 text-ink-500 border-ink-200',
  APPLIED: 'bg-accent-50 text-accent-700 border-accent-100',
  SCREENING: 'bg-warning-50 text-warning-700 border-warning-100',
  INTERVIEW: 'bg-accent-50 text-accent-700 border-accent-100',
  OFFERED: 'bg-success-50 text-success-700 border-success-100',
  HIRED: 'bg-accent-50 text-accent-700 border-accent-100',
}

const statusLabels: Record<string, string> = {
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  OPEN: 'Open',
  CLOSED: 'Closed',
  ARCHIVED: 'Archived',
  APPLIED: 'Applied',
  SCREENING: 'Screening',
  INTERVIEW: 'Interview',
  OFFERED: 'Offered',
  HIRED: 'Hired',
}

export function StatusBadge({ status }: { status: AnyStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium ${statusStyles[status] ?? 'bg-ink-100 text-ink-600 border-ink-200'}`}
    >
      {statusLabels[status] ?? status}
    </span>
  )
}
