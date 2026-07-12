import { api } from './client'
import type { EmployeeDocumentResponse, PayrollAdjustment, PayrollPreviewResponse, PayslipResponse } from '../types'

export const payrollApi = {
  previewPayroll: (periodMonth: number, periodYear: number, adjustments?: PayrollAdjustment[]) =>
    api.post<PayrollPreviewResponse>('/payroll/preview', { periodMonth, periodYear, adjustments }),

  runPayroll: (periodMonth: number, periodYear: number, adjustments?: PayrollAdjustment[]) =>
    api.post<PayslipResponse[]>('/payroll/run', { periodMonth, periodYear, adjustments }),

  myPayslips: () => api.get<PayslipResponse[]>('/payroll/payslips/me'),
}

export const documentsApi = {
  myDocuments: () => api.get<EmployeeDocumentResponse[]>('/documents/me'),
}
