package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 活动业务规则校验器
 * 集中管理活动创建/更新时的所有时间窗口和状态校验
 */
@Component
public class ActivityValidator {

    /**
     * 校验活动通用时间规则
     */
    public void validateCommon(Activity activity) {
        // 报名时间必填
        if (activity.getEnrollmentStart() == null) {
            throw new BusinessException("报名开始时间不能为空");
        }
        if (activity.getEnrollmentEnd() == null) {
            throw new BusinessException("报名结束时间不能为空");
        }
        if (activity.getEnrollmentStart().isAfter(activity.getEnrollmentEnd())) {
            throw new BusinessException("报名开始时间必须早于报名结束时间");
        }
    }

    /**
     * 校验 C 级活动特有的考试规则
     */
    public void validateCLevel(Activity activity) {
        if (activity.getLevel() != Activity.Level.C) {
            return;
        }
        if (activity.getExamStart() == null) {
            throw new BusinessException("C级活动考试开始时间不能为空");
        }
        // 自动计算考试结束时间
        if (activity.getExamEnd() == null && activity.getExamDurationMinutes() != null) {
            activity.setExamEnd(activity.getExamStart().plusMinutes(activity.getExamDurationMinutes()));
        }
        if (activity.getExamEnd() == null) {
            throw new BusinessException("C级活动考试结束时间不能为空");
        }
        if (activity.getExamStart().isBefore(activity.getEnrollmentEnd())) {
            throw new BusinessException("C级活动考试开始时间必须位于报名时间结束之后");
        }
        if (activity.getExamStart().isAfter(activity.getExamEnd())) {
            throw new BusinessException("考试开始时间必须早于考试结束时间");
        }
    }

    /**
     * 校验非 C 级活动特有的材料上传规则
     */
    public void validateNonCLevel(Activity activity) {
        if (activity.getLevel() == Activity.Level.C) {
            return;
        }
        if (activity.getMaterialStart() == null) {
            throw new BusinessException("材料上传开始时间不能为空");
        }
        if (activity.getMaterialEnd() == null) {
            throw new BusinessException("材料上传结束时间不能为空");
        }
        if (activity.getMaterialStart().isAfter(activity.getMaterialEnd())) {
            throw new BusinessException("材料上传开始时间必须早于材料上传结束时间");
        }
    }

    /**
     * 校验活动启用前的业务规则
     */
    public void validateActivation(Activity activity) {
        // C级为客观题考核，无需评分员
        if (activity.getLevel() == Activity.Level.C) {
            return;
        }
        Integer reviewerCount = activity.getReviewerCount();
        if (reviewerCount == null || reviewerCount == 0) {
            throw new BusinessException("评分人数量不能为0，请先添加评分人");
        }
        String reviewerIds = activity.getReviewerIds();
        if (reviewerIds == null || reviewerIds.isEmpty() || "[]".equals(reviewerIds)) {
            throw new BusinessException("评分人ID列表为空，请先添加评分人");
        }
    }

    /**
     * 执行完整的创建/更新校验
     */
    public void validateForSave(Activity activity) {
        validateCommon(activity);
        validateCLevel(activity);
        validateNonCLevel(activity);
    }
}
