package com.school.teacherEval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评分业务规则校验器
 * 集中管理评分提交时的所有校验规则
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationValidator {

    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final ExamRecordService examRecordService;
    private final DocumentRepository documentRepository;
    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 校验评分员和教师用户是否存在
     */
    public void validateUsersExist(Long evaluatorId, Long teacherId) {
        userRepository.findById(evaluatorId)
                .orElseThrow(() -> new BusinessException("考核员不存在"));
        userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException("教师不存在"));
    }

    /**
     * 校验考核员是否有权限对活动进行评分
     */
    public void validateEvaluatorPermission(Long evaluatorId, Long activityId) {
        Activity activity = activityService.getById(activityId);

        // C级为客观题考核，无需人工评分
        if (activity.getLevel() == Activity.Level.C) {
            throw new BusinessException("C级考核为客观题，无需人工评分");
        }

        String reviewerIds = activity.getReviewerIds();

        log.info("校验考核员权限: activityId={}, evaluatorId={}, reviewerIds={}", activityId, evaluatorId, reviewerIds);

        if (reviewerIds == null || reviewerIds.isEmpty()) {
            log.warn("活动 {} 未分配考核员", activityId);
            throw new BusinessException("该活动未分配考核员，无法评分");
        }

        try {
            List<Long> reviewerIdList = objectMapper.readValue(reviewerIds, new TypeReference<List<Long>>() {});
            log.info("解析到的考核员ID列表: {}", reviewerIdList);

            if (!reviewerIdList.contains(evaluatorId)) {
                log.warn("考核员 {} 无权对活动 {} 进行评分", evaluatorId, activityId);
                throw new BusinessException("您无权对该活动进行评分");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析考核员列表失败: reviewerIds={}", reviewerIds, e);
            throw new BusinessException("系统错误: " + e.getMessage());
        }
    }

    /**
     * 校验活动是否允许评分（未结束）
     */
    public void validateActivityOpenForEvaluation(Long activityId) {
        // 活动状态机制已移除，不再校验 enabled/disabled
    }

    /**
     * 校验分数范围（0-100，最多一位小数）
     */
    public void validateScoreRange(BigDecimal score) {
        if (score == null) {
            throw new BusinessException("评分不能为空");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("评分必须在 0-100 之间");
        }
        if (score.scale() > 1) {
            throw new BusinessException("评分最多保留一位小数");
        }
    }

    /**
     * 校验教师是否已完成考核内容：
     * - C级：已提交考试
     * - 非C级：已上传文档
     */
    public void validateTeacherCompletion(Long teacherId, Long activityId) {
        Activity activity = activityService.getById(activityId);
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
    }

    /**
     * 校验成绩是否已发布锁定
     */
    public void validateNotLocked(Long activityId, Long teacherId) {
        List<Evaluation> existingEvals = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
        boolean locked = existingEvals.stream().anyMatch(e -> Boolean.TRUE.equals(e.getIsLocked()));
        if (locked) {
            throw new BusinessException("该教师的成绩已发布锁定，无法修改评分");
        }
    }

    /**
     * 执行完整的评分提交前校验
     */
    public void validateForSubmit(Long evaluatorId, Long teacherId, Long activityId, BigDecimal score) {
        validateUsersExist(evaluatorId, teacherId);
        validateEvaluatorPermission(evaluatorId, activityId);
        validateActivityOpenForEvaluation(activityId);
        validateScoreRange(score);
        validateTeacherCompletion(teacherId, activityId);
        validateNotLocked(activityId, teacherId);
    }
}
