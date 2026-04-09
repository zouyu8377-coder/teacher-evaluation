package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    
    private final ActivityService activityService;
    
    @GetMapping(value = "/level-passed", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Long>> getLevelPassedCount() {
        Map<String, Long> result = new HashMap<>();
        for (Activity.Level level : Activity.Level.values()) {
            result.put(level.name(), activityService.getPassedCountByLevel(level));
        }
        return ResponseEntity.ok(result);
    }
    
    @GetMapping(value = "/active-activities", produces = "application/json;charset=UTF-8")
    public ApiResponse<List<Activity>> getActiveActivities() {
        List<Activity> activities = activityService.getAllActiveOrderByLevel();
        return ApiResponse.success(activities);
    }
}