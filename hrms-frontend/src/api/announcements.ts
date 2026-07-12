import { api } from './client'
import type { AnnouncementResponse, CommentResponse } from '../types'

export const announcementsApi = {
  getFeed: () => api.get<AnnouncementResponse[]>('/announcements'),

  create: (payload: { title: string; body: string; isPinned?: boolean }) =>
    api.post<AnnouncementResponse>('/announcements', payload),

  update: (id: number, payload: { title: string; body: string; isPinned?: boolean }) =>
    api.put<AnnouncementResponse>(`/announcements/${id}`, payload),

  remove: (id: number) => api.del<void>(`/announcements/${id}`),

  getComments: (id: number) => api.get<CommentResponse[]>(`/announcements/${id}/comments`),

  addComment: (id: number, body: string) =>
    api.post<CommentResponse>(`/announcements/${id}/comments`, { body }),

  removeComment: (id: number, commentId: number) =>
    api.del<void>(`/announcements/${id}/comments/${commentId}`),
}
