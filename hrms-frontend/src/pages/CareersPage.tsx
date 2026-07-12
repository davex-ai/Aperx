import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { Building2, MapPin, ArrowLeft, CheckCircle2, Upload, FileText } from 'lucide-react'
import { careersApi } from '../api/recruitment'
import type { EducationLevel, JobPostingResponse } from '../types'
import { Card } from '../components/Card'
import { Input, Select, Textarea } from '../components/Form'
import { Button } from '../components/Button'
import { Alert, EmptyState } from '../components/Alert'
import { HttpError } from '../api/client'

const EDUCATION_OPTIONS: { value: EducationLevel; label: string }[] = [
  { value: 'HIGH_SCHOOL', label: 'High school diploma' },
  { value: 'ASSOCIATE', label: "Associate's degree" },
  { value: 'BACHELORS', label: "Bachelor's degree" },
  { value: 'MASTERS', label: "Master's degree" },
  { value: 'DOCTORATE', label: 'Doctorate' },
  { value: 'OTHER', label: 'Other' },
]

export function CareersPage() {
  const { companySlug = '' } = useParams()
  const [jobs, setJobs] = useState<JobPostingResponse[]>([])
  const [selectedJob, setSelectedJob] = useState<JobPostingResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    careersApi
      .getOpenJobs(companySlug)
      .then(setJobs)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load open roles'))
      .finally(() => setIsLoading(false))
  }, [companySlug])

  return (
    <div className="min-h-screen bg-ink-50">
      <header className="border-b border-ink-200 bg-white">
        <div className="mx-auto flex max-w-4xl items-center gap-2.5 px-6 py-5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-ink-900">
            <Building2 className="h-4.5 w-4.5 text-white" />
          </div>
          <span className="text-[15px] font-semibold tracking-tight text-ink-900">AperX — Careers</span>
        </div>
      </header>

      <div className="mx-auto max-w-4xl px-6 py-12">
        {selectedJob ? (
          <ApplicationView companySlug={companySlug} job={selectedJob} onBack={() => setSelectedJob(null)} />
        ) : (
          <>
            <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Open roles</h1>
            <p className="mt-1.5 text-sm text-ink-500">Find your next role with us.</p>

            {error && (
              <div className="mt-6">
                <Alert variant="error">{error}</Alert>
              </div>
            )}

            <div className="mt-8 flex flex-col gap-4">
              {isLoading ? (
                <p className="text-sm text-ink-500">Loading…</p>
              ) : jobs.length === 0 ? (
                <EmptyState title="No open roles right now" description="Check back soon for new opportunities." />
              ) : (
                jobs.map((job) => (
                  <Card key={job.id} className="transition-shadow hover:shadow-md">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <h2 className="text-base font-semibold text-ink-900">{job.title}</h2>
                        <p className="mt-1 flex items-center gap-1.5 text-sm text-ink-500">
                          <MapPin className="h-3.5 w-3.5" /> {job.department}
                        </p>
                        <p className="mt-3 line-clamp-2 text-sm text-ink-600">{job.description}</p>
                      </div>
                      <Button size="sm" onClick={() => setSelectedJob(job)} className="shrink-0">
                        Apply
                      </Button>
                    </div>
                  </Card>
                ))
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function ApplicationView({
  companySlug,
  job,
  onBack,
}: {
  companySlug: string
  job: JobPostingResponse
  onBack: () => void
}) {
  const [candidateName, setCandidateName] = useState('')
  const [candidateEmail, setCandidateEmail] = useState('')
  const [candidatePhone, setCandidatePhone] = useState('')
  const [whyJoin, setWhyJoin] = useState('')
  const [availability, setAvailability] = useState('')
  const [yearsOfExperience, setYearsOfExperience] = useState('')
  const [highestEducation, setHighestEducation] = useState<EducationLevel>('BACHELORS')
  const [resume, setResume] = useState<File | null>(null)
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSubmitted, setIsSubmitted] = useState(false)

  function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    const validTypes = [
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    ]
    if (!validTypes.includes(file.type)) {
      setError('Resume must be a PDF or Word (.docx) document')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      setError('Resume file must be under 10MB')
      return
    }
    setError(null)
    setResume(file)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    if (!resume) {
      setError('Please attach your resume')
      return
    }
    for (const q of job.questions) {
      if (q.isRequired && !answers[q.id]?.trim()) {
        setError(`Please answer: ${q.questionText}`)
        return
      }
    }

    setIsSubmitting(true)
    try {
      await careersApi.apply(companySlug, job.id, {
        candidateName,
        candidateEmail,
        candidatePhone,
        whyJoin,
        availability,
        yearsOfExperience: Number(yearsOfExperience),
        highestEducation,
        answers,
        resume,
      })
      setIsSubmitted(true)
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to submit application')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isSubmitted) {
    return (
      <Card className="mx-auto max-w-lg text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-success-50 text-success-700">
          <CheckCircle2 className="h-6 w-6" />
        </div>
        <h2 className="text-lg font-semibold text-ink-900">Application submitted</h2>
        <p className="mt-1.5 text-sm text-ink-500">
          Thanks for applying to {job.title}. We'll be in touch if there's a match.
        </p>
        <Button variant="secondary" className="mt-6" onClick={onBack} icon={<ArrowLeft className="h-4 w-4" />}>
          Back to open roles
        </Button>
      </Card>
    )
  }

  return (
    <div>
      <button
        onClick={onBack}
        className="mb-6 flex items-center gap-1.5 text-sm font-medium text-ink-600 hover:text-ink-900"
      >
        <ArrowLeft className="h-4 w-4" /> Back to open roles
      </button>

      <h1 className="text-2xl font-semibold tracking-tight text-ink-900">{job.title}</h1>
      <p className="mt-1 flex items-center gap-1.5 text-sm text-ink-500">
        <MapPin className="h-3.5 w-3.5" /> {job.department}
      </p>
      <p className="mt-4 whitespace-pre-line text-sm leading-relaxed text-ink-700">{job.description}</p>

      <Card className="mt-8">
        <h2 className="mb-4 text-base font-semibold text-ink-900">Apply for this role</h2>
        {error && (
          <div className="mb-4">
            <Alert variant="error">{error}</Alert>
          </div>
        )}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input label="Full name" required value={candidateName} onChange={(e) => setCandidateName(e.target.value)} />
            <Input
              label="Email"
              type="email"
              required
              value={candidateEmail}
              onChange={(e) => setCandidateEmail(e.target.value)}
            />
            <Input
              label="Phone number"
              required
              value={candidatePhone}
              onChange={(e) => setCandidatePhone(e.target.value)}
            />
            <Input
              label="How soon can you start?"
              required
              value={availability}
              onChange={(e) => setAvailability(e.target.value)}
              placeholder="e.g. Immediately, 2 weeks notice"
            />
            <Input
              label="Years of experience"
              type="number"
              required
              min={0}
              step="0.5"
              value={yearsOfExperience}
              onChange={(e) => setYearsOfExperience(e.target.value)}
            />
            <Select
              label="Highest level of education"
              required
              value={highestEducation}
              onChange={(e) => setHighestEducation(e.target.value as EducationLevel)}
            >
              {EDUCATION_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </Select>
          </div>

          <Textarea
            label="Why do you want to work with us?"
            required
            value={whyJoin}
            onChange={(e) => setWhyJoin(e.target.value)}
          />

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-ink-700">
              Resume <span className="ml-0.5 text-danger-600">*</span>
            </label>
            <label className="flex cursor-pointer items-center justify-center gap-2 rounded-lg border border-dashed border-ink-300 bg-ink-50 px-4 py-6 text-sm text-ink-500 hover:border-accent-500 hover:text-accent-600">
              {resume ? (
                <span className="flex items-center gap-2 text-ink-700">
                  <FileText className="h-4 w-4" /> {resume.name}
                </span>
              ) : (
                <span className="flex items-center gap-2">
                  <Upload className="h-4 w-4" /> Upload PDF or Word document (max 10MB)
                </span>
              )}
              <input type="file" accept=".pdf,.docx" className="hidden" onChange={handleFileChange} />
            </label>
          </div>

          {job.questions.length > 0 && (
            <div className="flex flex-col gap-4 border-t border-ink-100 pt-4">
              <p className="text-sm font-medium text-ink-700">Additional questions</p>
              {job.questions.map((q) => (
                <Textarea
                  key={q.id}
                  label={q.questionText}
                  required={q.isRequired}
                  value={answers[q.id] ?? ''}
                  onChange={(e) => setAnswers((prev) => ({ ...prev, [q.id]: e.target.value }))}
                />
              ))}
            </div>
          )}

          <Button type="submit" isLoading={isSubmitting}>
            Submit application
          </Button>
        </form>
      </Card>
    </div>
  )
}
