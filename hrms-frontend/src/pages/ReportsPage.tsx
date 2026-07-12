import { useEffect, useState, type FormEvent } from 'react'
import { ShieldAlert, MessageSquare, Send, X, PenLine, UserRound } from 'lucide-react'
import { reportsApi } from '../api/reports'
import type { AnonymousReportResponse, ReportCategory, ReportCommentResponse, ReportStatus } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Input, Select, Textarea } from '../components/Form'
import { Button } from '../components/Button'
import { Alert, EmptyState } from '../components/Alert'
import { HttpError } from '../api/client'
import { useAuth } from '../context/AuthContext'

const CATEGORY_LABELS: Record<ReportCategory, string> = {
  HARASSMENT: 'Harassment',
  DISCRIMINATION: 'Discrimination',
  SAFETY: 'Safety concern',
  ETHICS_VIOLATION: 'Ethics violation',
  FINANCIAL_MISCONDUCT: 'Financial misconduct',
  OTHER: 'Other',
}

const STATUS_LABELS: Record<ReportStatus, string> = {
  OPEN: 'Open',
  UNDER_REVIEW: 'Under review',
  RESOLVED: 'Resolved',
  DISMISSED: 'Dismissed',
}

const STATUS_STYLES: Record<ReportStatus, string> = {
  OPEN: 'bg-warning-50 text-warning-700 border-warning-100',
  UNDER_REVIEW: 'bg-accent-50 text-accent-700 border-accent-100',
  RESOLVED: 'bg-success-50 text-success-700 border-success-100',
  DISMISSED: 'bg-ink-100 text-ink-600 border-ink-200',
}

function timeAgo(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d ago`
  return new Date(iso).toLocaleDateString()
}

export function ReportsPage() {
  const { session } = useAuth()
  const isAdmin = session?.role === 'ROLE_ADMIN'

  const [reports, setReports] = useState<AnonymousReportResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isComposerOpen, setIsComposerOpen] = useState(false)

  function loadFeed() {
    reportsApi
      .getFeed()
      .then(setReports)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load reports'))
      .finally(() => setIsLoading(false))
  }

  useEffect(() => {
    loadFeed()
  }, [])

  async function handleStatusChange(id: number, status: ReportStatus) {
    try {
      const updated = await reportsApi.updateStatus(id, status)
      setReports((prev) => prev.map((r) => (r.id === id ? updated : r)))
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to update status')
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Anonymous reports</h1>
          <p className="mt-1 text-sm text-ink-500">
            Raise a concern without revealing your identity. Each report gets its own anonymous handle.
          </p>
        </div>
        <Button icon={<PenLine className="h-4 w-4" />} onClick={() => setIsComposerOpen(true)}>
          New report
        </Button>
      </div>

      <Alert variant="info">
        Your name is never shown on a report or its comments. If you reply to your own report, you'll still appear
        under its anonymous handle.
      </Alert>

      {error && <Alert variant="error">{error}</Alert>}

      {isComposerOpen && (
        <Composer
          onClose={() => setIsComposerOpen(false)}
          onCreated={(report) => {
            setIsComposerOpen(false)
            setReports((prev) => [report, ...prev])
          }}
        />
      )}

      {isLoading ? (
        <p className="text-sm text-ink-500">Loading…</p>
      ) : reports.length === 0 ? (
        <Card>
          <EmptyState
            title="No reports yet"
            description="If something needs raising, you can do it here without revealing who you are."
            icon={<ShieldAlert className="h-5 w-5" />}
          />
        </Card>
      ) : (
        <div className="flex flex-col gap-4">
          {reports.map((report) => (
            <ReportCard key={report.id} report={report} isAdmin={isAdmin} onStatusChange={handleStatusChange} />
          ))}
        </div>
      )}
    </div>
  )
}

function Composer({
  onClose,
  onCreated,
}: {
  onClose: () => void
  onCreated: (report: AnonymousReportResponse) => void
}) {
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [category, setCategory] = useState<ReportCategory>('OTHER')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      const report = await reportsApi.create({ title, body, category })
      onCreated(report)
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to submit report')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card>
      <CardHeader
        title="New anonymous report"
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
        <Select label="Category" required value={category} onChange={(e) => setCategory(e.target.value as ReportCategory)}>
          {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
        <Input label="Title" required value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Brief summary" />
        <Textarea
          label="Details"
          required
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Share as much detail as you're comfortable with…"
        />
        <div className="flex items-center gap-3">
          <Button type="submit" isLoading={isSubmitting}>
            Submit anonymously
          </Button>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </form>
    </Card>
  )
}

function ReportCard({
  report,
  isAdmin,
  onStatusChange,
}: {
  report: AnonymousReportResponse
  isAdmin: boolean
  onStatusChange: (id: number, status: ReportStatus) => void
}) {
  const [isExpanded, setIsExpanded] = useState(false)
  const [comments, setComments] = useState<ReportCommentResponse[]>([])
  const [isLoadingComments, setIsLoadingComments] = useState(false)
  const [commentBody, setCommentBody] = useState('')
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [commentCount, setCommentCount] = useState(report.commentCount)
  const [error, setError] = useState<string | null>(null)

  function toggleExpanded() {
    if (!isExpanded && comments.length === 0 && commentCount > 0) {
      setIsLoadingComments(true)
      reportsApi
        .getComments(report.id)
        .then(setComments)
        .catch(() => setError('Failed to load comments'))
        .finally(() => setIsLoadingComments(false))
    }
    setIsExpanded((v) => !v)
  }

  async function handleAddComment(e: FormEvent) {
    e.preventDefault()
    if (!commentBody.trim()) return
    setIsSubmittingComment(true)
    setError(null)
    try {
      const comment = await reportsApi.addComment(report.id, commentBody)
      setComments((prev) => [...prev, comment])
      setCommentCount((c) => c + 1)
      setCommentBody('')
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to post comment')
    } finally {
      setIsSubmittingComment(false)
    }
  }

  return (
    <Card>
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-ink-100 text-ink-500">
            <UserRound className="h-4.5 w-4.5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <p className="text-sm font-medium text-ink-900">{report.displayHandle}</p>
              {report.isMine && (
                <span className="rounded-full bg-accent-50 px-2 py-0.5 text-[11px] font-medium text-accent-700">
                  Your report
                </span>
              )}
            </div>
            <p className="text-xs text-ink-500">
              {CATEGORY_LABELS[report.category]} · {timeAgo(report.createdAt)}
            </p>
          </div>
        </div>

        {isAdmin ? (
          <select
            value={report.status}
            onChange={(e) => onStatusChange(report.id, e.target.value as ReportStatus)}
            className={`rounded-full border px-2.5 py-1 text-xs font-medium ${STATUS_STYLES[report.status]}`}
          >
            {Object.entries(STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        ) : (
          <span className={`rounded-full border px-2.5 py-1 text-xs font-medium ${STATUS_STYLES[report.status]}`}>
            {STATUS_LABELS[report.status]}
          </span>
        )}
      </div>

      <h3 className="mt-4 text-base font-semibold text-ink-900">{report.title}</h3>
      <p className="mt-1.5 whitespace-pre-line text-sm leading-relaxed text-ink-700">{report.body}</p>

      {error && (
        <div className="mt-3">
          <Alert variant="error">{error}</Alert>
        </div>
      )}

      <button
        onClick={toggleExpanded}
        className="mt-4 flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-900"
      >
        <MessageSquare className="h-4 w-4" />
        {commentCount === 0 ? 'Comment' : `${commentCount} comment${commentCount === 1 ? '' : 's'}`}
      </button>

      {isExpanded && (
        <div className="mt-4 border-t border-ink-100 pt-4">
          {isLoadingComments ? (
            <p className="text-sm text-ink-500">Loading comments…</p>
          ) : (
            <div className="flex flex-col gap-3">
              {comments.map((c) => (
                <div key={c.id} className="flex items-start gap-2.5">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-ink-100 text-ink-500">
                    <UserRound className="h-3.5 w-3.5" />
                  </div>
                  <div className="flex-1 rounded-lg bg-ink-50 px-3 py-2">
                    <div className="flex items-center gap-2">
                      <p className="text-xs font-medium text-ink-900">{c.displayName}</p>
                      {c.isReporter && (
                        <span className="rounded-full bg-accent-50 px-1.5 py-0.5 text-[10px] font-medium text-accent-700">
                          Reporter
                        </span>
                      )}
                      {c.isMine && <span className="text-[11px] text-ink-400">you</span>}
                      <span className="text-[11px] text-ink-400">· {timeAgo(c.createdAt)}</span>
                    </div>
                    <p className="mt-0.5 text-sm text-ink-700">{c.body}</p>
                  </div>
                </div>
              ))}
            </div>
          )}

          <form onSubmit={handleAddComment} className="mt-3 flex items-center gap-2">
            <input
              value={commentBody}
              onChange={(e) => setCommentBody(e.target.value)}
              placeholder="Write a comment…"
              className="flex-1 rounded-lg border border-ink-300 bg-white px-3.5 py-2 text-sm text-ink-900 placeholder:text-ink-400 focus:border-accent-600"
            />
            <Button type="submit" size="sm" isLoading={isSubmittingComment} icon={<Send className="h-3.5 w-3.5" />}>
              Send
            </Button>
          </form>
        </div>
      )}
    </Card>
  )
}
