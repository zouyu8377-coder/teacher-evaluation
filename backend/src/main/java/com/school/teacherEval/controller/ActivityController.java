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
import com.school.teacherEval.service.AssessmentStatusService;
import com.school.teacherEval.vo.AssessmentStatusVO;
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
import java.util.Set;

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
    private final AssessmentStatusService assessmentStatusService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userService.getCurrentUser(username);
    }
    
    @GetMapping(produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<List<Activity>> getAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ApiResponse.success(activityService.getAllActiveOrderByLevel());
        }
        return ApiResponse.success(activityService.getAll());
    }
    
    @GetMapping(value = "/level/{level}", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<List<Activity>> getByLevel(@PathVariable Activity.Level level) {
        return ApiResponse.success(activityService.getByLevel(level));
    }
    
    @GetMapping(value = "/{id}", produces = "application/json;charset=UTF-8")
    public ApiResponse<Activity> getById(@PathVariable Long id) {
        return ApiResponse.success(activityService.getById(id));
    }
    
    @GetMapping(value = "/available", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<List<Activity>> getAvailable() {
        return ApiResponse.success(activityService.getAvailableActivities());
    }
    
    @GetMapping(value = "/active", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
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

                AssessmentStatusVO status = assessmentStatusService.evaluate(
                    activity,
                    e,
                    examRecord,
                    docOpt.orElse(null),
                    publishedEval.isPresent(),
                    publishedEval.map(Evaluation::getIsPassed).orElse(null)
                );

                MyEnrollmentVO vo = new MyEnrollmentVO();
                vo.setId(e.getId());
                vo.setActivityId(e.getActivityId());
                vo.setEnrolledAt(e.getEnrolledAt());
                vo.setActivityName(activity.getName());
                vo.setLevel(activity.getLevel() != null ? activity.getLevel().name() : null);
                vo.setHasExam(activity.getHasExam());
                vo.setStartDate(activity.getStartDate());
                vo.setEndDate(activity.getEndDate());
                vo.setExamStart(activity.getExamStart());
                vo.setExamEnd(activity.getExamEnd());
                vo.setMaterialStart(activity.getMaterialStart());
                vo.setMaterialEnd(activity.getMaterialEnd());
                vo.setExamRecordId(examRecord != null ? examRecord.getId() : null);
                vo.setExamScore(examRecord != null ? examRecord.getScore() : null);
                vo.setExamStatus(examRecord != null && examRecord.getStatus() != null ? examRecord.getStatus().name() : null);
                vo.setExamSubmittedAt(examRecord != null ? examRecord.getSubmittedAt() : null);
                vo.setDocumentId(docOpt.map(Document::getId).orElse(null));
                if (activity.getLevel() != Activity.Level.C) {
                    PeriodEnrollment effectiveEnrollment = docOpt.isPresent()
                            ? documentService.autoConfirmIfExpired(activity, e)
                            : e;
                    vo.setMaterialStatus(getMaterialStatus(activity, effectiveEnrollment, docOpt.isPresent()));
                    vo.setMaterialSubmittedAt(effectiveEnrollment.getMaterialSubmittedAt());
                    vo.setCanConfirmMaterial(canConfirmMaterial(activity, effectiveEnrollment, docOpt.isPresent()));
                    vo.setCanCancelMaterial(canCancelMaterial(activity, effectiveEnrollment));
                }
                vo.setScorePublished(publishedEval.isPresent());
                vo.setFinalScore(publishedEval.map(Evaluation::getFinalScore).orElse(null));
                vo.setIsPassed(publishedEval.map(Evaluation::getIsPassed).orElse(null));
                vo.setComment(publishedEval.map(Evaluation::getComment).orElse(null));
                vo.setBusinessStatus(status.getBusinessStatus());
                vo.setStatusText(status.getStatusText());
                vo.setAvailableActions(status.getAvailableActions());
                return vo;
            })
            .collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @GetMapping(value = "/reviewer/{evaluatorId}", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
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
        return ApiResponse.success("success", null);
    }
    
    @GetMapping(value = "/{id}/review-progress", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<ReviewProgressVO> getReviewProgress(@PathVariable Long id) {
        Activity activity = activityService.getById(id);
        List<User> enrolledTeachers = enrollmentService.getEnrolledTeachersByActivity(id);
        int totalTeachers = enrolledTeachers.size();
        int submittedTeachers = totalTeachers;
        Set<Long> reviewableTeacherIds = enrolledTeachers.stream()
            .map(User::getId)
            .collect(java.util.stream.Collectors.toSet());
        if (activity.getLevel() != Activity.Level.C) {
            reviewableTeacherIds = enrolledTeachers.stream()
                .filter(teacher -> {
                    Optional<Document> docOpt = documentService.getLatestDocument(teacher.getId(), id);
                    PeriodEnrollment enrollment = enrollmentService.getEnrollment(id, teacher.getId());
                    if (docOpt.isPresent()) {
                        enrollment = documentService.autoConfirmIfExpired(activity, enrollment);
                    }
                    return documentService.isMaterialReviewable(activity, enrollment, docOpt.isPresent());
                })
                .map(User::getId)
                .collect(java.util.stream.Collectors.toSet());

            Set<Long> evaluatedTeacherIds = evaluationService.getActivitySummary(id, null).getEvaluations() == null
                ? Set.of()
                : evaluationService.getActivitySummary(id, null).getEvaluations().stream()
                    .filter(e -> e.getScore() != null)
                    .filter(e -> e.getStatus() == Evaluation.Status.submitted)
                    .map(Evaluation::getTeacherId)
                    .collect(java.util.stream.Collectors.toSet());
            reviewableTeacherIds = new java.util.HashSet<>(reviewableTeacherIds);
            reviewableTeacherIds.addAll(evaluatedTeacherIds);
            submittedTeachers = reviewableTeacherIds.size();
        }
        int totalRequired = submittedTeachers * (activity.getReviewerCount() != null ? activity.getReviewerCount() : 0);

        // 闁荤喐鐟辩徊楣冩倵閻ｅ本瀚氶柛鏇ㄥ亜閻庤霉濠婂懐妲癉闂佸憡甯楅〃澶愬Υ?
        List<Long> reviewerIdList = new ArrayList<>();
        if (activity.getReviewerIds() != null && !activity.getReviewerIds().isEmpty()) {
            try {
                reviewerIdList = new ObjectMapper().readValue(activity.getReviewerIds(),
                    new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                log.error("瑙ｆ瀽 reviewerIds 澶辫触", e);
            }
        }

        // 闂佸吋鍎抽崲鑼躲亹閸ヮ灝鎺曠疀鎼淬劌娈濋柣鐘叉搐鐎氼剟宕规惔銏㈩洸闁惧繗顫夐悾閬嶆煙闂堟稓绉烘俊鎻掑瀵偅瀵奸弶鎴烆仭
        List<ReviewerStatVO> reviewerStats = new ArrayList<>();
        for (Long reviewerId : reviewerIdList) {
            User evaluator = userService.getUserById(reviewerId);
            if (evaluator != null) {
                final Set<Long> countedTeacherIds = reviewableTeacherIds;
                long completedCount = evaluationService.getActivitySummary(id, null).getEvaluations() == null
                    ? 0
                    : evaluationService.getActivitySummary(id, null).getEvaluations().stream()
                        .filter(e -> reviewerId.equals(e.getEvaluatorId()))
                        .filter(e -> countedTeacherIds.contains(e.getTeacherId()))
                        .filter(e -> e.getScore() != null)
                        .count();
                reviewerStats.add(new ReviewerStatVO(
                    reviewerId,
                    evaluator.getRealName(),
                    completedCount,
                    activity.getLevel() == Activity.Level.C ? totalTeachers : submittedTeachers
                ));
            }
        }

        // 闁荤姳绶ょ槐鏇㈡偩婵犳艾绠戦悹鍥皺閺嗘岸鏌熺€涙ê濮堥柡?
        long totalCompleted = reviewerStats.stream()
            .mapToLong(ReviewerStatVO::getCompletedCount)
            .sum();

        // 闂佸憡甯囬崐鏍蓟閸モ晜瀚氶柛鏇ㄥ亜閻庡鏌ｅΟ鍨厫闁?
        String reviewStatus;
        if (Boolean.TRUE.equals(activity.getScoresPublished())) {
            reviewStatus = "published";
        } else if (totalRequired > 0 && totalCompleted >= totalRequired) {
            reviewStatus = "complete";
        } else if (totalCompleted > 0) {
            reviewStatus = "in_progress";
        } else if (totalRequired > 0) {
            reviewStatus = "pending";
        } else {
            reviewStatus = "not_configured";
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

        // 闂佸搫琚崕鎾敋濡ゅ嫨浜归柟鎯у暱椤ゅ懘鏌ｉ～顒€濡介柛鈺傜洴閹啴宕熼鈧闂佸憡鑹剧粔鐑筋敋濞戙垹绠?
        List<PeriodEnrollment> enrollments = enrollmentService.getTeacherEnrollments(teacherId);
        Optional<PeriodEnrollment> myEnrollment = enrollments.stream()
            .filter(e -> e.getActivityId().equals(id) && e.getStatus() == PeriodEnrollment.Status.enrolled)
            .findFirst();

        if (myEnrollment.isPresent()) {
            PeriodEnrollment enrollment = myEnrollment.get();
            vo.setEnrolledAt(enrollment.getEnrolledAt());
            vo.setEnrollmentStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null);

            // 闂佸搫琚崕鎾敋濡ゅ懏鍤€闁告劦鍘惧Σ鎼佹偣娴ｈ绶茬紓?
            ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacherId, id);
            if (examRecord != null) {
                vo.setExamRecordId(examRecord.getId());
                vo.setExamScore(examRecord.getScore());
                vo.setExamStatus(examRecord.getStatus() != null ? examRecord.getStatus().name() : null);
                vo.setExamSubmittedAt(examRecord.getSubmittedAt());
            }

            // 闂佸搫琚崕鎾敋濡ゅ懎妫橀柛銉ｅ妸閳?
            Optional<Document> docOpt = documentService.getLatestDocument(teacherId, id);
            if (docOpt.isPresent()) {
                Document doc = docOpt.get();
                vo.setDocumentId(doc.getId());
                vo.setDocumentTitle(doc.getTitle());
                vo.setDocumentFileName(doc.getFileName());
                vo.setDocumentFileSize(doc.getFileSize());
                vo.setDocumentCreatedAt(doc.getCreatedAt());
            if (activity.getLevel() != Activity.Level.C) {
                PeriodEnrollment effectiveEnrollment = docOpt.isPresent()
                        ? documentService.autoConfirmIfExpired(activity, enrollment)
                        : enrollment;
                vo.setMaterialStatus(getMaterialStatus(activity, effectiveEnrollment, docOpt.isPresent()));
                vo.setMaterialSubmittedAt(effectiveEnrollment.getMaterialSubmittedAt());
                vo.setCanConfirmMaterial(canConfirmMaterial(activity, effectiveEnrollment, docOpt.isPresent()));
                vo.setCanCancelMaterial(canCancelMaterial(activity, effectiveEnrollment));
            }            }

            // 闂佸搫琚崕鎾敋濡ゅ啯瀚氶柛鏇ㄥ亜閻庡鏌ㄥ☉妯煎闁告埊绻濆畷锝夊箣濠靛牜浼岄梺?
            List<Evaluation> evaluations = evaluationService.getActivityTeacherEvaluations(id, teacherId);
            Optional<Evaluation> publishedEval = evaluations.stream()
                .filter(ev -> Boolean.TRUE.equals(ev.getIsPublished()))
                .findFirst();
            if (publishedEval.isPresent()) {
                Evaluation eval = publishedEval.get();
                vo.setScorePublished(true);
                vo.setFinalScore(eval.getFinalScore());
                vo.setIsPassed(eval.getIsPassed());
                vo.setComment(eval.getComment());
            } else {
                vo.setScorePublished(false);
            }

            AssessmentStatusVO status = assessmentStatusService.evaluate(
                activity,
                enrollment,
                examRecord,
                docOpt.orElse(null),
                vo.getScorePublished(),
                vo.getIsPassed()
            );
            vo.setBusinessStatus(status.getBusinessStatus());
            vo.setStatusText(status.getStatusText());
            vo.setAvailableActions(status.getAvailableActions());
        } else {
            AssessmentStatusVO status = assessmentStatusService.evaluate(activity, null, null, null, false, null);
            vo.setBusinessStatus(status.getBusinessStatus());
            vo.setStatusText(status.getStatusText());
            vo.setAvailableActions(status.getAvailableActions());
        }

        return ApiResponse.success(vo);
    }
    
    @PostMapping(value = "/{id}/enroll", produces = "application/json;charset=UTF-8")
    public ApiResponse<Void> enroll(@PathVariable Long id) {
        User user = getCurrentUser();
        enrollmentService.enroll(id, user.getId());
        return ApiResponse.success("success", null);
    }

    @GetMapping(value = "/{id}/enrollments", produces = "application/json;charset=UTF-8")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<List<EnrollmentTeacherVO>> getEnrollments(@PathVariable Long id) {
        List<User> teachers = enrollmentService.getEnrolledTeachersByActivity(id);
        Activity activity = activityService.getById(id);
        var evaluationSummary = evaluationService.getActivitySummary(id, null);
        List<Evaluation> activityEvaluations = evaluationSummary.getEvaluations() != null
                ? evaluationSummary.getEvaluations()
                : List.of();
        List<EnrollmentTeacherVO> result = teachers.stream()
            .map(teacher -> {
                PeriodEnrollment enrollment = enrollmentService.getEnrollment(id, teacher.getId());
                Long examRecordId = null;
                LocalDateTime submittedAt = null;
                String submissionStatus = "not_started";
                String materialStatus = null;

                java.math.BigDecimal examScore = null;
                java.math.BigDecimal finalScore = null;
                Boolean isPassed = null;
                var publishedEvaluation = activityEvaluations.stream()
                        .filter(ev -> teacher.getId().equals(ev.getTeacherId()))
                        .filter(ev -> Boolean.TRUE.equals(ev.getIsPublished()))
                        .filter(ev -> ev.getFinalScore() != null)
                        .findFirst();
                if (publishedEvaluation.isPresent()) {
                    finalScore = publishedEvaluation.get().getFinalScore();
                    isPassed = publishedEvaluation.get().getIsPassed();
                }

                if (activity.getLevel() == Activity.Level.C) {
                    ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacher.getId(), id);
                    if (examRecord != null) {
                        examRecordId = examRecord.getId();
                        examScore = examRecord.getScore();
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
                        // 闂佺懓鐡ㄩ崝妤呭传濡も偓椤斿繘骞撻幒鎴犲矝闁汇埄鍨伴崯顖氼渻閸屾粍濯奸柨娑樺閺嗩剟鏌￠崟闈涚仩闁诡垯绶氶弻鍛潩瀹曞洨鐣?
                        if (Boolean.TRUE.equals(activity.getScoresPublished()) && activity.getPassingScore() != null) {
                            java.math.BigDecimal scoreForResult = finalScore != null ? finalScore : examScore;
                            if (scoreForResult != null) {
                                isPassed = scoreForResult.compareTo(activity.getPassingScore()) >= 0;
                            }
                        }
                    }
                } else {
                    var docOpt = documentService.getLatestDocument(teacher.getId(), id);
                    if (docOpt.isPresent()) {
                        enrollment = documentService.autoConfirmIfExpired(activity, enrollment);
                        submittedAt = docOpt.get().getCreatedAt();
                        materialStatus = getMaterialStatus(activity, enrollment, true);
                        submissionStatus = documentService.isMaterialReviewable(activity, enrollment, true)
                                ? "submitted"
                                : "uploaded_unconfirmed";
                    } else {
                        materialStatus = getMaterialStatus(activity, enrollment, false);
                        LocalDateTime materialEnd = documentService.getMaterialEnd(activity);
                        if (materialEnd != null && LocalDateTime.now().isAfter(materialEnd)) {
                            submissionStatus = "not_submitted";
                        }
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
                    submissionStatus,
                    materialStatus,
                    examScore,
                    finalScore,
                    isPassed
                );
            })
            .collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(result);
    }

    private String getMaterialStatus(Activity activity, PeriodEnrollment enrollment, boolean hasDocument) {
        if (activity == null || activity.getLevel() == Activity.Level.C) {
            return null;
        }
        if (enrollment == null || !hasDocument) {
            return "not_submitted";
        }
        PeriodEnrollment.MaterialStatus status = enrollment.getMaterialStatus();
        if (status == PeriodEnrollment.MaterialStatus.submitted) {
            return "submitted";
        }
        if (status == PeriodEnrollment.MaterialStatus.auto_submitted) {
            return "auto_submitted";
        }
        LocalDateTime end = documentService.getMaterialEnd(activity);
        if (end != null && !LocalDateTime.now().isBefore(end)) {
            return "auto_submitted";
        }
        return "draft";
    }

    private boolean canConfirmMaterial(Activity activity, PeriodEnrollment enrollment, boolean hasDocument) {
        if (activity == null || activity.getLevel() == Activity.Level.C || enrollment == null || !hasDocument) {
            return false;
        }
        if (enrollment.getMaterialStatus() == PeriodEnrollment.MaterialStatus.submitted
                || enrollment.getMaterialStatus() == PeriodEnrollment.MaterialStatus.auto_submitted) {
            return false;
        }
        LocalDateTime end = documentService.getMaterialEnd(activity);
        return end == null || LocalDateTime.now().isBefore(end);
    }

    private boolean canCancelMaterial(Activity activity, PeriodEnrollment enrollment) {
        if (activity == null || activity.getLevel() == Activity.Level.C || enrollment == null) {
            return false;
        }
        if (enrollment.getMaterialStatus() != PeriodEnrollment.MaterialStatus.submitted) {
            return false;
        }
        LocalDateTime end = documentService.getMaterialEnd(activity);
        if (end != null && !LocalDateTime.now().isBefore(end)) {
            return false;
        }
        return evaluationService.getActivityTeacherEvaluations(activity.getId(), enrollment.getTeacherId()).stream()
                .noneMatch(e -> e.getScore() != null);
    }
}

