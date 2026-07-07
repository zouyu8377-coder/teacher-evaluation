// 通用分页响应（对应后端 PageVO）
export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// 通用 API 响应包装
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 用户 VO（对应后端 UserVO / CurrentUserVO）
export interface UserVO {
  id: number
  username: string
  realName: string
  role: string
  department: string
  status?: number
  createdAt?: string
  teacherLevel?: string
  levelChangedAt?: string
}

// 活动（已有 Activity 接口，复用）
export interface Activity {
  id: number
  name: string
  level: 'C' | 'B2' | 'B1' | 'A2' | 'A1'
  description: string
  maxParticipants: number
  timeStatus?: 'not_started' | 'in_progress' | 'ended'
  scoresPublished?: boolean
  enrollmentStart: string
  enrollmentEnd: string
  examStart?: string
  examEnd?: string
  startDate: string
  endDate: string
  reviewerCount: number
  reviewerIds: string
  examPaperId?: number
  hasExam: boolean
  passingScore?: number | null
  examDurationMinutes: number
  createdAt: string
}

// 我的报名 VO（对应后端 MyEnrollmentVO）
export interface MyEnrollmentVO {
  id: number
  activityId: number
  enrolledAt: string
  activityName: string
  level: string
  hasExam: boolean
  startDate: string
  endDate: string
  examStart: string | null
  examEnd: string | null
  materialStart: string | null
  materialEnd: string | null
  examRecordId: number | null
  examScore: number | null
  examStatus: string | null
  examSubmittedAt: string | null
  documentId: number | null
  materialStatus?: string | null
  materialSubmittedAt?: string | null
  canConfirmMaterial?: boolean
  canCancelMaterial?: boolean
  scorePublished: boolean
  finalScore: number | null
  isPassed: boolean | null
  comment: string | null
  businessStatus?: string
  statusText?: string
  availableActions?: string[]
}

// 报名教师 VO（对应后端 EnrollmentTeacherVO）
export interface EnrollmentTeacherVO {
  id: number
  username: string
  realName: string
  department: string
  enrolledAt: string | null
  examRecordId: number | null
  submittedAt: string | null
  submissionStatus: string
  materialStatus?: string | null
  examScore?: number | null
  finalScore?: number | null
  isPassed?: boolean | null
  evaluations?: EvaluationVO[]
}

// 报名信息 VO（对应后端 EnrollmentInfoVO）
export interface EnrollmentInfoVO {
  activityId: number
  activityName: string
  level: string
  hasExam: boolean
  maxParticipants: number | null
  enrolledCount: number
  remaining: number
  enrollmentStart: string | null
  enrollmentEnd: string | null
  startDate: string | null
  endDate: string | null
  reviewerCount: number | null
  enrolledAt: string | null
  enrollmentStatus: string | null
  examRecordId: number | null
  examScore: number | null
  examStatus: string | null
  examSubmittedAt: string | null
  documentId: number | null
  documentTitle: string | null
  documentFileName: string | null
  documentFileSize: number | null
  documentCreatedAt: string | null
  materialStatus?: string | null
  materialSubmittedAt?: string | null
  canConfirmMaterial?: boolean
  canCancelMaterial?: boolean
  scorePublished: boolean
  finalScore: number | null
  isPassed: boolean | null
  comment: string | null
  businessStatus?: string
  statusText?: string
  availableActions?: string[]
}

// 评分人统计 VO（对应后端 ReviewerStatVO）
export interface ReviewerStatVO {
  id: number
  realName: string
  completedCount: number
  totalRequired: number
}

// 评分进度 VO（对应后端 ReviewProgressVO）
export interface ReviewProgressVO {
  enrolledCount: number
  reviewerCount: number | null
  reviewerStats: ReviewerStatVO[]
  totalCompleted: number
  totalRequired: number
  reviewStatus: string
  scoresPublished: boolean
}

// 文档 VO（对应后端 DocumentVO）
export interface DocumentVO {
  id: number
  userId: number
  activityId: number
  title: string
  fileName: string
  fileSize: number
  fileType: string
  description: string
  createdAt: string
  realName: string
}

// 评分 VO（对应后端 EvaluationVO）
export interface EvaluationVO {
  id: number
  activityId: number
  evaluatorId: number
  teacherId: number
  score: number
  finalScore: number | null
  comment: string | null
  status: string
  isPublished: boolean
  isLocked: boolean
  isPassed: boolean | null
  createdAt: string
  evaluatorName: string
  teacherName: string
}

// 评分列表 VO（对应后端 EvaluationListVO）
export interface EvaluationListVO {
  evaluations: EvaluationVO[]
  count: number
  averageScore: number | null
}

// 评分汇总 VO（对应后端 EvaluationSummaryVO）
export interface EvaluationSummaryVO {
  totalEvaluations: number
  averageScore: number | null
  evaluations: EvaluationVO[]
}

// 学习资料 VO（对应后端 LearningMaterialVO）
export interface LearningMaterialVO {
  id: number
  activityId: number
  title: string
  fileName: string
  fileSize: number
  fileType: string
  description: string
  createdBy: number
  createdAt: string
  creatorName: string
  activityName: string
}
