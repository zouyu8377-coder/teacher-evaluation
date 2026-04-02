package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.EvaluationPeriod;
import com.school.teacherEval.service.EvaluationPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/periods")
@RequiredArgsConstructor
@Tag(name = "考核周期管理")
public class PeriodController {
    
    private final EvaluationPeriodService periodService;
    
    @GetMapping
    @Operation(summary = "获取考核周期列表")
    public ApiResponse<List<Map<String, Object>>> getPeriods() {
        List<EvaluationPeriod> periods = periodService.getAllPeriods();
        List<Map<String, Object>> result = periods.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @GetMapping("/active")
    @Operation(summary = "获取当前活跃周期")
    public ApiResponse<Map<String, Object>> getActivePeriod() {
        EvaluationPeriod period = periodService.getActivePeriod();
        if (period == null) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(toVO(period));
    }
    
    @PostMapping
    @Operation(summary = "创建考核周期")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<EvaluationPeriod> createPeriod(@RequestBody EvaluationPeriod period) {
        EvaluationPeriod created = periodService.createPeriod(period);
        return ApiResponse.success(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新考核周期")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<EvaluationPeriod> updatePeriod(@PathVariable Long id, @RequestBody EvaluationPeriod period) {
        EvaluationPeriod updated = periodService.updatePeriod(id, period);
        return ApiResponse.success(updated);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除考核周期")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> deletePeriod(@PathVariable Long id) {
        periodService.deletePeriod(id);
        return ApiResponse.success("删除成功", null);
    }
    
    private Map<String, Object> toVO(EvaluationPeriod period) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", period.getId());
        map.put("name", period.getName());
        map.put("startDate", period.getStartDate());
        map.put("endDate", period.getEndDate());
        map.put("description", period.getDescription());
        map.put("status", period.getStatus().name());
        map.put("createdAt", period.getCreatedAt());
        return map;
    }
}