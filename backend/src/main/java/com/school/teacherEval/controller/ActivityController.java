package com.school.teacherEval.controller;

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
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;
    private final EvaluationService evaluationService;
    private final ExamRecordService examRecordService;
    private final DocumentService documentService;
    private final EvaluationRepository evaluationRepository;
    private final DocumentRepository documentRepository;

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
    public ApiResponse<List<Map<String, Object>>> getMyEnrollments() {
        User user = getCurrentUser();
        Long teacherId = user.getId();
        List<PeriodEnrollment> enrollments = enrollmentService.getTeacherEnrollments(teacherId);
        List<Map<String, Object>> result = enrollments.stream()
            .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", e.getId());
                map.put("activityId", e.getActivityId());
                map.put("enrolledAt", e.getEnrolledAt());
                Activity activity = activityService.getById(e.getActivityId());
                map.put("activityName", activity.getName());
                map.put("level", activity.getLevel());
                map.put("hasExam", activity.getHasExam());
                map.put("startDate", activity.getStartDate());
                map.put("endDate", activity.getEndDate());
                // 考试/材料时间
                map.put("examStart", activity.getExamStart());
                map.put("examEnd", activity.getExamEnd());
                map.put("materialStart", activity.getMaterialStart());
                map.put("materialEnd", activity.getMaterialEnd());

                // 查询考试记录
                ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacherId, e.getActivityId());
                if (examRecord != null) {
                    map.put("examRecordId", examRecord.getId());
                    map.put("examScore", examRecord.getScore());
                    map.put("examStatus", examRecord.getStatus());
                    map.put("examSubmittedAt", examRecord.getSubmittedAt());
                } else {
                    map.put("examRecordId", null);
                    map.put("examScore", null);
                    map.put("examStatus", null);
                    map.put("examSubmittedAt", null);
                }

                // 查询文档
                Optional<Document> docOpt = documentRepository.findFirstByActivityIdAndUserId(e.getActivityId(), teacherId);
                map.put("documentId", docOpt.map(Document::getId).orElse(null));

                // 查询评分（已发布）
                List<Evaluation> evaluations = evaluationRepository.findByActivityIdAndTeacherId(e.getActivityId(), teacherId);
                Optional<Evaluation> publishedEval = evaluations.stream()
                    .filter(ev -> Boolean.TRUE.equals(ev.getIsPublished()))
                    .findFirst();
                if (publishedEval.isPresent()) {
                    Evaluation eval = publishedEval.get();
                    map.put("scorePublished", true);
                    map.put("finalScore", eval.getScore());
                    map.put("comment", eval.getComment());
                } else {
                    map.put("scorePublished", false);
                    map.put("finalScore", null);
                    map.put("comment", null);
                }

                return map;
            })
            .toList();
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
        // 先验证删除条件
        activityService.validateDelete(id);
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
    
    @GetMapping(value = "/{id}/can-enroll", produces = "application/json;charset=UTF-8")
    public ApiResponse<Boolean> canEnroll(@PathVariable Long id) {
        User user = getCurrentUser();
        return ApiResponse.success(activityService.canEnroll(id, user.getId()));
    }
    
    @GetMapping(value = "/{id}/enrollment-info", produces = "application/json;charset=UTF-8")
    public ApiResponse<Map<String, Object>> getEnrollmentInfo(@PathVariable Long id) {
        User user = getCurrentUser();
        Long teacherId = user.getId();
        Activity activity = activityService.getById(id);
        long enrolledCount = activityService.getEnrolledCount(id);
        Integer maxParticipants = activity.getMaxParticipants();
        int remaining = (maxParticipants != null && maxParticipants > 0)
            ? (int)(maxParticipants - enrolledCount)
            : -1;

        Map<String, Object> info = new HashMap<>();
        info.put("activityId", id);
        info.put("activityName", activity.getName());
        info.put("level", activity.getLevel());
        info.put("hasExam", activity.getHasExam());
        info.put("maxParticipants", maxParticipants);
        info.put("enrolledCount", enrolledCount);
        info.put("remaining", remaining);
        info.put("enrollmentStart", activity.getEnrollmentStart());
        info.put("enrollmentEnd", activity.getEnrollmentEnd());
        info.put("startDate", activity.getStartDate());
        info.put("endDate", activity.getEndDate());
        info.put("reviewerCount", activity.getReviewerCount());

        // 查询当前用户的报名详情
        List<PeriodEnrollment> enrollments = enrollmentService.getTeacherEnrollments(teacherId);
        Optional<PeriodEnrollment> myEnrollment = enrollments.stream()
            .filter(e -> e.getActivityId().equals(id) && e.getStatus() == PeriodEnrollment.Status.enrolled)
            .findFirst();

        if (myEnrollment.isPresent()) {
            PeriodEnrollment enrollment = myEnrollment.get();
            info.put("enrolledAt", enrollment.getEnrolledAt());
            info.put("status", enrollment.getStatus());

            // 查询考试记录
            ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacherId, id);
            if (examRecord != null) {
                info.put("examRecordId", examRecord.getId());
                info.put("examScore", examRecord.getScore());
                info.put("examStatus", examRecord.getStatus());
                info.put("examSubmittedAt", examRecord.getSubmittedAt());
            }

            // 查询文档
            Optional<Document> docOpt = documentRepository.findFirstByActivityIdAndUserId(id, teacherId);
            if (docOpt.isPresent()) {
                Document doc = docOpt.get();
                info.put("documentId", doc.getId());
                info.put("documentTitle", doc.getTitle());
                info.put("documentFileName", doc.getFileName());
                info.put("documentFileSize", doc.getFileSize());
                info.put("documentCreatedAt", doc.getCreatedAt());
            }

            // 查询评分（已发布）
            List<Evaluation> evaluations = evaluationRepository.findByActivityIdAndTeacherId(id, teacherId);
            Optional<Evaluation> publishedEval = evaluations.stream()
                .filter(ev -> Boolean.TRUE.equals(ev.getIsPublished()))
                .findFirst();
            if (publishedEval.isPresent()) {
                Evaluation eval = publishedEval.get();
                info.put("scorePublished", true);
                info.put("finalScore", eval.getScore());
                info.put("comment", eval.getComment());
            } else {
                info.put("scorePublished", false);
                info.put("finalScore", null);
                info.put("comment", null);
            }
        } else {
            info.put("enrolledAt", null);
            info.put("status", null);
            info.put("examRecordId", null);
            info.put("examScore", null);
            info.put("documentId", null);
            info.put("scorePublished", false);
            info.put("finalScore", null);
            info.put("comment", null);
        }

        return ApiResponse.success(info);
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
    public ApiResponse<List<Map<String, Object>>> getEnrollments(@PathVariable Long id) {
        List<User> teachers = enrollmentService.getEnrolledTeachersByActivity(id);
        Activity activity = activityService.getById(id);
        List<Map<String, Object>> result = teachers.stream()
            .map(teacher -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", teacher.getId());
                map.put("username", teacher.getUsername());
                map.put("realName", teacher.getRealName());
                map.put("department", teacher.getDepartment());
                // 获取报名时间
                PeriodEnrollment enrollment = enrollmentService.getEnrollment(id, teacher.getId());
                if (enrollment != null) {
                    map.put("enrolledAt", enrollment.getEnrolledAt());
                }
                // 根据活动级别获取提交时间
                if ("C".equals(activity.getLevel())) {
                    // C级：获取考试提交时间
                    ExamRecord examRecord = examRecordService.getRecordByTeacherAndActivity(teacher.getId(), id);
                    if (examRecord != null) {
                        map.put("examRecordId", examRecord.getId());
                        if (examRecord.getSubmittedAt() != null) {
                            map.put("submittedAt", examRecord.getSubmittedAt());
                            map.put("submissionStatus", examRecord.getStatus().name());
                        } else {
                            map.put("submittedAt", null);
                            map.put("submissionStatus", examRecord.getStatus().name());
                        }
                    } else {
                        map.put("examRecordId", null);
                        map.put("submittedAt", null);
                        map.put("submissionStatus", "not_started");
                    }
                } else {
                    // 其他级别：获取文档上传时间
                    var docOpt = documentService.getLatestDocument(teacher.getId(), id);
                    if (docOpt.isPresent()) {
                        Document doc = docOpt.get();
                        map.put("submittedAt", doc.getCreatedAt());
                        map.put("submissionStatus", "submitted");
                    } else {
                        map.put("submittedAt", null);
                        map.put("submissionStatus", "not_started");
                    }
                }
                return map;
            })
            .toList();
        return ApiResponse.success(result);
    }
}