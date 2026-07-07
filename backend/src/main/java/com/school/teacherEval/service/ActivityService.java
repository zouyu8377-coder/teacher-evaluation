package com.school.teacherEval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.ExamRecordRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final ExamRecordRepository examRecordRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ActivityValidator activityValidator;
    private final EnrollmentEligibilityService enrollmentEligibilityService;

    public List<Activity> getAll() {
        return activityRepository.findAll();
    }
    
    public List<Activity> getByLevel(Activity.Level level) {
        return activityRepository.findByLevel(level);
    }
    
    public List<Activity> getActiveByLevel(Activity.Level level) {
        return activityRepository.findByLevel(level);
    }
    
    public Activity getById(Long id) {
        return activityRepository.findById(id).orElseThrow(() -> new RuntimeException("活动不存在"));
    }
    
    @Transactional
    public Activity create(Activity activity) {
        prepareActivityForLevel(activity);
        normalizeReviewerConfig(activity);
        activityValidator.validateForSave(activity);

        // 默认评分人为0人
        if (activity.getReviewerCount() == null) {
            activity.setReviewerCount(0);
        }
        // C级固定为考试，其他级别固定为文档
        activity.setHasExam(activity.getLevel() == Activity.Level.C);

        // 创建时即校验考核员配置
        activityValidator.validateActivation(activity);

        return activityRepository.save(activity);
    }
    
    @Transactional
    public Activity update(Long id, Activity updated) {
        Activity activity = getById(id);

        validateImmutableFieldsForStartedActivity(id, activity, updated);
        
        if (updated.getMaxParticipants() != null && !updated.getMaxParticipants().equals(activity.getMaxParticipants())) {
            validateCapacityUpdate(id, updated.getMaxParticipants());
        }
        
        if (updated.getName() != null) activity.setName(updated.getName());
        if (updated.getLevel() != null) activity.setLevel(updated.getLevel());
        if (updated.getDescription() != null) activity.setDescription(updated.getDescription());
        if (updated.getMaxParticipants() != null) activity.setMaxParticipants(updated.getMaxParticipants());
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
        if (updated.getPassingScore() != null) {
            if (Boolean.TRUE.equals(activity.getScoresPublished())) {
                throw new BusinessException("成绩已发布，不能修改通过分数线");
            }
            activity.setPassingScore(updated.getPassingScore());
        }

        prepareActivityForLevel(activity);

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
        prepareActivityForLevel(activity);
        normalizeReviewerConfig(activity);
        activityValidator.validateActivation(activity);
        // 保存时保持原有status，不自动更新
        return activityRepository.save(activity);
    }

    private void normalizeReviewerConfig(Activity activity) {
        if (activity == null || activity.getLevel() == Activity.Level.C) {
            if (activity != null) {
                activity.setReviewerIds("[]");
                activity.setReviewerCount(0);
            }
            return;
        }

        List<Long> reviewerIds = parseReviewerIds(activity.getReviewerIds());
        if (reviewerIds.isEmpty()) {
            activity.setReviewerIds("[]");
            activity.setReviewerCount(0);
            return;
        }

        List<Long> validEvaluatorIds = new ArrayList<>();
        for (Long reviewerId : new LinkedHashSet<>(reviewerIds)) {
            User reviewer = userRepository.findById(reviewerId)
                    .orElseThrow(() -> new BusinessException("Reviewer does not exist: " + reviewerId));
            if (reviewer.getRole() != User.Role.evaluator) {
                throw new BusinessException("Only evaluator users can be assigned as reviewers: " + reviewer.getUsername());
            }
            if (!Integer.valueOf(1).equals(reviewer.getStatus())) {
                throw new BusinessException("Reviewer is disabled: " + reviewer.getUsername());
            }
            validEvaluatorIds.add(reviewerId);
        }

        try {
            activity.setReviewerIds(objectMapper.writeValueAsString(validEvaluatorIds));
            activity.setReviewerCount(validEvaluatorIds.size());
        } catch (Exception e) {
            throw new BusinessException("Failed to serialize reviewer config: " + e.getMessage());
        }
    }

    private void prepareActivityForLevel(Activity activity) {
        if (activity == null || activity.getLevel() == null) {
            return;
        }
        if (activity.getLevel() == Activity.Level.C) {
            activity.setHasExam(true);
            activity.setMaterialStart(null);
            activity.setMaterialEnd(null);
            return;
        }
        if (activity.getMaterialStart() == null) {
            activity.setMaterialStart(activity.getEnrollmentStart());
        }
        if (activity.getMaterialEnd() == null) {
            activity.setMaterialEnd(activity.getEnrollmentEnd());
        }
        if (activity.getMaterialStart() != null) {
            activity.setEnrollmentStart(activity.getMaterialStart());
        }
        if (activity.getMaterialEnd() != null) {
            activity.setEnrollmentEnd(activity.getMaterialEnd());
        }
        activity.setHasExam(false);
        activity.setExamStart(null);
        activity.setExamEnd(null);
        activity.setExamPaperId(null);
    }

    private void validateImmutableFieldsForStartedActivity(Long id, Activity current, Activity updated) {
        if (!hasStartedBusinessData(id)) {
            return;
        }

        List<String> changedFields = new ArrayList<>();
        if (updated.getLevel() != null && updated.getLevel() != current.getLevel()) {
            changedFields.add("level");
        }
        if (updated.getEnrollmentStart() != null && !Objects.equals(updated.getEnrollmentStart(), current.getEnrollmentStart())) {
            changedFields.add("enrollmentStart");
        }
        if (updated.getEnrollmentEnd() != null && !Objects.equals(updated.getEnrollmentEnd(), current.getEnrollmentEnd())) {
            changedFields.add("enrollmentEnd");
        }
        if (updated.getExamStart() != null && !Objects.equals(updated.getExamStart(), current.getExamStart())) {
            changedFields.add("examStart");
        }
        if (updated.getExamDurationMinutes() != null && !Objects.equals(updated.getExamDurationMinutes(), current.getExamDurationMinutes())) {
            changedFields.add("examDurationMinutes");
        }
        if (updated.getMaterialStart() != null && !Objects.equals(updated.getMaterialStart(), current.getMaterialStart())) {
            changedFields.add("materialStart");
        }
        if (updated.getMaterialEnd() != null && !Objects.equals(updated.getMaterialEnd(), current.getMaterialEnd())) {
            changedFields.add("materialEnd");
        }
        if (updated.getExamPaperId() != null && !Objects.equals(updated.getExamPaperId(), current.getExamPaperId())) {
            changedFields.add("examPaperId");
        }

        if (!changedFields.isEmpty()) {
            throw new BusinessException("活动已产生报名、考试、材料或评分数据，不能修改关键配置: " + String.join(", ", changedFields));
        }
    }

    private boolean hasStartedBusinessData(Long activityId) {
        return enrollmentRepository.countByActivityIdAndStatus(activityId, PeriodEnrollment.Status.enrolled) > 0
                || evaluationRepository.countByActivityId(activityId) > 0
                || examRecordRepository.countByActivityId(activityId) > 0
                || documentRepository.countByActivityIdAndIsDeleted(activityId, 0) > 0;
    }
    
    @Transactional
    public void delete(Long id) {
        Activity activity = getById(id);
        validateDelete(id);

        // 解除所有报名关系
        List<PeriodEnrollment> enrollments = enrollmentRepository.findByActivityId(id);
        for (PeriodEnrollment enrollment : enrollments) {
            enrollmentRepository.delete(enrollment);
        }

        activityRepository.deleteById(id);
        log.info("删除活动成功: {}, 名称: {}, 已解除 {} 人报名", id, activity.getName(), enrollments.size());
    }
    
    public List<Activity> getAvailableActivities() {
        return activityRepository.findAvailableActivities();
    }

    public List<Activity> getActiveByDate(LocalDate date) {
        return activityRepository.findActiveByDate(date);
    }

    public List<Activity> getAllActiveOrderByLevel() {
        return activityRepository.findAllOrderByLevel();
    }
    
    public List<Activity> getByReviewerId(Long evaluatorId) {
        return activityRepository.findAll().stream()
            .filter(activity -> parseReviewerIds(activity.getReviewerIds()).contains(evaluatorId))
            .toList();
    }
    
    public boolean canEnroll(Long activityId, Long teacherId) {
        Activity activity = getById(activityId);
        return enrollmentEligibilityService.canEnroll(teacherId, activity)
                && isEnrollmentWindowOpen(activity)
                && !isAssessmentWindowEnded(activity);
    }
    
    public List<Activity> getAvailableForTeacher(Long teacherId) {
        List<Activity> activities = activityRepository.findAllOrderByLevel();
        return activities.stream()
            .filter(a -> canEnroll(a.getId(), teacherId))
            .filter(a -> !enrollmentRepository.existsByActivityIdAndTeacherIdAndStatus(
                a.getId(), teacherId, PeriodEnrollment.Status.enrolled))
            .toList();
    }

    public List<Activity> getOtherForTeacher(Long teacherId) {
        List<Activity> available = getAvailableForTeacher(teacherId);
        Set<Long> availableIds = available.stream().map(Activity::getId).collect(Collectors.toSet());
        return activityRepository.findAllOrderByLevel().stream()
                .filter(activity -> activity.getId() != null && !availableIds.contains(activity.getId()))
                .toList();
    }

    public boolean isEnrollmentWindowOpen(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = getEnrollmentWindowStart(activity);
        LocalDateTime end = getEnrollmentWindowEnd(activity);
        if (start != null && now.isBefore(start)) {
            return false;
        }
        return end == null || !now.isAfter(end);
    }

    public boolean isAssessmentWindowEnded(Activity activity) {
        LocalDateTime end = getAssessmentWindowEnd(activity);
        return end != null && LocalDateTime.now().isAfter(end);
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

    private LocalDateTime getAssessmentWindowEnd(Activity activity) {
        if (Boolean.TRUE.equals(activity.getHasExam()) && activity.getExamEnd() != null) {
            return activity.getExamEnd();
        }
        if (activity.getMaterialEnd() != null) {
            return activity.getMaterialEnd();
        }
        return activity.getEnrollmentEnd();
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
        long examRecordCount = examRecordRepository.countByActivityId(id);
        long documentCount = documentRepository.countByActivityIdAndIsDeleted(id, 0);
        if (evalCount > 0) {
            throw new BusinessException("该活动已有评分记录，无法删除");
        }
        if (examRecordCount > 0) {
            throw new BusinessException("该活动已有考试记录，无法删除");
        }
        if (documentCount > 0) {
            throw new BusinessException("该活动已有提交材料，无法删除");
        }

        log.info("删除活动验证通过: {}, 名称: {}", id, activity.getName());
    }
    
    public long getPassedCountByLevel(Activity.Level level) {
        return evaluationRepository.countPassedTeachersByLevel(level);
    }

    private List<Long> parseReviewerIds(String reviewerIds) {
        if (reviewerIds == null || reviewerIds.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(reviewerIds, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse reviewerIds for activity filtering: {}", reviewerIds, e);
            return List.of();
        }
    }
}
