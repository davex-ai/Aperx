import type { ApiError } from '../types'

const API_BASE = '/api'

export class HttpError extends Error {
  status: number
  fieldErrors: Record<string, string> | null

  constructor(apiError: ApiError) {
    super(apiError.message)
    this.status = apiError.status
    this.fieldErrors = apiError.fieldErrors
  }
}

function getToken(): string | null {
  return localStorage.getItem('hrms_token')
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem('hrms_token', token)
  else localStorage.removeItem('hrms_token')
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''

  if (!response.ok) {
    if (contentType.includes('application/json')) {
      const errorBody = (await response.json()) as ApiError
      throw new HttpError(errorBody)
    }
    throw new HttpError({
      timestamp: new Date().toISOString(),
      status: response.status,
      message: `Request failed with status ${response.status}`,
      path,
      fieldErrors: null,
    })
  }

  if (contentType.includes('application/json')) {
    return (await response.json()) as T
  }

  return undefined as T
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  del: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}

export async function postFormData<T>(path: string, formData: FormData): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers,
    body: formData,
  })

  const contentType = response.headers.get('content-type') ?? ''

  if (!response.ok) {
    if (contentType.includes('application/json')) {
      const errorBody = (await response.json()) as ApiError
      throw new HttpError(errorBody)
    }
    throw new HttpError({
      timestamp: new Date().toISOString(),
      status: response.status,
      message: `Request failed with status ${response.status}`,
      path,
      fieldErrors: null,
    })
  }

  return (await response.json()) as T
}

export interface ResumePreview {
  kind: 'pdf' | 'html'
  pdfUrl?: string
  html?: string
}

export async function fetchResumePreview(path: string): Promise<ResumePreview> {
  const token = getToken()
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch(`${API_BASE}${path}`, { headers })
  if (!response.ok) {
    throw new Error('Failed to load resume preview')
  }

  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/pdf')) {
    const blob = await response.blob()
    return { kind: 'pdf', pdfUrl: window.URL.createObjectURL(blob) }
  }
  const html = await response.text()
  return { kind: 'html', html }
}

export async function downloadFile(path: string, suggestedFileName: string) {
  const token = getToken()
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch(`${API_BASE}${path}`, { headers })
  if (!response.ok) {
    throw new Error('Download failed')
  }
  const blob = await response.blob()
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = suggestedFileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}
