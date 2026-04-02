package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.EvaluationPeriod;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.EvaluationPeriodService;
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
    private final EvaluationPeriodService periodService;
    
    @GetMapping
    @Operation(summary = "获取评分列表")
    public ApiResponse<Map<String, Object>> getEvaluations(
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        // 教师只能查看自己的评分
        if (currentUser.getRole() == User.Role.teacher) {
            teacherId = currentUser.getId();
        }
        
        Page<Evaluation> evalPage = evaluationService.getEvaluations(periodId, teacherId, page, size);
        
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
    public ApiResponse<List<Map<String, Object>>> getMyEvaluations(
            @RequestParam(required = false) Long periodId) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        List<Evaluation> evaluations;
        if (periodId != null) {
            Evaluation eval = evaluationService.getTeacherPeriodEvaluation(currentUser.getId(), periodId);
            evaluations = eval != null ? List.of(eval) : List.of();
        } else {
            evaluations = evaluationService.getTeacherEvaluations(currentUser.getId());
        }
        
        List<Map<String, Object>> result = evaluations.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        
        return ApiResponse.success(result);
    }
    
    @PostMapping
    @Operation(summary = "提交评分")
    @PreAuthorize("hasRole('evaluator') or hasRole('admin')")
    public ApiResponse<Evaluation> createEvaluation(@RequestBody Map<String, Object> request) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        Long teacherId = Long.valueOf(request.get("teacherId").toString());
        Long periodId = Long.valueOf(request.get("periodId").toString());
        java.math.BigDecimal score = new java.math.BigDecimal(request.get("score").toString());
        String comment = request.get("comment") != null ? request.get("comment").toString() : null;
        
        Evaluation evaluation = evaluationService.createOrUpdateEvaluation(
                currentUser.getId(), teacherId, periodId, score, comment);
        
        return ApiResponse.success(evaluation);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取评分详情")
    public ApiResponse<Map<String, Object>> getEvaluation(@PathVariable Long id) {
        Evaluation evaluation = evaluationService.getEvaluationById(id);
        return ApiResponse.success(toVO(evaluation));
    }
    
    private Map<String, Object> toVO(Evaluation eval) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", eval.getId());
        map.put("evaluatorId", eval.getEvaluatorId());
        map.put("teacherId", eval.getTeacherId());
        map.put("periodId", eval.getPeriodId());
        map.put("score", eval.getScore());
        map.put("comment", eval.getComment());
        map.put("status", eval.getStatus().name());
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
        
        try {
            EvaluationPeriod period = periodService.getPeriodById(eval.getPeriodId());
            map.put("periodName", period.getName());
        } catch (Exception e) {
            map.put("periodName", "");
        }
        
        return map;
    }
}