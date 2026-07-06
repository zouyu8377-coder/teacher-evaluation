package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.vo.AssessmentStatusVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssessmentStatusService {

    public AssessmentStatusVO evaluate(Activity activity,
                                       PeriodEnrollment enrollment,
                                       ExamRecord examRecord,
                                       Document document,
                                       Boolean scorePublished,
                                       Boolean isPassed) {
        LocalDateTime now = LocalDateTime.now();
        boolean enrolled = enrollment != null && enrollment.getStatus() == PeriodEnrollment.Status.enrolled;
        boolean published = Boolean.TRUE.equals(scorePublished);

        if (published) {
            return new AssessmentStatusVO(
                    Boolean.TRUE.equals(isPassed) ? "published_passed" : "published_failed",
                    Boolean.TRUE.equals(isPassed) ? "已发布通过" : "已发布未通过",
                    List.of("view_result")
            );
        }

        if (activity == null) {
            return new AssessmentStatusVO("unknown", "状态未知", List.of());
        }

        if (!enrolled) {
            return enrollmentStatus(activity, now);
        }

        if (Boolean.TRUE.equals(activity.getHasExam())) {
            return examStatus(activity, examRecord, now);
        }

        return materialStatus(activity, document, now);
    }

    private AssessmentStatusVO enrollmentStatus(Activity activity, LocalDateTime now) {
        if (activity.getEnrollmentStart() != null && now.isBefore(activity.getEnrollmentStart())) {
            return new AssessmentStatusVO("enrollment_pending", "报名未开始", List.of());
        }
        if (activity.getEnrollmentEnd() != null && now.isAfter(activity.getEnrollmentEnd())) {
            return new AssessmentStatusVO("enrollment_ended", "报名已结束", List.of());
        }
        return new AssessmentStatusVO("enrollment_open", "可报名", List.of("enroll"));
    }

    private AssessmentStatusVO examStatus(Activity activity, ExamRecord examRecord, LocalDateTime now) {
        if (examRecord != null && examRecord.getStatus() == ExamRecord.Status.submitted) {
            return new AssessmentStatusVO("exam_submitted_wait_publish", "考试完成，等待成绩发布", List.of("view_exam"));
        }

        String windowState = getWindowState(activity.getExamStart(), activity.getExamEnd(), now);
        if (examRecord != null && examRecord.getStatus() == ExamRecord.Status.in_progress) {
            if ("open".equals(windowState)) {
                return new AssessmentStatusVO("exam_in_progress", "考试中", List.of("continue_exam"));
            }
            return new AssessmentStatusVO("exam_window_closed", "考试时间已结束", List.of());
        }

        if ("pending".equals(windowState)) {
            return new AssessmentStatusVO("exam_pending", "考试尚未开始", List.of());
        }
        if ("ended".equals(windowState)) {
            return new AssessmentStatusVO("exam_missed", "考试时间已结束", List.of());
        }
        if ("not_configured".equals(windowState)) {
            return new AssessmentStatusVO("exam_not_configured", "考试时间未设置", List.of());
        }
        return new AssessmentStatusVO("exam_open", "可参加考试", List.of("start_exam"));
    }

    private AssessmentStatusVO materialStatus(Activity activity, Document document, LocalDateTime now) {
        String windowState = getWindowState(activity.getMaterialStart(), activity.getMaterialEnd(), now);
        if (document != null) {
            List<String> actions = "open".equals(windowState)
                    ? List.of("view_document", "replace_document")
                    : List.of("view_document");
            return new AssessmentStatusVO("material_submitted_wait_review", "作品已提交，等待评分", actions);
        }

        if ("pending".equals(windowState)) {
            return new AssessmentStatusVO("material_pending", "活动尚未开始", List.of());
        }
        if ("ended".equals(windowState)) {
            return new AssessmentStatusVO("material_missed", "活动已截止，未提交作品", List.of());
        }
        if ("not_configured".equals(windowState)) {
            return new AssessmentStatusVO("material_not_configured", "活动时间未设置", List.of());
        }
        return new AssessmentStatusVO("material_open", "可提交作品", List.of("upload_document"));
    }

    private String getWindowState(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (start == null || end == null) {
            return "not_configured";
        }
        if (now.isBefore(start)) {
            return "pending";
        }
        if (now.isAfter(end)) {
            return "ended";
        }
        return "open";
    }

    public boolean hasAction(AssessmentStatusVO status, String action) {
        List<String> actions = status != null ? status.getAvailableActions() : new ArrayList<>();
        return actions.contains(action);
    }
}
