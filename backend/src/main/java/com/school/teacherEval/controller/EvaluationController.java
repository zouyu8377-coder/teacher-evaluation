package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.EvaluationService;
import com.school.teacherEval.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Tag(name = "考核评分管理")
public class EvaluationController {
    
    private final EvaluationService evaluationService;
    private final UserService userService;
    
    @GetMapping
    @Operation(summary = "获取评分列表")
    public ApiResponse<Map<String, Object>> getEvaluations(
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        if (currentUser.getRole() == User.Role.teacher) {
            teacherId = currentUser.getId();
        }
        
        Page<Evaluation> evalPage = evaluationService.getEvaluations(activityId, teacherId, page, size);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", evalPage.getContent().stream().map(this::toVO).collect(Collectors.toList()));
        data.put("total", evalPage.getTotalElements());
        data.put("page", page);
        data.put("size", size);
        
        return ApiResponse.success(data);
    }
    
    @GetMapping("/teacher/me")
    @Operation(summary = "教师查看自己的成绩")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<List<Map<String, Object>>> getMyEvaluations() {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        List<Evaluation> evaluations = evaluationService.getTeacherPublishedEvaluations(currentUser.getId());
        
        List<Map<String, Object>> result = evaluations.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        
        return ApiResponse.success(result);
    }
    
    @GetMapping("/activity/{activityId}/teacher/{teacherId}")
    @Operation(summary = "获取某教师的所有评分")
    public ApiResponse<Map<String, Object>> getTeacherActivityEvaluations(
            @PathVariable Long activityId, 
            @PathVariable Long teacherId) {
        
        List<Evaluation> evaluations = evaluationService.getActivityTeacherEvaluations(activityId, teacherId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("evaluations", evaluations.stream().map(this::toVO).collect(Collectors.toList()));
        data.put("count", evaluations.size());
        
        BigDecimal avgScore = evaluationService.calculateAverageScore(evaluations);
        data.put("averageScore", avgScore);
        
        return ApiResponse.success(data);
    }
    
    @GetMapping("/activity/{activityId}/summary")
    @Operation(summary = "获取活动评分汇总")
    public ApiResponse<Map<String, Object>> getActivitySummary(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long teacherId) {
        
        EvaluationService.EvaluationSummary summary = evaluationService.getActivitySummary(activityId, teacherId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("totalEvaluations", summary.getTotalEvaluations());
        data.put("averageScore", summary.getAverageScore());
        data.put("evaluations", summary.getEvaluations() != null 
            ? summary.getEvaluations().stream().map(this::toVO).collect(Collectors.toList())
            : List.of());
        
        return ApiResponse.success(data);
    }
    
    @PostMapping
    @Operation(summary = "提交评分")
    @PreAuthorize("hasRole('evaluator') or hasRole('admin')")
    public ApiResponse<Evaluation> createEvaluation(@RequestBody Map<String, Object> request) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        Long teacherId = Long.valueOf(request.get("teacherId").toString());
        Long activityId = Long.valueOf(request.get("activityId").toString());
        BigDecimal score = new BigDecimal(request.get("score").toString());
        String comment = request.get("comment") != null ? request.get("comment").toString() : null;
        
        Evaluation evaluation = evaluationService.createOrUpdateEvaluation(
                currentUser.getId(), teacherId, activityId, score, comment);
        
        return ApiResponse.success(evaluation);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取评分详情")
    public ApiResponse<Map<String, Object>> getEvaluation(@PathVariable Long id) {
        Evaluation evaluation = evaluationService.getEvaluationById(id);
        return ApiResponse.success(toVO(evaluation));
    }
    
    @PostMapping("/publish")
    @Operation(summary = "公布成绩")
    @PreAuthorize("hasRole('evaluator') or hasRole('admin')")
    public ApiResponse<Integer> publishScores(
            @RequestParam Long activityId, 
            @RequestParam(required = false) Long teacherId) {
        int count = evaluationService.publishScores(activityId, teacherId);
        return ApiResponse.success(count);
    }
    
    private Map<String, Object> toVO(Evaluation eval) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", eval.getId());
        map.put("activityId", eval.getActivityId());
        map.put("evaluatorId", eval.getEvaluatorId());
        map.put("teacherId", eval.getTeacherId());
        map.put("score", eval.getScore());
        map.put("finalScore", eval.getFinalScore());
        map.put("comment", eval.getComment());
        map.put("status", eval.getStatus().name());
        map.put("isPublished", eval.getIsPublished());
        map.put("isLocked", eval.getIsLocked());
        map.put("createdAt", eval.getCreatedAt());
        
        try {
            User evaluator = userService.getUserById(eval.getEvaluatorId());
            map.put("evaluatorName", evaluator.getRealName());
        } catch (Exception e) {
            map.put("evaluatorName", "");
        }
        
        try {
            User teacher = userService.getUserById(eval.getTeacherId());
            map.put("teacherName", teacher.getRealName());
        } catch (Exception e) {
            map.put("teacherName", "");
        }
        
        return map;
    }
}