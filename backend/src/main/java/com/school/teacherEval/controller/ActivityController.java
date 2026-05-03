package com.school.teacherEval.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.service.ActivityService;
import com.school.teacherEval.service.EnrollmentService;
import com.school.teacherEval.service.UserService;
import com.school.teacherEval.service.EvaluationService;
import com.school.teacherEval.service.ExamRecordService;
import com.school.teacherEval.service.DocumentService;
import com.school.teacherEval.vo.EnrollmentInfoVO;
import com.school.teacherEval.vo.EnrollmentTeacherVO;
import com.school.teacherEval.vo.MyEnrollmentVO;
import com.school.teacherEval.vo.ReviewProgressVO;
import com.school.teacherEval.vo.ReviewerStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/activities")
@Slf4j
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;
    private final EvaluationService evaluationService;
    private final ExamRecordService examRecordService;
    private final DocumentService documentService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userService.getCurrentUser(username);
    }
    
    @GetMapping(produces = "application/json;charset=UTF-8")
    public ApiResponse<List<Activity>> getAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ApiResponse.success(activityService.getAllActiveOrderByLevel());
        }
        return ApiResponse.success(activityService.getAll());
    }
    
    @GetMapping(value = "/level/{level}", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<Activity>> getByLevel(@PathVariable Activity.Level level) {
        return ApiResponse.success(activityService.getByLevel(level));
    }
    
    @GetMapping(value = "/{id}", produces = "application/json;charset=UTF-8")
    public ApiResponse<Activity> getById(@PathVariable Long id) {
        return ApiResponse.success(activityService.getById(id));
    }
    
    @GetMapping(value = "/available", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<Activity>> getAvailable() {
        return ApiResponse.success(activityService.getAvailableActivities());
    }
    
    @GetMapping(value = "/active", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<Activity>> getActive() {
        return ApiResponse.success(activityService.getAllActiveOrderByLevel());
    }
    
    @GetMapping(value = "/teacher/available", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<Activity>> getAvailableForTeacher() {
        User user = getCurrentUser();
        return ApiResponse.success(activityService.getAvailableForTeacher(user.getId()));
    }

    @GetMapping(value = "/my-enrollments", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<MyEnrollmentVO>> getMyEnrollments() {
        User user = getCurrentUser();
        Long teacherId = user.getId();
        List<PeriodEnrollment> enrollments = enrollmentService.getTeacherEnrollments(teacherId);
        List<MyEnrollmentVO> result = enrollments.stream()
            .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
            .map(e -> {
                Activity activity = activityService.getById(e.getActivityId());
                ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacherId, e.getActivityId());
                Optional<Document> docOpt = documentService.getLatestDocument(teacherId, e.getActivityId());
                List<Evaluation> evaluations = evaluationService.getActivityTeacherEvaluations(e.getActivityId(), teacherId);
                Optional<Evaluation> publishedEval = evaluations.stream()
                    .filter(ev -> Boolean.TRUE.equals(ev.getIsPublished()))
                    .findFirst();

                return new MyEnrollmentVO(
                    e.getId(),
                    e.getActivityId(),
                    e.getEnrolledAt(),
                    activity.getName(),
                    activity.getLevel() != null ? activity.getLevel().name() : null,
                    activity.getHasExam(),
                    activity.getStartDate(),
                    activity.getEndDate(),
                    activity.getExamStart(),
                    activity.getExamEnd(),
                    activity.getMaterialStart(),
                    activity.getMaterialEnd(),
                    examRecord != null ? examRecord.getId() : null,
                    examRecord != null ? examRecord.getScore() : null,
                    examRecord != null && examRecord.getStatus() != null ? examRecord.getStatus().name() : null,
                    examRecord != null ? examRecord.getSubmittedAt() : null,
                    docOpt.map(Document::getId).orElse(null),
                    publishedEval.isPresent(),
                    publishedEval.map(Evaluation::getFinalScore).orElse(null),
                    publishedEval.map(Evaluation::getComment).orElse(null)
                );
            })
            .collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @GetMapping(value = "/reviewer/{evaluatorId}", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<Activity>> getByReviewer(@PathVariable Long evaluatorId) {
        return ApiResponse.success(activityService.getByReviewerId(evaluatorId));
    }
    
    @PostMapping(produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Activity> create(@RequestBody Activity activity) {
        return ApiResponse.success(activityService.create(activity));
    }
    
    @PutMapping(value = "/{id}", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Activity> update(@PathVariable Long id, @RequestBody Activity activity) {
        return ApiResponse.success(activityService.update(id, activity));
    }
    
    @DeleteMapping(value = "/{id}", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
    
    @PutMapping(value = "/{id}/reviewer-config", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Activity> updateReviewerConfig(
            @PathVariable Long id,
            @RequestParam Integer reviewerCount,
            @RequestParam String reviewerIds) {
        return ApiResponse.success(activityService.updateReviewerConfig(id, reviewerCount, reviewerIds));
    }

    @GetMapping(value = "/{id}/review-progress", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<ReviewProgressVO> getReviewProgress(@PathVariable Long id) {
        Activity activity = activityService.getById(id);
        List<User> enrolledTeachers = enrollmentService.getEnrolledTeachersByActivity(id);
        int totalTeachers = enrolledTeachers.size();
        int totalRequired = totalTeachers * (activity.getReviewerCount() != null ? activity.getReviewerCount() : 0);

        // 解析评分人ID列表
        List<Long> reviewerIdList = new ArrayList<>();
        if (activity.getReviewerIds() != null && !activity.getReviewerIds().isEmpty()) {
            try {
                reviewerIdList = new ObjectMapper().readValue(activity.getReviewerIds(),
                    new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                log.error("解析reviewerIds失败", e);
            }
        }

        // 获取每个评分人的批阅数量
        List<ReviewerStatVO> reviewerStats = new ArrayList<>();
        for (Long reviewerId : reviewerIdList) {
            User evaluator = userService.getUserById(reviewerId);
            if (evaluator != null) {
                long completedCount = evaluationService.countByActivityIdAndEvaluatorId(id, reviewerId);
                reviewerStats.add(new ReviewerStatVO(
                    reviewerId,
                    evaluator.getRealName(),
                    completedCount,
                    totalTeachers
                ));
            }
        }

        // 计算总完成数
        long totalCompleted = reviewerStats.stream()
            .mapToLong(ReviewerStatVO::getCompletedCount)
            .sum();

        // 判断评分状态
        String reviewStatus;
        if (activity.getScoresPublished() != null && activity.getScoresPublished()) {
            reviewStatus = "已发布";
        } else if (totalRequired > 0 && totalCompleted >= totalRequired) {
            reviewStatus = "评分完成";
        } else if (totalCompleted > 0) {
            reviewStatus = "评分中";
        } else if (totalRequired > 0) {
            reviewStatus = "待评分";
        } else {
            reviewStatus = "未配置";
        }

        ReviewProgressVO result = new ReviewProgressVO(
            totalTeachers,
            activity.getReviewerCount(),
            reviewerStats,
            totalCompleted,
            totalRequired,
            reviewStatus,
            activity.getScoresPublished()
        );

        return ApiResponse.success(result);
    }
    
    @GetMapping(value = "/{id}/can-enroll", produces = "application/json;charset=UTF-8")
    public ApiResponse<Boolean> canEnroll(@PathVariable Long id) {
        User user = getCurrentUser();
        return ApiResponse.success(activityService.canEnroll(id, user.getId()));
    }
    
    @GetMapping(value = "/{id}/enrollment-info", produces = "application/json;charset=UTF-8")
    public ApiResponse<EnrollmentInfoVO> getEnrollmentInfo(@PathVariable Long id) {
        User user = getCurrentUser();
        Long teacherId = user.getId();
        Activity activity = activityService.getById(id);
        long enrolledCount = activityService.getEnrolledCount(id);
        Integer maxParticipants = activity.getMaxParticipants();
        int remaining = (maxParticipants != null && maxParticipants > 0)
            ? (int)(maxParticipants - enrolledCount)
            : -1;

        EnrollmentInfoVO vo = new EnrollmentInfoVO();
        vo.setActivityId(id);
        vo.setActivityName(activity.getName());
        vo.setLevel(activity.getLevel() != null ? activity.getLevel().name() : null);
        vo.setHasExam(activity.getHasExam());
        vo.setMaxParticipants(maxParticipants);
        vo.setEnrolledCount(enrolledCount);
        vo.setRemaining(remaining);
        vo.setEnrollmentStart(activity.getEnrollmentStart());
        vo.setEnrollmentEnd(activity.getEnrollmentEnd());
        vo.setStartDate(activity.getStartDate());
        vo.setEndDate(activity.getEndDate());
        vo.setReviewerCount(activity.getReviewerCount());

        // 查询当前用户的报名详情
        List<PeriodEnrollment> enrollments = enrollmentService.getTeacherEnrollments(teacherId);
        Optional<PeriodEnrollment> myEnrollment = enrollments.stream()
            .filter(e -> e.getActivityId().equals(id) && e.getStatus() == PeriodEnrollment.Status.enrolled)
            .findFirst();

        if (myEnrollment.isPresent()) {
            PeriodEnrollment enrollment = myEnrollment.get();
            vo.setEnrolledAt(enrollment.getEnrolledAt());
            vo.setEnrollmentStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null);

            // 查询考试记录
            ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacherId, id);
            if (examRecord != null) {
                vo.setExamRecordId(examRecord.getId());
                vo.setExamScore(examRecord.getScore());
                vo.setExamStatus(examRecord.getStatus() != null ? examRecord.getStatus().name() : null);
                vo.setExamSubmittedAt(examRecord.getSubmittedAt());
            }

            // 查询文档
            Optional<Document> docOpt = documentService.getLatestDocument(teacherId, id);
            if (docOpt.isPresent()) {
                Document doc = docOpt.get();
                vo.setDocumentId(doc.getId());
                vo.setDocumentTitle(doc.getTitle());
                vo.setDocumentFileName(doc.getFileName());
                vo.setDocumentFileSize(doc.getFileSize());
                vo.setDocumentCreatedAt(doc.getCreatedAt());
            }

            // 查询评分（已发布）
            List<Evaluation> evaluations = evaluationService.getActivityTeacherEvaluations(id, teacherId);
            Optional<Evaluation> publishedEval = evaluations.stream()
                .filter(ev -> Boolean.TRUE.equals(ev.getIsPublished()))
                .findFirst();
            if (publishedEval.isPresent()) {
                Evaluation eval = publishedEval.get();
                vo.setScorePublished(true);
                vo.setFinalScore(eval.getFinalScore());
                vo.setComment(eval.getComment());
            } else {
                vo.setScorePublished(false);
            }
        }

        return ApiResponse.success(vo);
    }
    
    @PostMapping(value = "/{id}/enroll", produces = "application/json;charset=UTF-8")
    public ApiResponse<Void> enroll(@PathVariable Long id) {
        User user = getCurrentUser();
        try {
            enrollmentService.enroll(id, user.getId());
            return ApiResponse.success("报名成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping(value = "/{id}/enrollments", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<EnrollmentTeacherVO>> getEnrollments(@PathVariable Long id) {
        List<User> teachers = enrollmentService.getEnrolledTeachersByActivity(id);
        Activity activity = activityService.getById(id);
        List<EnrollmentTeacherVO> result = teachers.stream()
            .map(teacher -> {
                PeriodEnrollment enrollment = enrollmentService.getEnrollment(id, teacher.getId());
                Long examRecordId = null;
                LocalDateTime submittedAt = null;
                String submissionStatus = "not_started";

                if ("C".equals(activity.getLevel())) {
                    ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacher.getId(), id);
                    if (examRecord != null) {
                        examRecordId = examRecord.getId();
                        if (examRecord.getSubmittedAt() != null) {
                            submittedAt = examRecord.getSubmittedAt();
                            submissionStatus = "submitted";
                        } else {
                            LocalDateTime now = LocalDateTime.now();
                            if (activity.getExamEnd() != null && now.isAfter(activity.getExamEnd())) {
                                submissionStatus = "not_submitted";
                            } else {
                                submissionStatus = examRecord.getStatus() != null ? examRecord.getStatus().name() : "not_started";
                            }
                        }
                    }
                } else {
                    var docOpt = documentService.getLatestDocument(teacher.getId(), id);
                    if (docOpt.isPresent()) {
                        submittedAt = docOpt.get().getCreatedAt();
                        submissionStatus = "submitted";
                    }
                }

                return new EnrollmentTeacherVO(
                    teacher.getId(),
                    teacher.getUsername(),
                    teacher.getRealName(),
                    teacher.getDepartment(),
                    enrollment != null ? enrollment.getEnrolledAt() : null,
                    examRecordId,
                    submittedAt,
                    submissionStatus
                );
            })
            .collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(result);
    }
}