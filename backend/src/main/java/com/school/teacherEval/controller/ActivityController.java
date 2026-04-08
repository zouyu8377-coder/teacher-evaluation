package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.ActivityService;
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
    
    @GetMapping
    public ApiResponse<List<Activity>> getAll() {
        return ApiResponse.success(activityService.getAll());
    }
    
    @GetMapping("/level/{level}")
    public ApiResponse<List<Activity>> getByLevel(@PathVariable Activity.Level level) {
        return ApiResponse.success(activityService.getByLevel(level));
    }
    
    @GetMapping("/{id}")
    public ApiResponse<Activity> getById(@PathVariable Long id) {
        return ApiResponse.success(activityService.getById(id));
    }
    
    @GetMapping("/available")
    public ApiResponse<List<Activity>> getAvailable() {
        return ApiResponse.success(activityService.getAvailableActivities());
    }
    
    @GetMapping("/active")
    public ApiResponse<List<Activity>> getActive() {
        return ApiResponse.success(activityService.getAllActiveOrderByLevel());
    }
    
    @GetMapping("/teacher/available")
    public ApiResponse<List<Activity>> getAvailableForTeacher(@AuthenticationPrincipal User user) {
        return ApiResponse.success(activityService.getAvailableForTeacher(user.getId()));
    }
    
    @GetMapping("/reviewer/{evaluatorId}")
    public ApiResponse<List<Activity>> getByReviewer(@PathVariable Long evaluatorId) {
        return ApiResponse.success(activityService.getByReviewerId(evaluatorId));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Activity> create(@RequestBody Activity activity) {
        return ApiResponse.success(activityService.create(activity));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Activity> update(@PathVariable Long id, @RequestBody Activity activity) {
        return ApiResponse.success(activityService.update(id, activity));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ApiResponse.success(null);
    }
    
    @PutMapping("/{id}/reviewer-config")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Activity> updateReviewerConfig(
            @PathVariable Long id,
            @RequestParam Integer reviewerCount,
            @RequestParam String reviewerIds) {
        return ApiResponse.success(activityService.updateReviewerConfig(id, reviewerCount, reviewerIds));
    }
    
    @GetMapping("/{id}/can-enroll")
    public ApiResponse<Boolean> canEnroll(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ApiResponse.success(activityService.canEnroll(id, user.getId()));
    }
    
    @GetMapping("/{id}/enrollment-info")
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
}