import { useEffect, useState, type FormEvent } from 'react'
import { Briefcase, Plus, X, Users, ChevronRight, FileText, GraduationCap, Clock3, Mail, Phone, Award } from 'lucide-react'
import { recruitmentApi, type ExtendOfferPayload } from '../api/recruitment'
import { fetchResumePreview, HttpError, type ResumePreview } from '../api/client'
import type { ApplicationResponse, ApplicationStatus, JobPostingResponse, JobQuestionRequest, JobStatus, PayType, UserRole } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Input, Select, Textarea } from '../components/Form'
import { Button } from '../components/Button'
import { Alert, EmptyState } from '../components/Alert'
import { StatusBadge } from '../components/StatusBadge'
import { ApplicationPipeline } from '../components/ApplicationPipeline'
import { useAuth } from '../context/AuthContext'
import { Link2 } from 'lucide-react'

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'ROLE_EMPLOYEE', label: 'Employee' },
  { value: 'ROLE_MANAGER', label: 'Manager' },
  { value: 'ROLE_ADMIN', label: 'Admin' },
]

const NEXT_STAGE: Record<ApplicationStatus, ApplicationStatus | null> = {
  APPLIED: 'SCREENING',
  SCREENING: 'INTERVIEW',
  INTERVIEW: 'OFFERED',
  OFFERED: null,
  HIRED: null,
  REJECTED: null,
}

const STAGE_LABEL: Record<ApplicationStatus, string> = {
  APPLIED: 'Move to screening',
  SCREENING: 'Move to interview',
  INTERVIEW: 'Mark as offered',
  OFFERED: 'Offered',
  HIRED: 'Hired',
  REJECTED: 'Rejected',
}

const EDUCATION_LABEL: Record<string, string> = {
  HIGH_SCHOOL: 'High school diploma',
  ASSOCIATE: "Associate's degree",
  BACHELORS: "Bachelor's degree",
  MASTERS: "Master's degree",
  DOCTORATE: 'Doctorate',
  OTHER: 'Other',
}

export function RecruitmentPage() {
  const { session } = useAuth()
  const isAdmin = session?.role === 'ROLE_ADMIN'

  const [jobs, setJobs] = useState<JobPostingResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [selectedJobId, setSelectedJobId] = useState<number | null>(null)
  const [applications, setApplications] = useState<ApplicationResponse[]>([])
  const [isLoadingApplications, setIsLoadingApplications] = useState(false)
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const [previewApplication, setPreviewApplication] = useState<ApplicationResponse | null>(null)
  const [extendingOfferFor, setExtendingOfferFor] = useState<ApplicationResponse | null>(null)
  const [copied, setCopied] = useState(false)

  function handleCopyLink() {
    const url = `${window.location.origin}/careers/${session?.companySlug}`
    navigator.clipboard.writeText(url)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  function loadJobs() {
    recruitmentApi
      .getAllJobs()
      .then(setJobs)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load job postings'))
      .finally(() => setIsLoading(false))
  }

  useEffect(() => {
    loadJobs()
  }, [])

  function openJob(jobId: number) {
    setSelectedJobId(jobId)
    setIsLoadingApplications(true)
    recruitmentApi
      .getApplicationsForJob(jobId)
      .then(setApplications)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load applications'))
      .finally(() => setIsLoadingApplications(false))
  }

  async function advanceStage(application: ApplicationResponse) {
    const next = NEXT_STAGE[application.status]
    if (!next) return
    setUpdatingId(application.id)
    try {
      const updated = await recruitmentApi.updateApplicationStatus(application.id, next)
      setApplications((prev) => prev.map((a) => (a.id === updated.id ? updated : a)))
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to update application')
    } finally {
      setUpdatingId(null)
    }
  }

  async function rejectApplication(application: ApplicationResponse) {
    setUpdatingId(application.id)
    try {
      const updated = await recruitmentApi.updateApplicationStatus(application.id, 'REJECTED')
      setApplications((prev) => prev.map((a) => (a.id === updated.id ? updated : a)))
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to update application')
    } finally {
      setUpdatingId(null)
    }
  }

  const selectedJob = jobs.find((j) => j.id === selectedJobId)

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Recruitment</h1>
          <p className="mt-1 text-sm text-ink-500">Post openings and move candidates through the pipeline.</p>
        </div>
        {isAdmin && (
          <Button icon={<Plus className="h-4 w-4" />} onClick={() => setIsFormOpen(true)}>
            New job posting
          </Button>
        )}
        <Button
          variant="secondary"
          icon={<Link2 className="h-4 w-4" />}
          onClick={handleCopyLink}
        >
          {copied ? 'Link copied!' : 'Copy public careers link'}
        </Button>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      {isFormOpen && (
        <JobPostingForm
          onClose={() => setIsFormOpen(false)}
          onCreated={() => {
            setIsFormOpen(false)
            loadJobs()
          }}
        />
      )}

      {previewApplication && (
        <ResumePreviewModal application={previewApplication} onClose={() => setPreviewApplication(null)} />
      )}

      {extendingOfferFor && (
        <ExtendOfferModal
          application={extendingOfferFor}
          onClose={() => setExtendingOfferFor(null)}
          onExtended={() => {
            setExtendingOfferFor(null)
            if (selectedJobId) openJob(selectedJobId)
          }}
        />
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
        <div className="lg:col-span-2">
          <Card padded={false}>
            <div className="p-6 pb-0">
              <CardHeader title={`${jobs.length} job postings`} action={<Briefcase className="h-5 w-5 text-ink-400" />} />
            </div>
            {isLoading ? (
              <p className="px-6 pb-6 text-sm text-ink-500">Loading…</p>
            ) : jobs.length === 0 ? (
              <div className="px-6 pb-6">
                <EmptyState title="No job postings" description="Create a posting to start receiving applications." />
              </div>
            ) : (
              <div className="flex flex-col divide-y divide-ink-100 pb-2">
                {jobs.map((job) => (
                  <button
                    key={job.id}
                    onClick={() => openJob(job.id)}
                    className={`flex items-center justify-between gap-3 px-6 py-3.5 text-left transition-colors hover:bg-ink-50 ${
                      selectedJobId === job.id ? 'bg-accent-50' : ''
                    }`}
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-ink-900">{job.title}</p>
                      <p className="text-xs text-ink-500">
                        {job.department} · {job.applicantCount} applicant{job.applicantCount === 1 ? '' : 's'}
                      </p>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <StatusBadge status={job.status} />
                      <ChevronRight className="h-4 w-4 text-ink-300" />
                    </div>
                  </button>
                ))}
              </div>
            )}
          </Card>
        </div>

        <div className="lg:col-span-3">
          <Card>
            {!selectedJob ? (
              <EmptyState
                title="Select a job posting"
                description="Choose a posting on the left to review its candidates."
                icon={<Users className="h-5 w-5" />}
              />
            ) : (
              <>
                <CardHeader
                  title={selectedJob.title}
                  subtitle={`${applications.length} candidate${applications.length === 1 ? '' : 's'}`}
                />
                {isLoadingApplications ? (
                  <p className="text-sm text-ink-500">Loading…</p>
                ) : applications.length === 0 ? (
                  <EmptyState title="No applications yet" description="Candidates who apply will show up here." />
                ) : (
                  <div className="flex flex-col divide-y divide-ink-100">
                    {applications.map((app) => (
                      <div key={app.id} className="flex flex-col gap-3 py-4">
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-ink-900">{app.candidateName}</p>
                            <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-ink-500">
                              <span className="flex items-center gap-1">
                                <Mail className="h-3 w-3" /> {app.candidateEmail}
                              </span>
                              <span className="flex items-center gap-1">
                                <Phone className="h-3 w-3" /> {app.candidatePhone}
                              </span>
                              <span className="flex items-center gap-1">
                                <Clock3 className="h-3 w-3" /> {app.availability}
                              </span>
                              <span className="flex items-center gap-1">
                                <GraduationCap className="h-3 w-3" /> {EDUCATION_LABEL[app.highestEducation]}
                              </span>
                              <span>{app.yearsOfExperience} yrs experience</span>
                            </div>
                            <button
                              onClick={() => setPreviewApplication(app)}
                              className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-accent-600 hover:text-accent-700"
                            >
                              <FileText className="h-3 w-3" /> View resume ({app.resumeFileName})
                            </button>
                          </div>
                          <ApplicationPipeline status={app.status} />
                        </div>

                        <div className="rounded-lg bg-ink-50 px-3 py-2 text-xs text-ink-700">
                          <span className="font-medium text-ink-900">Why they want to join: </span>
                          {app.whyJoin}
                        </div>

                        {app.answers.length > 0 && (
                          <div className="flex flex-col gap-1.5">
                            {app.answers.map((a) => (
                              <div key={a.questionId} className="text-xs">
                                <p className="font-medium text-ink-700">{a.questionText}</p>
                                <p className="mt-0.5 text-ink-600">{a.answerText}</p>
                              </div>
                            ))}
                          </div>
                        )}

                        {app.status !== 'REJECTED' && app.status !== 'OFFERED' && app.status !== 'HIRED' && (
                          <div className="flex shrink-0 gap-2">
                            <Button
                              variant="secondary"
                              size="sm"
                              isLoading={updatingId === app.id}
                              onClick={() => rejectApplication(app)}
                            >
                              Reject
                            </Button>
                            <Button size="sm" isLoading={updatingId === app.id} onClick={() => advanceStage(app)}>
                              {STAGE_LABEL[app.status]}
                            </Button>
                          </div>
                        )}

                        {app.status === 'OFFERED' && (
                          <div className="flex shrink-0 gap-2">
                            <Button
                              variant="secondary"
                              size="sm"
                              isLoading={updatingId === app.id}
                              onClick={() => rejectApplication(app)}
                            >
                              Withdraw offer
                            </Button>
                            <Button size="sm" icon={<Award className="h-3.5 w-3.5" />} onClick={() => setExtendingOfferFor(app)}>
                              Extend offer
                            </Button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}

function ResumePreviewModal({ application, onClose }: { application: ApplicationResponse; onClose: () => void }) {
  const [preview, setPreview] = useState<ResumePreview | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchResumePreview(application.resumePreviewUrl)
      .then(setPreview)
      .catch(() => setError('Failed to load resume preview'))
  }, [application.resumePreviewUrl])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/50 p-6">
      <div className="flex max-h-[85vh] w-full max-w-3xl flex-col rounded-[var(--radius-card)] bg-white shadow-lg">
        <div className="flex items-center justify-between border-b border-ink-100 px-6 py-4">
          <div>
            <p className="text-sm font-semibold text-ink-900">{application.candidateName}'s resume</p>
            <p className="text-xs text-ink-500">{application.resumeFileName}</p>
          </div>
          <button onClick={onClose} className="text-ink-400 hover:text-ink-600">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-6">
          {error && <Alert variant="error">{error}</Alert>}
          {!preview && !error && <p className="text-sm text-ink-500">Loading preview…</p>}
          {preview?.kind === 'pdf' && preview.pdfUrl && (
            <iframe title="Resume preview" src={preview.pdfUrl} className="h-[65vh] w-full rounded-lg border border-ink-200" />
          )}
          {preview?.kind === 'html' && preview.html && (
            <div className="rounded-lg border border-ink-200 p-6" dangerouslySetInnerHTML={{ __html: preview.html }} />
          )}
        </div>
      </div>
    </div>
  )
}

function JobPostingForm({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [title, setTitle] = useState('')
  const [department, setDepartment] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState<JobStatus>('OPEN')
  const [questions, setQuestions] = useState<JobQuestionRequest[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function addQuestion() {
    setQuestions((prev) => [...prev, { questionText: '', isRequired: true }])
  }

  function updateQuestion(index: number, text: string) {
    setQuestions((prev) => prev.map((q, i) => (i === index ? { ...q, questionText: text } : q)))
  }

  function removeQuestion(index: number) {
    setQuestions((prev) => prev.filter((_, i) => i !== index))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await recruitmentApi.createJob({
        title,
        department,
        description,
        status,
        questions: questions.filter((q) => q.questionText.trim()),
      })
      onCreated()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to create job posting')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card>
      <CardHeader
        title="New job posting"
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
          <Input label="Job title" required value={title} onChange={(e) => setTitle(e.target.value)} />
          <Input label="Department" required value={department} onChange={(e) => setDepartment(e.target.value)} />
        </div>
        <Textarea
          label="Description"
          required
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Responsibilities, requirements, and what makes this role great"
        />
        <Select label="Status" value={status} onChange={(e) => setStatus(e.target.value as JobStatus)}>
          <option value="OPEN">Open</option>
          <option value="CLOSED">Closed</option>
          <option value="ARCHIVED">Archived</option>
        </Select>

        <div className="border-t border-ink-100 pt-4">
          <div className="mb-3 flex items-center justify-between">
            <p className="text-sm font-medium text-ink-700">Screening questions</p>
            <Button type="button" variant="secondary" size="sm" onClick={addQuestion}>
              Add question
            </Button>
          </div>
          <p className="mb-3 text-xs text-ink-500">
            Full name, email, phone, motivation, availability, experience, and education are always collected automatically.
          </p>
          <div className="flex flex-col gap-2">
            {questions.map((q, i) => (
              <div key={i} className="flex items-center gap-2">
                <input
                  value={q.questionText}
                  onChange={(e) => updateQuestion(i, e.target.value)}
                  placeholder="e.g. Describe a challenging project you led"
                  className="flex-1 rounded-lg border border-ink-300 bg-white px-3.5 py-2 text-sm text-ink-900 placeholder:text-ink-400 focus:border-accent-600"
                />
                <button type="button" onClick={() => removeQuestion(i)} className="text-ink-400 hover:text-danger-600">
                  <X className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Button type="submit" isLoading={isSubmitting}>
            Publish posting
          </Button>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </form>
    </Card>
  )
}

function ExtendOfferModal({
  application,
  onClose,
  onExtended,
}: {
  application: ApplicationResponse
  onClose: () => void
  onExtended: () => void
}) {
  const [form, setForm] = useState<ExtendOfferPayload>({
    department: '',
    jobTitle: '',
    hireDate: '',
    salaryAmount: 0,
    payType: 'SALARIED',
    role: 'ROLE_EMPLOYEE',
  })
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  function update<K extends keyof ExtendOfferPayload>(key: K, value: ExtendOfferPayload[K]) {
    setForm((f) => ({ ...f, [key]: value }))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await recruitmentApi.extendOffer(application.id, form)
      onExtended()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to extend offer')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/50 p-6">
      <div className="w-full max-w-lg rounded-[var(--radius-card)] bg-white p-6 shadow-lg">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <p className="text-sm font-semibold text-ink-900">Extend an offer to {application.candidateName}</p>
            <p className="text-xs text-ink-500">This creates their employee profile. You'll assign a company email afterward.</p>
          </div>
          <button onClick={onClose} className="text-ink-400 hover:text-ink-600">
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && (
          <div className="mb-4">
            <Alert variant="error">{error}</Alert>
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
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
            />
            <Select
              label="Pay type"
              required
              value={form.payType}
              onChange={(e) => update('payType', e.target.value as PayType)}
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
            <Input label="Start date" type="date" required value={form.hireDate} onChange={(e) => update('hireDate', e.target.value)} />
            <Select label="Role" required value={form.role} onChange={(e) => update('role', e.target.value as UserRole)}>
              {ROLE_OPTIONS.map((r) => (
                <option key={r.value} value={r.value}>
                  {r.label}
                </option>
              ))}
            </Select>
          </div>
          <div className="flex items-center gap-3">
            <Button type="submit" isLoading={isSubmitting} icon={<Award className="h-4 w-4" />}>
              Extend offer & create profile
            </Button>
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
