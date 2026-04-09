package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.ActivityService;
import com.school.teacherEval.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {
    
    private final ActivityService activityService;
    private final EnrollmentService enrollmentService;
    
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
    public ApiResponse<List<Activity>> getAvailableForTeacher(@AuthenticationPrincipal User user) {
        return ApiResponse.success(activityService.getAvailableForTeacher(user.getId()));
    }
    
    @GetMapping(value = "/my-enrollments", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<Map<String, Object>>> getMyEnrollments(@AuthenticationPrincipal User user) {
        List<PeriodEnrollment> enrollments = enrollmentService.getTeacherEnrollments(user.getId());
        List<Map<String, Object>> result = enrollments.stream()
            .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("activityId", e.getActivityId());
                map.put("enrolledAt", e.getEnrolledAt());
                Activity activity = activityService.getById(e.getActivityId());
                map.put("activityName", activity.getName());
                map.put("level", activity.getLevel());
                map.put("hasExam", activity.getHasExam());
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
        activityService.delete(id);
        return ApiResponse.success(null);
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
    public ApiResponse<Boolean> canEnroll(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ApiResponse.success(activityService.canEnroll(id, user.getId()));
    }
    
    @GetMapping(value = "/{id}/enrollment-info", produces = "application/json;charset=UTF-8")
    public ApiResponse<Map<String, Object>> getEnrollmentInfo(@PathVariable Long id) {
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
        info.put("maxParticipants", maxParticipants);
        info.put("enrolledCount", enrolledCount);
        info.put("remaining", remaining);
        info.put("enrollmentStart", activity.getEnrollmentStart());
        info.put("enrollmentEnd", activity.getEnrollmentEnd());
        info.put("startDate", activity.getStartDate());
        info.put("endDate", activity.getEndDate());
        info.put("reviewerCount", activity.getReviewerCount());
        
        return ApiResponse.success(info);
    }
    
    @PostMapping(value = "/{id}/enroll", produces = "application/json;charset=UTF-8")
    public ApiResponse<Void> enroll(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            enrollmentService.enroll(id, user.getId());
            return ApiResponse.success("报名成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}