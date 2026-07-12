export type UserRole = 'ROLE_ADMIN' | 'ROLE_MANAGER' | 'ROLE_EMPLOYEE'

export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type LeaveType = 'ANNUAL' | 'SICK' | 'MATERNITY' | 'CASUAL'
export type JobStatus = 'OPEN' | 'CLOSED' | 'ARCHIVED'
export type ApplicationStatus = 'APPLIED' | 'SCREENING' | 'INTERVIEW' | 'OFFERED' | 'HIRED' | 'REJECTED'
export type DocumentType = 'PAYSLIP' | 'TAX_FORM' | 'EMPLOYMENT_LETTER' | 'RESUME' | 'MEDICAL_CERTIFICATE'

export interface AuthResponse {
  token: string
  email: string
  role: UserRole
  companySlug: string | null
  mustCompleteOnboarding: boolean
  employeeId: number | null
  fullName: string | null
  companyId: number | null
  companyName: string | null
}

export type PayType = 'HOURLY' | 'SALARIED'
export type TimesheetStatus = 'OPEN' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export interface EmployeeResponse {
  id: number
  personalEmail: string
  companyEmail: string | null
  firstName: string
  lastName: string
  phoneNumber: string | null
  department: string | null
  jobTitle: string | null
  salaryGrade: string | null
  hireDate: string
  salaryAmount: number | null
  payType: PayType
  hourlyRate: number | null
  role: UserRole
  managerId: number | null
  managerName: string | null
  active: boolean
  invitationSent: boolean | null
  onboardingStage: 'DRAFT' | 'INVITED' | 'ACTIVE'
}

export interface TimeEntryResponse {
  id: number
  clockInAt: string
  clockInLat: number | null
  clockInLng: number | null
  clockOutAt: string | null
  clockOutLat: number | null
  clockOutLng: number | null
  durationHours: number
  notes: string | null
  isActive: boolean
}

export interface WeeklyTimesheetResponse {
  id: number
  employeeId: number
  employeeName: string
  weekStartDate: string
  weekEndDate: string
  totalHours: number
  status: TimesheetStatus
  submittedAt: string | null
  reviewedByName: string | null
  reviewComment: string | null
  entries: TimeEntryResponse[]
}

export interface LeaveRequestResponse {
  id: number
  employeeId: number
  employeeName: string
  type: LeaveType
  startDate: string
  endDate: string
  reason: string | null
  certificateUrl: string | null
  status: LeaveStatus
  reviewComment: string | null
  approvedByName: string | null
  createdAt: string
}

export interface LeaveBalanceResponse {
  type: LeaveType
  totalDays: number
  usedDays: number
  remainingDays: number
}

export type EducationLevel = 'HIGH_SCHOOL' | 'ASSOCIATE' | 'BACHELORS' | 'MASTERS' | 'DOCTORATE' | 'OTHER'

export interface JobQuestionRequest {
  questionText: string
  isRequired?: boolean
}

export interface JobQuestionResponse {
  id: number
  questionText: string
  isRequired: boolean
  displayOrder: number
}

export interface JobPostingResponse {
  id: number
  title: string
  description: string
  department: string
  status: JobStatus
  applicantCount: number
  createdAt: string
  questions: JobQuestionResponse[]
}

export interface ApplicationAnswerResponse {
  questionId: number
  questionText: string
  answerText: string
}

export interface ApplicationResponse {
  id: number
  jobId: number
  jobTitle: string
  candidateName: string
  candidateEmail: string
  candidatePhone: string
  resumeFileName: string
  resumePreviewUrl: string
  whyJoin: string
  availability: string
  yearsOfExperience: number
  highestEducation: EducationLevel
  status: ApplicationStatus
  appliedAt: string
  answers: ApplicationAnswerResponse[]
}

export interface PayslipResponse {
  id: number
  employeeId: number
  employeeName: string
  periodMonth: number
  periodYear: number
  payType: PayType
  hoursWorked: number | null
  grossSalary: number
  taxDeduction: number
  socialSecurityDeduction: number
  bonusAmount: number
  otherDeductions: number
  netSalary: number
  downloadUrl: string
}

export interface PayrollAdjustment {
  employeeId: number
  bonusAmount?: number
  deductionAmount?: number
  note?: string
}

export interface PayrollPreviewResponse {
  periodMonth: number
  periodYear: number
  alreadyProcessed: boolean
  lines: PayslipResponse[]
  totalGross: number
  totalNet: number
  employeeCount: number
}

export interface EmployeeDocumentResponse {
  id: number
  type: DocumentType
  title: string
  downloadUrl: string
  createdAt: string
}

export interface DashboardStatsResponse {
  totalHeadcount: number
  newHiresThisMonth: number
  exitsThisYear: number
  turnoverRatePercent: number
  pendingLeaveRequests: number
  openJobPostings: number
  departmentDistribution: Record<string, number>
  headcountTrend: { month: string; count: number }[]
}

export interface AnnouncementResponse {
  id: number
  title: string
  body: string
  isPinned: boolean
  authorId: number
  authorName: string
  authorJobTitle: string | null
  commentCount: number
  createdAt: string
  updatedAt: string
  canManage: boolean
}

export interface CommentResponse {
  id: number
  body: string
  authorId: number
  authorName: string
  createdAt: string
  canManage: boolean
}

export type ReportCategory = 'HARASSMENT' | 'DISCRIMINATION' | 'SAFETY' | 'ETHICS_VIOLATION' | 'FINANCIAL_MISCONDUCT' | 'OTHER'
export type ReportStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'DISMISSED'

export interface AnonymousReportResponse {
  id: number
  displayHandle: string
  title: string
  body: string
  category: ReportCategory
  status: ReportStatus
  commentCount: number
  createdAt: string
  isMine: boolean
}

export interface ReportCommentResponse {
  id: number
  displayName: string
  isReporter: boolean
  isMine: boolean
  body: string
  createdAt: string
}

export interface ApiError {
  timestamp: string
  status: number
  message: string
  path: string
  fieldErrors: Record<string, string> | null
}
