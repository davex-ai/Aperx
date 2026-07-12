import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

interface FieldWrapperProps {
  label?: string
  error?: string
  hint?: string
  children: ReactNode
  required?: boolean
}

function FieldWrapper({ label, error, hint, children, required }: FieldWrapperProps) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label className="text-sm font-medium text-ink-700">
          {label}
          {required && <span className="ml-0.5 text-danger-600">*</span>}
        </label>
      )}
      {children}
      {error && <p className="text-xs font-medium text-danger-600">{error}</p>}
      {!error && hint && <p className="text-xs text-ink-500">{hint}</p>}
    </div>
  )
}

const baseInputClasses =
  'w-full rounded-lg border border-ink-300 bg-white px-3.5 py-2.5 text-sm text-ink-900 placeholder:text-ink-400 focus:border-accent-600 disabled:bg-ink-100 disabled:text-ink-500'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
}

export function Input({ label, error, hint, required, className = '', ...rest }: InputProps) {
  return (
    <FieldWrapper label={label} error={error} hint={hint} required={required}>
      <input
        className={`${baseInputClasses} ${error ? 'border-danger-400' : ''} ${className}`}
        required={required}
        {...rest}
      />
    </FieldWrapper>
  )
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  hint?: string
  children: ReactNode
}

export function Select({ label, error, hint, required, children, className = '', ...rest }: SelectProps) {
  return (
    <FieldWrapper label={label} error={error} hint={hint} required={required}>
      <select
        className={`${baseInputClasses} ${error ? 'border-danger-400' : ''} ${className}`}
        required={required}
        {...rest}
      >
        {children}
      </select>
    </FieldWrapper>
  )
}

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
  hint?: string
}

export function Textarea({ label, error, hint, required, className = '', ...rest }: TextareaProps) {
  return (
    <FieldWrapper label={label} error={error} hint={hint} required={required}>
      <textarea
        className={`${baseInputClasses} min-h-[90px] resize-y ${error ? 'border-danger-400' : ''} ${className}`}
        required={required}
        {...rest}
      />
    </FieldWrapper>
  )
}
