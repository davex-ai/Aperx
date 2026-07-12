import { api } from './client'
import type { TimeEntryResponse, TimesheetStatus, WeeklyTimesheetResponse } from '../types'

export const timeTrackingApi = {
  clockIn: (payload: { latitude?: number; longitude?: number; notes?: string }) =>
    api.post<TimeEntryResponse>('/time-tracking/clock-in', payload),

  clockOut: (payload: { latitude?: number; longitude?: number; notes?: string }) =>
    api.post<TimeEntryResponse>('/time-tracking/clock-out', payload),

  getActive: () => api.get<TimeEntryResponse | null>('/time-tracking/active'),

  getMyEntries: () => api.get<TimeEntryResponse[]>('/time-tracking/entries/me'),

  submitCurrentWeek: () => api.post<WeeklyTimesheetResponse>('/time-tracking/timesheets/submit-current-week'),

  getMyTimesheets: () => api.get<WeeklyTimesheetResponse[]>('/time-tracking/timesheets/me'),

  getPendingTimesheets: () => api.get<WeeklyTimesheetResponse[]>('/time-tracking/timesheets/pending'),

  reviewTimesheet: (id: number, status: TimesheetStatus, reviewComment?: string) =>
    api.put<WeeklyTimesheetResponse>(`/time-tracking/timesheets/${id}/review`, { status, reviewComment }),
}
