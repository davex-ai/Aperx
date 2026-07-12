import { api } from './client'
import type { DashboardStatsResponse } from '../types'

export const dashboardApi = {
  getStats: () => api.get<DashboardStatsResponse>('/dashboard/stats'),
}
