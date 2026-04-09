package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EvaluationRepository evaluationRepository;
    
    public List<Activity> getAll() {
        return activityRepository.findAll();
    }
    
    public List<Activity> getByLevel(Activity.Level level) {
        return activityRepository.findByLevel(level);
    }
    
    public List<Activity> getActiveByLevel(Activity.Level level) {
        return activityRepository.findByLevelAndStatus(level, Activity.Status.active);
    }
    
    public Activity getById(Long id) {
        return activityRepository.findById(id).orElseThrow(() -> new RuntimeException("活动不存在"));
    }
    
    @Transactional
    public Activity create(Activity activity) {
        if (activity.getReviewerCount() == null) {
            activity.setReviewerCount(2);
        }
        return activityRepository.save(activity);
    }
    
    @Transactional
    public Activity update(Long id, Activity updated) {
        Activity activity = getById(id);
        
        if (updated.getMaxParticipants() != null && !updated.getMaxParticipants().equals(activity.getMaxParticipants())) {
            validateCapacityUpdate(id, updated.getMaxParticipants());
        }
        
        if (updated.getName() != null) activity.setName(updated.getName());
        if (updated.getLevel() != null) activity.setLevel(updated.getLevel());
        if (updated.getDescription() != null) activity.setDescription(updated.getDescription());
        if (updated.getMaxParticipants() != null) activity.setMaxParticipants(updated.getMaxParticipants());
        if (updated.getStatus() != null) {
            activity.setStatus(updated.getStatus());
        }
        if (updated.getEnrollmentStart() != null) activity.setEnrollmentStart(updated.getEnrollmentStart());
        if (updated.getEnrollmentEnd() != null) activity.setEnrollmentEnd(updated.getEnrollmentEnd());
        if (updated.getStartDate() != null) activity.setStartDate(updated.getStartDate());
        if (updated.getEndDate() != null) activity.setEndDate(updated.getEndDate());
        if (updated.getReviewerCount() != null) {
            activity.setReviewerCount(updated.getReviewerCount());
        } else if (activity.getReviewerCount() == null) {
            activity.setReviewerCount(2);
        }
        if (updated.getReviewerIds() != null) activity.setReviewerIds(updated.getReviewerIds());
        if (updated.getExamPaperId() != null) activity.setExamPaperId(updated.getExamPaperId());
        if (updated.getHasExam() != null) activity.setHasExam(updated.getHasExam());
        if (updated.getExamDurationMinutes() != null) activity.setExamDurationMinutes(updated.getExamDurationMinutes());
        return activityRepository.save(activity);
    }
    
    @Transactional
    public void delete(Long id) {
        activityRepository.deleteById(id);
    }
    
    public List<Activity> getAvailableActivities() {
        return activityRepository.findAvailableActivities();
    }
    
    public List<Activity> getActiveByDate(LocalDate date) {
        return activityRepository.findActiveByDate(date);
    }
    
    public List<Activity> getAllActiveOrderByLevel() {
        return activityRepository.findAllActiveOrderByLevel();
    }
    
    public List<Activity> getByReviewerId(Long evaluatorId) {
        return activityRepository.findByReviewerId(evaluatorId);
    }
    
    @Transactional
    public Activity updateReviewerConfig(Long id, Integer reviewerCount, String reviewerIds) {
        Activity activity = getById(id);
        activity.setReviewerCount(reviewerCount);
        activity.setReviewerIds(reviewerIds);
        return activityRepository.save(activity);
    }
    
    public boolean canEnroll(Long activityId, Long teacherId) {
        Activity activity = getById(activityId);
        Activity.Level currentLevel = activity.getLevel();
        
        if (currentLevel == Activity.Level.C) {
            return true;
        }
        
        Activity.Level prevLevel = Activity.Level.getPrevLevel(currentLevel);
        
        List<PeriodEnrollment> prevEnrollments = enrollmentRepository.findByTeacherId(teacherId);
        
        for (PeriodEnrollment enrollment : prevEnrollments) {
            if (enrollment.getStatus() != PeriodEnrollment.Status.enrolled) {
                continue;
            }
            Activity prevActivity = activityRepository.findById(enrollment.getActivityId()).orElse(null);
            if (prevActivity == null || prevActivity.getLevel() != prevLevel) {
                continue;
            }
            
            List<Evaluation> evals = evaluationRepository.findByTeacherIdAndActivityId(teacherId, enrollment.getActivityId());
            for (Evaluation eval : evals) {
                if (eval.getStatus() == Evaluation.Status.submitted && 
                    eval.getIsPublished() != null && eval.getIsPublished() &&
                    eval.getFinalScore() != null && eval.getFinalScore().compareTo(new java.math.BigDecimal("60")) >= 0) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public List<Activity> getAvailableForTeacher(Long teacherId) {
        List<Activity> activities = activityRepository.findAllActiveOrderByLevel();
        return activities.stream()
            .filter(a -> canEnroll(a.getId(), teacherId))
            .toList();
    }
    
    public long getEnrolledCount(Long activityId) {
        return enrollmentRepository.countByActivityIdAndStatus(activityId, PeriodEnrollment.Status.enrolled);
    }
    
    public void validateCapacityUpdate(Long activityId, Integer newMaxParticipants) {
        long currentCount = getEnrolledCount(activityId);
        if (newMaxParticipants != null && newMaxParticipants < currentCount) {
            throw new RuntimeException("容量不能小于当前已报名人数(" + currentCount + "人)");
        }
    }
    
    public long getPassedCountByLevel(Activity.Level level) {
        return evaluationRepository.countPassedTeachersByLevel(level);
    }
}