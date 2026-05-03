package com.school.teacherEval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final ActivityValidator activityValidator;

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
        activityValidator.validateForSave(activity);

        // 默认评分人为0人
        if (activity.getReviewerCount() == null) {
            activity.setReviewerCount(0);
        }
        // 默认状态为草稿
        if (activity.getStatus() == null) {
            activity.setStatus(Activity.Status.draft);
        }
        // C级固定为考试，其他级别固定为文档
        activity.setHasExam(activity.getLevel() == Activity.Level.C);
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
            Activity.Status oldStatus = activity.getStatus();
            activity.setStatus(updated.getStatus());
            // 只有在状态切换为 active 时才执行启用校验
            if (updated.getStatus() == Activity.Status.active && oldStatus != Activity.Status.active) {
                activityValidator.validateActivation(activity);
            }
        }
        if (updated.getEnrollmentStart() != null) activity.setEnrollmentStart(updated.getEnrollmentStart());
        if (updated.getEnrollmentEnd() != null) activity.setEnrollmentEnd(updated.getEnrollmentEnd());
        if (updated.getExamStart() != null) activity.setExamStart(updated.getExamStart());
        // 更新考试时长
        if (updated.getExamDurationMinutes() != null) {
            activity.setExamDurationMinutes(updated.getExamDurationMinutes());
        }
        // 计算考试结束时间（只要 examStart 或 examDurationMinutes 有传入就重新计算）
        if (activity.getExamStart() != null && activity.getExamDurationMinutes() != null) {
            activity.setExamEnd(activity.getExamStart().plusMinutes(activity.getExamDurationMinutes()));
        }
        if (updated.getMaterialStart() != null) activity.setMaterialStart(updated.getMaterialStart());
        if (updated.getMaterialEnd() != null) activity.setMaterialEnd(updated.getMaterialEnd());
        if (updated.getStartDate() != null) activity.setStartDate(updated.getStartDate());
        if (updated.getEndDate() != null) activity.setEndDate(updated.getEndDate());

        // 统一业务规则校验
        activityValidator.validateForSave(activity);

        if (updated.getReviewerCount() != null) {
            activity.setReviewerCount(updated.getReviewerCount());
        } else if (activity.getReviewerCount() == null) {
            activity.setReviewerCount(0);
        }
        if (updated.getReviewerIds() != null) activity.setReviewerIds(updated.getReviewerIds());
        if (updated.getExamPaperId() != null) activity.setExamPaperId(updated.getExamPaperId());
        // 只有在创建时才设置hasExam，update时不自动修改
        if (updated.getExamDurationMinutes() != null) activity.setExamDurationMinutes(updated.getExamDurationMinutes());
        // 保存时保持原有status，不自动更新
        return activityRepository.save(activity);
    }
    
    @Transactional
    public void delete(Long id) {
        validateDelete(id);
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
        Activity.Level targetLevel = activity.getLevel();

        // C级没有前置要求
        if (targetLevel.getPrevLevels().isEmpty()) {
            return true;
        }

        // 获取该教师所有已通过的活动级别
        List<PeriodEnrollment> enrollments = enrollmentRepository.findByTeacherId(teacherId);
        for (PeriodEnrollment enrollment : enrollments) {
            if (enrollment.getStatus() != PeriodEnrollment.Status.enrolled) {
                continue;
            }

            Activity prevActivity = activityRepository.findById(enrollment.getActivityId()).orElse(null);
            if (prevActivity == null) {
                continue;
            }

            Activity.Level passedLevel = prevActivity.getLevel();

            // 检查通过的级别是否是目标级别的前置级别之一
            if (Activity.Level.canProgressTo(passedLevel, targetLevel)) {
                // 需要确认该活动已发布成绩且及格
                List<Evaluation> evals = evaluationRepository.findByTeacherIdAndActivityId(teacherId, enrollment.getActivityId());
                for (Evaluation eval : evals) {
                    if (eval.getStatus() == Evaluation.Status.submitted &&
                        Boolean.TRUE.equals(eval.getIsPublished()) &&
                        eval.getFinalScore() != null &&
                        eval.getFinalScore().compareTo(new java.math.BigDecimal("60")) >= 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    
    public List<Activity> getAvailableForTeacher(Long teacherId) {
        List<Activity> activities = activityRepository.findAllActiveOrderByLevel();
        return activities.stream()
            .filter(a -> canEnroll(a.getId(), teacherId))
            .filter(a -> !enrollmentRepository.existsByActivityIdAndTeacherIdAndStatus(
                a.getId(), teacherId, PeriodEnrollment.Status.enrolled))
            .toList();
    }
    
    public long getEnrolledCount(Long activityId) {
        return enrollmentRepository.countByActivityIdAndStatus(activityId, PeriodEnrollment.Status.enrolled);
    }
    
    public void validateCapacityUpdate(Long activityId, Integer newMaxParticipants) {
        long currentCount = getEnrolledCount(activityId);
        if (newMaxParticipants != null && newMaxParticipants < currentCount) {
            throw new BusinessException("容量不能小于当前已报名人数(" + currentCount + "人)");
        }
    }

    /**
     * 删除活动前的关联检查
     */
    public void validateDelete(Long id) {
        Activity activity = getById(id);

        // 检查是否有报名记录
        long enrollmentCount = enrollmentRepository.countByActivityIdAndStatus(id, PeriodEnrollment.Status.enrolled);
        if (enrollmentCount > 0) {
            throw new BusinessException("该活动已有 " + enrollmentCount + " 人报名，无法删除");
        }

        // 检查是否有评分记录
        long evalCount = evaluationRepository.countByActivityId(id);
        if (evalCount > 0) {
            throw new BusinessException("该活动已有评分记录，无法删除");
        }

        log.info("删除活动验证通过: {}, 名称: {}", id, activity.getName());
    }
    
    public long getPassedCountByLevel(Activity.Level level) {
        return evaluationRepository.countPassedTeachersByLevel(level);
    }
}