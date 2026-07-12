import { api } from './client'
import type { EmployeeResponse, PayType, UserRole } from '../types'

export interface CreateEmployeePayload {
  personalEmail: string
  firstName: string
  lastName: string
  phoneNumber?: string
  department: string
  jobTitle: string
  salaryGrade?: string
  hireDate: string
  salaryAmount: number
  payType: PayType
  hourlyRate?: number
  managerId?: number
  role: UserRole
}

export const employeeApi = {
  getMyProfile: () => api.get<EmployeeResponse>('/profile/me'),

  updateMyProfile: (payload: {
    phoneNumber?: string
    bankName?: string
    accountHolderName?: string
    accountNumber?: string
    routingNumber?: string
  }) => api.put<EmployeeResponse>('/profile/me', payload),

  addEmergencyContact: (payload: {
    contactName: string
    relationship?: string
    phoneNumber: string
    email?: string
  }) => api.post<void>('/profile/me/emergency-contacts', payload),

  getEmployee: (id: number) => api.get<EmployeeResponse>(`/employees/${id}`),

  getMyTeam: () => api.get<EmployeeResponse[]>('/manager/team'),

  getAllEmployees: () => api.get<EmployeeResponse[]>('/admin/employees'),

  createEmployee: (payload: CreateEmployeePayload) =>
    api.post<EmployeeResponse>('/admin/employees', payload),

  assignCompanyEmail: (employeeId: number, companyEmail: string) =>
    api.put<EmployeeResponse>(`/admin/employees/${employeeId}/assign-company-email`, { companyEmail }),

  resendInvitation: (employeeId: number) =>
    api.post<boolean>(`/admin/employees/${employeeId}/resend-invitation`),

  terminateEmployee: (employeeId: number) =>
    api.put<void>(`/admin/employees/${employeeId}/terminate`),
}
