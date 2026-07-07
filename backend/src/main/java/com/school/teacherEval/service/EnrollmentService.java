package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    
    private final EnrollmentRepository enrollmentRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final EnrollmentEligibilityService enrollmentEligibilityService;
    
    public boolean isEnrolledByActivity(Long activityId, Long teacherId) {
        return enrollmentRepository.existsByActivityIdAndTeacherIdAndStatus(
            activityId, teacherId, PeriodEnrollment.Status.enrolled);
    }
    
    public List<Activity> getAvailableActivities(Long teacherId) {
        List<Activity> allActivities = activityRepository.findAll();

        return allActivities.stream()
            .filter(activity -> canEnroll(teacherId, activity))
            .filter(activity -> !isEnrolledByActivity(activity.getId(), teacherId))
            .toList();
    }
    
    private boolean canEnroll(Long teacherId, Activity activity) {
        return enrollmentEligibilityService.canEnroll(teacherId, activity)
            && isEnrollmentWindowOpen(activity)
            && !isAssessmentWindowEnded(activity);
    }
    
    @Transactional
    public PeriodEnrollment enroll(Long activityId, Long teacherId) {
        if (isEnrolledByActivity(activityId, teacherId)) {
            throw new BusinessException("您已报名该活动");
        }

        Activity activity = activityRepository.findById(activityId)
            .orElseThrow(() -> new BusinessException("活动不存在"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime enrollStart = getEnrollmentWindowStart(activity);
        LocalDateTime enrollEnd = getEnrollmentWindowEnd(activity);
        if (enrollStart != null && now.isBefore(enrollStart)) {
            throw new BusinessException("报名尚未开始");
        }
        if (enrollEnd != null && now.isAfter(enrollEnd)) {
            throw new BusinessException("报名已结束");
        }
        if (isAssessmentWindowEnded(activity)) {
            throw new BusinessException("活动已结束");
        }

        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            long enrolledCount = enrollmentRepository.findByActivityId(activityId).stream()
                .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
                .count();
            if (enrolledCount >= activity.getMaxParticipants()) {
                throw new BusinessException("该活动报名人数已满");
            }
        }

        if (!canEnroll(teacherId, activity)) {
            throw new BusinessException("您需要先通过上一级别考核才能报名");
        }

        PeriodEnrollment enrollment = new PeriodEnrollment();
        enrollment.setActivityId(activityId);
        enrollment.setTeacherId(teacherId);
        enrollment.setStatus(PeriodEnrollment.Status.enrolled);

        return enrollmentRepository.save(enrollment);
    }

    private LocalDateTime getEnrollmentWindowStart(Activity activity) {
        if (activity.getLevel() != Activity.Level.C && activity.getMaterialStart() != null) {
            return activity.getMaterialStart();
        }
        return activity.getEnrollmentStart();
    }

    private LocalDateTime getEnrollmentWindowEnd(Activity activity) {
        if (activity.getLevel() != Activity.Level.C && activity.getMaterialEnd() != null) {
            return activity.getMaterialEnd();
        }
        return activity.getEnrollmentEnd();
    }

    private boolean isEnrollmentWindowOpen(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime enrollStart = getEnrollmentWindowStart(activity);
        LocalDateTime enrollEnd = getEnrollmentWindowEnd(activity);
        if (enrollStart != null && now.isBefore(enrollStart)) {
            return false;
        }
        return enrollEnd == null || !now.isAfter(enrollEnd);
    }

    private boolean isAssessmentWindowEnded(Activity activity) {
        LocalDateTime end = getAssessmentWindowEnd(activity);
        return end != null && LocalDateTime.now().isAfter(end);
    }

    private LocalDateTime getAssessmentWindowEnd(Activity activity) {
        if (Boolean.TRUE.equals(activity.getHasExam()) && activity.getExamEnd() != null) {
            return activity.getExamEnd();
        }
        if (activity.getMaterialEnd() != null) {
            return activity.getMaterialEnd();
        }
        return activity.getEnrollmentEnd();
    }
    
    public List<PeriodEnrollment> getTeacherEnrollments(Long teacherId) {
        return enrollmentRepository.findByTeacherId(teacherId);
    }
    
    public List<User> getEnrolledTeachersByActivity(Long activityId) {
        List<PeriodEnrollment> enrollments = enrollmentRepository.findByActivityId(activityId);
        List<Long> teacherIds = enrollments.stream()
            .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
            .map(PeriodEnrollment::getTeacherId)
            .toList();
        return userRepository.findAllById(teacherIds);
    }

    public PeriodEnrollment getEnrollment(Long activityId, Long teacherId) {
        return enrollmentRepository.findByActivityIdAndTeacherId(activityId, teacherId).orElse(null);
    }

    @Transactional
    public void removeEnrollment(Long activityId, Long teacherId) {
        PeriodEnrollment enrollment = enrollmentRepository
            .findByActivityIdAndTeacherId(activityId, teacherId)
            .orElseThrow(() -> new RuntimeException("报名记录不存在"));
        
        enrollment.setStatus(PeriodEnrollment.Status.removed);
        enrollmentRepository.save(enrollment);
    }
}
