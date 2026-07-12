import { api } from './client'
import type { LeaveBalanceResponse, LeaveRequestResponse, LeaveStatus, LeaveType } from '../types'

export const leaveApi = {
  submit: (payload: {
    type: LeaveType
    startDate: string
    endDate: string
    reason?: string
    certificateUrl?: string
  }) => api.post<LeaveRequestResponse>('/leave-requests', payload),

  myRequests: () => api.get<LeaveRequestResponse[]>('/leave-requests/me'),

  myBalances: () => api.get<LeaveBalanceResponse[]>('/leave-requests/balances/me'),

  pendingRequests: () => api.get<LeaveRequestResponse[]>('/leave-requests/pending'),

  review: (id: number, status: LeaveStatus, reviewComment?: string) =>
    api.put<LeaveRequestResponse>(`/leave-requests/${id}/review`, { status, reviewComment }),
}
