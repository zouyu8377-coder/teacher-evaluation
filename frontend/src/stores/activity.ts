import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getActivityList,
  getAvailableActivitiesForTeacher,
  getOtherActivitiesForTeacher,
  getMyEnrollments,
  getEnrollmentInfo,
  enrollActivity,
  canEnrollActivity,
  getReviewProgress,
  getActivityEnrollments
} from '@/api/activity'
import type { Activity, MyEnrollmentVO, EnrollmentInfoVO, ReviewProgressVO, EnrollmentTeacherVO } from '@/api/types'

export const useActivityStore = defineStore('activity', () => {
  // State
  const allActivities = ref<Activity[]>([])
  const availableActivities = ref<Activity[]>([])
  const otherActivities = ref<Activity[]>([])
  const myEnrollments = ref<MyEnrollmentVO[]>([])
  const enrollmentInfoMap = ref<Record<number, EnrollmentInfoVO>>({})
  const reviewProgressMap = ref<Record<number, ReviewProgressVO>>({})
  const enrollmentTeachersMap = ref<Record<number, EnrollmentTeacherVO[]>>({})
  const loading = ref(false)

  // Actions
  const loadAllActivities = async (activeOnly = false) => {
    loading.value = true
    try {
      const res: any = await getActivityList(activeOnly)
      if (res.code === 200) {
        allActivities.value = res.data || []
      }
      return allActivities.value
    } finally {
      loading.value = false
    }
  }

  const loadAvailableActivities = async () => {
    loading.value = true
    try {
      const res: any = await getAvailableActivitiesForTeacher()
      if (res.code === 200) {
        availableActivities.value = res.data || []
      }
      return availableActivities.value
    } finally {
      loading.value = false
    }
  }

  const loadOtherActivities = async () => {
    loading.value = true
    try {
      const res: any = await getOtherActivitiesForTeacher()
      if (res.code === 200) {
        otherActivities.value = res.data || []
      }
      return otherActivities.value
    } finally {
      loading.value = false
    }
  }

  const loadMyEnrollments = async () => {
    loading.value = true
    try {
      const res: any = await getMyEnrollments()
      if (res.code === 200) {
        myEnrollments.value = res.data || []
      }
      return myEnrollments.value
    } finally {
      loading.value = false
    }
  }

  const loadEnrollmentInfo = async (activityId: number) => {
    const res: any = await getEnrollmentInfo(activityId)
    if (res.code === 200 && res.data) {
      enrollmentInfoMap.value[activityId] = res.data
    }
    return enrollmentInfoMap.value[activityId]
  }

  const loadReviewProgress = async (activityId: number) => {
    const res: any = await getReviewProgress(activityId)
    if (res.code === 200 && res.data) {
      reviewProgressMap.value[activityId] = res.data
    }
    return reviewProgressMap.value[activityId]
  }

  const loadEnrollmentTeachers = async (activityId: number) => {
    const res: any = await getActivityEnrollments(activityId)
    if (res.code === 200 && res.data) {
      enrollmentTeachersMap.value[activityId] = res.data
    }
    return enrollmentTeachersMap.value[activityId]
  }

  const doEnroll = async (activityId: number) => {
    const res: any = await enrollActivity(activityId)
    if (res.code === 200) {
      // 报名成功后刷新相关缓存
      await loadMyEnrollments()
      await loadAvailableActivities()
    }
    return res
  }

  const checkCanEnroll = async (activityId: number) => {
    const res: any = await canEnrollActivity(activityId)
    return res.code === 200 ? res.data : false
  }

  // Getters
  const getEnrollmentInfoById = (activityId: number) => enrollmentInfoMap.value[activityId]
  const getReviewProgressById = (activityId: number) => reviewProgressMap.value[activityId]
  const getEnrollmentTeachersById = (activityId: number) => enrollmentTeachersMap.value[activityId]

  return {
    allActivities,
    availableActivities,
    otherActivities,
    myEnrollments,
    loading,
    loadAllActivities,
    loadAvailableActivities,
    loadOtherActivities,
    loadMyEnrollments,
    loadEnrollmentInfo,
    loadReviewProgress,
    loadEnrollmentTeachers,
    doEnroll,
    checkCanEnroll,
    getEnrollmentInfoById,
    getReviewProgressById,
    getEnrollmentTeachersById
  }
})
