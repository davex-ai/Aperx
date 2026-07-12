import { Check, PartyPopper, X } from 'lucide-react'
import type { ApplicationStatus } from '../types'

const STAGES: { key: ApplicationStatus; label: string }[] = [
  { key: 'APPLIED', label: 'Applied' },
  { key: 'SCREENING', label: 'Screening' },
  { key: 'INTERVIEW', label: 'Interview' },
  { key: 'OFFERED', label: 'Offered' },
]

export function ApplicationPipeline({ status }: { status: ApplicationStatus }) {
  if (status === 'REJECTED') {
    return (
      <div className="flex items-center gap-2 text-danger-700">
        <div className="flex h-6 w-6 items-center justify-center rounded-full bg-danger-100">
          <X className="h-3.5 w-3.5" />
        </div>
        <span className="text-sm font-medium">Not moving forward</span>
      </div>
    )
  }

  if (status === 'HIRED') {
    return (
      <div className="flex items-center gap-2 text-success-700">
        <div className="flex h-6 w-6 items-center justify-center rounded-full bg-success-100">
          <PartyPopper className="h-3.5 w-3.5" />
        </div>
        <span className="text-sm font-medium">Hired</span>
      </div>
    )
  }

  const currentIndex = STAGES.findIndex((s) => s.key === status)

  return (
    <div className="flex items-center">
      {STAGES.map((stage, index) => {
        const isComplete = index < currentIndex
        const isCurrent = index === currentIndex
        const isLast = index === STAGES.length - 1

        return (
          <div key={stage.key} className="flex items-center">
            <div className="flex flex-col items-center gap-1.5">
              <div
                className={`flex h-6 w-6 items-center justify-center rounded-full border-2 text-xs font-semibold transition-colors ${
                  isComplete
                    ? 'border-accent-600 bg-accent-600 text-white'
                    : isCurrent
                      ? 'border-accent-600 bg-white text-accent-600'
                      : 'border-ink-200 bg-white text-ink-300'
                }`}
              >
                {isComplete ? <Check className="h-3.5 w-3.5" /> : index + 1}
              </div>
              <span
                className={`text-[11px] font-medium whitespace-nowrap ${
                  isComplete || isCurrent ? 'text-ink-700' : 'text-ink-400'
                }`}
              >
                {stage.label}
              </span>
            </div>
            {!isLast && (
              <div
                className={`mb-4 h-0.5 w-8 sm:w-12 ${index < currentIndex ? 'bg-accent-600' : 'bg-ink-200'}`}
              />
            )}
          </div>
        )
      })}
    </div>
  )
}
