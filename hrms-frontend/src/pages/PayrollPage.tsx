import { useEffect, useState } from 'react'
import { Download, FileText, Wallet, Calculator, CheckCircle2, Clock3 } from 'lucide-react'
import { payrollApi, documentsApi } from '../api/payroll'
import type { EmployeeDocumentResponse, PayrollPreviewResponse, PayslipResponse } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Button } from '../components/Button'
import { Select } from '../components/Form'
import { Alert, EmptyState } from '../components/Alert'
import { HttpError, downloadFile } from '../api/client'
import { useAuth } from '../context/AuthContext'

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

const currency = (n: number) => n.toLocaleString(undefined, { style: 'currency', currency: 'USD' })

export function PayrollPage() {
  const { session } = useAuth()
  const isAdmin = session?.role === 'ROLE_ADMIN'

  const [payslips, setPayslips] = useState<PayslipResponse[]>([])
  const [documents, setDocuments] = useState<EmployeeDocumentResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [downloadingId, setDownloadingId] = useState<string | null>(null)

  function loadSelf() {
    Promise.all([payrollApi.myPayslips().then(setPayslips), documentsApi.myDocuments().then(setDocuments)])
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load payroll data'))
      .finally(() => setIsLoading(false))
  }

  useEffect(() => {
    loadSelf()
  }, [])

  async function handleDownloadPayslip(payslip: PayslipResponse) {
    setDownloadingId(`payslip-${payslip.id}`)
    try {
      await downloadFile(payslip.downloadUrl, `payslip-${payslip.periodYear}-${payslip.periodMonth}.pdf`)
    } catch {
      setError('Failed to download payslip')
    } finally {
      setDownloadingId(null)
    }
  }

  async function handleDownloadDocument(doc: EmployeeDocumentResponse) {
    setDownloadingId(`doc-${doc.id}`)
    try {
      await downloadFile(doc.downloadUrl, `${doc.title}.pdf`)
    } catch {
      setError('Failed to download document')
    } finally {
      setDownloadingId(null)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Payroll & documents</h1>
        <p className="mt-1 text-sm text-ink-500">
          {isAdmin ? 'Run payroll for the company, and download your own payslips below.' : 'Download your payslips, tax forms, and employment letters.'}
        </p>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      {isAdmin && <RunPayrollPanel onCompleted={loadSelf} />}

      <Card>
        <CardHeader title="Payslips" action={<Wallet className="h-5 w-5 text-ink-400" />} />
        {isLoading ? (
          <p className="text-sm text-ink-500">Loading…</p>
        ) : payslips.length === 0 ? (
          <EmptyState title="No payslips yet" description="Payslips appear here after payroll has been processed." />
        ) : (
          <div className="flex flex-col divide-y divide-ink-100">
            {payslips.map((p) => (
              <div key={p.id} className="flex items-center justify-between gap-4 py-3.5">
                <div>
                  <p className="text-sm font-medium text-ink-900">
                    {MONTH_NAMES[p.periodMonth - 1]} {p.periodYear}
                  </p>
                  <p className="text-xs text-ink-500">Net pay: {currency(p.netSalary)}</p>
                </div>
                <Button
                  variant="secondary"
                  size="sm"
                  isLoading={downloadingId === `payslip-${p.id}`}
                  icon={<Download className="h-3.5 w-3.5" />}
                  onClick={() => handleDownloadPayslip(p)}
                >
                  Download
                </Button>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card>
        <CardHeader title="Tax forms & letters" action={<FileText className="h-5 w-5 text-ink-400" />} />
        {isLoading ? (
          <p className="text-sm text-ink-500">Loading…</p>
        ) : documents.length === 0 ? (
          <EmptyState title="No documents yet" description="Tax forms and employment letters will appear here." />
        ) : (
          <div className="flex flex-col divide-y divide-ink-100">
            {documents.map((d) => (
              <div key={d.id} className="flex items-center justify-between gap-4 py-3.5">
                <div>
                  <p className="text-sm font-medium text-ink-900">{d.title}</p>
                  <p className="text-xs text-ink-500">{new Date(d.createdAt).toLocaleDateString()}</p>
                </div>
                <Button
                  variant="secondary"
                  size="sm"
                  isLoading={downloadingId === `doc-${d.id}`}
                  icon={<Download className="h-3.5 w-3.5" />}
                  onClick={() => handleDownloadDocument(d)}
                >
                  Download
                </Button>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}

function RunPayrollPanel({ onCompleted }: { onCompleted: () => void }) {
  const now = new Date()
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [year, setYear] = useState(now.getFullYear())
  const [preview, setPreview] = useState<PayrollPreviewResponse | null>(null)
  const [isPreviewing, setIsPreviewing] = useState(false)
  const [isRunning, setIsRunning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  async function handlePreview() {
    setError(null)
    setSuccessMessage(null)
    setIsPreviewing(true)
    try {
      const result = await payrollApi.previewPayroll(month, year)
      setPreview(result)
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to calculate payroll preview')
    } finally {
      setIsPreviewing(false)
    }
  }

  async function handleConfirm() {
    setError(null)
    setIsRunning(true)
    try {
      const result = await payrollApi.runPayroll(month, year)
      setSuccessMessage(
        `Payroll processed successfully for ${MONTH_NAMES[month - 1]} ${year} — ${result.length} payslip${result.length === 1 ? '' : 's'} generated.`,
      )
      setPreview(null)
      onCompleted()
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to run payroll')
    } finally {
      setIsRunning(false)
    }
  }

  return (
    <Card>
      <CardHeader
        title="Run payroll"
        subtitle="Review calculated pay before confirming. No real funds move — this records payroll in the system and generates payslips."
        action={<Calculator className="h-5 w-5 text-ink-400" />}
      />

      {error && (
        <div className="mb-4">
          <Alert variant="error">{error}</Alert>
        </div>
      )}
      {successMessage && (
        <div className="mb-4">
          <Alert variant="success">
            <span className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4" /> {successMessage}
            </span>
          </Alert>
        </div>
      )}

      <div className="flex flex-wrap items-end gap-3">
        <Select label="Month" value={month} onChange={(e) => setMonth(Number(e.target.value))}>
          {MONTH_NAMES.map((name, i) => (
            <option key={name} value={i + 1}>
              {name}
            </option>
          ))}
        </Select>
        <Select label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
          {[now.getFullYear() - 1, now.getFullYear(), now.getFullYear() + 1].map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </Select>
        <Button variant="secondary" isLoading={isPreviewing} icon={<Calculator className="h-4 w-4" />} onClick={handlePreview}>
          Calculate
        </Button>
      </div>

      {preview && (
        <div className="mt-6">
          {preview.alreadyProcessed && (
            <div className="mb-4">
              <Alert variant="error">Payroll for this period has already been processed and cannot be run again.</Alert>
            </div>
          )}

          {preview.employeeCount === 0 ? (
            <EmptyState title="No active employees to pay" description="Nothing to process for this period." />
          ) : (
            <>
              <div className="mb-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Employees</p>
                  <p className="mt-1 text-xl font-semibold text-ink-900">{preview.employeeCount}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Total gross</p>
                  <p className="mt-1 text-xl font-semibold text-ink-900">{currency(preview.totalGross)}</p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Total net</p>
                  <p className="mt-1 text-xl font-semibold text-ink-900">{currency(preview.totalNet)}</p>
                </div>
              </div>

              <div className="overflow-x-auto rounded-lg border border-ink-200">
                <table className="w-full text-left text-sm">
                  <thead className="bg-ink-50 text-xs font-medium uppercase tracking-wide text-ink-500">
                    <tr>
                      <th className="px-4 py-2.5">Employee</th>
                      <th className="px-4 py-2.5">Type</th>
                      <th className="px-4 py-2.5">Hours</th>
                      <th className="px-4 py-2.5">Gross</th>
                      <th className="px-4 py-2.5">Tax</th>
                      <th className="px-4 py-2.5">Social Security</th>
                      <th className="px-4 py-2.5">Net</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {preview.lines.map((line) => (
                      <tr key={line.employeeId}>
                        <td className="px-4 py-2.5 font-medium text-ink-900">{line.employeeName}</td>
                        <td className="px-4 py-2.5 text-ink-500">{line.payType === 'HOURLY' ? 'Hourly' : 'Salaried'}</td>
                        <td className="px-4 py-2.5 text-ink-500">
                          {line.payType === 'HOURLY' ? (
                            <span className="flex items-center gap-1">
                              <Clock3 className="h-3 w-3" /> {line.hoursWorked ?? 0}
                            </span>
                          ) : (
                            '—'
                          )}
                        </td>
                        <td className="px-4 py-2.5">{currency(line.grossSalary)}</td>
                        <td className="px-4 py-2.5 text-danger-600">-{currency(line.taxDeduction)}</td>
                        <td className="px-4 py-2.5 text-danger-600">-{currency(line.socialSecurityDeduction)}</td>
                        <td className="px-4 py-2.5 font-semibold text-ink-900">{currency(line.netSalary)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {!preview.alreadyProcessed && (
                <Button className="mt-4" isLoading={isRunning} icon={<CheckCircle2 className="h-4 w-4" />} onClick={handleConfirm}>
                  Confirm & run payroll
                </Button>
              )}
            </>
          )}
        </div>
      )}
    </Card>
  )
}
