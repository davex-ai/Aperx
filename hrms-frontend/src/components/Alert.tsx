import type { ReactNode } from 'react'
import { AlertTriangle, CheckCircle2, Info, Inbox } from 'lucide-react'

type AlertVariant = 'error' | 'success' | 'info'

const alertStyles: Record<AlertVariant, { wrapper: string; icon: ReactNode }> = {
  error: {
    wrapper: 'bg-danger-50 border-danger-100 text-danger-700',
    icon: <AlertTriangle className="h-4 w-4 shrink-0" />,
  },
  success: {
    wrapper: 'bg-success-50 border-success-100 text-success-700',
    icon: <CheckCircle2 className="h-4 w-4 shrink-0" />,
  },
  info: {
    wrapper: 'bg-accent-50 border-accent-100 text-accent-700',
    icon: <Info className="h-4 w-4 shrink-0" />,
  },
}

export function Alert({ variant, children }: { variant: AlertVariant; children: ReactNode }) {
  const style = alertStyles[variant]
  return (
    <div className={`flex items-start gap-2.5 rounded-lg border px-4 py-3 text-sm ${style.wrapper}`}>
      {style.icon}
      <div>{children}</div>
    </div>
  )
}

export function EmptyState({
  title,
  description,
  icon,
}: {
  title: string
  description?: string
  icon?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-ink-100 text-ink-400">
        {icon ?? <Inbox className="h-5 w-5" />}
      </div>
      <div>
        <p className="text-sm font-medium text-ink-700">{title}</p>
        {description && <p className="mt-1 text-sm text-ink-500">{description}</p>}
      </div>
    </div>
  )
}
