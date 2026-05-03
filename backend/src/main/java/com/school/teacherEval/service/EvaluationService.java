package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final ActivityRepository activityRepository;
    private final ExamRecordService examRecordService;
    private final DocumentRepository documentRepository;
    
    public Page<Evaluation> getEvaluations(Long activityId, Long teacherId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (activityId != null && teacherId != null) {
            // 注意：JPA仓库方法 findByActivityIdAndTeacherId 返回 List，这里统一走分页查询
            // 由于 repository 没有分页版本，先查 List 再手动分页
            List<Evaluation> list = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), list.size());
            List<Evaluation> subList = start < list.size() ? list.subList(start, end) : List.of();
            return new org.springframework.data.domain.PageImpl<>(subList, pageable, list.size());
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
                .orElseThrow(() -> new RuntimeException("评分记录不存在"));
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
        // 验证用户存在
        userRepository.findById(evaluatorId).orElseThrow(() -> new BusinessException("考核员不存在"));
        userRepository.findById(teacherId).orElseThrow(() -> new BusinessException("教师不存在"));

        // 校验考核员权限 - 必须被分配到该活动
        validateEvaluatorPermission(evaluatorId, activityId);

        // 校验活动状态
        Activity activity = activityService.getById(activityId);
        if (activity.getStatus() == Activity.Status.closed) {
            throw new BusinessException("该活动已结束，无法评分");
        }

        // 校验分数范围
        if (score == null) {
            throw new BusinessException("评分不能为空");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("评分必须在 0-100 之间");
        }
        if (score.scale() > 1) {
            throw new BusinessException("评分最多保留一位小数");
        }

        // 校验教师是否已完成考核内容（C级需提交考试，非C级需上传文档）
        if (activity.getLevel() == Activity.Level.C) {
            ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacherId, activityId);
            if (examRecord == null || examRecord.getStatus() != ExamRecord.Status.submitted) {
                throw new BusinessException("该教师尚未完成考试，无法评分");
            }
        } else {
            var docOpt = documentRepository.findFirstByActivityIdAndUserId(activityId, teacherId);
            if (docOpt.isEmpty()) {
                throw new BusinessException("该教师尚未上传考核文档，无法评分");
            }
        }

        // 校验成绩是否已发布锁定
        List<Evaluation> existingEvals = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
        boolean locked = existingEvals.stream().anyMatch(e -> Boolean.TRUE.equals(e.getIsLocked()));
        if (locked) {
            throw new BusinessException("该教师的成绩已发布锁定，无法修改评分");
        }

        log.info("考核员 {} 为教师 {} 在活动 {} 评分: {}", evaluatorId, teacherId, activityId, score);

        Evaluation evaluation = evaluationRepository
                .findByTeacherIdAndActivityIdAndEvaluatorId(teacherId, activityId, evaluatorId)
                .orElse(new Evaluation());

        evaluation.setActivityId(activityId);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setTeacherId(teacherId);
        evaluation.setScore(score);
        evaluation.setComment(comment);
        evaluation.setStatus(Evaluation.Status.submitted);

        return evaluationRepository.save(evaluation);
    }

    /**
     * 校验考核员是否有权限评分
     */
    private void validateEvaluatorPermission(Long evaluatorId, Long activityId) {
        var activity = activityService.getById(activityId);
        String reviewerIds = activity.getReviewerIds();

        log.info("校验考核员权限: activityId={}, evaluatorId={}, reviewerIds={}", activityId, evaluatorId, reviewerIds);

        if (reviewerIds == null || reviewerIds.isEmpty()) {
            log.warn("活动 {} 未分配考核员", activityId);
            throw new BusinessException("该活动未分配考核员，无法评分");
        }

        // 解析JSON格式的考核员ID列表
        try {
            List<Long> reviewerIdList = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    reviewerIds,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {}
            );

            log.info("解析到的考核员ID列表: {}", reviewerIdList);

            if (!reviewerIdList.contains(evaluatorId)) {
                log.warn("考核员 {} 无权对活动 {} 进行评分", evaluatorId, activityId);
                throw new BusinessException("您无权对该活动进行评分");
            }
        } catch (Exception e) {
            log.error("解析考核员列表失败: reviewerIds={}", reviewerIds, e);
            throw new BusinessException("系统错误: " + e.getMessage());
        }
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
    public int publishScores(Long activityId, Long teacherId) {
        // 获取活动信息并校验
        Activity activity = activityService.getById(activityId);

        // 校验4：考试时间/材料提交时间结束前不得公布成绩
        LocalDateTime now = LocalDateTime.now();
        if (activity.getExamEnd() != null) {
            if (now.isBefore(activity.getExamEnd())) {
                throw new BusinessException("考试时间尚未结束，无法公布成绩");
            }
        } else if (activity.getMaterialEnd() != null) {
            if (now.isBefore(activity.getMaterialEnd())) {
                throw new BusinessException("材料提交时间尚未结束，无法公布成绩");
            }
        }

        // 校验2：已公布成绩的考核无法再次公布（仅针对批量发布）
        if (teacherId == null && activity.getScoresPublished() != null && activity.getScoresPublished()) {
            throw new BusinessException("该考核活动的成绩已公布，无法重复公布");
        }

        // 清理非配置评分员的历史遗留评分（排除系统参考评分）
        List<Long> reviewerIdList = parseReviewerIds(activity.getReviewerIds());
        if (!reviewerIdList.isEmpty()) {
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

        List<Evaluation> evaluations;
        if (teacherId != null) {
            evaluations = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
        } else {
            evaluations = evaluationRepository.findByActivityId(activityId);
        }

        // 按教师分组计算平均分（排除系统参考评分）
        java.util.Map<Long, List<Evaluation>> byTeacher = evaluations.stream()
                .filter(e -> !SYSTEM_EVALUATOR_ID.equals(e.getEvaluatorId()))
                .collect(java.util.stream.Collectors.groupingBy(Evaluation::getTeacherId));

        int count = 0;
        for (java.util.Map.Entry<Long, List<Evaluation>> entry : byTeacher.entrySet()) {
            Long tid = entry.getKey();
            List<Evaluation> teacherEvals = entry.getValue();

            // 计算该教师的平均分
            BigDecimal avgScore = calculateAverageScore(teacherEvals);

            // 为该教师所有已提交的评分设置最终成绩
            for (Evaluation eval : teacherEvals) {
                if (eval.getScore() != null && eval.getStatus() == Evaluation.Status.submitted) {
                    eval.setFinalScore(avgScore);
                    eval.setIsPublished(true);
                    eval.setIsLocked(true);
                    evaluationRepository.save(eval);
                    count++;
                }
            }
        }

        log.info("发布成绩 - 活动: {}, 教师: {}, 发布数量: {}", activityId, teacherId, count);

        // 仅在批量发布（teacherId == null）时更新活动的成绩公布状态
        // 单教师发布不锁定整活动，避免其他教师成绩无法发布
        if (teacherId == null) {
            activity.setScoresPublished(true);
            activity.setScoresPublishedAt(LocalDateTime.now());

            // 报名时间、考试时间/材料时间都结束后自动关闭活动
            boolean enrollmentEnded = activity.getEnrollmentEnd() == null || now.isAfter(activity.getEnrollmentEnd());
            boolean examOrMaterialEnded;
            if (activity.getExamEnd() != null) {
                examOrMaterialEnded = now.isAfter(activity.getExamEnd());
            } else if (activity.getMaterialEnd() != null) {
                examOrMaterialEnded = now.isAfter(activity.getMaterialEnd());
            } else {
                examOrMaterialEnded = true;
            }
            if (enrollmentEnded && examOrMaterialEnded) {
                activity.setStatus(Activity.Status.closed);
                log.info("活动 {} 所有时间窗口已结束，自动关闭", activityId);
            }

            activityRepository.save(activity);
        }

        return count;
    }

    private List<Long> parseReviewerIds(String reviewerIds) {
        if (reviewerIds == null || reviewerIds.isEmpty()) {
            return List.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    reviewerIds,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {}
            );
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
        List<Evaluation> evaluations;
        if (teacherId != null) {
            evaluations = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
        } else {
            evaluations = evaluationRepository.findByActivityId(activityId);
        }
        
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