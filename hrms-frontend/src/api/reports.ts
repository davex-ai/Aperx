import { api } from './client'
import type { AnonymousReportResponse, ReportCategory, ReportCommentResponse, ReportStatus } from '../types'

export const reportsApi = {
  getFeed: () => api.get<AnonymousReportResponse[]>('/reports'),

  create: (payload: { title: string; body: string; category: ReportCategory }) =>
    api.post<AnonymousReportResponse>('/reports', payload),

  updateStatus: (id: number, status: ReportStatus) =>
    api.put<AnonymousReportResponse>(`/reports/${id}/status`, { status }),

  getComments: (id: number) => api.get<ReportCommentResponse[]>(`/reports/${id}/comments`),

  addComment: (id: number, body: string) =>
    api.post<ReportCommentResponse>(`/reports/${id}/comments`, { body }),
}
