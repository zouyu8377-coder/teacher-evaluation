package com.school.teacherEval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationValidator {

    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final ExamRecordService examRecordService;
    private final DocumentRepository documentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final DocumentService documentService;
    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validateUsersExist(Long evaluatorId, Long teacherId) {
        userRepository.findById(evaluatorId)
                .orElseThrow(() -> new BusinessException("评分员不存在"));
        userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException("教师不存在"));
    }

    public void validateEvaluatorPermission(Long evaluatorId, Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity.getLevel() == Activity.Level.C) {
            throw new BusinessException("C级活动为客观题考试，无需人工评分");
        }

        String reviewerIds = activity.getReviewerIds();
        if (reviewerIds == null || reviewerIds.isEmpty()) {
            throw new BusinessException("该活动未配置评分员，无法评分");
        }

        try {
            List<Long> reviewerIdList = objectMapper.readValue(reviewerIds, new TypeReference<List<Long>>() {});
            if (!reviewerIdList.contains(evaluatorId)) {
                throw new BusinessException("您无权对该活动评分");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析评分员列表失败: reviewerIds={}", reviewerIds, e);
            throw new BusinessException("评分员配置解析失败: " + e.getMessage());
        }
    }

    public void validateActivityOpenForEvaluation(Long activityId) {
        // A/B activities remain scoreable after the material window ends until scores are published.
    }

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

    public void validateTeacherCompletion(Long teacherId, Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity.getLevel() == Activity.Level.C) {
            ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacherId, activityId);
            if (examRecord == null || examRecord.getStatus() != ExamRecord.Status.submitted) {
                throw new BusinessException("该教师尚未完成考试，无法评分");
            }
            return;
        }

        boolean hasDocument = documentRepository.findFirstByActivityIdAndUserId(activityId, teacherId).isPresent();
        if (!hasDocument) {
            throw new BusinessException("该教师尚未上传考核材料，无法评分");
        }
        PeriodEnrollment enrollment = enrollmentRepository.findByActivityIdAndTeacherId(activityId, teacherId)
                .orElseThrow(() -> new BusinessException("教师未报名该活动"));
        enrollment = documentService.autoConfirmIfExpired(activity, enrollment);
        if (!documentService.isMaterialReviewable(activity, enrollment, true)) {
            throw new BusinessException("教师尚未确认材料提交，暂不能评分");
        }
    }

    public void validateNotLocked(Long activityId, Long teacherId) {
        List<Evaluation> existingEvals = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
        boolean locked = existingEvals.stream().anyMatch(e -> Boolean.TRUE.equals(e.getIsLocked()));
        if (locked) {
            throw new BusinessException("该教师的成绩已发布锁定，无法修改评分");
        }
    }

    public void validateForSubmit(Long evaluatorId, Long teacherId, Long activityId, BigDecimal score) {
        validateUsersExist(evaluatorId, teacherId);
        validateEvaluatorPermission(evaluatorId, activityId);
        validateActivityOpenForEvaluation(activityId);
        validateScoreRange(score);
        validateTeacherCompletion(teacherId, activityId);
        validateNotLocked(activityId, teacherId);
    }
}