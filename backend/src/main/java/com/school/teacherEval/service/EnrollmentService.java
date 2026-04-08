package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
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
    private final EvaluationRepository evaluationRepository;
    
    public boolean isEnrolledByActivity(Long activityId, Long teacherId) {
        return enrollmentRepository.existsByActivityIdAndTeacherIdAndStatus(
            activityId, teacherId, PeriodEnrollment.Status.enrolled);
    }
    
    public List<Activity> getAvailableActivities(Long teacherId) {
        List<Activity> allActive = activityRepository.findByStatus(Activity.Status.active);
        
        return allActive.stream()
            .filter(activity -> canEnroll(teacherId, activity))
            .filter(activity -> !isEnrolledByActivity(activity.getId(), teacherId))
            .toList();
    }
    
    private boolean canEnroll(Long teacherId, Activity activity) {
        Activity.Level level = activity.getLevel();
        if (level == Activity.Level.C) {
            return true;
        }
        
        Activity.Level prevLevel = Activity.Level.getPrevLevel(level);
        if (prevLevel == null) {
            return true;
        }
        
        return evaluationRepository.findByTeacherIdAndActivityIdAndScoreGreaterThanEqual(teacherId, 
            getLastActivityId(teacherId, prevLevel), 60).isPresent();
    }
    
    private Long getLastActivityId(Long teacherId, Activity.Level level) {
        List<Activity> activities = activityRepository.findByLevel(level);
        return activities.isEmpty() ? null : activities.get(0).getId();
    }
    
    @Transactional
    public PeriodEnrollment enroll(Long activityId, Long teacherId) {
        if (isEnrolledByActivity(activityId, teacherId)) {
            throw new RuntimeException("您已报名该活动");
        }
        
        Activity activity = activityRepository.findById(activityId)
            .orElseThrow(() -> new RuntimeException("活动不存在"));
        
        if (activity.getStatus() != Activity.Status.active) {
            throw new RuntimeException("该活动未开启");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEnrollmentStart() != null && now.isBefore(activity.getEnrollmentStart())) {
            throw new RuntimeException("报名尚未开始");
        }
        if (activity.getEnrollmentEnd() != null && now.isAfter(activity.getEnrollmentEnd())) {
            throw new RuntimeException("报名已结束");
        }
        
        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            long enrolledCount = enrollmentRepository.findByActivityId(activityId).stream()
                .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
                .count();
            if (enrolledCount >= activity.getMaxParticipants()) {
                throw new RuntimeException("该活动报名人数已满");
            }
        }
        
        if (!canEnroll(teacherId, activity)) {
            throw new RuntimeException("您需要先通过上一级别考核才能报名");
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
    
    @Transactional
    public void removeEnrollment(Long activityId, Long teacherId) {
        PeriodEnrollment enrollment = enrollmentRepository
            .findByActivityIdAndTeacherId(activityId, teacherId)
            .orElseThrow(() -> new RuntimeException("报名记录不存在"));
        
        enrollment.setStatus(PeriodEnrollment.Status.removed);
        enrollmentRepository.save(enrollment);
    }
}