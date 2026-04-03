package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.EvaluationPeriod;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.EnrollmentService;
import com.school.teacherEval.service.EvaluationPeriodService;
import com.school.teacherEval.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/periods")
@RequiredArgsConstructor
@Tag(name = "考核周期管理")
public class PeriodController {
    
    private final EvaluationPeriodService periodService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;
    
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
    
    @PostMapping("/{id}/enroll")
    @Operation(summary = "教师报名考核周期")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<PeriodEnrollment> enroll(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        PeriodEnrollment enrollment = enrollmentService.enroll(id, currentUser.getId());
        return ApiResponse.success(enrollment);
    }
    
    @GetMapping("/{id}/enrollments")
    @Operation(summary = "获取已报名老师列表")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<List<Map<String, Object>>> getEnrollments(@PathVariable Long id) {
        List<User> teachers = enrollmentService.getEnrolledTeachers(id);
        List<Map<String, Object>> result = teachers.stream()
                .map(this::toTeacherVO)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @DeleteMapping("/{id}/enrollments/{teacherId}")
    @Operation(summary = "踢出已报名老师")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> removeEnrollment(@PathVariable Long id, @PathVariable Long teacherId) {
        enrollmentService.removeEnrollment(id, teacherId);
        return ApiResponse.success("已踢出该老师", null);
    }
    
    @GetMapping("/available")
    @Operation(summary = "获取可报名周期(教师)")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<List<Map<String, Object>>> getAvailablePeriods() {
        List<EvaluationPeriod> periods = enrollmentService.getAvailablePeriods();
        List<Map<String, Object>> result = periods.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @GetMapping("/my-enrollments")
    @Operation(summary = "获取我的报名周期(教师)")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<List<Map<String, Object>>> getMyEnrollments() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        List<EvaluationPeriod> periods = enrollmentService.getEnrolledPeriods(currentUser.getId());
        List<Map<String, Object>> result = periods.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @GetMapping("/enrolled-teachers")
    @Operation(summary = "获取已报名的教师列表(考核员)")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<List<Map<String, Object>>> getEnrolledTeachers() {
        List<EvaluationPeriod> periods = periodService.getAllPeriods().stream()
                .filter(p -> p.getStatus() == EvaluationPeriod.Status.active)
                .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (EvaluationPeriod period : periods) {
            Map<String, Object> periodMap = new HashMap<>();
            periodMap.put("id", period.getId());
            periodMap.put("name", period.getName());
            
            List<User> teachers = enrollmentService.getEnrolledTeachers(period.getId());
            List<Map<String, Object>> teacherList = teachers.stream()
                    .map(this::toTeacherVO)
                    .collect(Collectors.toList());
            periodMap.put("teachers", teacherList);
            periodMap.put("enrolledCount", teachers.size());
            
            result.add(periodMap);
        }
        
        return ApiResponse.success(result);
    }
    
    private Map<String, Object> toVO(EvaluationPeriod period) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", period.getId());
        map.put("name", period.getName());
        map.put("startDate", period.getStartDate());
        map.put("endDate", period.getEndDate());
        map.put("description", period.getDescription());
        map.put("status", period.getStatus().name());
        map.put("createdAt", period.getCreatedAt());
        
        long count = enrollmentService.getEnrolledTeachers(period.getId()).size();
        map.put("enrolledCount", count);
        
        return map;
    }
    
    private Map<String, Object> toTeacherVO(User teacher) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", teacher.getId());
        map.put("realName", teacher.getRealName());
        map.put("department", teacher.getDepartment());
        return map;
    }
}