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
        return enrollmentEligibilityService.canEnroll(teacherId, activity);
    }
    
    @Transactional
    public PeriodEnrollment enroll(Long activityId, Long teacherId) {
        if (isEnrolledByActivity(activityId, teacherId)) {
            throw new BusinessException("您已报名该活动");
        }

        Activity activity = activityRepository.findById(activityId)
            .orElseThrow(() -> new BusinessException("活动不存在"));

        LocalDateTime now = LocalDateTime.now();
        if (activity.getEnrollmentStart() != null && now.isBefore(activity.getEnrollmentStart())) {
            throw new BusinessException("报名尚未开始");
        }
        if (activity.getEnrollmentEnd() != null && now.isAfter(activity.getEnrollmentEnd())) {
            throw new BusinessException("报名已结束");
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
