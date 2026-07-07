package com.school.teacherEval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final EvaluationValidator evaluationValidator;
    private final TeacherLevelService teacherLevelService;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamRecordService examRecordService;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Page<Evaluation> getEvaluations(Long activityId, Long teacherId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (activityId != null && teacherId != null) {
            List<Evaluation> list = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), list.size());
            List<Evaluation> subList = start < list.size() ? list.subList(start, end) : List.of();
            return new PageImpl<>(subList, pageable, list.size());
        }
        if (activityId != null) {
            return evaluationRepository.findByActivityId(activityId, pageable);
        }
        if (teacherId != null) {
            return evaluationRepository.findByTeacherId(teacherId, pageable);
        }

        return evaluationRepository.findAll(pageable);
    }

    public Evaluation getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("评分记录不存在"));
    }

    public List<Evaluation> getTeacherEvaluations(Long teacherId) {
        return evaluationRepository.findByTeacherId(teacherId);
    }

    public List<Evaluation> getTeacherActivityEvaluations(Long teacherId, Long activityId) {
        return evaluationRepository.findByTeacherIdAndActivityId(teacherId, activityId);
    }

    public List<Evaluation> getTeacherPublishedEvaluations(Long teacherId) {
        return evaluationRepository.findByTeacherIdAndIsPublished(teacherId);
    }

    public List<Evaluation> getEvaluatorEvaluations(Long evaluatorId) {
        return evaluationRepository.findByEvaluatorId(evaluatorId);
    }

    public List<Evaluation> getActivityTeacherEvaluations(Long activityId, Long teacherId) {
        return evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
    }

    @Transactional
    public Evaluation createOrUpdateEvaluation(Long evaluatorId, Long teacherId, Long activityId,
                                                BigDecimal score, String comment) {
        // 统一业务校验
        evaluationValidator.validateForSubmit(evaluatorId, teacherId, activityId, score);

        log.info("考核员 {} 为教师 {} 在活动 {} 评分: {}", evaluatorId, teacherId, activityId, score);

        Evaluation evaluation = evaluationRepository
                .findByTeacherIdAndActivityIdAndEvaluatorId(teacherId, activityId, evaluatorId)
                .orElse(new Evaluation());

        return buildAndSaveEvaluation(evaluation, evaluatorId, teacherId, activityId, score, comment);
    }

    private Evaluation buildAndSaveEvaluation(Evaluation evaluation, Long evaluatorId, Long teacherId,
                                               Long activityId, BigDecimal score, String comment) {
        evaluation.setActivityId(activityId);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setTeacherId(teacherId);
        evaluation.setScore(score);
        evaluation.setComment(comment);
        evaluation.setStatus(Evaluation.Status.submitted);
        return evaluationRepository.save(evaluation);
    }

    public long countByActivityId(Long activityId) {
        return evaluationRepository.countByActivityId(activityId);
    }

    public long countByStatus(Evaluation.Status status) {
        return evaluationRepository.countByStatus(status);
    }

    public long countByActivityIdAndEvaluatorId(Long activityId, Long evaluatorId) {
        return evaluationRepository.countByActivityIdAndEvaluatorId(activityId, evaluatorId);
    }

    private static final Long SYSTEM_EVALUATOR_ID = 1L;

    @Transactional
    public int publishScores(Long activityId, Long teacherId, BigDecimal passingScore) {
        Activity activity = activityService.getById(activityId);

        validatePublishTiming(activity, teacherId);
        validateNotDuplicatePublish(activity, teacherId);

        if (passingScore == null) {
            throw new BusinessException("请设置通过分数线");
        }

        // 保存通过分数线
        activity.setPassingScore(passingScore);
        activityRepository.save(activity);

        // 非C级活动：清理非配置评分员的历史遗留评分
        if (activity.getLevel() != Activity.Level.C) {
            cleanupExtraEvaluations(activityId, activity.getReviewerIds());
        }

        if (activity.getLevel() == Activity.Level.C) {
            finalizeMissingCLevelSubmissions(activity, teacherId);
        }

        List<Evaluation> evaluations = teacherId != null
                ? evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId)
                : evaluationRepository.findByActivityId(activityId);

        int count;
        if (activity.getLevel() == Activity.Level.C) {
            count = applyFinalScoresForCLevel(activity, evaluations, passingScore);
        } else {
            count = applyFinalScores(activity, evaluations, passingScore);
        }

        log.info("发布成绩 - 活动: {}, 教师: {}, 通过线: {}, 发布数量: {}", activityId, teacherId, passingScore, count);

        if (teacherId == null) {
            finalizeActivityAfterPublish(activity);
        }

        return count;
    }

    private void validatePublishTiming(Activity activity, Long teacherId) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getLevel() == Activity.Level.C) {
            if (activity.getExamEnd() != null && now.isBefore(activity.getExamEnd())) {
                throw new BusinessException("???????????????");
            }
            return;
        }
        LocalDateTime materialEnd = activity.getMaterialEnd() != null
                ? activity.getMaterialEnd()
                : activity.getEnrollmentEnd();
        if (materialEnd != null && now.isBefore(materialEnd) && !isNonCReviewComplete(activity, teacherId)) {
            throw new BusinessException("?????????????????????????");
        }
    }

    private boolean isNonCReviewComplete(Activity activity, Long targetTeacherId) {
        int requiredReviewers = activity.getReviewerCount() != null ? activity.getReviewerCount() : 0;
        if (requiredReviewers <= 0) {
            return false;
        }

        List<PeriodEnrollment> enrollments = enrollmentRepository.findByActivityId(activity.getId()).stream()
                .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
                .filter(e -> targetTeacherId == null || targetTeacherId.equals(e.getTeacherId()))
                .toList();
        if (enrollments.isEmpty()) {
            return false;
        }

        List<Long> reviewerIds = parseReviewerIds(activity.getReviewerIds());
        List<Evaluation> evaluations = evaluationRepository.findByActivityId(activity.getId()).stream()
                .filter(e -> e.getScore() != null)
                .filter(e -> e.getStatus() == Evaluation.Status.submitted)
                .filter(e -> !SYSTEM_EVALUATOR_ID.equals(e.getEvaluatorId()))
                .filter(e -> reviewerIds.isEmpty() || reviewerIds.contains(e.getEvaluatorId()))
                .toList();

        for (PeriodEnrollment enrollment : enrollments) {
            long completed = evaluations.stream()
                    .filter(e -> enrollment.getTeacherId().equals(e.getTeacherId()))
                    .map(Evaluation::getEvaluatorId)
                    .distinct()
                    .count();
            if (completed < requiredReviewers) {
                return false;
            }
        }
        return true;
    }

    private void validateNotDuplicatePublish(Activity activity, Long teacherId) {
        if (teacherId == null && Boolean.TRUE.equals(activity.getScoresPublished())) {
            throw new BusinessException("该考核活动的成绩已公布，无法重复公布");
        }
    }

    private void cleanupExtraEvaluations(Long activityId, String reviewerIdsJson) {
        List<Long> reviewerIdList = parseReviewerIds(reviewerIdsJson);
        if (reviewerIdList.isEmpty()) {
            return;
        }
        List<Evaluation> allEvals = evaluationRepository.findByActivityId(activityId);
        List<Evaluation> extraEvals = allEvals.stream()
                .filter(e -> !SYSTEM_EVALUATOR_ID.equals(e.getEvaluatorId()))
                .filter(e -> !reviewerIdList.contains(e.getEvaluatorId()))
                .toList();
        if (!extraEvals.isEmpty()) {
            log.warn("清理活动 {} 的非配置评分员评分记录，数量: {}", activityId, extraEvals.size());
            evaluationRepository.deleteAll(extraEvals);
        }
    }

    private void finalizeMissingCLevelSubmissions(Activity activity, Long targetTeacherId) {
        List<PeriodEnrollment> enrollments = enrollmentRepository.findByActivityId(activity.getId());
        for (PeriodEnrollment enrollment : enrollments) {
            if (enrollment.getStatus() != PeriodEnrollment.Status.enrolled) {
                continue;
            }
            if (targetTeacherId != null && !targetTeacherId.equals(enrollment.getTeacherId())) {
                continue;
            }
            examRecordService.markMissingSubmissionAsZero(activity, enrollment.getTeacherId());
        }
    }

    private int applyFinalScores(Activity activity, List<Evaluation> evaluations, BigDecimal passingScore) {
        Map<Long, List<Evaluation>> byTeacher = evaluations.stream()
                .filter(e -> !SYSTEM_EVALUATOR_ID.equals(e.getEvaluatorId()))
                .collect(Collectors.groupingBy(Evaluation::getTeacherId));

        int count = 0;
        List<PeriodEnrollment> enrollments = enrollmentRepository.findByActivityId(activity.getId()).stream()
                .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
                .toList();
        for (PeriodEnrollment enrollment : enrollments) {
            Long teacherId = enrollment.getTeacherId();
            boolean hasDocument = documentRepository.findFirstByActivityIdAndUserId(activity.getId(), teacherId).isPresent();
            if (!hasDocument) {
                publishMissingDocumentAsFailed(activity, teacherId);
                count++;
                continue;
            }
            enrollment = documentService.autoConfirmIfExpired(activity, enrollment);
            if (!documentService.isMaterialReviewable(activity, enrollment, true)) {
                throw new BusinessException("教师 " + teacherId + " 尚未确认材料提交，无法发布成绩");
            }

            List<Evaluation> teacherEvals = byTeacher.getOrDefault(teacherId, List.of()).stream()
                    .filter(e -> e.getScore() != null && e.getStatus() == Evaluation.Status.submitted)
                    .toList();
            int requiredReviewers = activity.getReviewerCount() != null ? activity.getReviewerCount() : 0;
            if (requiredReviewers > 0 && teacherEvals.size() < requiredReviewers) {
                throw new BusinessException("教师 " + teacherId + " 评分未完成，无法发布成绩");
            }
            BigDecimal avgScore = calculateAverageScore(teacherEvals);
            boolean isPassed = avgScore != null && avgScore.compareTo(passingScore) >= 0;

            for (Evaluation eval : teacherEvals) {
                eval.setFinalScore(avgScore);
                eval.setIsPublished(true);
                eval.setIsLocked(true);
                eval.setIsPassed(isPassed);
                evaluationRepository.save(eval);
                count++;
            }

            if (isPassed) {
                teacherLevelService.upgradeIfHigher(teacherId, activity.getLevel());
            }
        }
        return count;
    }

    private void publishMissingDocumentAsFailed(Activity activity, Long teacherId) {
        Evaluation evaluation = evaluationRepository
                .findByTeacherIdAndActivityIdAndEvaluatorId(teacherId, activity.getId(), SYSTEM_EVALUATOR_ID)
                .orElse(new Evaluation());
        evaluation.setActivityId(activity.getId());
        evaluation.setEvaluatorId(SYSTEM_EVALUATOR_ID);
        evaluation.setTeacherId(teacherId);
        evaluation.setScore(BigDecimal.ZERO);
        evaluation.setFinalScore(BigDecimal.ZERO);
        evaluation.setStatus(Evaluation.Status.submitted);
        evaluation.setIsPublished(true);
        evaluation.setIsLocked(true);
        evaluation.setIsPassed(false);
        evaluation.setComment("未在活动截止前提交作品");
        evaluationRepository.save(evaluation);
    }

    private int applyFinalScoresForCLevel(Activity activity, List<Evaluation> evaluations, BigDecimal passingScore) {
        // C级：直接取系统评分记录（客观题得分）作为最终成绩
        int count = 0;
        for (Evaluation eval : evaluations) {
            if (SYSTEM_EVALUATOR_ID.equals(eval.getEvaluatorId())
                    && eval.getScore() != null
                    && eval.getStatus() == Evaluation.Status.submitted) {
                BigDecimal finalScore = eval.getScore();
                boolean isPassed = finalScore.compareTo(passingScore) >= 0;
                eval.setFinalScore(finalScore);
                eval.setIsPublished(true);
                eval.setIsLocked(true);
                eval.setIsPassed(isPassed);
                evaluationRepository.save(eval);
                count++;

                if (isPassed) {
                    teacherLevelService.upgradeIfHigher(eval.getTeacherId(), activity.getLevel());
                }
            }
        }
        return count;
    }

    private void finalizeActivityAfterPublish(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        activity.setScoresPublished(true);
        activity.setScoresPublishedAt(now);
        activity.setEnrollmentEnd(minNow(activity.getEnrollmentEnd(), now));
        if (Boolean.TRUE.equals(activity.getHasExam())) {
            activity.setExamEnd(minNow(activity.getExamEnd(), now));
        } else {
            activity.setMaterialEnd(minNow(activity.getMaterialEnd(), now));
        }
        activity.setEndDate(now.toLocalDate());
        activityRepository.save(activity);
        log.info("Activity {} scores published and closed", activity.getId());
    }

    private LocalDateTime minNow(LocalDateTime current, LocalDateTime now) {
        if (current == null || current.isAfter(now)) {
            return now;
        }
        return current;
    }

    private List<Long> parseReviewerIds(String reviewerIds) {
        if (reviewerIds == null || reviewerIds.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(reviewerIds, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("解析reviewerIds失败: {}", reviewerIds, e);
            return List.of();
        }
    }

    public BigDecimal calculateAverageScore(List<Evaluation> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (Evaluation eval : evaluations) {
            if (eval.getScore() != null) {
                sum = sum.add(eval.getScore());
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    public EvaluationSummary getActivitySummary(Long activityId, Long teacherId) {
        List<Evaluation> evaluations = teacherId != null
                ? evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId)
                : evaluationRepository.findByActivityId(activityId);

        EvaluationSummary summary = new EvaluationSummary();
        summary.setTotalEvaluations(evaluations.size());

        if (!evaluations.isEmpty()) {
            BigDecimal avgScore = calculateAverageScore(evaluations);
            summary.setAverageScore(avgScore);
            summary.setEvaluations(evaluations);
        }

        return summary;
    }

    public static class EvaluationSummary {
        private int totalEvaluations;
        private BigDecimal averageScore;
        private List<Evaluation> evaluations;

        public int getTotalEvaluations() { return totalEvaluations; }
        public void setTotalEvaluations(int totalEvaluations) { this.totalEvaluations = totalEvaluations; }
        public BigDecimal getAverageScore() { return averageScore; }
        public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }
        public List<Evaluation> getEvaluations() { return evaluations; }
        public void setEvaluations(List<Evaluation> evaluations) { this.evaluations = evaluations; }
    }
}
