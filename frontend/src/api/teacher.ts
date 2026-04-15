import api from './index'

export interface UserInfo {
  id: number
  username: string
  realName: string
  department: string
  role: string
  status: number
  createdAt: string
}

export interface LevelInfo {
  level: string
  levelName: string
  hasPassed: boolean
  bestScore: number | null
  passedAt: string | null
  nextLevel: string
  canEnrollNext: boolean
}

export interface EnrollmentInfo {
  enrollmentId: number
  activityId: number
  activityName: string
  level: string
  levelName: string
  status: string
  enrolledAt: string
  examStartTime: string | null
  examEndTime: string | null
  materialStartTime: string | null
  materialEndTime: string | null
  hasExam: boolean
  examDurationMinutes: number
  examStatus: 'not_started' | 'in_progress' | 'completed'
  examRecordId: number | null
}

export interface HistoryRecord {
  activityId: number
  activityName: string
  level: string
  levelName: string
  status: string
  finalScore: number | null
  isPassed: boolean
  enrolledAt: string
  completedAt: string | null
}

export interface TodoItem {
  type: 'exam' | 'enrollment' | 'evaluation'
  title: string
  description: string
  relatedId: number
  actionUrl: string
  deadline: string | null
}

export interface TeacherDashboard {
  userInfo: UserInfo
  currentLevel: LevelInfo
  currentEnrollments: EnrollmentInfo[]
  historyRecords: HistoryRecord[]
  todoItems: TodoItem[]
}

export const getTeacherDashboard = () => {
  return api.get('/teacher/dashboard')
}