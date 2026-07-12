import { api, postFormData } from './client'
import type {
  ApplicationResponse,
  ApplicationStatus,
  EducationLevel,
  EmployeeResponse,
  JobPostingResponse,
  JobQuestionRequest,
  JobStatus,
  PayType,
  UserRole,
} from '../types'

export interface ExtendOfferPayload {
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

export const recruitmentApi = {
  getAllJobs: () => api.get<JobPostingResponse[]>('/jobs'),

  createJob: (payload: {
    title: string
    description: string
    department: string
    status?: JobStatus
    questions?: JobQuestionRequest[]
  }) => api.post<JobPostingResponse>('/jobs', payload),

  updateJob: (
    id: number,
    payload: {
      title: string
      description: string
      department: string
      status?: JobStatus
      questions?: JobQuestionRequest[]
    },
  ) => api.put<JobPostingResponse>(`/jobs/${id}`, payload),

  getApplicationsForJob: (jobId: number) =>
    api.get<ApplicationResponse[]>(`/jobs/${jobId}/applications`),

  updateApplicationStatus: (applicationId: number, status: ApplicationStatus) =>
    api.put<ApplicationResponse>(`/jobs/applications/${applicationId}/status`, { status }),

  extendOffer: (applicationId: number, payload: ExtendOfferPayload) =>
    api.post<EmployeeResponse>(`/jobs/applications/${applicationId}/extend-offer`, payload),
}

export const careersApi = {
  getOpenJobs: (companySlug: string) => api.get<JobPostingResponse[]>(`/careers/${companySlug}/jobs`),

  getOpenJob: (companySlug: string, jobId: number) =>
    api.get<JobPostingResponse>(`/careers/${companySlug}/jobs/${jobId}`),

  apply: (
    companySlug: string,
    jobId: number,
    payload: {
      candidateName: string
      candidateEmail: string
      candidatePhone: string
      whyJoin: string
      availability: string
      yearsOfExperience: number
      highestEducation: EducationLevel
      answers: Record<number, string>
      resume: File
    },
  ) => {
    const formData = new FormData()
    formData.append('candidateName', payload.candidateName)
    formData.append('candidateEmail', payload.candidateEmail)
    formData.append('candidatePhone', payload.candidatePhone)
    formData.append('whyJoin', payload.whyJoin)
    formData.append('availability', payload.availability)
    formData.append('yearsOfExperience', String(payload.yearsOfExperience))
    formData.append('highestEducation', payload.highestEducation)
    formData.append('answersJson', JSON.stringify(payload.answers))
    formData.append('resume', payload.resume)

    return postFormData<ApplicationResponse>(`/careers/${companySlug}/jobs/${jobId}/apply`, formData)
  },
}
