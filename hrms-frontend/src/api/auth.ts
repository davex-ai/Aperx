import { api } from './client'
import type { AuthResponse } from '../types'

export const authApi = {
  registerCompany: (payload: {
    companyName: string
    adminFirstName: string
    adminLastName: string
    adminEmail: string
    adminPassword: string
  }) => api.post<AuthResponse>('/auth/register-company', payload),

  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }),

  completeSignup: (token: string, newPassword: string) =>
    api.post<void>('/auth/complete-signup', { token, newPassword }),

  completeFirstLoginProfile: (payload: {
    phoneNumber: string
    bankName?: string
    accountHolderName?: string
    accountNumber?: string
    routingNumber?: string
    emergencyContactName?: string
    emergencyRelationship?: string
    emergencyPhoneNumber?: string
  }) => api.post<void>('/auth/first-login-profile', payload),
}
